# TAI LIEU CHUC NANG TAT CA FILE CODE

Tai lieu nay mo ta lai cau truc va chuc nang cua code hien tai trong workspace `khoaluantotnghiep` (Backend + Frontend). Noi dung duoc viet lai theo trang thai source code dang co trong may local, da bo toan bo conflict marker cu.

## 1) Tong quan he thong

- Mo hinh: fullstack clinic management system.
- Backend: Spring Boot 3.5.14, Java 17, PostgreSQL, Redis, JWT, SMTP, OpenAPI, PDF export.
- Frontend: Next.js 16, React 19, TypeScript, Axios, Sonner, Recharts, lucide-react.
- Vai tro chinh: ADMIN, DOCTOR, RECEPTIONIST, CASHIER, PATIENT.
- Luong nghiep vu chinh:
  - Dang nhap, dang ky benh nhan, quen mat khau OTP.
  - Dat lich kham, le tan duyet lich va dieu phoi bac si.
  - Bac si kham benh, tao benh an, ke don, luu ket qua dich vu.
  - Thu ngan xu ly thanh toan, in bien lai, theo doi lich su giao dich.
    - Thanh toan chuyen khoan: nhan webhook SePay va job dinh ky kiem tra SePay API de xac nhan giao dich.
  - Benh nhan xem lich hen, hoa don, lich su benh an.
  - Chatbot AI Gemini phuc vu hoi dap thong tin phong kham.

## 2) Backend - Build va cau hinh

| File                                                                                | Chuc nang                                                                                      |
| ----------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| `Backend/pom.xml`                                                                   | Khai bao dependency Spring Boot, JPA, Security, Validation, Mail, Redis, JWT, PDFBox, OpenAPI. |
| `Backend/mvnw`, `Backend/mvnw.cmd`                                                  | Maven Wrapper.                                                                                 |
| `Backend/src/main/resources/application.properties`                                 | Cau hinh DB, Redis, JWT, CORS, SMTP, Gemini, logging, server port.                             |
| `Backend/src/main/resources/META-INF/additional-spring-configuration-metadata.json` | Metadata cho custom properties de IDE goi y.                                                   |

## 3) Backend - Khoi dong va cau hinh he thong

| File                                                                                | Chuc nang                                                          |
| ----------------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| `Backend/src/main/java/com/example/demo/DemoApplication.java`                       | Entry point khoi dong Spring Boot.                                 |
| `Backend/src/main/java/com/example/demo/config/OpenApiConfig.java`                  | Cau hinh Swagger/OpenAPI va security scheme.                       |
| `Backend/src/main/java/com/example/demo/config/SecurityConfig.java`                 | Cau hinh phan quyen endpoint, stateless session, JWT filter chain. |
| `Backend/src/main/java/com/example/demo/config/CustomAuthenticationEntryPoint.java` | Tra ve loi 401 nhat quan.                                          |
| `Backend/src/main/java/com/example/demo/config/CustomAccessDeniedHandler.java`      | Tra ve loi 403 nhat quan.                                          |
| `Backend/src/main/java/com/example/demo/config/GeminiConfig.java`                   | Cau hinh bean dung cho chatbot Gemini.                             |
| `Backend/src/main/java/com/example/demo/config/GeminiProperties.java`               | Map cau hinh Gemini tu application.properties.                     |
| `Backend/src/main/java/com/example/demo/config/SpecialtyRoomMappingConfig.java`     | Cau hinh mapping chuyen khoa - phong kham.                         |
| `Backend/src/main/java/com/example/demo/config/SchedulingConfig.java`               | Bat scheduling cho cac job dinh ky.                                |
| `Backend/src/main/java/com/example/demo/security/JwtFilter.java`                    | Doc Bearer token va nap SecurityContext.                           |
| `Backend/src/main/java/com/example/demo/exception/AppException.java`                | Custom exception cho nghiep vu.                                    |
| `Backend/src/main/java/com/example/demo/exception/GlobalExceptionHandler.java`      | Xu ly loi toan cuc cho API.                                        |

