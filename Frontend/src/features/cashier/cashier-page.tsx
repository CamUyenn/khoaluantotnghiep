'use client';

import { useEffect, useRef, useState } from "react";
import axios from "axios";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Prescription } from "@/types/pharmacy.type";
import PharmacyStats from "./components/pharmacy-stats";
import PendingPrescriptions from "./components/pending-prescriptions";
import DispensedPrescriptions from "./components/dispensed-prescriptions";
import PaymentDialog from "./components/payment-dialog";
import CashierRefreshListener from "@/components/cashier-refresh-listener";
import { cashierService } from "@/services/cashierService";
import { getApiErrorMessage } from "@/services/api";
import styles from "@/styles/common.module.css";

export function PharmacyDashboard() {
  const router = useRouter();
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [selectedPrescription, setSelectedPrescription] = useState<Prescription | null>(null);
  const [isPaymentOpen, setIsPaymentOpen] = useState(false);
  const [paymentData, setPaymentData] = useState({
    serviceFee: "0",
    insuranceDiscount: "0",
  });
  const [paymentMethod, setPaymentMethod] = useState<"TIEN_MAT" | "CHUYEN_KHOAN" | "POS">("TIEN_MAT");
  const [transferConfirmed, setTransferConfirmed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  const autoPaymentSubmittedRef = useRef(false);

  const pendingPrescriptions = prescriptions.filter((p) => p.status === "pending");
  const dispensedPrescriptions = prescriptions.filter((p) => p.status === "dispensed");

  const toPrescription = (
    detail: Awaited<ReturnType<typeof cashierService.searchPaymentRecord>>,
    status: "pending" | "dispensed",
    paidAt?: string,
    paymentMethod?: string,
  ): Prescription => {
    const totalMedicationCost = 0;
    const serviceFee = Number(detail.totalServiceFee || 0);
    const grandTotal = Number(detail.grandTotal || 0);
    const remainingAmount = Number(detail.remainingAmount ?? grandTotal);
    const advanceAmount = Number(detail.advanceAmount ?? Math.max(grandTotal - remainingAmount, 0));
    const insuranceDiscount = 0;
    const payableServiceAmount = status === "pending" ? remainingAmount : grandTotal;

    return {
      id: `RX-${detail.invoiceId}`,
      invoiceId: detail.invoiceId,
      appointmentId: detail.appointmentId,
      patientName: detail.patientName,
      phone: detail.phoneNumber || "",
      insuranceNumber: "",
      doctor: `LH #${detail.appointmentId}`,
      diagnosis: "Theo bệnh án từ bác sĩ",
      treatment: "Thanh toán dịch vụ khám theo chỉ định",
      prescriptionItems: (detail.medicines || []).map((item) => ({
        medicationId: String(item.medicineId),
        medicationName: item.medicineName,
        dosage: "",
        unit: "đv",
        quantity: item.quantity,
        price: Number(item.unitPrice || 0),
        usage: item.usageInstructions || "Chưa cập nhật",
      })),
      totalMedicationCost,
      date: paidAt || detail.appointmentTime,
      status,
      serviceFee: serviceFee || grandTotal,
      serviceItems: detail.services || [],
      insuranceDiscount,
      totalAmount: payableServiceAmount,
      grandTotal,
      advanceAmount,
      remainingAmount,
      paymentMethod,
    };
  };

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const [queue, history] = await Promise.all([
        cashierService.getWaitingPaymentQueue(),
        cashierService.getTransactionHistory(),
      ]);

      const pendingDetails = await Promise.all(
        queue.map((item) => cashierService.searchPaymentRecord(String(item.invoiceId)).catch(() => null)),
      );
      const pendingMapped = pendingDetails
        .filter((detail): detail is NonNullable<typeof detail> => Boolean(detail))
        .map((detail) => toPrescription(detail, "pending"));

      const paidDetails = await Promise.all(
        (history.transactions || []).map((item) =>
          cashierService
            .getPaidInvoiceDetail(item.invoiceId)
            .then((detail) => ({ detail, paidAt: item.paidAt, paymentMethod: item.paymentMethod }))
            .catch(() => null),
        ),
      );
      const paidMapped = paidDetails
        .filter((row): row is NonNullable<typeof row> => Boolean(row))
        .map((row) => toPrescription(row.detail, "dispensed", row.paidAt, row.paymentMethod));

      // Không loại hồ sơ chờ theo lịch sử đã thanh toán, vì một hóa đơn có thể đã thanh toán
      // trước đó nhưng được mở lại khi bác sĩ chỉ định thêm dịch vụ và phát sinh số tiền mới.
      const merged = [...pendingMapped, ...paidMapped];
      const unique = new Map<string, Prescription>();
      merged.forEach((item) => {
        const key = `${item.invoiceId ?? item.id}-${item.status}`;
        if (!unique.has(key)) {
          unique.set(key, item);
        }
      });
      setPrescriptions(Array.from(unique.values()));
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể tải dữ liệu thu ngân"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadDashboardData();
  }, []);

  const totalRevenue = dispensedPrescriptions.reduce((sum, p) => sum + (p.totalAmount || 0), 0);

  const openPaymentDialog = (prescription: Prescription) => {
    setSelectedPrescription(prescription);
    setPaymentMethod("TIEN_MAT");
    setTransferConfirmed(false);
    autoPaymentSubmittedRef.current = false;
    setPaymentData({
      serviceFee: String(Math.floor(prescription.remainingAmount ?? prescription.totalAmount ?? 0)),
      insuranceDiscount: String(Math.floor(prescription.insuranceDiscount || 0)),
    });
    setIsPaymentOpen(true);
  };

  const handlePaymentDataChange = (field: string, value: string) => {
    setPaymentData((prev) => ({ ...prev, [field]: value }));
  };

  const handlePayment = async (autoTriggered = false) => {
    if (!selectedPrescription?.invoiceId) return;

    if (autoTriggered && autoPaymentSubmittedRef.current) {
      return;
    }

    if (paymentMethod === "CHUYEN_KHOAN" && !autoTriggered) {
      toast.info("Đang chờ hệ thống tự động xác nhận chuyển khoản");
      return;
    }

    if (paymentMethod === "POS" && !transferConfirmed) {
      toast.error("Vui lòng xác nhận giao dịch POS thành công trước khi thanh toán");
      return;
    }

    try {
      setProcessing(true);
      if (autoTriggered) {
        autoPaymentSubmittedRef.current = true;
      }

      const response = await cashierService.processPayment(selectedPrescription.invoiceId, {
        paymentMethod,
        paymentSuccessful: paymentMethod === "TIEN_MAT" ? true : (autoTriggered || transferConfirmed),
        exportInvoice: false,
        applyHealthInsurance: Number(paymentData.insuranceDiscount || 0) > 0,
      });

      toast.success(response.message || "Thanh toán thành công");
      setIsPaymentOpen(false);
      setSelectedPrescription(null);
      autoPaymentSubmittedRef.current = false;
      router.push("/cashier/history");
    } catch (error) {
      const isAlreadyPaid =
        paymentMethod === "CHUYEN_KHOAN"
        && autoTriggered
        && axios.isAxiosError(error)
        && error.response?.status === 409;

      autoPaymentSubmittedRef.current = false;
      if (isAlreadyPaid) {
        toast.success("Thanh toán đã được ghi nhận tự động.");
        setIsPaymentOpen(false);
        setSelectedPrescription(null);
        router.push("/cashier/history");
      } else {
        toast.error(getApiErrorMessage(error, "Thanh toán thất bại"));
      }
    } finally {
      setProcessing(false);
    }
  };

  return (
      <main className={styles.mainArea}>
        <div className={styles.container}>
          <CashierRefreshListener enabled onRefresh={loadDashboardData} />
          <div className={styles.header}>
            <h1>Thu ngân phòng khám</h1>
            <p>Thanh toán phí khám và các dịch vụ được chỉ định</p>
          </div>

          {loading && <div className={styles.emptyBox}>Đang tải dữ liệu...</div>}

          {!loading && (
            <>
              <PharmacyStats 
                pendingCount={pendingPrescriptions.length}
                dispensedCount={dispensedPrescriptions.length}
                totalRevenue={totalRevenue}
              />

              <PendingPrescriptions 
                prescriptions={pendingPrescriptions}
                onOpenPayment={openPaymentDialog}
              />

              <DispensedPrescriptions 
                prescriptions={dispensedPrescriptions}
              />
            </>
          )}

          <PaymentDialog 
            isOpen={isPaymentOpen}
            onOpenChange={setIsPaymentOpen}
            prescription={selectedPrescription}
            paymentData={paymentData}
            onPaymentDataChange={handlePaymentDataChange}
            paymentMethod={paymentMethod}
            onPaymentMethodChange={setPaymentMethod}
            transferConfirmed={transferConfirmed}
            onTransferConfirmedChange={setTransferConfirmed}
            onConfirm={handlePayment}
            isProcessing={processing}
          />
        </div>
      </main>
  );
}