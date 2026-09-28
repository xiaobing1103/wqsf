const API_BASE_URL = (((import.meta as ImportMeta & { env?: Record<string, string> }).env?.VITE_API_BASE_URL)
  || "http://127.0.0.1:8080/api/v1").replace(/\/$/, "");
export const CLIENT_TOKEN_STORAGE_KEY = "wqst_client_token";

export type UploadMaterialOptions = {
  caseId: number;
  materialId: number;
  filePath: string;
  idempotencyKey?: string;
};

/** 小程序文件必须通过 uni.uploadFile 发送，避免把本地临时路径当作 JSON。 */
export function uploadMaterialFile(options: UploadMaterialOptions): Promise<unknown> {
  const token = uni.getStorageSync(CLIENT_TOKEN_STORAGE_KEY);
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE_URL}/files`,
      filePath: options.filePath,
      name: "file",
      formData: { caseId: String(options.caseId), materialId: String(options.materialId) },
      header: {
        Accept: "application/json",
        Authorization: token ? `Bearer ${token}` : "",
        "X-Request-Id": `uni-upload-${Date.now()}-${Math.random().toString(16).slice(2)}`,
        ...(options.idempotencyKey ? { "Idempotency-Key": options.idempotencyKey } : {}),
      },
      success: (response) => {
        let payload: { success?: boolean; message?: string; data?: unknown };
        try { payload = JSON.parse(response.data); } catch { reject(new Error("服务返回了无法识别的数据")); return; }
        if (response.statusCode < 200 || response.statusCode >= 300 || payload.success === false) {
          reject(new Error(payload.message || `上传失败（${response.statusCode}）`));
          return;
        }
        resolve(payload.data);
      },
      fail: (error) => reject(new Error(error.errMsg || "文件上传失败")),
    });
  });
}