## 4) Backend - Seeder va du lieu ban dau

| File                                                                                    | Chuc nang                           |
| --------------------------------------------------------------------------------------- | ----------------------------------- |
| `Backend/src/main/java/com/example/demo/config/DiagnosisDataSeeder.java`                | Seed du lieu mau cho chuan doan.    |
| `Backend/src/main/java/com/example/demo/config/DiagnosisMedicineRuleSeeder.java`        | Seed rule chuan doan - thuoc.       |
| `Backend/src/main/java/com/example/demo/config/SymptomDataSeeder.java`                  | Seed du lieu trieu chung.           |
| `Backend/src/main/java/com/example/demo/config/seeder/DataSeeder.java`                  | Bootstrapping du lieu tong hop.     |
| `Backend/src/main/java/com/example/demo/config/seeder/AppSeedProperties.java`           | Cau hinh cho seeder.                |
| `Backend/src/main/java/com/example/demo/config/seeder/MedicalServiceSeeder.java`        | Seed dich vu y te.                  |
| `Backend/src/main/java/com/example/demo/config/seeder/MedicineSeeder.java`              | Seed danh muc thuoc.                |
| `Backend/src/main/java/com/example/demo/config/seeder/PatientSeeder.java`               | Seed tai khoan benh nhan mau.       |
| `Backend/src/main/java/com/example/demo/config/seeder/RoomSeeder.java`                  | Seed phong kham.                    |
| `Backend/src/main/java/com/example/demo/config/seeder/SeedUtils.java`                   | Ham tien ich cho seeder.            |
| `Backend/src/main/java/com/example/demo/config/seeder/SymptomServiceMappingSeeder.java` | Seed mapping trieu chung - dich vu. |
| `Backend/src/main/java/com/example/demo/config/seeder/SystemSettingSeeder.java`         | Seed cau hinh he thong.             |
| `Backend/src/main/java/com/example/demo/config/seeder/UserSeeder.java`                  | Seed nguoi dung mau.                |

## 5) Backend - Controller (API layer)

| File                                                                               | Chuc nang                                                                          |
| ---------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| `Backend/src/main/java/com/example/demo/controller/AuthController.java`            | Dang nhap, dang ky benh nhan, refresh token, logout, quen mat khau OTP.            |
| `Backend/src/main/java/com/example/demo/controller/AdminController.java`           | Dashboard, bao cao doanh thu, quan ly users, rooms, medicines, services, settings. |
| `Backend/src/main/java/com/example/demo/controller/AdminMasterDataController.java` | Quan ly du lieu master nhu trieu chung, benh/chuan doan, rule lien quan.           |
| `Backend/src/main/java/com/example/demo/controller/AppointmentController.java`     | Nghiep vu lich hen tong quat.                                                      |
| `Backend/src/main/java/com/example/demo/controller/ReceptionistController.java`    | Duyet/tu choi lich, hang doi, dieu phoi bac si va phong kham.                      |
| `Backend/src/main/java/com/example/demo/controller/DoctorController.java`          | Dashboard bac si, danh sach benh nhan cho kham, phong kham, lich su benh nhan.     |
| `Backend/src/main/java/com/example/demo/controller/MedicalRecordController.java`   | Tao va cap nhat benh an, don thuoc, ket qua dich vu, workspace ke don.             |
| `Backend/src/main/java/com/example/demo/controller/CashierController.java`         | Hang doi thanh toan, xu ly thanh toan, bien lai, lich su giao dich.                |
| `Backend/src/main/java/com/example/demo/controller/PatientController.java`         | Thong tin benh nhan, dat lich, lich hen, lich su benh an va hoa don.               |
| `Backend/src/main/java/com/example/demo/controller/ChatbotController.java`         | Hoi dap chatbot va lich su hoi thoai.                                              |
| `Backend/src/main/java/com/example/demo/controller/CatalogController.java`         | Tra danh muc dung chung cho frontend.                                              |
| `Backend/src/main/java/com/example/demo/controller/PaymentWebhookController.java`  | Nhan webhook SePay, ghi giao dich va cap nhat hoa don.                             |

