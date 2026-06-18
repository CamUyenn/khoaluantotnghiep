import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DollarSign } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { toast } from "sonner";
import { Prescription } from "@/types/pharmacy.type";
import { clinicConfigService } from "@/services/clinicConfigService";
import { cashierService } from "@/services/cashierService";
import PaymentRealtimeListener from "@/components/payment-realtime-listener";
import { getServicePaymentBreakdown } from "../utils/service-payment-breakdown";
import styles from "../cashier.module.css";

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(
    amount,
  );

interface PaymentDialogProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  prescription: Prescription | null;
  paymentData: { serviceFee: string; insuranceDiscount: string };
  onPaymentDataChange: (field: string, value: string) => void;
  paymentMethod: "TIEN_MAT" | "CHUYEN_KHOAN" | "POS";
  onPaymentMethodChange: (value: "TIEN_MAT" | "CHUYEN_KHOAN" | "POS") => void;
  transferConfirmed: boolean;
  onTransferConfirmedChange: (value: boolean) => void;
  onConfirm: (autoTriggered?: boolean) => void | Promise<void>;
  isProcessing?: boolean;
}

function PaymentDialog({
  isOpen,
  onOpenChange,
  prescription,
  paymentData,
  onPaymentDataChange,
  paymentMethod,
  onPaymentMethodChange,
  transferConfirmed,
  onTransferConfirmedChange,
  onConfirm,
  isProcessing = false,
}: PaymentDialogProps) {
  const PAYMENT_CHECK_INTERVAL_MS = 10_000;
  const [bankConfig, setBankConfig] = useState<{
    bankBin: string;
    bankAccount: string;
    accountName: string;
    bankName: string;
  } | null>(null);
  const [bankConfigLoading, setBankConfigLoading] = useState(true);
  const transferPollingStartedRef = useRef(false);

  useEffect(() => {
    let mounted = true;

    void clinicConfigService.getBankConfig()
      .then((config) => {
        if (mounted) {
          setBankConfig(config);
        }
      })
      .catch(() => {
        if (mounted) {
          setBankConfig(null);
        }
      })
      .finally(() => {
        if (mounted) {
          setBankConfigLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (!prescription) {
      return;
    }

    if (!isOpen || paymentMethod !== "CHUYEN_KHOAN") {
      transferPollingStartedRef.current = false;
      return;
    }

    const invoiceIdRef = prescription.invoiceId ?? Number(prescription.id);
    const paymentReference = invoiceIdRef
      ? `BK-INV-${invoiceIdRef}`
      : `BK-INV-${Date.now()}`;

    if (!paymentReference || transferPollingStartedRef.current) {
      return;
    }

    transferPollingStartedRef.current = true;
    let mounted = true;
    let timer: ReturnType<typeof setTimeout> | null = null;

    const pollStatus = async () => {
      try {
        const status = await cashierService.getPaymentReferenceStatus(paymentReference);
        if (!mounted) return;

        const normalizedTransactionStatus = (status.transactionStatus || "").toUpperCase();
        const normalizedPaymentStatus = (status.paymentStatus || "").toUpperCase();

        if (normalizedTransactionStatus === "SUCCESS" || normalizedPaymentStatus === "FULLY_PAID") {
          onTransferConfirmedChange(true);
          toast.success("Đã nhận thanh toán chuyển khoản.");
          void onConfirm(true);
          return;
        }

        timer = setTimeout(() => {
          void pollStatus();
        }, PAYMENT_CHECK_INTERVAL_MS);
      } catch {
        if (!mounted) return;
        timer = setTimeout(() => {
          void pollStatus();
        }, PAYMENT_CHECK_INTERVAL_MS);
      }
    };

    void pollStatus();

    return () => {
      mounted = false;
      transferPollingStartedRef.current = false;
      if (timer) {
        clearTimeout(timer);
      }
    };
  }, [isOpen, onConfirm, onTransferConfirmedChange, paymentMethod, prescription]);

  if (!prescription) return null;

  const serviceItems = prescription.serviceItems ?? [];
  const totalServiceFee = Number((prescription.grandTotal ?? paymentData.serviceFee) || 0);
  const breakdown = getServicePaymentBreakdown(prescription, Number(prescription.advanceAmount ?? 0));
  const { consultationServices, consultationFee, unpaidAdditionalServices, unpaidAdditionalServiceFee, totalDueAfterAdvance, consultationCoveredAmount } = breakdown;
  const initialConsultationFee = Number(
    prescription.advanceAmount ?? consultationCoveredAmount ?? consultationFee ?? 0,
  );
  const prepaidConsultationItems = consultationServices.filter((item) => Number(item.coveredLineTotal || 0) > 0);
  const insuranceDiscount = parseFloat(paymentData.insuranceDiscount || "0");
  const remainingAmount = prescription.remainingAmount ?? null;
  const computedRemaining = Math.max(0, totalDueAfterAdvance);
  const effectiveRemaining = remainingAmount !== null && remainingAmount > 0
    ? remainingAmount
    : computedRemaining;
  const payableAmount = Math.max(0, effectiveRemaining - insuranceDiscount);

  const bankBin = bankConfig?.bankBin?.trim();
  const bankAccount = bankConfig?.bankAccount?.trim();
  const accountNameRaw = bankConfig?.accountName?.trim();
  const bankName = bankConfig?.bankName?.trim() || "TRAN CAM UYEN";
  const qrReady = Boolean(bankBin && bankAccount && accountNameRaw);
  const accountName = encodeURIComponent(accountNameRaw || "");
  const invoiceIdRef = prescription.invoiceId ?? Number(prescription.id);
  const paymentReference = invoiceIdRef
    ? `BK-INV-${invoiceIdRef}`
    : `BK-INV-${Date.now()}`;
  const transferNote = encodeURIComponent(`REF:${paymentReference} THANH TOAN HD ${prescription.invoiceId || prescription.id}`);
  const paymentStageLabel =
    (prescription.status === "pending" && Number(prescription.advanceAmount ?? 0) > 0)
      ? "Thanh toán bổ sung để chốt hồ sơ"
      : "Thanh toán ban đầu để vào phòng bác sĩ";
  const qrUrl = qrReady
    ? `https://img.vietqr.io/image/${bankBin}-${bankAccount}-compact2.png?amount=${Math.round(payableAmount)}&addInfo=${transferNote}&accountName=${accountName}`
    : "";
  const normalizedPaymentCode = paymentReference.trim().replace(/\s+/g, "_");

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent className={styles.dialogContent}>
        <DialogHeader>
          <DialogTitle>Thanh toán viện phí - {prescription.patientName}</DialogTitle>
        </DialogHeader>

        <div className={styles.dialogBody}>
          <p className={styles.textSmall}>{paymentStageLabel}</p>

          <div className={styles.patientInfoBox}>
            <div className={styles.patientInfoGrid}>
              <div><span className={styles.infoLabel}>Bệnh nhân:</span><span className={styles.infoValue}>{prescription.patientName}</span></div>
              <div><span className={styles.infoLabel}>SĐT:</span><span className={styles.infoValue}>{prescription.phone}</span></div>
              <div><span className={styles.infoLabel}>Bác sĩ:</span><span className={styles.infoValue}>{prescription.doctor}</span></div>
              {prescription.insuranceNumber && (
                <div><span className={styles.infoLabel}>BHYT:</span><span className={styles.infoValue}>{prescription.insuranceNumber}</span></div>
              )}
            </div>
          </div>

          <div className={styles.formStack}>
            <div>
              <Label>Tổng tiền dịch vụ</Label>
              <Input
                type="text"
                value={formatCurrency(totalServiceFee)}
                onChange={(e) => onPaymentDataChange("serviceFee", e.target.value)}
                disabled
              />
            </div>
            <div>
              <Label>Phương thức thanh toán</Label>
              <select
                className={styles.selectInput}
                value={paymentMethod}
                onChange={(e) => onPaymentMethodChange(e.target.value as "TIEN_MAT" | "CHUYEN_KHOAN" | "POS")}
                disabled={isProcessing}
              >
                <option value="TIEN_MAT">Tiền mặt</option>
                <option value="CHUYEN_KHOAN">Chuyển khoản</option>
                <option value="POS">POS</option>
              </select>
            </div>

            {paymentMethod === "CHUYEN_KHOAN" && (
              <div className={styles.qrBox}>
                <p className={styles.qrTitle}>Quét mã QR để thanh toán</p>
                {qrReady ? (
                  <>
                    <img src={qrUrl} alt="QR chuyển khoản phòng khám" className={styles.qrImage} />
                    <p className={styles.qrAmount}>Số tiền: {Math.round(payableAmount).toLocaleString("vi-VN")}đ</p>
                    <p className={styles.textSmall}>{bankName}</p>
                    <p className={styles.textSmall}>{bankAccount}</p>
                    <PaymentRealtimeListener
                      paymentCode={normalizedPaymentCode}
                      enabled={Boolean(normalizedPaymentCode)}
                      successMessage="Thanh toán đã được xác nhận tự động cho lễ tân."
                      waitingMessage="Hệ thống đang chờ ngân hàng xác nhận..."
                      onPaymentSuccess={() => {
                        onTransferConfirmedChange(true);
                        toast.success("Đã nhận thanh toán chuyển khoản.");
                        void onConfirm(true);
                      }}
                    />
                  </>
                ) : bankConfigLoading ? (
                  <p className={styles.textDanger}>Đang tải cấu hình ngân hàng từ hệ thống...</p>
                ) : (
                  <p className={styles.textDanger}>
                    Không lấy được cấu hình QR từ hệ thống. Vui lòng thử lại sau.
                  </p>
                )}
              </div>
            )}

            {paymentMethod === "POS" && (
              <div className={styles.checkboxRow}>
                <input
                  id="transfer-confirmed"
                  type="checkbox"
                  checked={transferConfirmed}
                  onChange={(e) => onTransferConfirmedChange(e.target.checked)}
                />
                <Label htmlFor="transfer-confirmed">
                  Đã quẹt thẻ POS thành công
                </Label>
              </div>
            )}

            <div>
              <Label>
                Giảm trừ BHYT
                {prescription.insuranceNumber && <span className={styles.discountHint}>(Tự động tính 70%)</span>}
              </Label>
              <Input
                type="number"
                value={paymentData.insuranceDiscount}
                onChange={(e) => onPaymentDataChange("insuranceDiscount", e.target.value)}
                disabled
              />
            </div>

            <div className={styles.summaryBox}>
              <div className={styles.summaryList}>
                <div className={styles.summaryRow}>
                  <span className={styles.summaryMuted}>Đã nộp trước:</span>
                  <span className={styles.amountValue}>{formatCurrency(prescription.advanceAmount ?? 0)}</span>
                </div>
                <div className={styles.summaryRow}>
                  <span className={styles.summaryMuted}>Còn nộp sau:</span>
                  <span className={styles.amountValue}>{formatCurrency(effectiveRemaining)}</span>
                </div>

                {breakdown.consultationServices.filter((s) => Number(s.unpaidLineTotal || 0) > 0).length > 0 && (
                  <div className={styles.remainingItems}>
                    {breakdown.consultationServices
                      .filter((s) => Number(s.unpaidLineTotal || 0) > 0)
                      .map((s) => (
                        <div key={`rem-cons-${s.serviceId}`} className={styles.remainingRow}>
                          <span className={styles.remainingLabel}>- {s.serviceName}</span>
                          <span className={styles.remainingValue}>{formatCurrency(Number(s.unpaidLineTotal || 0))}</span>
                        </div>
                      ))}
                  </div>
                )}

                {unpaidAdditionalServices.length > 0 && (
                  <div className={styles.dialogPrescriptionBox}>
                    <p className={styles.dialogPrescriptionTitle}>Dịch vụ phát sinh chưa thanh toán</p>
                    <div className={styles.dialogItems}>
                      {unpaidAdditionalServices.map((item) => (
                        <div key={`additional-${item.serviceId}`} className={styles.dialogItem}>
                          <div className={styles.dialogItemTop}>
                            <div>
                              <div className={styles.dialogItemName}>{item.serviceName} x{item.quantity}</div>
                              <div className={styles.dialogItemMeta}>Chưa nộp: {formatCurrency(item.unpaidLineTotal)}</div>
                            </div>
                            <div className={styles.dialogItemAmount}>{formatCurrency(item.unpaidLineTotal)}</div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {serviceItems.length > 0 && consultationFee > 0 ? (
                  <>
                    <div className={styles.summaryRow}>
                      <span className={styles.summaryMuted}>Phí khám ban đầu:</span>
                      <span className={styles.amountValue}>{formatCurrency(initialConsultationFee)}</span>
                    </div>
                    {prepaidConsultationItems.map((item) => (
                      <div key={`consult-${item.serviceId}`} className={styles.summaryRow}>
                        <span className={styles.summaryMuted}>- {item.serviceName}</span>
                        <span className={styles.amountValue}>{formatCurrency(Number(item.coveredLineTotal || 0))}</span>
                      </div>
                    ))}
                    {unpaidAdditionalServiceFee > 0 && (
                      <>
                        <div className={styles.summaryRow}>
                          <span className={styles.summaryMuted}>Dịch vụ chỉ định thêm chưa thanh toán:</span>
                          <span className={styles.amountValue}>{formatCurrency(unpaidAdditionalServiceFee)}</span>
                        </div>
                      </>
                    )}
                  </>
                ) : (
                  <div className={styles.summaryRow}><span className={styles.summaryMuted}>Phí khám:</span><span className={styles.amountValue}>{formatCurrency(totalServiceFee)}</span></div>
                )}
                {insuranceDiscount > 0 && (
                  <div className={`${styles.summaryRow} ${styles.summaryDiscount}`}>
                    <span>Giảm trừ BHYT (70%):</span><span className={styles.amountValue}>-{formatCurrency(insuranceDiscount)}</span>
                  </div>
                )}
                <div className={`${styles.summaryRow} ${styles.summaryTotal}`}>
                  <span className={styles.summaryTotalLabel}>Tổng cộng:</span>
                  <span className={styles.summaryTotalValue}>
                    {formatCurrency(payableAmount)}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className={styles.dialogActions}>
            <Button
              onClick={() => void onConfirm()}
              className={styles.confirmBtn}
              disabled={isProcessing || paymentMethod === "CHUYEN_KHOAN"}
            >
              <DollarSign size={18} />
              {isProcessing
                ? "Đang xử lý..."
                : paymentMethod === "CHUYEN_KHOAN"
                  ? "Đang chờ xác nhận chuyển khoản"
                  : "Xác nhận thanh toán"}
            </Button>
            <Button variant="outline" onClick={() => onOpenChange(false)} className={styles.cancelBtn} disabled={isProcessing}>
              Hủy
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

export { PaymentDialog };
export default PaymentDialog;