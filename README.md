# Лабораторная работа 2

## HTTPS в Docker Compose

Внутренние соединения Config Server, Eureka, Zuul, Collection, Starship и HAProxy,
а также Caddy → Zuul работают по HTTPS с проверкой CA и имени сервера.
PostgreSQL и JDBC URL не изменены. Фронтенд `soa-front` работает в Compose:
Caddy → фронтенд → gateway также используют HTTPS с проверкой сертификатов.

```bash
python3 scripts/prepare-tls.py
docker compose up --build -d
curl --cacert certs/ca/root-ca.crt https://localhost/api/starship-service/starships
```

Приложение доступно на **https://helios.hw.zin** (также локально на `https://localhost`), gateway — `https://localhost:8080`,
Eureka — `https://localhost:8761`, Config Server — `https://localhost:8888`.
Caddy перенаправляет HTTP на HTTPS. Для браузера импортируйте `certs/ca/root-ca.crt`
в доверенные центры сертификации вашей системы/браузера.

Сертификаты подключаются с хоста, не включаются в Docker-образы и Git.
Скрипт подготовки создаёт пароль TLS-хранилищ в `.env`, если он ещё не задан.
После перевыпуска сертификатов повторите подготовку и пересоздайте контейнеры.
Подробности и сроки действия: [certs/README.md](certs/README.md).

Для развёртывания на helios.hw.zin направьте DNS на сервер и откройте TCP-порты 80/443.
Папку `certs/` и файл `.env` нужно перенести отдельно: они исключены из Git.
На клиентских машинах должен быть доверенным `certs/ca/root-ca.crt`.

Примеры HTTP и схемы ниже описывают локальный запуск и прежнюю архитектуру;
для текущего Compose используйте HTTPS-адреса выше.


[Схемы архитектуры и объяснение Spring Cloud Config](docs/architecture.html) — автономная HTML-страница;
скачайте или откройте файл в браузере. Включает связи компонентов, шаги запуска и приоритеты настроек.

Два самостоятельных Spring Boot 3.5.11 / Java 21 / Spring MVC REST приложения:

| Проект             | Порт | Назначение                                                                |
| ------------------ | ---- | ------------------------------------------------------------------------- |
| collection-service | 8081 | CRUD SpaceMarine, фильтры, сортировка, пагинация, дополнительные операции |
| starship-service   | 8082 | Посадка, высадка, просмотр экипажа и список кораблей                      |

## API Gateway: Zuul Proxy

Все обращения к бизнес-сервисам проходят через отдельный модуль api-gateway:
- Клиент → Caddy :80 → Zuul :8080 → HAProxy :8082 → Starship (в Docker Compose).
- Клиент → Caddy :80 → Zuul :8080 → Ribbon → Collection.
- Starship → Collection по адресу `COLLECTION_URL`.

Zuul использует Spring Boot 2.3.12, Spring Cloud Hoxton.SR12 и Java 11 (в Docker). Остальные модули используют Spring Boot 3.5 и Java 21. Сборку всего проекта можно выполнять JDK 21. Старый стек изолирован в gateway, потому что Zuul starter использует javax.servlet и отсутствует в современном Spring Cloud.

Публичные адреса:
- http://localhost:8080/api/collection-service/space-marines
- http://localhost:8080/api/starship-service/starships
- http://localhost:8080/collection-service/openapi.yaml
- http://localhost:8080/starship-service/openapi.yaml
- http://localhost:8080/collection-service/swagger-ui/index.html
- http://localhost:8080/starship-service/swagger-ui/index.html

Те же пути доступны через Caddy на порту 80. Префикс /api/{service} заменяется Zuul на /api/v1. Маршруты /{service}/ сохраняют полный путь ресурса. Неизвестные сервисы не маршрутизируются. В Compose порты бизнес-сервисов не опубликованы. Config Server и Eureka доступны на localhost для инфраструктуры и диагностики.

Порядок запуска: Config Server → Eureka → Zuul → бизнес-сервисы; дождитесь появления API-GATEWAY, COLLECTION-SERVICE и STARSHIP-SERVICE в Eureka. Gateway получает конфигурацию через bootstrap.properties, остальные сервисы — через spring.config.import. Starship использует collection.base-url=http://api-gateway/api/collection-service. В каждом Starship Ribbon выбирает экземпляр gateway; внутри Zuul Ribbon выбирает экземпляр бизнес-сервиса. Повторы запросов выключены, в том числе для POST/PATCH.