## 6) Backend - Service layer

| File                                                                       | Chuc nang                                                     |
| -------------------------------------------------------------------------- | ------------------------------------------------------------- |
| `Backend/src/main/java/com/example/demo/service/AuthService.java`          | Login/register patient, OTP reset password, refresh/logout.   |
| `Backend/src/main/java/com/example/demo/service/RefreshTokenService.java`  | Quan ly refresh token tren Redis.                             |
| `Backend/src/main/java/com/example/demo/service/JwtService.java`           | Tao va parse JWT.                                             |
| `Backend/src/main/java/com/example/demo/service/NotificationService.java`  | Gui email OTP va thong bao nghiep vu.                         |
| `Backend/src/main/java/com/example/demo/service/AdminService.java`         | Dashboard, report doanh thu, CRUD user/room/medicine/service. |
| `Backend/src/main/java/com/example/demo/service/MasterDataService.java`    | Nghiep vu master data cho diagnosis/symptom/rule.             |
| `Backend/src/main/java/com/example/demo/service/AppointmentService.java`   | Tao lich, dieu pho bac si, kiem tra lich.                     |
| `Backend/src/main/java/com/example/demo/service/ReceptionistService.java`  | Duyet/tu choi lich va xu ly hang doi.                         |
| `Backend/src/main/java/com/example/demo/service/DoctorService.java`        | Nghiep vu bac si va phong kham.                               |
| `Backend/src/main/java/com/example/demo/service/MedicalRecordService.java` | Benh an, don thuoc, dich vu, workspace ke don.                |
| `Backend/src/main/java/com/example/demo/service/InvoiceService.java`       | Tao va xu ly hoa don, bien lai, PDF.                          |
| `Backend/src/main/java/com/example/demo/service/PatientService.java`       | Du lieu benh nhan, lich su benh an.                           |
| `Backend/src/main/java/com/example/demo/service/SystemSettingService.java` | Doc/cap nhat settings he thong.                               |
| `Backend/src/main/java/com/example/demo/service/GeminiChatbotService.java` | Xu ly prompt chatbot, goi Gemini va luu lich su.              |
| `Backend/src/main/java/com/example/demo/service/SepayClient.java`          | Goi SePay API va parse danh sach giao dich.                   |
| `Backend/src/main/java/com/example/demo/service/PaymentReconciliationJob.java` | Job dinh ky kiem tra SePay API de xac nhan thanh toan.     |

## 7) Backend - Repository layer

| File                                                                                          | Chuc nang                                            |
| --------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| `Backend/src/main/java/com/example/demo/repository/UserRepository.java`                       | Truy van user theo username/role/trang thai.         |
| `Backend/src/main/java/com/example/demo/repository/PatientRepository.java`                    | Truy van benh nhan va thong tin lien quan.           |
| `Backend/src/main/java/com/example/demo/repository/AppointmentRepository.java`                | Truy van lich hen theo doctor/status/time.           |
| `Backend/src/main/java/com/example/demo/repository/MedicalRecordRepository.java`              | Truy van benh an.                                    |
| `Backend/src/main/java/com/example/demo/repository/PrescriptionDetailRepository.java`         | Truy van chi tiet don thuoc.                         |
| `Backend/src/main/java/com/example/demo/repository/MedicalRecordServiceDetailRepository.java` | Truy van ket qua dich vu can lam sang trong benh an. |
| `Backend/src/main/java/com/example/demo/repository/MedicineRepository.java`                   | Truy van danh muc thuoc.                             |
| `Backend/src/main/java/com/example/demo/repository/MedicalServiceRepository.java`             | Truy van danh muc dich vu y te.                      |
| `Backend/src/main/java/com/example/demo/repository/RoomRepository.java`                       | Truy van phong kham va phan cong bac si.             |
| `Backend/src/main/java/com/example/demo/repository/InvoiceRepository.java`                    | Truy van hoa don va giao dich thanh toan.            |
| `Backend/src/main/java/com/example/demo/repository/ChatbotMessageRepository.java`             | Truy van lich su hoi thoai chatbot.                  |
| `Backend/src/main/java/com/example/demo/repository/SystemSettingRepository.java`              | Truy van bang system_settings theo key.              |
| `Backend/src/main/java/com/example/demo/repository/PaymentTransactionRepository.java`         | Truy van giao dich thanh toan.                       |

