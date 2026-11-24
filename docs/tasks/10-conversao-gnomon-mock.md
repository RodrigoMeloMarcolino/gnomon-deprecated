# Fase 10 — Conversão em mock/reference do Umbra

Status: done (concluída em 2026-08-19)

## Objetivo

Preservar o backend Java como `gnomon-mock`, uma implementação de referência executável da API
`/v1`, e abrir espaço para o sucessor Node.js no repositório vizinho `gnomon`.

## Entregas

- [x] Papel do repositório Java redefinido em README, AGENTS, PRD e ADR 0025.
- [x] Histórico Git preservado na renomeação da pasta local.
- [x] Novo repositório `gnomon` criado com PRD e ADRs próprios.
- [x] Transição documentada no Umbra sem alterar seus contratos locais em desenvolvimento.
- [x] Separação explícita: mock mantém slots; sucessor nasce com GiST.

## Validação e riscos

- Nenhum código Java, contrato HTTP ou migration foi alterado.
- O remote GitHub existente permanece com o nome anterior até uma ação externa explícita.
- A compatibilidade só estará comprovada quando a suíte black-box comum rodar contra as duas APIs.
