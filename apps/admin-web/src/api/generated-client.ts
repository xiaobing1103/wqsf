import { rawApiRequest } from "./request";

/** Orval 生成函数使用的后台请求适配器，请勿在生成目录内修改。 */
export const adminGeneratedRequest = <T>(
  url: string,
  options: RequestInit = {},
): Promise<T> => rawApiRequest<T>(normalizeGeneratedUrl(url), {
  ...options,
  auth: !isPublicOperation(url),
});

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

export type ErrorType<Error> = Error;
export type BodyType<BodyData> = BodyData;
