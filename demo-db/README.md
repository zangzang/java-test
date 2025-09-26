# Demo Database Project

이 프로젝트는 Java를 사용하여 PostgreSQL 데이터베이스와의 상호작용을 보여주는 데모 애플리케이션입니다. JPA (Hibernate)와 jOOQ를 사용하여 데이터베이스 작업을 수행하며, 성능 벤치마킹을 포함합니다.

## 기술 스택

- **Java**: 17
- **빌드 도구**: Maven
- **데이터베이스**: PostgreSQL
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
│   │       ├── application.properties # 설정 파일
│   │       └── persistence.xml        # JPA 설정 파일
│   └── test/
│       └── java/                      # 테스트 코드 (현재 없음)
└── target/
    └── generated-sources/jooq/        # jOOQ 생성 코드
        └── com/example/generated/     # 생성된 클래스들
```

## 설정

### 데이터베이스 설정

1. PostgreSQL을 설치하고 실행합니다.
2. 데이터베이스를 생성합니다:
   ```sql
   CREATE DATABASE testdb;
   CREATE USER juro WITH PASSWORD 'jurodb_-1q2w3e4r5t';
   GRANT ALL PRIVILEGES ON DATABASE testdb TO juro;
   ```

3. `src/main/resources/application.properties` 파일을 편집하여 데이터베이스 연결 정보를 설정합니다:
   ```properties
   db.url=jdbc:postgresql://localhost:5432/testdb
   db.user=juro
   db.password=jurodb_-1q2w3e4r5t
   app.iterations=1000
   ```

### jOOQ 코드 생성

이 프로젝트는 jOOQ를 사용하여 데이터베이스 스키마로부터 Java 코드를 자동 생성합니다. 생성된 코드는 `target/generated-sources/jooq` 디렉토리에 위치합니다.

jOOQ 코드 생성은 Maven 플러그인을 통해 수행되며, `pom.xml`에 다음과 같이 설정되어 있습니다:

- **데이터베이스 연결**: PostgreSQL 데이터베이스에 연결하여 스키마 정보를 읽습니다.
- **생성 대상**: 테이블, 레코드, DAO, POJO 클래스 등을 생성합니다.
- **패키지**: `com.example.generated`

코드 생성을 수동으로 실행하려면:
```bash
mvn generate-sources
```

빌드 시 자동으로 실행됩니다.

## 빌드 및 실행

### 빌드

프로젝트를 빌드하려면:
```bash
mvn clean compile
```

### 실행

애플리케이션을 실행하려면:
```bash
mvn exec:java -Prun
```

이 명령은 다음과 같은 작업을 수행합니다:
1. 데이터베이스 연결 설정
2. 테이블 초기화 (TRUNCATE)
3. JPA와 jOOQ를 사용한 데이터 삽입 벤치마킹
4. 데이터 조회 벤치마킹

## 벤치마크 결과 예시

실행 시 다음과 같은 성능 측정 결과를 볼 수 있습니다:

```
JPA INSERT: 10581ms
jOOQ INSERT: 6176ms
JPA LIST INSERT: 4928ms
jOOQ LIST INSERT: 136ms
JPA SELECT: 286ms, count=3120
jOOQ SELECT: 260ms, count=3120
```

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