Проверка: .\mvnw.cmd -pl api-gateway,collection-service,starship-service verify.
GatewayRoutingTest запускает настоящий Zuul на случайном порту и три HTTP-сервера вместо upstream: проверяет GET/POST/PATCH/DELETE, JSON, query, заголовки, 404/409, два экземпляра Collection, ресурсные пути и вложенный вызов Starship → Zuul → Collection. Эти проверки не подтверждают регистрацию настоящих сервисов в Eureka и работу БД.

## Service Discovery: Eureka

`eureka-server` на порту 8761 хранит реестр экземпляров `collection-service` и `starship-service`.
Клиенты регистрируются под своим `spring.application.name` и отправляют heartbeat.
Eureka хранит реестр адресов. Starship обращается к http://api-gateway/api/collection-service через свой Ribbon. Zuul находит Collection через Eureka и выбирает его экземпляр через Ribbon. Все бизнес-запросы проходят через gateway; Caddy передаёт API-запросы только в Zuul.

Порядок локального запуска в отдельных терминалах:

```powershell
.\mvnw.cmd -pl config-server spring-boot:run
.\mvnw.cmd -pl eureka-server spring-boot:run
.\mvnw.cmd -pl api-gateway spring-boot:run
# После запуска инфраструктуры, с DB_PASSWORD в окружении каждого приложения:
.\mvnw.cmd -pl collection-service spring-boot:run
.\mvnw.cmd -pl starship-service spring-boot:run
```

Панель: <http://localhost:8761/>. Реестр: <http://localhost:8761/eureka/apps>.
В панели ожидаются `COLLECTION-SERVICE` и `STARSHIP-SERVICE` со статусом UP.
Настройки клиентов и сервера Eureka находятся в `config-server/src/main/resources/config-repository/`.
Адрес Eureka переопределяется переменной `EUREKA_URL` (по умолчанию `http://localhost:8761/eureka/`).
В Compose используется `http://eureka-server:8761/eureka/`; сервисы публикуют свои IP и порты внутри Docker-сети.
Порт панели опубликован только на localhost. Локальные и Docker-экземпляры запускайте как отдельные окружения,
чтобы клиенты могли обращаться ко всем адресам, зарегистрированным в своём реестре.

Config Server находится по известному URL: сначала загружаются настройки, затем включается Eureka.
Сам Config Server не регистрируется в Eureka, что исключает циклическую зависимость при запуске.
Eureka Server получает настройки из Config Server и не регистрируется сам у себя.
Caddy передаёт запросы в api-gateway:8080; gateway маршрутизирует их по service ID через Eureka и Ribbon.

