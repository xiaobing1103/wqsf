import { ApiError } from "../types/api";

/**
 * 统一拆解 Orval 返回的 ApiResponse。
 * 页面层不直接判断 success，接口异常统一交给 request.ts 转换为 ApiError。
 */
export function unwrapResponse<T>(response: {
  success?: boolean;
  code?: string;
  message?: string;
  data?: T;
  requestId?: string;
}): T {
  if (!response.success || response.data === undefined) {
    throw new ApiError(
      response.code || "EMPTY_RESPONSE",
      response.message || "服务未返回业务数据",
      200,
      undefined,
      response.requestId,
    );
  }
  return response.data;
}
