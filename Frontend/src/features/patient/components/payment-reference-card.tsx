"use client";

import { useMemo } from "react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { toast } from "sonner";
import { Copy, QrCode } from "lucide-react";
import styles from "./payment-reference-card.module.css";

interface PaymentReferenceCardProps {
  paymentReference: string | null | undefined;
  amount: number;
  title?: string;
  subtitle?: string;
  paidAt?: string | null;
  hideQr?: boolean;
}

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(amount);

export function PaymentReferenceCard({
  paymentReference,
  amount,
  title = "Mã chuyển khoản / QR thanh toán",
  subtitle = "Dùng nội dung này để SePay tự động đối soát với hóa đơn.",
  paidAt,
  hideQr = false,
}: PaymentReferenceCardProps) {
  const bankBin = process.env.NEXT_PUBLIC_CLINIC_BANK_BIN?.trim();
  const bankAccount = process.env.NEXT_PUBLIC_CLINIC_BANK_ACCOUNT?.trim();
  const accountName = process.env.NEXT_PUBLIC_CLINIC_ACCOUNT_NAME?.trim();
  const bankName = process.env.NEXT_PUBLIC_CLINIC_BANK_NAME?.trim() || "Phòng khám";

  const qrReady = Boolean(paymentReference && bankBin && bankAccount && accountName);
  const qrUrl = useMemo(() => {
    if (!qrReady) {
      return "";
    }

    const addInfo = encodeURIComponent(paymentReference ?? "");
    const accountNameEncoded = encodeURIComponent(accountName ?? "");
    return `https://img.vietqr.io/image/${bankBin}-${bankAccount}-compact2.png?amount=${Math.max(0, Math.round(amount))}&addInfo=${addInfo}&accountName=${accountNameEncoded}`;
  }, [accountName, amount, bankAccount, bankBin, paymentReference, qrReady]);

  const handleCopy = async () => {
    if (!paymentReference) {
      return;
    }

    try {
      await navigator.clipboard.writeText(paymentReference);
      toast.success("Đã sao chép mã chuyển khoản");
    } catch {
      toast.error("Không thể sao chép mã chuyển khoản");
    }
  };

  if (!paymentReference) {
    return null;
  }

  return (
    <Card className={styles.card}>
      <div className={styles.header}>
        <div>
          <p className={styles.title}>{title}</p>
          <p className={styles.hint}>{subtitle}</p>
        </div>
        {paidAt ? <Badge variant="secondary">{paidAt}</Badge> : null}
      </div>

      <div className={styles.referenceBox}>
        <span className={styles.referenceCode}>{paymentReference}</span>
        <Button type="button" variant="outline" size="sm" className={styles.copyButton} onClick={handleCopy}>
          <Copy size={14} />
          Sao chép
        </Button>
      </div>

      <div className={styles.statusRow}>
        <span className={styles.amountBadge}>Số tiền: {formatCurrency(amount)}</span>
        <span className={styles.meta}>{bankName}</span>
      </div>

      {!hideQr ? (
        <div className={styles.qrBox}>
          <div className={styles.qrTitle}>
            <QrCode size={15} style={{ display: "inline", marginRight: 6 }} />
            QR thanh toán
          </div>

          {qrReady ? (
            <>
              <img src={qrUrl} alt="QR thanh toán SePay" className={styles.qrImage} />
              <div className={styles.meta}>
                {bankAccount} {accountName ? `• ${accountName}` : ""}
              </div>
            </>
          ) : (
            <p className={styles.error}>
              Thiếu cấu hình QR trong `.env`. Cần đủ: `NEXT_PUBLIC_CLINIC_BANK_BIN`, `NEXT_PUBLIC_CLINIC_BANK_ACCOUNT`, `NEXT_PUBLIC_CLINIC_ACCOUNT_NAME`.
            </p>
          )}
        </div>
      ) : null}
    </Card>
  );
}

export default PaymentReferenceCard;