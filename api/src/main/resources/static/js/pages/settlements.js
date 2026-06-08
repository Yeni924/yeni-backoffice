(function () {
    let table;
    let rows = [];

    document.addEventListener("DOMContentLoaded", async function () {
        table = new Tabulator("#settlementPageTable", {
            layout: "fitColumns",
            pagination: "local",
            paginationSize: 15,
            paginationSizeSelector: [10, 15, 30, 50],
            placeholder: "조회된 정산 명세가 없습니다.",
            rowClick: function (event, row) {
                openSettlementDetail(row.getData());
            },
            columns: [
                {title: "ID", field: "id", width: 80, hozAlign: "center"},
                {title: "정산일", field: "settlementDate", width: 130, sorter: "date"},
                {title: "PG사", field: "pgCompany", width: 110, formatter: pgCompanyFormatter},
                {title: "가맹점 ID", field: "mid", width: 150},
                {title: "원장금액", field: "grossAmount", hozAlign: "right", formatter: moneyFormatter},
                {title: "PG 수수료", field: "feeAmount", hozAlign: "right", formatter: moneyFormatter},
                {title: "수수료 VAT", field: "vatAmount", hozAlign: "right", formatter: moneyFormatter},
                {title: "정산 예정금액", field: "netAmount", hozAlign: "right", formatter: moneyFormatter},
                {title: "상태", field: "settlementStatus", width: 120, formatter: statusFormatter}
            ]
        });

        bindFilters();
        bindModal();
        setDefaultDateRange("settlementStartDate", "settlementEndDate");
        await refresh();
    });

    function bindFilters() {
        document.getElementById("runSettlementPageBtn").addEventListener("click", runSettlement);
        document.getElementById("settlementSearchBtn").addEventListener("click", refresh);
        document.getElementById("settlementResetBtn").addEventListener("click", async function () {
            document.getElementById("settlementKeyword").value = "";
            document.getElementById("settlementStatusFilter").value = "";
            setDefaultDateRange("settlementStartDate", "settlementEndDate");
            await refresh();
        });
        ["settlementKeyword", "settlementStatusFilter"].forEach(function (id) {
            document.getElementById(id).addEventListener("input", applyFilters);
            document.getElementById(id).addEventListener("change", applyFilters);
        });
        document.getElementById("settlementKeyword").addEventListener("keydown", function (event) {
            if (event.key === "Enter") {
                refresh();
            }
        });
    }

    function bindModal() {
        document.getElementById("closeSettlementDetailBtn").addEventListener("click", closeSettlementDetail);
        document.getElementById("settlementDetailModal").addEventListener("click", function (event) {
            if (event.target.id === "settlementDetailModal") {
                closeSettlementDetail();
            }
        });
    }

    async function runSettlement() {
        const button = document.getElementById("runSettlementPageBtn");
        const originalText = button.textContent;
        const targetDate = new Date().toISOString().slice(0, 10);
        button.disabled = true;
        button.textContent = "정산 배치 실행 중...";
        try {
            const request = fetch("/admin/api/settlements/batch/run", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({targetDate: targetDate})
            });
        const response = window.AppLoading
                ? await window.AppLoading.track(request, "정산 데이터를 계산하고 있어요")
                : await request;
            await parseApiResponse(response);
            await refresh();
        } finally {
            button.disabled = false;
            button.textContent = originalText;
        }
    }

    async function refresh() {
        const params = new URLSearchParams();
        appendParam(params, "startDate", document.getElementById("settlementStartDate").value);
        appendParam(params, "endDate", document.getElementById("settlementEndDate").value);

        const request = fetch("/admin/api/settlements" + (params.toString() ? "?" + params.toString() : ""));
        const response = window.AppLoading
            ? await window.AppLoading.track(request, "정산 명세를 불러오고 있어요")
            : await request;
        rows = await parseApiResponse(response);
        applyFilters();
    }

    function appendParam(params, key, value) {
        if (value) {
            params.append(key, value);
        }
    }

    function applyFilters() {
        const keyword = document.getElementById("settlementKeyword").value.trim().toLowerCase();
        const status = document.getElementById("settlementStatusFilter").value;

        const filtered = rows.filter(function (row) {
            const matchesStatus = !status || row.settlementStatus === status;
            const matchesKeyword = !keyword || [
                row.settlementDate,
                pgCompanyLabel(row.pgCompany),
                row.mid,
                statusLabel(row.settlementStatus)
            ].some(function (value) {
                return String(value || "").toLowerCase().includes(keyword);
            });
            return matchesStatus && matchesKeyword;
        });

        table.setData(filtered);
        renderSummary(filtered);
    }

    function renderSummary(values) {
        const gross = sum(values, "grossAmount");
        const fee = sum(values, "feeAmount");
        const vat = sum(values, "vatAmount");
        const net = sum(values, "netAmount");
        document.getElementById("settlementSaleAmount").textContent = formatMoney(values.filter(row => Number(row.grossAmount || 0) > 0).reduce((sum, row) => sum + Number(row.grossAmount || 0), 0));
        document.getElementById("settlementCancelAmount").textContent = formatMoney(values.filter(row => Number(row.grossAmount || 0) < 0).reduce((sum, row) => sum + Number(row.grossAmount || 0), 0));
        document.getElementById("settlementGrossAmount").textContent = formatMoney(gross);
        document.getElementById("settlementFeeAmount").textContent = formatMoney(fee);
        document.getElementById("settlementVatAmount").textContent = formatMoney(vat);
        document.getElementById("settlementFixedFeeAmount").textContent = formatMoney(0);
        document.getElementById("settlementAdjustmentAmount").textContent = formatMoney(0);
        document.getElementById("settlementCarryOverAmount").textContent = formatMoney(0);
        document.getElementById("settlementNetAmount").textContent = formatMoney(net);
    }

    function sum(values, field) {
        return values.reduce(function (total, row) {
            return total + Number(row[field] || 0);
        }, 0);
    }

    async function openSettlementDetail(row) {
        const request = fetch("/admin/api/settlements/" + row.id);
        const response = window.AppLoading
            ? await window.AppLoading.track(request, "정산 상세를 정리하고 있어요")
            : await request;
        const page = await parseApiResponse(response);
        document.getElementById("settlementDetailTitle").textContent = "정산 명세 #" + row.id;
        document.getElementById("settlementDetailBody").innerHTML = renderDetail(page);
        document.getElementById("settlementDetailModal").hidden = false;
    }

    async function parseApiResponse(response) {
        const text = await response.text();
        const data = text ? JSON.parse(text) : {};
        if (!response.ok) {
            const requestId = data.requestId ? " (requestId: " + data.requestId + ")" : "";
            throw new Error((data.message || "요청 처리에 실패했습니다.") + requestId);
        }
        return data;
    }

    function closeSettlementDetail() {
        document.getElementById("settlementDetailModal").hidden = true;
    }

    function renderDetail(page) {
        const statement = page.statement || {};
        const feeDetails = page.feeDetails || [];
        const details = page.details || [];
        const feeRows = feeDetails.length > 0
            ? feeDetails.map(function (item) {
                return "<li><span>정책 #" + item.feePolicyId + " / 수수료율 " + percent(item.feeRate) + "</span><strong>수수료 " + formatMoney(item.feeAmount) + " / VAT " + formatMoney(item.vatAmount) + "</strong></li>";
            }).join("")
            : "<li class=\"receipt-empty\">수수료 계산 상세 없음</li>";
        const detailRows = details.length > 0
            ? details.map(function (item) {
                return "<tr><td>#" + item.salesId + "</td><td>" + saleTypeLabel(item.saleType) + "</td><td>" + formatMoney(item.saleAmount) + "</td><td>" + formatMoney(item.feeAmount) + "</td><td>" + formatMoney(item.netAmount) + "</td></tr>";
            }).join("")
            : "<tr><td colspan=\"5\">정산 상세 없음</td></tr>";

        return [
            "<article class=\"ledger-receipt\">",
            "  <header class=\"receipt-head\">",
            "    <div>",
            "      <span class=\"receipt-kicker normal\">" + statusLabel(statement.settlementStatus) + "</span>",
            "      <h4>" + display(statement.settlementDate) + " 정산</h4>",
            "      <p>" + pgCompanyLabel(statement.pgCompany) + " / " + display(statement.mid) + "</p>",
            "    </div>",
            "    <div class=\"receipt-total paid\">",
            "      <span>최종 정산 예정금액</span>",
            "      <strong>" + formatMoney(statement.netAmount) + "</strong>",
            "    </div>",
            "  </header>",
            receiptSection("정산 금액 구성", [
                receiptRow("원장금액", formatMoney(statement.grossAmount)),
                receiptRow("PG 수수료", formatMoney(statement.feeAmount)),
                receiptRow("수수료 VAT", formatMoney(statement.vatAmount)),
                receiptRow("정액 수수료", formatMoney(0)),
                receiptRow("조정금", formatMoney(0)),
                receiptRow("다음정산차감액", formatMoney(0)),
                receiptRow("정산 예정금액", formatMoney(statement.netAmount), "total")
            ]),
            receiptSection("적용 정책 snapshot", [
                "<div class=\"receipt-link-list\"><p>수수료 계산 라인</p><ul>" + feeRows + "</ul></div>"
            ]),
            receiptSection("원장별 정산 상세", [
                "<div class=\"receipt-table-wrap\"><table class=\"receipt-table\"><thead><tr><th>원장</th><th>유형</th><th>원장금액</th><th>PG 수수료</th><th>정산 예정금액</th></tr></thead><tbody>" + detailRows + "</tbody></table></div>"
            ]),
            "</article>"
        ].join("");
    }

    function receiptSection(title, children) {
        return "<section class=\"receipt-section\"><h4>" + title + "</h4>" + children.join("") + "</section>";
    }

    function receiptRow(label, value, className) {
        return "<div class=\"receipt-row " + (className || "") + "\"><span>" + label + "</span><strong>" + display(value) + "</strong></div>";
    }

    function setDefaultDateRange(startId, endId) {
        const today = new Date();
        const start = new Date(today);
        start.setDate(today.getDate() - 30);
        document.getElementById(startId).value = toDateInput(start);
        document.getElementById(endId).value = toDateInput(today);
    }

    function toDateInput(date) {
        return date.toISOString().slice(0, 10);
    }

    function moneyFormatter(cell) {
        return formatMoney(cell.getValue());
    }

    function statusFormatter(cell) {
        return "<span class=\"text-signal normal\" title=\"" + statusDescription(cell.getValue()) + "\">" + statusLabel(cell.getValue()) + "</span>";
    }

    function pgCompanyFormatter(cell) {
        return pgCompanyLabel(cell.getValue());
    }

    function saleTypeLabel(value) {
        const labels = {
            SALE: "SALE 결제매출",
            CANCEL: "CANCEL 취소매출",
            ADJUST: "ADJUST 보정매출"
        };
        return labels[value] || value || "-";
    }

    function pgCompanyLabel(value) {
        const labels = {
            INICIS: "이니시스",
            MOCK: "Mock PG"
        };
        return labels[value] || value || "-";
    }

    function statusLabel(value) {
        const labels = {
            DRAFT: "작성중",
            CONFIRMED: "확정",
            PAID: "지급완료"
        };
        return labels[value] || value || "-";
    }

    function statusDescription(value) {
        const descriptions = {
            DRAFT: "정산 배치가 생성한 초안 상태입니다.",
            CONFIRMED: "검토 후 지급 대상으로 확정된 상태입니다.",
            PAID: "지급 처리까지 완료된 상태입니다."
        };
        return descriptions[value] || "상태: " + (value || "-");
    }

    function percent(value) {
        return (Number(value || 0) * 100).toLocaleString("ko-KR", {maximumFractionDigits: 2}) + "%";
    }

    function formatMoney(value) {
        return Number(value || 0).toLocaleString("ko-KR") + "원";
    }

    function display(value) {
        return value === null || value === undefined || value === "" ? "-" : value;
    }
})();
