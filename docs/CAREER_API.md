# CV Intelligence API

All routes use the existing `Authorization: Bearer <session-token>` and return `Cache-Control: no-store`. Authentication happens before multipart resolution. Errors use `{ "message": "..." }`. No CV files have public URLs.

| Method /api/v1/career/... | Request | Response |
| --- | --- | --- |
| GET access | — | freeAvailable, pro, busy, providerConfigured, provider, model, retentionDays, maxFileBytes |
| POST preview | multipart `file` | redactedText, characters, disclosure. Local extraction only; no quota or AI call. |
| POST analyze | multipart below | typed Result with matching + analysis + limitations |
| GET analyses | — | owned saved-result metadata: id, createdAt/expiresAt (epoch milliseconds) |
| GET analyses/{id} | — | owned, unexpired Result |
| DELETE analyses/{id} | — | deletes owned saved result; no quota reset |
| DELETE analyses | — | deletes all owned saved results, cancels pending persistence; no quota reset |

Multipart analyze fields:

- `file`: mandatory text-based PDF or DOCX, default <=5 MiB. Matching filename extension, MIME and signature required; strict MIME rejection is intentional.
- `role`: optional career goal, <=200 characters.
- `jobDescription`: optional, <=12,000 characters; empty means general quality analysis.
- `language`: `az`, `en` (default), `ru`, `tr`, `de`.
- `consent`: must be `true`; never inferred from upload or account signup.
- `saveHistory`: default `false`. Opt-in results expire after configured retention, 1..90 days.

Original PDF/DOCX is parsed in memory, not saved. Contact/known-name reduction applies to CV, role and job description; automatic redaction is incomplete. PDF JavaScript, attachments, active forms/actions and encrypted documents are rejected. DOCX macros, embedded objects, suspicious paths, archive count/size/ratio abuse are rejected; nothing is executed or fetched from document links. Scanned documents need OCR, which is unsupported.

`Result` contains id, type=`ai-cv-analysis`, provider, model, language, UTC createdAt/expiresAt, saved, role, matching, analysis, limitations. Matching includes nullable overallScore (0..100), scoreKind, six category scores (nullable if unassessed), matchedRequirements, missingRequirements. Draft contains supported skills/education/projects/experience with exact CV excerpts; explicit JD requirements with JD/CV excerpts; quality scores/reasons; missingInformation, strengths, priorities, skillsToDevelop, learningTopics, portfolioIdeas, bulletImprovements, interviewTopics, 30/60/90 roadmap, self-reported confidence and limitations.

Matching weights: technical 30, projects 20, relevant experience 20, job-relevant education 10, evidence quality 10, readability 10. For the first four categories, per-requirement MATCHED=100, PARTIAL=50, MISSING=0; average within each category. Only categories grounded in explicit JD requirements apply. Quality categories use AI judgment with evidence. Exclude unassessed categories and renormalize weights. General mode uses the two quality categories only. Nonempty JD without supported requirements yields null overall compatibility. These are heuristic scores, never guaranteed ATS scores, probabilities, or hiring decisions.

Status codes: 400 missing consent/invalid options/legacy JSON; 401 absent/expired session; 402 free quota exhausted without PRO; 404 missing/expired/other-owner result; 409 concurrent/canceled/expired lease; 413 oversized upload; 422 unreadable/unsupported file; 429 local capacity; 502 invalid/provider failure; 503 unconfigured/rate-limited provider; 504 provider timeout. No failures generate fake AI results.

Compatibility: POST `/career/analyze` remains the route, but the former `{skills,role}` JSON demo deliberately returns an actionable 400 migration error. It cannot analyze a real CV or carry valid upload consent. Other REST endpoints remain unchanged. The old `analyses` table is retained without being used by this engine.

Quota: lazy `career_access` row per registered account; conditional UPDATE atomically claims a five-minute lease. A successful validated result marks free_used in the same transaction as optional persistence. Failure releases the lease without spending the free allowance. Lease tokens prevent stale completions overwriting a newer request. Canceled requests retain the lease until completion/expiry so deletion cannot enable concurrent requests. A crash leaves a bounded lease. Deletion/retention never resets free_used.

PRO: `career_access.pro_until` is a UTC epoch-millisecond expiry maintained by a trusted operator. There is no client-writable entitlement endpoint, new role system or payment integration. To grant/revoke, an operator with database access ensures a career_access row exists, then sets pro_until for the correct user. Never accept this value from a browser or expose database credentials. Every existing registered account is eligible; the existing application has no student-role verification.

Privacy: no input CV, extracted text or source filename is persisted by CampusLink. Opt-in result excerpts may still contain personal data. Default response stays only in page memory and is cleared on logout/reload. Expired results are excluded immediately and purged hourly; backups need their own deletion policy. Delete-all cancels pending result storage but cannot recall data already sent to the provider. Quota/entitlement metadata remains after result deletion. Provider requests set `store:false`, use no tools and are never automatically retried. OpenAI abuse-monitoring retention depends on account controls; this is not a zero-retention guarantee.

See [OpenAI structured output](https://developers.openai.com/api/docs/guides/structured-outputs) and [provider data controls](https://developers.openai.com/api/docs/guides/your-data). Only `AI_PROVIDER=openai` is implemented. Other values fail closed, with no fallback. Use a model available to your account that supports Responses + strict structured outputs.

Security limits: prompt instructions plus typed/semantic validation reduce injection risk but cannot guarantee semantic faithfulness or perfectly detect discriminatory/hallucinated prose. Evidence validation checks exact presence, not real-world truth. Extractors run in the JVM with bounded inputs; production should isolate document parsing and apply resource limits, rate limits, malware review, TLS, protected token storage and a security review. Do not call this production-ready.
