package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentApproveRequest;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentApproveResponse;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentBridgeCancelRequest;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentBridgeCancelResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.ScenarioRunResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.ScenarioTimelineStep;
import com.yeni.backoffice.core.payment.entity.AlimtalkQueue;
import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;
import com.yeni.backoffice.core.payment.entity.PaymentRecoveryTask;
import com.yeni.backoffice.core.payment.entity.PaymentTransaction;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import com.yeni.backoffice.core.payment.enums.PaymentStatus;
import com.yeni.backoffice.core.payment.enums.PgProvider;
import com.yeni.backoffice.core.payment.enums.RecoveryStatus;
import com.yeni.backoffice.core.payment.enums.RecoveryType;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import com.yeni.backoffice.core.payment.repository.PaymentTransactionRepository;
import com.yeni.backoffice.core.payment.repository.SalesTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PaymentScenarioService {

    private static final BigDecimal DEFAULT_AMOUNT = BigDecimal.valueOf(12_000);
    private static final BigDecimal PARTIAL_CANCEL_AMOUNT = BigDecimal.valueOf(3_000);

    private final PaymentApproveService paymentApproveService;
    private final PaymentCancelService paymentCancelService;
    private final ExternalSendService externalSendService;
    private final PaymentTransactionRepository paymentRepository;
    private final SalesTransactionRepository salesRepository;
    private final ExternalSendRequestRepository externalSendRequestRepository;
    private final AlimtalkQueueRepository alimtalkQueueRepository;
    private final PaymentRecoveryTaskRepository recoveryTaskRepository;

    public PaymentScenarioService(
            PaymentApproveService paymentApproveService,
            PaymentCancelService paymentCancelService,
            ExternalSendService externalSendService,
            PaymentTransactionRepository paymentRepository,
            SalesTransactionRepository salesRepository,
            ExternalSendRequestRepository externalSendRequestRepository,
            AlimtalkQueueRepository alimtalkQueueRepository,
            PaymentRecoveryTaskRepository recoveryTaskRepository) {
        this.paymentApproveService = paymentApproveService;
        this.paymentCancelService = paymentCancelService;
        this.externalSendService = externalSendService;
        this.paymentRepository = paymentRepository;
        this.salesRepository = salesRepository;
        this.externalSendRequestRepository = externalSendRequestRepository;
        this.alimtalkQueueRepository = alimtalkQueueRepository;
        this.recoveryTaskRepository = recoveryTaskRepository;
    }

    @Transactional
    public ScenarioRunResponse run(String scenarioType) {
        String type = scenarioType == null ? "normal-approve" : scenarioType;
        return switch (type) {
            case "partial-cancel" -> partialCancel();
            case "duplicate-approve" -> duplicateApprove();
            case "over-cancel" -> overCancel();
            case "approve-timeout" -> approveTimeout();
            case "cancel-timeout" -> cancelTimeout();
            case "network-cancel-required" -> networkCancelRequired();
            case "external-send-fail" -> externalSendFail();
            case "alimtalk-fail" -> alimtalkFail();
            case "settlement-preview" -> settlementPreview();
            default -> normalApprove();
        };
    }

    private ScenarioRunResponse normalApprove() {
        List<ScenarioTimelineStep> steps = new ArrayList<>();
        PaymentApproveResponse approved = approve(newOrderNo("정상승인"));
        add(steps, "모의 PG 승인 성공", "SUCCESS", "모의 PG가 결제 승인 성공 응답을 반환했습니다.", approved.tid());
        add(steps, "결제 거래 저장", "SUCCESS", "결제 상태가 결제완료로 저장되었습니다.", approved.paymentId());
        add(steps, "승인 매출 생성", "SUCCESS", "승인 금액 기준으로 매출 원장에 SALE 거래를 생성했습니다.", latestSalesId(approved.orderNo()));
        add(steps, "외부전송 대기 생성", "READY", "회계/운영 시스템 전송을 위한 대기 건을 생성했습니다.", "매출-" + approved.orderNo());
        add(steps, "알림톡 발송 대기 생성", "READY", "고객 안내 알림톡 발송 대기 건을 생성했습니다.", "매출-" + approved.orderNo());
        return response("정상 결제 승인", approved.orderNo(), approved.paymentId(), approved.paymentStatus(), "정상 승인 시나리오가 완료되었습니다.", steps);
    }

    private ScenarioRunResponse partialCancel() {
        List<ScenarioTimelineStep> steps = new ArrayList<>();
        PaymentApproveResponse approved = approve(newOrderNo("부분취소"));
        PaymentBridgeCancelResponse canceled = paymentCancelService.cancelPaymentBridge(approved.paymentId(),
                cancelRequest(PARTIAL_CANCEL_AMOUNT, "취소-" + approved.orderNo()));
        add(steps, "취소 가능 금액 검증", "SUCCESS", "누적 취소 금액과 요청 금액이 승인 금액을 넘지 않는지 확인했습니다.", PARTIAL_CANCEL_AMOUNT);
        add(steps, "모의 PG 부분취소 성공", "SUCCESS", "모의 PG가 취소 성공 응답을 반환했습니다.", canceled.cancelId());
        add(steps, "결제 상태 변경", "SUCCESS", "결제 상태를 부분취소 또는 취소완료로 갱신했습니다.", canceled.paymentStatus());
        add(steps, "취소 매출 생성", "SUCCESS", "취소 금액을 음수 성격의 CANCEL 매출로 별도 기록했습니다.", latestSalesId(approved.orderNo()));
        add(steps, "후속 처리 대기 생성", "READY", "외부전송과 알림톡 대기 건을 결제 저장과 분리해 생성했습니다.", "취소-" + approved.orderNo());
        return response("3,000원 부분취소", approved.orderNo(), approved.paymentId(), canceled.paymentStatus(), "부분취소 시나리오가 완료되었습니다.", steps);
    }

    private ScenarioRunResponse duplicateApprove() {
        List<ScenarioTimelineStep> steps = new ArrayList<>();
        String orderNo = newOrderNo("중복승인");
        PaymentApproveRequest request = approveRequest(orderNo);
        PaymentApproveResponse first = paymentApproveService.approvePayment(request);
        PaymentApproveResponse second = paymentApproveService.approvePayment(request);
        add(steps, "첫 번째 승인 저장", "SUCCESS", "첫 요청에서 결제 거래와 매출 원장을 생성했습니다.", first.paymentId());
        add(steps, "중복 요청 감지", "SUCCESS", "같은 중복방지키로 들어온 두 번째 요청은 기존 승인 결과를 반환했습니다.", second.paymentId());
        add(steps, "중복 생성 방지", "SUCCESS", "결제, 매출, 외부전송, 알림톡 데이터가 중복 생성되지 않았습니다.", orderNo);
        return response("중복 승인 요청", orderNo, first.paymentId(), second.paymentStatus(), "기존 승인 결과를 재사용했습니다.", steps);
    }

    private ScenarioRunResponse overCancel() {
        List<ScenarioTimelineStep> steps = new ArrayList<>();
        PaymentApproveResponse approved = approve(newOrderNo("초과취소"));
        try {
            paymentCancelService.cancelPaymentBridge(approved.paymentId(),
                    cancelRequest(DEFAULT_AMOUNT.add(BigDecimal.ONE), "취소초과-" + approved.orderNo()));
        } catch (IllegalArgumentException e) {
            add(steps, "취소 가능 금액 검증", "FAILED", "요청 취소 금액이 남은 취소 가능 금액보다 커서 거절했습니다.", e.getMessage());
            add(steps, "PG 취소 호출 차단", "SUCCESS", "금액 검증 실패로 모의 PG 취소 API를 호출하지 않았습니다.", approved.paymentId());
            add(steps, "취소 매출 미생성", "SUCCESS", "실패한 취소 요청은 매출 원장에 반영하지 않았습니다.", approved.orderNo());
            return response("취소금액 초과 요청", approved.orderNo(), approved.paymentId(), "REJECTED", "취소 가능 금액 초과 요청을 차단했습니다.", steps);
        }
        return response("취소금액 초과 요청", approved.orderNo(), approved.paymentId(), "UNEXPECTED", "초과 취소가 차단되지 않았습니다.", steps);
    }

    private ScenarioRunResponse approveTimeout() {
        List<ScenarioTimelineStep> steps = new ArrayList<>();
        PaymentApproveResponse approved = approve(newOrderNo("승인결과불명"));
        add(steps, "PG 승인 응답 지연", "UNKNOWN", "모의 PG가 승인 결과불명 응답을 반환했습니다.", approved.resultCode());
        add(steps, "결과불명 상태 저장", "READY", "실패로 단정하지 않고 승인 결과불명 상태로 저장했습니다.", approved.paymentId());
        add(steps, "복구 작업 생성", "READY", "PG 승인 결과를 재조회할 복구 작업을 생성했습니다.", "승인결과확인-" + approved.orderNo());
        return response("PG 승인 결과불명", approved.orderNo(), approved.paymentId(), approved.paymentStatus(), "승인 결과불명 복구 대상이 생성되었습니다.", steps);
    }

    private ScenarioRunResponse cancelTimeout() {
        List<ScenarioTimelineStep> steps = new ArrayList<>();
        PaymentApproveResponse approved = approve(newOrderNo("취소결과불명"));
        PaymentBridgeCancelResponse canceled = paymentCancelService.cancelPaymentBridge(approved.paymentId(),
                cancelRequest(PARTIAL_CANCEL_AMOUNT, "취소결과확인-" + approved.orderNo()));
        add(steps, "PG 취소 응답 지연", "UNKNOWN", "모의 PG가 취소 결과불명 응답을 반환했습니다.", canceled.resultCode());
        add(steps, "취소 결과불명 저장", "READY", "취소 실패로 단정하지 않고 취소 결과불명 상태로 저장했습니다.", approved.paymentId());
        add(steps, "복구 작업 생성", "READY", "PG 취소 결과를 재조회할 복구 작업을 생성했습니다.", "취소결과확인-" + approved.orderNo());
        return response("PG 취소 결과불명", approved.orderNo(), approved.paymentId(), canceled.paymentStatus(), "취소 결과불명 복구 대상이 생성되었습니다.", steps);
    }

    private ScenarioRunResponse networkCancelRequired() {
        List<ScenarioTimelineStep> steps = new ArrayList<>();
        PaymentApproveResponse approved = approve(newOrderNo("망취소필요"));
        PaymentTransaction payment = paymentRepository.findById(approved.paymentId()).orElseThrow();
        payment.updateStatus(PaymentStatus.NETWORK_CANCEL_REQUIRED, "승인 후 내부 저장 실패 상황을 재현했습니다.");
        recoveryTask("망취소-" + payment.getOrderNo(), payment.getId(), null, RecoveryType.NETWORK_CANCEL, "망취소 재시도가 필요합니다.");
        add(steps, "PG 승인 성공", "SUCCESS", "PG 승인은 성공했지만 내부 후처리 실패 상황을 재현했습니다.", approved.tid());
        add(steps, "망취소 필요 저장", "READY", "결제 상태를 망취소 필요로 전환했습니다.", payment.getId());
        add(steps, "복구 작업 생성", "READY", "망취소 작업을 생성했습니다.", "망취소-" + payment.getOrderNo());
        return response("망취소 필요 상황", payment.getOrderNo(), payment.getId(), payment.getPaymentStatus().name(), "망취소 운영 처리가 필요한 상태를 만들었습니다.", steps);
    }

    private ScenarioRunResponse externalSendFail() {
        List<ScenarioTimelineStep> steps = new ArrayList<>();
        PaymentApproveResponse approved = approve(newOrderNo("외부전송실패"));
        SalesTransaction sales = latestSales(approved.orderNo());
        ExternalSendRequest request = externalSendRequestRepository.save(ExternalSendRequest.builder()
                .salesId(sales.getId())
                .requestKey("외부전송실패-" + approved.orderNo())
                .targetSystem("매출운영시스템")
                .sendStatus(ExternalSendStatus.READY)
                .retryCount(0)
                .build());
        externalSendService.send(request.getId());
        recoveryTask("외부전송재시도-" + request.getId(), approved.paymentId(), null, RecoveryType.EXTERNAL_SEND_RETRY, "외부전송 실패 재시도가 필요합니다.");
        add(steps, "결제/매출 저장 유지", "SUCCESS", "외부전송 실패와 무관하게 결제와 매출 원장은 정상 저장되어 있습니다.", approved.paymentId());
        add(steps, "외부전송 실패 기록", "FAILED", "전송 대기 건에 실패 이력과 재시도 횟수를 기록했습니다.", request.getId());
        add(steps, "재시도 대상 생성", "READY", "외부전송 재시도 복구 작업을 생성했습니다.", "외부전송재시도-" + request.getId());
        return response("외부전송 실패", approved.orderNo(), approved.paymentId(), "EXTERNAL_SEND_FAILED", "외부전송 실패가 결제 저장을 되돌리지 않음을 보여줍니다.", steps);
    }

    private ScenarioRunResponse alimtalkFail() {
        List<ScenarioTimelineStep> steps = new ArrayList<>();
        PaymentApproveResponse approved = approve(newOrderNo("알림톡실패"));
        AlimtalkQueue queue = alimtalkQueueRepository.findByMessageKey("SALE-" + approved.orderNo()).orElseThrow();
        queue.markFailed("포트폴리오용 알림톡 발송 실패 재현");
        queue.markRetryReady();
        recoveryTask("알림톡재시도-" + queue.getId(), approved.paymentId(), null, RecoveryType.ALIMTALK_RETRY, "알림톡 발송 재시도가 필요합니다.");
        add(steps, "결제/매출 저장 유지", "SUCCESS", "알림톡 실패와 무관하게 결제와 매출 원장은 정상 저장되어 있습니다.", approved.paymentId());
        add(steps, "알림톡 발송 실패", "FAILED", "알림톡 대기 건을 재시도 대기 상태로 전환했습니다.", queue.getId());
        add(steps, "재발송 대상 생성", "READY", "알림톡 재발송 복구 작업을 생성했습니다.", "알림톡재시도-" + queue.getId());
        return response("알림톡 발송 실패", approved.orderNo(), approved.paymentId(), "ALIMTALK_RETRY_READY", "알림톡 실패 재처리 대상이 생성되었습니다.", steps);
    }

    private ScenarioRunResponse settlementPreview() {
        List<ScenarioTimelineStep> steps = new ArrayList<>();
        PaymentApproveResponse approved = approve(newOrderNo("정산대상"));
        add(steps, "매출 원장 준비", "READY", "정산 배치가 읽을 승인 매출을 생성했습니다.", latestSalesId(approved.orderNo()));
        add(steps, "수수료 정책 적용 대상", "READY", "PG 수수료 정책을 기준으로 정산 금액을 계산할 수 있습니다.", approved.paymentId());
        add(steps, "정산 배치 실행 가능", "READY", "정산 관리 화면의 배치 버튼으로 일별 정산 명세를 생성할 수 있습니다.", approved.orderNo());
        return response("정산 배치 미리보기", approved.orderNo(), approved.paymentId(), approved.paymentStatus(), "정산 대상 매출을 준비했습니다.", steps);
    }

    private PaymentApproveResponse approve(String orderNo) {
        return paymentApproveService.approvePayment(approveRequest(orderNo));
    }

    private PaymentApproveRequest approveRequest(String orderNo) {
        return new PaymentApproveRequest(PgProvider.MOCK, orderNo, DEFAULT_AMOUNT, "KRW", "포트폴리오 고객",
                "백오피스 결제 테스트 상품", "승인-" + orderNo, "웹", "포트폴리오몰", "카드");
    }

    private PaymentBridgeCancelRequest cancelRequest(BigDecimal amount, String key) {
        return new PaymentBridgeCancelRequest(PgProvider.MOCK, amount, "포트폴리오 시나리오 취소", key);
    }

    private String newOrderNo(String scenario) {
        return "주문-" + scenario + "-" + System.currentTimeMillis();
    }

    private SalesTransaction latestSales(String orderNo) {
        return salesRepository.findAll().stream()
                .filter(sales -> orderNo.equals(sales.getOrderNo()))
                .max(Comparator.comparing(SalesTransaction::getId))
                .orElseThrow();
    }

    private String latestSalesId(String orderNo) {
        return String.valueOf(latestSales(orderNo).getId());
    }

    private PaymentRecoveryTask recoveryTask(String key, Long paymentId, Long cancelId, RecoveryType type, String message) {
        return recoveryTaskRepository.findByTaskKey(key)
                .orElseGet(() -> recoveryTaskRepository.save(PaymentRecoveryTask.builder()
                        .taskKey(key)
                        .paymentId(paymentId)
                        .cancelId(cancelId)
                        .recoveryType(type)
                        .status(RecoveryStatus.READY)
                        .retryCount(0)
                        .maxRetryCount(5)
                        .lastErrorMessage(message)
                        .build()));
    }

    private void add(List<ScenarioTimelineStep> steps, String name, String status, String description, Object referenceId) {
        steps.add(new ScenarioTimelineStep(name, status, description, referenceId == null ? null : String.valueOf(referenceId), LocalDateTime.now()));
    }

    private ScenarioRunResponse response(String scenarioName, String orderNo, Long paymentId, String status, String message, List<ScenarioTimelineStep> steps) {
        return new ScenarioRunResponse(scenarioName, orderNo, paymentId, status, message, steps);
    }
}
