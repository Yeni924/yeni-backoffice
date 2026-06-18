(function () {
    let currentPaymentId = null;
    let currentPaymentFilter = "ALL";
    let paymentRows = [];
    let externalRows = [];
    let alimtalkRows = [];
    let recoveryRows = [];
    const tables = {};

    document.addEventListener("DOMContentLoaded", function () {
        const orderNo = document.getElementById("orderNo");
        if (orderNo) {
            orderNo.value = "ORDER-MANUAL-" + Date.now();
        }

        bindTabs();
        initTables();
        bindPaymentTraceModal();
        bindQuickFilters();
        bind("runPaymentBtn", runPaymentFlow);
        bind("cancelPaymentBtn", function () {
            return cancelPaymentWithKey("CANCEL-" + Date.now());
        });
        bind("refreshBtn", refreshAll);
        bind("runExternalWorkerBtn", function () {
            return runWorker("/api/admin/external-send/worker/run", "외부전송");
        });
        bind("runAlimtalkWorkerBtn", function () {
            return runWorker("/api/admin/alimtalk/worker/run", "알림톡");
        });
        bind("approveUnknownBtn", runApproveUnknown);
        bind("cancelUnknownBtn", function () {
            return cancelPaymentWithKey("CANCEL-UNKNOWN-" + Date.now());
        });

        document.querySelectorAll("[data-scenario]").forEach(function (button) {
            button.addEventListener("click", function () {
                runScenario(button.dataset.scenario, button);
            });
        });

        document.addEventListener("keydown", function (event) {
            const modal = document.getElementById("paymentTraceModal");
            if (event.key === "Escape" && modal && !modal.hidden) {
                closePaymentTrace();
            }
        });

        activateHashTab();
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
                activateTab(button.dataset.tabTarget);
                history.replaceState(null, "", "#" + button.dataset.tabTarget);
            });
        });
    }

    function activateHashTab() {
        const target = window.location.hash ? window.location.hash.slice(1) : "";
        if (target && document.querySelector("[data-tab-target='" + target + "']")) {
            activateTab(target);
        }
    }

    function bindQuickFilters() {
        document.querySelectorAll("[data-ops-filter]").forEach(function (button) {
            button.addEventListener("click", function () {
                applyQuickFilter(button.dataset.opsFilter);
            });
        });
    }

    function initTables() {
        tables.payments = createTable("#paymentTable", {
            paginationSize: 10,
            placeholder: "조회된 결제 거래가 없습니다.",
            columns: [
                actionColumn("추적", "보기", function (row) { openPaymentTrace(row.id); }),
                {title: "ID", field: "id", width: 80, hozAlign: "center"},
                {title: "주문번호", field: "orderNo", minWidth: 190},
                {title: "PG 거래번호", field: "tid", minWidth: 180, formatter: emptyFormatter},
                {title: "승인금액", field: "approvedAmount", hozAlign: "right", formatter: moneyFormatter},
                {title: "취소금액", field: "canceledAmount", hozAlign: "right", formatter: moneyFormatter},
                {title: "상태", field: "paymentStatus", width: 160, formatter: statusFormatter, headerFilter: "select", headerFilterParams: {values: paymentStatusFilterValues()}},
                {title: "승인일시", field: "approvedAt", minWidth: 170, formatter: dateTimeFormatter}
            ],
            rowClick: function (event, row) {
                openPaymentTrace(row.getData().id);
            }
        });

        tables.external = createTable("#externalTable", {
            paginationSize: 8,
            placeholder: "조회된 외부전송 대상이 없습니다.",
            columns: [
                {title: "ID", field: "id", width: 80, hozAlign: "center"},
                {title: "매출 ID", field: "salesId", width: 100, hozAlign: "center"},
                {title: "요청키", field: "requestKey", minWidth: 190},
                {title: "대상 시스템", field: "targetSystem", minWidth: 160, formatter: targetSystemFormatter},
                {title: "상태", field: "sendStatus", width: 130, formatter: statusFormatter, headerFilter: "select", headerFilterParams: {values: followupStatusFilterValues()}},
                {title: "재시도", field: "retryCount", width: 90, hozAlign: "center"},
                {title: "최근 오류", field: "lastErrorMessage", minWidth: 180, formatter: emptyFormatter}
            ]
        });

        tables.alimtalk = createTable("#alimtalkTable", {
            paginationSize: 8,
            placeholder: "조회된 알림톡 발송 대상이 없습니다.",
            columns: [
                {title: "ID", field: "id", width: 80, hozAlign: "center"},
                {title: "결제 ID", field: "paymentId", width: 100, hozAlign: "center"},
                {title: "메시지키", field: "messageKey", minWidth: 190},
                {title: "이벤트", field: "eventType", width: 120, formatter: eventTypeFormatter},
                {title: "상태", field: "status", width: 130, formatter: statusFormatter, headerFilter: "select", headerFilterParams: {values: followupStatusFilterValues()}},
                {title: "재시도", field: "retryCount", width: 90, hozAlign: "center"},
                {title: "최근 오류", field: "lastErrorMessage", minWidth: 180, formatter: emptyFormatter}
            ]
        });

        tables.recovery = createTable("#recoveryTable", {
            paginationSize: 8,
            placeholder: "조회된 복구 작업이 없습니다.",
            columns: [
                {title: "ID", field: "id", width: 70, hozAlign: "center"},
                {title: "작업키", field: "taskKey", minWidth: 190},
                {title: "결제 ID", field: "paymentId", width: 95, hozAlign: "center", formatter: emptyFormatter},
                {title: "주문번호", field: "orderNo", minWidth: 170, formatter: emptyFormatter},
                {title: "PG 거래번호", field: "tid", minWidth: 170, formatter: emptyFormatter},
                {title: "멱등키", field: "idempotencyKey", minWidth: 150, formatter: emptyFormatter},
                {title: "복구 유형", field: "recoveryType", minWidth: 180, formatter: recoveryTypeFormatter},
                {title: "상태", field: "status", width: 120, formatter: statusFormatter, headerFilter: "select", headerFilterParams: {values: recoveryStatusFilterValues()}},
                {title: "재시도", field: "retryCount", width: 80, hozAlign: "center"},
                {title: "최근 오류", field: "lastErrorMessage", minWidth: 200, formatter: emptyFormatter},
                {title: "운영 처리", width: 230, hozAlign: "center", formatter: recoveryActionFormatter, cellClick: recoveryActionClick}
            ]
        });
    }

    function createTable(selector, options) {
        const table = new Tabulator(selector, {
            layout: "fitColumns",
            pagination: "local",
            paginationSize: options.paginationSize,
            paginationSizeSelector: [8, 10, 20, 50],
            placeholder: options.placeholder,
            columns: options.columns
        });
        if (options.rowClick) {
            table.on("rowClick", options.rowClick);
        }
        return table;
    }

    function actionColumn(title, label, handler) {
        return {
            title: title,
            field: "id",
            width: 76,
            hozAlign: "center",
            formatter: function () {
                return "<button class=\"table-action\" type=\"button\">" + label + "</button>";
            },
            cellClick: function (event, cell) {
                event.stopPropagation();
                handler(cell.getRow().getData());
            }
        };
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
                : "결제 완료: " + (approved.tid || "-"));
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
            setMessage(result.message || "시나리오가 완료되었습니다.");
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

        setText("paymentCount", summary.approvedPaymentCount);
        setText("approvedAmount", formatMoney(summary.approvedAmount));
        setText("salesAmount", (summary.salesTransactionCount || 0) + "건 / " + formatMoney(summary.salesAmount));
        setText("failedExternalSendCount", (summary.readyExternalSendCount || 0) + " / " + (summary.failedExternalSendCount || 0));
        setText("recoveryTaskCount", summary.recoveryTaskCount);
        setText("alimtalkQueueCount", (summary.readyAlimtalkCount || 0) + " / " + (summary.failedAlimtalkCount || 0));

        const totalRows = Number(summary.approvedPaymentCount || 0)
            + Number(summary.salesTransactionCount || 0)
            + Number(summary.readyExternalSendCount || 0)
            + Number(summary.failedExternalSendCount || 0)
            + Number(summary.recoveryTaskCount || 0)
            + Number(summary.readyAlimtalkCount || 0)
            + Number(summary.failedAlimtalkCount || 0);
        const emptyGuide = document.getElementById("emptyGuide");
        if (emptyGuide) {
            emptyGuide.hidden = totalRows > 0;
        }

        paymentRows = payments || [];
        externalRows = externalRequests || [];
        alimtalkRows = alimtalkQueues || [];
        recoveryRows = recoveryTasks.data || [];

        renderPayments();
        tables.external.setData(externalRows);
        tables.alimtalk.setData(alimtalkRows);
        tables.recovery.setData(recoveryRows);
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value == null ? 0 : value;
        }
    }

    function applyQuickFilter(filter) {
        currentPaymentFilter = filter || "ALL";
        document.querySelectorAll("[data-ops-filter]").forEach(function (button) {
            button.classList.toggle("active", button.dataset.opsFilter === currentPaymentFilter);
        });

        clearFollowupFilters();
        if (currentPaymentFilter === "FOLLOWUP_FAILED") {
            activateTab("followups");
            tables.external.setFilter("sendStatus", "=", "FAILED");
            tables.alimtalk.setFilter("status", "=", "FAILED");
            tables.recovery.setFilter("status", "=", "FAILED");
            setMessage("후속처리 실패 건만 표시했습니다. 외부전송, 알림톡, 복구 작업 상태를 확인해 주세요.");
            return;
        }
        if (currentPaymentFilter === "SETTLEMENT_READY") {
            window.location.href = "/admin/payment-operations/sales-ledger?settlementStatus=NOT_SETTLED";
            return;
        }
        renderPayments();
    }

    function renderPayments() {
        const filtered = paymentRows.filter(function (payment) {
            if (currentPaymentFilter === "UNKNOWN") {
                return payment.paymentStatus === "APPROVE_UNKNOWN" || payment.paymentStatus === "CANCEL_UNKNOWN";
            }
            if (currentPaymentFilter === "NEEDS_ACTION") {
                return ["APPROVE_UNKNOWN", "CANCEL_UNKNOWN", "NETWORK_CANCEL_REQUIRED", "APPROVE_FAILED", "CANCEL_FAILED"].includes(payment.paymentStatus);
            }
            return true;
        });
        tables.payments.setData(filtered);
    }

    function clearFollowupFilters() {
        if (tables.external) tables.external.clearFilter(true);
        if (tables.alimtalk) tables.alimtalk.clearFilter(true);
        if (tables.recovery) tables.recovery.clearFilter(true);
    }

    function activateTab(target) {
        document.querySelectorAll("[data-tab-target]").forEach(function (tab) {
            tab.classList.toggle("active", tab.dataset.tabTarget === target);
        });
        document.querySelectorAll("[data-tab-panel]").forEach(function (panel) {
            panel.classList.toggle("active", panel.dataset.tabPanel === target);
        });
        setTimeout(function () {
            Object.values(tables).forEach(function (table) {
                if (table) table.redraw(true);
            });
        }, 0);
    }

    async function runWorker(url, label) {
        try {
            const result = await postJson(url, {limit: 20});
            setMessage(label + " 처리를 완료했습니다. 대상 " + result.targetCount
                + "건 / 처리 " + result.claimedCount
                + "건 / 성공 " + result.successCount
                + "건 / 실패 " + result.failureCount
                + "건 / 건너뜀 " + result.skippedCount + "건");
            await refreshAll();
        } catch (error) {
            setMessage(error.message);
        }
    }

    function bindPaymentTraceModal() {
        bind("closePaymentTraceBtn", closePaymentTrace);
        const modal = document.getElementById("paymentTraceModal");
        if (!modal) return;
        modal.addEventListener("click", function (event) {
            if (event.target === modal) {
                closePaymentTrace();
            }
            handleTraceActionClick(event);
        });
    }

    async function openPaymentTrace(paymentId) {
        try {
            const trace = await getJson("/api/payment-bridge/payments/" + paymentId + "/trace");
            document.getElementById("paymentTraceTitle").textContent = "결제 #" + paymentId + " 통합 추적";
            document.getElementById("paymentTraceBody").innerHTML = renderPaymentTrace(trace);
            document.getElementById("paymentTraceModal").hidden = false;
        } catch (error) {
            setMessage(error.message);
        }
    }

    function closePaymentTrace() {
        document.getElementById("paymentTraceModal").hidden = true;
    }

    function renderPaymentTrace(trace) {
        const payment = trace.payment || {};
        const sales = trace.sales || [];
        return [
            "<article class=\"trace-sheet\">",
            "<header class=\"trace-summary\">",
            "<div><span class=\"receipt-kicker " + statusClass(payment.paymentStatus) + "\">" + statusLabel(payment.paymentStatus) + "</span>",
            "<h4>" + escapeHtml(payment.orderNo || "-") + "</h4>",
            "<p>PG 거래번호 " + escapeHtml(payment.tid || "-") + " / MID " + escapeHtml(payment.mid || "-") + "</p></div>",
            "<div class=\"trace-amount\"><span>승인금액</span><strong>" + formatMoney(payment.approvedAmount) + "</strong><small>취소 " + formatMoney(payment.canceledAmount) + "</small></div>",
            "</header>",
            traceActions(trace),
            traceSection("매출 원장", traceTable(["유형", "금액", "정산상태", "발생일시"], sales.map(function (item) {
                return [saleTypeLabel(item.saleType), formatMoney(item.totalAmount), statusLabel(item.settlementStatus), formatDateTime(item.occurredAt)];
            }), "연결된 매출 원장이 없습니다.")),
            traceSection("취소 이력", traceTable(["취소금액", "상태", "유형", "일시"], (trace.cancels || []).map(function (item) {
                return [formatMoney(item.cancelAmount), statusLabel(item.cancelStatus), cancelTypeLabel(item.cancelType), formatDateTime(item.canceledAt)];
            }), "취소 이력이 없습니다.")),
            traceSection("후속처리", renderFollowups(trace)),
            traceSection("처리 타임라인", traceTimeline(trace)),
            "</article>"
        ].join("");
    }

    function traceActions(trace) {
        const payment = trace.payment || {};
        const buttons = [];
        if (payment.paymentStatus === "APPROVE_UNKNOWN") {
            buttons.push(traceActionButton("retry-query", payment.id, "PG 결과 재조회", "승인 결과를 다시 조회하고 확정 시 SALE 원장과 후속처리를 생성합니다."));
        }
        if ((trace.externalSends || []).some(function (item) { return item.sendStatus === "FAILED"; })) {
            buttons.push(traceActionButton("external-worker", "", "외부전송 재처리", "실패한 외부전송 Queue를 Worker로 다시 처리합니다."));
        }
        if ((trace.alimtalkQueues || []).some(function (item) { return item.status === "FAILED"; })) {
            buttons.push(traceActionButton("alimtalk-worker", "", "알림톡 재처리", "실패한 알림톡 Queue를 Worker로 다시 처리합니다."));
        }
        (trace.recoveryTasks || [])
            .filter(function (task) { return task.status !== "SUCCESS"; })
            .slice(0, 3)
            .forEach(function (task) {
                buttons.push(traceActionButton("recovery-retry", task.id, "RecoveryTask #" + task.id + " 재시도", recoveryTypeLabel(task.recoveryType)));
            });
        if (!buttons.length) {
            return "";
        }
        return "<section class=\"trace-action-panel\"><h4>다음 조치</h4><div class=\"trace-action-grid\">" + buttons.join("") + "</div></section>";
    }

    function traceActionButton(action, id, label, description) {
        return "<button type=\"button\" data-trace-action=\"" + action + "\" data-trace-id=\"" + escapeHtml(id || "") + "\">"
            + "<strong>" + escapeHtml(label) + "</strong>"
            + "<span>" + escapeHtml(description || "") + "</span>"
            + "</button>";
    }

    async function handleTraceActionClick(event) {
        const button = event.target.closest("[data-trace-action]");
        if (!button) {
            return;
        }
        const action = button.dataset.traceAction;
        const id = button.dataset.traceId;
        const originalHtml = button.innerHTML;
        button.disabled = true;
        button.innerHTML = "<strong>처리 중</strong><span>요청을 반영하고 있습니다.</span>";
        try {
            if (action === "retry-query") {
                await postJson("/api/payment-bridge/payments/" + id + "/retry-query", {});
                setMessage("PG 결과 재조회를 완료했습니다.");
                await refreshAll();
                await openPaymentTrace(id);
            } else if (action === "external-worker") {
                await runWorker("/api/admin/external-send/worker/run", "외부전송");
                closePaymentTrace();
            } else if (action === "alimtalk-worker") {
                await runWorker("/api/admin/alimtalk/worker/run", "알림톡");
                closePaymentTrace();
            } else if (action === "recovery-retry") {
                await postJson("/admin/api/recovery/tasks/" + id + "/retry", {});
                setMessage("RecoveryTask #" + id + " 재시도를 요청했습니다.");
                await refreshAll();
                closePaymentTrace();
            }
        } catch (error) {
            setMessage(error.message);
            button.disabled = false;
            button.innerHTML = originalHtml;
        }
    }

    function traceTimeline(trace) {
        const payment = trace.payment || {};
        const events = [{
            at: payment.approvedAt,
            label: "결제",
            status: payment.paymentStatus,
            description: "승인 " + formatMoney(payment.approvedAmount)
        }];
        (trace.cancels || []).forEach(function (item) {
            events.push({at: item.canceledAt, label: "취소", status: item.cancelStatus, description: formatMoney(item.cancelAmount)});
        });
        (trace.externalSends || []).forEach(function (item) {
            events.push({at: item.lastSentAt || item.processingStartedAt, label: "외부전송", status: item.sendStatus, description: item.lastErrorMessage || item.targetSystem});
        });
        (trace.alimtalkQueues || []).forEach(function (item) {
            events.push({at: item.sentAt || item.processingStartedAt, label: "알림톡", status: item.status, description: item.lastErrorMessage || item.eventType});
        });
        events.sort(function (a, b) { return String(a.at || "").localeCompare(String(b.at || "")); });
        return "<ol class=\"trace-timeline\">" + events.map(function (item) {
            return "<li><time>" + formatDateTime(item.at) + "</time><div><strong>" + item.label + " · " + statusLabel(item.status) + "</strong><span>" + escapeHtml(item.description || "-") + "</span></div></li>";
        }).join("") + "</ol>";
    }

    function renderFollowups(trace) {
        const rows = [];
        (trace.externalSends || []).forEach(function (item) {
            rows.push(["외부전송 #" + item.id, statusLabel(item.sendStatus), item.retryCount + "회", item.lastErrorMessage || "-"]);
        });
        (trace.alimtalkQueues || []).forEach(function (item) {
            rows.push(["알림톡 #" + item.id, statusLabel(item.status), item.retryCount + "회", item.lastErrorMessage || "-"]);
        });
        (trace.recoveryTasks || []).forEach(function (item) {
            rows.push(["복구 #" + item.id + " " + recoveryTypeLabel(item.recoveryType), statusLabel(item.status), item.retryCount + "회", item.lastErrorMessage || "-"]);
        });
        return traceTable(["구분", "상태", "재시도", "최근 실패 사유"], rows, "후속 처리 이력이 없습니다.");
    }

    function traceSection(title, content) {
        return "<section class=\"trace-section\"><h4>" + title + "</h4>" + content + "</section>";
    }

    function traceTable(headers, rows, emptyText) {
        if (!rows.length) {
            return "<p class=\"trace-empty\">" + emptyText + "</p>";
        }
        return "<div class=\"receipt-table-wrap\"><table class=\"receipt-table\"><thead><tr>"
            + headers.map(function (header) { return "<th>" + escapeHtml(header) + "</th>"; }).join("")
            + "</tr></thead><tbody>"
            + rows.map(function (row) {
                return "<tr>" + row.map(function (value) { return "<td>" + escapeHtml(value) + "</td>"; }).join("") + "</tr>";
            }).join("")
            + "</tbody></table></div>";
    }

    function renderScenarioResult(result) {
        const statusClass = result.status && result.status.includes("FAILED") ? "cancel"
            : result.status && (result.status.includes("UNKNOWN") || result.status.includes("REQUIRED") || result.status.includes("RETRY")) ? "unknown"
                : "paid";
        setScenarioStatus(statusLabel(result.status), statusClass);
        document.getElementById("scenarioResult").textContent =
            (result.scenarioName || "시나리오") + " / 주문번호 " + (result.orderNo || "-") + " / " + (result.message || "");
        const timeline = document.getElementById("scenarioTimeline");
        timeline.innerHTML = "";
        (result.timelineSteps || []).forEach(function (step) {
            const li = document.createElement("li");
            li.innerHTML = "<strong>" + escapeHtml(step.stepName) + " · " + statusLabel(step.status) + "</strong>"
                + escapeHtml(step.description || "")
                + (step.referenceId ? "<br><span>참조: " + escapeHtml(step.referenceId) + "</span>" : "");
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
        return formatDateTime(cell.getValue());
    }

    function emptyFormatter(cell) {
        const value = cell.getValue();
        return value === null || value === undefined || value === "" ? "-" : value;
    }

    function targetSystemFormatter(cell) {
        const labels = {SALES_OPERATION_MOCK: "매출 운영 시스템"};
        return labels[cell.getValue()] || cell.getValue() || "-";
    }

    function eventTypeFormatter(cell) {
        const labels = {SALE: "결제", CANCEL: "취소", APPROVE: "승인"};
        return labels[cell.getValue()] || cell.getValue() || "-";
    }

    function saleTypeLabel(value) {
        return value === "SALE" ? "SALE 결제매출" : value === "CANCEL" ? "CANCEL 취소매출" : value || "-";
    }

    function recoveryTypeFormatter(cell) {
        return recoveryTypeLabel(cell.getValue());
    }

    function recoveryTypeLabel(value) {
        const labels = {
            APPROVE_UNKNOWN_CHECK: "승인 결과불명 확인",
            CANCEL_UNKNOWN_CHECK: "취소 결과불명 확인",
            NETWORK_CANCEL: "망취소 처리",
            APPROVE_INTERNAL_SAVE_FAILED: "승인 후 내부 저장 실패",
            EXTERNAL_SEND_RETRY: "외부전송 재시도",
            ALIMTALK_RETRY: "알림톡 재시도"
        };
        return labels[value] || value || "-";
    }

    function cancelTypeLabel(value) {
        const labels = {PARTIAL: "부분취소", FULL: "전체취소"};
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
            ? await window.AppLoading.track(request, "요청을 처리하고 있습니다.")
            : await request;
        return parseResponse(response);
    }

    async function getJson(url) {
        const request = fetch(url);
        const response = window.AppLoading
            ? await window.AppLoading.track(request, "운영 데이터를 불러오고 있습니다.")
            : await request;
        return parseResponse(response);
    }

    async function parseResponse(response) {
        const text = await response.text();
        let data = {};
        try {
            data = text ? JSON.parse(text) : {};
        } catch (error) {
            throw new Error("서버 응답을 JSON으로 해석하지 못했습니다. 로그인 리다이렉트나 서버 오류 응답인지 확인해 주세요.");
        }
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

    function formatDateTime(value) {
        return value ? String(value).replace("T", " ").slice(0, 19) : "-";
    }

    function escapeHtml(value) {
        return String(value || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }
})();
