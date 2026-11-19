# Starship: SOAP и REST-адаптер

Объяснение изменений, архитектуры и протокола: [docs/soap-migration.html](docs/soap-migration.html). Откройте файл в браузере.

Вызывающий `starship-service` предоставляет SOAP 1.1 на `/services/starships` и WSDL на `/services/starships?wsdl`. Отдельный `starship-rest-adapter` сохраняет прежние REST-операции и вызывает только SOAP. Gateway направляет прежние клиентские адреса на адаптер. Исходники фронтенда не изменены.

## Локальный запуск (Windows / PowerShell)

Нужны JDK 21, Docker, Node.js 22.6+ для дополнительной проверки исходных клиентских TypeScript-схем. Maven скачивается wrapper-скриптом.

```powershell
cd soa-front
npx --yes pnpm@10 install --frozen-lockfile
cd ..
.\scripts\start-local.ps1 -WithFrontend
```

Скрипт делает чистую сборку с тестами, поднимает отдельную PostgreSQL на порту 55432, Config Server, Eureka, Collection, SOAP Starship, REST-адаптер, gateway и фронтенд. Повторный запуск с готовыми артефактами: `-SkipBuild`. Существующие процессы нужно сначала остановить.

- Клиент: http://localhost:3000/
- REST через gateway: http://localhost:18080/api/starship-service/starships
- REST напрямую: http://localhost:8083/api/v1/starships
- WSDL: http://localhost:8082/services/starships?wsdl

Проверка реальной цепочки:

```powershell
node scripts/test-e2e.mjs
# Проверка тех же ответов исходными Zod-схемами неизменённого клиента:
node --experimental-strip-types scripts/test-e2e.mjs --client-schemas
```

Остановка (данные локальной БД сохраняются):

```powershell
.\scripts\stop-local.ps1
```

Логи и PID-файлы: `.local-run/`; отчёт сквозных тестов: `.local-run/e2e-results.json`.

## Проверки

19.11.2026: `mvnw.cmd -B clean package` — успешно; 4 SOAP HTTP-теста и 9 REST→SOAP HTTP-тестов прошли. Сквозной прогон с настоящей PostgreSQL — 26/26, включая проверку исходными Zod-схемами фронтенда. `rsbuild build` — успешно; страницы `/`, `/starships`, `/space-marines` возвращают HTTP 200. После тестов временные десантники и записи о посадках удалены.

Визуальная проверка браузером не завершена: Computer Use не смог определить URL Chrome. HTTPS-развёртывание не запускалось: в репозитории отсутствуют сертификаты.

## Docker с HTTPS

Основной `compose.yaml` сохраняет TLS. Дополнительно к существующим сертификатам нужен `certs/starship-rest-adapter/keystore.p12` с DNS-именем `starship-rest-adapter`, подписанный используемым CA. Нужны переменные `TLS_STORE_PASSWORD` и `DB_PASSWORD`, как и ранее. Запуск: `docker compose up --build -d`. Адаптер вызывает SOAP через HAProxy, балансирующий два экземпляра `starship-service`; проверка доступности HAProxy использует WSDL.
