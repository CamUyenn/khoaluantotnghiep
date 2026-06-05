import { useMemo, useState } from "react";
import { PatientInfo } from "./patient-info";
import { MedicalRecordForm } from "./medical-record-form";
import { PrescriptionSection } from "./prescription-section";
import { DoctorMedicalService, type DiagnosisTemplateResponse } from "@/services/doctorService";
import type { Appointment, MedicalRecordInput, Medicine, PrescriptionItem, SelectedServiceItem } from "@/types/doctor.type";
import styles from "@/styles/common.module.css";

const formatDate = (value?: string | null) => {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : date.toLocaleDateString("vi-VN");
};

interface ExaminationModalProps {
  appointment: Appointment;
  saving: boolean;
  diagnosisLoading: boolean;
  diagnosisOptions: DiagnosisTemplateResponse[];
  selectedDiagnosisId: number | null;
  medicalRecord: MedicalRecordInput;
  prescriptions: PrescriptionItem[];
  availableMedicines: Medicine[];
  availableServices: DoctorMedicalService[];
  selectedServices: SelectedServiceItem[];
  historyRows: any[];
  historyDetail: any;
  historyLoading: boolean;
  historyDetailLoading: boolean;
  totalServiceCost: number;
  onClose: () => void;
  onComplete: () => void;
  onMedicalRecordChange: (field: string, value: string) => void;
  onDiagnosisSelect: (diagnosisId: number) => void;
  onAddMedicine: (medicine: Medicine) => void;
  onUpdatePrescription: (id: number, field: "quantity" | "usage_instructions", value: any) => void;
  onRemoveMedicine: (id: number) => void;
  onAddService: (service: DoctorMedicalService) => void;
  onUpdateService: (id: number, field: "quantity" | "actual_price" | "result_note", value: any) => void;
  onRemoveService: (id: number) => void;
  onViewHistoryDetail: (historyId: number) => void;
}

type DoctorModalTab = "diagnosis" | "services" | "records";

