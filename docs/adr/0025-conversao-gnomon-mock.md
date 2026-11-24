# ADR 0025 — Conversão do backend Java em mock/reference do Umbra

Status: Accepted
Data: 2026-08-19

## Contexto

O backend Java consolidou o contrato `/v1`, o OpenAPI, fixtures determinísticos e os fluxos
críticos consumidos pelo Umbra. A próxima implementação de produção será recriada em Node.js e
adotará uma estratégia interna de booking baseada em exclusion constraint GiST.

Modificar este código e seu schema até atingir a nova arquitetura misturaria troca de linguagem,
persistência e concorrência em um único cutover. Ao mesmo tempo, descartar a implementação Java
removeria a melhor referência executável disponível para o frontend.

## Decisão

1. O repositório Java passa a se chamar `gnomon-mock` e preserva seu histórico Git.
2. Seu papel é servir como implementação de referência, mock executável e baseline de
   compatibilidade da API `/v1` para o Umbra e para a suíte black-box do sucessor.
3. O novo repositório vizinho `gnomon` é o backend de produção e o proprietário das futuras
   decisões de produto e arquitetura.
4. Compatibilidade significa comportamento HTTP observável: métodos, paths, headers, autenticação,
   CORS, schemas, casing, status, envelope/códigos de erro, idempotência e semântica temporal.
5. Compatibilidade não exige schema, ORM, linguagem ou mecanismo de concorrência iguais.
6. O mock mantém o modelo vigente de `appointment_slots`; GiST pertence somente ao sucessor.
7. Novas funcionalidades de produto não são implementadas aqui. São permitidos apenas ajustes de
   fidelidade, fixtures, segurança, manutenção e correções indispensáveis ao contrato congelado.
8. O OpenAPI baseline e os cenários de compatibilidade devem ser executáveis contra mock e
   sucessor. Depois do cutover, o novo `gnomon` passa a publicar a evolução canônica do contrato.

## Consequências

- O Umbra pode continuar seu desenvolvimento sem aguardar a reimplementação Node.js.
- O sucessor pode nascer com banco e arquitetura próprios, sem dual write ou compartilhamento de
  schema com o mock.
- Drift entre as duas implementações passa a ser o principal risco e exige OpenAPI diff, fixtures
  estáveis e testes black-box comuns.
- O mock não é serviço de produção nem fallback automático; qualquer uso externo deve permanecer
  restrito a ambientes locais e de teste.
- A renomeação do remote GitHub é uma operação externa separada desta decisão local.

## Rastreabilidade

- Preserva: ADR 0023 e task `integration-umbra-01.5` como baseline de integração.
- O sucessor registra sua stack, compatibilidade e estratégia GiST em ADRs próprios.
