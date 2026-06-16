(function () {
    let orderTable;

    document.addEventListener("DOMContentLoaded", function () {
        initTable();
        bind("orderRefreshBtn", refreshAll);
        bind("createOrderBtn", createOrder);
        bind("createMockOrderBtn", createMockOrder);
        refreshAll().catch(function (error) {
            setMessage(error.message);
        });
    });

    function bind(id, handler) {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener("click", handler);
        }
    }

    function initTable() {
        orderTable = new Tabulator("#commerceOrderTable", {
            layout: "fitColumns",
            pagination: "local",
            paginationSize: 12,
            paginationSizeSelector: [12, 24, 50],
            placeholder: "조회된 주문이 없습니다.",
            columns: [
                {title: "ID", field: "id", width: 75, hozAlign: "center"},
                {title: "주문번호", field: "orderNo", minWidth: 220},
                {title: "구매자", field: "buyerName", width: 130},
                {title: "상품명", field: "productName", minWidth: 190},
                {title: "주문금액", field: "orderAmount", hozAlign: "right", formatter: moneyFormatter},
                {title: "주문상태", field: "orderStatus", width: 120, formatter: statusFormatter},
                {title: "결제상태", field: "paymentStatus", width: 150, formatter: statusFormatter},
                {title: "결제 ID", field: "paymentId", width: 100, hozAlign: "center", formatter: paymentLinkFormatter},
                {title: "최근 메시지", field: "lastMessage", minWidth: 180, formatter: emptyFormatter},
                {title: "처리", width: 240, hozAlign: "center", formatter: actionFormatter, cellClick: actionClick}
            ]
        });
    }

    async function refreshAll() {
        const [summary, orders] = await Promise.all([
            getJson("/admin/api/commerce/orders/summary"),
            getJson("/admin/api/commerce/orders")
        ]);
        document.getElementById("orderTotalCount").textContent = summary.totalCount;
        document.getElementById("orderPaidCount").textContent = summary.paidCount;
        document.getElementById("orderPaymentReadyCount").textContent = summary.paymentReadyCount;
        document.getElementById("orderPaymentUnknownCount").textContent = summary.paymentUnknownCount;
        document.getElementById("orderTotalAmount").textContent = formatMoney(summary.totalOrderAmount);
        orderTable.setData(orders);
    }

    async function createOrder() {
        try {
            const payload = {
                orderNo: document.getElementById("orderNoInput").value.trim() || null,
                buyerName: document.getElementById("buyerNameInput").value.trim(),
                productName: document.getElementById("productNameInput").value.trim(),
                orderAmount: Number(document.getElementById("orderAmountInput").value)
            };
            const order = await postJson("/admin/api/commerce/orders", payload);
            setMessage("주문이 생성되었습니다: " + order.orderNo);
            document.getElementById("orderNoInput").value = "";
            await refreshAll();
        } catch (error) {
            setMessage(error.message);
        }
    }

    async function createMockOrder() {
        try {
            const order = await postJson("/admin/api/commerce/orders/mock", {});
            setMessage("Mock 주문이 생성되었습니다: " + order.orderNo);
            await refreshAll();
        } catch (error) {
            setMessage(error.message);
        }
    }

    async function actionClick(event, cell) {
        const action = event.target && event.target.dataset.orderAction;
        if (!action) {
            return;
        }
        const order = cell.getRow().getData();
        try {
            if (action === "pay") {
                const updated = await postJson("/admin/api/commerce/orders/" + order.id + "/pay", {});
                setMessage(updated.paymentStatus === "APPROVED"
                    ? "결제 승인이 완료되었습니다. 결제 ID #" + updated.paymentId + "에서 원장과 후속처리를 추적할 수 있습니다."
                    : "결제 결과가 확정되지 않았습니다. PG 운영 화면에서 RecoveryTask를 확인해 주세요.");
                await refreshAll();
            } else if (action === "ledger") {
                window.location.href = "/admin/payment-operations/sales-ledger?keyword=" + encodeURIComponent(order.orderNo);
            }
        } catch (error) {
            setMessage(error.message);
        }
    }

    function actionFormatter(cell) {
        const order = cell.getRow().getData();
        const payDisabled = order.paymentStatus !== "READY" ? " disabled" : "";
        const traceDisabled = order.paymentId ? "" : " disabled";
        return "<button type=\"button\" class=\"btn btn-light\" data-order-action=\"pay\"" + payDisabled + ">결제 승인</button>"
            + " <button type=\"button\" class=\"btn btn-light\" data-order-action=\"ledger\"" + traceDisabled + ">원장 보기</button>";
    }

    function paymentLinkFormatter(cell) {
        const value = cell.getValue();
        return value ? "<a href=\"/admin/payment-operations\">#" + value + "</a>" : "-";
    }

    function statusFormatter(cell) {
        const value = cell.getValue();
        return "<span class=\"status " + statusClass(value) + "\">" + statusLabel(value) + "</span>";
    }

    function statusClass(value) {
        return value === "PAID" || value === "APPROVED" ? "paid"
            : value === "APPROVE_UNKNOWN" ? "unknown"
                : value === "FAILED" || value === "CANCELED" ? "cancel"
                    : "ready";
    }

    function statusLabel(value) {
        const labels = {
            CREATED: "주문생성",
            PAID: "결제완료",
            CANCELED: "취소완료",
            READY: "결제대기",
            APPROVED: "승인완료",
            APPROVE_UNKNOWN: "승인 결과불명",
            FAILED: "실패"
        };
        return labels[value] || value || "-";
    }

    async function postJson(url, payload) {
        const request = fetch(url, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload || {})
        });
        const response = window.AppLoading
            ? await window.AppLoading.track(request, "주문 흐름을 처리하고 있어요")
            : await request;
        return parseResponse(response);
    }

    async function getJson(url) {
        const request = fetch(url);
        const response = window.AppLoading
            ? await window.AppLoading.track(request, "주문 데이터를 불러오고 있어요")
            : await request;
        return parseResponse(response);
    }

    async function parseResponse(response) {
        const text = await response.text();
        const data = text ? JSON.parse(text) : {};
        if (!response.ok) {
            const requestId = data.requestId ? " (requestId: " + data.requestId + ")" : "";
            throw new Error((data.message || "요청 처리에 실패했습니다.") + requestId);
        }
        return data;
    }

    function moneyFormatter(cell) {
        return formatMoney(cell.getValue());
    }

    function emptyFormatter(cell) {
        const value = cell.getValue();
        return value === null || value === undefined || value === "" ? "-" : value;
    }

    function formatMoney(value) {
        return Number(value || 0).toLocaleString("ko-KR") + "원";
    }

    function setMessage(message) {
        document.getElementById("orderMessageBox").textContent = message;
    }
})();