export function ExaminationModal(props: ExaminationModalProps) {
  const { appointment } = props;
  const [activeCategory, setActiveCategory] = useState<string>("Tất cả");
  const [activeTab, setActiveTab] = useState<DoctorModalTab>("diagnosis");

  const diagnosisCategories = useMemo(() => {
    const unique = Array.from(
      new Set(props.diagnosisOptions.map((item) => (item.categoryName || "Khác").trim() || "Khác"))
    );
    return ["Tất cả", ...unique];
  }, [props.diagnosisOptions]);

  const filteredDiagnosis = useMemo(() => {
    if (activeCategory === "Tất cả") {
      return props.diagnosisOptions;
    }
    return props.diagnosisOptions.filter(
      (item) => (item.categoryName || "Khác").trim() === activeCategory
    );
  }, [activeCategory, props.diagnosisOptions]);

  const selectedTemplate = useMemo(
    () => props.diagnosisOptions.find((item) => item.id === props.selectedDiagnosisId) ?? null,
    [props.diagnosisOptions, props.selectedDiagnosisId]
  );

  return (
    <div className={styles.modal} onClick={props.onClose}>
      <div className={`${styles.modalContent} ${styles.modalContentFull}`} onClick={(e) => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <h2>Khám bệnh - {appointment.patient.full_name}</h2>
        </div>

        <div className={styles.tabRow}>
          <button
            type="button"
            className={`${styles.tabButton} ${activeTab === "diagnosis" ? styles.tabButtonActive : ""}`}
            onClick={() => setActiveTab("diagnosis")}
          >
            Chẩn đoán &amp; Thuốc
          </button>
          <button
            type="button"
            className={`${styles.tabButton} ${activeTab === "services" ? styles.tabButtonActive : ""}`}
            onClick={() => setActiveTab("services")}
          >
            Dịch vụ
          </button>
          <button
            type="button"
            className={`${styles.tabButton} ${activeTab === "records" ? styles.tabButtonActive : ""}`}
            onClick={() => setActiveTab("records")}
          >
            Hồ sơ bệnh án
          </button>
        </div>

        {activeTab === "diagnosis" && (
          <div className={styles.doctorModalGrid}>
            <div className={styles.doctorModalColumn}>
              <PatientInfo 
                patient={appointment.patient} 
                symptoms={appointment.symptoms} 
                categoryName={appointment.category?.name ?? null}
              />

              <PrescriptionSection 
                prescriptions={props.prescriptions}
                availableMedicines={props.availableMedicines}
                onAddMedicine={props.onAddMedicine}
                onUpdatePrescription={props.onUpdatePrescription}
                onRemoveMedicine={props.onRemoveMedicine}
              />

              <div className={styles.summaryCard}>
                <p className={styles.textSmall}>Dịch vụ đã được thu ngân thanh toán trước.</p>
              </div>
            </div>

            <div className={styles.doctorModalColumn}>
              <div className={styles.panelBorder}>
                <div className={`${styles.flexBetween} ${styles.mb2}`}>
                  <h4 className={styles.titleSmall}>Gợi ý chẩn đoán mẫu</h4>
                  <span className={styles.textSmall}>Diagnosis Templates</span>
                </div>
                <div className={styles.categoryTabs}>
                  {diagnosisCategories.map((category) => (
                    <button
                      key={category}
                      type="button"
                      className={`${styles.categoryTab} ${activeCategory === category ? styles.categoryTabActive : ""}`}
                      onClick={() => setActiveCategory(category)}
                    >
                      {category}
                    </button>
                  ))}
                </div>

                <div className={styles.diagnosisTemplateScrollArea}>
                  <div className={styles.diagnosisTemplateGrid}>
                    {filteredDiagnosis.map((template) => (
                      <button
                        key={template.id}
                        type="button"
                        className={`${styles.templateCard} ${template.id === props.selectedDiagnosisId ? styles.templateCardActive : ""}`}
                        onClick={() => props.onDiagnosisSelect(template.id)}
                      >
                        <div className={styles.templateHeader}>
                          <span className={styles.templateTitle}>{template.diagnosisName}</span>
                          <span className={styles.templateCategory}>{template.categoryName || "Khác"}</span>
                        </div>
                        <p className={styles.templateAdvice}>
                          {template.defaultAdvice || "Chưa có lời khuyên mẫu"}
                        </p>
                      </button>
                    ))}
                    {filteredDiagnosis.length === 0 && (
                      <div className={styles.emptyBox}>Chưa có gợi ý chẩn đoán cho nhóm này.</div>
                    )}
                  </div>
                </div>

                {selectedTemplate && (
                  <div className={styles.templateDetail}>
                    <h5 className={styles.titleSmall}>Chẩn đoán &amp; lời khuyên</h5>
                    <p className={styles.textSmall}><strong>Chẩn đoán:</strong> {selectedTemplate.diagnosisName}</p>
                    <p className={styles.textSmall}><strong>Lời khuyên:</strong> {selectedTemplate.defaultAdvice || "Chưa có"}</p>
                    <div className={styles.mt2}>
                      <p className={styles.textSmall}><strong>Thuốc đã gợi ý:</strong></p>
                      {props.prescriptions.length > 0 ? (
                        <div className={styles.rowStack}>
                          {props.prescriptions.map((item) => (
                            <div key={item.medicine_id} className={styles.itemCard}>
                              <p className={styles.textSmall}>{item.medicine?.medicine_name} - SL: {item.quantity}</p>
                              <p className={styles.textSmall}>{item.usage_instructions || "-"}</p>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className={styles.textSmall}>Chưa có thuốc gợi ý.</p>
                      )}
                    </div>
                  </div>
                )}
              </div>

              <div className={styles.panelBorder}>
                <MedicalRecordForm 
                  medicalRecord={props.medicalRecord}
                  diagnosisLoading={props.diagnosisLoading}
                  diagnosisOptions={props.diagnosisOptions}
                  selectedDiagnosisId={props.selectedDiagnosisId}
                  historyRows={props.historyRows}
                  historyDetail={props.historyDetail}
                  historyLoading={props.historyLoading}
                  historyDetailLoading={props.historyDetailLoading}
                  onDiagnosisSelect={props.onDiagnosisSelect}
                  onChange={props.onMedicalRecordChange}
                  onViewHistory={props.onViewHistoryDetail}
                  mode="full"
                />
              </div>
            </div>
          </div>
        )}

        {activeTab === "services" && (
          <div className={styles.doctorModalGrid}>
            <div className={styles.doctorModalColumn}>
              <PatientInfo 
                patient={appointment.patient} 
                symptoms={appointment.symptoms} 
                categoryName={appointment.category?.name ?? null}
              />

              <div className={styles.panelBorder}>
                <div className={`${styles.flexBetween} ${styles.mb2}`}>
                  <h4 className={styles.titleSmall}>Dịch vụ đã chọn</h4>
                </div>
                {props.selectedServices.length > 0 ? (
                  <div className={styles.rowStack}>
                    {props.selectedServices.map((item) => (
                      <div key={item.service_id} className={styles.itemCard}>
                        <div className={styles.flexBetween}>
                          <span className={styles.textMedium}>{item.service_name}</span>
                          <button
                            type="button"
                            className={`${styles.button} ${styles.outline} ${styles.textDanger}`}
                            onClick={() => props.onRemoveService(item.service_id)}
                          >
                            Xóa
                          </button>
                        </div>
                        <div className={`${styles.grid2} ${styles.mt2}`}>
                          <div>
                            <label className={`${styles.textSmall} ${styles.textMuted}`}>Số lượng</label>
                            <input
                              type="number"
                              className={`${styles.input} ${styles.compactInput}`}
                              min="1"
                              value={item.quantity}
                              onChange={(e) => props.onUpdateService(item.service_id, "quantity", Number(e.target.value) || 1)}
                            />
                          </div>
                          <div>
                            <label className={`${styles.textSmall} ${styles.textMuted}`}>Đơn giá</label>
                            <input
                              type="number"
                              className={`${styles.input} ${styles.compactInput}`}
                              min="0"
                              value={item.actual_price}
                              onChange={(e) => props.onUpdateService(item.service_id, "actual_price", Number(e.target.value) || 0)}
                            />
                          </div>
                        </div>
                        <div className={styles.mt2}>
                          <label className={`${styles.textSmall} ${styles.textMuted}`}>Ghi chú kết quả</label>
                          <textarea
                            className={styles.input}
                            rows={2}
                            value={item.result_note || ""}
                            onChange={(e) => props.onUpdateService(item.service_id, "result_note", e.target.value)}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className={styles.textSmall}>Chưa có dịch vụ nào được chọn.</p>
                )}
              </div>
            </div>

            <div className={styles.doctorModalColumn}>
              <div className={styles.panelBorder}>
                <div className={`${styles.flexBetween} ${styles.mb2}`}>
                  <h4 className={styles.titleSmall}>Danh sách dịch vụ</h4>
                  <span className={styles.textSmall}>Chọn thêm từ hệ thống</span>
                </div>
                <div className={styles.serviceGrid}>
                  {props.availableServices.map((service) => (
                    <button
                      key={service.id}
                      type="button"
                      className={styles.serviceItem}
                      onClick={() => props.onAddService(service)}
                    >
                      <p className={styles.textMedium}>{service.serviceName}</p>
                      <p className={`${styles.textSmall} ${styles.textMuted}`}>
                        {service.currentPrice.toLocaleString("vi-VN")}đ
                      </p>
                    </button>
                  ))}
                </div>
                {props.availableServices.length === 0 && (
                  <div className={styles.emptyBox}>Chưa có dịch vụ trong hệ thống.</div>
                )}
              </div>
            </div>
          </div>
        )}

        {activeTab === "records" && (
          <div className={styles.panelBorder}>
            <MedicalRecordForm 
              medicalRecord={props.medicalRecord}
              diagnosisLoading={props.diagnosisLoading}
              diagnosisOptions={props.diagnosisOptions}
              selectedDiagnosisId={props.selectedDiagnosisId}
              historyRows={props.historyRows}
              historyDetail={props.historyDetail}
              historyLoading={props.historyLoading}
              historyDetailLoading={props.historyDetailLoading}
              onDiagnosisSelect={props.onDiagnosisSelect}
              onChange={props.onMedicalRecordChange}
              onViewHistory={props.onViewHistoryDetail}
              mode="history"
            />
          </div>
        )}

          <div className={styles.actionRow}>
          <button className={`${styles.button} ${styles.primary} ${styles.actionButtonGrow}`} onClick={props.onComplete} disabled={props.saving}>
            Hoàn thành khám
          </button>
          <button className={`${styles.button} ${styles.outline} ${styles.actionButtonGrow}`} onClick={props.onClose} disabled={props.saving}>
            Hủy
          </button>
        </div>
      </div>
    </div>
  );
}