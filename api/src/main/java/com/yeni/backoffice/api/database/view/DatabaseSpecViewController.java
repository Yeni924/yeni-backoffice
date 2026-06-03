package com.yeni.backoffice.api.database.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/database-spec")
public class DatabaseSpecViewController {

    @GetMapping
    public String page(Model model) {
        model.addAttribute("title", "DB Specification");
        model.addAttribute("description", "Payment, sales, external send and settlement table specification.");
        model.addAttribute("tableSpecs", tableSpecs());
        return "database/spec";
    }

    private List<TableSpec> tableSpecs() {
        return List.of(
                table("payment_auth_session", "표준결제 요청부터 인증 결과 수신 전까지의 결제 세션을 관리합니다.",
                        column("id", "PK", "결제 인증 세션 ID"),
                        column("mid", "VARCHAR", "PG 가맹점 ID"),
                        column("order_no", "VARCHAR", "내부 주문번호"),
                        column("amount", "DECIMAL", "결제 요청 금액"),
                        column("auth_token", "VARCHAR", "Mock 인증 토큰"),
                        column("status", "ENUM", "REQUEST_READY, AUTH_RESULT_RECEIVED, APPROVED, FAILED")),
                table("payment_transaction", "승인 완료된 결제 거래와 취소 누적 금액을 관리합니다.",
                        column("id", "PK", "결제 거래 ID"),
                        column("order_no", "VARCHAR", "내부 주문번호"),
                        column("tid", "VARCHAR", "PG 거래번호"),
                        column("approved_amount", "DECIMAL", "승인 금액"),
                        column("canceled_amount", "DECIMAL", "누적 취소 금액"),
                        column("payment_status", "ENUM", "APPROVED, PARTIAL_CANCELED, CANCELED")),
                table("payment_cancel", "전체/부분 취소 요청과 idempotency key를 관리합니다.",
                        column("id", "PK", "취소 ID"),
                        column("payment_id", "BIGINT", "결제 거래 ID"),
                        column("cancel_request_key", "VARCHAR", "중복 취소 방지 키"),
                        column("cancel_amount", "DECIMAL", "취소 요청 금액"),
                        column("cancel_type", "ENUM", "FULL, PARTIAL"),
                        column("cancel_status", "ENUM", "SUCCESS, FAILED")),
                table("pg_api_log", "PG 요청/응답 전문과 처리 결과를 추적합니다.",
                        column("id", "PK", "로그 ID"),
                        column("request_id", "VARCHAR", "요청 추적 ID"),
                        column("pg_provider", "ENUM", "PGB에서 라우팅한 Provider. MOCK, INICIS"),
                        column("event_type", "ENUM", "APPROVE, CANCEL, QUERY, CALLBACK"),
                        column("api_type", "ENUM", "APPROVE, CANCEL 등 PG API 유형"),
                        column("idempotency_key", "VARCHAR", "중복 요청 방지 키"),
                        column("http_status", "INTEGER", "Mock API 처리 HTTP 상태"),
                        column("success_yn", "BOOLEAN", "성공 여부"),
                        column("duration_ms", "BIGINT", "처리 소요 시간"),
                        column("request_body", "CLOB", "민감정보를 제외한 요청 내용"),
                        column("response_body", "CLOB", "민감정보를 제외한 응답 내용"),
                        column("error_message", "VARCHAR", "실패/UNKNOWN 원인 메시지")),
                table("sales_transaction", "결제/취소 결과를 매출 데이터로 분리해 정산 대상화합니다.",
                        column("id", "PK", "매출 ID"),
                        column("source_type", "VARCHAR", "SALE 또는 CANCEL"),
                        column("order_no", "VARCHAR", "내부 주문번호"),
                        column("sale_amount", "DECIMAL", "매출 금액"),
                        column("sale_status", "ENUM", "READY, SETTLED"),
                        column("business_date", "DATE", "정산 기준 영업일")),
                table("external_send_request", "외부 영업관리 시스템 전송 큐를 관리합니다.",
                        column("id", "PK", "전송 요청 ID"),
                        column("sales_id", "BIGINT", "매출 ID"),
                        column("request_key", "VARCHAR", "전송 중복 방지 키"),
                        column("target_system", "VARCHAR", "전송 대상 시스템"),
                        column("send_status", "ENUM", "READY, SUCCESS, FAILED"),
                        column("retry_count", "INTEGER", "재시도 횟수")),
                table("external_send_history", "외부 전송 시도별 요청/응답 이력을 저장합니다.",
                        column("id", "PK", "전송 이력 ID"),
                        column("send_request_id", "BIGINT", "전송 요청 ID"),
                        column("request_body", "CLOB", "전송 요청 내용"),
                        column("response_body", "CLOB", "전송 응답 내용"),
                        column("result_status", "VARCHAR", "전송 결과"),
                        column("sent_at", "DATETIME", "전송 시각")),
                table("pg_fee_policy", "PG 수수료율과 적용 기간을 관리합니다.",
                        column("id", "PK", "수수료 정책 ID"),
                        column("pg_company", "VARCHAR", "PG사"),
                        column("mid", "VARCHAR", "가맹점 ID"),
                        column("payment_method", "VARCHAR", "결제수단"),
                        column("fee_rate", "DECIMAL", "수수료율"),
                        column("effective_start_date", "DATE", "적용 시작일"),
                        column("effective_end_date", "DATE", "적용 종료일")),
                table("settlement_statement", "일별 정산 집계 금액과 정산 상태를 관리합니다.",
                        column("id", "PK", "정산 명세 ID"),
                        column("settlement_date", "DATE", "정산 기준일"),
                        column("gross_amount", "DECIMAL", "총 매출 금액"),
                        column("fee_amount", "DECIMAL", "PG 수수료"),
                        column("vat_amount", "DECIMAL", "수수료 VAT"),
                        column("net_amount", "DECIMAL", "지급 예정 금액"),
                        column("settlement_status", "ENUM", "DRAFT, CONFIRMED, PAID")),
                table("settlement_detail", "정산 명세에 포함된 매출 단위 상세를 관리합니다.",
                        column("id", "PK", "정산 상세 ID"),
                        column("settlement_statement_id", "BIGINT", "정산 명세 ID"),
                        column("sales_id", "BIGINT", "매출 ID"),
                        column("sale_amount", "DECIMAL", "매출 금액"),
                        column("fee_amount", "DECIMAL", "수수료"),
                        column("net_amount", "DECIMAL", "차감 후 금액")),
                table("settlement_batch_log", "정산 배치 실행 상태와 처리 건수를 기록합니다.",
                        column("id", "PK", "배치 로그 ID"),
                        column("target_date", "DATE", "배치 대상일"),
                        column("batch_status", "ENUM", "RUNNING, SUCCESS, FAILED"),
                        column("target_count", "INTEGER", "대상 건수"),
                        column("success_count", "INTEGER", "성공 건수"),
                        column("failure_count", "INTEGER", "실패 건수")),
                table("audit_log", "결제/매출/정산 주요 액션의 감사 로그를 저장합니다.",
                        column("id", "PK", "감사 로그 ID"),
                        column("domain_type", "VARCHAR", "도메인 구분"),
                        column("action_type", "VARCHAR", "행위 구분"),
                        column("reference_key", "VARCHAR", "참조 키"),
                        column("description", "CLOB", "처리 설명"),
                        column("logged_at", "DATETIME", "로그 시각"))
        );
    }

    private TableSpec table(String name, String description, ColumnSpec... columns) {
        return new TableSpec(name, description, List.of(columns));
    }

    private ColumnSpec column(String name, String type, String description) {
        return new ColumnSpec(name, type, description);
    }

    public record TableSpec(String name, String description, List<ColumnSpec> columns) {
    }

    public record ColumnSpec(String name, String type, String description) {
    }
}
