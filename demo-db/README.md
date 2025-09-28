# Demo Database Project

이 프로젝트는 Java를 사용하여 PostgreSQL과 MySQL 데이터베이스와의 상호작용을 보여주는 데모 애플리케이션입니다. JPA (Hibernate)와 jOOQ를 사용하여 데이터베이스 작업을 수행하며, 성능 벤치마킹을 포함합니다.

## 기술 스택

- **Java**: 25
- **빌드 도구**: Maven
- **데이터베이스**: PostgreSQL, MySQL
- **ORM**: JPA (Hibernate)
- **SQL DSL**: jOOQ
- **의존성 관리**: Maven

## 프로젝트 구조

```
demo-db/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/
│   │   │       ├── Main.java          # 메인 애플리케이션 클래스
│   │   │       ├── entity/
│   │   │       │   └── User.java      # JPA 엔티티 클래스
│   │   │       └── dao/
│   │   │           ├── JpaUserDao.java # JPA DAO 클래스
│   │   │           └── JooqUserDao.java # jOOQ DAO 클래스
│   │   └── resources/
│   │       ├── application.properties # 설정 파일 (기본 PostgreSQL)
│   │       ├── application-mysql.properties # MySQL 설정 파일
│   │       └── persistence.xml        # JPA 설정 파일
│   └── test/
│       └── java/                      # 테스트 코드 (현재 없음)
└── target/
    └── generated-sources/jooq/        # jOOQ 생성 코드
        └── com/example/generated/     # 생성된 클래스들
```

## 설정

### 데이터베이스 설정

1. PostgreSQL과 MySQL을 설치하고 실행합니다.
2. PostgreSQL 데이터베이스를 생성합니다:
   ```sql
   CREATE DATABASE testdb;
   CREATE USER juro WITH PASSWORD 'jurodb_-1q2w3e4r5t';
   GRANT ALL PRIVILEGES ON DATABASE testdb TO juro;
   ```
3. MySQL 데이터베이스를 생성합니다:
   ```sql
   CREATE DATABASE juro;
   CREATE USER 'juro'@'%' IDENTIFIED BY 'jurodb_-1q2w3e4r5t';
   GRANT ALL PRIVILEGES ON juro.* TO 'juro'@'%';
   ```

### Maven 프로필

프로젝트는 Maven 프로필을 사용하여 PostgreSQL과 MySQL을 분리합니다:
- `run`: PostgreSQL용 (기본)
- `mysql`: MySQL용

각 프로필은 jOOQ 코드 생성과 실행 시 적절한 설정을 적용합니다.

## 빌드 및 실행

프로젝트는 Maven 프로필을 사용하여 PostgreSQL, MySQL, MSSQL을 분리합니다. 각 DB별로 컴파일과 실행을 분리하여 수행하세요.

### PostgreSQL 실행

```bash
mvn clean compile -Prun
mvn exec:java -Prun
```

### MySQL 실행

```bash
mvn clean compile -Pmysql
mvn exec:java -Pmysql -Ddb=mysql
```

**주의:** MySQL 실행 시 `-Ddb=mysql` 시스템 프로퍼티를 반드시 추가하세요. 그렇지 않으면 PostgreSQL 설정으로 실행되어 오류가 발생합니다.

### MSSQL 실행

```bash
mvn clean compile -Pmssql
mvn exec:java -Pmssql -Ddb=mssql
```

**주의:** MSSQL 실행 시 `-Ddb=mssql` 시스템 프로퍼티를 반드시 추가하세요. 그렇지 않으면 PostgreSQL 설정으로 실행되어 오류가 발생합니다.

### 패키징

PostgreSQL용 JAR:
```bash
mvn -Prun package
```

MySQL용 JAR은 현재 지원되지 않음 (필요 시 추가 가능).

## 벤치마크 결과

### 벤치마크 결과 (iterations=1000)

