import { mkdir, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";

const apiUrl = process.env.OPENAPI_URL ?? "http://127.0.0.1:8080/v3/api-docs";
const output = resolve(process.cwd(), "openapi/万企商服通_OpenAPI_V1.json");

function fail(message) {
  console.error(`OpenAPI 导出失败：${message}`);
  process.exit(1);
}

let response;
try {
  response = await fetch(apiUrl, { headers: { Accept: "application/json" } });
} catch (error) {
  fail(`无法连接 ${apiUrl}，请先启动后端服务（${error.message}）`);
}
if (!response.ok) fail(`后端返回 HTTP ${response.status}`);

let document;
try {
  document = await response.json();
} catch {
  fail("后端返回内容不是合法 JSON");
}

const operations = Object.values(document.paths ?? {}).flatMap((path) => Object.values(path));
const operationIds = operations.map((operation) => operation.operationId).filter(Boolean);
const duplicateIds = operationIds.filter((id, index) => operationIds.indexOf(id) !== index);
const chinese = /[\u4e00-\u9fff]/;
if (document.info?.title !== "万企商服通接口文档") fail("接口标题不是中文产品标题");
if (document.info?.version !== "1.0.0") fail("接口版本不是 1.0.0");
if (operations.length < 40) fail(`接口数量异常：${operations.length}`);
if (duplicateIds.length > 0) fail(`operationId 重复：${[...new Set(duplicateIds)].join(", ")}`);
if (operations.some((operation) => !operation.operationId || !chinese.test(operation.summary ?? "")))
  fail("存在缺少稳定 operationId 或中文 summary 的接口");
if (!document.components?.securitySchemes?.BearerAuth) fail("缺少 BearerAuth 鉴权定义");

await mkdir(dirname(output), { recursive: true });
await writeFile(output, `${JSON.stringify(document, null, 2)}\n`, "utf8");
console.log(`已导出 ${output}`);
console.log(`接口操作数：${operations.length}，路径数：${Object.keys(document.paths ?? {}).length}`);
