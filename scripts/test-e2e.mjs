import assert from 'node:assert/strict';
import { mkdir, writeFile } from 'node:fs/promises';

const gateway = process.env.TEST_GATEWAY_URL ?? 'http://localhost:18080';
const soapUrl = process.env.TEST_SOAP_URL ?? 'http://localhost:8082/services/starships';
const rest = `${gateway}/api/starship-service`;
const collection = `${gateway}/api/collection-service`;
const results = [];
const created = [];
const ship = 1700000000 + Math.floor(Math.random() * 1000000);
const clientSchemas = process.argv.includes('--client-schemas') ? Object.assign({}, ...await Promise.all(
  ['spaceMarine', 'pageOfStarship', 'pageOfSpaceMarine', 'unloadAll200', 'apiError'].map(name =>
    import(new URL(`../soa-front/src/api/generated/starships/models/${name}.zod.ts`, import.meta.url)))
)) : null;
async function request(url, method = 'GET', body) {
  const response = await fetch(url, {
    method, headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined, signal: AbortSignal.timeout(25000),
  });
  const text = await response.text();
  let data;
  try { data = JSON.parse(text); } catch { data = text; }
  return { status: response.status, data };
}
async function check(name, fn) {
  try { await fn(); results.push({ name, passed: true }); console.log(`PASS ${name}`); }
  catch (error) { results.push({ name, passed: false, error: error.message }); console.error(`FAIL ${name}: ${error.message}`); }
}
async function status(url, expected, method = 'GET') {
  const result = await request(url, method);
  assert.equal(result.status, expected, JSON.stringify(result.data));
  if (clientSchemas && url.startsWith(rest)) {
    const path = new URL(url).pathname;
    const schema = expected >= 400 ? clientSchemas.ApiError
      : path.endsWith('/starships') ? clientSchemas.PageOfStarship
      : path.endsWith('/space-marines') ? clientSchemas.PageOfSpaceMarine
      : path.endsWith('/unload-all') ? clientSchemas.UnloadAll200 : clientSchemas.SpaceMarine;
    schema.parse(result.data);
  }
  if (expected >= 400) {
    assert.equal(result.data.status, expected);
    assert.equal(typeof result.data.message, 'string');
    assert.ok(Object.hasOwn(result.data, 'field'));
  }
  return result.data;
}
async function soap(operation, args = '') {
  const response = await fetch(soapUrl, {
    method: 'POST', headers: { 'Content-Type': 'text/xml; charset=utf-8', SOAPAction: '""' },
    body: `<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" xmlns:t="urn:soa:starship:v1"><s:Body><t:${operation}>${args}</t:${operation}></s:Body></s:Envelope>`,
    signal: AbortSignal.timeout(25000),
  });
  return { status: response.status, xml: await response.text() };
}
try {
  await check('WSDL publishes all four typed SOAP operations', async () => {
    const response = await fetch(`${soapUrl}?wsdl`);
    assert.equal(response.status, 200);
    const wsdl = await response.text();
    for (const name of ['getStarships', 'getStarshipSpaceMarines', 'loadSpaceMarine', 'unloadAll', 'StarshipFault']) assert.ok(wsdl.includes(name), name);
    assert.ok(wsdl.includes('http://schemas.xmlsoap.org/wsdl/soap/'));
  });
  await check('SOAP service no longer exposes old REST endpoints', async () => {
    assert.equal((await request(`${new URL(soapUrl).origin}/api/v1/starships`)).status, 404);
  });
  await check('Direct SOAP call returns an XML envelope', async () => {
    const result = await soap('getStarships', '<page>0</page><size>10</size>');
    assert.equal(result.status, 200); assert.match(result.xml, /Envelope/); assert.match(result.xml, /getStarshipsResponse/);
  });
  await check('SOAP rejects negative identifier using a typed fault', async () => {
    const result = await soap('unloadAll', '<starshipId>-1</starshipId>');
    assert.equal(result.status, 500); assert.match(result.xml, /Fault/); assert.match(result.xml, /<status>400<\/status>/);
  });
  await check('Unknown ship has an empty JSON array and correct pagination', async () => {
    const page = await status(`${rest}/starship/${ship}/space-marines`, 200);
    assert.deepEqual(page, { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  });
  await check('Unloading an unknown ship returns zero', async () => assert.equal(await status(`${rest}/starship/${ship}/unload-all`, 200, 'POST'), 0));
  for (const [suffix, expected] of [['?page=-1', 400], ['?size=0', 400], ['?size=101', 400], ['?page=x', 400]]) {
    await check(`Pagination error ${suffix} preserves ApiError JSON`, () => status(`${rest}/starships${suffix}`, expected));
  }
  await check('Wrong HTTP method preserves 405 JSON', () => status(`${rest}/starships`, 405, 'POST'));
  await check('Negative identifier is validated by SOAP and mapped to REST 400', () => status(`${rest}/starship/-1/unload-all`, 400, 'POST'));
  await check('Missing marine maps SOAP domain error to REST 404', () => status(`${rest}/starship/${ship}/load/2147483647`, 404, 'POST'));
  for (const [index, health] of [80, 120].entries()) {
    await check(`Create real marine ${index + 1} in collection`, async () => {
      const result = await request(`${collection}/space-marines`, 'POST', {
        name: index === 0 ? `SOAP, roundtrip ${ship}` : `SOAP second ${ship}`,
        coordinates: { x: 12.5, y: 7 }, health, achievements: null,
        weaponType: 'COMBI_FLAMER', meleeWeapon: null,
        chapter: { name: 'Integration', parentLegion: null, world: 'Terra' },
      });
      assert.equal(result.status, 201, JSON.stringify(result.data)); created.push(result.data);
      // PostgreSQL stores timestamps at microsecond precision. Compare SOAP with
      // the persisted GET representation, rather than the pre-flush POST entity.
      const persisted = await status(`${collection}/space-marines/${result.data.id}`, 200);
      created[created.length - 1] = persisted;
    });
  }
  assert.equal(created.length, 2, 'Cannot exercise boarding without two created marines');
  for (const marine of created) {
    await check(`Board marine ${marine.id}: JSON fields survive SOAP roundtrip`, async () => {
      const result = await status(`${rest}/starship/${ship}/load/${marine.id}`, 200, 'POST');
      assert.deepEqual(result, marine); assert.ok(!Number.isNaN(Date.parse(result.creationDate)));
    });
  }
  await check('Already boarded marine returns 409 on a second ship', () => status(`${rest}/starship/${ship + 1}/load/${created[0].id}`, 409, 'POST'));
  await check('Ship list includes the boarded ship', async () => {
    const page = await status(`${rest}/starships?size=100`, 200);
    assert.ok(page.content.some(item => item.id === ship));
  });
  await check('Crew pagination uses real boarding data', async () => {
    const page = await status(`${rest}/starship/${ship}/space-marines?page=1&size=1`, 200);
    assert.equal(page.content[0].id, created[1].id); assert.equal(page.totalElements, 2); assert.equal(page.totalPages, 2);
  });
  await check('Repeated filters combine with AND and preserve commas in names', async () => {
    const query = new URLSearchParams();
    query.append('filter', `name,eq,${created[0].name}`); query.append('filter', 'health,lt,100');
    const page = await status(`${rest}/starship/${ship}/space-marines?${query}`, 200);
    assert.deepEqual(page.content.map(item => item.id), [created[0].id]); assert.equal(page.totalElements, 1);
  });
  await check('Invalid filter is a SOAP validation error mapped to 400', () => status(`${rest}/starship/${ship}/space-marines?filter=unknown,eq,x`, 400));
  await check('Unload returns the actual crew size', async () => assert.equal(await status(`${rest}/starship/${ship}/unload-all`, 200, 'POST'), 2));
  await check('Repeated unload returns zero', async () => assert.equal(await status(`${rest}/starship/${ship}/unload-all`, 200, 'POST'), 0));
  await check('Unloaded ship has empty crew', async () => assert.deepEqual((await status(`${rest}/starship/${ship}/space-marines`, 200)).content, []));
  await check('Unloaded ship disappears from ship list', async () => assert.ok(!(await status(`${rest}/starships?size=100`, 200)).content.some(item => item.id === ship)));
} catch (error) {
  results.push({ name: 'Suite setup', passed: false, error: error.message }); console.error(error);
} finally {
  await request(`${rest}/starship/${ship}/unload-all`, 'POST').catch(() => {});
  for (const marine of created) await request(`${collection}/space-marines/${marine.id}`, 'DELETE').catch(() => {});
  const report = { timestamp: new Date().toISOString(), gateway, soapUrl, clientSchemas: Boolean(clientSchemas), passed: results.filter(r => r.passed).length, failed: results.filter(r => !r.passed).length, results };
  await mkdir('.local-run', { recursive: true });
  await writeFile('.local-run/e2e-results.json', JSON.stringify(report, null, 2));
  console.log(`${report.passed} passed, ${report.failed} failed`);
  process.exitCode = report.failed ? 1 : 0;
}
