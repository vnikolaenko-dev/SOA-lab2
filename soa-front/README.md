# Фронтенд SOA

React / TanStack Start / Rsbuild. Браузер обращается к приложению через Caddy,
а серверные функции фронтенда вызывают оба API через Zuul.

## Docker Compose и HTTPS

Из корня репозитория:

```bash
python3 scripts/prepare-tls.py
docker compose up --build -d
```

Приложение: **https://helios.hw.zin** (локально также https://localhost). Caddy подключается к `https://soa-front:3000`
с проверкой сертификата. Фронтенд доверяет CA через `NODE_EXTRA_CA_CERTS` и вызывает:

- `SPACE_MARINES_API_URL=https://api-gateway:8080/collection-service`;
- `STARSHIPS_API_URL=https://api-gateway:8080/starship-service`.

Gateway передаёт запросы Starship через HAProxy. Сертификаты монтируются с хоста
в режиме read-only, не попадают в образ и Git. Приложение работает от пользователя
`node`: entrypoint копирует закрытый ключ в приватный каталог контейнера перед
сбросом привилегий. Порт 3000 наружу не публикуется.
Для браузера нужен доверенный `certs/ca/root-ca.crt`.

## Локальная разработка

Backend запускается через Compose; dev-сервер фронтенда — отдельно:

```bash
cd soa-front
pnpm install --frozen-lockfile
NODE_EXTRA_CA_CERTS=../certs/ca/root-ca.crt pnpm dev
```

Dev-сервер доступен на http://localhost:3000. Серверные функции по умолчанию
обращаются к HTTPS gateway на localhost:8080. Для других адресов задайте
`SPACE_MARINES_API_URL` и `STARSHIPS_API_URL`.
Префикс пути в адресе сохраняется перед сгенерированным `/api/v1/...`.

## Сборка и генерация API

```bash
pnpm build
NODE_EXTRA_CA_CERTS=../certs/ca/root-ca.crt pnpm api:generate
```

Orval загружает OpenAPI через HTTPS gateway. Сгенерированные файлы находятся
в `src/api/generated/`; вручную их редактировать не нужно.
`pnpm start` запускает HTTP-сервер для локального использования;
Docker-образ запускает srvx напрямую через Node с TLS-параметрами и подключёнными сертификатами.

Страницы: `/space-marines`, `/space-marines/new`, `/space-marines/:id`,
`/space-marines/:id/edit`, `/starships`, `/starships/:id`.

## Логирование ошибок

Серверные ошибки доступны через `docker compose logs -f soa-front`.
События `api.request`, `api.http` и `api.schema` содержат сервис, метод, путь без
query-параметров, длительность запроса, HTTP-статус (если есть), stack и цепочку cause.
Это позволяет увидеть, в частности, ошибки TLS и подключения к gateway.
При ошибке схемы добавляются пути полей и описания проблем валидации.
Тела запросов/ответов и заголовки в логи не записываются.

В консоли браузера логируются ошибки действий пользователя, React Query,
необработанные исключения и отклонённые Promise. Общий префикс — `[frontend]`.
