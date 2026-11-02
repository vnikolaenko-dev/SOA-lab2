import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';
import assert from 'node:assert/strict';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const executable = process.env.CHROME_PATH || [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
].find(file => fs.existsSync(file));
if (!executable) throw new Error('Set CHROME_PATH to an installed Chromium browser executable.');
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'lab2-html-check-'));
const output = path.join(root, 'api-gateway/target/html-verification');
fs.mkdirSync(output, { recursive: true });
const browser = spawn(executable, ['--headless=new', '--disable-gpu', '--no-first-run',
  '--no-default-browser-check', '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank'],
  { windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'] });
let browserError = '';
browser.stderr.on('data', chunk => { browserError += chunk; });
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const errors = [], checks = [];
let socket;
try {
  const portFile = path.join(profile, 'DevToolsActivePort');
  for (let i = 0; i < 100 && !fs.existsSync(portFile); i++) await delay(100);
  if (!fs.existsSync(portFile)) throw new Error(`Browser did not open a debugging port: ${browserError.slice(-1000)}`);
  const port = fs.readFileSync(portFile, 'utf8').split('\n')[0];
  const pages = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
  socket = new WebSocket(pages.find(page => page.type === 'page').webSocketDebuggerUrl);
  await new Promise((resolve, reject) => { socket.addEventListener('open', resolve, { once: true }); socket.addEventListener('error', reject, { once: true }); });
  const pending = new Map(); let nextId = 0;
  socket.addEventListener('message', event => {
    const message = JSON.parse(event.data);
    if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.text);
    if (!message.id) return;
    const item = pending.get(message.id); if (!item) return;
    pending.delete(message.id); clearTimeout(item.timer);
    if (message.error) item.reject(new Error(JSON.stringify(message.error))); else item.resolve(message.result);
  });
  const send = (method, params = {}) => new Promise((resolve, reject) => {
    const id = ++nextId;
    const timer = setTimeout(() => { pending.delete(id); reject(new Error(`CDP timed out: ${method}`)); }, 15000);
    pending.set(id, { resolve, reject, timer }); socket.send(JSON.stringify({ id, method, params }));
  });
  const evaluate = async expression => {
    const response = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    if (response.exceptionDetails) throw new Error(JSON.stringify(response.exceptionDetails));
    return response.result.value;
  };
  const check = (name, condition) => { assert.ok(condition, name); checks.push(name); console.log(`PASS: ${name}`); };
  await send('Page.enable'); await send('Runtime.enable');
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1080, deviceScaleFactor: 1, mobile: false });
  await send('Page.navigate', { url: pathToFileURL(path.join(root, 'docs/architecture.html')).href });
  for (let i = 0; i < 100; i++) { if (await evaluate('document.readyState === "complete" && !!document.querySelector("#map-flow .node")')) break; await delay(100); }
  check('Page runs directly from file:// with no JavaScript exceptions', errors.length === 0);
  check('All in-page links have targets', await evaluate(`[...document.querySelectorAll('a[href^="#"]')].every(a=>!!document.querySelector(a.getAttribute('href')))`));
  check('Map tabs change the diagram', await evaluate('document.querySelector("[data-map=config]").click();document.querySelector("#map-flow").textContent.includes("Config Server")'));
  check('Map nodes show explanations', await evaluate('document.querySelectorAll("#map-flow .node")[1].click();document.querySelector("#map-detail").textContent.includes("@EnableConfigServer")'));
  check('Request step advances', await evaluate('document.querySelector("#step-next").click();document.querySelector("#step-count").textContent.startsWith("1 /")'));
  check('All request scenarios complete', await evaluate(`(()=>{const select=document.querySelector('#scenario');for(const option of select.options){select.value=option.value;select.dispatchEvent(new Event('change'));for(let i=0;i<12&&!document.querySelector('#step-next').disabled;i++)document.querySelector('#step-next').click();if(!document.querySelector('#step-next').disabled)return false;}return true})()`));
  check('Ribbon distributes ten requests evenly', await evaluate('document.querySelector("#balance-ten").click();document.querySelector("#count-a").textContent==="5"&&document.querySelector("#count-b").textContent==="5"'));
  check('Ribbon handles an empty server list', await evaluate('document.querySelector("#server-a").checked=false;document.querySelector("#server-b").checked=false;document.querySelector("#balance-one").click();document.querySelector("#balance-log").textContent.includes("HTTP не отправлен")'));
  check('Eureka model shows stale server registry', await evaluate('document.querySelector("#registry-crash").click();document.querySelector("#process-list .offline")!==null&&document.querySelector("#eureka-list .offline")===null'));
  check('Eureka model separates expiry and client fetch', await evaluate('document.querySelector("#registry-expire").click();const stale=!document.querySelector("#client-list .offline");document.querySelector("#registry-fetch").click();stale&&!!document.querySelector("#client-list .offline")'));
  check('Technology search filters visible cards', await evaluate('const search=document.querySelector("#tech-search");search.value="Flyway";search.dispatchEvent(new Event("input"));document.querySelectorAll(".tech:not([hidden])").length>0&&document.querySelectorAll(".tech:not([hidden])").length<12'));
  check('Every code selector loads the matching file', await evaluate(`(()=>{const select=document.querySelector('#source-select');for(const option of select.options){select.value=option.value;select.dispatchEvent(new Event('change'));if(document.querySelector('#source-code').textContent.length<20)return false;}return select.options.length===Object.keys(SOURCES).length})()`));
  check('No page-level horizontal overflow on desktop', await evaluate('document.documentElement.scrollWidth <= innerWidth'));
  await evaluate(`document.querySelector('[data-map=business]').click();document.querySelector('#scenario').value='load';document.querySelector('#scenario').dispatchEvent(new Event('change'));document.querySelector('#tech-search').value='';document.querySelector('#tech-search').dispatchEvent(new Event('input'));document.querySelector('#source-select').value='routes';document.querySelector('#source-select').dispatchEvent(new Event('change'));scrollTo(0,0)`);
  const desktop = await send('Page.captureScreenshot', { format: 'png' });
  fs.writeFileSync(path.join(output, 'desktop.png'), Buffer.from(desktop.data, 'base64'));
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  await send('Page.reload');
  await delay(500);
  check('Mobile viewport uses the requested CSS width', await evaluate('document.documentElement.clientWidth === 390'));
  if (!(await evaluate('document.documentElement.scrollWidth <= 390'))) {
    console.log(await evaluate(`JSON.stringify([...document.querySelectorAll('body *')].filter(e=>e.getBoundingClientRect().right>390&&getComputedStyle(e).position!=='fixed').map(e=>({tag:e.tagName,id:e.id,class:e.className,right:e.getBoundingClientRect().right})).slice(0,20))`));
  }
  check('No page-level horizontal overflow on mobile', await evaluate('document.documentElement.scrollWidth <= 390'));
  const mobile = await send('Page.captureScreenshot', { format: 'png' });
  fs.writeFileSync(path.join(output, 'mobile.png'), Buffer.from(mobile.data, 'base64'));
  check('No JavaScript errors after all interactions', errors.length === 0);
  fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify({ checks, errors }, null, 2));
  console.log(`Results and screenshots: ${output}`);
} finally {
  if (socket) socket.close();
  browser.kill();
  // Keep the isolated temporary browser profile; do not delete paths recursively.
}
