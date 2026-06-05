# Tai lieu hoc nhanh FE-BE (ban tom tat)

Muc tieu tai lieu:

- Nhin nhanh man hinh Frontend dang dung file nao.
- Nhin nhanh Endpoint dang dung DTO nao.
- Hieu luong du lieu Controller -> Service -> Repository theo module.

## 1) Frontend: man hinh nao dung file nao

### 1.1 Public + Auth

| Route                  | Page file                                       | Feature render                                  | Service chinh |
| ---------------------- | ----------------------------------------------- | ----------------------------------------------- | ------------- |
| /                      | Frontend/src/app/page.tsx                       | features/landingpage/landingpage.tsx            | -             |
| /(auth)/signin         | Frontend/src/app/(auth)/signin/page.tsx         | features/auth/sign-in/sign-in-form.tsx          | authService   |
| /(auth)/signup         | Frontend/src/app/(auth)/signup/page.tsx         | features/auth/sign-up/sign-up-form.tsx          | authService   |
| /(auth)/forgotpassword | Frontend/src/app/(auth)/forgotpassword/page.tsx | features/auth/forgotpassword/forgotpassword.tsx | authService   |

### 1.2 Patient

| Route                       | Page file                                            | Feature render                                       | Service chinh  |
| --------------------------- | ---------------------------------------------------- | ---------------------------------------------------- | -------------- |
| /(patients)/dashboard       | Frontend/src/app/(patients)/dashboard/page.tsx       | features/patient/dashboard/dashboard.tsx             | patientService |
| /(patients)/booking         | Frontend/src/app/(patients)/booking/page.tsx         | features/patient/booking/booking-page.tsx            | patientService |
| /(patients)/appointments    | Frontend/src/app/(patients)/appointments/page.tsx    | features/patient/appointments/appointments.tsx       | patientService |
| /(patients)/invoices        | Frontend/src/app/(patients)/invoices/page.tsx        | features/patient/invoices/invoices.tsx               | patientService |
| /(patients)/patient-history | Frontend/src/app/(patients)/patient-history/page.tsx | features/patient/patient-history/patient-history.tsx | patientService |

### 1.3 Admin

| Route            | Page file                                 | Feature render                                     | Service chinh |
| ---------------- | ----------------------------------------- | -------------------------------------------------- | ------------- |
| /admin           | Frontend/src/app/admin/page.tsx           | features/admin/components/reports.tsx              | adminService  |
| /admin/reports   | Frontend/src/app/admin/reports/page.tsx   | features/admin/components/reports.tsx              | adminService  |
| /admin/users     | Frontend/src/app/admin/users/page.tsx     | features/admin/components/user-management.tsx      | adminService  |
| /admin/rooms     | Frontend/src/app/admin/rooms/page.tsx     | features/admin/components/rooms-management.tsx     | adminService  |
| /admin/medicines | Frontend/src/app/admin/medicines/page.tsx | features/admin/components/medicines-management.tsx | adminService  |
| /admin/services  | Frontend/src/app/admin/services/page.tsx  | features/admin/components/service-management.tsx   | adminService  |

### 1.4 Receptionist

| Route                   | Page file                                        | Feature render                              | Service chinh       |
| ----------------------- | ------------------------------------------------ | ------------------------------------------- | ------------------- |
| /receptionist           | Frontend/src/app/receptionist/page.tsx           | features/receptionist/receptionist-page.tsx | receptionistService |
| /receptionist/pending   | Frontend/src/app/receptionist/pending/page.tsx   | features/receptionist/receptionist-page.tsx | receptionistService |
| /receptionist/confirmed | Frontend/src/app/receptionist/confirmed/page.tsx | features/receptionist/receptionist-page.tsx | receptionistService |

### 1.5 Doctor + Cashier

| Route            | Page file                                 | Feature render                       | Service chinh  |
| ---------------- | ----------------------------------------- | ------------------------------------ | -------------- |
| /doctor          | Frontend/src/app/doctor/page.tsx          | features/doctor/doctor-page.tsx      | doctorService  |
| /cashier         | Frontend/src/app/cashier/page.tsx         | features/cashier/cashier-page.tsx    | cashierService |
| /cashier/history | Frontend/src/app/cashier/history/page.tsx | features/cashier/invoice-history.tsx | cashierService |

Ghi nho nhanh luong FE:

1. Page route -> feature component.
2. Feature component -> services/\*.ts.
3. services/\*.ts -> axios instance trong services/api.ts -> Backend endpoint.

## 2) Backend: Endpoint dung DTO nao

Chu thich:

- Req DTO = DTO trong @RequestBody.
- Res DTO = DTO tra ve.
- Neu tra thang Entity thi ghi ten Entity.

### 2.1 AuthController (/api/auth)

| Method + Endpoint                | Req DTO                        | Res DTO      |
| -------------------------------- | ------------------------------ | ------------ |
| POST /login                      | LoginRequest                   | AuthResponse |
| POST /register/patient           | PatientRegisterRequest         | AuthResponse |
| POST /refresh                    | RefreshTokenRequest            | AuthResponse |
| POST /logout                     | RefreshTokenRequest            | String       |
| POST /forgot-password/send-otp   | ForgotPasswordRequest          | String       |
| POST /forgot-password/verify-otp | VerifyForgotPasswordOtpRequest | String       |
| POST /forgot-password/reset      | ResetPasswordWithOtpRequest    | String       |

### 2.2 AdminController (/api/admin)

| Method + Endpoint                 | Req DTO                               | Res DTO                           |
| --------------------------------- | ------------------------------------- | --------------------------------- |
| GET /dashboard                    | -                                     | DashboardResponse                 |
| GET /revenue-report               | query params                          | AdminRevenueReportResponse        |
| GET /revenue-report/export        | query params                          | byte[] file                       |
| GET /users                        | -                                     | List<AdminUserResponse>           |
| POST /users                       | AdminCreateUserRequest                | AdminUserResponse                 |
| PUT /users/{userId}               | AdminUpdateUserRequest                | AdminUserResponse                 |
| DELETE /users/{userId}            | -                                     | void                              |
| GET /rooms                        | -                                     | List<AdminRoomResponse>           |
| POST /rooms                       | AdminRoomCreateRequest                | AdminRoomResponse                 |
| PUT /rooms/{roomId}               | AdminRoomUpdateRequest                | AdminRoomResponse                 |
| PUT /rooms/{roomId}/assign-doctor | AdminRoomAssignDoctorRequest          | AdminRoomResponse                 |
| GET /medicines                    | -                                     | List<AdminMedicineResponse>       |
| POST /medicines                   | AdminMedicineCreateRequest            | AdminMedicineResponse             |
| PUT /medicines/{medicineId}       | AdminMedicineUpdateRequest            | AdminMedicineResponse             |
| DELETE /medicines/{medicineId}    | -                                     | void                              |
| GET /services                     | -                                     | List<AdminMedicalServiceResponse> |
| POST /services                    | AdminMedicalServiceCreateRequest      | AdminMedicalServiceResponse       |
| PUT /services/{serviceId}/price   | AdminMedicalServiceUpdatePriceRequest | AdminMedicalServiceResponse       |
| DELETE /services/{serviceId}      | -                                     | void                              |

### 2.3 PatientController (/api/patient)

| Method + Endpoint                        | Req DTO                   | Res DTO                                       |
| ---------------------------------------- | ------------------------- | --------------------------------------------- |
| GET /profile                             | query patientId(optional) | PatientPrefillResponse                        |
| POST /appointments                       | PatientAppointmentRequest | Appointment (Entity)                          |
| GET /appointments                        | -                         | List<Appointment> (Entity)                    |
| PUT /appointments/{appointmentId}/cancel | -                         | Appointment (Entity)                          |
| GET /medical-records                     | -                         | List<PatientMedicalRecordHistoryItemResponse> |
| GET /medical-records/{medicalRecordId}   | -                         | PatientMedicalRecordDetailResponse            |

### 2.4 AppointmentController (/api/appointments)

| Method + Endpoint                  | Req DTO                        | Res DTO                    |
| ---------------------------------- | ------------------------------ | -------------------------- |
| GET /                              | -                              | List<Appointment> (Entity) |
| GET /waiting-assignment            | -                              | List<Appointment> (Entity) |
| POST /                             | AppointmentRequest             | Appointment (Entity)       |
| PUT /{appointmentId}/assign-doctor | AppointmentAssignDoctorRequest | Appointment (Entity)       |

### 2.5 ReceptionistController (/api/receptionist)

