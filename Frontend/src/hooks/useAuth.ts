"use client";

import { useCallback, useEffect, useState } from "react";

import { authService, isTokenExpired } from "@/services/authService";
import type {
  AuthSession,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterPatientRequest,
  ResetPasswordWithOtpRequest,
  VerifyForgotPasswordOtpRequest,
} from "@/types/auth";

export function useAuth() {
  const [session, setSession] = useState<AuthSession | null>(null);

  const syncSession = useCallback(async () => {
    const storedSession = await authService.initializeSession();
    setSession(storedSession);
    return storedSession;
  }, []);

  useEffect(() => {
    void syncSession();
  }, [syncSession]);

  const login = useCallback(async (payload: LoginRequest) => {
    const nextSession = await authService.login(payload);
    setSession(nextSession);
    return nextSession;
  }, []);

  const registerPatient = useCallback(async (payload: RegisterPatientRequest) => {
    const nextSession = await authService.registerPatient(payload);
    setSession(nextSession);
    return nextSession;
  }, []);

  const sendForgotPasswordOtp = useCallback(async (payload: ForgotPasswordRequest) => {
    return authService.sendForgotPasswordOtp(payload);
  }, []);

  const verifyForgotPasswordOtp = useCallback(async (payload: VerifyForgotPasswordOtpRequest) => {
    return authService.verifyForgotPasswordOtp(payload);
  }, []);

  const resetPasswordWithOtp = useCallback(async (payload: ResetPasswordWithOtpRequest) => {
    return authService.resetPasswordWithOtp(payload);
  }, []);

  const logout = useCallback(async () => {
    await authService.logout();
    setSession(null);
  }, []);

  const token = session?.token ?? null;
  const role = session?.role ?? null;
  const username = session?.username ?? null;
  const isAuthenticated = Boolean(token && !isTokenExpired(token));

  return {
    session,
    token,
    role,
    username,
    isAuthenticated,
    login,
    registerPatient,
    sendForgotPasswordOtp,
    verifyForgotPasswordOtp,
    resetPasswordWithOtp,
    logout,
    refreshSession: syncSession,
  };
}