| DB | JPA INSERT | jOOQ INSERT | JPA LIST INSERT | jOOQ LIST INSERT | JPA SELECT (ms,count) | jOOQ SELECT (ms,count) |
|---:|---:|---:|---:|---:|---:|---:|
| PostgreSQL | 11325ms | 6340ms | 5211ms | 167ms | 280ms, 2340 | 292ms, 2340 |
| MySQL      | 32080ms | 16531ms | 5049ms | 11593ms | 401ms, 2340 | 323ms, 2340 |
| MSSQL      | 26152ms | 9292ms | 6003ms | 3484ms | 227ms, 2340 | 235ms, 2340 |

주의: MSSQL에서 이 환경의 jOOQ 빌드에는 SQLServer용 enum이 포함되어 있지 않아, `Main`은 런타임에 jOOQ dialect enum을 찾지 못할 경우 `DSL.using(conn)`으로 안전하게 폴백하여 DSL 기반 쿼리를 수행했습니다. (이 때문에 빌드 시 경고가 출력되었고, 현재는 폴백을 사용해 벤치를 성공적으로 실행했습니다.)

## 변경 이력(중요)

- 2025-09-27: `JooqDslUserDao` 제거
   - 이유: jOOQ 코드 생성(root-fix)을 적용하여 `JooqUserDao`(생성된 코드 기반 DAO)가 안정적으로 생성/컴파일되므로, 더 이상 프로젝트에 DSL 기반 대체 DAO가 필요하지 않아 제거했습니다.
   - 영향: 코드베이스가 단순화되었고, 벤치마크는 생성된 DAO를 사용해 수행됩니다.

- 2025-09-27: 종료 훅(Shutdown hook) 추가
   - `Main.java`에 애플리케이션 종료 시 JDBC 드라이버를 `DriverManager.deregisterDriver(...)`로 해제하고, MySQL의 `AbandonedConnectionCleanupThread`가 있으면 `checkedShutdown()`을 호출하도록 정리 로직을 추가했습니다.
   - 목적: Maven `exec:java` 실행 후에 남는 드라이버 관련 백그라운드 스레드 경고(`mysql-cj-abandoned-connection-cleanup`)를 제거하여 클린한 종료를 보장합니다.

   참고: 종료 훅은 JVM이 정상적으로 종료되거나 SIGTERM 등으로 종료될 때 실행됩니다. 강제 종료(SIGKILL)에서는 실행되지 않을 수 있습니다.

## 테스트(로컬 확인) — 권장 순서

아래 순서로 로컬에서 빌드와 벤치 실행을 하면 필요한 코드 생성 및 런타임 동작을 검증할 수 있습니다.

1. 클린 빌드 및 코드 생성 (Postgres 기본 프로필)

```powershell
mvn -f d:\ws\java-test\demo-db\pom.xml clean compile -Prun
```

2. 벤치 실행 (Postgres)

```powershell
mvn -f d:\ws\java-test\demo-db\pom.xml clean compile -Ppostgres
mvn -f d:\ws\java-test\demo-db\pom.xml exec:java -Prun -Ddb=postgres
```

3. MySQL 벤치 실행

```powershell
mvn -f d:\ws\java-test\demo-db\pom.xml clean compile -Pmysql
mvn -f d:\ws\java-test\demo-db\pom.xml exec:java -Pmysql -Ddb=mysql
```

4. MSSQL 벤치 실행

```powershell
mvn -f d:\ws\java-test\demo-db\pom.xml clean compile -Pmssql
mvn -f d:\ws\java-test\demo-db\pom.xml exec:java -Pmssql -Ddb=mssql
```

5. 확인 포인트

- 각 실행에서 프로그램이 정상 종료되면 `BUILD SUCCESS`가 출력되어야 합니다.
- 프로그램 종료 시 더 이상 `mysql-cj-abandoned-connection-cleanup` 관련 경고나 "thread was interrupted but is still alive" 같은 메시지가 없어야 합니다.

### 빠른 스모크 테스트 (CI용)

CI 환경에서는 DB 접근이 제한될 수 있으므로 간단한 컴파일-스모크를 추천합니다:

```powershell
mvn -f d:\ws\java-test\demo-db\pom.xml -DskipTests package
```

