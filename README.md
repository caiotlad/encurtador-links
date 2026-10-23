# 4. Encurtador de links com estatísticas

API que aceita uma URL HTTP/HTTPS, gera um código curto, redireciona visitantes e registra o horário de cada clique para exibir totais diários.

**Nome planejado do repositório GitHub:** `encurtador-links`.

## Requisitos e execução

- Java 17 ou superior, Maven 3.6.3+ e Docker Compose.
- Na pasta deste projeto: `docker compose up -d` e depois `mvn spring-boot:run`.
- API: `http://localhost:8080`. PostgreSQL: porta `5435`.
- O Flyway cria as tabelas e o índice de análise de cliques.
- Para encerrar: `docker compose down`; para apagar também os dados: `docker compose down -v`.

Leia `docs/GUIA_DE_ESTUDO.md` e `docs/API.md`.

## Decisões do MVP

- Código aleatório de 8 caracteres, com alfabeto Base62 e restrição `UNIQUE` no banco.
- A aplicação tenta gerar um código novamente em caso de colisão; a restrição única continua sendo a proteção final.
- A rota `/r/{code}` grava um clique e devolve HTTP 302 para o destino.
- Cada clique é registrado no PostgreSQL. A consulta agrupa por dia UTC; `from` e `to` filtram um intervalo inclusivo de datas.
- Nenhum IP, user-agent ou dado pessoal é coletado.
- Somente URLs com esquema HTTP ou HTTPS, host e sem credenciais embutidas são aceitas.

## Limites

O registro de clique é síncrono e o armazenamento cresce sem política de retenção. Autenticação, rate limiting, expiração de link, Redis, fila de eventos e painel web são evoluções futuras. Use localmente e não o exponha à internet como serviço público sem controles contra abuso.

## Organização de pastas

```text
04-encurtador-links/
├── docs/
│   ├── API.md                 # chamadas e respostas
│   └── GUIA_DE_ESTUDO.md      # dados, índice e fluxo
├── src/main/java/br/com/portfolio/links/
│   ├── LinksApplication.java
│   ├── LinkController.java    # criar, redirecionar e consultar
│   ├── LinkDtos.java          # contratos HTTP
│   └── LinkService.java       # validar URL, SQL e gerar código
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/V1__schema_inicial.sql
├── compose.yaml
├── pom.xml
└── README.md
```

## Rotas

| Método | Rota | Responsabilidade |
|---|---|---|
| `POST` | `/api/links` | Validar destino e gerar código curto |
| `GET` | `/r/{code}` | Registrar clique e responder 302 |
| `GET` | `/api/links/{code}/stats` | Somar cliques por intervalo/dia UTC |

`short_links` contém o mapeamento; `link_clicks` contém um evento por visita. O índice `(link_id, clicked_at)` atende à consulta temporal. Nenhum IP ou user-agent é armazenado.

## Como estudar este projeto

1. Compare a validação de URL no serviço com a restrição de protocolo descrita no README.
2. Observe que a constraint `UNIQUE` no banco, e não uma checagem anterior na aplicação, decide se o código pode ser usado.
3. Siga a visita de `/r/{code}` até o `INSERT` de clique e o cabeçalho `Location`.
4. Execute estatísticas com `from` e `to` e confira como cada data afeta a consulta.
5. Avalie uma fila para cliques somente depois de documentar atraso, retentativas e consistência eventual.
