export interface LoginUser {
  id: number;
  displayName: string;
  phoneMask: string;
}

export interface LoginResult {
  token: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: LoginUser;
}

export interface AdminUser {
  id: number;
  displayName: string;
  accountType: "ADMIN";
  roles: string[];
  permissions: string[];
}

export interface AdminLoginPayload {
  username: string;
  password: string;
}
