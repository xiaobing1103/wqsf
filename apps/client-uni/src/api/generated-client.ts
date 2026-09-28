const API_BASE_URL = (((import.meta as ImportMeta & { env?: Record<string, string> }).env?.VITE_API_BASE_URL)
  || "http://127.0.0.1:8080/api/v1").replace(/\/$/, "");
export const CLIENT_TOKEN_STORAGE_KEY = "wqst_client_token";

type UniRequestOptions = RequestInit & { body?: unknown };

/** Orval 生成函数使用的 uni.request 适配器；文件上传请调用 upload.ts。 */
export const uniGeneratedRequest = <T>(url: string, options: UniRequestOptions = {}): Promise<T> => {
  if (typeof FormData !== "undefined" && options.body instanceof FormData) {
    return Promise.reject(new Error("小程序文件请使用 uni.uploadFile 适配器，不要使用普通 JSON 请求"));
  }
  const headers: Record<string, string> = {};
  if (Array.isArray(options.headers)) options.headers.forEach(([key, value]) => { headers[key] = value; });
  else if (options.headers instanceof Headers) options.headers.forEach((value, key) => { headers[key] = value; });
  else if (options.headers) Object.assign(headers, options.headers);
  headers.Accept ??= "application/json";
  headers["X-Request-Id"] ??= `uni-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  const token = uni.getStorageSync(CLIENT_TOKEN_STORAGE_KEY);
  if (token && !isPublicOperation(url)) headers.Authorization = `Bearer ${token}`;
  const body = typeof options.body === "string" ? safelyParse(options.body) : options.body;
  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${normalizeGeneratedUrl(url)}`,
      method: (options.method || "GET") as UniRequestOptions["method"],
      header: headers,
      data: body as string | ArrayBuffer | Record<string, unknown>,
      success: (response) => {
        const payload = response.data as { success?: boolean; code?: string; message?: string; data?: unknown };
        if (response.statusCode < 200 || response.statusCode >= 300 || payload?.success === false) {
          reject(new Error(payload?.message || `请求失败（${response.statusCode}）`));
          return;
        }
        resolve(response.data as T);
      },
      fail: (error) => reject(new Error(error.errMsg || "无法连接后端服务")),
    } as Uni.RequestOptions);
  });
};

function normalizeGeneratedUrl(url: string) {
  return url.startsWith("/api/v1") ? url.slice("/api/v1".length) || "/" : url;
}

function isPublicOperation(url: string) {
  return url === "/api/v1/auth/admin/login"
    || url === "/api/v1/auth/client/dev-login"
    || url === "/api/v1/auth/wechat/login"
    || url === "/api/v1/services/catalog"
    || /^\/api\/v1\/products\/\d+\/material-template(?:\?|$)/.test(url);
}

function safelyParse(value: string): unknown {
  try { return JSON.parse(value); } catch { return value; }
}

export type ErrorType<Error> = Error;
export type BodyType<BodyData> = BodyData;
