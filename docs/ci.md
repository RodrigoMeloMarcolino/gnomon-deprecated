# CI — GitHub Actions

Os workflows usam permissões mínimas, timeout, concurrency e actions fixadas por SHA. `policy`
valida skills e higiene; `quality` executa Spotless, unitários, ArchUnit, JaCoCo e SpotBugs;
`integration` executa Testcontainers para mudança de código/configuração; `dependency-review` e
`codeql` analisam dependências e código; `nightly-full` roda a suíte e OWASP completas.

`merge-gate` classifica o diff e exige integração para código, migrations, segurança,
configuração, Maven ou CI; documentação evita scans pesados. `review-evidence` é executado de
`main`, sem checkout do PR, e aceita apenas o relatório para o SHA atual e
`/accept-review <sha>` do owner.

```bash
./mvnw verify -Pquality
./mvnw verify -Pintegration
./mvnw verify -Pall-tests,quality
./mvnw verify -Psecurity
```

Após o merge bootstrap, o owner configura o ruleset da `main`: PR obrigatório, branch atualizada,
conversas resolvidas, sem force-push/deleção, e checks `merge-gate`, `codeql`,
`dependency-review` e `review-evidence`. Habilita também Dependabot security updates, secret
scanning e push protection. `NVD_API_KEY` permanece somente em GitHub Secrets.
