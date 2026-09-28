declare namespace Uni {
  type RequestOptions = {
    url: string;
    method?: string;
    header?: Record<string, string>;
    data?: string | ArrayBuffer | Record<string, unknown>;
    success?: (response: { statusCode: number; data: unknown }) => void;
    fail?: (error: { errMsg?: string }) => void;
  };
}

declare const uni: {
  getStorageSync(key: string): string;
  request(options: Uni.RequestOptions): unknown;
  uploadFile(options: {
    url: string;
    filePath: string;
    name: string;
    formData?: Record<string, string>;
    header?: Record<string, string>;
    success?: (response: { statusCode: number; data: string }) => void;
    fail?: (error: { errMsg?: string }) => void;
  }): unknown;
};
