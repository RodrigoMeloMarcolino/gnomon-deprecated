# Registro de riscos aceitos

Riscos `medium` descobertos no review independente só podem ser aceitos temporariamente nesta
tabela. `critical` e `high` nunca podem ser aceitos. Cada entrada expira em no máximo 30 dias e
deve referenciar a issue que acompanha a correção.

| ID | Finding / impacto | PR e SHA | Justificativa | Issue | Owner | Expira em | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |

## Regras

- `Status` aceito é somente `accepted` enquanto a expiração não venceu.
- Uma entrada deve usar o ID do finding do relatório `GNOMON_REVIEW_EVIDENCE`.
- Ao corrigir, altere o status para `resolved`; não apague o histórico.
