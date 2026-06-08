(function () {
    let currentPaymentId = null;
    const tables = {};

    document.addEventListener("DOMContentLoaded", function () {
        const orderNo = document.getElementById("orderNo");
        if (orderNo) {
            orderNo.value = "ORDER-MANUAL-" + Date.now();
        }

        bindTabs();
        initTables();
        bind("runPaymentBtn", runPaymentFlow);
        bind("cancelPaymentBtn", function () {
            return cancelPaymentWithKey("CANCEL-" + Date.now());
        });
        bind("refreshBtn", refreshAll);
        bind("approveUnknownBtn", runApproveUnknown);
        bind("cancelUnknownBtn", function () {
            return cancelPaymentWithKey("CANCEL-UNKNOWN-" + Date.now());
        });

        document.querySelectorAll("[data-scenario]").forEach(function (button) {
            button.addEventListener("click", function () {
                runScenario(button.dataset.scenario, button);
            });
        });

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

    function bindTabs() {
        document.querySelectorAll("[data-tab-target]").forEach(function (button) {
            button.addEventListener("click", function () {
                const target = button.dataset.tabTarget;
                document.querySelectorAll("[data-tab-target]").forEach(function (tab) {
                    tab.classList.toggle("active", tab.dataset.tabTarget === target);
                });
                document.querySelectorAll("[data-tab-panel]").forEach(function (panel) {
                    panel.classList.toggle("active", panel.dataset.tabPanel === target);
                });
                setTimeout(function () {
                    Object.values(tables).forEach(function (table) {
                        table.redraw(true);
                    });
                }, 0);
            });
        });
    }

    function initTables() {
        tables.payments = new Tabulator("#paymentTable", {
            layout: "fitColumns",
            pagination: "local",
            paginationSize: 10,
            paginationSizeSelector: [10, 20, 50],
            placeholder: "조회된 결제 거래가 없습니다.",
            columns: [
                {title: "ID", field: "id", width: 80, hozAlign: "center"},
                {title: "주문번호", field: "orderNo", minWidth: 190},
                {title: "PG 거래번호", field: "tid", minWidth: 180},
                {title: "승인금액", field: "approvedAmount", hozAlign: "right", formatter: moneyFormatter},
                {title: "취소금액", field: "canceledAmount", hozAlign: "right", formatter: moneyFormatter},
                {title: "상태", field: "paymentStatus", width: 150, formatter: statusFormatter, headerFilter: "select", headerFilterParams: {values: paymentStatusFilterValues()}},
                {title: "승인일시", field: "approvedAt", minWidth: 170, formatter: dateTimeFormatter}
            ]
        });

        tables.external = new Tabulator("#externalTable", {
            layout: "fitColumns",
            pagination: "local",
            paginationSize: 8,
            paginationSizeSelector: [8, 15, 30],
            placeholder: "조회된 외부전송 대기건이 없습니다.",
            columns: [
                {title: "ID", field: "id", width: 80, hozAlign: "center"},
                {title: "매출 ID", field: "salesId", width: 100, hozAlign: "center"},
                {title: "요청 키", field: "requestKey", minWidth: 190},
                {title: "대상 시스템", field: "targetSystem", minWidth: 160, formatter: targetSystemFormatter},
                {title: "상태", field: "sendStatus", width: 130, formatter: statusFormatter, headerFilter: "select", headerFilterParams: {values: followupStatusFilterValues()}},
                {title: "재시도", field: "retryCount", width: 90, hozAlign: "center"},
                {title: "마지막 오류", field: "lastErrorMessage", minWidth: 180, formatter: emptyFormatter}
            ]
        });

        tables.alimtalk = new Tabulator("#alimtalkTable", {
            layout: "fitColumns",
            pagination: "local",
            paginationSize: 8,
            paginationSizeSelector: [8, 15, 30],
            placeholder: "조회된 알림톡 발송 대기건이 없습니다.",
            columns: [
                {title: "ID", field: "id", width: 80, hozAlign: "center"},
                {title: "결제 ID", field: "paymentId", width: 100, hozAlign: "center"},
                {title: "메시지 키", field: "messageKey", minWidth: 190},
                {title: "이벤트", field: "eventType", width: 110, formatter: eventTypeFormatter},
                {title: "상태", field: "status", width: 130, formatter: statusFormatter, headerFilter: "select", headerFilterParams: {values: followupStatusFilterValues()}},
                {title: "재시도", field: "retryCount", width: 90, hozAlign: "center"},
                {title: "마지막 오류", field: "lastErrorMessage", minWidth: 180, formatter: emptyFormatter}
            ]
        });

        tables.recovery = new Tabulator("#recoveryTable", {
            layout: "fitColumns",
            pagination: "local",
            paginationSize: 8,
            paginationSizeSelector: [8, 15, 30],
            placeholder: "조회된 복구 작업이 없습니다.",
            columns: [
                {title: "ID", field: "id", width: 70, hozAlign: "center"},
                {title: "작업 키", field: "taskKey", minWidth: 190},
                {title: "결제 ID", field: "paymentId", width: 95, hozAlign: "center", formatter: emptyFormatter},
                {title: "주문번호", field: "orderNo", minWidth: 170, formatter: emptyFormatter},
                {title: "PG 거래번호", field: "tid", minWidth: 170, formatter: emptyFormatter},
                {title: "멱등키", field: "idempotencyKey", minWidth: 150, formatter: emptyFormatter},
                {title: "복구 유형", field: "recoveryType", minWidth: 180, formatter: recoveryTypeFormatter},
                {title: "상태", field: "status", width: 120, formatter: statusFormatter, headerFilter: "select", headerFilterParams: {values: recoveryStatusFilterValues()}},
                {title: "재시도", field: "retryCount", width: 80, hozAlign: "center"},
                {title: "마지막 오류", field: "lastErrorMessage", minWidth: 200, formatter: emptyFormatter},
                {title: "운영 처리", width: 230, hozAlign: "center", formatter: recoveryActionFormatter, cellClick: recoveryActionClick}
            ]
        });
    }

    async function runPaymentFlow() {
        try {
            setStatus("요청 중", "ready");
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
            setCancelButtonsEnabled(approved.paymentStatus === "APPROVED");
            setStatus(statusLabel(approved.paymentStatus), approved.paymentStatus === "APPROVED" ? "paid" : "unknown");
            setMessage(approved.paymentStatus === "APPROVE_UNKNOWN"
                ? "승인 결과불명: PG 결과 확인을 위한 복구 작업이 생성되었습니다."
                : "결제 완료: " + approved.tid);
            await refreshAll();
        } catch (error) {
            setStatus("실패", "cancel");
            setMessage(error.message);
        }
    }

    async function runApproveUnknown() {
        document.getElementById("orderNo").value = "ORDER-APPROVE-UNKNOWN-" + Date.now();
        await runPaymentFlow();
    }

    async function cancelPaymentWithKey(idempotencyKey) {
        if (!currentPaymentId) {
            setMessage("먼저 결제를 승인하거나 취소 시나리오를 실행해 주세요.");
            return;
        }
        try {
            const amount = Math.max(1000, Math.floor(Number(document.getElementById("amount").value) / 4));
            const result = await postJson("/api/payment-bridge/payments/" + currentPaymentId + "/cancel", {
                pgProvider: "MOCK",
                cancelAmount: amount,
                cancelReason: "포트폴리오 시나리오 부분취소",
                idempotencyKey: idempotencyKey
            });
            const statusClass = result.paymentStatus === "CANCELED" ? "cancel"
                : result.paymentStatus === "CANCEL_UNKNOWN" ? "unknown"
                    : "shipping";
            setStatus(statusLabel(result.paymentStatus), statusClass);
            setMessage(result.paymentStatus === "CANCEL_UNKNOWN"
                ? "취소 결과불명: PG 결과 확인을 위한 복구 작업이 생성되었습니다."
                : "취소 완료: " + cancelTypeLabel(result.cancelType) + " " + formatMoney(result.cancelAmount));
            await refreshAll();
        } catch (error) {
            setMessage(error.message);
        }
    }

    async function runScenario(scenarioType, button) {
        const originalText = button ? button.textContent : "";
        try {
            if (button) {
                button.disabled = true;
                button.textContent = "실행 중...";
            }
            setScenarioStatus("실행 중", "ready");
            const result = await postJson("/api/payment-bridge/scenarios/" + scenarioType, {});
            currentPaymentId = result.paymentId || currentPaymentId;
            setCancelButtonsEnabled(Boolean(currentPaymentId));
            renderScenarioResult(result);
            setMessage(result.message);
            await refreshAll();
            document.querySelector(".scenario-output")?.scrollIntoView({behavior: "smooth", block: "nearest"});
        } catch (error) {
            setScenarioStatus("실패", "cancel");
            document.getElementById("scenarioResult").textContent = error.message;
            setMessage(error.message);
        } finally {
            if (button) {
                button.disabled = false;
                button.textContent = originalText;
            }
        }
    }

    async function refreshAll() {
        const [summary, payments, externalRequests, alimtalkQueues, recoveryTasks] = await Promise.all([
            getJson("/api/admin/payment-statistics/summary"),
            getJson("/api/payment-bridge/payments"),
            getJson("/api/admin/external-send-requests"),
            getJson("/api/admin/alimtalk-queues"),
            getJson("/admin/api/recovery/tasks?size=100")
        ]);
        document.getElementById("paymentCount").textContent = summary.approvedPaymentCount;
        document.getElementById("approvedAmount").textContent = formatMoney(summary.approvedAmount);
        document.getElementById("salesAmount").textContent = summary.salesTransactionCount + "건 / " + formatMoney(summary.salesAmount);
        document.getElementById("failedExternalSendCount").textContent = summary.readyExternalSendCount + " / " + summary.failedExternalSendCount;
        document.getElementById("recoveryTaskCount").textContent = summary.recoveryTaskCount;
        document.getElementById("alimtalkQueueCount").textContent = summary.readyAlimtalkCount + " / " + summary.failedAlimtalkCount;
        tables.payments.setData(payments);
        tables.external.setData(externalRequests);
        tables.alimtalk.setData(alimtalkQueues);
        tables.recovery.setData(recoveryTasks.data || []);
    }

    function renderScenarioResult(result) {
        const statusClass = result.status && result.status.includes("FAILED") ? "cancel"
            : result.status && (result.status.includes("UNKNOWN") || result.status.includes("REQUIRED") || result.status.includes("RETRY")) ? "unknown"
                : "paid";
        setScenarioStatus(statusLabel(result.status), statusClass);
        document.getElementById("scenarioResult").textContent =
            result.scenarioName + " / 주문번호 " + result.orderNo + " / " + result.message;
        const timeline = document.getElementById("scenarioTimeline");
        timeline.innerHTML = "";
        (result.timelineSteps || []).forEach(function (step) {
            const li = document.createElement("li");
            li.innerHTML = "<strong>" + step.stepName + " · " + statusLabel(step.status) + "</strong>"
                + step.description
                + (step.referenceId ? "<br><span>참조: " + step.referenceId + "</span>" : "");
            timeline.appendChild(li);
        });
    }

    function recoveryActionFormatter(cell) {
        const task = cell.getRow().getData();
        const disabled = task.status === "SUCCESS" ? " disabled" : "";
        return "<button type=\"button\" class=\"btn btn-light\" data-recovery-action=\"retry\"" + disabled + ">재시도</button>"
            + " <button type=\"button\" class=\"btn btn-light\" data-recovery-action=\"success\"" + disabled + ">성공</button>"
            + " <button type=\"button\" class=\"btn btn-light\" data-recovery-action=\"failed\"" + disabled + ">실패</button>";
    }

    async function recoveryActionClick(event, cell) {
        const action = event.target && event.target.dataset.recoveryAction;
        if (!action) {
            return;
        }
        const task = cell.getRow().getData();
        const path = action === "retry" ? "retry" : action === "success" ? "mark-success" : "mark-failed";
        try {
            await postJson("/admin/api/recovery/tasks/" + task.id + "/" + path, {});
            setMessage("복구 작업을 처리했습니다: " + task.taskKey);
            await refreshAll();
        } catch (error) {
            setMessage(error.message);
        }
    }

    function statusFormatter(cell) {
        return "<span class=\"status " + statusClass(cell.getValue()) + "\" title=\"" + statusDescription(cell.getValue()) + "\">"
            + statusLabel(cell.getValue())
            + "</span>";
    }

    function paymentStatusFilterValues() {
        return {
            "": "전체",
            READY: "대기",
            APPROVED: "결제완료",
            APPROVE_FAILED: "승인실패",
            APPROVE_UNKNOWN: "승인 결과불명",
            PARTIAL_CANCELED: "부분취소",
            CANCELED: "취소완료",
            CANCEL_FAILED: "취소실패",
            CANCEL_UNKNOWN: "취소 결과불명",
            NETWORK_CANCEL_REQUIRED: "망취소 필요"
        };
    }

    function followupStatusFilterValues() {
        return {
            "": "전체",
            READY: "대기",
            SENDING: "전송중",
            SENT: "전송완료",
            SUCCESS: "성공",
            FAILED: "실패",
            RETRY_READY: "재시도 대기"
        };
    }

    function recoveryStatusFilterValues() {
        return {
            "": "전체",
            READY: "대기",
            PROCESSING: "처리중",
            SUCCESS: "성공",
            FAILED: "실패"
        };
    }

    function moneyFormatter(cell) {
        return formatMoney(cell.getValue());
    }

    function dateTimeFormatter(cell) {
        const value = cell.getValue();
        return value ? value.replace("T", " ").slice(0, 19) : "-";
    }

    function emptyFormatter(cell) {
        const value = cell.getValue();
        return value === null || value === undefined || value === "" ? "-" : value;
    }

    function targetSystemFormatter(cell) {
        const labels = {
            SALES_OPERATION_MOCK: "매출운영시스템"
        };
        return labels[cell.getValue()] || cell.getValue() || "-";
    }

    function eventTypeFormatter(cell) {
        const labels = {
            SALE: "결제",
            CANCEL: "취소",
            APPROVE: "승인"
        };
        return labels[cell.getValue()] || cell.getValue() || "-";
    }

    function recoveryTypeFormatter(cell) {
        const labels = {
            APPROVE_UNKNOWN_CHECK: "승인 결과불명 확인",
            CANCEL_UNKNOWN_CHECK: "취소 결과불명 확인",
            NETWORK_CANCEL: "망취소 처리",
            APPROVE_INTERNAL_SAVE_FAILED: "승인 후 내부 저장 실패",
            EXTERNAL_SEND_RETRY: "외부전송 재시도",
            ALIMTALK_RETRY: "알림톡 재발송"
        };
        return labels[cell.getValue()] || cell.getValue() || "-";
    }

    function cancelTypeLabel(value) {
        const labels = {
            PARTIAL: "부분취소",
            FULL: "전체취소"
        };
        return labels[value] || value || "취소";
    }

    function statusClass(status) {
        return status === "APPROVED" || status === "SUCCESS" || status === "PAID" || status === "SENT" ? "paid"
            : status === "FAILED" || status === "CANCELED" || status === "APPROVE_FAILED" || status === "CANCEL_FAILED" ? "cancel"
                : status === "PARTIAL_CANCELED" || status === "CONFIRMED" || status === "SENDING" || status === "PROCESSING" ? "shipping"
                    : status === "APPROVE_UNKNOWN" || status === "CANCEL_UNKNOWN" || status === "NETWORK_CANCEL_REQUIRED" || status === "RETRY_READY" ? "unknown"
                        : "ready";
    }

    function statusLabel(status) {
        const labels = {
            READY: "대기",
            REQUESTED: "요청",
            PROCESSING: "처리중",
            SENDING: "전송중",
            SENT: "전송완료",
            APPROVED: "결제완료",
            APPROVE_FAILED: "승인실패",
            APPROVE_UNKNOWN: "승인 결과불명",
            PARTIAL_CANCELED: "부분취소",
            CANCELED: "취소완료",
            CANCEL_FAILED: "취소실패",
            CANCEL_UNKNOWN: "취소 결과불명",
            NETWORK_CANCEL_REQUIRED: "망취소 필요",
            NETWORK_CANCEL_FAILED: "망취소 실패",
            SUCCESS: "성공",
            FAILED: "실패",
            CONFIRMED: "확정",
            PAID: "지급완료",
            RETRY_READY: "재시도 대기",
            EXTERNAL_SEND_FAILED: "외부전송 실패",
            ALIMTALK_RETRY_READY: "알림톡 재시도",
            REJECTED: "거절",
            UNKNOWN: "알 수 없음"
        };
        return labels[status] || status || "-";
    }

    function statusDescription(status) {
        const descriptions = {
            READY: "아직 처리되지 않았거나 다음 배치를 기다리는 상태입니다.",
            APPROVED: "PG 승인이 성공했고 결제 거래가 완료된 상태입니다.",
            PARTIAL_CANCELED: "승인 금액 중 일부만 취소된 상태입니다.",
            CANCELED: "승인 금액 전체가 취소된 상태입니다.",
            APPROVE_UNKNOWN: "PG 승인 응답이 불명확해 실제 승인 여부를 다시 확인해야 합니다.",
            CANCEL_UNKNOWN: "PG 취소 응답이 불명확해 실제 취소 여부를 다시 확인해야 합니다.",
            NETWORK_CANCEL_REQUIRED: "PG 승인은 성공했지만 내부 처리 실패로 망취소 또는 복구가 필요한 상태입니다.",
            RETRY_READY: "실패한 후속처리를 다시 시도할 수 있는 상태입니다.",
            FAILED: "처리가 실패해 원인 확인이 필요한 상태입니다.",
            SUCCESS: "처리가 정상 완료된 상태입니다."
        };
        return descriptions[status] || "상태: " + (status || "-");
    }

    function setScenarioStatus(text, cls) {
        const target = document.getElementById("scenarioStatus");
        target.className = "status " + cls;
        target.textContent = text;
    }

    function setStatus(text, cls) {
        const target = document.getElementById("flowStatus");
        target.className = "status " + cls;
        target.textContent = text;
    }

    function setMessage(message) {
        document.getElementById("messageBox").textContent = message;
    }

    function setCancelButtonsEnabled(enabled) {
        document.getElementById("cancelPaymentBtn").disabled = !enabled;
        document.getElementById("cancelUnknownBtn").disabled = !enabled;
    }

    async function postJson(url, payload) {
        const request = fetch(url, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload || {})
        });
        const response = window.AppLoading
            ? await window.AppLoading.track(request, "요청 처리 중입니다")
            : await request;
        return parseResponse(response);
    }

    async function getJson(url) {
        const request = fetch(url);
        const response = window.AppLoading
            ? await window.AppLoading.track(request, "데이터 조회 중입니다")
            : await request;
        return parseResponse(response);
    }

    async function parseResponse(response) {
        const text = await response.text();
        const data = text ? JSON.parse(text) : {};
        if (!response.ok) {
            const requestId = data.requestId ? " (requestId: " + data.requestId + ")" : "";
            const fields = Array.isArray(data.fieldErrors) && data.fieldErrors.length > 0
                ? " / " + data.fieldErrors.map(function (field) {
                    return field.field + ": " + field.message;
                }).join(", ")
                : "";
            throw new Error((data.message || "요청 처리에 실패했습니다.") + fields + requestId);
        }
        return data;
    }

    function formatMoney(value) {
        return Number(value || 0).toLocaleString("ko-KR") + "원";
    }
})();
