package com.yeni.backoffice.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeni.backoffice.api.database.view.DatabaseSpecDescriptionCatalog;
import com.yeni.backoffice.api.database.view.DatabaseSpecService;
import com.yeni.backoffice.core.common.exception.BusinessException;
import com.yeni.backoffice.core.common.exception.ErrorCode;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentApproveRequest;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentApproveResponse;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentBridgeCancelRequest;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentBridgeCancelResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesLedgerLinksResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesLedgerPageResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementBatchRunRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementStatementResponse;
import com.yeni.backoffice.core.payment.enums.PaymentStatus;
import com.yeni.backoffice.core.payment.enums.PgProvider;
import com.yeni.backoffice.core.payment.enums.RecoveryStatus;
import com.yeni.backoffice.core.payment.enums.RecoveryType;
import com.yeni.backoffice.core.payment.enums.SaleType;
import com.yeni.backoffice.core.payment.enums.LedgerStatus;
import com.yeni.backoffice.core.payment.enums.SalesSettlementStatus;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import com.yeni.backoffice.core.payment.repository.PaymentCancelRepository;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import com.yeni.backoffice.core.payment.repository.PaymentTransactionRepository;
import com.yeni.backoffice.core.payment.repository.SalesTransactionRepository;
import com.yeni.backoffice.core.payment.repository.SettlementStatementRepository;
import com.yeni.backoffice.core.payment.service.PaymentApproveService;
import com.yeni.backoffice.core.payment.service.PaymentCancelService;
import com.yeni.backoffice.core.payment.service.PaymentQueryService;
import com.yeni.backoffice.core.payment.service.PaymentRecoveryOperationService;
import com.yeni.backoffice.core.payment.service.PaymentRecoveryService;
import com.yeni.backoffice.core.payment.service.SalesLedgerService;
import com.yeni.backoffice.core.payment.service.SettlementOperationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:yeni-backoffice-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.h2.console.enabled=false"
})
@AutoConfigureMockMvc
class YeniBackofficeApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PaymentApproveService paymentApproveService;

	@Autowired
	private PaymentCancelService paymentCancelService;

	@Autowired
	private PaymentQueryService paymentQueryService;

	@Autowired
	private PaymentRecoveryService paymentRecoveryService;

	@Autowired
	private PaymentRecoveryOperationService paymentRecoveryOperationService;

	@Autowired
	private SalesLedgerService salesLedgerService;

	@Autowired
	private SettlementOperationService settlementOperationService;

	@Autowired
	private PaymentTransactionRepository paymentRepository;

	@Autowired
	private PaymentCancelRepository cancelRepository;

	@Autowired
	private SalesTransactionRepository salesRepository;

	@Autowired
	private PaymentRecoveryTaskRepository recoveryTaskRepository;

	@Autowired
	private ExternalSendRequestRepository externalSendRequestRepository;

	@Autowired
	private AlimtalkQueueRepository alimtalkQueueRepository;

	@Autowired
	private SettlementStatementRepository settlementStatementRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private DatabaseSpecService databaseSpecService;

	@Autowired
	private DatabaseSpecDescriptionCatalog databaseSpecDescriptionCatalog;

	@Test
	void contextLoads() {
	}

	@Test
	void dashboardPageLoads() throws Exception {
		mockMvc.perform(get("/dashboard"))
				.andExpect(status().isOk());
	}

	@Test
	void faviconLoadsAsStaticResource() throws Exception {
		mockMvc.perform(get("/favicon.ico"))
				.andExpect(status().isOk());
	}

	@Test
	void portfolioAdminNavigationPageLoadsWithoutLogin() throws Exception {
		mockMvc.perform(get("/admin/navigation"))
				.andExpect(status().isOk());
	}

	@Test
	void portfolioAdminApiLoadsWithoutLogin() throws Exception {
		mockMvc.perform(get("/admin/api/settlements"))
				.andExpect(status().isOk());
	}

	@Test
	void databaseSpecPageLoadsWithoutLogin() throws Exception {
		mockMvc.perform(get("/admin/database-spec"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("전체 스키마 자동 명세")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("payment_transaction")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("결제 승인 요청과 PG 승인 결과")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("중복 승인을 방지하는 승인 요청 고유 키")));
	}

	@Test
	void salesLedgerAndSettlementPagesExplainFiltersAndDraftRecalculation() throws Exception {
		mockMvc.perform(get("/admin/payment-operations/sales-ledger"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("빠른 필터")));

		mockMvc.perform(get("/admin/payment-operations/settlements"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("오늘 정산 초안 생성/재계산")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("기존 명세에 누적")));
	}

	@Test
	void databaseSpecDescriptionsCoverEveryTableAndColumn() {
		List<DatabaseSpecService.TableSpec> tableSpecs = databaseSpecService.getTableSpecs();

		assertThat(tableSpecs).isNotEmpty();
		assertThat(tableSpecs).allSatisfy(table -> {
			assertThat(databaseSpecDescriptionCatalog.hasTableDescription(table.tableName()))
					.as("table description: %s", table.tableName())
					.isTrue();
			assertThat(table.columns()).allSatisfy(column ->
					assertThat(databaseSpecDescriptionCatalog.hasColumnDescription(table.tableName(), column.columnName()))
							.as("column description: %s.%s", table.tableName(), column.columnName())
							.isTrue());
		});
	}

	@Test
	void paymentBridgeApproveCancelAndQueryFlow() throws Exception {
		MvcResult approveResult = mockMvc.perform(post("/api/payment-bridge/payments/approve")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "pgProvider": "MOCK",
								  "orderNo": "ORDER-TEST-001",
								  "amount": 12000,
								  "currency": "KRW",
								  "buyerName": "Portfolio Buyer",
								  "productName": "Payment Bridge Mock Item",
								  "idempotencyKey": "APPROVE-ORDER-TEST-001",
								  "channelType": "WEB",
								  "storeCode": "PORTFOLIO",
								  "paymentMethod": "CARD"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode approve = objectMapper.readTree(approveResult.getResponse().getContentAsString());
		Long paymentId = approve.get("paymentId").asLong();

		mockMvc.perform(post("/api/payment-bridge/payments/{paymentId}/cancel", paymentId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "pgProvider": "MOCK",
								  "cancelAmount": 3000,
								  "cancelReason": "partial cancel mock",
								  "idempotencyKey": "CANCEL-TEST-001"
								}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/payment-bridge/payments/{paymentId}/retry-query", paymentId))
				.andExpect(status().isOk());
	}

	@Test
	void invalidApproveRequestReturnsValidationErrorWithRequestIdAndFieldErrors() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/payment-bridge/payments/approve")
						.header("X-Request-Id", "REQ-VALIDATION-TEST")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "pgProvider": "MOCK",
								  "orderNo": "",
								  "amount": 0
								}
								"""))
				.andExpect(status().isBadRequest())
				.andReturn();

		JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(error.get("code").asText()).isEqualTo("VALIDATION_ERROR");
		assertThat(error.get("requestId").asText()).isEqualTo("REQ-VALIDATION-TEST");
		assertThat(error.get("fieldErrors")).isNotEmpty();
		assertThat(result.getResponse().getHeader("X-Request-Id")).isEqualTo("REQ-VALIDATION-TEST");
	}

	@Test
	void cancelMissingPaymentReturnsPaymentNotFoundCode() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/payment-bridge/payments/{paymentId}/cancel", 999999L)
						.header("X-Request-Id", "REQ-NOT-FOUND-TEST")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "pgProvider": "MOCK",
								  "cancelAmount": 1000,
								  "cancelReason": "없는 결제 취소",
								  "idempotencyKey": "CANCEL-NOT-FOUND"
								}
								"""))
				.andExpect(status().isNotFound())
				.andReturn();

		JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(error.get("code").asText()).isEqualTo("PAYMENT_NOT_FOUND");
		assertThat(error.get("requestId").asText()).isEqualTo("REQ-NOT-FOUND-TEST");
	}

	@Test
	void invalidSalesLedgerFilterReturnsSalesInvalidFilterCode() throws Exception {
		MvcResult result = mockMvc.perform(get("/admin/api/sales-ledger")
						.param("transactionType", "WRONG")
						.header("X-Request-Id", "REQ-FILTER-TEST"))
				.andExpect(status().isBadRequest())
				.andReturn();

		JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(error.get("code").asText()).isEqualTo("SALES_INVALID_FILTER");
		assertThat(error.get("requestId").asText()).isEqualTo("REQ-FILTER-TEST");
	}

	@Test
	void duplicateApproveRequestReturnsExistingPaymentAndCreatesOneSale() {
		String orderNo = unique("ORDER-IDEMPOTENT");
		String approveKey = "APPROVE-" + orderNo;
		PaymentApproveRequest request = approveRequest(orderNo, new BigDecimal("50000"), approveKey);

		PaymentApproveResponse first = paymentApproveService.approvePayment(request);
		PaymentApproveResponse second = paymentApproveService.approvePayment(request);

		assertThat(second.paymentId()).isEqualTo(first.paymentId());
		assertThat(paymentRepository.findAll().stream().filter(payment -> orderNo.equals(payment.getOrderNo()))).hasSize(1);
		assertThat(salesRepository.findAll().stream()
				.filter(sales -> orderNo.equals(sales.getOrderNo()) && SaleType.SALE.equals(sales.getSaleType()))).hasSize(1);
	}

	@Test
	void approvedPaymentCreatesPostedSaleLedgerWithWonVatBreakdown() {
		String orderNo = unique("ORDER-SALE-LEDGER");

		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("12000"), "APPROVE-" + orderNo));

		SalesTransaction sale = salesRepository.findAll().stream()
				.filter(sales -> orderNo.equals(sales.getOrderNo()) && SaleType.SALE.equals(sales.getSaleType()))
				.findFirst()
				.orElseThrow();
		assertThat(sale.getPaymentId()).isEqualTo(approved.paymentId());
		assertThat(sale.getCancelId()).isNull();
		assertThat(sale.getOriginalSalesTransactionId()).isNull();
		assertThat(sale.getTotalAmount()).isEqualByComparingTo("12000");
		assertThat(sale.getSupplyAmount()).isEqualByComparingTo("10909");
		assertThat(sale.getVatAmount()).isEqualByComparingTo("1091");
		assertThat(sale.getLedgerStatus()).isEqualTo(LedgerStatus.POSTED);
		assertThat(sale.getSettlementStatus()).isEqualTo(SalesSettlementStatus.NOT_SETTLED);
	}

	@Test
	void duplicateCancelRequestReturnsExistingCancelAndCreatesOneCancelSale() {
		String orderNo = unique("ORDER-CANCEL-IDEMPOTENT");
		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("50000"), "APPROVE-" + orderNo));
		String cancelKey = "CANCEL-" + orderNo;
		PaymentBridgeCancelRequest request = cancelRequest(new BigDecimal("10000"), cancelKey);

		PaymentBridgeCancelResponse first = paymentCancelService.cancelPaymentBridge(approved.paymentId(), request);
		PaymentBridgeCancelResponse second = paymentCancelService.cancelPaymentBridge(approved.paymentId(), request);

		assertThat(second.cancelId()).isEqualTo(first.cancelId());
		assertThat(cancelRepository.findAll().stream().filter(cancel -> cancelKey.equals(cancel.getCancelRequestKey()))).hasSize(1);
		assertThat(salesRepository.findAll().stream()
				.filter(sales -> orderNo.equals(sales.getOrderNo()) && SaleType.CANCEL.equals(sales.getSaleType()))).hasSize(1);
	}

	@Test
	void successfulCancelCreatesNegativeCancelLedgerLinkedToOriginalSale() {
		String orderNo = unique("ORDER-CANCEL-LEDGER");
		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("12000"), "APPROVE-" + orderNo));
		PaymentBridgeCancelResponse canceled = paymentCancelService.cancelPaymentBridge(
				approved.paymentId(), cancelRequest(new BigDecimal("3000"), "CANCEL-" + orderNo));

		SalesTransaction sale = salesRepository.findAll().stream()
				.filter(sales -> orderNo.equals(sales.getOrderNo()) && SaleType.SALE.equals(sales.getSaleType()))
				.findFirst()
				.orElseThrow();
		SalesTransaction cancel = salesRepository.findAll().stream()
				.filter(sales -> orderNo.equals(sales.getOrderNo()) && SaleType.CANCEL.equals(sales.getSaleType()))
				.findFirst()
				.orElseThrow();

		assertThat(cancel.getPaymentId()).isEqualTo(approved.paymentId());
		assertThat(cancel.getCancelId()).isEqualTo(canceled.cancelId());
		assertThat(cancel.getOriginalSalesTransactionId()).isEqualTo(sale.getId());
		assertThat(cancel.getTotalAmount()).isEqualByComparingTo("-3000");
		assertThat(cancel.getSupplyAmount()).isEqualByComparingTo("-2727");
		assertThat(cancel.getVatAmount()).isEqualByComparingTo("-273");
		assertThat(cancel.getLedgerStatus()).isEqualTo(LedgerStatus.POSTED);
		assertThat(cancel.getSettlementStatus()).isEqualTo(SalesSettlementStatus.NOT_SETTLED);
	}

	@Test
	void partialCancelIsIncludedInSettlementSaleAndCancelAmounts() {
		String orderNo = unique("ORDER-PARTIAL-CANCEL-SETTLEMENT");
		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("12000"), "APPROVE-" + orderNo));
		paymentCancelService.cancelPaymentBridge(
				approved.paymentId(), cancelRequest(new BigDecimal("3000"), "CANCEL-" + orderNo));

		SettlementStatementResponse statement = settlementOperationService.runDailySettlement(
				new SettlementBatchRunRequest(LocalDate.now()));

		assertThat(statement.saleAmount()).isGreaterThanOrEqualTo(new BigDecimal("12000"));
		assertThat(statement.cancelAmount()).isLessThanOrEqualTo(new BigDecimal("-3000"));
		assertThat(statement.saleAmount().add(statement.cancelAmount()))
				.isEqualByComparingTo(statement.grossAmount());
	}

	@Test
	void settlementBatchApiAccumulatesNewSalesIntoSameDraftStatement() throws Exception {
		String firstOrderNo = unique("ORDER-SETTLEMENT-API-FIRST");
		PaymentApproveResponse firstApproved = paymentApproveService.approvePayment(
				approveRequest(firstOrderNo, new BigDecimal("12000"), "APPROVE-" + firstOrderNo));
		paymentCancelService.cancelPaymentBridge(
				firstApproved.paymentId(), cancelRequest(new BigDecimal("3000"), "CANCEL-" + firstOrderNo));

		JsonNode first = objectMapper.readTree(mockMvc.perform(post("/admin/api/settlements/batch/run")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString());

		String secondOrderNo = unique("ORDER-SETTLEMENT-API-SECOND");
		paymentApproveService.approvePayment(
				approveRequest(secondOrderNo, new BigDecimal("8000"), "APPROVE-" + secondOrderNo));

		JsonNode recalculated = objectMapper.readTree(mockMvc.perform(post("/admin/api/settlements/batch/run")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString());

		assertThat(recalculated.get("id").asLong()).isEqualTo(first.get("id").asLong());
		assertThat(recalculated.get("saleAmount").decimalValue())
				.isEqualByComparingTo(first.get("saleAmount").decimalValue().add(new BigDecimal("8000")));
		assertThat(recalculated.get("cancelAmount").decimalValue())
				.isEqualByComparingTo(first.get("cancelAmount").decimalValue());
		assertThat(recalculated.get("grossAmount").decimalValue())
				.isEqualByComparingTo(first.get("grossAmount").decimalValue().add(new BigDecimal("8000")));
	}

	@Test
	void cancelAmountCannotExceedRemainingCancelableAmount() {
		String orderNo = unique("ORDER-OVER-CANCEL");
		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("50000"), "APPROVE-" + orderNo));
		paymentCancelService.cancelPaymentBridge(approved.paymentId(), cancelRequest(new BigDecimal("30000"), "CANCEL-1-" + orderNo));

		assertThatThrownBy(() -> paymentCancelService.cancelPaymentBridge(
				approved.paymentId(), cancelRequest(new BigDecimal("25000"), "CANCEL-2-" + orderNo)))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("취소 가능 금액");
	}

	@Test
	void concurrentPartialCancelCreatesOnlyAllowedAmount() throws Exception {
		String orderNo = unique("ORDER-CONCURRENT-PARTIAL-CANCEL");
		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("10000"), "APPROVE-" + orderNo));

		ConcurrentCancelResult result = runConcurrentCancels(
				approved.paymentId(), new BigDecimal("8000"),
				approved.paymentId(), new BigDecimal("8000"));

		assertThat(result.successCount()).isEqualTo(1);
		assertThat(result.failureCount()).isEqualTo(1);

		var payment = paymentRepository.findById(approved.paymentId()).orElseThrow();
		var cancels = cancelRepository.findAll().stream()
				.filter(cancel -> approved.paymentId().equals(cancel.getPaymentId()))
				.toList();
		var cancelLedgers = salesRepository.findAll().stream()
				.filter(sales -> approved.paymentId().equals(sales.getPaymentId()) && SaleType.CANCEL.equals(sales.getSaleType()))
				.toList();

		assertThat(payment.getCanceledAmount()).isEqualByComparingTo("8000");
		assertThat(payment.getCanceledAmount().compareTo(payment.getApprovedAmount())).isLessThanOrEqualTo(0);
		assertThat(cancels).hasSize(1);
		assertThat(cancelLedgers).hasSize(1);
		assertThat(cancelLedgers.get(0).getTotalAmount()).isEqualByComparingTo("-8000");
	}

	@Test
	void partialCancelLockDoesNotBlockDifferentPaymentRows() throws Exception {
		String orderNoA = unique("ORDER-CONCURRENT-PAYMENT-A");
		String orderNoB = unique("ORDER-CONCURRENT-PAYMENT-B");
		PaymentApproveResponse approvedA = paymentApproveService.approvePayment(
				approveRequest(orderNoA, new BigDecimal("10000"), "APPROVE-" + orderNoA));
		PaymentApproveResponse approvedB = paymentApproveService.approvePayment(
				approveRequest(orderNoB, new BigDecimal("10000"), "APPROVE-" + orderNoB));

		ConcurrentCancelResult result = runConcurrentCancels(
				approvedA.paymentId(), new BigDecimal("3000"),
				approvedB.paymentId(), new BigDecimal("4000"));

		assertThat(result.successCount()).isEqualTo(2);
		assertThat(result.failureCount()).isZero();
		assertThat(paymentRepository.findById(approvedA.paymentId()).orElseThrow().getCanceledAmount())
				.isEqualByComparingTo("3000");
		assertThat(paymentRepository.findById(approvedB.paymentId()).orElseThrow().getCanceledAmount())
				.isEqualByComparingTo("4000");
		assertThat(cancelRepository.findAll().stream().filter(cancel -> approvedA.paymentId().equals(cancel.getPaymentId())))
				.hasSize(1);
		assertThat(cancelRepository.findAll().stream().filter(cancel -> approvedB.paymentId().equals(cancel.getPaymentId())))
				.hasSize(1);
		assertThat(salesRepository.findAll().stream()
				.filter(sales -> approvedA.paymentId().equals(sales.getPaymentId()) && SaleType.CANCEL.equals(sales.getSaleType())))
				.hasSize(1);
		assertThat(salesRepository.findAll().stream()
				.filter(sales -> approvedB.paymentId().equals(sales.getPaymentId()) && SaleType.CANCEL.equals(sales.getSaleType())))
				.hasSize(1);
	}

	@Test
	void cancelRequestRequiresIdempotencyKey() {
		String orderNo = unique("ORDER-CANCEL-KEY-REQUIRED");
		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("50000"), "APPROVE-" + orderNo));

		assertThatThrownBy(() -> paymentCancelService.cancelPaymentBridge(
				approved.paymentId(), new PaymentBridgeCancelRequest(PgProvider.MOCK, new BigDecimal("1000"), "portfolio cancel", null)))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("취소 중복 방지 키");
	}

	@Test
	void partialAndFullCancelUpdatePaymentStatusByRemainingAmount() {
		String orderNo = unique("ORDER-PARTIAL-FULL");
		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("50000"), "APPROVE-" + orderNo));

		paymentCancelService.cancelPaymentBridge(approved.paymentId(), cancelRequest(new BigDecimal("10000"), "CANCEL-PART-" + orderNo));
		assertThat(paymentRepository.findById(approved.paymentId()).orElseThrow().getPaymentStatus())
				.isEqualTo(PaymentStatus.PARTIAL_CANCELED);

		paymentCancelService.cancelPaymentBridge(approved.paymentId(), cancelRequest(new BigDecimal("40000"), "CANCEL-FULL-" + orderNo));
		assertThat(paymentRepository.findById(approved.paymentId()).orElseThrow().getPaymentStatus())
				.isEqualTo(PaymentStatus.CANCELED);
	}

	@Test
	void approveUnknownCreatesRecoveryTask() {
		String orderNo = unique("ORDER-UNKNOWN");
		PaymentApproveResponse response = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("50000"), "APPROVE-" + orderNo));

		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.APPROVE_UNKNOWN.name());
		assertThat(recoveryTaskRepository.findAll().stream()
				.anyMatch(task -> task.getPaymentId().equals(response.paymentId())
						&& RecoveryType.APPROVE_UNKNOWN_CHECK.equals(task.getRecoveryType())))
				.isTrue();
		assertThat(salesRepository.findAll().stream()
				.noneMatch(sales -> orderNo.equals(sales.getOrderNo())))
				.isTrue();
	}

	@Test
	void retryQueryApproveUnknownSuccessCreatesSaleAndFollowUps() {
		String orderNo = unique("ORDER-UNKNOWN_RECOVERABLE");
		PaymentApproveResponse response = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("50000"), "APPROVE-" + orderNo));

		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.APPROVE_UNKNOWN.name());
		assertThat(salesRepository.findAll().stream()
				.noneMatch(sales -> orderNo.equals(sales.getOrderNo())))
				.isTrue();
		assertThat(recoveryTaskRepository.findByTaskKey("APPROVE_UNKNOWN-" + orderNo).orElseThrow().getStatus())
				.isEqualTo(RecoveryStatus.READY);

		paymentQueryService.retryQuery(response.paymentId());

		assertThat(paymentRepository.findById(response.paymentId()).orElseThrow().getPaymentStatus())
				.isEqualTo(PaymentStatus.APPROVED);
		List<SalesTransaction> saleLedgers = salesRepository.findAll().stream()
				.filter(sales -> orderNo.equals(sales.getOrderNo()) && SaleType.SALE.equals(sales.getSaleType()))
				.toList();
		assertThat(saleLedgers).hasSize(1);
		SalesTransaction sale = saleLedgers.get(0);
		assertThat(externalSendRequestRepository.findBySalesIdOrderByIdAsc(sale.getId())).hasSize(1);
		assertThat(alimtalkQueueRepository.findBySalesIdOrderByIdAsc(sale.getId())).hasSize(1);
		assertThat(recoveryTaskRepository.findByTaskKey("APPROVE_UNKNOWN-" + orderNo).orElseThrow().getStatus())
				.isEqualTo(RecoveryStatus.SUCCESS);

		paymentQueryService.retryQuery(response.paymentId());

		assertThat(salesRepository.findAll().stream()
				.filter(sales -> orderNo.equals(sales.getOrderNo()) && SaleType.SALE.equals(sales.getSaleType())))
				.hasSize(1);
		assertThat(externalSendRequestRepository.findBySalesIdOrderByIdAsc(sale.getId())).hasSize(1);
		assertThat(alimtalkQueueRepository.findBySalesIdOrderByIdAsc(sale.getId())).hasSize(1);
	}

	@Test
	void concurrentRecoveryRetryIsClaimedOnce() throws Exception {
		String orderNo = unique("UNKNOWN-RECOVERABLE-CONC");
		PaymentApproveResponse response = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("50000"), "APPROVE-" + orderNo));
		Long taskId = recoveryTaskRepository.findByTaskKey("APPROVE_UNKNOWN-" + orderNo).orElseThrow().getId();

		ConcurrentRetryResult result = runConcurrentRecoveryRetries(taskId);

		assertThat(result.successCount()).isEqualTo(1);
		assertThat(result.businessFailureCount()).isEqualTo(1);
		assertThat(result.unexpectedFailureCount()).isZero();
		assertThat(recoveryTaskRepository.findById(taskId).orElseThrow().getStatus()).isEqualTo(RecoveryStatus.SUCCESS);

		List<SalesTransaction> saleLedgers = salesRepository.findAll().stream()
				.filter(sales -> orderNo.equals(sales.getOrderNo()) && SaleType.SALE.equals(sales.getSaleType()))
				.toList();
		assertThat(saleLedgers).hasSize(1);
		assertThat(externalSendRequestRepository.findBySalesIdOrderByIdAsc(saleLedgers.get(0).getId())).hasSize(1);
		assertThat(alimtalkQueueRepository.findBySalesIdOrderByIdAsc(saleLedgers.get(0).getId())).hasSize(1);
		assertThat(paymentRepository.findById(response.paymentId()).orElseThrow().getPaymentStatus())
				.isEqualTo(PaymentStatus.APPROVED);
	}

	@Test
	void retryProcessingTaskIsRejected() {
		String orderNo = unique("UNKNOWN-PROCESSING");
		paymentApproveService.approvePayment(approveRequest(orderNo, new BigDecimal("50000"), "APPROVE-" + orderNo));
		var task = recoveryTaskRepository.findByTaskKey("APPROVE_UNKNOWN-" + orderNo).orElseThrow();
		task.markProcessing();
		recoveryTaskRepository.saveAndFlush(task);

		assertThatThrownBy(() -> paymentRecoveryOperationService.retry(task.getId()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RECOVERY_TASK_NOT_CLAIMABLE));
		assertThat(recoveryTaskRepository.findById(task.getId()).orElseThrow().getStatus())
				.isEqualTo(RecoveryStatus.PROCESSING);
	}

	@Test
	void retrySuccessTaskIsRejected() {
		String orderNo = unique("UNKNOWN-RECOVERABLE-OK");
		PaymentApproveResponse response = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("50000"), "APPROVE-" + orderNo));
		var task = recoveryTaskRepository.findByTaskKey("APPROVE_UNKNOWN-" + orderNo).orElseThrow();
		paymentQueryService.retryQuery(response.paymentId());

		assertThatThrownBy(() -> paymentRecoveryOperationService.retry(task.getId()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RECOVERY_TASK_NOT_CLAIMABLE));
		assertThat(recoveryTaskRepository.findById(task.getId()).orElseThrow().getStatus())
				.isEqualTo(RecoveryStatus.SUCCESS);
	}

	@Test
	void retryFailedTaskCanBeClaimedAgain() {
		String orderNo = unique("UNKNOWN-RECOVERABLE-FAIL");
		PaymentApproveResponse response = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("50000"), "APPROVE-" + orderNo));
		var task = recoveryTaskRepository.findByTaskKey("APPROVE_UNKNOWN-" + orderNo).orElseThrow();
		task.markFailed("이전 재처리 실패");
		recoveryTaskRepository.saveAndFlush(task);

		var retried = paymentRecoveryOperationService.retry(task.getId());

		assertThat(retried.status()).isEqualTo(RecoveryStatus.SUCCESS.name());
		assertThat(recoveryTaskRepository.findById(task.getId()).orElseThrow().getStatus())
				.isEqualTo(RecoveryStatus.SUCCESS);
		assertThat(paymentRepository.findById(response.paymentId()).orElseThrow().getPaymentStatus())
				.isEqualTo(PaymentStatus.APPROVED);
	}

	@Test
	void retryUnsupportedTaskFailsWithoutProcessingStuck() {
		String orderNo = unique("CANCEL-UNKNOWN-UNSUPPORTED");
		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("50000"), "APPROVE-" + orderNo));
		paymentCancelService.cancelPaymentBridge(
				approved.paymentId(), cancelRequest(new BigDecimal("10000"), "CANCEL-UNKNOWN-" + orderNo));
		var task = recoveryTaskRepository.findAll().stream()
				.filter(candidate -> approved.paymentId().equals(candidate.getPaymentId())
						&& RecoveryType.CANCEL_UNKNOWN_CHECK.equals(candidate.getRecoveryType()))
				.findFirst()
				.orElseThrow();

		assertThatThrownBy(() -> paymentRecoveryOperationService.retry(task.getId()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RECOVERY_RETRY_NOT_ALLOWED));

		var failed = recoveryTaskRepository.findById(task.getId()).orElseThrow();
		assertThat(failed.getStatus()).isEqualTo(RecoveryStatus.FAILED);
		assertThat(failed.getLastErrorMessage()).contains("자동 재처리를 지원하지 않습니다");
	}

	@Test
	void cancelUnknownCreatesRecoveryTaskWithoutCancelSale() {
		String orderNo = unique("ORDER-CANCEL-UNKNOWN");
		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("50000"), "APPROVE-" + orderNo));

		PaymentBridgeCancelResponse response = paymentCancelService.cancelPaymentBridge(
				approved.paymentId(), cancelRequest(new BigDecimal("10000"), "CANCEL-UNKNOWN-" + orderNo));

		assertThat(response.cancelId()).isNull();
		assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.CANCEL_UNKNOWN.name());
		assertThat(salesRepository.findAll().stream()
				.noneMatch(sales -> orderNo.equals(sales.getOrderNo()) && SaleType.CANCEL.equals(sales.getSaleType())))
				.isTrue();
		assertThat(recoveryTaskRepository.findAll().stream()
				.anyMatch(task -> task.getPaymentId().equals(approved.paymentId())
						&& RecoveryType.CANCEL_UNKNOWN_CHECK.equals(task.getRecoveryType())))
				.isTrue();
	}

	@Test
	void salesLedgerSearchReturnsPageableResult() {
		String prefix = unique("ORDER-PAGEABLE");
		paymentApproveService.approvePayment(approveRequest(prefix + "-1", new BigDecimal("12000"), "APPROVE-" + prefix + "-1"));
		paymentApproveService.approvePayment(approveRequest(prefix + "-2", new BigDecimal("13000"), "APPROVE-" + prefix + "-2"));
		paymentApproveService.approvePayment(approveRequest(prefix + "-3", new BigDecimal("14000"), "APPROVE-" + prefix + "-3"));

		SalesLedgerPageResponse page = salesLedgerService.getSalesLedger(
				null, null, "SALE", null, null, prefix, 0, 2);

		assertThat(page.data()).hasSize(2);
		assertThat(page.totalCount()).isEqualTo(3);
		assertThat(page.size()).isEqualTo(2);
		assertThat(page.summary().totalSaleAmount()).isEqualByComparingTo("39000");
	}

	@Test
	void salesLedgerSummaryUsesAllFilteredRowsRegardlessOfPageSize() {
		String orderNo = unique("ORDER-SUMMARY-DB");
		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("12000"), "APPROVE-" + orderNo));
		paymentCancelService.cancelPaymentBridge(
				approved.paymentId(), cancelRequest(new BigDecimal("3000"), "CANCEL-" + orderNo));

		SalesLedgerPageResponse page = salesLedgerService.getSalesLedger(
				null, null, null, null, null, orderNo, 0, 1);

		assertThat(page.data()).hasSize(1);
		assertThat(page.totalCount()).isEqualTo(2);
		assertThat(page.summary().totalSaleAmount()).isEqualByComparingTo("12000");
		assertThat(page.summary().totalCancelAmount()).isEqualByComparingTo("-3000");
		assertThat(page.summary().netSalesAmount()).isEqualByComparingTo("9000");
	}

	@Test
	void salesLedgerSummaryAppliesSaleTypeKeywordAndSettlementStatusFilters() {
		String orderNo = unique("ORDER-SUMMARY-FILTER");
		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("12000"), "APPROVE-" + orderNo));
		paymentCancelService.cancelPaymentBridge(
				approved.paymentId(), cancelRequest(new BigDecimal("3000"), "CANCEL-" + orderNo));

		SalesLedgerPageResponse saleOnly = salesLedgerService.getSalesLedger(
				null, null, "SALE", null, null, orderNo, 0, 10);
		assertThat(saleOnly.summary().totalSaleAmount()).isEqualByComparingTo("12000");
		assertThat(saleOnly.summary().totalCancelAmount()).isEqualByComparingTo("0");
		assertThat(saleOnly.summary().netSalesAmount()).isEqualByComparingTo("12000");

		salesRepository.findAll().stream()
				.filter(sales -> orderNo.equals(sales.getOrderNo()))
				.forEach(sales -> {
					sales.markIncludedInSettlement();
					salesRepository.save(sales);
				});

		SalesLedgerPageResponse notSettled = salesLedgerService.getSalesLedger(
				null, null, null, null, "NOT_SETTLED", orderNo, 0, 10);
		assertThat(notSettled.summary().netSalesAmount()).isEqualByComparingTo("0");
		assertThat(notSettled.summary().notSettledCount()).isZero();

		SalesLedgerPageResponse calculated = salesLedgerService.getSalesLedger(
				null, null, null, null, "CALCULATED", orderNo, 0, 10);
		assertThat(calculated.summary().netSalesAmount()).isEqualByComparingTo("9000");
		assertThat(calculated.summary().calculatedCount()).isEqualTo(2);
	}

	@Test
	void runningSameDraftSettlementTwiceReturnsExistingSingleStatement() {
		LocalDate targetDate = uniqueSettlementDate();

		SettlementStatementResponse first = settlementOperationService.runDailySettlement(new SettlementBatchRunRequest(targetDate));
		SettlementStatementResponse second = settlementOperationService.runDailySettlement(new SettlementBatchRunRequest(targetDate));

		assertThat(second.id()).isEqualTo(first.id());
		assertThat(settlementStatementRepository.findAll().stream()
				.filter(statement -> targetDate.equals(statement.getSettlementDate()))
				.count()).isEqualTo(1);
	}

	@Test
	void rerunningDraftSettlementIncludesNewSalesWithoutCreatingAnotherStatement() {
		LocalDate targetDate = LocalDate.now();
		String firstOrderNo = unique("ORDER-SETTLEMENT-FIRST");
		String secondOrderNo = unique("ORDER-SETTLEMENT-SECOND");
		paymentApproveService.approvePayment(
				approveRequest(firstOrderNo, new BigDecimal("12000"), "APPROVE-" + firstOrderNo));

		SettlementStatementResponse first = settlementOperationService.runDailySettlement(new SettlementBatchRunRequest(targetDate));

		paymentApproveService.approvePayment(
				approveRequest(secondOrderNo, new BigDecimal("8000"), "APPROVE-" + secondOrderNo));
		SettlementStatementResponse recalculated = settlementOperationService.runDailySettlement(new SettlementBatchRunRequest(targetDate));
		BigDecimal expectedGrossAmount = first.grossAmount().add(new BigDecimal("8000"));

		assertThat(recalculated.id()).isEqualTo(first.id());
		assertThat(recalculated.grossAmount()).isEqualByComparingTo(expectedGrossAmount);
		assertThat(settlementStatementRepository.findById(first.id()).orElseThrow().getGrossAmount())
				.isEqualByComparingTo(expectedGrossAmount);
		assertThat(salesRepository.findAll().stream()
				.filter(sale -> firstOrderNo.equals(sale.getOrderNo()) || secondOrderNo.equals(sale.getOrderNo()))
				.allMatch(SalesTransaction::getSettlementIncludedYn)).isTrue();
		assertThat(settlementStatementRepository.findAll().stream()
				.filter(statement -> targetDate.equals(statement.getSettlementDate()))
				.count()).isEqualTo(1);
	}

	@Test
	void recoveryTaskSurvivesOuterTransactionRollback() {
		String suffix = UUID.randomUUID().toString();
		String taskKey = "APPROVE_INTERNAL_SAVE_FAILED-" + suffix;
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
			paymentRecoveryService.createRecoveryTask(
					null,
					null,
					"ORDER-" + suffix,
					"TID-" + suffix,
					"APPROVE-" + suffix,
					RecoveryType.APPROVE_INTERNAL_SAVE_FAILED,
					taskKey,
					"강제 롤백 테스트"
			);
			throw new IllegalStateException("outer transaction rollback");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(recoveryTaskRepository.findByTaskKey(taskKey)).isPresent();
	}

	@Test
	void confirmedAndPaidSettlementStatusTransitionsAreRestricted() {
		LocalDate targetDate = uniqueSettlementDate();
		SettlementStatementResponse draft = settlementOperationService.runDailySettlement(new SettlementBatchRunRequest(targetDate));

		SettlementStatementResponse confirmed = settlementOperationService.confirmStatement(draft.id());
		assertThat(confirmed.settlementStatus()).isEqualTo("CONFIRMED");
		assertThatThrownBy(() -> settlementOperationService.confirmStatement(draft.id()))
				.isInstanceOf(BusinessException.class);

		SettlementStatementResponse paid = settlementOperationService.markPaid(draft.id());
		assertThat(paid.settlementStatus()).isEqualTo("PAID");
		assertThatThrownBy(() -> settlementOperationService.markPaid(draft.id()))
				.isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> settlementOperationService.runDailySettlement(new SettlementBatchRunRequest(targetDate)))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SETTLEMENT_DUPLICATE_EXECUTION));
	}

	@Test
	void salesLedgerLinksReturnFollowUpRowsBySalesId() {
		String orderNo = unique("ORDER-LINKS");
		PaymentApproveResponse approved = paymentApproveService.approvePayment(
				approveRequest(orderNo, new BigDecimal("12000"), "APPROVE-" + orderNo));
		SalesTransaction sale = salesRepository.findAll().stream()
				.filter(sales -> orderNo.equals(sales.getOrderNo()) && SaleType.SALE.equals(sales.getSaleType()))
				.findFirst()
				.orElseThrow();

		SalesLedgerLinksResponse links = salesLedgerService.getSalesLedgerLinks(sale.getId());

		assertThat(links.payment().id()).isEqualTo(approved.paymentId());
		assertThat(links.externalSends()).hasSize(1);
		assertThat(links.alimtalkQueues()).hasSize(1);
	}

	private PaymentApproveRequest approveRequest(String orderNo, BigDecimal amount, String idempotencyKey) {
		return new PaymentApproveRequest(
				PgProvider.MOCK,
				orderNo,
				amount,
				"KRW",
				"Portfolio Buyer",
				"Payment Bridge Mock Item",
				idempotencyKey,
				"WEB",
				"PORTFOLIO",
				"CARD"
		);
	}

	private PaymentBridgeCancelRequest cancelRequest(BigDecimal amount, String idempotencyKey) {
		return new PaymentBridgeCancelRequest(PgProvider.MOCK, amount, "portfolio cancel", idempotencyKey);
	}

	private ConcurrentCancelResult runConcurrentCancels(
			Long firstPaymentId,
			BigDecimal firstAmount,
			Long secondPaymentId,
			BigDecimal secondAmount) throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(2);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failureCount = new AtomicInteger();

		try {
			submitCancel(executor, ready, start, done, successCount, failureCount, firstPaymentId, firstAmount);
			submitCancel(executor, ready, start, done, successCount, failureCount, secondPaymentId, secondAmount);

			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
			return new ConcurrentCancelResult(successCount.get(), failureCount.get());
		} finally {
			executor.shutdownNow();
		}
	}

	private ConcurrentRetryResult runConcurrentRecoveryRetries(Long taskId) throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(2);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger businessFailureCount = new AtomicInteger();
		AtomicInteger unexpectedFailureCount = new AtomicInteger();

		try {
			for (int i = 0; i < 2; i++) {
				executor.submit(() -> {
					try {
						ready.countDown();
						start.await();
						paymentRecoveryOperationService.retry(taskId);
						successCount.incrementAndGet();
					} catch (BusinessException exception) {
						businessFailureCount.incrementAndGet();
					} catch (Exception exception) {
						unexpectedFailureCount.incrementAndGet();
					} finally {
						done.countDown();
					}
				});
			}

			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
			return new ConcurrentRetryResult(
					successCount.get(),
					businessFailureCount.get(),
					unexpectedFailureCount.get()
			);
		} finally {
			executor.shutdownNow();
		}
	}

	private void submitCancel(
			ExecutorService executor,
			CountDownLatch ready,
			CountDownLatch start,
			CountDownLatch done,
			AtomicInteger successCount,
			AtomicInteger failureCount,
			Long paymentId,
			BigDecimal amount) {
		executor.submit(() -> {
			try {
				ready.countDown();
				start.await();
				paymentCancelService.cancelPaymentBridge(
						paymentId,
						cancelRequest(amount, "CANCEL-CONCURRENT-" + UUID.randomUUID()));
				successCount.incrementAndGet();
			} catch (Exception exception) {
				failureCount.incrementAndGet();
			} finally {
				done.countDown();
			}
		});
	}

	private String unique(String prefix) {
		return prefix + "-" + UUID.randomUUID();
	}

	private LocalDate uniqueSettlementDate() {
		long offset = Math.abs(UUID.randomUUID().getMostSignificantBits() % 100000L) + 10L;
		return LocalDate.now().plusDays(offset);
	}

	private record ConcurrentCancelResult(int successCount, int failureCount) {
	}

	private record ConcurrentRetryResult(int successCount, int businessFailureCount, int unexpectedFailureCount) {
	}
}
