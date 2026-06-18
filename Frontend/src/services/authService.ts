import { clearStoredAuthSession, getStoredAuthSession, saveStoredAuthSession, api, setApiTimeout } from "./api";
import type {
  AuthResponse,
  AuthSession,
  ForgotPasswordRequest,
  JwtPayload,
  LoginRequest,
  RegisterPatientRequest,
  ResetPasswordWithOtpRequest,
  VerifyForgotPasswordOtpRequest,
} from "@/types/auth";

const normalizeRole = (role: string) => role.toUpperCase();
const PROACTIVE_REFRESH_WINDOW_SECONDS = 60;

const toSession = (response: AuthResponse): AuthSession => ({
  token: response.token,
  refreshToken: response.refreshToken,
  username: response.username,
  role: normalizeRole(response.role),
});

const decodeBase64 = (value: string) => {
  if (typeof window !== "undefined" && typeof window.atob === "function") {
    return window.atob(value);
  }

  return Buffer.from(value, "base64").toString("utf-8");
};

export const decodeJwtPayload = (token: string): JwtPayload | null => {
  try {
    const payloadPart = token.split(".")[1];
    if (!payloadPart) {
      return null;
    }

    const base64 = payloadPart.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
    return JSON.parse(decodeBase64(padded)) as JwtPayload;
  } catch {
    return null;
  }
};

export const isTokenExpired = (token: string) => {
  const payload = decodeJwtPayload(token);
  if (!payload?.exp) {
    return false;
  }

  return Date.now() >= payload.exp * 1000;
};

const shouldRefreshTokenProactively = (token: string) => {
  const payload = decodeJwtPayload(token);
  if (!payload?.exp) {
    return false;
  }

  return payload.exp * 1000 - Date.now() <= PROACTIVE_REFRESH_WINDOW_SECONDS * 1000;
};

export const getCurrentAuthSession = (): AuthSession | null => {
  const stored = getStoredAuthSession();
  if (!stored) {
    return null;
  }

  if (isTokenExpired(stored.token)) {
    clearStoredAuthSession();
    return null;
  }

  return stored;
};

export const authService = {
  async refreshSession() {
    const stored = getStoredAuthSession();
    const refreshToken = stored?.refreshToken;

    if (!refreshToken) {
      clearStoredAuthSession();
      return null;
    }

    const response = await api.post<AuthResponse>("/api/auth/refresh", { refreshToken });
    const session = toSession(response.data);
    saveStoredAuthSession(session);
    try {
      const s = getStoredAuthSession();
      if (s?.clientTimeoutMs) setApiTimeout(Number(s.clientTimeoutMs));
    } catch {}
    return session;
  },

  async initializeSession() {
    const stored = getStoredAuthSession();
    if (!stored) {
      return null;
    }

    if (shouldRefreshTokenProactively(stored.token)) {
      try {
        return await this.refreshSession();
      } catch {
        clearStoredAuthSession();
        return null;
      }
    }

    try {
      if (stored?.clientTimeoutMs) setApiTimeout(Number(stored.clientTimeoutMs));
    } catch {}

    return stored;
  },

  async login(payload: LoginRequest) {
    const response = await api.post<AuthResponse>("/api/auth/login", payload);
    const session = toSession(response.data);
    saveStoredAuthSession(session);
    try {
      const s = getStoredAuthSession();
      if (s?.clientTimeoutMs) setApiTimeout(Number(s.clientTimeoutMs));
    } catch {}
    return session;
  },

  async registerPatient(payload: RegisterPatientRequest) {
    const response = await api.post<AuthResponse>("/api/auth/register/patient", payload);
    const session = toSession(response.data);
    saveStoredAuthSession(session);
    try {
      const s = getStoredAuthSession();
      if (s?.clientTimeoutMs) setApiTimeout(Number(s.clientTimeoutMs));
    } catch {}
    return session;
  },

  async sendForgotPasswordOtp(payload: ForgotPasswordRequest) {
    const response = await api.post<string>("/api/auth/forgot-password/send-otp", payload);
    return response.data;
  },

  async verifyForgotPasswordOtp(payload: VerifyForgotPasswordOtpRequest) {
    const response = await api.post<string>("/api/auth/forgot-password/verify-otp", payload);
    return response.data;
  },

  async resetPasswordWithOtp(payload: ResetPasswordWithOtpRequest) {
    const response = await api.post<string>("/api/auth/forgot-password/reset", payload);
    return response.data;
  },

  async logout() {
    const stored = getStoredAuthSession();
    const refreshToken = stored?.refreshToken;

    if (refreshToken) {
      try {
        await api.post<string>("/api/auth/logout", { refreshToken });
      } catch {
        // Logout must be resilient even when token is already invalid or network fails.
      }
    }

    clearStoredAuthSession();
  },
};
