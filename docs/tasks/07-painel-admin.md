# Fase 07 — Painel administrativo (appointments e customers)

Status: done (concluída em 2026-08-13)

## Objetivo

Owner/admin acompanham a agenda do tenant; staff acompanha a própria agenda.

## Escopo

- `GET /v1/tenants/{tenantSlug}/appointments` com filtros `date` (ou range), `calendar_id`,
  `status` e paginação (default 20, máximo 100); staff recebe apenas o próprio calendário
  (filtro forçado).
- `GET /v1/tenants/{tenantSlug}/appointments/{id}` (detalhe com customer e serviço).
- Transições administrativas com `SELECT ... FOR UPDATE`:
  `POST .../appointments/{id}/cancel`, `/complete`, `/no-show`
  (apenas de `scheduled`, conforme a máquina de estados da spec booking seção 3; transição
  inválida → 409; cancel libera os slots na mesma transação — antecipa a regra de liberação de
  slots da fase 08 para o caminho admin; `completed`/`no_show` preservam o appointment histórico,
  enquanto os locks expiram conforme o ADR 0022).
- `GET /v1/tenants/{tenantSlug}/customers` e `/customers/{id}`: customers com appointments no
  tenant (inferência via appointments), campos globais mínimos.
- Eventos admin P1 de logging (`appointment.cancelled/completed/no_show`).

## Fora de escopo

- Edição de appointment; remarcação admin; exportações; métricas de agenda.

## Testes

- Integração: filtros combinados; staff restringido ao próprio calendário (403/missing fora
  dele); cancel admin libera slots (available-slots volta a mostrar o horário); transições
  inválidas (de `cancelled`) → 409; cross-tenant 403/404.

## Critérios de aceite

- [x] Matriz de autorização da spec multi-tenancy coberta para appointments/customers.
- [x] Cancelamento admin libera horário comprovadamente.
- [x] Listagens admin com paginação obrigatória (sem varredura completa — dívida Moira).

## Contrato implementado

- Appointments administrativos usam intervalo RFC 3339 (`from` inclusivo, `to` exclusivo),
  máximo de 31 dias, paginação `page`/`size` (0/20, máximo 100), filtros `calendar_id` e
  `status`, e ordenação `start_at ASC, id ASC`.
- Owner/admin enxergam o tenant inteiro; staff é automaticamente limitado ao calendário do
  colaborador vinculado. Filtro divergente retorna `403 staff_calendar_mismatch`.
- Transições aceitas: `scheduled → cancelled|completed|no_show`; operações terminais retornam
  `409 appointment_status_conflict`. Cancelamento remove os locks na mesma transação; os demais
  preservam-nos. Eventos técnicos são publicados no log após commit.
- Customers são globais, mas a leitura exige appointment relacionado no tenant; staff recebe
  `403 insufficient_role`.

## Notas de implementação

- Não há migration: as consultas reutilizam os índices de appointments existentes.
- A restauração inicial da fase corrigiu os dois erros de compilação do checkpoint: fallback de
  `AdminAppointment` separado do fallback de `Appointment`, e iteração do OpenAPI por entrada de
  path usando `readOperationsMap()`.
- As fronteiras cross-module iniciais foram endurecidas: tenancy agora publica
  `TenantAccessUseCase`, catalog publica `StaffCalendarAccessUseCase`, e booking/customers usam
  adapters locais sem depender dos `port.out` de catalog.
- Concluído no slice de 2026-08-06: leitura e transição foram separadas em
  `AdminAppointmentQueryService`/`AdminAppointmentTransitionService` e nos respectivos input
  ports. A transição continua transacional, usa lock pessimista e publica o evento somente após
  commit; as exceções de domínio continuam traduzidas para `BookingException` na fronteira de
  aplicação, sem mudança no contrato HTTP.
- Continuação em 2026-08-06: adicionada `AdminPanelIntegrationTest` com filtros combinados e
  paginação, escopo automático de staff, isolamento de appointments/customers, cancelamento com
  liberação de slots e corrida de transições. A suíte normal e Spotless passaram em Java 21; a
  execução Testcontainers ficou bloqueada nesta sessão pela ausência do daemon Docker.
- Pendente: executar essa suíte contra PostgreSQL real, confirmar o contrato de erro em todas as
  matrizes cross-tenant e então marcar a fase como `done`.
- Fechamento em 2026-08-13: o fixture JDBC agora envia `Timestamp.from(...)` para todas as
  colunas `TIMESTAMPTZ`; a corrida `cancel × complete` submete ambas as requisições antes de
  liberar o latch e comprova uma única resposta `200` e uma `409`, com slots consistentes com a
  transição vencedora. A suíte cobre owner/admin, staff limitado ao próprio calendário, negação
  de customers para staff, 401 sem JWT, 403 cross-tenant, filtros/paginação, detalhes e conflitos
  terminais.
- Cancelamento administrativo invalida, após commit, o cache Redis de disponibilidade do
  calendário/dia afetado; `complete` e `no_show` preservam os locks e não invalidam o cache.
  O cenário de integração aquece Redis, confirma a remoção dos slots e verifica o reaparecimento
  do horário em `available-slots`.
- Validação Java 21/Testcontainers verde: `test -Pintegration -Dtest=AdminPanelIntegrationTest`
  (7), `spotless:check`, `test` (250), `test -Dtest=ArchitectureTest` (5),
  `verify -Pintegration` (45) e `verify -Pall-tests` (295). `git diff --check` também passou.
- Não houve mudança de endpoint, payload, migration ou ADR. A fase 06 permanece `doing`.