위 명령은 코드 생성 플러그인이 실행되는 경우에도 컴파일까지 확인합니다(네트워크가 필요한 경우 CI에서 별도 DB mock 또는 테스트 컨테이너가 필요할 수 있음).


### 성능 비교 요약

- 테이블/데이터량 관련
   - 벤치마크는 동일한 스키마(간단한 `users` 테이블)와 동일한 로우 수(실행 시 내부적으로 생성되는 레코드 수, 위의 SELECT 결과의 count=2340)를 사용했습니다. 따라서 DB별 성능 차이는 주로 데이터베이스 엔진의 처리 속도, 네트워크 레이턴시(같은 호스트 환경에서 발생할 경우 작지만 존재), JDBC 드라이버 최적화, 그리고 각 DB의 기본 설정(트랜잭션 처리, 디스크 동기화 등)에 기인합니다.

- INSERT (단건 삽입)
   - PostgreSQL이 MySQL보다 빠른 결과를 보였습니다. (Postgres 11325ms vs MySQL 32080ms). 이 차이는 MySQL의 JDBC 드라이버/서버 설정(예: fsync, binlog 동기화), 그리고 본 예제에서 사용한 MySQL 서버의 디스크/네트워크 성능 영향일 가능성이 큽니다.

- jOOQ LIST INSERT (배치/집단 삽입)
   - jOOQ의 배치 삽입 성능은 DB별로 크게 다릅니다. 이번 측정에서 PostgreSQL의 jOOQ LIST INSERT가 매우 빠른 편(167ms)인 반면 MySQL은 느린 편(11593ms)으로 보였습니다. 이 차이는 드라이버에서 배치 구현 방식, JDBC 드라이버의 기본 batch size, 그리고 MySQL 서버의 설정(autocommit, tx flush 정책 등)에 민감합니다.

- SELECT
   - 세 DB 모두 단일-스레드 기준 유사한 SELECT 응답 시간을 보였습니다(대략 200~400ms 범위). 이는 인메모리 캐시, 인덱스, 네트워크 레이턴시 등 여러 요인의 영향을 받습니다. 본 벤치의 목적은 CRUD 비교이므로 SELECT는 참고용입니다.

- 해석 및 권장
   - 배치 성능 차이를 줄이려면 MySQL에서 JDBC 배치 크기와 autocommit 설정을 조정해보세요. MySQL의 경우 커밋 빈도와 binlog 설정(binlog_format, sync_binlog 등)이 큰 영향을 줄 수 있습니다.
   - 테스트 결과는 환경(하드웨어, DB 설정, 네트워크)에 민감합니다. CI나 다른 머신에서 반복 측정을 권장합니다.

## 주요 기능

- **JPA DAO**: Hibernate를 사용한 객체-관계 매핑
- **jOOQ DAO**: 타입 안전한 SQL 쿼리 빌더
- **벤치마킹**: 개별 삽입 vs. 배치 삽입 성능 비교
- **다중 DB 지원**: Maven 프로필을 통한 PostgreSQL/MySQL 전환
- **설정 외부화**: Properties 파일을 통한 설정 관리

## 의존성

주요 의존성은 `pom.xml`에 정의되어 있습니다:

- PostgreSQL JDBC 드라이버
- MySQL JDBC 드라이버
- Hibernate ORM
- jOOQ 코어 및 코드 생성
- Build Helper Maven Plugin (생성된 소스 추가용)

## 주요 기능

- **JPA DAO**: Hibernate를 사용한 객체-관계 매핑
- **jOOQ DAO**: 타입 안전한 SQL 쿼리 빌더
- **벤치마킹**: 개별 삽입 vs. 배치 삽입 성능 비교
- **설정 외부화**: Properties 파일을 통한 설정 관리

## 의존성

주요 의존성은 `pom.xml`에 정의되어 있습니다:

- PostgreSQL JDBC 드라이버
- Hibernate ORM
- jOOQ 코어 및 코드 생성
- Build Helper Maven Plugin (생성된 소스 추가용)

## 참고 사항

