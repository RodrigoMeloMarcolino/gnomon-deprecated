---
name: gnomon-post-implementation-review
description: Execute an independent three-pass review after a Gnomon implementation and publish the required PR evidence. Use when a Gnomon task is ready for review, a draft PR is being updated, or review findings and accepted risks must be verified.
---

# Gnomon post-implementation review

Review a change independently. Do not inherit the implementer’s rationale or use its review conclusions as evidence.

1. Read the relevant task, specification, ADRs, `AGENTS.md`, the base-to-head diff and gate output.
2. Run separate quality (correctness, regressions, transactions and tests), security (auth, tenant isolation, injection, validation, PII and dependencies) and architecture (PRD/ADRs, layers, ports, HTTP and migrations) passes.
3. Give every finding a stable ID, severity, location, evidence and remediation. Never log secrets, PII or idempotency keys.
4. `critical` and `high` fail. A `medium` passes only when fixed or recorded in `docs/security/risk-register.md` with issue, owner and expiry within 30 days. `low` is visible and a follow-up when unresolved.
5. Comment the exact head SHA with this marker, followed by readable Markdown:

```text
<!-- GNOMON_REVIEW_EVIDENCE {"sha":"<40-char-sha>","quality":"PASS","security":"PASS","architecture":"PASS","findings":{"critical":0,"high":0,"medium":0,"low":0}} -->
```

The owner accepts an all-pass report only with `/accept-review <full-head-sha>`; a new commit invalidates both records.
