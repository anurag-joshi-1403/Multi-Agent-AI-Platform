<div align="center">

<img src="Frontend/public/logo.svg" width="88" alt="Multi-Agent AI Platform logo" />

# 🗄️ Accounts in a Database

**Store sign-ups in a database, and check every login against it — every change shows exactly where it goes.**

[![SQL](https://img.shields.io/badge/SQL-H2%20%C2%B7%20PostgreSQL-336791?logo=postgresql&logoColor=white)](#-choose-your-database)
[![MongoDB](https://img.shields.io/badge/MongoDB-optional-47A248?logo=mongodb&logoColor=white)](#-choose-your-database)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-BCrypt-6db33f?logo=springsecurity&logoColor=white)](#-golden-rules)
[![Status](https://img.shields.io/badge/MongoDB-set%20up%20%E2%9C%85-34d399)](#-done-when)

</div>

> [!NOTE]
> ✅ **The MongoDB path (Option B) is built and checked** (Oct 10, 2026): sign-up saves the account in MongoDB, and
> every login checks it there. The code below matches what is in the repo, so this page doubles as an explanation of
> it — and as the recipe for the SQL path, which is not built. `AUTH_USERS` is gone.
> Before: accounts came from `AUTH_USERS` in `Backend/.env` and lived in memory.

---

## 📌 How to read every step

| You see | It means | You do |
|---|---|---|
| 🆕 **New file** | The file does not exist yet | Create it at the path shown, paste the **whole** block |
| 📍 **Find** | Text that is **already** in your file | <kbd>Ctrl</kbd>+<kbd>F</kbd> in VS Code and type part of it to jump there |
| ✂️ **Paste below** | New lines | Click at the end of the found text, press <kbd>Enter</kbd>, paste |
| ✂️ **Paste above** | New lines | Click at the start of the found text, paste, press <kbd>Enter</kbd> |
| ✏️ **Replace** | A new version of the found text | Select the found text, paste over it |
| 🗑️ **Delete** | Text to remove | Select it, press <kbd>Delete</kbd> |

**Three Java rules that prevent most mistakes:**

1. **Inside the class** means between `public class Something {` and the **very last** `}` of the file.
2. **Imports** go at the top, under the `package …;` line. In VS Code, <kbd>Shift</kbd>+<kbd>Alt</kbd>+<kbd>O</kbd> adds missing imports and removes unused ones.
3. **One class per file**, and the file name must match: `public class UserStore` lives in `UserStore.java`.

**Creating a new file in VS Code:** right-click the folder in the Explorer → *New File…* → type the name
(e.g. `UserAccount.java`) → paste the code. To make the `user` folder: right-click
`multi_agent_ai_platform` → *New Folder…* → `user`.

---

## 🧭 Choose your database

| Database | Install | Manage by hand with | Effort |
|---|---|---|---|
| 🟦 **H2** (already connected, `Backend/data/`) | Nothing | DBeaver (stop the backend first) | ⭐ easiest |
| 🐘 **PostgreSQL** | PostgreSQL server | pgAdmin or DBeaver | ⭐⭐ recommended SQL |
| 🐬 **MySQL** | MySQL server | MySQL Workbench | ⭐⭐⭐ `schema.sql` needs a MySQL version |
| 🍃 **MongoDB** | MongoDB server or Atlas | MongoDB Compass | ⭐⭐ accounts only, next to H2 |

---

## 🛣️ The route — do the steps in this order

| # | Step | File | SQL | MongoDB |
|---|---|---|---|---|
| 1 | Install / connect the database | `pom.xml`, `.env`, `application.properties` | **A1** | **B1–B4** |
| 2 | Users table | `schema.sql` | **A2** | — |
| 3 | Account + store interface | 🆕 `user/UserAccount`, `UserStore`, `UsernameTakenException` | **3** | **3** |
| 4 | The store | 🆕 `user/JdbcUserStore` · 🆕 `user/UserDocument`, `UserRepository`, `MongoUserStore` | **A4** | **B5–B7** |
| 5 | Login reads the database | 🆕 `user/DatabaseUserDetailsService` | **5** | **5** |
| 6 | Open sign-up, drop `AUTH_USERS` | ✏️ `config/SecurityConfig` | **6** | **6** |
| 7 | Sign-up input | 🆕 `web/dto/SignupRequest` | **7** | **7** |
| 8 | Sign-up endpoint | ✏️ `web/AuthController` | **8** | **8** |
| 9 | "Username taken" → 409 | ✏️ `web/ApiExceptionHandler` | **9** | **9** |
| 10 | Try it | PowerShell | **10** | **10** |
| 11 | Fix the tests | ✏️ 3 test files | **11** | **11** |
| 12 | Sign-up form | ✏️ 4 frontend files | **12** | **12** |

> ⚠️ The backend only starts again once steps **3–9 are all done** — before that, some classes point at others that
> don't exist yet. That's normal.

```mermaid
flowchart LR
    S["📝 Sign up"] -->|"POST /api/auth/signup"| C["🌐 AuthController"]
    L["🔐 Log in"] -->|"POST /api/auth/login"| C
    C -->|"hash password"| P["🔒 PasswordEncoder"]
    C --> U["👥 UserStore"]
    C --> AM["🛡️ AuthenticationManager"] --> D["🔎 DatabaseUserDetailsService"] --> U
    U --> DB[("🗄️ SQL or MongoDB")]
```

<details>
<summary>🔎 How one login is checked</summary>

```mermaid
sequenceDiagram
    actor Y as 👤 You
    participant A as 🌐 AuthController
    participant M as 🛡️ AuthenticationManager
    participant D as 🔎 DatabaseUserDetailsService
    participant DB as 🗄️ Database
    Y->>A: username + password
    A->>M: authenticate
    M->>D: loadUserByUsername
    D->>DB: find user by username
    DB-->>D: username + password hash
    D-->>M: user details
    M->>M: BCrypt: does the password match the hash?
    M-->>A: ✅ match → session · ❌ no match → 401
    A-->>Y: signed in
```

</details>

### 📁 Where the files live

```text
Backend/
├── pom.xml                          ✏️ A1 / B2
├── .env                             ✏️ A1 / B3
└── src/
    ├── main/resources/
    │   ├── schema.sql               ✏️ A2      (SQL only)
    │   └── application.properties   ✏️ B4      (MongoDB only)
    ├── main/java/com/project/multi_agent_ai_platform/
    │   ├── agent/  config/  document/  web/      ← already there
    │   ├── user/                    🆕 new folder, next to web/ and config/
    │   ├── config/SecurityConfig.java            ✏️ 6
    │   └── web/AuthController.java · ApiExceptionHandler.java · dto/SignupRequest.java   ✏️ 8 · ✏️ 9 · 🆕 7
    ├── test/resources/application.properties     ✏️ B4  (MongoDB only)
    └── test/java/com/project/multi_agent_ai_platform/
        ├── config/SecurityConfigTest.java        ✏️ 11
        └── web/SignedInWebTest.java · AuthIntegrationTest.java   ✏️ 11
Frontend/src/  api/client.ts · hooks/useAuth.ts · App.tsx · pages/LoginPage.tsx   ✏️ 12
```

---

## 🅰️ SQL only — steps A1, A2

### 🔌 A1 · Connect the database

🟦 **H2:** already connected — skip to **A2**.

🐘 **PostgreSQL:** install it and create a database named `agents` (pgAdmin → *Databases* → *Create*). Then:

📄 **`Backend/pom.xml`** — 📍 **Find:**

```xml
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency>
```

✂️ **Paste below:**

```xml
		<!-- PostgreSQL driver -->
		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>
```

📄 **`Backend/.env`** — ✂️ **paste at the end** (`application.properties` already reads these three):

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/agents
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your-database-password
```

🐬 **MySQL:** same idea with `com.mysql:mysql-connector-j` and `jdbc:mysql://localhost:3306/agents` — but first change
`TIMESTAMP WITH TIME ZONE` to `DATETIME(6)` and `CREATE INDEX IF NOT EXISTS` to `CREATE INDEX` in `schema.sql`.

### 🧱 A2 · The users table

📄 **`Backend/src/main/resources/schema.sql`** — ✂️ **paste at the very end** of the file:

```sql
-- Console accounts. Only the BCrypt hash of a password is ever stored.
CREATE TABLE IF NOT EXISTS platform_users (
    id            VARCHAR(64)  NOT NULL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);
```

➡️ Next: **step 3**, then **A4**.

---

## 🅱️ MongoDB only — steps B1–B4

### 🍃 B1 · Install

MongoDB Community Server + **MongoDB Compass** — or a free **MongoDB Atlas** cluster. The click-by-click version is
in [TASKS.md → MongoDB accounts](TASKS.md#-mongodb-accounts--manual-setup).

### ➕ B2 · The dependency

📄 **`Backend/pom.xml`** — 📍 **Find** these two blocks (around line 66):

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-jdbc</artifactId>
		</dependency>
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency>
```

✂️ **Paste below** them:

```xml
		<!-- Accounts in MongoDB (the "users" collection) -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-mongodb</artifactId>
		</dependency>
```

> ⚠️ **Keep the H2 block** — uploads and chat memory still live in H2. And **no `<scope>`** on the MongoDB one: with
> `runtime`, the MongoDB classes in steps B5–B7 would not compile.

### 🔑 B3 · The address

📄 **`Backend/.env`** — ✂️ **paste at the end** (one line):

```properties
MONGODB_URI=mongodb://localhost:27017/agents
```

(With a database user: `mongodb://agents_app:YOUR-PASSWORD@localhost:27017/agents?authSource=agents` · Atlas: the
`mongodb+srv://…/agents?…` string from Atlas → *Connect*.)

### ⚙️ B4 · The settings

📄 **`Backend/src/main/resources/application.properties`** (the **main** one) — 📍 **Find** the last line of the file:

```properties
spring.ai.chat.memory.repository.jdbc.initialize-schema=always
```

✂️ **Paste below:**

```properties

# --- Accounts (MongoDB) ------------------------------------------------------
# The address comes from MONGODB_URI in Backend/.env
spring.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/agents}
# Create the unique index on username at startup, so the database refuses duplicate accounts
spring.data.mongodb.auto-index-creation=true
```

📄 **`Backend/src/test/resources/application.properties`** (the **test** one) — ✂️ **paste at the end** (one line):

```properties
# Tests use their own database, so they never touch your real accounts
spring.mongodb.uri=mongodb://localhost:27017/agents_test
```

> ⚠️ Only that one line in the test file — no `${…}` there, and no `auto-index-creation` (it would make every test
> need MongoDB just to start).
> ✅ Checked against Spring Boot 4.1.1: `spring.mongodb.uri` is the name (the old `spring.data.mongodb.uri` is
> deprecated), and `spring.data.mongodb.auto-index-creation` is right. MongoDB must be running **before** the backend
> starts: the index is created at startup.

➡️ Next: **step 3**, then **B5–B7**.

---

## 👥 Step 3 · The account and the store interface — both databases

Create the folder **`user`** inside `Backend/src/main/java/com/project/multi_agent_ai_platform/`, then three files:

🆕 **`user/UserAccount.java`**

```java
package com.project.multi_agent_ai_platform.user;

import java.time.Instant;

/** One console account, as stored in the database. */
public record UserAccount(String id, String username, String passwordHash, String role, Instant createdAt) {
}
```

🆕 **`user/UserStore.java`**

```java
package com.project.multi_agent_ai_platform.user;

import java.util.Optional;

/** Where accounts live. Exactly one implementation: JdbcUserStore (SQL) or MongoUserStore (MongoDB). */
public interface UserStore {

	Optional<UserAccount> findByUsername(String username);

	/** @throws UsernameTakenException when the username is already registered */
	UserAccount create(String username, String passwordHash);
}
```

🆕 **`user/UsernameTakenException.java`**

```java
package com.project.multi_agent_ai_platform.user;

/** Sign-up with a username that already exists. Mapped to HTTP 409. */
public class UsernameTakenException extends RuntimeException {

	public UsernameTakenException(String username) {
		super("Username '" + username + "' is already registered");
	}
}
```

---

## 🅰️ A4 · The SQL store — SQL only

<details>
<summary>🆕 <b><code>user/JdbcUserStore.java</code></b> — click to open the code</summary>

```java
package com.project.multi_agent_ai_platform.user;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Accounts in the platform_users table (schema.sql). */
@Component
public class JdbcUserStore implements UserStore {

	private final JdbcClient jdbc;

	public JdbcUserStore(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<UserAccount> findByUsername(String username) {
		return jdbc.sql("SELECT id, username, password_hash, role, created_at FROM platform_users WHERE username = ?")
			.param(username)
			.query((rs, rowNum) -> new UserAccount(rs.getString("id"), rs.getString("username"),
					rs.getString("password_hash"), rs.getString("role"),
					rs.getObject("created_at", OffsetDateTime.class).toInstant()))
			.optional();
	}

	@Override
	public UserAccount create(String username, String passwordHash) {
		UserAccount user = new UserAccount(UUID.randomUUID().toString(), username, passwordHash, "USER",
				Instant.now().truncatedTo(ChronoUnit.MICROS));
		try {
			jdbc.sql("INSERT INTO platform_users (id, username, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?)")
				.params(user.id(), user.username(), user.passwordHash(), user.role(),
						OffsetDateTime.ofInstant(user.createdAt(), ZoneOffset.UTC))
				.update();
		}
		catch (DuplicateKeyException ex) { // the UNIQUE column caught a duplicate, even two sign-ups at once
			throw new UsernameTakenException(username);
		}
		return user;
	}
}
```

</details>

➡️ Next: **step 5**.

---

## 🅱️ B5–B7 · The MongoDB store — MongoDB only

<details>
<summary>🆕 <b><code>user/UserDocument.java</code></b> (B5) — click to open the code</summary>

```java
package com.project.multi_agent_ai_platform.user;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** One account in the "users" collection. */
@Document("users")
public class UserDocument {

	@Id
	private String id;

	@Indexed(unique = true)
	private String username;

	private String passwordHash;

	private String role = "USER";

	private Instant createdAt = Instant.now();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
```

</details>

<details>
<summary>🆕 <b><code>user/UserRepository.java</code></b> (B6) — click to open the code</summary>

```java
package com.project.multi_agent_ai_platform.user;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Spring Data writes the code for these methods from their names. */
public interface UserRepository extends MongoRepository<UserDocument, String> {

	Optional<UserDocument> findByUsername(String username);

	boolean existsByUsername(String username);
}
```

</details>

<details>
<summary>🆕 <b><code>user/MongoUserStore.java</code></b> (B7) — click to open the code</summary>

```java
package com.project.multi_agent_ai_platform.user;

import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/** Accounts in MongoDB. */
@Component
public class MongoUserStore implements UserStore {

	private final UserRepository repository;

	public MongoUserStore(UserRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<UserAccount> findByUsername(String username) {
		return repository.findByUsername(username).map(MongoUserStore::toAccount);
	}

	@Override
	public UserAccount create(String username, String passwordHash) {
		// The check gives a clear answer in the common case; the unique index on username is what
		// stops two sign-ups with the same name that arrive at the same moment.
		if (repository.existsByUsername(username)) {
			throw new UsernameTakenException(username);
		}
		UserDocument doc = new UserDocument();
		doc.setUsername(username);
		doc.setPasswordHash(passwordHash);
		try {
			return toAccount(repository.save(doc));
		}
		catch (DuplicateKeyException ex) {
			throw new UsernameTakenException(username);
		}
	}

	private static UserAccount toAccount(UserDocument d) {
		return new UserAccount(d.getId(), d.getUsername(), d.getPasswordHash(), d.getRole(), d.getCreatedAt());
	}
}
```

</details>

➡️ Next: **step 5**.

---

## 🔗 Steps 5–12 — the same for SQL and MongoDB

### 🔎 Step 5 · Login reads the database

<details>
<summary>🆕 <b><code>user/DatabaseUserDetailsService.java</code></b> — click to open the code</summary>

```java
package com.project.multi_agent_ai_platform.user;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/** What Spring Security calls on every login: find the account; it then checks the password hash. */
@Component
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserStore users;

	public DatabaseUserDetailsService(UserStore users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		UserAccount account = users.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException(username));
		return User.withUsername(account.username())
			.password(account.passwordHash())
			.roles(account.role()) // USER, ADMIN… — without the ROLE_ prefix
			.build();
	}
}
```

</details>

### 🛡️ Step 6 · `SecurityConfig.java`

📄 **`Backend/src/main/java/com/project/multi_agent_ai_platform/config/SecurityConfig.java`** — two changes.

**6a · Open the sign-up route.** 📍 **Find** (around line 59):

```java
				.requestMatchers("/api/auth/login", "/api/auth/logout", "/actuator/health", "/error")
```

✏️ **Replace** with — every path starts with **`/`**:

```java
				.requestMatchers("/api/auth/login", "/api/auth/signup", "/api/auth/logout", "/actuator/health", "/error")
```

**6b · Remove the `AUTH_USERS` accounts.** 📍 **Find** the method that starts with (around line 101):

```java
	@Bean
	UserDetailsService userDetailsService(PlatformProperties properties, PasswordEncoder encoder) {
```

🗑️ **Delete** it — from that `@Bean` down to its closing `}` (about 15 lines). The next thing below it must be:

```java
	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
```

✋ **Keep** `authenticationManager` and `passwordEncoder` — they pick up the new service from step 5 by themselves.
In the repo, the old method's helpers (`parseUsers`, `Credential`, `randomPassword`) are deleted too, together with
`platform.auth.users` in `application.properties`, the `Auth` part of `PlatformProperties` and `AUTH_USERS` in
`.env.example` — nothing reads them any more.

### 📦 Step 7 · The sign-up input

<details>
<summary>🆕 <b><code>web/dto/SignupRequest.java</code></b> — click to open the code</summary>

```java
package com.project.multi_agent_ai_platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of POST /api/auth/signup. */
public record SignupRequest(
		@NotBlank @Size(min = 3, max = 50)
		@Pattern(regexp = "[A-Za-z0-9._-]+", message = "letters, digits, dot, dash and underscore only") String username,
		@NotBlank @Size(min = 8, max = 200, message = "at least 8 characters") String password) {

	/** Never print the password, e.g. in a log line. */
	@Override
	public String toString() {
		return "SignupRequest[username=" + username + ", password=***]";
	}
}
```

</details>

### 🌐 Step 8 · `AuthController.java` — replace the whole file

📄 **`Backend/src/main/java/com/project/multi_agent_ai_platform/web/AuthController.java`** — this one has changes in
four places, so it's easier to swap the file: ✏️ <kbd>Ctrl</kbd>+<kbd>A</kbd> in the file, then paste.

<details>
<summary>📄 The complete new <code>AuthController.java</code> — click to open the code</summary>

```java
package com.project.multi_agent_ai_platform.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.project.multi_agent_ai_platform.user.UserStore;
import com.project.multi_agent_ai_platform.web.dto.AuthResponse;
import com.project.multi_agent_ai_platform.web.dto.LoginRequest;
import com.project.multi_agent_ai_platform.web.dto.SignupRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Session-cookie login for the console. Every other {@code /api} route needs the session this
 * starts (see {@link com.project.multi_agent_ai_platform.config.SecurityConfig}).
 * <pre>
 *   POST /api/auth/signup   create an account, start a session            -> 201 {username} or 409
 *   POST /api/auth/login    check username + password, start a session   -> 200 {username}
 *   POST /api/auth/logout   end the session (works without one too)      -> 204
 *   GET  /api/auth/me       who the session belongs to                    -> 200 {username} or 401
 * </pre>
 */
@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

	private final AuthenticationManager authenticationManager;

	private final SecurityContextRepository securityContextRepository;

	private final UserStore users;

	private final PasswordEncoder passwordEncoder;

	public AuthController(AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository, UserStore users, PasswordEncoder passwordEncoder) {
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	/** Create an account (password stored as a BCrypt hash), then sign straight in. */
	@PostMapping(path = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse signup(@Valid @RequestBody SignupRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		users.create(body.username(), passwordEncoder.encode(body.password()));
		return login(new LoginRequest(body.username(), body.password()), request, response);
	}

	/** A wrong username or password throws here and becomes the same generic {@code 401} either way. */
	@PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
	public AuthResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		Authentication result = authenticationManager
			.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));

		// A session id that existed before login must not survive it (session fixation)
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(result);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);

		return new AuthResponse(result.getName());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
	}

	@GetMapping("/me")
	public AuthResponse me(Authentication authentication) {
		// Only reachable with a session: SecurityConfig answers 401 before this runs otherwise
		return new AuthResponse(authentication.getName());
	}
}
```

</details>

What changed, so you can see it: 1️⃣ three new imports (`PasswordEncoder`, `UserStore`, `SignupRequest`) · 2️⃣ two new
fields · 3️⃣ the constructor takes them · 4️⃣ the new `signup(…)` method above `login(…)`.

### ❗ Step 9 · `ApiExceptionHandler.java` — two small additions

📄 **`Backend/src/main/java/com/project/multi_agent_ai_platform/web/ApiExceptionHandler.java`**

**9a · The import.** 📍 **Find** (around line 20):

```java
import com.project.multi_agent_ai_platform.document.UnsupportedDocumentException;
```

✂️ **Paste below:**

```java
import com.project.multi_agent_ai_platform.user.UsernameTakenException;
```

**9b · The handler.** 📍 **Find** this method (around line 56):

```java
	@ExceptionHandler(UnsupportedDocumentException.class)
	ProblemDetail unsupportedDocument(UnsupportedDocumentException ex) {
		return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported document", ex.getMessage());
	}
```

✂️ **Paste below** it (after its closing `}`):

```java

	@ExceptionHandler(UsernameTakenException.class)
	ProblemDetail usernameTaken(UsernameTakenException ex) {
		return problem(HttpStatus.CONFLICT, "Username taken", "That username is already registered. Pick another one.");
	}
```

> ➕ **Also in the repo:** an override of `handleMethodArgumentNotValid` in the same file, so a broken sign-up
> answers `400` *"password: at least 8 characters"* instead of Spring's *"Invalid request content."*

### 🧪 Step 10 · Try it

Start the backend from `Backend/` (`.\mvnw spring-boot:run`), then in a second PowerShell window:

```powershell
# Sign up → 201 and you are signed in
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/signup -ContentType 'application/json' `
  -Body '{"username":"anurag","password":"a-long-password"}'

# Run it again → 409 "Username taken"
```

| Database | Where to look |
|---|---|
| 🐘 SQL | `SELECT username, role, created_at FROM platform_users;` — `password_hash` starts with `$2a$` |
| 🍃 MongoDB | Compass → `agents` → `users` — `passwordHash` starts with `$2a$`; *Indexes* shows `username_1` **UNIQUE** |

### 🔧 Step 11 · Fix the tests

**11a · 📄 `Backend/src/test/java/com/project/multi_agent_ai_platform/config/SecurityConfigTest.java`**

(In the repo the whole file is deleted, since every test in it was about `AUTH_USERS`.) Otherwise: 🗑️ **delete**
these two test methods — each from its `@Test` line down to its closing `}`:

```java
	@Test
	void configuredAccountsAreStoredHashed() {
```

```java
	@Test
	void withNoAccountsThereIsAnAdminWithARandomPasswordNotADefaultOne() {
```

Then 🗑️ **delete** the three things only they used, near the top of the class: the `config` field, the `encoder`
field and the `users(String raw)` method. Press <kbd>Shift</kbd>+<kbd>Alt</kbd>+<kbd>O</kbd> to drop the unused imports.

**11b · 📄 `Backend/src/test/java/com/project/multi_agent_ai_platform/web/SignedInWebTest.java`**

Controller tests don't load the `user/` folder, so give them an empty account list. 📍 **Find:**

```java
	@Bean
	MockMvcBuilderCustomizer signedInByDefault() {
		return builder -> builder.defaultRequest(get("/").with(user("tester")));
	}
```

✂️ **Paste below** it:

```java

	/** Controller tests don't load the user/ folder, so SecurityConfig gets an empty account list. */
	@Bean
	UserDetailsService userDetailsService() {
		return new InMemoryUserDetailsManager();
	}
```

…and ✂️ **paste these imports below** `import org.springframework.context.annotation.Import;`:

```java
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
```

**11c · 📄 `Backend/src/test/java/com/project/multi_agent_ai_platform/web/AuthIntegrationTest.java`**

The login tests use the account `tester`; it now has to exist in the database. 📍 **Find:**

```java
	@Autowired
	MockMvc mvc;
```

✂️ **Paste below:**

```java

	@Autowired
	UserStore users;

	@Autowired
	PasswordEncoder encoder;

	/** The account the login tests sign in with, created once in the test database. */
	@BeforeEach
	void testerExists() {
		if (users.findByUsername("tester").isEmpty()) {
			users.create("tester", encoder.encode("tester-password"));
		}
	}
```

…and add the imports: <kbd>Shift</kbd>+<kbd>Alt</kbd>+<kbd>O</kbd>, or paste below `import org.junit.jupiter.api.Test;`:

```java
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.project.multi_agent_ai_platform.user.UserStore;
```

> 🍃 With MongoDB, `AuthIntegrationTest` needs MongoDB running (it uses the `agents_test` database from step B4).
> Then run `.\mvnw test` from `Backend/`. The repo also has `user/MongoUserStoreTest` (real MongoDB: save, find,
> duplicate refused, the unique index exists) and `user/DatabaseUserDetailsServiceTest` (no database), plus sign-up
> tests in `AuthIntegrationTest` (`201`, `409`, `400`, then login).

### 🖥️ Step 12 · The sign-up form

**12a · 📄 `Frontend/src/api/client.ts`** — 📍 **Find:**

```ts
export async function logout(): Promise<void> {
```

✂️ **Paste above** it:

```ts
export async function signup(username: string, password: string): Promise<AuthUser> {
  try {
    return toAuthUser(await http<unknown>('/auth/signup', { method: 'POST', body: JSON.stringify({ username, password }) }))
  } catch (err) {
    throw unreachable(err)
  }
}

```

**12b · 📄 `Frontend/src/hooks/useAuth.ts`** — four small changes:

| 📍 Find | ✏️ Replace with / ✂️ paste |
|---|---|
| `import { login as apiLogin, logout as apiLogout, me, subscribeAuthExpired } from '../api/client'` | ✏️ `import { login as apiLogin, logout as apiLogout, me, signup as apiSignup, subscribeAuthExpired } from '../api/client'` |
| `  login: (username: string, password: string) => Promise<void>` | ✂️ paste below: `  signup: (username: string, password: string) => Promise<void>` |
| `  const logout = useCallback(async () => {` | ✂️ paste **above** it: the block below |
| `  return { status, user, checkError, login, logout }` | ✏️ `  return { status, user, checkError, login, signup, logout }` |

```ts
  const signup = useCallback(async (username: string, password: string) => {
    const u = await apiSignup(username, password)
    setUser(u)
    setCheckError(null)
    setStatus('authenticated')
  }, [])

```

**12c · 📄 `Frontend/src/App.tsx`** — 📍 **Find:**

```tsx
    return <LoginPage onLogin={auth.login} checkError={auth.checkError} theme={theme} onToggleTheme={toggleTheme} />
```

✏️ **Replace** with:

```tsx
    return <LoginPage onLogin={auth.login} onSignup={auth.signup} checkError={auth.checkError} theme={theme} onToggleTheme={toggleTheme} />
```

**12d · 📄 `Frontend/src/pages/LoginPage.tsx`** — six small changes, top to bottom:

| # | 📍 Find | ✏️ Replace with / ✂️ paste |
|---|---|---|
| 1 | `  onLogin: (username: string, password: string) => Promise<void>` | ✂️ paste below: `  onSignup: (username: string, password: string) => Promise<void>` |
| 2 | `export function LoginPage({ onLogin, checkError, theme, onToggleTheme }: Props) {` | ✏️ `export function LoginPage({ onLogin, onSignup, checkError, theme, onToggleTheme }: Props) {` |
| 3 | `  const [busy, setBusy] = useState(false)` | ✂️ paste below: `  const [mode, setMode] = useState<'login' \| 'signup'>('login')` |
| 4 | inside `function goToSignUp`: `    show('signup')` | ✏️ `    setNotice(null)` and on the next line `    setMode('signup')` |
| 5 | `      await onLogin(user, password)` | ✏️ `      await (mode === 'signup' ? onSignup : onLogin)(user, password)` |
| 6 | `{busy ? 'Signing in…' : 'Login'}` | ✏️ `{busy ? 'Please wait…' : mode === 'signup' ? 'Create account' : 'Login'}` |

Then the title and the link under the form:

📍 **Find:**

```tsx
            <h1>Welcome Back!</h1>
            <p className="ap-sub">Login to continue to your account</p>
```

✏️ **Replace** with:

```tsx
            <h1>{mode === 'signup' ? 'Create your account' : 'Welcome Back!'}</h1>
            <p className="ap-sub">{mode === 'signup' ? 'Sign up to start using the agents' : 'Login to continue to your account'}</p>
```

📍 **Find** (near the end of the form):

```tsx
            <p className="ap-foot">
              Don&rsquo;t have an account?{' '}
              <a href="#signup" className="ap-link" onClick={(e) => { e.preventDefault(); show('signup') }}>
                Sign Up
              </a>
            </p>
```

✏️ **Replace** with:

```tsx
            <p className="ap-foot">
              {mode === 'login' ? 'Don’t have an account?' : 'Already have an account?'}{' '}
              <a
                href="#signup"
                className="ap-link"
                onClick={(e) => {
                  e.preventDefault()
                  setError(null)
                  setNotice(null)
                  setMode(mode === 'login' ? 'signup' : 'login')
                }}
              >
                {mode === 'login' ? 'Sign Up' : 'Log in'}
              </a>
            </p>
```

> ➕ **Also in the repo:** the form checks the same rules as `SignupRequest` before sending, shows them as a hint in
> sign-up mode, uses `autocomplete="new-password"` there, and the old "no self-service sign-up" notice is gone.

Check it: `npm run lint` and `npm run build` in `Frontend/`, then open <http://localhost:5173> → *Sign Up*.

---

## ✅ Done when

- [ ] Sign up from the login page → you land in the console
- [ ] The new account is in the database, with a hashed password
- [ ] Signing up the same name again shows *"Username taken"*
- [ ] Log out, log in again → works · a wrong password → *"Incorrect username or password."*
- [ ] Restart the backend → the account still logs in
- [ ] `.\mvnw test` 🟢 · `npm run lint` + `npm run build` 🟢

---

## 🔍 Managing it by hand

| You want to… | How |
|---|---|
| 👀 See accounts | SQL: `SELECT * FROM platform_users;` · MongoDB: Compass → `users` |
| 🛡️ Make someone an admin | Set `role` to `ADMIN` (no `ROLE_` prefix) |
| 🗑️ Remove an account | Delete the row / document |
| 🔑 Reset a password | Delete the account and sign up again — the column holds a **hash**, typing a password there won't work |
| ➕ Add an account by hand | Use the sign-up form; a hand-made entry needs a BCrypt hash, not a password |

> 🟦 **H2 only:** the file is locked while the backend runs. Stop the backend, open `Backend/data/platform.mv.db` in
> DBeaver (H2 driver, user `sa`, empty password), and close DBeaver before starting the backend again.

---

## ⭐ Golden rules

1. 🔒 **Store only the BCrypt hash** — never the password. `passwordEncoder.encode(…)` in step 8 does it.
2. 🧷 **Let the database enforce unique usernames** (`UNIQUE` / `@Indexed(unique = true)`) — a check in code alone can race.
3. 🔑 **Database passwords go in `Backend/.env`** — never in `application.properties`, which git tracks.
4. 💸 **Open sign-up spends your AI credits.** Anyone who can reach the backend can sign up and use your keys. Fine
   while it only listens on `127.0.0.1`; before going public, add an invite code or admin approval, plus rate limits.
5. 1️⃣ **Exactly one `UserStore`** — `JdbcUserStore` *or* `MongoUserStore`, never both.

---

## 🧩 Common paste mistakes

| The error says | What happened | Fix |
|---|---|---|
| `class, interface, enum, or record expected` | Code was pasted **outside** the class (after the last `}`) | Move it above the last `}` |
| `cannot find symbol` | An import is missing | <kbd>Shift</kbd>+<kbd>Alt</kbd>+<kbd>O</kbd>, or add the import from the step |
| `… is already defined` · `duplicate class` | The same method or file was pasted twice | Delete the copy |
| `class X is public, should be declared in a file named X.java` | The file name doesn't match the class | Rename the file |
| `package … does not exist` · `wrong package` | The file is in the wrong folder, or its `package` line is different | Check the folder against the 📁 map |
| `illegal start of expression` | A method was pasted **inside** another method | Paste it after the other method's closing `}` |

---

## 🆘 Troubleshooting

| What you see | Why · what to do |
|---|---|
| Startup: *required a bean of type … UserDetailsService* | Step 5 isn't done yet, or `DatabaseUserDetailsService` is outside the `user/` folder |
| Startup: *expected single matching bean … UserDetailsService* | The old `AUTH_USERS` method is still in `SecurityConfig` — step 6b |
| Startup: *expected single matching bean … UserStore* | Both `JdbcUserStore` and `MongoUserStore` exist — keep one |
| Startup: *Failed to determine a suitable driver class* · *Cannot load driver class: org.h2.Driver* | The H2 dependency was removed from `pom.xml` — put it back (step B2) |
| Startup: *Connection refused* · *Timed out after 30000 ms* | The database server isn't running, or the address in `.env` is wrong |
| *password authentication failed* · *Exception authenticating* | Wrong database user or password in `.env` |
| Sign-up answers `401` | `/api/auth/signup` is missing from the open routes, or written without the leading `/` — step 6a |
| Sign-up answers `400` | The username or password breaks the rules in `SignupRequest` (3–50 chars / 8+ chars) |
| MongoDB accepts the same name twice | `spring.data.mongodb.auto-index-creation=true` is missing — step B4 |
| DBeaver: *file is locked* | The backend is running — stop it first (H2 only) |
| A new account can't log in | Usernames are case-sensitive — check the exact name in the database |

Errors that aren't here → write them in the [setup errors log](TASKS.md#-setup-errors-log) and ask for help.

---

<div align="center">
<sub>🗄️ Guide for accounts in a database · <a href="TASKS.md">TASKS.md</a> has the MongoDB checklist and the errors log · <a href="BACKEND.md">BACKEND.md</a> · <a href="README.md">README.md</a></sub>
</div>