Документация: [Spring Cloud Netflix](https://docs.spring.io/spring-cloud-netflix/reference/spring-cloud-netflix.html),
[Eureka](https://spring.io/projects/spring-cloud-netflix/).

## Spring Cloud Config

Модуль `config-server` (порт 8888) использует Spring Cloud 2025.0.3 и native backend:
общие настройки находятся в `config-server/src/main/resources/config-repository/application.properties`,
настройки сервисов — в `collection-service.properties` и `starship-service.properties` в той же папке.
Имена файлов соответствуют `spring.application.name` клиентов.

В отдельном терминале запустите сервер, затем оба приложения:

```powershell
.\mvnw.cmd -pl config-server spring-boot:run
# В других терминалах:
.\mvnw.cmd -pl collection-service spring-boot:run
.\mvnw.cmd -pl starship-service spring-boot:run
```

Проверка конфигураций: `http://localhost:8888/collection-service/default` и
`http://localhost:8888/starship-service/default`. Клиенты получают настройки при запуске.
Для применения изменений перезапустите сервер и клиенты; автоматическое обновление не настроено.
Другой адрес сервера задаётся через `CONFIG_SERVER_URL`. Импорт обязателен: Config Server должен быть доступен при запуске обоих сервисов. В локальных `application.properties` оставлены только имя приложения и импорт конфигурации.
Переменные окружения клиентов (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `EUREKA_URL`, `PORT`)
переопределяют настройки. Config Server передаёт только ссылку `${DB_PASSWORD}`; значение пароля задаётся в окружении клиента, без значения по умолчанию.

В Docker Compose сервер запускается автоматически, клиенты требуют его доступности и повторяют
подключение до 20 раз. Порт 8888 опубликован только на localhost.
Для внешней папки конфигураций задайте серверу `CONFIG_REPOSITORY=file:/absolute/path/`;
в контейнере дополнительно смонтируйте эту папку. По умолчанию файлы включены в JAR.

Документация: [Config Client](https://docs.spring.io/spring-cloud-config/reference/client.html),
[Native backend](https://docs.spring.io/spring-cloud-config/reference/server/environment-repository/file-system-backend.html).

## Contract First

Контракт исходного SpaceMarine разделён по путям. Источник для каждого проекта -
`src/main/resources/static/openapi.yaml`. Maven OpenAPI Generator в фазе `generate-sources` генерирует Spring MVC
контроллеры, API-интерфейсы, интерфейсы делегатов и DTO с Jakarta Validation в `target/generated-sources/openapi`.
Контроллеры и API генерируются в пакет `controller.generated`, DTO - в `dto`. Компоненты
`controller.CollectionApiDelegate` / `controller.StarshipApiDelegate` реализуют сгенерированные интерфейсы делегатов и
вызывают сервисы. Сгенерированные файлы редактировать не нужно. Исходный проект SpaceMarine не изменён.

Для DTO в обоих Maven-проектах задан `modelNameSuffix=DTO`: например, `SpaceMarineDTO`, `CoordinatesDTO`, `ChapterDTO`,
`WeaponDTO`. Доменные модели сохраняют имена `SpaceMarine`, `Coordinates`, `Chapter`, `Weapon`; это позволяет
использовать обычные импорты без полных имён классов в коде. Суффикс меняет только имена Java-классов, структура JSON и
пути API остаются прежними. Делегаты реализованы классами `CollectionApiDelegate` и `StarshipControllerDelegate`.

Настройки генератора: https://openapi-generator.tech/docs/generators/spring/

Комментарии и сообщения ошибок приложения написаны на русском. Для генерации классов используются встроенные шаблоны OpenAPI Generator. Обработчики ошибок возвращают русские описания ошибок JSON, валидации, параметров, HTTP-методов и конфликтов данных.

## PostgreSQL

Создайте базу один раз (например, в pgAdmin):

```sql
CREATE DATABASE space_marine;
```

По умолчанию оба проекта подключаются к `jdbc:postgresql://localhost:5432/space_marine`, пользователь - `root`. Пароль в
репозитории не хранится: перед запуском обязательно задайте `DB_PASSWORD`. Для своей установки задайте действительного
пользователя PostgreSQL и его пароль через переменные окружения PowerShell:

```powershell
$env:DB_USER = 'postgres'
$env:DB_PASSWORD = 'ваш пароль'
```

Также поддерживаются `DB_URL`, `PORT` и `EUREKA_URL`. Второй сервис обращается к первому по логическому имени `collection-service` через Netflix Ribbon. Flyway создаёт схемы `collection` и `starship` и таблицы автоматически. Пользователю БД
нужны права создавать схемы и таблицы. Первый сервис хранит десантников в обычных столбцах через JPA-сущность
`model.SpaceMarine`: `Coordinates` и `Chapter` являются встраиваемыми объектами `@Embeddable`, типы оружия - enum.
Второй хранит JPA-сущности `model.Boarding` в собственной схеме и обращается к первому через `client.CollectionClient`,
не читая его таблицы. Полученный десантник представлен объектом `model.SpaceMarine` (снимок удалённого объекта).

Миграции V1 сохранены. Новая V2 переносит существующие JSONB-данные коллекции в столбцы с сохранением id и creationDate;
в посадках добавляет собственный сгенерированный id и сохраняет уникальность marine_id. На новой базе последовательно
выполняются V1 и V2. Hibernate настроен с `ddl-auto=validate`, поэтому структуру изменяет только Flyway.

## Пакеты и слои

```text
controller/generated  - сгенерированные из OpenAPI контроллеры, API и интерфейсы делегатов
controller            - реализация делегатов, HTTP-ответы и обработка ошибок
dto                   - сгенерированные DTO с валидацией
mapper                - преобразование DTO ↔ model
model                 - JPA-сущности, встраиваемые объекты, enum и удалённые модели
service               - бизнес-логика и транзакции, работа с объектами model
repository            - Spring Data JpaRepository, JPA Specifications
client                - HTTP-клиент первого сервиса (в starship-service)
```

Цепочка обработки запроса: сгенерированный контроллер → делегат → mapper/service → repository → Hibernate → PostgreSQL.
Сервисы не зависят от DTO или HttpServletRequest и не формируют HTTP-ответы. Фильтрация реализована через
`Specification` / JPA Criteria, сортировка и пагинация - через `Sort` / `PageRequest`. Прямых SQL-запросов в Java-коде
нет; SQL используется в миграциях Flyway. Отдельные bulk-delete операции репозиториев используют JPQL по сущностям.

Документация: [Spring Data JPA Specifications](https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html), [Hibernate ORM](https://docs.jboss.org/hibernate/orm/6.6/introduction/html_single/Hibernate_Introduction.html).

## CQRS

В каждом сервисе обработка команд отделена от обработки запросов:

| Сервис | Команды (изменение данных) | Запросы (чтение) |
| --- | --- | --- |
| collection-service | `CollectionCommandService`: create, update, patch, delete, deleteByHealth, deleteOneByWeapon | `CollectionQueryService`: get, getMaxName, list |
| starship-service | `StarshipCommandService`: load, unloadAll | `StarshipQueryService`: listCrew, listStarships |

HTTP-делегаты направляют операции в соответствующий сервис. Команды выполняются в обычных
транзакциях, запросы — в `@Transactional(readOnly = true)`. Для списка коллекции сохранён
уровень изоляции `REPEATABLE_READ`, чтобы содержимое страницы и общее количество были согласованы.
Сервисы запросов зависят от `SpaceMarineQueryRepository` и `BoardingQueryRepository`,
которые предоставляют только методы чтения. Эти интерфейсы реализуются существующими
Spring Data репозиториями; отдельные Spring-бины репозиториев запросов не создаются.

Используется CQRS с общей базой и общей моделью хранения: запись сразу доступна последующим
запросам после фиксации транзакции. Отдельные проекции, брокер сообщений и Event Sourcing
не используются. Команда может читать данные для проверки бизнес-правил и возвращать
результат согласно действующему REST-контракту. Экипаж по-прежнему получает актуальные
данные десантников через `CollectionClient`. Пути API, JSON и схема БД сохранены.

## Проверки CQRS и моделей

Модели используют Lombok `@Builder(toBuilder = true, setterPrefix = "with")` и `@With`. Создание выполняется через
`Model.builder().with...().build()`, обновление - через `existing.toBuilder().with...().build()` или
`existing.withHealth(200)`. Методы `with...` модели возвращают копию, которую нужно сохранить или присвоить; исходный
объект не изменяется. В PUT сервис берёт id и creationDate из существующей сущности, обновляет только разрешённые поля и
сохраняет результат через JPA merge. Для Hibernate сохранены protected-конструкторы без аргументов и доступ к полям. DTO
продолжают генерироваться из OpenAPI.

`clean verify` генерирует API из OpenAPI, компилирует и упаковывает приложения. Тесты проверяют Ribbon и HTTP-маршрутизацию Zuul. Проверка запуска с PostgreSQL требует действительных DB_USER/DB_PASSWORD.

## Starship: Spring Boot JAR и HAProxy

`starship-service` использует Java 21 и встроенный Tomcat из `spring-boot-starter-web`.
Сборка: `bash ./mvnw -f starship-service/pom.xml clean verify`.
Результат: `starship-service/target/starship-service-1.0.0.jar`; Docker запускает его командой `java -jar /app/app.jar`.

`compose.yaml` запускает два экземпляра Starship и HAProxy с конфигурацией [haproxy.cfg](haproxy.cfg)
из корня проекта. Оба маршрута Zuul (`/api/starship-service/**` и `/starship-service/**`)
направлены на `http://haproxy:8082`. HAProxy сохраняет пути запросов и распределяет их по round robin.
HTTP-проверка `GET /api/v1/starships` исключает недоступные экземпляры; Docker DNS обновляет адреса
после пересоздания контейнеров. Автоматические повторы запросов в HAProxy выключены.

Starship использует общую PostgreSQL-схему `starship`, а адрес Collection получает из `COLLECTION_URL`
(в Compose — `http://collection-service:8081/api/v1`). В текущем Starship нет клиентов Eureka,
Config Server и Ribbon; упоминания этих клиентов ниже относятся к прежней архитектуре.

```bash
docker compose up --build -d
# Изменить число экземпляров (HAProxy настроен максимум на 10):
docker compose up -d --scale starship-service=3
# Посмотреть выбранные серверы starship_instances/starshipN в HTTP-логах:
docker compose logs -f haproxy
curl http://localhost:8080/api/starship-service/starships
```

HAProxy и Starship доступны внутри Docker-сети на порту 8082; вход с хоста — через Zuul :8080 или Caddy.
При запуске без Docker запросы можно отправлять непосредственно на Starship :8082.

## Сборка и запуск

Для запуска обоих сервисов, PostgreSQL и Caddy в Docker задайте пароль БД и выполните из корня проекта:

```bash
export DB_PASSWORD='ваш пароль'
docker compose up --build -d
```

Compose создаёт базу `space_marine`; Flyway создаёт схемы и таблицы. Данные PostgreSQL хранятся в томе
`postgres_data`. Бизнес-сервисы доступны через Zuul на порту `8080`; порты `8081` и `8082` не опубликованы, PostgreSQL - на `5432` (можно изменить порт хоста через
`DB_PORT`). Caddy слушает порт `80` (или `CADDY_PORT`) и перенаправляет запросы:

| Внешний путь                              | Адрес назначения                                 |
| ----------------------------------------- | ------------------------------------------------ |
| `/api/collection-service/space-marines`   | Zuul → Ribbon → `collection-service:8081/api/v1/space-marines`   |
| `/api/starship-service/starship/1/load/1` | Zuul → HAProxy → `starship-service:8082/api/v1/starship/1/load/1` |
| Любой другой путь                         | `https://soa-front:3000`                         |

Фронтенд собирается из `soa-front/` и доступен через Caddy по HTTPS; серверные функции вызывают API через gateway. Для остановки используйте `docker compose down`.

Для запуска без Docker:

Из `lab2` выполните `./mvnw.cmd clean verify` в PowerShell (на Linux/macOS - `./mvnw clean verify`). Maven Wrapper
загрузит Maven автоматически; нужен Java 21 и доступ к Maven Central при первой сборке. При установленном Maven также
работает `mvn clean verify`. Каждый дочерний проект можно собирать отдельно через собственный pom.xml, например
`./mvnw.cmd -f collection-service/pom.xml clean verify`.

После запуска Config Server, Eureka и gateway запустите бизнес-сервисы в двух отдельных терминалах:

```powershell
java -jar collection-service/target/collection-service-1.0.0.war
java -jar starship-service/target/starship-service-1.0.0.jar
```

Starship — исполняемый Spring Boot JAR со встроенным Tomcat, запускается через `java -jar`. Collection остаётся исполняемым WAR.

Контракты доступны по `http://localhost:8080/collection-service/openapi.yaml` и `http://localhost:8080/starship-service/openapi.yaml`.

Swagger UI: `http://localhost:8080/collection-service/swagger-ui/index.html` для коллекции и `http://localhost:8080/starship-service/swagger-ui/index.html`
для кораблей. UI загружает исходный контракт `/openapi.yaml`. Страницы доступны после успешного запуска сервиса: при
ошибке аутентификации PostgreSQL (28P01) Flyway останавливает запуск приложения, поэтому сначала нужно исправить
DB_USER/DB_PASSWORD.

## Примеры запросов

```powershell
$marine = '{"name":"Marine","coordinates":{"x":1.5,"y":10},"health":100,"weaponType":"COMBI_FLAMER","chapter":{"name":"Ultramarines","world":"Macragge"}}'
Invoke-RestMethod http://localhost:8080/api/collection-service/space-marines -Method Post -ContentType application/json -Body $marine
Invoke-RestMethod 'http://localhost:8080/api/collection-service/space-marines?page=0&size=10&sort=health,desc&filter=health,gt,50'
Invoke-RestMethod http://localhost:8080/api/collection-service/space-marines/1 -Method Patch -ContentType application/json -Body '{"health":80,"meleeWeapon":null}'
Invoke-RestMethod http://localhost:8080/api/starship-service/starship/1/load/1 -Method Post
Invoke-RestMethod 'http://localhost:8080/api/starship-service/starships?page=0&size=10'
Invoke-RestMethod 'http://localhost:8080/api/starship-service/starship/1/space-marines?page=0&size=10&filter=health,gt,50'
Invoke-RestMethod http://localhost:8080/api/starship-service/starship/1/unload-all -Method Post
```

Список `/starships` строится из уникальных `starship_id` в таблице `boarding`. Корабль без записей о посадке в нём не появится.

Фильтры и сортировка принимают повторяющиеся параметры: `sort=health,desc&sort=name,asc`. Доступные поля: id, name,
creationDate, health, achievements, weaponType, meleeWeapon, coordinates.x, coordinates.y, chapter.name,
chapter.parentLegion, chapter.world. Несколько фильтров объединяются через AND. Неизвестные поля, операторы и неверные
значения дают 400. DELETE отсутствующего элемента идемпотентен. Ошибки возвращаются в формате ApiError.

## Допущения исходного контракта

В исходном OpenAPI отсутствуют сущность корабля, реестр кораблей, API их создания и поле корабля в SpaceMarine. Поэтому
любой положительный starshipId обозначает логический корабль; высадка с пустого корабля возвращает 0. Десантник может
находиться только на одном корабле: повторная посадка возвращает 409. Посадка возвращает неизменённый SpaceMarine из
первого API. Удаление десантника из коллекции не удаляет посадку автоматически: в контракте нет уведомления об удалении;
запись убирается при unload-all. Проверка существования через HTTP и запись посадки не являются распределённой
транзакцией.

Первый API определяет path id как integer без int64, хотя id в DTO - int64; это сохранено по исходному контракту.
Ограничения DTO проверяются автоматически (400), конфликты состояния дают 409, ненайденный десантник - 404. При
недоступности первого API второй возвращает ApiError с 503, при ошибке его ответа - 502 (инфраструктурные ответы сверх
исходного контракта).

## Netflix Ribbon

Конфигурация HTTP-клиента: `starship-service/src/main/java/soa/lab2/starship/client/CollectionClientConfiguration.java`.
Использование: `CollectionClient.java`; logical URL задаётся в Config Server, `starship-service.properties`.
Подробная карта, полный код, разбор запроса и интерактивная модель round robin: [архитектура](docs/architecture.html#loadbalancer).
Для двух локальных экземпляров Collection запустите второй с `PORT=8083` в отдельном терминале; не меняйте `spring.application.name`.
В Compose порт Collection не опубликован, поэтому можно запустить два экземпляра: `docker compose up -d --scale collection-service=2`. Оба регистрируются в Eureka; экземпляр выбирает Ribbon внутри Zuul.

Ribbon подключён напрямую (ribbon-core и ribbon-loadbalancer 2.7.18), поскольку старый Spring Cloud Netflix Ribbon starter отсутствует в Spring Cloud 2025. Eureka и Config Server сохранены. Балансировщик работает внутри вызывающего starship-service; collection-service менять не требуется. При пустом реестре клиент возвращает 503. Проверки: .\mvnw.cmd -pl starship-service test (чередование экземпляров, сохранение URI, обновление списка и пустой реестр).

## Подробная интерактивная документация

[Открыть HTML-разбор архитектуры](docs/architecture.html) можно прямо как локальный файл: интернет не нужен. На странице есть описание технологий, переключаемая карта потоков, пошаговые сценарии, модели Ribbon/Eureka, поиск по технологиям и текущий код проекта.

Обновить снимки кода и результаты сквозного теста:

~~~powershell
node scripts/build-architecture.mjs
~~~

Повторить сквозной тест настоящих Config Server, Eureka, Zuul, бизнес-сервисов и временной PostgreSQL:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-gateway.ps1
~~~

Отчёт и логи: api-gateway/target/live-verification/. Скрипт использует JAVA_HOME, временные порты и собственный контейнер БД, останавливает созданные процессы при завершении. Не использует рабочую базу проекта.

Проверить HTML в установленном Chromium-браузере:

~~~powershell
node scripts/verify-architecture.mjs
~~~

Для нестандартного пути браузера задайте CHROME_PATH. Скриншоты и отчёт: api-gateway/target/html-verification/. Страница генерируется из docs/architecture.template.html и встроенных исходников.