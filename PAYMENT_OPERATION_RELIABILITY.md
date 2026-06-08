# 결제 운영 신뢰성 보강 메모

이 문서는 결제 승인/취소 흐름에서 중복 요청, 결과불명, 부분취소, 후속 처리 실패를 어떻게 방어하는지 정리한 부록입니다. 파일명은 링크 호환성을 위해 유지합니다.

## 승인 요청

- 승인 요청은 `idempotencyKey`와 `orderNo`를 기준으로 멱등성을 보장합니다.
- 동일 승인 요청이 다시 들어오면 기존 결제 결과를 반환하고, `payment_transaction`, SALE `sales_transaction`, 외부전송 대기열을 중복 생성하지 않습니다.
- PG 승인 결과가 timeout 또는 응답 유실로 확정되지 않으면 `APPROVE_UNKNOWN` 상태로 저장하고, `APPROVE_UNKNOWN_CHECK` 유형의 `payment_recovery_task`를 생성합니다.
- `APPROVE_UNKNOWN` 상태에서는 SALE 원장을 바로 생성하지 않습니다.
- 이후 `retryQuery`로 PG 승인 성공이 확인되면 결제 상태를 `APPROVED`로 변경하고 SALE 원장, 외부전송 대기열, 알림톡 Queue를 생성합니다.
- 같은 `retryQuery`가 반복 호출되어도 SALE 원장과 후속 처리 Queue가 중복 생성되지 않도록 source key와 message/request key로 방어합니다.

## 취소 요청

- 취소 요청은 `cancelRequestKey`를 기준으로 멱등성을 보장합니다.
- 동일 취소 요청이 다시 들어오면 기존 `payment_cancel` 결과를 반환하고, CANCEL 매출 원장을 중복 생성하지 않습니다.
- 취소 금액은 0보다 커야 하며, 남은 취소 가능 금액을 초과할 수 없습니다.
- 부분취소 후 잔액이 남으면 `PARTIAL_CANCELED`, 전체 잔액을 취소하면 `CANCELED` 상태로 전이합니다.
- 취소 처리 시 `findByIdForUpdate` 기반의 pessimistic write lock과 JPA version column을 함께 사용해 동시 부분취소가 남은 취소 가능 금액을 초과하지 않도록 방어합니다.
- PG 취소 결과가 timeout 또는 응답 유실로 확정되지 않으면 `CANCEL_UNKNOWN` 상태로 저장하고, `CANCEL_UNKNOWN_CHECK` 유형의 `payment_recovery_task`를 생성합니다.
- `CANCEL_UNKNOWN` 상태에서는 취소가 확정되기 전까지 CANCEL 원장을 생성하지 않습니다.

## 망취소와 후속 처리

- PG 승인은 성공했지만 내부 저장 또는 후속 처리에서 실패하면, 정상 실패로 조용히 처리하지 않고 망취소 또는 복구 대상 흐름으로 남깁니다.
- `pg_api_log`에는 Mock PG 호출의 요청/응답 메타데이터, 결과 코드, 결과 메시지, 처리 상태, idempotency key를 저장합니다.
- 실제 PG 운영 secret, signKey, 외부망 전문 같은 민감 정보는 저장하지 않는 방향을 전제로 합니다.
- 외부전송은 `external_send_request` 기반 outbox 형태로 분리합니다. 결제/매출 트랜잭션은 전송 대기 row만 만들고, 실제 전송과 재시도는 결제 트랜잭션 밖에서 처리할 수 있도록 분리합니다.
- 알림톡도 `alimtalk_queue`에 대기 row를 생성해 결제 승인/취소와 발송 처리를 분리합니다.

## 매출 원장 무결성

- 매출 원장은 append-only 방식의 SALE/CANCEL 거래로 관리합니다.
- 취소가 발생해도 기존 SALE row를 수정하지 않고, 별도의 CANCEL 음수 거래를 생성합니다.
- `sales_transaction`은 `sourceType + sourceId` unique constraint로 동일 승인/취소에 대한 원장 중복 생성을 방어합니다.
- CANCEL 거래에는 원 SALE ID, 결제 ID, 취소 ID, PG 거래번호를 연결해 추적 가능성을 유지합니다.
