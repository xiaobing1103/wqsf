import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const source = resolve(process.cwd(), process.argv[2] ?? "openapi/万企商服通_OpenAPI_V1.json");
const document = JSON.parse(await readFile(source, "utf8"));
const operations = Object.values(document.paths ?? {}).flatMap((path) => Object.values(path));
const ids = operations.map((operation) => operation.operationId);
const chinese = /[\u4e00-\u9fff]/;
const duplicates = ids.filter((id, index) => ids.indexOf(id) !== index);
const errors = [];
if (document.info?.title !== "万企商服通接口文档") errors.push("标题不是中文产品标题");
if (document.info?.version !== "1.0.0") errors.push("版本不是 1.0.0");
if (operations.length < 40) errors.push(`接口操作数异常：${operations.length}`);
if (duplicates.length) errors.push(`operationId 重复：${[...new Set(duplicates)].join(", ")}`);
if (operations.some((operation) => !operation.operationId || !chinese.test(operation.summary ?? "")))
  errors.push("存在缺少稳定 operationId 或中文 summary 的接口");
if (!document.components?.securitySchemes?.BearerAuth) errors.push("缺少 BearerAuth 鉴权定义");
if (errors.length) {
  console.error(errors.map((error) => `- ${error}`).join("\n"));
  process.exit(1);
}
console.log(`OpenAPI 校验通过：${operations.length} 个操作，${Object.keys(document.paths ?? {}).length} 个路径`);
