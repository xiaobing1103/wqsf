export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  requestId: string;
  timestamp: string;
}

export interface ApiErrorDetails {
  fields?: Record<string, string>;
  [key: string]: unknown;
}

export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly status: number,
    public readonly details?: ApiErrorDetails,
    public readonly requestId?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}
