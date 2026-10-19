# Actual verification — 19 October 2026

Environment: Windows, Node.js 24.21.0, Maven 3.9.9, Oracle JDK 25.0.2 compiling with Java 21 target. Tests did not use the user's file database. Actual JDK 21 runtime was not available.

| Check | Actual outcome |
| --- | --- |
| Maven test/package | 20 tests, 0 failures, 0 errors, 0 skipped; executable `backend/target/campuslink-backend-1.0.0.jar` produced |
| Parsing tests | 4 passed: PDF/DOCX, MIME/extension/signature, malformed/empty/encrypted input, size, DOCX macro/ZIP abuse |
| Matching tests | 3 passed: weighted calculation, general mode, unsupported CV/JD excerpts |
| API/service integration | 7 passed: auth/ownership, legacy migration + existing routes, failures/consent, quota concurrency/PRO, privacy + no-history default, expiry, cancellation during inference |
| File database restart | 1 passed: quota, entitlement and result metadata survive H2 shutdown/reopen |
| Provider HTTP contract | 5 passed: strict request/no storage/no tools, invalid JSON/DTO/unknown fields/refusal/truncation, provider errors, timeout, absent credentials |
| Vite production build | Passed; 8 modules transformed; production assets emitted to `frontend/dist` |
| Chromium browser tests | 4 passed: upload/preview/consent/results, explicit provider failure, unconfigured state + five languages + 390px layout, invalid files + existing navigation/logout |
| Actual packaged Spring Boot HTTP | Passed on isolated port 8791 with an in-memory H2 database and AI disabled: health, registration/session, authorization, access, real PDF multipart preview + redaction, explicit 503 with preserved free quota, legacy 400, empty owner history |
| Actual browser → Spring → H2 | Passed: existing session bootstrap, actual PDF upload/extraction/preview, disabled provider state, no JavaScript exceptions and no horizontal overflow at 390px |

Browser fixture tests intercept APIs explicitly and use synthetic responses; provider HTTP tests use a local fake HTTP server. They are not live AI quality validation. The additional actual-browser check forwarded real API traffic to the isolated Spring server, used a generated synthetic PDF, and made no external AI call. Temporary local test servers were stopped afterward.

The first browser attempt started before Chromium finished downloading. Subsequent browser testing caught and fixed a submit handler redraw bug and the existing mobile header's intrinsic search width. Final verification uses the corrected code. Maven/JDK native-access and Mockito agent warnings and a Commons Logging discovery warning remain; they did not fail tests or packaging. No production claim follows from these results.

Not verified: real OpenAI credentials/model access, a paid live inference, model evidence faithfulness/coaching quality across all five languages, production provider retention settings, PostgreSQL runtime/concurrency, JDK 21 runtime, adversarial parser isolation, calibrated model confidence, complete PII removal, exhaustive security/dependency review or real payments. Configure a supported model and server-side key, then use a synthetic resume and explicit UI consent for a live provider acceptance check. PostgreSQL should be tested before deployment.

Reproduction commands are in [README](../README.md). API behavior, migration details, privacy guarantees and limits are documented in [CAREER_API.md](CAREER_API.md).
