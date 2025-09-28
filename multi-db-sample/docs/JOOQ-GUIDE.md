# jOOQ Guide for MyBatis/Dapper users

이 문서는 MyBatis 또는 Dapper에 익숙한 개발자가 JPA 대신 jOOQ를 도입할 때 참고할 수 있는 심화 가이드입니다. 코드 생성(codegen), DSL 사용, 마이그레이션 전략, UUID/ID 처리, 테스트/CI 권장사항, 그리고 실습 커맨드를 포함합니다.

## 목차

1. 소개 및 핵심 철학
2. 코드 생성 옵션
   - 데이터베이스 접속 기반
   - DDL 스크립트(DDLDatabase)
3. 프로젝트 설정 예 (Maven)
4. DSL 실전 예제
   - 기본 CRUD
   - JOIN, CTE, 윈도우 함수 예
   - 원시 SQL 사용
5. MyBatis/Dapper → jOOQ 마이그레이션 전략
6. UUID 및 ID 처리
7. 테스트 및 CI 적용 권장사항
8. 보안 및 운영 주의사항
9. 실습: 로컬에서 codegen 실행하기
10. 예제 변환 요청 방법

---

## 1. 소개 및 핵심 철학

jOOQ는 SQL을 자바 코드로 안전하게 표현하는 라이브러리입니다. 두 가지 주요 요소가 결합됩니다:

- Code generation: 데이터베이스 스키마를 기반으로 테이블/레코드/타입에 대한 Java 클래스를 생성합니다.
- DSL: 타입-세이프 SQL 빌더로서 복잡한 SQL(CTE, 윈도우 함수, 커스텀 타입 등)을 자바 코드로 표현합니다.

jOOQ는 ORM이 아니라 SQL-중심 접근법을 제공합니다. SQL 제어권을 개발자가 유지하면서도 컴파일 타임에 많은 오류를 잡아낼 수 있는 장점이 있습니다.

## 2. 코드 생성 옵션

jOOQ 코드 생성은 크게 두 가지 방식으로 수행할 수 있습니다.

A) 데이터베이스 접속 기반 (JDBC)
- jOOQ 플러그인이 JDBC로 실제 DB에 접속하여 메타데이터를 읽고 Java 코드를 생성합니다.
- 장점: 실제 DB 상태를 그대로 반영합니다.
- 단점: 빌드 시 DB 접속이 필요하므로 CI/다른 개발자 환경에서 제한이 있을 수 있습니다.

B) DDL 스크립트 기반 (DDLDatabase / ScriptDatabase)
- 로컬에 저장된 DDL(SQL 스크립트)을 기반으로 코드 생성합니다.
- 장점: DB 접속 불필요. CI에서 유용.
- 단점: DDL 스크립트가 DB의 실제 상태와 일치하도록 관리해야 합니다.

예: DDLDatabase 설정 스니펫 (pom.xml의 codegen 프로파일 안에서 사용)

```xml
<generator>
  <database>
    <name>org.jooq.meta.extensions.ddl.DDLDatabase</name>
    <properties>
      <property>
        <key>scripts</key>
        <value>${project.basedir}/src/main/resources/db/schema.sql</value>
      </property>
      <property>
        <key>sort</key>
        <value>semantic</value>
      </property>
    </properties>
  </database>
  <target>
    <packageName>com.example.entity.generated</packageName>
    <directory>src/main/java</directory>
  </target>
</generator>
```

## 3. 프로젝트 설정 예 (Maven)

- `postgres-module/pom.xml`에 jOOQ 코드 생성 플러그인을 `codegen` 프로파일로 추가합니다(이미 이 프로젝트에 적용되어 있음).
- 플러그인 설정은 JDBC 연결 문자열을 프로퍼티로 치환하여 `-D`로 안전하게 덮어쓸 수 있도록 구성하세요.

예:

