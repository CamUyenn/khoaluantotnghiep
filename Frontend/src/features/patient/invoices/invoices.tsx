"use client";

import { useEffect, useMemo, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { getApiErrorMessage } from "@/services/api";
import { PaymentReferenceCard } from "@/features/patient/components/payment-reference-card";
import {
  patientService,
  type PatientMedicalRecordDetailResponse,
  type PatientMedicalRecordHistoryItemResponse,
} from "@/services/patientService";
import { toast } from "sonner";
import { CalendarCheck2, Loader2, Receipt, Search } from "lucide-react";
import styles from "./invoices.module.css";

interface PatientInvoiceItem {
  invoiceCode: string;
  medicalRecordId: number;
  appointmentId: number | null;
  paidAt: string | null;
  paymentReference: string | null;
  doctorUsername: string | null;
  diagnosis: string | null;
  serviceTotal: number;
  totalAmount: number;
}

const normalizeStatus = (status: string | null | undefined) => (status ?? "").trim().toUpperCase();

const toLocaleDateTime = (raw: string | null) => {
  if (!raw) {
    return "--";
  }

  const date = new Date(raw);
  if (Number.isNaN(date.getTime())) {
    return raw;
  }

  return date.toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
};

export function PatientInvoices() {
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [invoices, setInvoices] = useState<PatientInvoiceItem[]>([]);

  const loadInvoices = async () => {
    try {
      setLoading(true);
      const history = await patientService.getMedicalRecordHistory();
      const completedRecords = history.filter((record) => normalizeStatus(record.appointmentStatus) === "COMPLETED");

      const details = await Promise.all(
        completedRecords.map((record) =>
          patientService.getMedicalRecordDetail(record.medicalRecordId).catch(() => null)
        )
      );

      const mappedInvoices = completedRecords.map((record, index) => {
        const detail = details[index];
        return mapToInvoiceItem(record, detail);
      });

      setInvoices(mappedInvoices.sort((a, b) => {
        const timeA = a.paidAt ? new Date(a.paidAt).getTime() : 0;
        const timeB = b.paidAt ? new Date(b.paidAt).getTime() : 0;
        return timeB - timeA;
      }));
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể tải lịch sử hóa đơn"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadInvoices();
  }, []);

  const filteredInvoices = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    if (!keyword) {
      return invoices;
    }

    return invoices.filter((invoice) => {
      return (
        invoice.invoiceCode.toLowerCase().includes(keyword) ||
        String(invoice.medicalRecordId).includes(keyword) ||
        (invoice.paymentReference ?? "").toLowerCase().includes(keyword) ||
        (invoice.doctorUsername ?? "").toLowerCase().includes(keyword) ||
        (invoice.diagnosis ?? "").toLowerCase().includes(keyword)
      );
    });
  }, [invoices, query]);

  const totalPaid = useMemo(() => {
    return invoices.reduce((sum, item) => sum + item.serviceTotal, 0);
  }, [invoices]);

  const totalExaminationCost = useMemo(() => {
    return invoices.reduce((sum, item) => sum + item.serviceTotal, 0);
  }, [invoices]);

  return (
    <main className={styles.page}>
      <section className={styles.headerSection}>
        <h1 className={styles.title}>Hóa đơn đã thanh toán</h1>
        <p className={styles.subtitle}>
          Lịch sử hóa đơn hoàn thành được tổng hợp từ hồ sơ khám đã kết thúc của bạn.
        </p>
      </section>

      <section className={styles.statsGrid}>
        <Card className={styles.statCard}>
          <p className={styles.statLabel}>Số hóa đơn đã hoàn thành</p>
          <p className={styles.statValue}>{invoices.length}</p>
        </Card>
        <Card className={styles.statCard}>
          <p className={styles.statLabel}>Tổng chi phí khám</p>
          <p className={styles.statValue}>{totalExaminationCost.toLocaleString("vi-VN")}đ</p>
        </Card>
        <Card className={styles.statCard}>
          <p className={styles.statLabel}>Tổng đã thanh toán</p>
          <p className={styles.statValue}>{totalPaid.toLocaleString("vi-VN")}đ</p>
        </Card>
      </section>

      <Card className={styles.tableCard}>
          {loading ? (
          <div className={styles.loadingBox}>
            <Loader2 className={styles.spinning} size={20} />
            <span>Đang tải hóa đơn...</span>
          </div>
        ) : filteredInvoices.length === 0 ? (
          <div className={styles.emptyBox}>
            <Receipt size={36} />
            <p>Chưa có hóa đơn hoàn thành.</p>
          </div>
        ) : (
          <div className={styles.list}>
            {filteredInvoices.map((invoice) => (
              <article key={invoice.invoiceCode} className={styles.invoiceItem}>
                <div className={styles.itemTop}>
                  <div>
                    <h3 className={styles.code}>{invoice.invoiceCode}</h3>
                    <p className={styles.recordText}>Bệnh án #{invoice.medicalRecordId}</p>
                  </div>
                  <Badge className={styles.paidBadge}>Đã thanh toán</Badge>
                </div>

                <div className={styles.metaGrid}>
                  <div className={styles.metaCell}>
                    <CalendarCheck2 size={15} />
                    <span>{toLocaleDateTime(invoice.paidAt)}</span>
                  </div>
                  <div className={styles.metaCell}>
                    <span>Bác sĩ: {invoice.doctorUsername ?? "Chưa cập nhật"}</span>
                  </div>
                </div>

                <p className={styles.diagnosis}>Chẩn đoán: {invoice.diagnosis ?? "Chưa cập nhật"}</p>

                <div className={styles.amountRow}>
                  <span>Chi phí khám</span>
                  <strong>{invoice.serviceTotal.toLocaleString("vi-VN")}đ</strong>
                </div>

                <div className={styles.amountRow}>
                  <span>Tổng đã thanh toán</span>
                  <strong>{invoice.serviceTotal.toLocaleString("vi-VN")}đ</strong>
                </div>

                <PaymentReferenceCard
                  paymentReference={invoice.paymentReference}
                  amount={invoice.totalAmount}
                  title="Mã chuyển khoản hóa đơn"
                  subtitle="Dùng mã này để ngân hàng/SePay đối soát đúng hóa đơn của bạn."
                  paidAt={invoice.paidAt ? toLocaleDateTime(invoice.paidAt) : undefined}
                  hideQr
                />
              </article>
            ))}
          </div>
        )}
      </Card>
    </main>
  );
}

function mapToInvoiceItem(
  history: PatientMedicalRecordHistoryItemResponse,
  detail: PatientMedicalRecordDetailResponse | null
): PatientInvoiceItem {
  const serviceTotal = Number(detail?.totalServiceFee ?? history.totalServiceFee ?? 0);
  const totalAmount = serviceTotal;

  return {
    invoiceCode: `HD-${history.invoiceId ?? history.medicalRecordId}`,
    medicalRecordId: history.medicalRecordId,
    appointmentId: history.appointmentId,
    paidAt: history.paidAt ?? history.createdAt,
    paymentReference: detail?.paymentReference ?? history.paymentReference ?? null,
    doctorUsername: detail?.doctorUsername ?? history.doctorUsername,
    diagnosis: detail?.diagnosis ?? history.diagnosis,
    serviceTotal,
    totalAmount,
  };
}