## 8) Backend - Entity (domain model)

| File                                                                            | Chuc nang                                 |
| ------------------------------------------------------------------------------- | ----------------------------------------- |
| `Backend/src/main/java/com/example/demo/entity/User.java`                       | Tai khoan nguoi dung va role.             |
| `Backend/src/main/java/com/example/demo/entity/Patient.java`                    | Ho so benh nhan.                          |
| `Backend/src/main/java/com/example/demo/entity/Role.java`                       | Enum role he thong.                       |
| `Backend/src/main/java/com/example/demo/entity/Appointment.java`                | Lich hen kham giua patient va doctor.     |
| `Backend/src/main/java/com/example/demo/entity/MedicalRecord.java`              | Benh an sau khi kham.                     |
| `Backend/src/main/java/com/example/demo/entity/PrescriptionDetail.java`         | Tung dong thuoc trong toa.                |
| `Backend/src/main/java/com/example/demo/entity/PrescriptionDetailId.java`       | Khoa ghep cho PrescriptionDetail.         |
| `Backend/src/main/java/com/example/demo/entity/MedicalService.java`             | Danh muc dich vu y te.                    |
| `Backend/src/main/java/com/example/demo/entity/MedicalRecordServiceDetail.java` | Ket qua/chi phi dich vu trong benh an.    |
| `Backend/src/main/java/com/example/demo/entity/MedicalRecordServiceId.java`     | Khoa ghep cho MedicalRecordServiceDetail. |
| `Backend/src/main/java/com/example/demo/entity/Medicine.java`                   | Danh muc thuoc va ton kho.                |
| `Backend/src/main/java/com/example/demo/entity/Invoice.java`                    | Hoa don thanh toan va thong tin thanh toan. |
| `Backend/src/main/java/com/example/demo/entity/Room.java`                       | Phong kham va doctor phu trach.           |
| `Backend/src/main/java/com/example/demo/entity/ChatbotMessage.java`             | Lich su hoi thoai chatbot.                |
| `Backend/src/main/java/com/example/demo/entity/SystemSetting.java`              | Bang key-value cau hinh dong cho admin.   |
| `Backend/src/main/java/com/example/demo/entity/PaymentTransaction.java`         | Giao dich thanh toan (webhook/job).       |
| `Backend/src/main/java/com/example/demo/entity/RoomSchedule.java`               | Lich su dung phong kham.                  |
| `Backend/src/main/java/com/example/demo/entity/DiagnosisTemplate.java`          | Mau chan doan.                            |
| `Backend/src/main/java/com/example/demo/entity/DiagnosisMedicineRule.java`      | Rule chan doan - thuoc.                   |
| `Backend/src/main/java/com/example/demo/entity/MedicalCategory.java`            | Danh muc benh/nhom y te.                  |
| `Backend/src/main/java/com/example/demo/entity/SymptomTemplate.java`            | Mau trieu chung.                          |
| `Backend/src/main/java/com/example/demo/entity/SymptomServiceMapping.java`      | Mapping trieu chung - dich vu.            |
| `Backend/src/main/java/com/example/demo/entity/SymptomServiceMappingId.java`    | Khoa ghep cho mapping.                    |

## 9) Backend - DTO (request/response)

