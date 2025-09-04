export enum SalesRole {
  ADMIN = 'ADMIN',
  SALES = 'SALES'
}

export interface Sales {
  phone: string;
  role: SalesRole;
}

export interface LoginRequest {
  phone: string;
  password: string;
}

export interface RegisterRequest {
  phone: string;
  password: string;
  confirmPassword: string;
}

export interface AuthResponse {
  token?: string;
  phone?: string;
  role?: SalesRole;
  error?: string;
}

export interface ValidateTokenRequest {
  token: string;
}