| Method + Endpoint                                | Req DTO                                | Res DTO                                |
| ------------------------------------------------ | -------------------------------------- | -------------------------------------- |
| GET /appointments/today                          | -                                      | List<Appointment> (Entity)             |
| GET /appointments/from-booking                   | -                                      | List<Appointment> (Entity)             |
| GET /doctors/by-specialty                        | query specialty(optional)              | List<ReceptionistDoctorOptionResponse> |
| PUT /appointments/{appointmentId}/approve        | ReceptionistApproveRequest             | Appointment (Entity)                   |
| PUT /appointments/{appointmentId}/assign-doctor  | ReceptionistApproveRequest             | Appointment (Entity)                   |
| GET /appointments/waiting                        | query status(optional)                 | List<Appointment> (Entity)             |
| PUT /appointments/{appointmentId}/waiting-status | ReceptionistWaitingStatusUpdateRequest | Appointment (Entity)                   |
| PUT /appointments/{appointmentId}/cancel         | ReceptionistCancelAppointmentRequest   | Appointment (Entity)                   |

### 2.6 DoctorController (/api/doctors)

| Method + Endpoint                                                   | Req DTO                 | Res DTO                            |
| ------------------------------------------------------------------- | ----------------------- | ---------------------------------- |
| GET /                                                               | -                       | List<DoctorResponse>               |
| GET /me/waiting-patients                                            | query date(optional)    | List<Appointment> (Entity)         |
| GET /me/completed-patients                                          | query date(optional)    | List<Appointment> (Entity)         |
| PUT /{doctorId}/clinic-room                                         | DoctorClinicRoomRequest | DoctorResponse                     |
| GET /appointments/{appointmentId}/patient-history                   | -                       | DoctorPatientHistoryResponse       |
| GET /appointments/{appointmentId}/patient-history/{medicalRecordId} | -                       | DoctorPatientHistoryDetailResponse |

### 2.7 MedicalRecordController (/api/medical-records)

| Method + Endpoint                                                 | Req DTO                                  | Res DTO                             |
| ----------------------------------------------------------------- | ---------------------------------------- | ----------------------------------- |
| GET /                                                             | -                                        | List<MedicalRecord> (Entity)        |
| GET /{id}                                                         | -                                        | MedicalRecord (Entity)              |
| GET /appointment/{appointmentId}                                  | -                                        | MedicalRecord (Entity)              |
| POST /                                                            | MedicalRecord body + query appointmentId | MedicalRecord (Entity)              |
| POST /doctor                                                      | CreateMedicalRecordRequest               | MedicalRecord (Entity)              |
| POST /{medicalRecordId}/prescription-details                      | AddPrescriptionDetailRequest             | PrescriptionDetail (Entity)         |
| GET /{medicalRecordId}/prescription-details                       | -                                        | List<PrescriptionDetail> (Entity)   |
| POST /{medicalRecordId}/service-results                           | UpsertMedicalRecordServiceResultRequest  | MedicalRecordServiceDetail (Entity) |
| GET /{medicalRecordId}/prescription-workspace                     | -                                        | PrescriptionWorkspaceResponse       |
| GET /{medicalRecordId}/medicine-catalog                           | query group(optional)                    | PrescriptionMedicineCatalogResponse |
| POST /{medicalRecordId}/prescription-details/quick-add            | QuickAddPrescriptionMedicineRequest      | PrescriptionWorkspaceResponse       |
| PUT /{medicalRecordId}/prescription-details/{medicineId}          | UpdatePrescriptionDetailRequest          | PrescriptionDetail (Entity)         |
| PUT /{medicalRecordId}/prescription-details/{medicineId}/autosave | UpdatePrescriptionDetailRequest          | PrescriptionAutosaveResponse        |
| DELETE /{medicalRecordId}/prescription-details/{medicineId}       | -                                        | PrescriptionWorkspaceResponse       |
| POST /{medicalRecordId}/prescription-save                         | -                                        | PrescriptionWorkspaceResponse       |
| PUT /{medicalRecordId}/complete                                   | -                                        | MedicalRecord (Entity)              |
| PUT /{id}                                                         | MedicalRecord body                       | MedicalRecord (Entity)              |
| DELETE /{id}                                                      | -                                        | void                                |

### 2.8 CashierController (/api/cashier)

| Method + Endpoint                          | Req DTO                            | Res DTO                                 |
| ------------------------------------------ | ---------------------------------- | --------------------------------------- |
| GET /payment-queue                         | query keyword(optional)            | List<CashierWaitingPaymentItemResponse> |
| GET /payment-records/search                | query keyword                      | CashierPaymentRecordDetailResponse      |
| GET /invoices/{invoiceId}/paid-detail      | -                                  | CashierPaymentRecordDetailResponse      |
| GET /transaction-history                   | query time/paymentMethod(optional) | CashierTransactionHistoryResponse       |
| GET /invoices/by-medical-record            | query medicalRecordId              | Invoice (Entity)                        |
| POST /invoices/aggregate                   | query medicalRecordId              | Invoice (Entity)                        |
| PUT /invoices/{invoiceId}/confirm-payment  | -                                  | Invoice (Entity)                        |
| POST /invoices/{invoiceId}/process-payment | CashierProcessPaymentRequest       | CashierProcessPaymentResponse           |
| GET /invoices/{invoiceId}/receipt          | -                                  | CashierReceiptResponse                  |
| POST /invoices/{invoiceId}/print-receipt   | query printerName(optional)        | CashierPrintReceiptResponse             |
| GET /invoices/{invoiceId}/export-pdf       | -                                  | byte[] PDF                              |

