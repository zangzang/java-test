# Multi-DB Sample Project

Maven 기반 멀티 데이터베이스 샘플 프로젝트입니다. 인터페이스 기반 설계로 DB 모듈을 쉽게 교체할 수 있습니다.

## 요구사항

- Java 21
- Maven 3.6+
- PostgreSQL/MySQL/MSSQL (선택적)

## 프로젝트 구조

```
multi-db-sample/
├─ base-entity/          # 공통 POJO (User 클래스)
├─ db-common/            # 공통 인터페이스 (UserDao, DatabaseConnection, DatabaseFactory)
├─ mysql-module/         # MySQL용 구현 (MySQLDatabaseConnection, MySQLUserDao, MySQLFactory)
├─ postgres-module/      # PostgreSQL용 구현 (PostgreSQLDatabaseConnection, PostgreSQLUserDao, PostgreSQLFactory, JOOQ Codegen)
├─ mssql-module/         # MSSQL용 구현 (MSSQLDatabaseConnection, MSSQLUserDao, MSSQLFactory)
└─ app/                  # 애플리케이션 모듈 (팩토리 패턴으로 DB 모듈 주입)
```

## 아키텍처

- **인터페이스 기반 설계**: `db-common` 모듈에 `UserDao`, `DatabaseConnection`, `DatabaseFactory` 인터페이스 정의
- **팩토리 패턴**: 각 DB 모듈이 팩토리를 구현하여 `UserDao`와 `DatabaseConnection` 인스턴스 생성
- **의존성 주입**: DAO가 DatabaseConnection을 주입받아 사용
- **외부 설정 주입**: DB 연결 정보(URL, 사용자, 비밀번호)를 애플리케이션 레벨에서 주입
- **JOOQ Codegen**: PostgreSQL 모듈에서 실제 DB 스키마 기반 코드 생성
- **모듈 분리**: DB 연결 코드와 비즈니스 로직 완전 분리

## 빌드 및 실행

1. 프로젝트 클론 또는 압축 해제
2. Maven으로 빌드:
   ```bash
   mvn clean install
   ```

3. 앱 실행 (현재 PostgreSQL 모듈 사용):
   ```bash
   mvn exec:java -pl app
   ```

## DB 모듈 교체

`app/AppService.java`에서 팩토리 구현체를 변경하여 DB 모듈을 교체할 수 있습니다.

현재 지원 모듈:
- PostgreSQL: `PostgreSQLFactory` (JOOQ 사용)
- MySQL: `MySQLFactory` (JDBC 사용)
- MSSQL: `MSSQLFactory` (JDBC 사용)

예: MySQL로 변경하려면 `AppService.java`에서:
```java
// PostgreSQLFactory에서 MySQLFactory로 변경
DatabaseFactory factory = new MySQLFactory();
```

## JOOQ Codegen

PostgreSQL 모듈은 실제 DB 연결 정보로 Codegen을 수행합니다:

- DB: PostgreSQL 16.9
- 스키마: public
- 테이블: user, users
- 생성 위치: `postgres-module/src/main/java/com/example/entity/generated/`

실제 DB 연결 시 `postgres-module/pom.xml`의 JOOQ 플러그인 설정을 확인하세요.

## jOOQ code generation (safe, opt-in)

이 프로젝트는 jOOQ로 생성된 코드를 DB 모듈에서 사용합니다. 기본 빌드에서 자동으로 데이터베이스에 연결하지 않도록
코드 생성은 옵트인(opt-in) 방식으로 구성되어 있습니다. 로컬에서 코드 생성을 실행하려면 `codegen` 프로파일을 사용하세요.

권장 방식: 로컬에 테스트 데이터베이스가 준비되어 있을 때만 생성 작업을 수행합니다.

PowerShell 예제 — 프로파일 사용 및 연결 정보 오버라이드:

```powershell
# postgres-module에서 codegen 프로파일을 활성화하고 DB 연결 정보를 전달
mvn -pl postgres-module -Pcodegen -Djooq.codegen.jdbc.url="jdbc:postgresql://localhost:5432/testdb" -Djooq.codegen.jdbc.user=postgres -Djooq.codegen.jdbc.password=password
```

직접 플러그인 목표를 호출하는 방법 (프로파일 없이 빠르게 실행할 때 유용):

```powershell
mvn -pl postgres-module org.jooq:jooq-codegen-maven:3.18.0:generate -Djooq.codegen.jdbc.url="jdbc:postgresql://localhost:5432/testdb" -Djooq.codegen.jdbc.user=postgres -Djooq.codegen.jdbc.password=password
```