```xml
<properties>
  <jooq.version>3.18.0</jooq.version>
</properties>

<profiles>
  <profile>
    <id>codegen</id>
    <build>
      <plugins>
        <plugin>
          <groupId>org.jooq</groupId>
          <artifactId>jooq-codegen-maven</artifactId>
          <version>${jooq.version}</version>
          <configuration>
            <jdbc>
              <driver>org.postgresql.Driver</driver>
              <url>${jooq.codegen.jdbc.url}</url>
              <user>${jooq.codegen.jdbc.user}</user>
              <password>${jooq.codegen.jdbc.password}</password>
            </jdbc>
            <generator> ... </generator>
          </configuration>
          <executions>
            <execution>
              <goals>
                <goal>generate</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

## 4. DSL 실전 예제

기본 CRUD 예는 README에도 있지만, 조금 더 구체적으로 소개합니다.

- Imports
```java
import static com.example.entity.generated.Tables.USER;
import org.jooq.DSLContext;
import com.example.entity.generated.tables.records.UserRecord;
```

- 조회 (조건, 페이징)
```java
List<UserRecord> list = dsl.selectFrom(USER)
    .where(USER.NAME.likeIgnoreCase("%lee%"))
    .orderBy(USER.CREATED_AT.desc())
    .limit(50)
    .offset(0)
    .fetchInto(UserRecord.class);
```

- 단건 조회
```java
UserRecord r = dsl.selectFrom(USER)
    .where(USER.ID.eq(42))
    .fetchOneInto(UserRecord.class);
```

- 삽입 (generated key가 있는 경우)
```java
UserRecord newR = dsl.newRecord(USER);
newR.setName("alice");
newR.setEmail("alice@example.com");
newR.store(); // newR.getId()로 반환된 키 접근 가능
```

- 배치 삽입
```java
dsl.batch(
  dsl.insertInto(USER).columns(USER.NAME, USER.EMAIL).values((String) null, (String) null)
).bind("a","a@x").bind("b","b@x").execute();
```

- JOIN과 매핑
```java
// 예: user 와 profile 테이블 join 후 DTO로 매핑
var result = dsl.select()
    .from(USER)
    .join(PROFILE).on(PROFILE.USER_ID.eq(USER.ID))
    .where(USER.ACTIVE.eq(true))
    .fetch(r -> new UserDTO(
        r.get(USER.ID),
        r.get(USER.NAME),
        r.get(PROFILE.AVATAR_URL)
    ));
```

- CTE와 윈도우 함수 (간단 예)
```java
// 예: 최근 일주일 가입자들을 가입일자 기준으로 순위를 매기고 상위 N을 가져오기
import static org.jooq.impl.DSL.*;

var recent = select(USER.ID, USER.CREATED_AT)
    .from(USER)
    .where(USER.CREATED_AT.greaterThan(currentTimestamp().minus(days(7))))
    .asTable("recent_users");

var ranked = select(rowNumber().over(orderBy(recent.field(USER.CREATED_AT).desc())).as("rn"),
        recent.field(USER.ID))
    .from(recent)
    .fetch();
// 필요시 rn 칼럼을 기준으로 필터링하거나 랭킹 기반 페이징을 적용하세요.
```

- 원시 SQL 실행 (필요 시)
```java
Result<Record> r = dsl.resultQuery("SELECT id, name FROM users WHERE name = ?", "bob").fetch();
```

## 5. MyBatis/Dapper → jOOQ 마이그레이션 전략

1. 우선 읽기 전용 쿼리부터 이전: 복잡한 읽기 쿼리(보고서용)부터 jOOQ DSL로 옮기면 영향범위가 작습니다.
2. 매핑 재사용: MyBatis의 resultMap을 jOOQ의 `fetchInto(POJO.class)`로 대체.
3. 트랜잭션/세션 관리: MyBatis에서 쓰던 트랜잭션 방식과 통일시키기. jOOQ는 기본적으로 `DSLContext`를 통해 커넥션을 사용합니다. Spring과 함께 쓸 경우 DataSourceTransactionManager와 함께 사용.
4. 점진적 전환: 비즈니스 로직에서 jOOQ와 기존 매퍼를 병행 사용하고, 문제없이 동작하면 기존 코드를 제거.

## 6. UUID 및 ID 처리

- jOOQ는 컬럼 타입(UUID 등)을 잘 지원합니다. 생성된 `USER.ID` 필드를 `UUID.class`로 캐스팅하거나, codegen에서 UUID 타입 매핑을 명시하면 됩니다.
- 프로젝트에서 다양한 ID 타입을 지원하려면 `IdConverter<T>` 같은 전략을 도입하여 DAO 계층에서 통일된 방식으로 변환하세요(이 레포에는 이미 구현되어 있음).

### UUID 예제 (PostgreSQL uuid PK)

실제 예제 — PostgreSQL에서 `uuid` 타입을 PK로 사용하는 경우:

```java
// 테이블 예시: CREATE TABLE users (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name text, email text);
import java.util.UUID;
import static com.example.entity.generated.Tables.USER;

