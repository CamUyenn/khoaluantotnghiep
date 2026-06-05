"use client";

import { useState } from "react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/hooks/useAuth";
import { getApiErrorMessage } from "@/services/api";

import { Activity, CheckCircle, KeyRound, Lock, Mail } from "lucide-react";
import { toast } from "sonner";
import styles from "./forgotpassword.module.css";

export function ForgotPassword() {
  const { sendForgotPasswordOtp, verifyForgotPasswordOtp, resetPasswordWithOtp } = useAuth();

  const [step, setStep] = useState(1);
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isResendingOtp, setIsResendingOtp] = useState(false);

  const handleSendEmail = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!email.trim()) {
      toast.error("Vui lòng nhập địa chỉ email");
      return;
    }

    setIsSubmitting(true);
    try {
      await sendForgotPasswordOtp({ email: email.trim().toLowerCase() });
      toast.success("Mã OTP đã được gửi đến email của bạn");
      setStep(2);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể gửi OTP"));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleVerifyOTP = async (e: React.FormEvent) => {
    e.preventDefault();

    if (otp.length < 6) {
      toast.error("Mã OTP phải bao gồm 6 chữ số");
      return;
    }

    setIsSubmitting(true);
    try {
      await verifyForgotPasswordOtp({
        email: email.trim().toLowerCase(),
        otp: otp.trim(),
      });
      toast.success("Xác thực OTP thành công");
      setStep(3);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "OTP không hợp lệ hoặc đã hết hạn"));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();

    if (password.length < 6) {
      toast.error("Mật khẩu phải có ít nhất 6 ký tự");
      return;
    }

    if (password !== confirmPassword) {
      toast.error("Mật khẩu xác nhận không khớp");
      return;
    }

    setIsSubmitting(true);
    try {
      await resetPasswordWithOtp({
        email: email.trim().toLowerCase(),
        newPassword: password,
      });
      toast.success("Cài đặt mật khẩu mới thành công");
      setStep(4);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể cập nhật mật khẩu"));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResendOtp = async () => {
    if (!email.trim()) {
      toast.error("Vui lòng nhập địa chỉ email");
      return;
    }

    setIsResendingOtp(true);
    try {
      await sendForgotPasswordOtp({ email: email.trim().toLowerCase() });
      toast.success("Đã gửi lại mã OTP");
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể gửi lại OTP"));
    } finally {
      setIsResendingOtp(false);
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.wrapper}>
        {step !== 4 && (
          <div className={styles.brandBlock}>
            <div className={styles.brandRow}>
              <div className={styles.brandIcon}>
                <Activity size={26} />
              </div>
              <h1 className={styles.title}>Phòng Khám Đa Khoa</h1>
            </div>
            <p className={styles.subtitle}>Khôi phục tài khoản</p>
          </div>
        )}

        {/* Form nhập email */}
        {step === 1 && (
          <Card className={styles.card}>
            <div className={styles.formHeader}>
              <h2 className={styles.formTitle}>Quên mật khẩu?</h2>
              <p className={styles.formDesc}>
                Nhập địa chỉ email đăng ký tài khoản của bạn để nhận mã xác thực (OTP).
              </p>
            </div>
            <form onSubmit={handleSendEmail} className={styles.form}>
              <div>
                <Label htmlFor="email">Email</Label>
                <div className={styles.inputWrap}>
                  <Mail className={styles.inputIcon} />
                  <Input
                    id="email"
                    type="email"
                    placeholder="email@example.com"
                    className={styles.inputPad}
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </div>
              </div>
              <Button type="submit" className={styles.submitBtn} disabled={isSubmitting}>
                {isSubmitting ? "Đang gửi..." : "Gửi mã xác thực"}
              </Button>
              <div className={styles.backLinkWrap}>
                <Link href="/signin" className={styles.link}>← Quay lại đăng nhập</Link>
              </div>
            </form>
          </Card>
        )}

        {/* Form nhập OTP */}
        {step === 2 && (
          <Card className={styles.card}>
            <div className={styles.formHeader}>
              <h2 className={styles.formTitle}>Nhập mã xác thực</h2>
              <p className={styles.formDesc}>
                Mã OTP 6 số đã được gửi tới <strong>{email}</strong>.
              </p>
            </div>
            <form onSubmit={handleVerifyOTP} className={styles.form}>
              <div>
                <Label htmlFor="otp">Mã OTP</Label>
                <div className={styles.inputWrap}>
                  <KeyRound className={styles.inputIcon} />
                  <Input
                    id="otp"
                    type="text"
                    maxLength={6}
                    placeholder="123456"
                    className={styles.inputPad}
                    value={otp}
                    onChange={(e) => setOtp(e.target.value)}
                    required
                  />
                </div>
              </div>
              <Button type="submit" className={styles.submitBtn} disabled={isSubmitting}>
                {isSubmitting ? "Đang xác nhận..." : "Xác nhận"}
              </Button>
              <p className={styles.otpText}>
                Chưa nhận được mã? <button type="button" className={styles.resendLink} onClick={handleResendOtp} disabled={isResendingOtp || isSubmitting}>{isResendingOtp ? "Đang gửi..." : "Gửi lại"}</button>
              </p>
              <div className={styles.backLinkWrap}>
                <button type="button" onClick={() => setStep(1)} className={styles.link} style={{ background: 'none', border: 'none', padding: 0 }}>
                  ← Đổi email khác
                </button>
              </div>
            </form>
          </Card>
        )}
        {/* Form đặt mật khẩu mới */}
        {step === 3 && (
          <Card className={styles.card}>
            <div className={styles.formHeader}>
              <h2 className={styles.formTitle}>Tạo mật khẩu mới</h2>
              <p className={styles.formDesc}>
                Vui lòng đặt mật khẩu mới cho tài khoản của bạn. Đảm bảo mật khẩu đủ mạnh.
              </p>
            </div>
            <form onSubmit={handleResetPassword} className={styles.form}>
              <div>
                <Label htmlFor="password">Mật khẩu mới</Label>
                <div className={styles.inputWrap}>
                  <Lock className={styles.inputIcon} />
                  <Input
                    id="password"
                    type="password"
                    placeholder="••••••••"
                    className={styles.inputPad}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                </div>
              </div>
              <div>
                <Label htmlFor="confirmPassword">Xác nhận mật khẩu</Label>
                <div className={styles.inputWrap}>
                  <Lock className={styles.inputIcon} />
                  <Input
                    id="confirmPassword"
                    type="password"
                    placeholder="••••••••"
                    className={styles.inputPad}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                  />
                </div>
              </div>
              <Button type="submit" className={styles.submitBtn} disabled={isSubmitting}>
                {isSubmitting ? "Đang cập nhật..." : "Cập nhật mật khẩu"}
              </Button>
            </form>
          </Card>
        )}
        {/* Thành công*/}
        {step === 4 && (
          <Card className={`${styles.card} ${styles.successCard}`}>
            <div className={styles.successIconWrap}>
              <CheckCircle className={styles.successIcon} />
            </div>
            <h2 className={styles.successTitle}>Khôi phục thành công!</h2>
            <p className={styles.successDesc}>
              Mật khẩu của bạn đã được thay đổi thành công. Bạn có thể sử dụng mật khẩu mới để đăng nhập vào hệ thống phòng khám.
            </p>
            <div className={styles.successActions}>
              <Link href="/signin" className={styles.linkBlock}>
                <Button className={styles.btnFull}>Đăng nhập ngay</Button>
              </Link>
            </div>
          </Card>
        )}

        {/* Nút quay lại trang chủ luôn hiển thị ở dưới cùng (trừ khi ở bước 4) */}
        {step !== 4 && (
          <div className={styles.backHome}>
            <Link href="/" className={styles.homeLink}>
              ← Quay lại trang chủ
            </Link>
          </div>
        )}

      </div>
    </div>
  );
}