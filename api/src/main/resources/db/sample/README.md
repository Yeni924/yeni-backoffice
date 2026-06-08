# 샘플 데이터

이 폴더는 개발과 포트폴리오 시연에 사용할 샘플 데이터 SQL을 관리하기 위한 공간입니다.

## 초기 전략

로컬 개발에서는 H2 File DB와 JPA `ddl-auto=update`를 사용해 테이블 스키마를 자동 생성합니다. 샘플 SQL은 필요한 경우 별도로 실행해 데이터를 채웁니다.

## 향후 계획

기능이 안정화되면 아래와 같은 샘플 SQL을 추가할 수 있습니다.

- `product-sample.sql`: 상품 샘플 데이터
- `order-sample.sql`: 주문 샘플 데이터
- `payment-sample.sql`: 결제 샘플 데이터
- `notification-sample.sql`: 알림 샘플 데이터

## Git 정책

실제 H2 DB 파일은 Git에 올리지 않습니다. 각 개발자의 로컬 환경에서 독립적으로 관리합니다.

- `.gitignore`에서 `data/` 폴더의 `*.mv.db`, `*.trace.db` 파일을 제외합니다.
- SQL migration 또는 sample script는 Git에 저장할 수 있습니다.
- 개발자는 애플리케이션 시작 후 필요한 SQL script를 실행해 DB를 재생성할 수 있습니다.

## 사용 방법

### 1. 애플리케이션 시작

```bash
./gradlew bootRun
```

H2 File DB와 JPA `ddl-auto=update`가 테이블 스키마를 자동 생성합니다.

### 2. H2 Console에서 샘플 데이터 추가

```text
http://localhost:8080/h2-console
```

필요한 SQL 파일을 열어 실행합니다.

### 3. 데이터 확인

```sql
SELECT * FROM table_name;
```
