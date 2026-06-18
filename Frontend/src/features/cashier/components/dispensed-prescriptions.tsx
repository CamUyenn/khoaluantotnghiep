import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { CheckCircle } from "lucide-react";
import { Prescription } from "@/types/pharmacy.type";
import { getServicePaymentBreakdown } from "../utils/service-payment-breakdown";
import styles from "../cashier.module.css";

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(
    amount,
  );

interface DispensedPrescriptionsProps {
  prescriptions: Prescription[];
}

function DispensedPrescriptions({ prescriptions }: DispensedPrescriptionsProps) {
  return (
    <div className={styles.section}>
      <h2 className={styles.sectionTitle}>
        Đã thanh toán ({prescriptions.length})
      </h2>
      <div className={styles.doneGrid}>
        {prescriptions.map((prescription, idx) => (
          <Card key={`${prescription.id}-${prescription.invoiceId ?? "na"}-${idx}`} className={styles.doneCard}>
            {(() => {
              const serviceItems = prescription.serviceItems ?? [];
              const totalServiceFee = prescription.serviceFee ?? 0;
              const breakdown = getServicePaymentBreakdown(prescription, Number(prescription.advanceAmount ?? 0));
              const initialConsultationFee = Number(
                prescription.advanceAmount ?? breakdown.consultationCoveredAmount ?? breakdown.consultationFee ?? 0,
              );
              const prepaidConsultationItems = breakdown.consultationServices.filter((item) => Number(item.coveredLineTotal || 0) > 0);

              return (
                <>
                  <div className={styles.doneHeader}>
                    <div>
                      <h3 className={styles.doneName}>{prescription.patientName}</h3>
                      <p className={styles.donePhone}>{prescription.phone}</p>
                    </div>
                    <Badge className={styles.doneBadge}>Đã thanh toán</Badge>
                  </div>

                  <div className={styles.amountList}>
                    <div className={styles.amountRow}>
                      <span className={styles.amountLabel}>Bác sĩ:</span>
                      <span className={styles.amountValue}>{prescription.doctor}</span>
                    </div>
                    <div className={styles.amountRow}>
                      <span className={styles.amountLabel}>Ngày khám:</span>
                      <span className={styles.amountValue}>
                        {new Date(prescription.date).toLocaleDateString("vi-VN")}
                      </span>
                    </div>
                    {serviceItems.length > 0 && breakdown.consultationFee > 0 ? (
                      <>
                        <div className={`${styles.amountRow} ${styles.amountBorder}`}>
                          <span className={styles.amountLabel}>Phí khám ban đầu:</span>
                          <span className={styles.amountValue}>{formatCurrency(initialConsultationFee)}</span>
                        </div>
                        {prepaidConsultationItems.map((item) => (
                          <div key={`consult-${item.serviceId}`} className={styles.amountRow}>
                            <span className={styles.amountLabel}>- {item.serviceName}</span>
                            <span className={styles.amountValue}>{formatCurrency(Number(item.coveredLineTotal || 0))}</span>
                          </div>
                        ))}
                        {breakdown.additionalOutstandingAmount > 0 && (
                          <>
                            <div className={styles.amountRow}>
                              <span className={styles.amountLabel}>Dịch vụ chỉ định thêm chưa thanh toán:</span>
                              <span className={styles.amountValue}>{formatCurrency(breakdown.additionalOutstandingAmount)}</span>
                            </div>
                            {breakdown.unpaidAdditionalServices.map((item) => (
                              <div key={`additional-${item.serviceId}`} className={styles.amountRow}>
                                <span className={styles.amountLabel}>- {item.serviceName}</span>
                                <span className={styles.amountValue}>{formatCurrency(item.unpaidLineTotal)}</span>
                              </div>
                            ))}
                          </>
                        )}
                      </>
                    ) : (
                      <div className={styles.amountRow}>
                        <span className={styles.amountLabel}>Phí khám:</span>
                        <span className={styles.amountValue}>
                          {formatCurrency(totalServiceFee)}
                        </span>
                      </div>
                    )}
                    {prescription.insuranceDiscount && prescription.insuranceDiscount > 0 && (
                      <div className={`${styles.amountRow} ${styles.discountRow}`}>
                        <span>Giảm trừ BHYT:</span>
                        <span className={styles.amountValue}>
                          -{formatCurrency(prescription.insuranceDiscount)}
                        </span>
                      </div>
                    )}
                    <div className={`${styles.amountRow} ${styles.totalRow}`}>
                      <span className={styles.totalLabel}>Tổng cộng:</span>
                      <span className={styles.totalValue}>
                        {formatCurrency(prescription.grandTotal ?? prescription.totalAmount ?? totalServiceFee)}
                      </span>
                    </div>
                  </div>
                </>
              );
            })()}
          </Card>
        ))}

        {prescriptions.length === 0 && (
          <Card className={styles.emptyCard}>
            <CheckCircle size={44} color="#cbd5e1" />
            <p className={styles.emptyText}>Chưa có hồ sơ được thanh toán</p>
          </Card>
        )}
      </div>
    </div>
  );
}

export { DispensedPrescriptions };
export default DispensedPrescriptions;