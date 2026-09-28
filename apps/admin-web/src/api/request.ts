import { ApiError, type ApiErrorDetails, type ApiResponse } from "../types/api";

export const ADMIN_TOKEN_STORAGE_KEY = "wqst_admin_token";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "/api/v1").replace(
  /\/$/,
  "",
);

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  auth?: boolean;
};

export type RawRequestOptions = RequestOptions;

function createRequestId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto)
    return crypto.randomUUID();
  return `web-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function redirectToLogin() {
  sessionStorage.removeItem(ADMIN_TOKEN_STORAGE_KEY);
  if (window.location.pathname === "/login") return;
  const redirect = `${window.location.pathname}${window.location.search}`;
  window.location.assign(`/login?redirect=${encodeURIComponent(redirect)}`);
}

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const payload = await rawApiRequest<ApiResponse<T>>(path, options);
  return payload.data;
}

/**
 * 返回完整的统一响应包装，供 Orval 生成的 API 函数使用。
 * 业务页面继续使用 apiRequest<T>，避免把 success/code 等协议字段散落到页面。
 */
export async function rawApiRequest<T>(
  path: string,
  options: RawRequestOptions = {},
): Promise<T> {
  const {
    body,
    auth = true,
    headers: initialHeaders,
    ...requestInit
  } = options;
  const headers = new Headers(initialHeaders);
  headers.set("Accept", "application/json");
  headers.set("X-Request-Id", createRequestId());

  if (body !== undefined && !(body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  if (auth) {
    const token = sessionStorage.getItem(ADMIN_TOKEN_STORAGE_KEY);
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...requestInit,
      headers,
      body:
        body === undefined || body instanceof FormData || typeof body === "string"
          ? body
          : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(
      "NETWORK_ERROR",
      "无法连接后端服务，请确认服务已经启动",
      0,
    );
  }

  let payload: ApiResponse<T> | null = null;
  try {
    payload = (await response.json()) as ApiResponse<T>;
  } catch {
    throw new ApiError(
      "INVALID_RESPONSE",
      "服务返回了无法识别的数据",
      response.status,
    );
  }

  if (response.status === 401 && auth) redirectToLogin();

  if (!response.ok || !payload.success) {
    throw new ApiError(
      payload.code || "REQUEST_FAILED",
      payload.message || "请求失败",
      response.status,
      payload.data as ApiErrorDetails | undefined,
      payload.requestId,
    );
  }

  return payload as T;
}