주의사항:
- `codegen` 프로파일은 기본 비활성화되어 있으므로 CI나 일반 개발 빌드에서 DB 연결을 시도하지 않습니다.
- 일부 환경에서 빌드 시 DB에 접근할 수 없다면 생성된 소스를 VCS에 커밋하여 배포 파이프라인에서 사용하도록 고려하세요.
- DB 연결 정보는 `postgres-module/pom.xml`에서 `${jooq.codegen.jdbc.url}` 등으로 참조되므로 커맨드라인에서 `-D`로 안전하게 덮어쓸 수 있습니다.


## Generic DAO (공통) 사용법

`db-common` 모듈에 공통 제네릭 DAO인 `GenericJooqUserDao<R,I>`가 추가되어 있습니다. 이 클래스는 jOOQ의 `DSLContext`와 모듈별로 생성된 `USER` 테이블 및 컬럼(`ID`, `NAME`, `EMAIL`)을 주입받아 CRUD를 수행합니다.

간단 사용 예시 (모듈 내부):

```java
// PostgreSQL 예시: generated UserRecord 사용
var dao = new PostgreSQLUserDao(dbConnection);

// 직접 생성 사용 (DSLContext와 table/fields 주입)
var dao2 = new GenericJooqUserDao<>(dslContext, USER, USER.ID, USER.NAME, USER.EMAIL);
```

확장 포인트 (Id 타입 처리)
- `GenericJooqUserDao`는 기본적으로 숫자형 ID(Integer/Long/Short)를 처리하는 `DefaultIdConverter`를 내장합니다.
- 비-숫자형 ID(예: UUID, String)를 사용하는 경우 `IdConverter<T>` 인터페이스를 구현한 커스텀 변환기를 만들고, 서브클래스에서 이를 사용하도록 오버라이드하거나 생성자를 통해 주입할 수 있습니다.

테스트 실행 방법
- `db-common` 모듈에 단위/통합 테스트가 추가되어 있습니다:
   - `GenericJooqUserDaoTest` : id 변환 로직 단위 테스트
   - `GenericJooqUserDaoIntegrationTest` : H2 인메모리 DB를 이용한 CRUD 통합 테스트

모듈 단위로 테스트 실행:
```powershell
mvn -DskipTests=false -pl db-common test
```

또는 전체 프로젝트 테스트/빌드:
```powershell
mvn -DskipTests=true clean package
```

문제 발견 시
- 각 DB 모듈의 generated jOOQ 코드(특히 Table/Record의 제네릭 타입 및 필드 타입)를 확인하세요. `GenericJooqUserDao`는 모듈에서 생성한 `USER` 테이블과 필드를 그대로 받아 사용하도록 설계되어 있습니다.

---

### UUID Id 지원 예시

`GenericJooqUserDao`에 `IdConverter<UUID>`를 주입하면 UUID 기반 id를 사용할 수 있습니다. 예를 들어 `UUIDIdConverter`를 추가한 뒤 다음과 같이 DAO를 생성할 수 있습니다:

```java
// UUID 컬럼을 사용하는 경우 (예: PostgreSQL uuid 컬럼)
IdConverter<UUID> uuidConv = new UUIDIdConverter();
GenericJooqUserDao<UserRecord, UUID> dao = new GenericJooqUserDao<>(
   dslContext,
   USER,
   USER.ID.as("id", UUID.class),
   USER.NAME,
   USER.EMAIL,
   uuidConv
);
```

또는 PostgreSQL 전용 DAO에서 생성자 오버로드를 만들어 주입할 수 있습니다.

## jOOQ 상세 가이드 (MyBatis/Dapper 사용자 대상)

이 섹션은 MyBatis나 Dapper처럼 SQL 중심의 도구에 익숙하고, JPA 대신 jOOQ를 고려하는 개발자를 위해 작성했습니다. jOOQ는 SQL을 코드로 안전하게 표현하고, 필요하면 데이터베이스 메타데이터를 기반으로 강력한 타입-세이프 코드를 생성해 줍니다.

1) 핵심 개념
- jOOQ는 두 가지 축을 결합합니다: (A) 코드 생성(codegen)으로 DB 테이블/칼럼/레코드/타입을 Java 클래스와 타입으로 생성, (B) jOOQ의 DSL(도메인 특정 언어)로 안전한 SQL을 작성. SQL 문자열을 수동으로 조립하는 MyBatis/Dapper와 달리 쿼리 구조를 자바 타입으로 표현합니다.
- 결과: 컴파일 타임 타입 검사, 자동 완성, SQL 생성 시 컬럼/테이블 이름 실수 감소.