### 2.9 ChatbotController (/api/chatbot)

| Method + Endpoint | Req DTO           | Res DTO                          |
| ----------------- | ----------------- | -------------------------------- |
| POST /ask         | ChatbotAskRequest | ChatbotAskResponse               |
| GET /history      | -                 | List<ChatbotHistoryItemResponse> |
| DELETE /history   | -                 | String                           |

## 3) Luong du lieu Controller -> Service -> Repository (theo module)

### 3.1 Auth module

- Controller: AuthController
- Service: AuthService (phoi hop JwtService, RefreshTokenService, NotificationService)
- Repository chinh: UserRepository, PatientRepository
- Muc dich: dang nhap, dang ky benh nhan, refresh/logout, forgot-password OTP.

### 3.2 Admin module

- Controller: AdminController
- Service: AdminService
- Repository chinh:
  - UserRepository, RoomRepository, MedicineRepository, MedicalServiceRepository
  - AppointmentRepository, PatientRepository, MedicalRecordRepository, InvoiceRepository
  - MedicalRecordServiceDetailRepository, PrescriptionDetailRepository
- Muc dich: dashboard, report, CRUD users/rooms/medicines/services.

### 3.3 Patient module

- Controller: PatientController
- Service:
  - AppointmentService (profile prefill, dat/huy lich, danh sach lich)
  - PatientService (lich su benh an, chi tiet benh an)
- Repository chinh:
  - AppointmentService: AppointmentRepository, PatientRepository, UserRepository
  - PatientService: PatientRepository, UserRepository, MedicalRecordRepository, PrescriptionDetailRepository

### 3.4 Receptionist module

- Controller: ReceptionistController
- Service: ReceptionistService
- Repository chinh: AppointmentRepository, UserRepository, RoomRepository
- Service phu tro: NotificationService
- Muc dich: approve/assign/cancel appointment, waiting queue.

### 3.5 Doctor module

- Controller: DoctorController
- Service:
  - DoctorService (doctor list, waiting patients, room)
  - MedicalRecordService (history summary/detail)
- Repository chinh:
  - DoctorService: UserRepository, AppointmentRepository, RoomRepository
  - MedicalRecordService: MedicalRecordRepository, AppointmentRepository, UserRepository,
    MedicineRepository, MedicalServiceRepository, PrescriptionDetailRepository,
    MedicalRecordServiceDetailRepository, RoomRepository
- Service phu tro: PatientService, InvoiceService

### 3.6 MedicalRecord module

- Controller: MedicalRecordController
- Service: MedicalRecordService
- Repository chinh:
  - MedicalRecordRepository, AppointmentRepository, UserRepository
  - MedicineRepository, MedicalServiceRepository
  - PrescriptionDetailRepository, MedicalRecordServiceDetailRepository, RoomRepository
- Service phu tro: PatientService, InvoiceService
- Muc dich: tao benh an, workspace ke don, autosave, complete, service results.

### 3.7 Cashier module

- Controller: CashierController
- Service: InvoiceService
- Repository chinh:
  - InvoiceRepository, MedicalRecordRepository
  - MedicalRecordServiceDetailRepository, PrescriptionDetailRepository, MedicineRepository
- Muc dich: queue thanh toan, process payment, receipt, export PDF, history.

### 3.8 Chatbot module

- Controller: ChatbotController
- Service: GeminiChatbotService
- Repository chinh:
  - ChatbotMessageRepository
  - UserRepository, PatientRepository
  - AppointmentRepository, MedicalRecordRepository, InvoiceRepository
  - MedicalServiceRepository, MedicineRepository
- Muc dich: hoi dap AI, luu/lay/xoa lich su hoi thoai.

## 4) Ghi chu hoc nhanh

- Hien tai nhieu endpoint dang tra thang Entity (Appointment, MedicalRecord, Invoice).
  Voi he thong lon hon, nen uu tien tra ve DTO de:
  1. giam coupling voi DB schema,
  2. an field nhay cam,
  3. de version API.
