(function () {
    let table;
    let lastPage = null;

    document.addEventListener("DOMContentLoaded", async function () {
        table = new Tabulator("#salesLedgerTable", {
            layout: "fitDataStretch",
            pagination: "local",
            paginationSize: 15,
            paginationSizeSelector: [15, 30, 50],
            placeholder: "조회된 매출 원장 거래가 없습니다.",
            rowClick: function (event, row) {
                openDetail(row.getData());
            },
            columns: [
                {
                    title: "상세",
                    field: "id",
                    width: 78,
                    hozAlign: "center",
                    formatter: function () {
                        return "<button class=\"table-action\" type=\"button\">보기</button>";
                    },
                    cellClick: function (event, cell) {
                        event.stopPropagation();
                        openDetail(cell.getRow().getData());
                    }
                },
                {title: "번호", field: "id", width: 80, hozAlign: "center"},
                {title: "영업일", field: "businessDate", width: 120, sorter: "date"},
                {title: "거래일시", field: "occurredAt", width: 165, formatter: dateTimeFormatter},
                {title: "주문번호", field: "orderNo", width: 260},
                {title: "PG 거래번호", field: "pgTransactionId", width: 250},
                {title: "결제수단", field: "paymentMethod", width: 110, formatter: paymentMethodFormatter},
                {title: "거래유형", field: "saleType", width: 160, formatter: saleTypeFormatter},
                {title: "원 SALE ID", field: "originalSalesTransactionId", width: 120, hozAlign: "center", formatter: originalSaleFormatter},
                {title: "공급가액", field: "supplyAmount", width: 130, hozAlign: "right", formatter: moneyFormatter},
                {title: "부가세", field: "vatAmount", width: 115, hozAlign: "right", formatter: moneyFormatter},
                {title: "총액", field: "totalAmount", width: 140, hozAlign: "right", formatter: signedMoneyFormatter},
                {title: "원장상태", field: "ledgerStatus", width: 130, formatter: ledgerStatusFormatter},
                {title: "정산상태", field: "settlementStatus", width: 150, formatter: settlementStatusFormatter}
            ]
        });

        bindFilters();
        bindModal();
        setDefaultDateRange("salesStartDate", "salesEndDate");
        await refresh();
    });

    function bindFilters() {
        document.getElementById("salesSearchBtn").addEventListener("click", refresh);
        document.getElementById("salesResetBtn").addEventListener("click", async function () {
            document.getElementById("salesKeyword").value = "";
            document.getElementById("salesTypeFilter").value = "";
            document.getElementById("ledgerStatusFilter").value = "";
            document.getElementById("settlementStatusFilter").value = "";
            setQuickFilter("all");
            setDefaultDateRange("salesStartDate", "salesEndDate");
            await refresh();
        });
        ["salesStartDate", "salesEndDate", "salesTypeFilter", "ledgerStatusFilter", "settlementStatusFilter"].forEach(function (id) {
            document.getElementById(id).addEventListener("change", refresh);
        });
        document.getElementById("salesKeyword").addEventListener("keydown", function (event) {
            if (event.key === "Enter") {
                refresh();
            }
        });
        document.querySelectorAll("[data-quick-filter]").forEach(function (button) {
            button.addEventListener("click", function () {
                applyQuickFilter(button.dataset.quickFilter);
            });
        });
        setQuickFilter("all");
    }

    function bindModal() {
        document.getElementById("closeSalesDetailBtn").addEventListener("click", closeDetail);
        document.getElementById("salesDetailModal").addEventListener("click", function (event) {
            if (event.target.id === "salesDetailModal") {
                closeDetail();
            }
        });
    }

    async function refresh() {
        const params = new URLSearchParams();
        appendParam(params, "startDate", document.getElementById("salesStartDate").value);
        appendParam(params, "endDate", document.getElementById("salesEndDate").value);
        appendParam(params, "transactionType", document.getElementById("salesTypeFilter").value);
        appendParam(params, "ledgerStatus", document.getElementById("ledgerStatusFilter").value);
        appendParam(params, "settlementStatus", document.getElementById("settlementStatusFilter").value);
        appendParam(params, "keyword", document.getElementById("salesKeyword").value.trim());
        params.append("page", "0");
        params.append("size", "100");

        const response = await fetch("/admin/api/sales-ledger?" + params.toString());
        lastPage = await parseApiResponse(response);
        table.setData(lastPage.data || []);
        renderSummary(lastPage.summary);
    }

    function appendParam(params, key, value) {
        if (value) {
            params.append(key, value);
        }
    }

    async function applyQuickFilter(value) {
        setQuickFilter(value);
        document.getElementById("salesTypeFilter").value = "";
        document.getElementById("settlementStatusFilter").value = "";
        document.getElementById("ledgerStatusFilter").value = "";

        if (value === "SALE" || value === "CANCEL") {
            document.getElementById("salesTypeFilter").value = value;
        } else if (value === "NOT_SETTLED" || value === "SETTLED" || value === "CARRIED_OVER") {
            document.getElementById("settlementStatusFilter").value = value;
        } else if (value === "EXCLUDED") {
            document.getElementById("settlementStatusFilter").value = "EXCLUDED";
        }
        await refresh();
    }

    function setQuickFilter(value) {
        document.querySelectorAll("[data-quick-filter]").forEach(function (button) {
            button.classList.toggle("active", button.dataset.quickFilter === value);
        });
    }

    function renderSummary(summary) {
        const safe = summary || {};
        document.getElementById("totalSaleAmount").textContent = formatMoney(safe.totalSaleAmount);
        document.getElementById("totalCancelAmount").textContent = formatMoney(safe.totalCancelAmount);
        document.getElementById("netSalesAmount").textContent = formatMoney(safe.netSalesAmount);
        document.getElementById("notSettledCount").textContent = Number(safe.notSettledCount || 0).toLocaleString("ko-KR") + "건";
        document.getElementById("settledCount").textContent = Number(safe.settledCount || 0).toLocaleString("ko-KR") + "건";
        document.getElementById("carriedOverCount").textContent = Number(safe.carriedOverCount || 0).toLocaleString("ko-KR") + "건";
    }

    async function openDetail(row) {
        const [detail, links] = await Promise.all([
            fetchJson("/admin/api/sales-ledger/" + row.id),
            fetchJson("/admin/api/sales-ledger/" + row.id + "/links")
        ]);
        document.getElementById("salesDetailTitle").textContent = "매출 영수증 #" + detail.id;
        document.getElementById("salesDetailBody").innerHTML = renderReceipt(detail, links);
        document.getElementById("salesDetailModal").hidden = false;
    }

    async function fetchJson(url) {
        const response = await fetch(url);
        return parseApiResponse(response);
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

    function closeDetail() {
        document.getElementById("salesDetailModal").hidden = true;
    }

    function renderReceipt(detail, links) {
        const isCancel = detail.saleType === "CANCEL";
        const originalSaleText = links.originalSale
            ? "#" + links.originalSale.id + " / " + formatMoney(links.originalSale.totalAmount)
            : "-";

        return [
            "<article class=\"ledger-receipt\">",
            "  <header class=\"receipt-head\">",
            "    <div>",
            "      <span class=\"receipt-kicker " + saleTypeClass(detail.saleType) + "\">" + saleTypeLabel(detail.saleType) + "</span>",
            "      <h4>" + display(detail.orderNo) + "</h4>",
            "      <p>" + paymentMethodLabel(detail.paymentMethod) + " / PG 거래번호 " + display(detail.pgTransactionId || detail.tid) + "</p>",
            "    </div>",
            "    <div class=\"receipt-total " + (isCancel ? "cancel" : "paid") + "\">",
            "      <span>합계 금액</span>",
            "      <strong>" + formatMoney(detail.totalAmount) + "</strong>",
            "    </div>",
            "  </header>",
            "  <div class=\"receipt-status-line\">",
            textSignal("원장상태 " + ledgerStatusLabel(detail.ledgerStatus), ledgerStatusClass(detail.ledgerStatus)),
            textSignal("정산상태 " + settlementStatusLabel(detail.settlementStatus), settlementStatusClass(detail.settlementStatus)),
            "    <span>영업일 " + display(detail.businessDate) + "</span>",
            "    <span>거래일시 " + formatDateTime(detail.occurredAt) + "</span>",
            "  </div>",
            receiptSection("결제 정보", [
                metaGrid([
                    metaItem("결제수단", paymentMethodLabel(detail.paymentMethod)),
                    metaItem("PG 코드", display(detail.pgCode)),
                    metaItem("PG 거래번호", display(detail.pgTransactionId || detail.tid)),
                    metaItem("거래유형", saleTypeLabel(detail.saleType))
                ])
            ]),
            receiptSection("금액 구성", [
                receiptRow("공급가액", formatMoney(detail.supplyAmount)),
                receiptRow("부가세", formatMoney(detail.vatAmount)),
                receiptRow("합계", formatMoney(detail.totalAmount), "total")
            ]),
            receiptSection("거래 연결", [
                metaGrid([
                    metaItem("원장 ID", "#" + detail.id),
                    metaItem("결제 ID", detail.paymentId ? "#" + detail.paymentId : "-"),
                    metaItem("취소 ID", detail.cancelId ? "#" + detail.cancelId : "-"),
                    metaItem("원 SALE 거래", originalSaleText)
                ])
            ]),
            receiptSection(isCancel ? "CANCEL 취소매출 정보" : "취소 가능 정보", [
                metaGrid([
                    metaItem("누적 취소금액", formatMoney(links.cumulativeCanceledAmount)),
                    metaItem("취소 가능금액", formatMoney(links.cancelableAmount)),
                    metaItem("취소 사유", links.cancelReason || "-"),
                    metaItem("취소 발생일시", formatDateTime(links.canceledAt))
                ])
            ]),
            receiptSection("후속 처리", [
                linkedList("외부전송", links.externalSends, "sendStatus"),
                linkedList("알림톡", links.alimtalkQueues, "status"),
                linkedList("복구 작업", links.recoveryTasks, "status"),
                settlementList("정산 상세", links.settlementDetails)
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

    function metaGrid(items) {
        return "<div class=\"receipt-meta-grid\">" + items.join("") + "</div>";
    }

    function metaItem(label, value) {
        return "<div><span>" + label + "</span><strong>" + display(value) + "</strong></div>";
    }

    function linkedList(title, values, statusField) {
        const items = values && values.length > 0
            ? values.map(function (value) {
                return "<li><span>#" + value.id + "</span><strong>" + statusLabel(value[statusField]) + "</strong></li>";
            }).join("")
            : "<li class=\"receipt-empty\">처리 이력 없음</li>";
        return "<div class=\"receipt-link-list\"><p>" + title + "</p><ul>" + items + "</ul></div>";
    }

    function settlementList(title, values) {
        const items = values && values.length > 0
            ? values.map(function (value) {
                return "<li><span>#" + value.id + " / 원장 #" + value.salesId + "</span><strong>" + formatMoney(value.netAmount) + "</strong></li>";
            }).join("")
            : "<li class=\"receipt-empty\">정산 반영 전</li>";
        return "<div class=\"receipt-link-list\"><p>" + title + "</p><ul>" + items + "</ul></div>";
    }

    function textSignal(label, className) {
        return "<span class=\"text-signal " + className + "\">" + label + "</span>";
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

    function dateTimeFormatter(cell) {
        return formatDateTime(cell.getValue());
    }

    function formatDateTime(value) {
        return value ? String(value).replace("T", " ").slice(0, 19) : "-";
    }

    function moneyFormatter(cell) {
        return formatMoney(cell.getValue());
    }

    function signedMoneyFormatter(cell) {
        const value = Number(cell.getValue() || 0);
        const className = value < 0 ? "cancel" : "paid";
        return "<span class=\"text-signal " + className + "\">" + formatMoney(value) + "</span>";
    }

    function originalSaleFormatter(cell) {
        const row = cell.getRow().getData();
        if (row.saleType !== "CANCEL") {
            return "-";
        }
        return row.originalSalesTransactionId ? "#" + row.originalSalesTransactionId : "연결 필요";
    }

    function saleTypeFormatter(cell) {
        return "<span class=\"text-signal " + saleTypeClass(cell.getValue()) + "\">"
            + saleTypeLabel(cell.getValue()) + "</span>";
    }

    function ledgerStatusFormatter(cell) {
        const value = cell.getValue();
        return "<span class=\"text-signal " + ledgerStatusClass(value) + "\">" + ledgerStatusLabel(value) + "</span>";
    }

    function settlementStatusFormatter(cell) {
        const value = cell.getValue();
        return "<span class=\"text-signal " + settlementStatusClass(value) + "\">" + settlementStatusLabel(value) + "</span>";
    }

    function paymentMethodFormatter(cell) {
        return paymentMethodLabel(cell.getValue());
    }

    function saleTypeClass(value) {
        return value === "CANCEL" ? "cancel" : value === "ADJUST" ? "unknown" : "paid";
    }

    function ledgerStatusClass(value) {
        return value === "ERROR" ? "cancel" : value === "EXCLUDED" ? "unknown" : "paid";
    }

    function settlementStatusClass(value) {
        return value === "EXCLUDED" ? "cancel" : "normal";
    }

    function saleTypeLabel(value) {
        const labels = {
            SALE: "SALE 결제매출",
            CANCEL: "CANCEL 취소매출",
            ADJUST: "ADJUST 보정매출"
        };
        return labels[value] || value || "-";
    }

    function ledgerStatusLabel(value) {
        const labels = {
            POSTED: "반영 완료",
            EXCLUDED: "제외",
            ERROR: "오류",
            ADJUSTED: "보정",
            CANCELED: "취소됨"
        };
        return labels[value] || value || "-";
    }

    function settlementStatusLabel(value) {
        const labels = {
            NOT_SETTLED: "대기",
            SETTLEMENT_READY: "정산 대기",
            CALCULATED: "정산 계산 완료",
            SETTLED: "정산 확정",
            PAID: "지급 완료",
            CARRIED_OVER: "다음정산차감",
            EXCLUDED: "정산 제외"
        };
        return labels[value] || value || "-";
    }

    function statusLabel(value) {
        const labels = {
            READY: "대기",
            SUCCESS: "성공",
            FAILED: "실패",
            RETRY_READY: "재시도 대기",
            PROCESSING: "처리 중",
            COMPLETED: "완료"
        };
        return labels[value] || value || "-";
    }

    function paymentMethodLabel(value) {
        const labels = {
            CARD: "카드",
            BANK: "계좌이체",
            VBANK: "가상계좌",
            MOBILE: "휴대폰결제",
            POINT: "포인트"
        };
        return labels[value] || value || "-";
    }

    function formatMoney(value) {
        return Number(value || 0).toLocaleString("ko-KR") + "원";
    }

    function display(value) {
        return value === null || value === undefined || value === "" ? "-" : value;
    }
})();
