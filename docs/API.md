# API do encurtador de links

Base local: `http://localhost:8080`.

## Criar link curto

```bash
curl -i -X POST http://localhost:8080/api/links \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/artigo?origem=portfolio"}'
```

A resposta `201 Created` contém `code`, `shortUrl`, `originalUrl` e `createdAt`. `shortUrl` tem formato `http://localhost:8080/r/<code>` por padrão. Para outro host, defina `APP_BASE_URL` ao iniciar o processo.

## Visitar e registrar um clique

```bash
curl -i http://localhost:8080/r/<CODE>
```

A resposta é `302 Found`; confira o cabeçalho `Location`. Não use `curl -L` neste exemplo, pois ele seguiria o redirecionamento e esconderia a resposta 302.

## Consultar estatísticas

```bash
curl 'http://localhost:8080/api/links/<CODE>/stats'

curl 'http://localhost:8080/api/links/<CODE>/stats?from=2026-10-01&to=2026-10-31'
```

As datas são inclusivas e seguem UTC. O retorno contém total de cliques dentro da janela e a contagem por dia; dias sem cliques não aparecem na lista.

## Erros principais

- `400 Bad Request`: URL inválida, esquema diferente de HTTP/HTTPS ou intervalo de datas invertido.
- `404 Not Found`: código inexistente.
- `503 Service Unavailable`: oito tentativas de geração de código tiveram colisão.
