# Plano: Acompanhamento de peso

Spec: [spec.md](./spec.md)

## Sequenciamento

1. Modelar `WeightEntryEntity` referenciando `PetEntity` e armazenar `weightGrams`.
2. Implementar consultas por pet/data, último peso, ordenação e upsert.
3. Implementar conversão, validação, edição e soft delete no repositório/ViewModel.
4. Integrar formulário, histórico e gráfico Vico de barras.

## Arquitetura

- `weight_entries` usa `petId`, metadados de sincronização e consulta ativa.
- A combinação pet/data determina o comportamento de upsert.
- O gráfico usa Vico com colunas verticais; apresentação converte gramas para kg.

## Dependências e riscos

- Depende de `0001` para o vínculo com o pet.
- Datas devem ser normalizadas para evitar duplicidade por fuso horário.
