# ADR 0024 — Governança de hardening e review independente

Status: Accepted
Data: 2026-08-17

## Decisão

A fase 09 é dividida em 09-A (processo de desenvolvimento, CI e review) e 09-B (fechamento E2E,
schema drift e índices quando todos os módulos existirem). PRs exigem gates aplicáveis, review
independente nas perspectivas qualidade, segurança e arquitetura e comentário do owner
`/accept-review <sha>` para o SHA revisado. Novo commit invalida a evidência anterior.

Findings `critical` e `high` bloqueiam sem exceção. `medium` é corrigido ou aceito no registro de
riscos com issue, owner e expiração máxima de 30 dias; `low` é documentado e vira follow-up.
JaCoCo exige 80% de linhas e 70% de branches em domínio/aplicação; SpotBugs bloqueia severidade
medium/high e Dependency-Check bloqueia CVSS >= 7.

## Consequências

O ruleset remoto da `main` deve exigir `merge-gate`, `codeql`, `dependency-review` e
`review-evidence`, branch atualizada e conversas resolvidas, proibindo force-push/deleção. A
ativação é uma operação de owner posterior ao merge bootstrap, pois não pode ser versionada.
