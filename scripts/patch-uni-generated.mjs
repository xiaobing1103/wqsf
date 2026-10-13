import { readFile, writeFile } from "node:fs/promises";

const target = "./apps/client-uni/src/api/generated/endpoints.ts";
const browserChecks = [
  "if (h instanceof Headers)",
  "if (typeof Headers !== 'undefined' && h instanceof Headers)",
];
const compatibleCheck = "if (!Array.isArray(h) && 'entries' in h && typeof h.entries === 'function')";
const queryConstructor = "new URLSearchParams()";
const compatibleQueryConstructor = "createUniQueryParams()";
const queryHelperMarker = "const createUniQueryParams = () =>";
const importMarker = "import { uniGeneratedRequest } from '../generated-client';";
const queryHelper = `

const createUniQueryParams = () => {
  const entries: string[] = [];
  return {
    append(key: string, value: string) {
      entries.push(\`\${encodeURIComponent(key)}=\${encodeURIComponent(value)}\`);
    },
    toString() {
      return entries.join('&');
    },
  };
};`;
const source = await readFile(target, "utf8");
const headerOccurrenceCount = browserChecks.reduce(
  (count, check) => count + source.split(check).length - 1,
  0,
);
const queryOccurrenceCount = source.split(queryConstructor).length - 1;

if (headerOccurrenceCount === 0 && !source.includes(compatibleCheck)) {
  throw new Error("未找到 Orval Headers 类型判断，请检查生成结果是否变化");
}
if (queryOccurrenceCount === 0 && !source.includes(queryHelperMarker)) {
  throw new Error("未找到 Orval URLSearchParams 查询参数构造，请检查生成结果是否变化");
}

let patchedSource = browserChecks.reduce(
  (content, check) => content.replaceAll(check, compatibleCheck),
  source,
);
if (queryOccurrenceCount > 0) {
  if (!patchedSource.includes(importMarker)) {
    throw new Error("未找到 uniGeneratedRequest 导入位置，无法插入微信查询参数兼容函数");
  }
  patchedSource = patchedSource
    .replace(importMarker, `${importMarker}${queryHelper}`)
    .replaceAll(queryConstructor, compatibleQueryConstructor);
}
await writeFile(target, patchedSource, "utf8");
console.log(`已修复 ${headerOccurrenceCount} 处 Headers 判断和 ${queryOccurrenceCount} 处 URLSearchParams 微信兼容问题`);
