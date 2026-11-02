import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const files = {
  gateway: 'api-gateway/src/main/java/soa/lab2/gateway/GatewayApplication.java',
  routes: 'api-gateway/src/main/resources/application.properties',
  path: 'api-gateway/src/main/java/soa/lab2/gateway/ApiPathFilter.java',
  location: 'api-gateway/src/main/java/soa/lab2/gateway/ApiLocationFilter.java',
  ribbon: 'starship-service/src/main/java/soa/lab2/starship/client/RibbonRequestInterceptor.java',
  rest: 'starship-service/src/main/java/soa/lab2/starship/client/CollectionClientConfiguration.java',
  client: 'starship-service/src/main/java/soa/lab2/starship/client/CollectionClient.java',
  config: 'config-server/src/main/resources/application.properties',
  common: 'config-server/src/main/resources/config-repository/application.properties',
  collectionConfig: 'config-server/src/main/resources/config-repository/collection-service.properties',
  starshipConfig: 'config-server/src/main/resources/config-repository/starship-service.properties',
  gatewayConfig: 'config-server/src/main/resources/config-repository/api-gateway.properties',
  bootstrap: 'api-gateway/src/main/resources/bootstrap.properties',
  modernConfig: 'starship-service/src/main/resources/application.properties',
  discovery: 'eureka-server/src/main/java/soa/lab2/discovery/EurekaServerApplication.java',
  entity: 'collection-service/src/main/java/soa/lab2/collection/model/SpaceMarine.java',
  command: 'starship-service/src/main/java/soa/lab2/starship/service/StarshipCommandService.java',
  query: 'starship-service/src/main/java/soa/lab2/starship/service/StarshipQueryService.java',
  delegate: 'collection-service/src/main/java/soa/lab2/collection/controller/CollectionApiDelegate.java',
  errors: 'collection-service/src/main/java/soa/lab2/collection/controller/Errors.java',
  migration: 'starship-service/src/main/resources/db/migration/V1__boarding.sql',
  compose: 'compose.yaml',
  caddy: 'Caddyfile',
  docker: 'api-gateway/Dockerfile',
  gatewayTest: 'api-gateway/src/test/java/soa/lab2/gateway/GatewayRoutingTest.java',
  ribbonTest: 'starship-service/src/test/java/soa/lab2/starship/client/RibbonRequestInterceptorTest.java',
  liveTest: 'scripts/verify-gateway.ps1',
};
const sources = Object.fromEntries(Object.entries(files).map(([key, file]) => [key, {
  file,
  code: fs.readFileSync(path.join(root, file), 'utf8')
    .replace(/^spring\.datasource\.password=.*$/m, 'spring.datasource.password=${DB_PASSWORD}'),
}]));
const resultsPath = path.join(root, 'api-gateway/target/live-verification/results.json');
const checks = fs.existsSync(resultsPath)
  ? JSON.parse(fs.readFileSync(resultsPath, 'utf8').replace(/^\uFEFF/, ''))
  : { scope: 'Сквозная проверка ещё не завершена', checks: [] };
const inline = value => JSON.stringify(value).replace(/</g, '\\u003c');
const template = fs.readFileSync(path.join(root, 'docs/architecture.template.html'), 'utf8');
const html = template.replace('__PROJECT_SOURCES__', () => inline(sources))
  .replace('__CHECK_RESULTS__', () => inline(checks));
fs.writeFileSync(path.join(root, 'docs/architecture.html'), html);
console.log(`Generated docs/architecture.html: ${Object.keys(sources).length} code examples, ${checks.checks.length} live checks`);