### 9.1 Nhom Auth

- `LoginRequest.java`
- `AuthResponse.java`
- `RefreshTokenRequest.java`
- `PatientRegisterRequest.java`
- `ForgotPasswordRequest.java`
- `VerifyForgotPasswordOtpRequest.java`
- `ResetPasswordWithOtpRequest.java`

### 9.2 Nhom Admin va dashboard

- `DashboardResponse.java`
- `AdminRevenueReportResponse.java`
- `AdminRevenueReportItemResponse.java`
- `AdminRevenueChartPointResponse.java`
- `TopDoctorDTO.java`
- `AdminCreateUserRequest.java`
- `AdminUpdateUserRequest.java`
- `AdminUserResponse.java`
- `AdminRoomCreateRequest.java`
- `AdminRoomUpdateRequest.java`
- `AdminRoomAssignDoctorRequest.java`
- `AdminRoomResponse.java`
- `AdminRoomScheduleRequest.java`
- `AdminRoomScheduleResponse.java`
- `AdminSystemSettingResponse.java`
- `AdminUpdateSystemSettingRequest.java`

### 9.3 Nhom appointment va receptionist

- `AppointmentRequest.java`
- `AppointmentAssignDoctorRequest.java`
- `AppointmentFeeEstimateResponse.java`
- `ReceptionistApproveRequest.java`
- `ReceptionistApproveResponse.java`
- `ReceptionistCancelAppointmentRequest.java`
- `ReceptionistWaitingStatusUpdateRequest.java`
- `ReceptionistDoctorOptionResponse.java`
- `ReceptionistRoomSuggestionResponse.java`

### 9.4 Nhom doctor

- `DoctorResponse.java`
- `DoctorClinicRoomRequest.java`
- `DoctorPatientHistoryResponse.java`
- `DoctorPatientHistoryRowResponse.java`
- `DoctorPatientHistoryDetailResponse.java`
- `DoctorPatientPrescriptionItemResponse.java`
- `DoctorPatientServiceItemResponse.java`
- `DoctorMedicineResponse.java`

### 9.5 Nhom benh an va prescription workspace

- `CreateMedicalRecordRequest.java`
- `AddPrescriptionDetailRequest.java`
- `UpdatePrescriptionDetailRequest.java`
- `QuickAddPrescriptionMedicineRequest.java`
- `UpsertMedicalRecordServiceResultRequest.java`
- `PrescriptionWorkspaceResponse.java`
- `PrescriptionMedicineCatalogResponse.java`
- `PrescriptionCatalogMedicineResponse.java`
- `PrescriptionLineResponse.java`
- `PrescriptionAutosaveResponse.java`

### 9.6 Nhom cashier va payment

- `CashierWaitingPaymentItemResponse.java`
- `CashierPaymentRecordDetailResponse.java`
- `CashierServiceLineItemResponse.java`
- `CashierMedicineLineItemResponse.java`
- `CashierProcessPaymentRequest.java`
- `CashierProcessPaymentResponse.java`
- `CashierReceiptResponse.java`
- `CashierPrintReceiptResponse.java`
- `CashierTransactionHistoryResponse.java`
- `CashierTransactionHistoryItemResponse.java`

### 9.7 Nhom patient

- `PatientAppointmentRequest.java`
- `PatientPrefillResponse.java`
- `PatientMedicalRecordHistoryItemResponse.java`
- `PatientMedicalRecordDetailResponse.java`
- `PatientPrescriptionHistoryItemResponse.java`

### 9.8 Nhom chatbot

- `ChatbotAskRequest.java`
- `ChatbotAskResponse.java`
- `ChatbotHistoryItemResponse.java`

### 9.9 Nhom master data, diagnosis, symptom