// 새 UUID를 생성해서 삽입
UUID newId = UUID.randomUUID();
dsl.insertInto(USER)
   .set(USER.ID, newId)
   .set(USER.NAME, "kim")
   .set(USER.EMAIL, "kim@example.com")
   .execute();

// 조회
UserRecord r = dsl.selectFrom(USER)
    .where(USER.ID.eq(newId))
    .fetchOneInto(UserRecord.class);
```

만약 codegen에서 UUID가 올바르게 매핑되지 않는다면 `forcedTypes`를 사용해 강제 매핑할 수 있습니다. 예:

```xml
<forcedTypes>
  <forcedType>
    <name>UUID</name>
    <includeTypes>uuid</includeTypes>
    <includeExpression>.*\.id</includeExpression>
  </forcedType>
</forcedTypes>
```

## 7. 테스트 및 CI 적용 권장사항

- Codegen을 기본 빌드에서 분리하고 `-Pcodegen`으로 옵트인하세요.
- CI에서 생성이 필요하면 두 가지 방식:
  1. CI에서 접근 가능한 DB를 구성하고 codegen 프로파일을 실행
+  2. 또는 DDL 스크립트를 이용해 codegen(권장)
- 통합 테스트: H2와 같은 인메모리 DB로 기본 CRUD를 검증하되, 실제 DB 특화 기능은 별도의 환경에서 검증하세요.

## 8. 보안 및 운영 주의사항

- DB 자격 증명(특히 비밀번호)은 POM에 하드코딩하지 마세요. CI 시크릿 또는 환경 변수로 관리하세요.
- 코드 생성이 자동으로 실행되면 의도치 않게 DB에 접근할 수 있으므로 반드시 옵트인 방식으로 구성하세요.

## 9. 실습: 로컬에서 codegen 실행하기

PowerShell 예제 (JDBC 기반):

```powershell
mvn -pl postgres-module -Pcodegen -Djooq.codegen.jdbc.url="jdbc:postgresql://localhost:5432/testdb" -Djooq.codegen.jdbc.user=postgres -Djooq.codegen.jdbc.password=password
```

DDL 기반(스크립트)으로 실행하려면 `postgres-module`의 `codegen` 프로파일에서 generator/database 이름을 `org.jooq.meta.extensions.ddl.DDLDatabase`로 변경하고 `scripts` 프로퍼티를 지정하세요.

## 10. 예제 변환 요청 방법

원하시면 MyBatis/Dapper의 SQL 예제를 1~2개 보내 주세요. 해당 SQL을 jOOQ DSL로 변환해 드리고, 가능한 경우 프로젝트에 바로 적용 가능한 코드 스니펫으로 제공하겠습니다.

---

문서 추가를 완료했습니다. 이제 변경사항을 검토하시고, (A) 실제 SQL 예제 1~2개를 주실지, (B) DDL 기반 codegen 프로파일을 `postgres-module/pom.xml`에 추가할지, (C) CI 워크플로 예시를 생성할지 알려 주세요.