"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/hooks/useAuth";
import { getApiErrorMessage } from "@/services/api";

import styles from "./sign-in-form.module.css";

type Notice = {
  type: "success" | "error";
  message: string;
};

const ROLE_ROUTES: Record<string, string> = {
  ADMIN: "/admin",
  DOCTOR: "/doctor",
  RECEPTIONIST: "/receptionist",
  CASHIER: "/cashier",
  PATIENT: "/dashboard",
};

export function Login() {
  const router = useRouter();
  const { login } = useAuth();
  const [loginData, setLoginData] = useState({
    username: "",
    password: "",
  });
  const [notice, setNotice] = useState<Notice | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();

    if (!loginData.username.trim() || !loginData.password) {
      setNotice({ type: "error", message: "Vui lòng điền đầy đủ thông tin." });
      return;
    }

    setIsSubmitting(true);
    try {
      const auth = await login({
        username: loginData.username.trim(),
        password: loginData.password,
      });

      const nextRole = String(auth.role).toUpperCase();
      const nextPath = ROLE_ROUTES[nextRole] ?? "/";

      setNotice({ type: "success", message: "Đăng nhập thành công." });

      window.setTimeout(() => {
        router.push(nextPath);
      }, 400);
    } catch (error) {
      setNotice({
        type: "error",
        message: getApiErrorMessage(error, "Đăng nhập thất bại. Kiểm tra lại username và mật khẩu."),
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const updateField = (field: "username" | "password", value: string) => {
    setLoginData((prev) => ({ ...prev, [field]: value }));
  };

  return (
    <div className={styles.page}>
      <div className={styles.wrapper}>
        <div className={styles.brandBlock}>
          <div className={styles.brandRow}>
            <div className={styles.brandIcon} aria-hidden="true">
              +
            </div>
            <h1 className={styles.title}>Phòng Khám Đa Khoa</h1>
          </div>
          <p className={styles.subtitle}>Đăng nhập vào hệ thống quản lý.</p>
        </div>

        <Card className={styles.card}>
          <form onSubmit={handleLogin} className={styles.form}>
            <div className={styles.field}>
              <Label htmlFor="username">Tên đăng nhập</Label>
              <div className={styles.inputWrap}>
                <span className={styles.inputIcon}>@</span>
                <Input
                  id="username"
                  type="text"
                  placeholder="Nhập username"
                  className={styles.inputPad}
                  value={loginData.username}
                  onChange={(e) => updateField("username", e.target.value)}
                />
              </div>
            </div>

            <div className={styles.field}>
              <Label htmlFor="password">Mật khẩu</Label>
              <div className={styles.inputWrap}>
                <span className={styles.inputIcon}>*</span>
                <Input
                  id="password"
                  type="password"
                  placeholder="••••••••"
                  className={styles.inputPad}
                  value={loginData.password}
                  onChange={(e) => updateField("password", e.target.value)}
                />
              </div>
            </div>

            <div className={styles.inlineRow}>
              <div className={styles.centerText} style={{ marginTop: '8px' }}>
                Bạn chưa có tài khoản?{" "}
                <Link href="/signup" className={styles.link}>
                  Đăng ký ngay
                </Link>
            </div>
              <Link href="/forgotpassword" className={styles.link}>
                Quên mật khẩu?
              </Link>
            </div>

            <Button type="submit" className={styles.submitButton} disabled={isSubmitting}>
              {isSubmitting ? "Đang đăng nhập..." : "Đăng nhập"}
            </Button>
          </form>

          {notice && (
            <p
              className={
                notice.type === "error"
                  ? `${styles.notice} ${styles.noticeError}`
                  : `${styles.notice} ${styles.noticeSuccess}`
              }
            >
              {notice.message}
            </p>
          )}
        </Card>

        <div className={styles.backHome}>
          <Link href="/" className={styles.homeLink}>
            ← Quay lại trang chủ
          </Link>
        </div>
      </div>
    </div>
  );
}