- `DiagnosisTemplateRequest.java`
- `DiagnosisTemplateResponse.java`
- `DiagnosisMedicineRuleRequest.java`
- `DiagnosisMedicineRuleResponse.java`
- `MedicalCategoryRequest.java`
- `MedicalCategoryResponse.java`
- `SymptomTemplateRequest.java`
- `SymptomTemplateResponse.java`
- `SymptomServiceMappingRequest.java`
- `SymptomServiceMappingResponse.java`
- `AdminMedicalServiceCreateRequest.java`
- `AdminMedicalServiceUpdatePriceRequest.java`
- `AdminMedicalServiceResponse.java`
- `AdminMedicineCreateRequest.java`
- `AdminMedicineUpdateRequest.java`
- `AdminMedicineResponse.java`

## 10) Backend - Exception, test va script van hanh

| File                                                                                                     | Chuc nang                                               |
| -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------- |
| `Backend/src/main/java/com/example/demo/exception/AppException.java`                                     | Runtime exception tu dinh nghia cho nghiep vu he thong. |
| `Backend/src/main/java/com/example/demo/exception/GlobalExceptionHandler.java`                           | Chuan hoa response loi cho API.                         |
| `Backend/src/test/java/com/example/demo/DemoApplicationTests.java`                                       | Smoke test khoi dong context Spring Boot.               |
| `Backend/scripts/start-redis.ps1`, `Backend/scripts/start-redis.cmd`                                     | Khoi dong Redis local cho OTP flow.                     |
| `Backend/scripts/stop-redis.ps1`, `Backend/scripts/stop-redis.cmd`                                       | Dung Redis local va xoa marker pid.                     |
| `Backend/scripts/migrate-sqlserver-to-postgres.ps1`, `Backend/scripts/migrate-sqlserver-to-postgres.cmd` | Ho tro migrate du lieu SQL Server sang PostgreSQL.      |
| `Backend/scripts/MIGRATE_SQLSERVER_TO_POSTGRES.md`                                                       | Huong dan chi tiet migration pipeline.                  |
| `Backend/scripts/check-bom.ps1`, `Backend/scripts/remove-bom.ps1`                                        | Kiem tra/xu ly BOM trong source file.                   |
| `Backend/scripts/add-chatbot-history-postgres.sql`                                                       | Tao bang luu lich su hoi thoai chatbot tren PostgreSQL. |

## 11) Frontend - Build va cau hinh

| File                           | Chuc nang                                |
| ------------------------------ | ---------------------------------------- |
| `Frontend/package.json`        | Scripts va dependency cho Next.js/React. |
| `Frontend/next.config.ts`      | Cau hinh Next.js.                        |
| `Frontend/tsconfig.json`       | Cau hinh TypeScript.                     |
| `Frontend/next-env.d.ts`       | Khai bao type cho Next.js.               |
| `Frontend/src/app/globals.css` | Global style.                            |

## 12) Frontend - App router va route pages

