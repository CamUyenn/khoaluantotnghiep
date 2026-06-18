'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { getApiErrorMessage } from "@/services/api";
import { clinicConfigService } from "@/services/clinicConfigService";
import { patientService } from "@/services/patientService";
import { masterDataService, type SymptomTemplateResponse } from "@/services/masterDataService";
import PaymentRealtimeListener from "@/components/payment-realtime-listener";
import { Calendar, CreditCard, FileText, Loader2, User } from "lucide-react";
import { toast } from "sonner";
import styles from "../booking.module.css";

// Định nghĩa props để truyền sự kiện ra ngoài component cha
interface BookingFormProps {
  onSuccess: (appointmentId: number) => void;
}
type BookingFlowState = "form" | "waiting-transfer" | "success" | "failed";

//Hàm chuẩn hóa thời gian để backend hiểu được
const toApiDateTime = (value: string) => {
  const normalized = value.trim();
  if (!normalized) {
    return normalized;
  }

  // Backend: yyyy-MM-dd'T'HH:mm:ss.
  if (normalized.length === 16) {
    return `${normalized}:00`;
  }

  return normalized;
};

export function BookingForm({ onSuccess }: BookingFormProps) {
  const PAYMENT_CHECK_INTERVAL_MS = 10_000;
  const [submitting, setSubmitting] = useState(false);
  const [estimatingFee, setEstimatingFee] = useState(false);
  const [bookingFlow, setBookingFlow] = useState<BookingFlowState>("form");
  const [transferRequested, setTransferRequested] = useState(false);
  const [paymentReference, setPaymentReference] = useState<string>("");
  const [paymentStatusMessage, setPaymentStatusMessage] = useState<string>("");
  const [formData, setFormData] = useState({
    fullName: "",
    gender: "",
    dateOfBirth: "",
    hometown: "",
    phone: "",
    idNumber: "",
    insuranceNumber: "",
    appointmentTime: "",
    reason: "",
  });
  const [paymentMethod, setPaymentMethod] = useState<"TIEN_MAT" | "CHUYEN_KHOAN">("CHUYEN_KHOAN");
  const [categories, setCategories] = useState<{ id: number; name: string }[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [symptoms, setSymptoms] = useState<SymptomTemplateResponse[]>([]);
  const [selectedSymptomIds, setSelectedSymptomIds] = useState<number[]>([]);
  const [estimatedTotalFee, setEstimatedTotalFee] = useState<number | null>(null);
  const [estimatedServices, setEstimatedServices] = useState<
    {
      serviceId: number;
      serviceName: string;
      quantity: number;
      unitPrice: number;
      lineTotal: number;
    }[]
  >([]);
  const [bankConfig, setBankConfig] = useState<{
    bankBin: string;
    bankAccount: string;
    accountName: string;
    bankName: string;
  } | null>(null);
  const [bankConfigLoading, setBankConfigLoading] = useState(true);

  const generatePaymentReference = useMemo(() => {
    return () => {
      const randomPart = Math.random().toString(36).slice(2, 8).toUpperCase();
      return `BK-${Date.now().toString().slice(-8)}-${randomPart}`;
    };
  }, []);

  const normalizeGenderValue = (value?: string | null) => {
    if (!value) return "";
    const normalized = value.trim().toLowerCase();
    if (["male", "nam", "m"].includes(normalized)) return "male";
    if (["female", "nữ", "nu", "f"].includes(normalized)) return "female";
    return "other";
  };

  const normalizeDateInput = (value?: string | null) => {
    if (!value) return "";
    const trimmed = value.trim();
    if (!trimmed) return "";
    const datePart = trimmed.split("T")[0];
    return datePart;
  };

  const formatCurrency = (value: number | null) => {
    if (value == null) {
      return "0đ";
    }
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);
  };

  const transferTrackingStartedRef = useRef(false);
  const transferFinalizeStartedRef = useRef(false);
  const transferStartedAtRef = useRef<number | null>(null);

  const isTransferBookingReady =
    paymentMethod === 'CHUYEN_KHOAN' &&
    !submitting &&
    !estimatingFee &&
    estimatedTotalFee !== null &&
    Boolean(formData.fullName.trim()) &&
    Boolean(formData.gender.trim()) &&
    Boolean(formData.phone.trim()) &&
    Boolean(formData.appointmentTime.trim()) &&
    selectedCategoryId != null &&
    selectedSymptomIds.length > 0;

  const createAppointment = useCallback(async (source: 'manual' | 'after-payment') => {
    const normalizedCustomReason = formData.reason.trim();

    if (!formData.fullName || !formData.gender || !formData.phone || !formData.appointmentTime) {
      toast.error("Vui lòng điền đầy đủ thông tin bắt buộc");
      return;
    }
    if (selectedCategoryId == null) {
      toast.error("Vui lòng chọn nhóm triệu chứng");
      return;
    }
    if (selectedSymptomIds.length === 0) {
      toast.error("Vui lòng chọn ít nhất một triệu chứng");
      return;
    }

    try {
      setSubmitting(true);
      if (source === 'manual') {
        setBookingFlow('form');
      }

      const selectedSymptomNames = symptoms
        .filter((symptom) => selectedSymptomIds.includes(symptom.id))
        .map((symptom) => symptom.symptomName);

      const paymentReferenceForRequest = paymentMethod === 'CHUYEN_KHOAN'
        ? (paymentReference || generatePaymentReference())
        : undefined;

      const symptomsForApi = selectedSymptomNames.join(' | ')
        + (normalizedCustomReason ? ' - ' + normalizedCustomReason : '');

      const createdAppointment = await patientService.createAppointment({
        appointmentTime: toApiDateTime(formData.appointmentTime),
        categoryId: selectedCategoryId,
        symptomIds: selectedSymptomIds,
        paymentMethod,
        symptoms: symptomsForApi,
        paymentReference: paymentReferenceForRequest,
      });

      if (paymentMethod === 'CHUYEN_KHOAN') {
        const resolvedPaymentReference = paymentReferenceForRequest || createdAppointment.paymentReference || '';
        setPaymentReference(resolvedPaymentReference);
        if (source === 'after-payment') {
          setBookingFlow('success');
          setPaymentStatusMessage('Đã nộp tiền và tự động đặt lịch thành công');
          toast.success('Đã nộp tiền và tự động đặt lịch thành công');
          onSuccess(createdAppointment.id);
          return;
        }

        setBookingFlow('waiting-transfer');
        setPaymentStatusMessage('Chưa nộp tiền. Quét QR và chuyển khoản trong 10 giây.');
        toast.info('Chưa nộp tiền. Hệ thống sẽ chờ tối đa 10 giây để xác nhận');
        return;
      }

      onSuccess(createdAppointment.id);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể đặt lịch khám"));
    } finally {
      setSubmitting(false);
    }
  }, [
    formData.appointmentTime,
    formData.fullName,
    formData.gender,
    formData.phone,
    formData.reason,
    paymentMethod,
    paymentReference,
    generatePaymentReference,
    onSuccess,
    selectedCategoryId,
    selectedSymptomIds,
    symptoms,
  ]);

  useEffect(() => {
    let isMounted = true;

    const loadProfile = async () => {
      try {
        const profile = await patientService.getProfile(); //Lấy thông tin của mình
        if (!isMounted) {
          return;
        }
        // Điền thông tin vào form nếu có
        setFormData((prev) => ({
          ...prev,
          fullName: profile.fullName ?? "",
          gender: normalizeGenderValue(profile.gender),
          dateOfBirth: normalizeDateInput(profile.dateOfBirth as unknown as string | null | undefined),
          hometown: profile.hometown ?? "",
          phone: profile.phoneNumber ?? "",
          idNumber: profile.nationalId ?? "",
          insuranceNumber: profile.healthInsuranceNumber ?? "",
        }));
      } catch {
        // Do not block form usage when profile API fails.
      }
    };

    const loadCategoriesAndSymptoms = async () => {
      try {
        const cats = await masterDataService.getCategories();
        if (!isMounted) return;
        setCategories(cats.map(c => ({ id: c.id, name: c.name })));
        if (cats.length > 0) {
          const firstId = cats[0].id;
          setSelectedCategoryId(firstId);
          try {
            const s = await masterDataService.getSymptomsByCategory(firstId);
            if (!isMounted) return;
            setSymptoms(s);
          } catch {
            if (!isMounted) return;
            setSymptoms([]);
          }
        } else {
          setSymptoms([]);
        }
      } catch {
        if (!isMounted) return;
        setSymptoms([]);
      }
    };

    void loadProfile();
    void loadCategoriesAndSymptoms();

    void clinicConfigService.getBankConfig()
      .then((config) => {
        if (isMounted) {
          setBankConfig(config);
        }
      })
      .catch(() => {
        if (isMounted) {
          setBankConfig(null);
        }
      })
      .finally(() => {
        if (isMounted) {
          setBankConfigLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    if (selectedCategoryId == null) return;
    let mounted = true;
    const load = async () => {
      try {
        const s = await masterDataService.getSymptomsByCategory(selectedCategoryId);
        if (!mounted) return;
        setSymptoms(s);
      } catch {
        if (!mounted) return;
        setSymptoms([]);
      }
    };
    void load();
    return () => { mounted = false; };
  }, [selectedCategoryId]);

  useEffect(() => {
    if (selectedCategoryId == null) return;
    setSelectedSymptomIds([]);
    setEstimatedTotalFee(null);
    setEstimatedServices([]);
    setFormData((prev) => ({ ...prev, reason: '' }));
      setTransferRequested(false);
      setPaymentReference('');
  }, [selectedCategoryId]);

  useEffect(() => {
    if (paymentMethod === "TIEN_MAT") {
      setPaymentReference('');
      setTransferRequested(false);
      setBookingFlow('form');
      setPaymentStatusMessage('');
      transferTrackingStartedRef.current = false;
      transferFinalizeStartedRef.current = false;
      transferStartedAtRef.current = null;
    }
  }, [generatePaymentReference, paymentMethod]);

  useEffect(() => {
    if (paymentMethod !== 'CHUYEN_KHOAN' || !transferRequested) {
      return;
    }

    if (!isTransferBookingReady) {
      setBookingFlow('form');
      setPaymentStatusMessage('');
      transferTrackingStartedRef.current = false;
      transferFinalizeStartedRef.current = false;
      transferStartedAtRef.current = null;
      return;
    }

    if (paymentReference) {
      if (!transferTrackingStartedRef.current) {
        transferTrackingStartedRef.current = true;
        transferStartedAtRef.current = Date.now();
      }
      setBookingFlow('waiting-transfer');
      if (!paymentStatusMessage) {
        setPaymentStatusMessage('Chưa nộp tiền. Quét QR và hệ thống sẽ kiểm tra lại sau 1 phút.');
      }
      return;
    }

    setPaymentReference((prev) => prev || generatePaymentReference());
  }, [generatePaymentReference, isTransferBookingReady, paymentMethod, paymentReference, paymentStatusMessage]);

  useEffect(() => {
    if (selectedSymptomIds.length === 0 || selectedCategoryId == null) {
      setEstimatedTotalFee(null);
      setEstimatedServices([]);
      return;
    }

    let mounted = true;
    const load = async () => {
      try {
        setEstimatingFee(true);
        const result = await patientService.estimateAppointmentFee(selectedSymptomIds);
        if (!mounted) return;
        setEstimatedTotalFee(Number(result.estimatedTotalFee ?? 0));
        setEstimatedServices(result.services ?? []);
      } catch {
        if (!mounted) return;
        setEstimatedTotalFee(null);
        setEstimatedServices([]);
      } finally {
        if (mounted) {
          setEstimatingFee(false);
        }
      }
    };

    void load();
    return () => {
      mounted = false;
    };
  }, [selectedSymptomIds]);

  useEffect(() => {
    if (paymentMethod !== 'CHUYEN_KHOAN' || bookingFlow !== 'waiting-transfer' || !paymentReference) {
      return;
    }

    let mounted = true;
    let timer: ReturnType<typeof setTimeout> | null = null;
    const startedAt = transferStartedAtRef.current ?? Date.now();
    transferStartedAtRef.current = startedAt;

    const pollStatus = async () => {
      try {
        const status = await patientService.getPaymentReferenceStatus(paymentReference);
        if (!mounted) return;

        const normalizedTransactionStatus = (status.transactionStatus || '').toUpperCase();
        const normalizedPaymentStatus = (status.paymentStatus || '').toUpperCase();

        if (normalizedTransactionStatus === 'SUCCESS' || normalizedPaymentStatus === 'FULLY_PAID') {
          setPaymentStatusMessage('Đã nộp tiền thành công. Đang tự động đặt lịch...');
          if (!transferFinalizeStartedRef.current) {
            transferFinalizeStartedRef.current = true;
            await createAppointment('after-payment');
          }
          return;
        }

        if (normalizedTransactionStatus === 'FAILED') {
          setBookingFlow('failed');
          setPaymentStatusMessage('Thanh toán thất bại. Vui lòng thử lại.');
          toast.error('Thanh toán thất bại. Vui lòng thử lại');
          return;
        }

        const elapsedSeconds = Math.max(0, Math.floor((Date.now() - startedAt) / 1000));
        setPaymentStatusMessage(
          status.message || `Chưa nộp tiền. Hệ thống sẽ kiểm tra lại sau 1 phút. Đã chờ ${elapsedSeconds}s.`
        );
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
      if (timer) {
        clearTimeout(timer);
      }
    };
  }, [bookingFlow, createAppointment, paymentMethod, paymentReference]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (paymentMethod === 'CHUYEN_KHOAN') {
      if (!isTransferBookingReady) {
        toast.info('Điền đủ thông tin để hiển thị QR và chờ xác nhận chuyển khoản');
        return;
      }
      const nextReference = paymentReference || generatePaymentReference();
      setPaymentReference(nextReference);
      setTransferRequested(true);
      setBookingFlow('waiting-transfer');
      setPaymentStatusMessage('Chưa nộp tiền. Quét QR và hệ thống sẽ kiểm tra lại sau 1 phút.');
      return;
    }

    await createAppointment('manual');
  };

  const selectedSymptomsCount = selectedSymptomIds.length;
  const selectedSymptomNames = symptoms
    .filter((symptom) => selectedSymptomIds.includes(symptom.id))
    .map((symptom) => symptom.symptomName);
  const selectedSymptomsText = selectedSymptomNames.join(', ');
  const reasonPrefix = selectedSymptomsText
    ? `Triệu chứng đã chọn: ${selectedSymptomsText}\n`
    : '';
  const reasonInputValue = `${reasonPrefix}${formData.reason}`;
  const patientLabel = formData.fullName.trim() || "BENHNHAN";
  const phoneLabel = formData.phone.trim() || "NA";
  const symptomLabel = selectedSymptomsText || "KHONG_RO";

  const extractSymptomQuery = (value: string) => {
    const trimmed = value.trim();
    if (!trimmed) return '';
    const tokens = trimmed.split(/[,\n]/).map((token) => token.trim()).filter(Boolean);
    return tokens[tokens.length - 1] ?? '';
  };

  const replaceLastSymptomToken = (value: string, replacement: string) => {
    const separatorMatch = value.match(/[\s\S]*([,\n])\s*[^,\n]*$/);
    if (!separatorMatch) {
      return replacement;
    }
    const separatorIndex = separatorMatch[0].lastIndexOf(separatorMatch[1]);
    const prefix = value.slice(0, separatorIndex + 1);
    return `${prefix} ${replacement}`.replace(/\s+$/u, '');
  };

  const removeSymptomTokens = (value: string, tokensToRemove: string[]) => {
    const removeSet = new Set(tokensToRemove.map((token) => token.toLowerCase()));
    return value
      .split(/[\n,]/)
      .map((token) => token.trim())
      .filter((token) => token && !removeSet.has(token.toLowerCase()))
      .join(', ');
  };

  const symptomQuery = extractSymptomQuery(formData.reason);
  const symptomSuggestions = symptomQuery
    ? symptoms.filter((symptom) => {
        if (selectedSymptomIds.includes(symptom.id)) return false;
        return symptom.symptomName.toLowerCase().includes(symptomQuery.toLowerCase());
      })
    : [];
  const estimatedTransferAmount = Math.max(0, Math.round(estimatedTotalFee ?? 0));
  const bankBin = bankConfig?.bankBin?.trim();
  const bankAccount = bankConfig?.bankAccount?.trim();
  const accountNameRaw = bankConfig?.accountName?.trim();
  const bankName = bankConfig?.bankName?.trim() || "TRAN CAM UYEN";
  const qrReady = Boolean(bankBin && bankAccount && accountNameRaw);
  const transferAccountName = encodeURIComponent(accountNameRaw || "");
  const transferNote = encodeURIComponent(
    `REF:${paymentReference || "NA"} BN:${patientLabel} SDT:${phoneLabel}`
  );
  const transferQrUrl = qrReady
    ? `https://img.vietqr.io/image/${bankBin}-${bankAccount}-compact2.png?amount=${estimatedTransferAmount}&addInfo=${transferNote}&accountName=${transferAccountName}`
    : "";
  const normalizedPaymentCode = paymentReference ? paymentReference.trim().replace(/\s+/g, "_") : "";

  return (
    <form onSubmit={handleSubmit} className={styles.form}>
      {(bookingFlow === 'waiting-transfer' || bookingFlow === 'failed' || bookingFlow === 'success') && (
        <div className={styles.paymentStatusBanner}>
          <div className={styles.paymentStatusTitle}>
            {bookingFlow === 'success'
              ? 'Đã thanh toán thành công'
              : bookingFlow === 'failed'
                ? 'Thanh toán thất bại'
                : 'Đang chờ xác nhận thanh toán'}
          </div>
          <div className={styles.paymentStatusText}>{paymentStatusMessage}</div>
          {bookingFlow === 'failed' && (
            <button
              type="button"
              className={styles.retryPaymentButton}
              onClick={() => {
                setBookingFlow('form');
                setPaymentStatusMessage('');
                setPaymentReference(generatePaymentReference());
                transferTrackingStartedRef.current = false;
                transferFinalizeStartedRef.current = false;
                transferStartedAtRef.current = null;
              }}
            >
              Thanh toán lại
            </button>
          )}
        </div>
      )}

      {/* Thông tin cá nhân */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <User className={styles.sectionIcon} />
          <h2 className={styles.sectionTitle}>Thông tin cá nhân</h2>
        </div>
        <div className={styles.grid}>
          <div className={styles.fullWidth}>
            <Label htmlFor="fullName">Họ và tên <span className={styles.required}>*</span></Label>
            <Input
              id="fullName"
              value={formData.fullName}
              onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
              placeholder="Nguyễn Văn A"
              required
            />
          </div>
          <div>
            <Label htmlFor="gender">Giới tính <span className={styles.required}>*</span></Label>
            <select
              id="gender"
              className={styles.selectInput}
              value={formData.gender}
              onChange={(e) => setFormData({ ...formData, gender: e.target.value })}
              required
            >
              <option value="">-- Chọn giới tính --</option>
              <option value="male">Nam</option>
              <option value="female">Nữ</option>
              <option value="other">Khác</option>
            </select>
          </div>
          <div>
            <Label htmlFor="dateOfBirth">Ngày sinh</Label>
            <Input
              id="dateOfBirth"
              type="date"
              value={formData.dateOfBirth}
              onChange={(e) => setFormData({ ...formData, dateOfBirth: e.target.value })}
            />
          </div>
          <div>
            <Label htmlFor="phone">Số điện thoại <span className={styles.required}>*</span></Label>
            <Input
              id="phone"
              type="tel"
              value={formData.phone}
              onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              placeholder="0123456789"
              required
            />
          </div>
          <div className={styles.fullWidth}>
            <Label htmlFor="hometown">Quê quán</Label>
            <Input
              id="hometown"
              value={formData.hometown}
              onChange={(e) => setFormData({ ...formData, hometown: e.target.value })}
              placeholder="Thành phố, Tỉnh"
            />
          </div>
        </div>
        <p className={styles.mutedNote}>
          Thông tin cá nhân được lấy từ hồ sơ bệnh nhân. Bạn có thể chỉnh sửa nếu cần.
        </p>
      </div>

      {/* Giấy tờ tùy thân */}
      <div className={styles.sectionDivider}>
        <div className={styles.sectionHeader}>
          <CreditCard className={styles.sectionIcon} />
          <h2 className={styles.sectionTitle}>Giấy tờ tùy thân</h2>
        </div>
        <div className={styles.grid}>
          <div>
            <Label htmlFor="idNumber">Số CCCD/CMND</Label>
            <Input
              id="idNumber"
              value={formData.idNumber}
              onChange={(e) => setFormData({ ...formData, idNumber: e.target.value })}
              placeholder="001234567890"
            />
          </div>
          <div>
            <Label htmlFor="insuranceNumber">Số thẻ BHYT</Label>
            <Input
              id="insuranceNumber"
              value={formData.insuranceNumber}
              onChange={(e) => setFormData({ ...formData, insuranceNumber: e.target.value })}
              placeholder="DN1234567890123"
            />
          </div>
        </div>
      </div>

      {/* Lý do khám */}
      <div className={styles.sectionDivider}>
        <div className={styles.sectionHeader}>
          <FileText className={styles.sectionIcon} />
          <h2 className={styles.sectionTitle}>Thông tin khám bệnh</h2>
        </div>
        <div className={styles.fieldGroup}>
          <Label htmlFor="appointmentTime" className={styles.fieldLabel}>
            Thời gian hẹn <span className={styles.required}>*</span>
          </Label>
          <Input
            id="appointmentTime"
            type="datetime-local"
            value={formData.appointmentTime}
            onChange={(e) => setFormData({ ...formData, appointmentTime: e.target.value })}
            required
          />
        </div>
        <div className={styles.fieldGroup}>
          <Label className={styles.fieldLabel}>
            Lý do khám / Triệu chứng <span className={styles.required}>*</span>
          </Label>
          <div className={styles.symptomCard}>
            <div className={styles.symptomHeader}>
              <div className={styles.symptomTitleBlock}>
                <span className={styles.symptomTitle}>Chọn triệu chứng có sẵn hoặc nhập mô tả thêm</span>
                <span className={styles.symptomHint}>Chọn nhanh để tiết kiệm thời gian, rồi bổ sung chi tiết nếu cần.</span>
              </div>
              <div className={styles.symptomMeta}>
                <span className={styles.symptomBadge}>{selectedSymptomsCount} đã chọn</span>
                {selectedSymptomsCount > 0 && (
                  <button
                    type="button"
                    className={styles.symptomClearBtn}
                    onClick={() => {
                      setSelectedSymptomIds([]);
                      setFormData((prev) => ({
                        ...prev,
                        reason: removeSymptomTokens(prev.reason, selectedSymptomNames),
                      }));
                    }}
                  >
                    Bỏ chọn tất cả
                  </button>
                )}
              </div>
            </div>

            <div className={styles.symptomRow}>
              <div className={styles.symptomCategory}>
                <Label htmlFor="symptomCategory">Nhóm triệu chứng</Label>
                <select
                  id="symptomCategory"
                  className={styles.selectInput}
                  value={selectedCategoryId ?? ''}
                  onChange={(e) => setSelectedCategoryId(Number(e.target.value) || null)}
                >
                  {categories.length === 0 && <option value="">-- Chọn nhóm --</option>}
                  {categories.map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div className={styles.symptomList}>
                {symptoms.map((s) => (
                  <label key={s.id} className={`${styles.symptomChip} ${selectedSymptomIds.includes(s.id) ? styles.symptomChipActive : ""}`}>
                    <input
                      type="checkbox"
                      checked={selectedSymptomIds.includes(s.id)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedSymptomIds((prev) => [...prev, s.id]);
                        } else {
                          setSelectedSymptomIds((prev) => prev.filter((x) => x !== s.id));
                          setFormData((prev) => ({
                            ...prev,
                            reason: removeSymptomTokens(prev.reason, [s.symptomName]),
                          }));
                        }
                      }}
                    />
                      <span className={styles.symptomChipSpan}>{s.symptomName}</span>
                  </label>
                ))}
              </div>
            </div>

            <div className={styles.reasonInputBlock}>
              <Textarea
                id="reason"
                className={styles.reasonTextarea}
                value={reasonInputValue}
                onChange={(e) => {
                  const nextValue = e.target.value;
                  const nextReason = nextValue.startsWith(reasonPrefix)
                    ? nextValue.slice(reasonPrefix.length)
                    : nextValue;
                  setFormData({ ...formData, reason: nextReason });
                }}
                placeholder="Mô tả triệu chứng (nếu không có trong danh sách hoặc cần mô tả thêm)..."
                rows={4}
              />
              {symptomSuggestions.length > 0 && (
                <div className={styles.symptomSuggestions}>
                  <div className={styles.symptomSuggestionsTitle}>Gợi ý triệu chứng phù hợp</div>
                  <div className={styles.symptomSuggestionsList}>
                    {symptomSuggestions.map((suggestion) => (
                      <button
                        key={suggestion.id}
                        type="button"
                        className={styles.symptomSuggestionChip}
                        onClick={() => {
                          setSelectedSymptomIds((prev) => [...prev, suggestion.id]);
                          setFormData((prev) => {
                            const currentValue = prev.reason || '';
                            const nextReason = replaceLastSymptomToken(currentValue, suggestion.symptomName);
                            return { ...prev, reason: nextReason };
                          });
                        }}
                      >
                        {suggestion.symptomName}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
            <div className={styles.paymentSection}>
              <div className={styles.servicePreview}>
                <div className={styles.servicePreviewTitle}>Dịch vụ dự kiến</div>
                {estimatedServices.length > 0 ? (
                  <ul className={styles.serviceList}>
                    {estimatedServices.map((service) => (
                      <li key={service.serviceId} className={styles.serviceItem}>
                        <span className={styles.serviceName}>{service.serviceName}</span>
                        <span className={styles.servicePrice}>{formatCurrency(service.lineTotal)}</span>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className={styles.serviceEmpty}>Chọn triệu chứng để xem các dịch vụ khám tương ứng.</p>
                )}
              </div>
              <div className={styles.paymentSummary}>
                <span className={styles.paymentSummaryLabel}>Tổng tiền tạm tính</span>
                <span className={styles.paymentSummaryValue}>
                  {estimatingFee ? "Đang tính..." : formatCurrency(estimatedTotalFee)}
                </span>
              </div>
              <div className={styles.paymentRow}>
                <div className={styles.paymentField}>
                  <Label htmlFor="paymentMethod">Phương thức thanh toán</Label>
                  <select
                    id="paymentMethod"
                    className={styles.selectInput}
                    value={paymentMethod}
                    onChange={(e) => setPaymentMethod(e.target.value as "TIEN_MAT" | "CHUYEN_KHOAN")}
                  >
                    <option value="CHUYEN_KHOAN">Chuyển khoản</option>
                    <option value="TIEN_MAT">Tiền mặt</option>
                  </select>
                </div>
                <p className={styles.paymentNote}>
                  Phí ước tính được tính từ các dịch vụ gắn với triệu chứng đã chọn.
                </p>

                {paymentMethod === "CHUYEN_KHOAN" && transferRequested && (
                  <div className={styles.paymentQrBox}>
                    <p className={styles.paymentQrTitle}>Quét mã QR để thanh toán</p>
                    {qrReady ? (
                      <>
                        <img src={transferQrUrl} alt="QR thanh toán đặt lịch" className={styles.paymentQrImage} />
                        <p className={styles.paymentQrAmount}>
                          Số tiền tạm tính: {estimatedTransferAmount.toLocaleString("vi-VN")}đ
                        </p>
                        <p className={styles.paymentQrMeta}>{bankName}</p>
                        <p className={styles.paymentQrMeta}>{bankAccount}</p>
                        <p className={styles.paymentQrMeta}>Mã tham chiếu: {paymentReference || 'đang tạo...'}</p>
                        <PaymentRealtimeListener
                          paymentCode={normalizedPaymentCode}
                          enabled={transferRequested && bookingFlow === 'waiting-transfer' && Boolean(normalizedPaymentCode)}
                          successMessage="Tuyệt vời! Thanh toán đã được xác nhận tự động."
                          waitingMessage="Hệ thống đang chờ nhận thanh toán..."
                          onPaymentSuccess={async () => {
                            if (!transferFinalizeStartedRef.current) {
                              transferFinalizeStartedRef.current = true;
                              setPaymentStatusMessage('Đã nộp tiền thành công. Đang tự động đặt lịch...');
                              await createAppointment('after-payment');
                            }
                          }}
                        />
                      </>
                    ) : bankConfigLoading ? (
                      <p className={styles.paymentQrError}>Đang tải cấu hình ngân hàng từ hệ thống...</p>
                    ) : (
                      <p className={styles.paymentQrError}>
                        Không lấy được cấu hình QR từ hệ thống. Vui lòng thử lại sau.
                      </p>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className={styles.actions}>
        <Button type="submit" className={styles.submitBtn} size="lg" disabled={submitting || bookingFlow === 'waiting-transfer'}>
          {submitting ? <Loader2 className="w-5 h-5 mr-2 animate-spin" /> : <Calendar className="w-5 h-5 mr-2" />}
          {submitting
            ? "Đang gửi..."
            : paymentMethod === 'CHUYEN_KHOAN'
              ? bookingFlow === 'waiting-transfer'
                ? 'Đang chờ thanh toán'
                : 'Lịch sẽ tự tạo khi đủ thông tin'
              : 'Đặt lịch khám'}
        </Button>
      </div>
    </form>
  );
}