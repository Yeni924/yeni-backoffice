(function () {
    let orderTable;

    document.addEventListener("DOMContentLoaded", function () {
        initTable();
        bind("orderRefreshBtn", refreshAll);
        bind("createOrderBtn", createOrder);
        bind("createMockOrderBtn", createMockOrder);
        bind("addOrderItemBtn", function () {
            addItemRow();
            recalculatePreview();
        });

        addItemRow({
            productCode: "K2-DEMO-001",
            productName: "커머스 주문 테스트 상품",
            optionName: "블랙 / 095",
            unitPrice: 10000,
            quantity: 2
        });
        addItemRow({
            productCode: "K2-DEMO-002",
            productName: "추가 구성 상품",
            optionName: "기본",
            unitPrice: 5000,
            quantity: 1
        });

        recalculatePreview();
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
                {title: "주문번호", field: "orderNo", minWidth: 210},
                {title: "구매자", field: "buyerName", width: 120},
                {title: "상품", field: "productName", minWidth: 180},
                {title: "상품 수", field: "itemCount", width: 95, hozAlign: "center"},
                {title: "상품합계", field: "productAmount", hozAlign: "right", formatter: moneyFormatter},
                {title: "배송비", field: "deliveryFee", hozAlign: "right", formatter: moneyFormatter},
                {title: "할인금액", field: "discountAmount", hozAlign: "right", formatter: moneyFormatter},
                {title: "최종 결제금액", field: "payableAmount", hozAlign: "right", formatter: moneyFormatter},
                {title: "주문상태", field: "orderStatus", width: 120, formatter: orderStatusFormatter},
                {title: "결제상태", field: "paymentStatus", width: 150, formatter: paymentStatusFormatter},
                {title: "결제 ID", field: "paymentId", width: 100, hozAlign: "center", formatter: paymentLinkFormatter},
                {title: "처리", width: 210, hozAlign: "center", formatter: actionFormatter, cellClick: actionClick}
            ],
            rowFormatter: function (row) {
                const order = row.getData();
                const holder = document.createElement("div");
                holder.className = "order-item-detail";
                holder.innerHTML = renderItems(order.items || []);
                row.getElement().appendChild(holder);
            }
        });
    }

    function addItemRow(item) {
        const body = document.getElementById("orderItemBody");
        const row = document.createElement("tr");
        row.innerHTML = ""
            + "<td><input class=\"item-product-code\" type=\"text\" value=\"" + escapeHtml(item?.productCode || "") + "\" placeholder=\"상품코드\" /></td>"
            + "<td><input class=\"item-product-name\" type=\"text\" value=\"" + escapeHtml(item?.productName || "") + "\" placeholder=\"상품명\" /></td>"
            + "<td><input class=\"item-option-name\" type=\"text\" value=\"" + escapeHtml(item?.optionName || "") + "\" placeholder=\"옵션\" /></td>"
            + "<td><input class=\"item-unit-price\" type=\"number\" min=\"1\" step=\"100\" value=\"" + Number(item?.unitPrice || 1000) + "\" /></td>"
            + "<td><input class=\"item-quantity\" type=\"number\" min=\"1\" step=\"1\" value=\"" + Number(item?.quantity || 1) + "\" /></td>"
            + "<td class=\"item-amount\">0원</td>"
            + "<td><button class=\"btn btn-light item-remove\" type=\"button\">삭제</button></td>";

        row.querySelectorAll("input").forEach(function (input) {
            input.addEventListener("input", recalculatePreview);
        });
        row.querySelector(".item-remove").addEventListener("click", function () {
            row.remove();
            recalculatePreview();
        });
        body.appendChild(row);
    }

    function collectItems() {
        return Array.from(document.querySelectorAll("#orderItemBody tr")).map(function (row) {
            return {
                productCode: row.querySelector(".item-product-code").value.trim(),
                productName: row.querySelector(".item-product-name").value.trim(),
                optionName: row.querySelector(".item-option-name").value.trim() || null,
                unitPrice: numberValue(row.querySelector(".item-unit-price").value),
                quantity: Math.floor(numberValue(row.querySelector(".item-quantity").value))
            };
        });
    }

    function validateClientOrder(payload) {
        if (!payload.buyerName) {
            throw new Error("구매자명을 입력해 주세요.");
        }
        if (!payload.items.length) {
            throw new Error("상품을 1개 이상 추가해 주세요.");
        }
        payload.items.forEach(function (item, index) {
            const rowNumber = index + 1;
            if (!item.productCode || !item.productName) {
                throw new Error(rowNumber + "번째 상품의 상품코드와 상품명을 입력해 주세요.");
            }
            if (item.unitPrice <= 0) {
                throw new Error(rowNumber + "번째 상품 단가는 0보다 커야 합니다.");
            }
            if (item.quantity <= 0) {
                throw new Error(rowNumber + "번째 상품 수량은 1 이상이어야 합니다.");
            }
        });

        const productAmount = calculateProductAmount(payload.items);
        const payableAmount = productAmount + payload.deliveryFee - payload.discountAmount;
        if (payload.deliveryFee < 0 || payload.discountAmount < 0) {
            throw new Error("배송비와 할인금액은 0 이상이어야 합니다.");
        }
        if (payload.discountAmount > productAmount + payload.deliveryFee) {
            throw new Error("할인금액은 상품합계와 배송비 합계를 초과할 수 없습니다.");
        }
        if (payableAmount <= 0) {
            throw new Error("최종 결제금액은 0보다 커야 합니다.");
        }
    }

    function recalculatePreview() {
        const items = collectItems();
        const productAmount = calculateProductAmount(items);
        const deliveryFee = Math.max(0, numberValue(document.getElementById("deliveryFeeInput").value));
        const discountAmount = Math.max(0, numberValue(document.getElementById("discountAmountInput").value));
        const payableAmount = productAmount + deliveryFee - discountAmount;

        document.querySelectorAll("#orderItemBody tr").forEach(function (row, index) {
            const item = items[index];
            row.querySelector(".item-amount").textContent = formatMoney(item.unitPrice * item.quantity);
        });
        document.getElementById("productAmountPreview").textContent = formatMoney(productAmount);
        document.getElementById("deliveryFeePreview").textContent = formatMoney(deliveryFee);
        document.getElementById("discountAmountPreview").textContent = formatMoney(discountAmount);
        document.getElementById("payableAmountPreview").textContent = formatMoney(payableAmount);
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
                buyerPhone: document.getElementById("buyerPhoneInput").value.trim() || null,
                deliveryFee: numberValue(document.getElementById("deliveryFeeInput").value),
                discountAmount: numberValue(document.getElementById("discountAmountInput").value),
                items: collectItems()
            };
            validateClientOrder(payload);
            const order = await postJson("/admin/api/commerce/orders", payload);
            setMessage("주문이 생성되었습니다: " + order.orderNo + " / 최종 결제금액 " + formatMoney(order.payableAmount));
            document.getElementById("orderNoInput").value = "";
            await refreshAll();
        } catch (error) {
            setMessage(error.message);
        }
    }

    async function createMockOrder() {
        try {
            const order = await postJson("/admin/api/commerce/orders/mock", {});
            setMessage("Mock 주문이 생성되었습니다: " + order.orderNo + " / 최종 결제금액 " + formatMoney(order.payableAmount));
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
        const ledgerDisabled = order.paymentId ? "" : " disabled";
        return "<button type=\"button\" class=\"btn btn-light\" data-order-action=\"pay\"" + payDisabled + ">결제 승인</button>"
            + " <button type=\"button\" class=\"btn btn-light\" data-order-action=\"ledger\"" + ledgerDisabled + ">원장 보기</button>";
    }

    function paymentLinkFormatter(cell) {
        const value = cell.getValue();
        return value ? "<a href=\"/admin/payment-operations\">#" + value + "</a>" : "-";
    }

    function orderStatusFormatter(cell) {
        return statusFormatter(cell.getValue(), {
            CREATED: "주문 생성",
            PAID: "결제 완료",
            CANCELED: "주문 취소"
        });
    }

    function paymentStatusFormatter(cell) {
        return statusFormatter(cell.getValue(), {
            READY: "결제 대기",
            APPROVED: "승인 완료",
            APPROVE_UNKNOWN: "승인 결과불명",
            FAILED: "승인 실패"
        });
    }

    function statusFormatter(value, labels) {
        return "<span class=\"status " + statusClass(value) + "\">" + (labels[value] || value || "-") + "</span>";
    }

    function statusClass(value) {
        return value === "PAID" || value === "APPROVED" ? "paid"
            : value === "APPROVE_UNKNOWN" ? "unknown"
                : value === "FAILED" || value === "CANCELED" ? "cancel"
                    : "ready";
    }

    function renderItems(items) {
        if (!items.length) {
            return "<span>주문 상품이 없습니다.</span>";
        }
        return items.map(function (item) {
            return "<span>"
                + escapeHtml(item.productCode) + " / "
                + escapeHtml(item.productName)
                + (item.optionName ? " / " + escapeHtml(item.optionName) : "")
                + " / " + formatMoney(item.unitPrice)
                + " x " + item.quantity
                + " = " + formatMoney(item.itemAmount)
                + "</span>";
        }).join("");
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

    function formatMoney(value) {
        return Number(value || 0).toLocaleString("ko-KR") + "원";
    }

    function calculateProductAmount(items) {
        return items.reduce(function (sum, item) {
            return sum + Math.max(0, item.unitPrice) * Math.max(0, item.quantity);
        }, 0);
    }

    function numberValue(value) {
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : 0;
    }

    function escapeHtml(value) {
        return String(value || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function setMessage(message) {
        document.getElementById("orderMessageBox").textContent = message;
    }

    ["deliveryFeeInput", "discountAmountInput"].forEach(function (id) {
        document.addEventListener("input", function (event) {
            if (event.target && event.target.id === id) {
                recalculatePreview();
            }
        });
    });
})();
