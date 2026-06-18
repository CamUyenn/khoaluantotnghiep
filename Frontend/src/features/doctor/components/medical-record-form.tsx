import { useId, useMemo, useState } from "react";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { DiagnosisTemplateResponse } from "@/services/doctorService";
import styles from "@/styles/common.module.css";
import type { MedicalRecordInput } from "@/types/doctor.type";

const formatDate = (value?: string | null) => {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : date.toLocaleDateString("vi-VN");
};

interface MedicalRecordFormProps {
  medicalRecord: MedicalRecordInput;
  diagnosisOptions: DiagnosisTemplateResponse[];
  selectedDiagnosisId: number | null;
  diagnosisLoading: boolean;
  historyRows: any[];
  historyDetail: any;
  historyLoading: boolean;
  historyDetailLoading: boolean;
  onDiagnosisSelect: (diagnosisId: number) => void;
  onChange: (field: string, value: string) => void;
  onViewHistory: (historyId: number) => void;
  mode?: "full" | "history";
}

export function MedicalRecordForm(props: MedicalRecordFormProps) {
  const idBase = useId();
  const selectId = `${idBase}-diagnosis`;
  const textareaId = `${idBase}-doctor-advice`;
  const [historyOpen, setHistoryOpen] = useState(false);
  const activeHistoryId = props.historyDetail?.medicalRecordId ?? null;
  const detailById = useMemo(() => {
    if (!props.historyDetail) {
      return new Map<number, any>();
    }
    return new Map<number, any>([[props.historyDetail.medicalRecordId, props.historyDetail]]);
  }, [props.historyDetail]);
  return (
    <>
      <div className={`${styles.flexBetween} ${styles.mb2}`}>
        <h4 className={styles.titleSmall}>Hồ sơ khám bệnh</h4>
        <button
          type="button"
          className={`${styles.button} ${styles.outline}`}
          onClick={() => setHistoryOpen((prev) => !prev)}
        >
          {historyOpen ? "Ẩn hồ sơ bệnh án" : "Xem hồ sơ bệnh án"}
        </button>
      </div>

      <div className={styles.formGroup}>
        <Label className={styles.label} htmlFor={selectId}>Chẩn đoán *</Label>
        <div style={{ position: "relative" }}>
          <input
            id={selectId}
            name="diagnosis"
            className={styles.input}
            value={props.medicalRecord.diagnosis ?? ""}
            onChange={(e) => {
              const v = e.target.value;
              props.onChange("diagnosis", v);
            }}
            onFocus={() => { /* show suggestions handled below */ }}
            placeholder={props.diagnosisLoading ? "Đang tải chẩn đoán..." : "Nhập chẩn đoán hoặc chọn gợi ý..."}
            disabled={props.diagnosisLoading}
          />

          {/* Simple client-side suggestion list (filter-as-you-type) */}
          {props.diagnosisOptions.length > 0 && (props.medicalRecord.diagnosis ?? "").trim().length > 0 && (
            <ul style={{ position: "absolute", zIndex: 40, left: 0, right: 0, background: "#fff", border: "1px solid #e5e7eb", maxHeight: 220, overflow: "auto", marginTop: 6, padding: 0, listStyle: "none" }}>
              {props.diagnosisOptions
                .filter((item) => item.diagnosisName.toLowerCase().includes((props.medicalRecord.diagnosis || "").toLowerCase()))
                .slice(0, 20)
                .map((item) => (
                  <li
                    key={item.id}
                    style={{ padding: "0.5rem 0.75rem", cursor: "pointer" }}
                    onMouseDown={(e) => { e.preventDefault(); }}
                    onClick={() => {
                      props.onChange("diagnosis", item.diagnosisName);
                      props.onDiagnosisSelect(item.id);
                    }}
                  >
                    {item.diagnosisName}
                  </li>
                ))}
            </ul>
          )}

        </div>
        {props.diagnosisOptions.length === 0 && !props.diagnosisLoading && (
          <p className={styles.textSmall} style={{ marginTop: "0.5rem" }}>
            Chuyên khoa này chưa có danh sách chẩn đoán mẫu.
          </p>
        )}
      </div>

      <div className={styles.formGroup}>
        <Label className={styles.label} htmlFor={textareaId}>Phương pháp điều trị / Lời khuyên *</Label>
        <Textarea
          id={textareaId}
          name="doctor_advice"
          className={styles.input}
          rows={3}
          value={props.medicalRecord.doctor_advice}
          onChange={(e) => props.onChange("doctor_advice", e.target.value)}
          placeholder="Lời khuyên sẽ tự điền từ chẩn đoán mẫu và có thể chỉnh sửa..."
        />
      </div>

      {historyOpen && (
        <div className={styles.panelBorder}>
          <div className={`${styles.flexBetween} ${styles.mb2}`}>
            <h4 className={styles.titleSmall}>Hồ sơ bệnh án cũ</h4>
            <span className={styles.textSmall}>Đối chiếu tiền sử khám</span>
          </div>
          
          {props.historyLoading && <p className={styles.textSmall}>Đang tải lịch sử bệnh án...</p>}
          
          {!props.historyLoading && props.historyRows.length > 0 && (
            <div className={styles.rowStack}>
              {props.historyRows.map((row) => {
                const detail = detailById.get(row.medicalRecordId);
                const isActive = activeHistoryId === row.medicalRecordId;
                return (
                  <div key={row.medicalRecordId} className={styles.itemCard}>
                    <div className={styles.flexBetween}>
                      <div>
                        <p className={styles.textSmall}><strong>Ngày khám:</strong> {formatDate(row.appointmentTime)}</p>
                        <p className={styles.textSmall}><strong>Chẩn đoán cũ:</strong> {row.oldDiagnosis || "-"}</p>
                      </div>
                      <button
                        type="button"
                        className={`${styles.button} ${styles.outline}`}
                        onClick={() => props.onViewHistory(row.medicalRecordId)}
                        disabled={props.historyDetailLoading && isActive}
                      >
                        {isActive ? "Đang xem" : "Xem chi tiết"}
                      </button>
                    </div>

                    {props.historyDetailLoading && isActive && (
                      <p className={styles.textSmall} style={{ marginTop: "0.5rem" }}>Đang tải chi tiết hồ sơ...</p>
                    )}
                    {detail && (
                      <div className={styles.historyDetailBox}>
                        <p className={styles.textSmall}><strong>Chẩn đoán:</strong> {detail.diagnosis || "-"}</p>
                        <p className={styles.textSmall}><strong>Lời dặn:</strong> {detail.doctorAdvice || "-"}</p>
                        {detail.prescriptions?.length > 0 ? (
                          <div className={styles.mt2}>
                            <p className={styles.textSmall}><strong>Thuốc đã kê:</strong></p>
                            <div className={styles.rowStack}>
                              {detail.prescriptions.map((item: any) => (
                                <div key={item.medicineId} className={styles.itemCard}>
                                  <p className={styles.textSmall}>{item.medicineName} - SL: {item.quantity}</p>
                                  <p className={styles.textSmall}>{item.usageInstructions || "-"}</p>
                                </div>
                              ))}
                            </div>
                          </div>
                        ) : (
                          <p className={styles.textSmall}>Không có thuốc.</p>
                        )}
                        {detail.services?.length > 0 && (
                          <div className={styles.mt2}>
                            <p className={styles.textSmall}><strong>Dịch vụ đã chỉ định:</strong></p>
                            <div className={styles.rowStack}>
                              {detail.services.map((item: any) => (
                                <div key={item.serviceId} className={styles.itemCard}>
                                  <p className={styles.textSmall}>{item.serviceName} - SL: {item.quantity}</p>
                                  <p className={styles.textSmall}>{item.resultNote || "-"}</p>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </>
  );
}