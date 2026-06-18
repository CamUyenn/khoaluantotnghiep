import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081";
const AUTH_STORAGE_KEY = "clinic-auth-session";

const DEFAULT_TIMEOUT_MS = Number(process.env.NEXT_PUBLIC_API_TIMEOUT_MS ?? 10000);
const CLIENT_TIMEOUT_STORAGE_KEY = "client.requestTimeoutMs";

export interface StoredAuthSession {
  token: string;
  refreshToken?: string;
  username: string;
  role: string;
  clientTimeoutMs?: number;
}

export const getStoredAuthSession = (): StoredAuthSession | null => {
  if (typeof window === "undefined") {
    return null;
  }

  const rawValue = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!rawValue) {
    return null;
  }

  try {
    return JSON.parse(rawValue) as StoredAuthSession;
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
};

export const saveStoredAuthSession = (session: StoredAuthSession) => {
  if (typeof window === "undefined") {
    return;
  }

  try {
    const existingRaw = window.localStorage.getItem(AUTH_STORAGE_KEY);
    if (existingRaw) {
      try {
        const existing = JSON.parse(existingRaw) as StoredAuthSession;
        // preserve clientTimeoutMs when not provided in new session
        if (existing?.clientTimeoutMs && !session.clientTimeoutMs) {
          session.clientTimeoutMs = existing.clientTimeoutMs;
        }
      } catch {}
    }
  } catch {}

  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
};

export const clearStoredAuthSession = () => {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(AUTH_STORAGE_KEY);
};

export const getStoredToken = () => getStoredAuthSession()?.token ?? null;

const isAuthEndpoint = (url?: string) => {
  if (!url) {
    return false;
  }

  return [
    "/api/auth/login",
    "/api/auth/register/patient",
    "/api/auth/refresh",
    "/api/auth/logout",
    "/api/auth/forgot-password/send-otp",
    "/api/auth/forgot-password/verify-otp",
    "/api/auth/forgot-password/reset",
  ].some((endpoint) => url.includes(endpoint));
};

const normalizeRole = (role: unknown) => String(role ?? "").toUpperCase();

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: (() => {
    try {
      if (typeof window !== "undefined") {
        const stored = window.localStorage.getItem(CLIENT_TIMEOUT_STORAGE_KEY);
        if (stored) {
          const n = Number(stored);
          if (!Number.isNaN(n) && n > 0) return n;
        }
      }
    } catch {}

    return DEFAULT_TIMEOUT_MS;
  })(),
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getStoredToken();
  if (token) {
    config.headers = config.headers ?? {};
    (config.headers as Record<string, string>).Authorization = `Bearer ${token}`;
  }

  return config;
});

type QueuedRequest = {
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
};

let isRefreshingToken = false;
let queuedRequests: QueuedRequest[] = [];

const resolveQueuedRequests = (token: string) => {
  queuedRequests.forEach(({ resolve }) => resolve(token));
  queuedRequests = [];
};

const rejectQueuedRequests = (error: unknown) => {
  queuedRequests.forEach(({ reject }) => reject(error));
  queuedRequests = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
    const status = error.response?.status;

    if (!originalRequest || status !== 401 || originalRequest._retry || isAuthEndpoint(originalRequest.url)) {
      return Promise.reject(error);
    }

    const session = getStoredAuthSession();
    if (!session?.refreshToken) {
      clearStoredAuthSession();
      return Promise.reject(error);
    }

    if (isRefreshingToken) {
      return new Promise((resolve, reject) => {
        queuedRequests.push({
          resolve: (newAccessToken: string) => {
            originalRequest.headers = originalRequest.headers ?? {};
            (originalRequest.headers as Record<string, string>).Authorization = `Bearer ${newAccessToken}`;
            resolve(api(originalRequest));
          },
          reject,
        });
      });
    }

    originalRequest._retry = true;
    isRefreshingToken = true;

    try {
      const refreshResponse = await axios.post<StoredAuthSession>(
        `${API_BASE_URL}/api/auth/refresh`,
        { refreshToken: session.refreshToken },
        {
          headers: {
            "Content-Type": "application/json",
          },
        },
      );

      const refreshedSession: StoredAuthSession = {
        token: refreshResponse.data.token,
        refreshToken: refreshResponse.data.refreshToken,
        username: refreshResponse.data.username,
        role: normalizeRole(refreshResponse.data.role),
      };

      saveStoredAuthSession(refreshedSession);
      resolveQueuedRequests(refreshedSession.token);

      originalRequest.headers = originalRequest.headers ?? {};
      (originalRequest.headers as Record<string, string>).Authorization = `Bearer ${refreshedSession.token}`;
      return api(originalRequest);
    } catch (refreshError) {
      clearStoredAuthSession();
      rejectQueuedRequests(refreshError);
      return Promise.reject(refreshError);
    } finally {
      isRefreshingToken = false;
    }
  },
);

export const getApiErrorMessage = (error: unknown, fallback = "Không thể kết nối tới máy chủ") => {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const messageText = String(error.message ?? "").toLowerCase();

    if (!error.response) {
      if (messageText.includes("timeout") || error.code === "ECONNABORTED") {
        return "Yêu cầu quá thời gian chờ. Vui lòng kiểm tra kết nối mạng hoặc thử lại.";
      }

      return "Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng hoặc thử lại sau.";
    }

    if (status === 401) {
      return "Tên đăng nhập hoặc mật khẩu không đúng.";
    }

    if (status === 403) {
      return "Tài khoản không có quyền truy cập.";
    }

    if (status === 429) {
      return "Bạn đã thao tác quá nhanh. Vui lòng thử lại sau.";
    }

    if (messageText.includes("timeout") || error.code === "ECONNABORTED") {
      return "Yêu cầu quá thời gian chờ. Vui lòng kiểm tra kết nối mạng hoặc thử lại.";
    }

    const responseData = error.response?.data as
      | { message?: string; error?: string; details?: string | Record<string, string> }
      | string
      | undefined;

    if (typeof responseData === "string") {
      return responseData;
    }

    if (responseData?.details) {
      if (typeof responseData.details === "string") {
        return responseData.details;
      }

      const firstDetail = Object.values(responseData.details)[0];
      if (firstDetail) {
        return firstDetail;
      }
    }

    if (responseData?.message) {
      return responseData.message;
    }

    if (responseData?.error) {
      return responseData.error;
    }

    return error.message || fallback;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
};

export const setApiTimeout = (ms: number) => {
  if (typeof ms !== "number" || Number.isNaN(ms) || ms <= 0) return;
  api.defaults.timeout = ms;
  try {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(CLIENT_TIMEOUT_STORAGE_KEY, String(ms));
    }
  } catch {}
};

export const getApiTimeout = () => Number(api.defaults.timeout ?? DEFAULT_TIMEOUT_MS);

export const getStoredUserClientTimeout = (): number | null => {
  try {
    const s = getStoredAuthSession();
    if (!s?.clientTimeoutMs) return null;
    const n = Number(s.clientTimeoutMs);
    return Number.isNaN(n) ? null : n;
  } catch {
    return null;
  }
};

export const setStoredUserClientTimeout = (ms: number) => {
  if (typeof ms !== "number" || Number.isNaN(ms) || ms <= 0) return;
  try {
    const stored = getStoredAuthSession();
    if (!stored) return;
    const next = { ...stored, clientTimeoutMs: ms } as StoredAuthSession;
    saveStoredAuthSession(next);
    setApiTimeout(ms);
  } catch {}
};