2) 코드 생성(codegen)
- 목적: DB 스키마(컬럼 타입 포함)를 Java 코드(테이블 클래스, 레코드 클래스, POJO 매핑 등)로 생성합니다.
- 실행 방법: 본 프로젝트처럼 Maven 플러그인 또는 별도 스크립트로 실행. DB 접속 또는 DDL 스크립트를 사용해 생성할 수 있습니다.
- 장점: 쿼리에서 컬럼 이름을 하드코딩할 필요가 없고, 타입 변환을 jOOQ가 제공.

3) DSL 사용 예제 (간단한 CRUD)
- 조회 예:

```java
// import static com.example.entity.generated.Tables.USER;
List<UserRecord> rows = dsl.selectFrom(USER)
      .where(USER.NAME.eq("alice"))
      .fetchInto(UserRecord.class);
// 또는 fetch()로 Record를 받고 필요한 열만 꺼내 사용
```

```java
// 삽입 예 (generated keys 처리)
UserRecord r = dsl.newRecord(USER);
r.setName("bob");
r.setEmail("bob@example.com");
r.store(); // 또는 dsl.insertInto(USER).set(r).returning(USER.ID).fetchOne();
```

```java
// 업데이트 예
dsl.update(USER)
    .set(USER.EMAIL, "new@example.com")
    .where(USER.ID.eq(123))
    .execute();
```

4) MyBatis/Dapper와의 비교
- MyBatis / Dapper:
   - SQL을 템플릿(또는 문자열)으로 관리하고, 매핑을 수동/DSL로 설정.
   - 장점: SQL을 완전히 제어할 수 있고 복잡한 쿼리(특히 DB 고유 기능)를 바로 사용하기 쉬움.
   - 단점: 문자열 기반이므로 런타임 오류(컬럼 오타, 타입 불일치)가 발생하기 쉬움.
- jOOQ:
   - 장점: 타입-세이프 DSL, 코드 생성으로 컬럼/테이블 타입 안전성 보장, 복잡한 SQL(CTE, 윈도우 함수 등)도 DSL로 표현 가능.
   - 단점: 처음 배우기에는 DSL 문법과 codegen 설정이 필요. 또한 코드 생성 단계가 추가됨.

5) JPA와의 비교
- JPA는 엔티티 중심(객체 중심)이며 ORM 계층에서 SQL이 추상화됩니다. 트랜잭션과 연관관계 관리에 강합니다.
- jOOQ는 SQL 중심이며, 필요한 경우 엔티티 매핑(POJO 변환)을 제공하지만 SQL 제어권은 개발자에게 있습니다.
- 언제 jOOQ를 선택하나:
   - 복잡한 SQL이 많고, 성능 튜닝/쿼리 최적화를 직접 제어하고 싶은 경우
   - DB 고유 기능(윈도우 함수, 고급 타입, 특정 문법)을 활용해야 할 경우

6) 마이그레이션 팁 (MyBatis/Dapper → jOOQ)
- 기존 SQL 재사용: MyBatis에서 쓰던 SQL을 jOOQ DSL로 점진적으로 옮길 수 있습니다. 우선 읽기 전용 쿼리부터 옮겨보세요.
- 매핑: MyBatis의 resultMap에 해당하는 매핑은 jOOQ의 fetchInto(POJO.class)를 사용해 간단히 대체할 수 있습니다.
- 복잡한 SQL: 복잡한 SQL은 jOOQ의 DSLContext.sql("raw sql")로 직접 실행하면서 Record 매핑을 받을 수도 있습니다. 즉, 필요하면 원시 SQL도 사용 가능합니다.

7) 테스트/CI 권장사항
- codegen 옵트인: 이 저장소처럼 프로파일을 사용해 codegen을 옵트인으로 구성하세요.
- DDL 기반 생성: CI에서 DB 접속이 어려우면 DDL 스크립트로 생성하도록 설정하세요. (jOOQ DDLDatabase)
- 통합 테스트: H2 같은 인메모리 DB로 기본 CRUD 통합 테스트를 구성하되, 실제 DB 고유 기능은 별도의 통합 환경에서 검증하세요.

8) 베스트 프랙티스
- 생성된 코드의 패키지/디렉터리를 명확히 관리하고, 모듈 경계에 맞게 의존성을 최소화하세요.
- Id 타입(숫자 vs UUID 등)은 일관되게 설계하고, 변환 정책(IdConverter 같은)을 적용하세요.
- 복잡한 쿼리는 재사용 가능한 메서드로 추출하고, 쿼리 빌더 패턴을 활용하세요.

9) 더 필요한 예제나 변환 지원이 있나요?
- 원하시면 MyBatis SQL 예제를 1~2개 주시면 동일한 쿼리를 jOOQ DSL로 변환해 드리겠습니다. 또한 DDL 기반 codegen 설정을 프로젝트에 추가해 드릴 수 있습니다.