- jOOQ 생성 코드는 빌드 시 자동으로 생성됩니다. 생성된 코드는 수동으로 수정하지 마시고, 스키마가 변경되면 `mvn generate-sources`를 실행하여 재생성하세요.

- 로그 경고: "Version mismatch" 메시지

  예시:

  > Version mismatch : Database version is older than what dialect POSTGRES supports: 16.9 (...)

  원인 및 영향
  - 원인: jOOQ의 코드 생성기는 연결한 DB의 버전을 자동 감지한 후, 내부에서 기대하는(또는 기본으로 설정된) PostgreSQL 방언 지원 버전과 비교합니다. 자동 감지된 버전이 기대 버전보다 낮거나 불일치하면 경고를 출력합니다.
  - 영향: 대개 정보성 경고이며 쿼리 생성/실행에 직접적인 문제를 일으키지 않습니다. 다만 CI에서 경고를 엄격히 검사하거나 로그 정리가 필요하면 조치하세요.

해결 방법 (두 가지)

1) 권장 - `databaseVersion` 명시 (간단하고 안전)

   - `pom.xml`의 jOOQ codegen 설정에서 `<generator><database>` 아래에 다음을 추가하세요:

```xml
<properties>
  <property>
    <key>databaseVersion</key>
    <value>16</value>
  </property>
</properties>
```

   - 적용 후: `mvn generate-sources` 또는 `mvn compile`을 실행하여 경고가 사라지는지 확인하세요.

2) 선택 - jOOQ 업그레이드

   - 설명: jOOQ의 최신 안정 버전으로 업그레이드하면 더 넓은 DB 버전 범위를 지원합니다. `pom.xml`에서 `jooq`, `jooq-meta`, `jooq-codegen-maven` 등의 버전을 최신 안정 버전으로 올리면 경고가 해결될 수 있습니다.

   - 검증: 의존성 버전 변경 후 `mvn clean compile`로 경고 여부를 확인하세요.

   - 실제 변경 예시:

```xml
<!-- core libraries -->
<dependency>
  <groupId>org.jooq</groupId>
  <artifactId>jooq</artifactId>
  <version>3.20.3</version>
</dependency>
<dependency>
  <groupId>org.jooq</groupId>
  <artifactId>jooq-meta</artifactId>
  <version>3.20.3</version>
</dependency>

<!-- codegen plugin -->
<plugin>
  <groupId>org.jooq</groupId>
  <artifactId>jooq-codegen-maven</artifactId>
  <version>3.20.3</version>
  <!-- existing configuration ... -->
</plugin>
```

   - 확인 절차 (Windows PowerShell):

```powershell
mvn -q clean generate-sources
mvn -q compile
```

   - 위 명령 후 로그에서 "Version mismatch" 메시지가 사라졌는지 확인하세요. 만약 남아있다면 `databaseVersion`을 명시하는 방법을 함께 사용하세요.

- 결론: 이 경고는 대부분 무해합니다. 빠른 해결을 원하면 `databaseVersion`을 명시하세요. 조직 정책상 의존성 업그레이드가 적절하면 jOOQ를 업그레이드하세요.

## 추가 팁: 빌드 로그에서 jOOQ 'Version mismatch' 경고 숨기기

경고가 정보성이고 실제 동작에 영향이 없다면, CI나 로컬 빌드 출력에서 해당 라인을 필터링할 수 있습니다. 예를 들어 Windows PowerShell에서는 다음과 같이 필터링할 수 있습니다:

```powershell
mvn -Prun -DskipTests package 2>&1 | Select-String -NotMatch 'Version mismatch'
```

이 명령은 빌드 출력을 파이프라인으로 넘겨 'Version mismatch' 문자열을 포함하는 라인을 제거합니다. 경고를 근본적으로 제거하려면 README에 설명된 "databaseVersion" 설정 또는 jOOQ 업그레이드를 적용하세요.

- 이 프로젝트는 데모 목적입니다. 프로덕션 환경에서는 DB 마이그레이션 전략 및 의존성 관리 정책을 수립하세요.