'use client';

import { useRouter } from "next/navigation";
import { Card } from "@/components/ui/card";
import { toast } from "sonner";
import { BookingForm } from "./components/booking-form"; 
import styles from "./booking.module.css";

export function BookingPageContent() {
  const router = useRouter();

  const handleBookingSuccess = (appointmentId: number) => {
    toast.success("Đặt lịch khám thành công!");
    router.push(`/appointments?createdId=${appointmentId}`);
  };

  return (
    <div className={styles.container}>
      <div className={styles.formWrapper}>
        <div className={styles.header}>
          <h1 className={styles.title}>Đặt lịch khám bệnh</h1>
          <p className={styles.subtitle}>Vui lòng điền đầy đủ thông tin để đặt lịch khám</p>
        </div>

        <Card className={styles.cardForm}>
          {/* Nhúng form vào và lắng nghe sự kiện onSuccess */}
          <BookingForm onSuccess={handleBookingSuccess} />
        </Card>

        <div className={styles.footer}>
          <p>Lưu ý: Các trường có dấu (*) là bắt buộc</p>
        </div>
      </div>
    </div>
  );
}