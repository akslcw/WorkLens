# WorkLens Login Security Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reject invalid desktop logins before the tray starts, surface actionable messages, enforce password-change requirements, limit repeated failures, and allow only the newest session for each account.

**Architecture:** The Python client will convert login HTTP failures into a typed, user-facing `LoginError`, preserve `mustChangePassword`, and authenticate synchronously before creating the tray icon. The Spring Boot backend will persist per-username login-attempt state in PostgreSQL, lock for 15 minutes after five consecutive failures, clear failures after success, and delete existing tokens before issuing the newest token.

**Tech Stack:** Python 3 `unittest`/`requests`/Tkinter/pystray; Java 17, Spring Boot 3.0.2, MyBatis-Plus, PostgreSQL, MockMvc/JUnit 5.

---

### Task 1: Desktop API error contract

**Files:**
- Modify: `worklens_desktop_client/api_client.py`
- Test: `worklens_desktop_client/tests/test_api_client.py`

- [ ] Add tests proving that HTTP 401 becomes `LoginError("用户名或密码错误，请重新输入。")`, HTTP 429 becomes the lockout message, connection failures become a server-unavailable message, missing tokens are rejected, and `mustChangePassword` is retained.
- [ ] Run `python -m unittest worklens_desktop_client.tests.test_api_client -v` and confirm the new tests fail because `LoginError` and `must_change_password` do not exist.
- [ ] Add `LoginError`, map login HTTP/network failures without exposing passwords, validate required success fields, and add `must_change_password: bool` to `LoginResult`.
- [ ] Re-run the focused tests and confirm all pass.

### Task 2: Authenticate before tray startup

**Files:**
- Modify: `worklens_desktop_client/sync_runtime.py`
- Modify: `worklens_desktop_client/tray_app.py`
- Modify: `worklens_desktop_client/run_sync_client.py`
- Test: `worklens_desktop_client/tests/test_sync_runtime.py`
- Create: `worklens_desktop_client/tests/test_tray_app.py`

- [ ] Add a runtime test proving `mustChangePassword=true` raises `LoginError` before `on_login` or collection starts.
- [ ] Add a tray test proving failed authentication displays the error and returns before `BackgroundRunner.start()` or `pystray.Icon.run()`.
- [ ] Run the focused tests and confirm they fail on the current post-prompt startup behavior.
- [ ] Add `SyncRuntime.login()` and allow `run()` to accept an already validated `LoginResult`, rejecting forced-password-change accounts before callbacks.
- [ ] In `tray_app.main()`, call `runtime.login()` synchronously, show a Tk error dialog on failure, return immediately, and only then construct/start the tray.
- [ ] In the console entry point, catch `LoginError` and print a concise login failure instead of a traceback.
- [ ] Re-run desktop tests and confirm all pass.

### Task 3: Persistent failed-login lockout

**Files:**
- Modify: `worklens_backend/src/main/resources/schema.sql`
- Create: `worklens_backend/src/main/java/com/su/worklens_backend/entity/AuthLoginAttempt.java`
- Create: `worklens_backend/src/main/java/com/su/worklens_backend/mapper/AuthLoginAttemptMapper.java`
- Modify: `worklens_backend/src/main/java/com/su/worklens_backend/service/impl/AuthServiceImpl.java`
- Modify: `worklens_backend/src/test/java/com/su/worklens_backend/WorklensBackendApplicationTests.java`

- [ ] Add integration tests proving failures one through four return 401, the fifth and subsequent attempts return 429, a successful login clears prior failures, and expired locks permit authentication.
- [ ] Run `mvnw.cmd -Dtest=WorklensBackendApplicationTests test` with the documented PostgreSQL environment and confirm the tests fail because no attempt state is stored.
- [ ] Add `auth_login_attempts` keyed by normalized username, with `failed_attempts`, `locked_until`, and `updated_at`; use a transaction and row lock to serialize updates.
- [ ] Use five failures and a 15-minute lock, return the same 401 for unknown users and bad passwords, return 429 while locked, and delete attempt state on successful authentication.
- [ ] Re-run the focused integration tests and confirm all pass.

### Task 4: Single active session

**Files:**
- Modify: `worklens_backend/src/main/java/com/su/worklens_backend/service/impl/AuthServiceImpl.java`
- Modify: `worklens_backend/src/test/java/com/su/worklens_backend/WorklensBackendApplicationTests.java`

- [ ] Add an integration test that logs in twice, verifies the first token receives 401 from `/auth/me`, and verifies the second token receives 200.
- [ ] Run the focused test and confirm it fails because both tokens remain valid.
- [ ] Delete all existing tokens for the authenticated user in the login transaction immediately before inserting the new token.
- [ ] Re-run the focused integration tests and confirm they pass.

### Task 5: Error response and documentation

**Files:**
- Modify: `worklens_backend/src/main/java/com/su/worklens_backend/controller/ApiExceptionHandler.java`
- Modify: `README.md`
- Test: `worklens_backend/src/test/java/com/su/worklens_backend/WorklensBackendApplicationTests.java`

- [ ] Add response assertions for stable `code` and `message` fields on 401 and 429 without revealing whether a username exists.
- [ ] Run the focused tests and confirm they fail with the default Spring error body.
- [ ] Map authentication exceptions to `ApiErrorResponse`, document five-attempt/15-minute lockout and newest-session-only behavior, and keep client messages status/code based.
- [ ] Re-run focused backend and desktop tests.

### Task 6: Full verification and manual scenarios

**Files:**
- No production files beyond Tasks 1-5.

- [ ] Run `python -m unittest discover worklens_desktop_client/tests -v`.
- [ ] Run `mvnw.cmd test` with PostgreSQL available.
- [ ] Run `git diff --check` and inspect `git status --short`.
- [ ] Start PostgreSQL and the backend, then exercise an unknown username and capture the HTTP status/body plus desktop behavior.
- [ ] Exercise an existing username with a wrong password and capture the HTTP status/body plus desktop behavior.
- [ ] Exercise correct credentials and capture the token-bearing response plus confirmed collector startup.
- [ ] Exercise five bad passwords and capture the 429 lockout response.
- [ ] After clearing/expiring the lock, log in twice and prove the first token is invalid while the second remains valid.
- [ ] Do not commit automatically; leave the isolated branch changes available for user review.
