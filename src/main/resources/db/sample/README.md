# Sample Data

이 폴더는 개발 및 포트폴리오 시연용 샘플 데이터를 관리하기 위한 공간입니다.

## 초기 전략

초기에는 H2 File DB와 JPA `ddl-auto=update`를 사용하여 자동으로 테이블 스키마를 생성합니다.

## 향후 계획

기능이 안정화되면 아래와 같은 SQL 파일을 추가할 예정입니다:

- `product-sample.sql` - 상품 샘플 데이터
- `order-sample.sql` - 주문 샘플 데이터
- `payment-sample.sql` - 결제 샘플 데이터
- `notification-sample.sql` - 알림 샘플 데이터

## Git 정책

실제 H2 DB 파일은 Git에 올리지 않습니다. 각 개발자의 로컬 환경에서 독립적으로 관리됩니다.

- `.gitignore`에 `data/` 폴더와 `*.mv.db`, `*.trace.db` 파일을 제외합니다.
- SQL migration/sample script는 Git에 저장됩니다.
- 각 개발자는 애플리케이션 시작 시 SQL script를 통해 DB를 재생성할 수 있습니다.

## 사용 방법

### 1. 애플리케이션 시작

```bash
./gradlew bootRun
```

H2 File DB와 JPA ddl-auto=update가 자동으로 테이블 스키마를 생성합니다.

### 2. H2 Console에서 샘플 데이터 추가

```
http://localhost:8080/h2-console
```

향후 SQL 파일들을 여기서 실행할 수 있습니다.

### 3. 데이터 확인

```sql
SELECT * FROM 테이블명;
```
