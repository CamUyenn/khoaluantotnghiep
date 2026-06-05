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

import { Lock, Mail, Phone, User } from "lucide-react";

import styles from "../sign-in/sign-in-form.module.css";

type Notice = {
  type: "success" | "error";
  message: string;
};

export function Register() {
  const router = useRouter();
  const { registerPatient } = useAuth();
  const usernamePattern = /^[a-zA-Z0-9._-]{4,50}$/;
  const [formData, setFormData] = useState({
    username: "",
    fullName: "",
    gender: "",
    phoneNumber: "",
    gmail: "",
    password: "",
    confirmPassword: "",
  });
  const [notice, setNotice] = useState<Notice | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleRegister = async (e: FormEvent) => {
    e.preventDefault();

    if (!formData.username.trim() || !formData.fullName.trim() || !formData.gender || !formData.phoneNumber.trim() || !formData.gmail.trim() || !formData.password) {
      setNotice({ type: "error", message: "Vui lòng điền đầy đủ thông tin." });
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setNotice({ type: "error", message: "Mật khẩu xác nhận không khớp." });
      return;
    }

    if (formData.password.length < 6) {
      setNotice({ type: "error", message: "Mật khẩu phải có ít nhất 6 ký tự." });
      return;
    }

    if (!usernamePattern.test(formData.username.trim())) {
      setNotice({
        type: "error",
        message: "Tên đăng nhập phải từ 4-50 ký tự và không chứa khoảng trắng.",
      });
      return;
    }

    setIsSubmitting(true);
    try {
      await registerPatient({
        username: formData.username.trim(),
        password: formData.password,
        fullName: formData.fullName.trim(),
        gender: formData.gender,
        phoneNumber: formData.phoneNumber.trim(),
        gmail: formData.gmail.trim().toLowerCase(),
      });

      setNotice({ type: "success", message: "Đăng ký tài khoản thành công!" });
      window.setTimeout(() => {
        router.push("/signin");
      }, 900);
    } catch (error) {
      setNotice({
        type: "error",
        message: getApiErrorMessage(error, "Đăng ký thất bại. Kiểm tra lại dữ liệu nhập vào."),
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const updateField = (field: keyof typeof formData, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
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
          <p className={styles.subtitle}>Đăng ký tài khoản bệnh nhân.</p>
        </div>

        <Card className={styles.card}>
          <form onSubmit={handleRegister} className={styles.form}>
            <div className={styles.field}>
              <Label htmlFor="username">
                Tên đăng nhập <span className="text-red-500">*</span>
              </Label>
              <div className={styles.inputWrap}>
                <User className={styles.inputIcon} size={18} />
                <Input
                  id="username"
                  type="text"
                  placeholder="nguyenvana"
                  className={styles.inputPad}
                  value={formData.username}
                  onChange={(e) => updateField("username", e.target.value)}
                />
              </div>
            </div>

            <div className={styles.field}>
              <Label htmlFor="fullName">
                Họ và tên <span className="text-red-500">*</span>
              </Label>
              <div className={styles.inputWrap}>
                <User className={styles.inputIcon} size={18} />
                <Input
                  id="fullName"
                  type="text"
                  placeholder="Nguyễn Văn A"
                  className={styles.inputPad}
                  value={formData.fullName}
                  onChange={(e) => updateField("fullName", e.target.value)}
                />
              </div>
            </div>

            <div className={styles.field}>
              <Label htmlFor="gender">
                Giới tính <span className="text-red-500">*</span>
              </Label>
              <select
                id="gender"
                className={styles.selectInput}
                value={formData.gender}
                onChange={(e) => updateField("gender", e.target.value)}
              >
                <option value="">-- Chọn giới tính --</option>
                <option value="male">Nam</option>
                <option value="female">Nữ</option>
                <option value="other">Khác</option>
              </select>
            </div>

            <div className={styles.field}>
              <Label htmlFor="phoneNumber">
                Số điện thoại <span className="text-red-500">*</span>
              </Label>
              <div className={styles.inputWrap}>
                <Phone className={styles.inputIcon} size={18} />
                <Input
                  id="phoneNumber"
                  type="tel"
                  placeholder="0123456789"
                  className={styles.inputPad}
                  value={formData.phoneNumber}
                  onChange={(e) => updateField("phoneNumber", e.target.value)}
                />
              </div>
            </div>

            <div className={styles.field}>
              <Label htmlFor="gmail">
                Email <span className="text-red-500">*</span>
              </Label>
              <div className={styles.inputWrap}>
                <Mail className={styles.inputIcon} size={18} />
                <Input
                  id="gmail"
                  type="email"
                  placeholder="email@example.com"
                  className={styles.inputPad}
                  value={formData.gmail}
                  onChange={(e) => updateField("gmail", e.target.value)}
                />
              </div>
            </div>

            <div className={styles.field}>
              <Label htmlFor="password">
                Mật khẩu <span className="text-red-500">*</span>
              </Label>
              <div className={styles.inputWrap}>
                <Lock className={styles.inputIcon} size={18} />
                <Input
                  id="password"
                  type="password"
                  placeholder="••••••••"
                  className={styles.inputPad}
                  value={formData.password}
                  onChange={(e) => updateField("password", e.target.value)}
                />
              </div>
            </div>

            <div className={styles.field}>
              <Label htmlFor="confirmPassword">
                Xác nhận mật khẩu <span className="text-red-500">*</span>
              </Label>
              <div className={styles.inputWrap}>
                <Lock className={styles.inputIcon} size={18} />
                <Input
                  id="confirmPassword"
                  type="password"
                  placeholder="••••••••"
                  className={styles.inputPad}
                  value={formData.confirmPassword}
                  onChange={(e) => updateField("confirmPassword", e.target.value)}
                />
              </div>
            </div>

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

            <Button type="submit" className={styles.submitButton} disabled={isSubmitting}>
              {isSubmitting ? "Đang đăng ký..." : "Đăng ký"}
            </Button>

            <div className={styles.centerText}>
              <span>Đã có tài khoản? </span>
              <Link href="/signin" className={styles.link}>
                Đăng nhập ngay
              </Link>
            </div>
          </form>
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