| File                                                                                                                                                                                                   | Chuc nang                      |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------ |
| `Frontend/src/app/page.tsx`                                                                                                                                                                            | Landing page.                  |
| `Frontend/src/app/(auth)/signin/page.tsx`                                                                                                                                                              | Man hinh dang nhap.            |
| `Frontend/src/app/(auth)/signup/page.tsx`                                                                                                                                                              | Dang ky benh nhan.             |
| `Frontend/src/app/(auth)/forgotpassword/page.tsx`                                                                                                                                                      | Quen mat khau.                 |
| `Frontend/src/app/(patients)/dashboard/page.tsx`                                                                                                                                                       | Dashboard benh nhan.           |
| `Frontend/src/app/(patients)/booking/page.tsx`                                                                                                                                                         | Dat lich.                      |
| `Frontend/src/app/(patients)/appointments/page.tsx`                                                                                                                                                    | Xem lich hen.                  |
| `Frontend/src/app/(patients)/invoices/page.tsx`                                                                                                                                                        | Xem hoa don.                   |
| `Frontend/src/app/(patients)/patient-history/page.tsx`                                                                                                                                                 | Lich su kham benh.             |
| `Frontend/src/app/admin/page.tsx`                                                                                                                                                                      | Trang admin tong quan.         |
| `Frontend/src/app/admin/users/page.tsx`                                                                                                                                                                | Quan ly nguoi dung.            |
| `Frontend/src/app/admin/rooms/page.tsx`                                                                                                                                                                | Quan ly phong kham.            |
| `Frontend/src/app/admin/medicines/page.tsx`                                                                                                                                                            | Quan ly thuoc.                 |
| `Frontend/src/app/admin/services/page.tsx`                                                                                                                                                             | Quan ly dich vu.               |
| `Frontend/src/app/admin/settings/page.tsx`                                                                                                                                                             | Cau hinh he thong.             |
| `Frontend/src/app/admin/diagnoses/page.tsx`                                                                                                                                                            | Quan ly mau chan doan.         |
| `Frontend/src/app/admin/disease-categories/page.tsx`                                                                                                                                                   | Quan ly danh muc benh.         |
| `Frontend/src/app/admin/symptoms/page.tsx`                                                                                                                                                             | Quan ly trieu chung.           |
| `Frontend/src/app/cashier/page.tsx`                                                                                                                                                                    | Man hinh thu ngan.             |
| `Frontend/src/app/cashier/history/page.tsx`                                                                                                                                                            | Lich su thanh toan.            |
| `Frontend/src/app/doctor/page.tsx`                                                                                                                                                                     | Man hinh bac si.               |
| `Frontend/src/app/receptionist/page.tsx`                                                                                                                                                               | Man hinh le tan.               |
| `Frontend/src/app/receptionist/pending/page.tsx`                                                                                                                                                       | Lich hen cho duyet.            |
| `Frontend/src/app/receptionist/confirmed/page.tsx`                                                                                                                                                     | Lich hen da xac nhan.          |
| `Frontend/src/app/(patients)/layout.tsx`, `Frontend/src/app/admin/layout.tsx`, `Frontend/src/app/cashier/layout.tsx`, `Frontend/src/app/doctor/layout.tsx`, `Frontend/src/app/receptionist/layout.tsx` | Layout theo tung khu vuc/role. |

## 13) Frontend - Feature folders va component chinh

| File/Folder                                                                                                                   | Chuc nang                                                                                                                                                                                  |
| ----------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `Frontend/src/features/landingpage/landingpage.tsx`                                                                           | Giao dien trang chu.                                                                                                                                                                       |
| `Frontend/src/features/auth/sign-in/sign-in-form.tsx`                                                                         | Form dang nhap.                                                                                                                                                                            |
| `Frontend/src/features/auth/sign-up/sign-up-form.tsx`                                                                         | Form dang ky.                                                                                                                                                                              |
| `Frontend/src/features/auth/forgotpassword/forgotpassword.tsx`                                                                | Luong quen mat khau.                                                                                                                                                                       |
| `Frontend/src/features/patient/booking/booking-page.tsx`, `Frontend/src/features/patient/booking/components/booking-form.tsx` | Dat lich.                                                                                                                                                                                  |
| `Frontend/src/features/patient/dashboard/dashboard.tsx`                                                                       | Dashboard benh nhan.                                                                                                                                                                       |
| `Frontend/src/features/patient/appointments/appointments.tsx`                                                                 | Danh sach lich hen.                                                                                                                                                                        |
| `Frontend/src/features/patient/invoices/invoices.tsx`                                                                         | Danh sach hoa don.                                                                                                                                                                         |
| `Frontend/src/features/patient/patient-history/patient-history.tsx`                                                           | Lich su kham.                                                                                                                                                                              |
| `Frontend/src/features/doctor/doctor-page.tsx`                                                                                | Trang bac si.                                                                                                                                                                              |
| `Frontend/src/features/doctor/components/*`                                                                                   | Waiting list, completed list, medical record form, prescription section, examination modal, patient info, doctor stats.                                                                    |
| `Frontend/src/features/receptionist/receptionist-page.tsx`                                                                    | Trang le tan.                                                                                                                                                                              |
| `Frontend/src/features/receptionist/components/*`                                                                             | Confirm/cancel appointment, pending appointment, cancelled appointment, dashboard stats.                                                                                                   |
| `Frontend/src/features/cashier/cashier-page.tsx`                                                                              | Trang thu ngan.                                                                                                                                                                            |
| `Frontend/src/features/cashier/components/*`                                                                                  | Pending prescriptions, dispensed prescriptions, payment dialog, pharmacy stats.                                                                                                            |
| `Frontend/src/features/admin/components/*`                                                                                    | User-management, rooms-management, medicines-management, service-management, settings-management, reports, diagnosis-template-management, disease-category-management, symptom-management. |
| `Frontend/src/components/chatbot/floating-chatbot.tsx`                                                                        | Widget chatbot noi tren giao dien.                                                                                                                                                         |
| `Frontend/src/components/layout/brand-logo.tsx`                                                                               | Logo/he thong nhan dien thuong hieu.                                                                                                                                                       |
| `Frontend/src/components/ui/*`                                                                                                | Cac component UI dung chung.                                                                                                                                                               |

