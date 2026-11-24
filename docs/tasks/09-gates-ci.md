# Fase 09 — Gates de CI e hardening

Status: doing (09-A — Development Process Hardening)

## Objetivo

Qualidade automatizada no nível do Moira: gates de arquitetura, cobertura, schema drift e
smoke E2E, com CI verde e branch protegida.

## 09-A — Development Process Hardening

Implementa workflows versionados, Maven quality/security, review independente evidenciado por
SHA, skills locais, Dependabot, template de PR e registro de riscos.

Critérios de aceite adicionais:

- [ ] PR canário prova bloqueio de ArchUnit, cobertura, SpotBugs, dependência vulnerável,
  mudança sensível e evidência/aceite stale ou não-owner.
- [ ] Owner ativa ruleset e secret scanning/push protection na `main` após o merge bootstrap.

## 09-B — Fechamento posterior

Permanece `todo`: smoke E2E completo, schema drift final e `EXPLAIN`/índices das consultas quentes
só fecham depois dos workstreams 06, 07.5 e 08.

## Escopo legado de 09-B

- Testes ArchUnit: direção de dependências por módulo (`api` → `application` → `domain` ←
  `infrastructure`), domínio sem Spring/JPA, controllers finos (sem chamada direta a
  repository), `shared` sem regra de módulo.
- Verificação de schema drift: Flyway migrations vs entidades JPA
  (`hbm2ddl` validate em perfil de teste ou task dedicada no CI).
- Cobertura com JaCoCo falhando abaixo de 80% de linhas e 70% de branches nos pacotes
  `domain` e `application`.
- Smoke E2E: bootstrap tenant → colaborador → offering → atribuição → availability →
  available-slots → booking → tentativa de conflito → cancelamento/remarcação (profile
  dedicado).
- GitHub Actions conforme `docs/ci.md`: jobs lint/unit/integration/approval-gate; proteção da
  `main` com os 4 checks obrigatórios.
- Índices de FK guiados por `EXPLAIN` nas consultas quentes (appointments por calendário/data,
  memberships por usuário).

## Fora de escopo

- Deploy/CD para ambiente real; performance tests formais; mutation testing.

## Testes

- Os próprios gates são os testes; smoke E2E roda no CI contra Testcontainers.

## Critérios de aceite

- [ ] PR só mergeia com os 4 checks verdes.
- [ ] Violação proposital de arquitetura quebra o build (prova do gate).

## Notas de implementação

- 2026-08-17: fundação 09-A adicionada; validação local, PR canário e ruleset remoto pendentes.
- 2026-08-18: remediação do baseline SpotBugs 09-A: dos 40 findings originais, a correção
  local pré-existente eliminou o único finding high, restando 39 medium; 19 findings reais
  foram corrigidos e 20 `EI_EXPOSE_REP2` foram classificados pontualmente como falsos positivos
  de referências compartilhadas gerenciadas pelo Spring. As supressões ficam somente nos
  construtores canônicos apontados pelo relatório, com justificativa explícita; `exclude.xml`
  permanece vazio. Nenhum contrato HTTP, schema, migration ou arquitetura foi alterado.
- 2026-08-18: adicionados testes para `create(null)` sem chamadas a ports e para timezone nulo,
  vazio e inválido. Em Java 21, `spotless:check test` passou com 257 testes e `verify -Pquality`
  passou com SpotBugs zerado; PR canário, integração/all-tests, configuração remota e review
  independente do SHA final continuam pendentes, portanto 09-A permanece `doing`.
