# Yeni Backoffice Portfolio

C#/.NET 실무 경험을 Java/Spring Boot 백오피스로 확장하는 포트폴리오 프로젝트입니다.

## 프로젝트 정보

- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **Build**: Gradle
- **Template**: Thymeleaf
- **Database**: H2 (개발) → MySQL/Docker (향후)
- **ORM**: Spring Data JPA

## 로컬 개발 환경 설정

### 사전 요구사항

- Java 17 이상
- Gradle (프로젝트에 포함된 Gradle wrapper 사용)

### 실행

```bash
# Linux/macOS
./gradlew bootRun

# Windows PowerShell
.\gradlew.bat bootRun
```

애플리케이션은 `http://localhost:8080`에서 시작됩니다.

## Local H2 Database

현재 기본 실행 profile은 `test`이며, H2 File DB를 사용합니다.

### H2 Console

브라우저에서 아래 주소로 접속합니다.

```
http://localhost:8080/h2-console
```

**접속 정보:**

```
Driver Class: org.h2.Driver
JDBC URL: jdbc:h2:file:./data/yeni-backoffice;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
User Name: sa
Password: (비워두기)
```

**주의:**

- `jdbc:h2:~/test`는 기본 샘플 DB이므로 사용하지 않습니다.
- H2 실제 DB 파일은 `data/` 폴더에 생성되며, Git에 올리지 않습니다.

## Database Sharing Strategy

회사 PC와 집 PC에서 모두 개발할 수 있도록 실제 DB 파일은 Git에 올리지 않습니다.

### Git에 올리는 것

- Entity 코드
- Repository, Service, Controller 코드
- `application-*.properties` 설정 파일
- DB migration 또는 sample data script
- README 실행 방법

### Git에 올리지 않는 것

- H2 실제 DB 파일
- `data/` 폴더
- `*.mv.db`, `*.trace.db`
- `.env`
- 운영 DB 비밀번호

**DB 재생성 방식:**

각 환경에서는 Entity 코드와 migration/sample script를 통해 DB 구조와 샘플 데이터를 자동 재생성합니다.

## 개발 프로필 설정

### test (개발 기본값)

H2 File DB를 사용하는 로컬 개발 환경입니다.

```bash
./gradlew bootRun
```

### local (Docker MySQL)

나중에 Docker MySQL/MariaDB 연결용 설정입니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### prod (클라우드 배포)

환경변수를 사용하는 프로덕션 환경입니다.

```bash
export DB_URL="jdbc:mysql://..."
export DB_USERNAME="user"
export DB_PASSWORD="password"
export PORT="8080"
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## 주요 기능 (계획 중)

- **상품관리**: 상품 등록, 수정, 판매상태 변경, 재고 관리
- **주문관리**: 주문 목록, 상세, 상태 변경, 이력 관리
- **결제관리**: 결제 승인 Mock, 취소, 이력 관리
- **배송관리**: 송장 등록, 배송 상태 변경, 이력 조회
- **프로모션관리**: 기간별 프로모션, 할인 정책 관리
- **알림톡 Queue**: 상태 변경 알림 큐, 재처리 구조

## 프로젝트 구조

```
src/
├── main/
│   ├── java/
│   │   └── com/yeni/yeni_backoffice/
│   │       ├── YeniBackofficeApplication.java
│   │       └── dashboard/
│   │           └── DashboardController.java
│   └── resources/
│       ├── templates/
│       │   ├── fragments/
│       │   │   ├── sidebar.html
│       │   │   └── topbar.html
│       │   └── dashboard/
│       │       └── index.html
│       ├── static/
│       │   ├── css/
│       │   │   ├── common.css
│       │   │   ├── layout.css
│       │   │   ├── components.css
│       │   │   └── pages/
│       │   │       └── dashboard.css
│       │   └── js/
│       │       ├── common.js
│       │       └── pages/
│       │           └── dashboard.js
│       ├── application.properties
│       ├── application-test.properties
│       ├── application-local.properties
│       └── application-prod.properties
└── test/
```

## 기술 스택

### Backend

- Java 17
- Spring Boot 3.5.x
- Spring Data JPA
- Hibernate

### Frontend

- Thymeleaf
- Vanilla JavaScript
- CSS3 Grid/Flexbox

### Database

- H2 (개발)
- MySQL 8.0+ (로컬/클라우드)

## 라이선스

Portfolio Project

## 연락처

권예은 (Ye Eun Kwon)
- Role: Backoffice / API Developer
- Email: [contact info]
