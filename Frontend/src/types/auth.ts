export type AuthRole = "ADMIN" | "RECEPTIONIST" | "DOCTOR" | "CASHIER" | "PATIENT" | string;

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  username: string;
  role: AuthRole;
}

export interface AuthSession extends AuthResponse {}

export interface JwtPayload {
  sub?: string;
  role?: string;
  exp?: number;
  iat?: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterPatientRequest {
  username: string;
  password: string;
  fullName: string;
  gender?: string | null;
  nationalId?: string | null;
  healthInsuranceNumber?: string | null;
  phoneNumber?: string | null;
  gmail?: string | null;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface VerifyForgotPasswordOtpRequest {
  email: string;
  otp: string;
}

export interface ResetPasswordWithOtpRequest {
  email: string;
  newPassword: string;
}
