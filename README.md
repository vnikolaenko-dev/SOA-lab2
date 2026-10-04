# Лабораторная работа 2

Два самостоятельных Spring Boot 3.5.11 / Java 21 / Spring MVC REST приложения:

| Проект | Порт | Назначение |
|---|---|---|
| collection-service | 8081 | CRUD SpaceMarine, фильтры, сортировка, пагинация, дополнительные операции |
| starship-service | 8082 | Посадка десантника и высадка всех с корабля |

## Contract First

Контракт исходного SpaceMarine разделён по путям. Источник для каждого проекта — `src/main/resources/static/openapi.yaml`. Maven OpenAPI Generator в фазе `generate-sources` генерирует Spring MVC контроллеры, API-интерфейсы, интерфейсы делегатов и DTO с Jakarta Validation в `target/generated-sources/openapi`. Контроллеры и API генерируются в пакет `controller.generated`, DTO — в `dto`. Компоненты `controller.CollectionApiDelegate` / `controller.StarshipApiDelegate` реализуют сгенерированные интерфейсы делегатов и вызывают сервисы. Сгенерированные файлы редактировать не нужно. Исходный проект SpaceMarine не изменён.

Для DTO в обоих Maven-проектах задан `modelNameSuffix=DTO`: например, `SpaceMarineDTO`, `CoordinatesDTO`, `ChapterDTO`, `WeaponDTO`. Доменные модели сохраняют имена `SpaceMarine`, `Coordinates`, `Chapter`, `Weapon`; это позволяет использовать обычные импорты без полных имён классов в коде. Суффикс меняет только имена Java-классов, структура JSON и пути API остаются прежними. Делегаты реализованы классами `CollectionApiDelegate` и `StarshipControllerDelegate`.

Настройки генератора: https://openapi-generator.tech/docs/generators/spring/

Комментарии и сообщения ошибок приложения написаны на русском. Локальные шаблоны OpenAPI Generator в `lab2/openapi-templates` переводят также комментарии и сообщения исключений сгенерированных классов; оба проекта используют их через `templateDirectory`. Обработчики ошибок возвращают русские описания ошибок JSON, валидации, параметров, HTTP-методов и конфликтов данных.

## PostgreSQL

Создайте базу один раз (например, в pgAdmin):

```sql
CREATE DATABASE space_marine;
```

По умолчанию оба проекта подключаются к `jdbc:postgresql://localhost:5432/space_marine`, пользователь — `root`. Пароль в репозитории не хранится: перед запуском обязательно задайте `DB_PASSWORD`. Для своей установки задайте действительного пользователя PostgreSQL и его пароль через переменные окружения PowerShell:

```powershell
$env:DB_USER = 'postgres'
$env:DB_PASSWORD = 'ваш пароль'
```

Также поддерживаются `DB_URL`, `PORT`, а для второго сервиса `COLLECTION_URL` (по умолчанию `http://localhost:8081/api/v1`). Flyway создаёт схемы `collection` и `starship` и таблицы автоматически. Пользователю БД нужны права создавать схемы и таблицы. Первый сервис хранит десантников в обычных столбцах через JPA-сущность `model.SpaceMarine`: `Coordinates` и `Chapter` являются встраиваемыми объектами `@Embeddable`, типы оружия — enum. Второй хранит JPA-сущности `model.Boarding` в собственной схеме и обращается к первому через `client.CollectionClient`, не читая его таблицы. Полученный десантник представлен объектом `model.SpaceMarine` (снимок удалённого объекта).

Миграции V1 сохранены. Новая V2 переносит существующие JSONB-данные коллекции в столбцы с сохранением id и creationDate; в посадках добавляет собственный сгенерированный id и сохраняет уникальность marine_id. На новой базе последовательно выполняются V1 и V2. Hibernate настроен с `ddl-auto=validate`, поэтому структуру изменяет только Flyway.

## Пакеты и слои

```text
controller/generated  — сгенерированные из OpenAPI контроллеры, API и интерфейсы делегатов
controller            — реализация делегатов, HTTP-ответы и обработка ошибок
dto                   — сгенерированные DTO с валидацией
mapper                — преобразование DTO ↔ model
model                 — JPA-сущности, встраиваемые объекты, enum и удалённые модели
service               — бизнес-логика и транзакции, работа с объектами model
repository            — Spring Data JpaRepository, JPA Specifications
client                — HTTP-клиент первого сервиса (в starship-service)
```

Цепочка обработки запроса: сгенерированный контроллер → делегат → mapper/service → repository → Hibernate → PostgreSQL. Сервисы не зависят от DTO или HttpServletRequest и не формируют HTTP-ответы. Фильтрация реализована через `Specification` / JPA Criteria, сортировка и пагинация — через `Sort` / `PageRequest`. Прямых SQL-запросов в Java-коде нет; SQL используется в миграциях Flyway. Отдельные bulk-delete операции репозиториев используют JPQL по сущностям.

