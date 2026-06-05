'use client';

import React from 'react';
import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';
import { getApiErrorMessage } from '@/services/api';
import {
  patientService,
  type PatientAppointmentResponse,
  type PatientMedicalRecordHistoryItemResponse,
  type PatientProfileResponse,
} from '@/services/patientService';
import styles from './dashboard.module.css';
import { 
  Calendar,
  ClipboardList,
  FileText,
  Loader2,
  Plus,
  Stethoscope,
} from 'lucide-react';
import { toast } from 'sonner';

const normalizeStatus = (status: string | null | undefined) => (status ?? '').trim().toUpperCase();

const formatDateTime = (raw: string | null | undefined) => {
  if (!raw) {
    return 'Chưa cập nhật';
  }

  const date = new Date(raw);
  if (Number.isNaN(date.getTime())) {
    return raw;
  }

  return date.toLocaleString('vi-VN', {
    hour12: false,
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export default function PatientDashboard() {
  const router = useRouter();
  const { username } = useAuth();
  const [loading, setLoading] = useState(true);
  const [displayName, setDisplayName] = useState('Bệnh nhân');
  const [profile, setProfile] = useState<PatientProfileResponse | null>(null);
  const [appointments, setAppointments] = useState<PatientAppointmentResponse[]>([]);
  const [records, setRecords] = useState<PatientMedicalRecordHistoryItemResponse[]>([]);

  useEffect(() => {
    let isMounted = true;

    const loadDashboardData = async () => {
      try {
        setLoading(true);
        const [profileData, appointmentData, recordData] = await Promise.all([
          patientService.getProfile(),
          patientService.getMyAppointments(),
          patientService.getMedicalRecordHistory(),
        ]);

        if (!isMounted) {
          return;
        }

        setProfile(profileData);
        setAppointments(appointmentData);
        setRecords(recordData);
        setDisplayName(profileData.fullName || profileData.gmail || username || 'Bệnh nhân');
      } catch (error) {
        if (!isMounted) {
          return;
        }

        toast.error(getApiErrorMessage(error, 'Không thể tải dữ liệu tổng quan bệnh nhân'));
        setDisplayName(username || 'Bệnh nhân');
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    void loadDashboardData();

    return () => {
      isMounted = false;
    };
  }, [username]);

  const appointmentStats = useMemo(() => {
    return appointments.reduce(
      (stats, item) => {
        const status = normalizeStatus(item.status);
        if (status === 'COMPLETED') {
          stats.completed += 1;
        } else if (status === 'CANCELLED' || status === 'CANCELLED_BY_CLINIC') {
          stats.cancelled += 1;
        } else if (status === 'IN_PROGRESS') {
          stats.inProgress += 1;
        } else if (status === 'WAITING' || status === 'APPROVED' || status === 'CONFIRMED') {
          stats.approved += 1;
        } else {
          stats.pending += 1;
        }

        return stats;
      },
      {
        pending: 0,
        approved: 0,
        inProgress: 0,
        completed: 0,
        cancelled: 0,
      }
    );
  }, [appointments]);

  const upcomingAppointment = useMemo(() => {
    const now = Date.now();

    const eligible = appointments.filter((item) => {
      const status = normalizeStatus(item.status);
      const cancelled = status === 'CANCELLED' || status === 'CANCELLED_BY_CLINIC';
      return !cancelled;
    });

    const future = eligible
      .filter((item) => {
        if (!item.appointmentTime) {
          return false;
        }
        return new Date(item.appointmentTime).getTime() >= now;
      })
      .sort((a, b) => new Date(a.appointmentTime ?? 0).getTime() - new Date(b.appointmentTime ?? 0).getTime());

    if (future.length > 0) {
      return future[0];
    }

    return eligible
      .sort((a, b) => new Date(b.appointmentTime ?? 0).getTime() - new Date(a.appointmentTime ?? 0).getTime())[0] ?? null;
  }, [appointments]);

  const recentRecords = useMemo(() => {
    return [...records]
      .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      .slice(0, 5);
  }, [records]);

  return (
    <main className={styles.main}>

        {/* CONTENT */}
        <div className={styles.content}>
          <h1 className={styles.pageTitle}>Chào mừng trở lại, {displayName}!</h1>

          {/* BANNER */}
          <div className={styles.banner}>
            <h2 className={styles.bannerTitle}>Sức khỏe của bạn là ưu tiên của chúng tôi.</h2>
            <p className={styles.bannerDesc}>Dễ dàng đặt lịch khám ngay hôm nay.</p>
            <button className={styles.btnPrimary} onClick={() => router.push('/booking')}>
              <Plus size={20} /> ĐẶT LỊCH KHÁM NGAY
            </button>
          </div>

          {/* CARDS GRID */}
          <div className={styles.cardsGrid}>
            <div className={styles.card}>
              <h3 className={styles.cardTitle}>Lịch hẹn sắp tới</h3>
              {loading ? (
                <div className={styles.cardRow}>
                  <Loader2 size={16} className="animate-spin" color="#2563eb" /> Đang tải...
                </div>
              ) : upcomingAppointment ? (
                <>
                  <div className={styles.cardRow}>
                    <Calendar size={16} color="#2563eb" /> {formatDateTime(upcomingAppointment.appointmentTime)}
                  </div>
                  <div className={styles.cardRow}>
                    <Stethoscope size={16} color="#2563eb" />
                    Bác sĩ: {upcomingAppointment.doctor?.fullName ?? upcomingAppointment.doctor?.username ?? 'Chưa phân công'}
                  </div>
                  <div className={styles.cardRow}>
                    Triệu chứng: {upcomingAppointment.symptoms || 'Chưa cập nhật'}
                  </div>
                </>
              ) : (
                <div className={styles.cardRow}>Chưa có lịch hẹn nào.</div>
              )}
              <button className={styles.btnOutline} onClick={() => router.push('/appointments')}>Xem lịch hẹn</button>
            </div>

            <div className={styles.card}>
              <h3 className={styles.cardTitle}>Trạng thái lịch hẹn</h3>
              <div className={styles.cardRow}>Đang chờ: {appointmentStats.pending}</div>
              <div className={styles.cardRow}>Đã duyệt: {appointmentStats.approved}</div>
              <div className={styles.cardRow}>Đang khám: {appointmentStats.inProgress}</div>
              <div className={styles.cardRow}>Hoàn thành: {appointmentStats.completed}</div>
              <button className={styles.btnOutline} onClick={() => router.push('/appointments')}>Theo dõi chi tiết</button>
            </div>

            <div className={styles.card}>
              <h3 className={styles.cardTitle}>Thông tin tài khoản bệnh nhân</h3>
              <div style={{ fontWeight: 600, color: '#334155', marginBottom: '12px' }}>
                Mã bệnh nhân: {profile?.patientId ?? '--'}
              </div>
              <div className={styles.cardRow}>
                <ClipboardList size={16} color="#2563eb" /> SĐT: {profile?.phoneNumber || 'Chưa cập nhật'}
              </div>
              <div className={styles.cardRow}>
                <FileText size={16} color="#2563eb" /> BHYT: {profile?.healthInsuranceNumber || 'Chưa cập nhật'}
              </div>
              <button className={styles.btnOutline} onClick={() => router.push('/patient-history')}>
                Xem hồ sơ bệnh án
              </button>
            </div>
          </div>

          {/* TABLE */}
          <div className={styles.tableContainer}>
            <div className={styles.tableHeader}>
              <h3 className={styles.cardTitle} style={{ margin: 0 }}>Lịch sử khám bệnh gần đây</h3>
            </div>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Ngày khám</th>
                  <th>Bác sĩ</th>
                  <th>Chẩn đoán</th>
                  <th style={{ textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {recentRecords.length === 0 ? (
                  <tr>
                    <td colSpan={4} style={{ textAlign: 'center', color: '#64748b' }}>
                      Chưa có hồ sơ khám bệnh.
                    </td>
                  </tr>
                ) : (
                  recentRecords.map((record) => (
                    <tr key={record.medicalRecordId}>
                      <td style={{ fontWeight: 500 }}>{formatDateTime(record.createdAt)}</td>
                      <td>{record.doctorUsername || 'Chưa cập nhật'}</td>
                      <td>{record.diagnosis || 'Chưa có chẩn đoán'}</td>
                      <td style={{ textAlign: 'right' }}>
                        <button
                          className={styles.btnAction}
                          onClick={() => router.push('/patient-history')}
                        >
                          Xem lại
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

        </div>
      </main>
  );
}