## 14) Frontend - Hooks, services, types

| File                                                                                                                                                               | Chuc nang                                   |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------- |
| `Frontend/src/hooks/useAuth.ts`                                                                                                                                    | Quan ly trang thai auth.                    |
| `Frontend/src/hooks/useFetch.ts`                                                                                                                                   | Hook fetch du lieu tong quat.               |
| `Frontend/src/hooks/useDebounce.ts`                                                                                                                                | Debounce input/filter.                      |
| `Frontend/src/hooks/useReceptionistDashboard.ts`                                                                                                                   | Du lieu dashboard le tan.                   |
| `Frontend/src/hooks/useDiagnosisTemplateDashboard.ts`                                                                                                              | Du lieu dashboard chan doan.                |
| `Frontend/src/services/api.ts`                                                                                                                                     | Axios instance va cau hinh goi backend.     |
| `Frontend/src/services/authService.ts`                                                                                                                             | API auth.                                   |
| `Frontend/src/services/adminService.ts`                                                                                                                            | API admin.                                  |
| `Frontend/src/services/cashierService.ts`                                                                                                                          | API thu ngan.                               |
| `Frontend/src/services/doctorService.ts`                                                                                                                           | API bac si.                                 |
| `Frontend/src/services/masterDataService.ts`                                                                                                                       | API master data.                            |
| `Frontend/src/services/patientService.ts`                                                                                                                          | API benh nhan.                              |
| `Frontend/src/services/receptionistService.ts`                                                                                                                     | API le tan.                                 |
| `Frontend/src/services/userService.ts`                                                                                                                             | API user chung.                             |
| `Frontend/src/types/appointment.type.ts`, `auth.ts`, `doctor.type.ts`, `invoice.type.ts`, `medicine.type.ts`, `pharmacy.type.ts`, `record.type.ts`, `user.type.ts` | Dinh nghia type cho cac nhom du lieu chinh. |

## 15) Tong ket dong chay nghiep vu

- Frontend goi API qua `services/*` va render theo tung role.
- Backend xu ly phan quyen qua JWT + SecurityConfig.
- Du lieu tiep nhan va bao cao nam o cac nhom: appointment, medical record, invoice/payment, master data va chatbot.
- Thanh toan chuyen khoan: webhook SePay ghi giao dich, job dinh ky goi SePay API de doi chieu va cap nhat hoa don.
- Cac file seeder va config giup khoi tao du lieu mau va map cong viec phong kham ngay khi chay local.

## 16) Ghi chu

- Tai lieu nay da duoc viet lai theo code hien tai cua workspace.
- Neu co thay doi file code moi sau nay, can cap nhat them cac nhom controller/service/dto/frontend tuong ung.