Документация: [Spring Data JPA Specifications](https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html), [Hibernate ORM](https://docs.jboss.org/hibernate/orm/6.6/introduction/html_single/Hibernate_Introduction.html).

## Проверки

Модели используют Lombok `@Builder(toBuilder = true, setterPrefix = "with")` и `@With`. Создание выполняется через `Model.builder().with...().build()`, обновление — через `existing.toBuilder().with...().build()` или `existing.withHealth(200)`. Методы `with...` модели возвращают копию, которую нужно сохранить или присвоить; исходный объект не изменяется. В PUT сервис берёт id и creationDate из существующей сущности, обновляет только разрешённые поля и сохраняет результат через JPA merge. Для Hibernate сохранены protected-конструкторы без аргументов и доступ к полям. DTO продолжают генерироваться из OpenAPI.

`clean verify` выполняет MVC-тесты контрактных маршрутов и валидации, тест сохранения id/creationDate при обновлении, тесты HTTP-взаимодействия сервисов, а также интеграционные тесты Hibernate и репозиториев на H2: сохранение и чтение вложенных объектов, фильтры, enum, сортировка, пагинация, удаление и уникальность посадки. H2 используется только в тестах; приложения подключаются к PostgreSQL. PostgreSQL-миграции и запуск на вашей БД требуют действительных DB_USER/DB_PASSWORD.

## Сборка и запуск

Из `lab2` выполните `./mvnw.cmd clean verify` в PowerShell (на Linux/macOS — `./mvnw clean verify`). Maven Wrapper загрузит Maven автоматически; нужен Java 21 и доступ к Maven Central при первой сборке. При установленном Maven также работает `mvn clean verify`. Каждый дочерний проект можно собирать отдельно через собственный pom.xml, например `./mvnw.cmd -f collection-service/pom.xml clean verify`.

В двух терминалах:

```powershell
java -jar collection-service/target/collection-service-1.0.0.jar
java -jar starship-service/target/starship-service-1.0.0.jar
```

Контракты доступны по `http://localhost:8081/openapi.yaml` и `http://localhost:8082/openapi.yaml`.

Swagger UI: `http://localhost:8081/swagger-ui/index.html` для коллекции и `http://localhost:8082/swagger-ui/index.html` для кораблей. UI загружает исходный контракт `/openapi.yaml`. Страницы доступны после успешного запуска сервиса: при ошибке аутентификации PostgreSQL (28P01) Flyway останавливает запуск приложения, поэтому сначала нужно исправить DB_USER/DB_PASSWORD.

## Примеры запросов

```powershell
$marine = '{"name":"Marine","coordinates":{"x":1.5,"y":10},"health":100,"weaponType":"COMBI_FLAMER","chapter":{"name":"Ultramarines","world":"Macragge"}}'
Invoke-RestMethod http://localhost:8081/api/v1/space-marines -Method Post -ContentType application/json -Body $marine
Invoke-RestMethod 'http://localhost:8081/api/v1/space-marines?page=0&size=10&sort=health,desc&filter=health,gt,50'
Invoke-RestMethod http://localhost:8082/api/v1/starship/1/load/1 -Method Post
Invoke-RestMethod http://localhost:8082/api/v1/starship/1/unload-all -Method Post
```

Фильтры и сортировка принимают повторяющиеся параметры: `sort=health,desc&sort=name,asc`. Доступные поля: id, name, creationDate, health, achievements, weaponType, meleeWeapon, coordinates.x, coordinates.y, chapter.name, chapter.parentLegion, chapter.world. Несколько фильтров объединяются через AND. Неизвестные поля, операторы и неверные значения дают 400. DELETE отсутствующего элемента идемпотентен. Ошибки возвращаются в формате ApiError.

## Допущения исходного контракта

В исходном OpenAPI отсутствуют сущность корабля, реестр кораблей, API их создания и поле корабля в SpaceMarine. Поэтому любой положительный starshipId обозначает логический корабль; высадка с пустого корабля возвращает 0. Десантник может находиться только на одном корабле: повторная посадка возвращает 409. Посадка возвращает неизменённый SpaceMarine из первого API. Удаление десантника из коллекции не удаляет посадку автоматически: в контракте нет уведомления об удалении; запись убирается при unload-all. Проверка существования через HTTP и запись посадки не являются распределённой транзакцией.

Первый API определяет path id как integer без int64, хотя id в DTO — int64; это сохранено по исходному контракту. Ограничения DTO проверяются автоматически (400), конфликты состояния дают 409, ненайденный десантник — 404. При недоступности первого API второй возвращает ApiError с 503, при ошибке его ответа — 502 (инфраструктурные ответы сверх исходного контракта).
