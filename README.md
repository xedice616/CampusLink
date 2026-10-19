# CampusLink — Java backend, CV Intelligence & original frontend

Existing Java/Spring Boot session authentication, courses, teams, chat, settings and cream/deep-green Vite design are preserved. The former skill-keyword career demo is replaced with authenticated PDF/DOCX extraction, real configurable OpenAI Responses integration, evidence validation, heuristic matching and career coaching. No synthetic AI fallback is used.

## Run on Windows

Requirements: JDK 21+ (build target 21), Node.js 18+, internet for dependencies. Open the inner `CampusLink_Java_Hackathon` folder.

Backend terminal:

```powershell
cd "C:\Users\user\Downloads\CampusLink_Java_Hackathon\CampusLink_Java_Hackathon"
$env:AI_PROVIDER='openai'
$env:AI_MODEL='YOUR_RESPONSES_STRUCTURED_OUTPUT_MODEL'
$env:AI_API_KEY='YOUR_SERVER_SIDE_KEY'
$env:AI_TIMEOUT='60s'
.\run-backend.ps1
```

The model/key above are placeholders. Select a model available to your account that supports Responses strict JSON-schema outputs. Never put the key in frontend code, a `VITE_` variable or source control. No credentials are included. With no key/model the app starts normally and the career screen explicitly reports AI unavailable; other features keep working. Provider calls can incur charges, so the UI requires consent before each analysis.

If PowerShell scripts are blocked, use `START_BACKEND.bat` or run `.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f backend/pom.xml spring-boot:run` after Maven is installed. The existing launcher downloads Maven 3.9.9 if absent.

Frontend terminal:

```powershell
cd "C:\Users\user\Downloads\CampusLink_Java_Hackathon\CampusLink_Java_Hackathon\frontend"
npm.cmd install
npm.cmd run dev
```

Open **http://localhost:5173**. Backend health: **http://localhost:8787/api/v1/health**. Register/sign in, open **AI Career Assistant**, choose a text-based PDF/DOCX, optionally enter a goal and job description, inspect the privacy preview, select one of five response languages and explicitly consent. Check “save” only if you want result history. The CV interface and decorative banners have no memes; existing chat stickers remain functional.

## Configuration

`.env.example` documents supported variables. Spring Boot does **not** automatically load that file; set variables in the backend process environment.

| Variable | Default / meaning |
| --- | --- |
| AI_PROVIDER | `openai`; other values fail closed. No hidden fallback provider. |
| AI_MODEL | empty; operator must select a supported model |
| AI_API_KEY | empty; backend-only credential |
| AI_TIMEOUT | `60s`, permitted 5s..120s; no automatic retries |
| CV_MAX_FILE_BYTES | `5242880`; extractor limit |
| CV_MAX_FILE_SIZE | `5MB`; multipart limit (keep aligned with bytes; max request currently 6MB) |
| CV_RETENTION_DAYS | `30`, permitted 1..90; opt-in results only |

Contact details, URLs and the account's known name are reduced before transmission. Other identifiers can remain: review the privacy preview and remove sensitive details from the document first. Uploaded files and extracted input are never saved by CampusLink. Results default to page memory; saved excerpts expire and can be deleted. Provider retention is separate: `store:false` is requested, but does not guarantee zero abuse-monitoring retention. See [OpenAI data controls](https://developers.openai.com/api/docs/guides/your-data).

## Matching, quotas and PRO

One successful AI analysis is free per registered account. Further analyses need an unexpired `career_access.pro_until` entitlement maintained by a trusted database operator. There is no client entitlement setter, new role system or payment integration. Quota claims use atomic database updates and per-request leases; concurrent requests cannot spend the same free entitlement. Failed parsing, failed provider calls and invalid AI output do not spend it. Deleting results cannot reset it.

Only explicit job-description requirements are matched. Without a job description, the engine performs general CV quality coaching instead. Weights, scores, source excerpts, confidence and limitations are returned separately. Scores are **heuristic advice, never guaranteed ATS scores or hiring probabilities**. The roadmap and suggested projects are future actions, not invented past achievements.

## Database

Default H2 database: `backend/data/campuslink.mv.db`. Keep it to preserve users, messages, quota and saved analyses. It is local to this machine. Additive `schema.sql` changes create `career_access` and `career_results`; the old demo `analyses` table remains untouched.

For PostgreSQL:

```powershell
$env:SPRING_PROFILES_ACTIVE='postgres'
$env:DB_URL='jdbc:postgresql://localhost:5432/campuslink'
$env:DB_USER='campuslink'
$env:DB_PASSWORD='YOUR_DATABASE_PASSWORD'
```

Create the empty database before starting. PostgreSQL is configured but has not been runtime-tested here. Production needs versioned migrations, managed credentials, encryption/backups and separate retention rules for backups.

## API and architecture

See [career API documentation](docs/CAREER_API.md) for multipart fields, response models, score formulas, quota lifecycle, entitlement handling and deletion guarantees. The existing `/api/v1/career/analyze` URL is retained; old `{skills,role}` JSON now returns a clear 400 migration error because it has no CV or processing consent. All unrelated REST routes remain.

- `CvExtractor`, `CvPrivacy`: bounded file validation/text extraction and contact reduction.
- `CareerAuth`, `CareerSecurityFilter`: existing session-token authentication before document parsing.
- `CareerAi`, `OpenAiCareerProvider`, `StructuredSchema`, `CareerModels`: provider boundary, closed structured JSON and typed validation.
- `CareerMatching`: evidence checks and weighted score calculations.
- `CareerService`, `CareerController`: atomic quota, optional result persistence, ownership and retention.
- `frontend/src/career.js`, `career.css`: upload, consent, progress/error states, evidence cards, gaps, coaching and roadmap.

## Checks

```powershell
# From project root
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f backend/pom.xml test
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f backend/pom.xml package
# From frontend
npm.cmd run build
npx.cmd playwright install chromium
npm.cmd test
```

Tests use synthetic CVs and explicitly mocked AI/network responses; they do not send personal CV data or make paid AI calls. Backend tests cover PDF/DOCX validation, malformed/encrypted files and ZIP abuse, structured response errors/timeouts, exact evidence, matching, auth/ownership, quota concurrency, entitlement, consent, deletion/expiry and H2 file persistence. Browser tests cover upload, privacy preview, consent, result rendering, errors, unconfigured state, languages, mobile layout and existing navigation.

See [verification results](docs/VERIFICATION.md) for actual command outcomes and remaining limits. This is an incremental implementation, **not a production-readiness claim**. The original app still has demo content, browser-stored auth tokens, no email verification/Google OAuth and no real-time chat. Document parsers should be isolated and resource-limited before public hosting. AI semantic faithfulness, complete PII removal and prompt-injection immunity cannot be guaranteed by schema/quote validation.
