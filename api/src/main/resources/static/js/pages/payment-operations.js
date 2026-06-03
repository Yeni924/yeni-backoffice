(function () {
    let currentPaymentId = null;

    document.addEventListener("DOMContentLoaded", function () {
        const orderNo = document.getElementById("orderNo");
        orderNo.value = "ORDER-" + Date.now();

        bind("runPaymentBtn", runPaymentFlow);
        bind("cancelPaymentBtn", cancelPayment);
        bind("runSettlementBtn", runSettlement);
        bind("refreshBtn", refreshAll);
        refreshAll();
    });

    function bind(id, handler) {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener("click", handler);
        }
    }

    async function runPaymentFlow() {
        try {
            setStatus("REQUESTED", "ready");
            const amount = Number(document.getElementById("amount").value);
            const orderNo = document.getElementById("orderNo").value.trim();
            const approved = await postJson("/api/payment-bridge/payments/approve", {
                pgProvider: "MOCK",
                orderNo: orderNo,
                amount: amount,
                currency: "KRW",
                buyerName: document.getElementById("buyerName").value,
                productName: document.getElementById("productName").value,
                idempotencyKey: "APPROVE-" + orderNo,
                channelType: "WEB",
                storeCode: "PORTFOLIO",
                paymentMethod: "CARD"
            });

            currentPaymentId = approved.paymentId;
            document.getElementById("cancelPaymentBtn").disabled = false;
            setStatus("APPROVED", "paid");
            setMessage("승인 완료: " + approved.tid);
            await refreshAll();
        } catch (error) {
            setStatus("FAILED", "cancel");
            setMessage(error.message);
        }
    }

    async function cancelPayment() {
        if (!currentPaymentId) {
            return;
        }
        try {
            const amount = Math.max(1000, Math.floor(Number(document.getElementById("amount").value) / 4));
            const result = await postJson("/api/payment-bridge/payments/" + currentPaymentId + "/cancel", {
                pgProvider: "MOCK",
                cancelAmount: amount,
                cancelReason: "portfolio partial cancel",
                idempotencyKey: "CANCEL-" + Date.now()
            });
            setStatus(result.paymentStatus, result.paymentStatus === "CANCELED" ? "cancel" : "shipping");
            setMessage("취소 완료: " + result.cancelType + " " + formatMoney(result.cancelAmount));
            await refreshAll();
        } catch (error) {
            setMessage(error.message);
        }
    }

    async function runSettlement() {
        try {
            const targetDate = new Date();
            await postJson("/api/admin/settlements/batch/run", {
                targetDate: targetDate.toISOString().slice(0, 10)
            });
            setMessage("정산 배치가 실행되었습니다.");
            await refreshAll();
        } catch (error) {
            setMessage(error.message);
        }
    }

    async function refreshAll() {
        const [summary, payments, sales, externalRequests, settlements] = await Promise.all([
            getJson("/api/admin/payment-statistics/summary"),
            getJson("/api/payment-bridge/payments"),
            getJson("/api/admin/sales"),
            getJson("/api/admin/external-send-requests"),
            getJson("/api/admin/settlements")
        ]);

        document.getElementById("paymentCount").textContent = summary.paymentCount;
        document.getElementById("approvedAmount").textContent = formatMoney(summary.approvedAmount);
        document.getElementById("salesAmount").textContent = formatMoney(summary.salesAmount);
        document.getElementById("failedExternalSendCount").textContent = summary.failedExternalSendCount;

        renderPayments(payments);
        renderSales(sales);
        renderExternal(externalRequests);
        renderSettlements(settlements);
    }

    function renderPayments(payments) {
        renderRows("paymentTableBody", payments, function (payment) {
            return [
                payment.id,
                payment.orderNo,
                shortText(payment.tid),
                formatMoney(payment.approvedAmount),
                formatMoney(payment.canceledAmount),
                badge(payment.paymentStatus)
            ];
        });
    }

    function renderSales(sales) {
        renderRows("salesTableBody", sales, function (sale) {
            return [
                sale.id,
                sale.orderNo,
                sale.saleType,
                formatMoney(sale.saleAmount),
                badge(sale.saleStatus),
                sale.businessDate
            ];
        });
    }

    function renderExternal(requests) {
        const tbody = document.getElementById("externalTableBody");
        tbody.innerHTML = "";
        if (!requests.length) {
            tbody.innerHTML = emptyRow(6);
            return;
        }
        requests.forEach(function (request) {
            const tr = document.createElement("tr");
            tr.innerHTML = cells([
                request.id,
                request.requestKey,
                request.targetSystem,
                badge(request.sendStatus),
                request.retryCount,
                "<button class=\"table-action\" data-send-id=\"" + request.id + "\">Send</button>"
            ]);
            tbody.appendChild(tr);
        });
        tbody.querySelectorAll("[data-send-id]").forEach(function (button) {
            button.addEventListener("click", async function () {
                await postJson("/api/admin/external-send-requests/" + button.dataset.sendId + "/send", {});
                await refreshAll();
            });
        });
    }

    function renderSettlements(settlements) {
        renderRows("settlementTableBody", settlements, function (settlement) {
            return [
                settlement.id,
                settlement.settlementDate,
                formatMoney(settlement.grossAmount),
                formatMoney(settlement.feeAmount),
                formatMoney(settlement.netAmount),
                badge(settlement.settlementStatus)
            ];
        });
    }

    function renderRows(bodyId, rows, mapper) {
        const tbody = document.getElementById(bodyId);
        tbody.innerHTML = "";
        if (!rows.length) {
            tbody.innerHTML = emptyRow(6);
            return;
        }
        rows.forEach(function (row) {
            const tr = document.createElement("tr");
            tr.innerHTML = cells(mapper(row));
            tbody.appendChild(tr);
        });
    }

    function cells(values) {
        return values.map(function (value) {
            return "<td>" + value + "</td>";
        }).join("");
    }

    function emptyRow(colspan) {
        return "<tr><td colspan=\"" + colspan + "\" class=\"empty-cell\">No data.</td></tr>";
    }

    function badge(status) {
        const cls = status === "APPROVED" || status === "SUCCESS" || status === "PAID" ? "paid"
            : status === "FAILED" || status === "CANCELED" ? "cancel"
                : status === "PARTIAL_CANCELED" || status === "CONFIRMED" ? "shipping"
                    : "ready";
        return "<span class=\"status " + cls + "\">" + status + "</span>";
    }

    async function postJson(url, payload) {
        const response = await fetch(url, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload || {})
        });
        return parseResponse(response);
    }

    async function getJson(url) {
        const response = await fetch(url);
        return parseResponse(response);
    }

    async function parseResponse(response) {
        const text = await response.text();
        const data = text ? JSON.parse(text) : {};
        if (!response.ok) {
            throw new Error(data.message || "Request failed.");
        }
        return data;
    }

    function setStatus(text, cls) {
        const target = document.getElementById("flowStatus");
        target.className = "status " + cls;
        target.textContent = text;
    }

    function setMessage(message) {
        document.getElementById("messageBox").textContent = message;
    }

    function shortText(text) {
        return text && text.length > 18 ? text.slice(0, 18) + "..." : text;
    }

    function formatMoney(value) {
        return Number(value || 0).toLocaleString("ko-KR");
    }
})();
