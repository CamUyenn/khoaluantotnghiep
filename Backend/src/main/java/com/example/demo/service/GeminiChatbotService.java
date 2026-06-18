package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.net.URI;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import com.example.demo.exception.AppException;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.demo.config.GeminiProperties;
import com.example.demo.dto.ChatbotAskResponse;
import com.example.demo.dto.ChatbotHistoryItemResponse;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.ChatbotMessage;
import com.example.demo.entity.Invoice;
import com.example.demo.entity.MedicalRecord;
import com.example.demo.entity.MedicalService;
import com.example.demo.entity.Medicine;
import com.example.demo.entity.Patient;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.ChatbotMessageRepository;
import com.example.demo.repository.InvoiceRepository;
import com.example.demo.repository.MedicalRecordRepository;
import com.example.demo.repository.MedicalServiceRepository;
import com.example.demo.repository.MedicineRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;

@Service
@SuppressWarnings("null")
public class GeminiChatbotService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiChatbotService.class);
    private static final String MESSAGE_ROLE_USER = "USER";
    private static final String MESSAGE_ROLE_ASSISTANT = "ASSISTANT";

    private static final String SYSTEM_PROMPT = "Bạn là AI chatbot hỗ trợ phòng khám tổng hợp. "
            + "Bạn chỉ cung cấp thông tin tham khảo, không thay thế bác sĩ. "
            + "Nếu có dấu hiệu nguy hiểm như khó thở, đau ngực, ngất, xuất huyết nhiều, hãy khuyên bệnh nhân đi cấp cứu ngay. "
            + "Trả lời bằng tiếng Việt rõ ràng, ngắn gọn, lịch sự. "
            + "Nếu có dữ liệu nghiệp vụ phòng khám được cung cấp trong prompt thì ưu tiên dùng đúng dữ liệu đó. "
            + "Khi người dùng hỏi giờ làm việc, thời gian mở cửa hoặc đóng cửa, phải dùng đúng dữ liệu đã cung cấp và không dùng placeholder như [Giờ mở cửa].";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int CATALOG_CONTEXT_LIMIT = 5;
    private static final int APPOINTMENT_CONTEXT_LIMIT = 5;
    private static final int MEDICAL_RECORD_CONTEXT_LIMIT = 3;
    private static final int INVOICE_CONTEXT_LIMIT = 3;
    private static final int MAX_TEXT_PREVIEW_LENGTH = 160;

    private static final Set<String> SEARCH_STOP_WORDS = Set.of(
            "toi", "minh", "cho", "xin", "duoc", "la", "ve", "cua", "va", "hoac", "nhung", "nhieu", "nay",
            "kia", "co", "khong", "muon", "tim", "hoi", "giup", "can", "tu", "den", "tai", "benh", "phongkham");

    private final GeminiProperties geminiProperties;
    private final RestTemplate geminiRestTemplate;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final InvoiceRepository invoiceRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final MedicineRepository medicineRepository;
    private final ChatbotMessageRepository chatbotMessageRepository;

    public GeminiChatbotService(
            GeminiProperties geminiProperties,
            @Qualifier("geminiRestTemplate") RestTemplate geminiRestTemplate,
            UserRepository userRepository,
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            MedicalRecordRepository medicalRecordRepository,
            InvoiceRepository invoiceRepository,
            MedicalServiceRepository medicalServiceRepository,
            MedicineRepository medicineRepository,
            ChatbotMessageRepository chatbotMessageRepository) {
        this.geminiProperties = geminiProperties;
        this.geminiRestTemplate = geminiRestTemplate;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.invoiceRepository = invoiceRepository;
        this.medicalServiceRepository = medicalServiceRepository;
        this.medicineRepository = medicineRepository;
        this.chatbotMessageRepository = chatbotMessageRepository;
    }

    // Chức năng: gửi câu hỏi người dùng đến Gemini API, có lưu lịch sử nếu người
    // dùng
    // đã đăng nhập.
    public ChatbotAskResponse ask(String message, String username) {
        validateGeminiConfig();
        String normalizedMessage = trimToNull(message);
        if (normalizedMessage == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Nội dung câu hỏi là bắt buộc");
        }

        User user = null;
        List<ChatbotMessage> recentHistory = List.of();
        String businessContext;

        try {
            user = resolveUserIfAuthenticated(username);
            recentHistory = user == null
                    ? List.of()
                    : loadRecentMessages(user.getId(), normalizeHistoryContextLimit());
            businessContext = buildBusinessContext(normalizedMessage, user);
        } catch (Exception contextError) {
            logger.warn("Không thể nạp ngữ cảnh hội thoại cho user '{}': {}. Hệ thống sẽ tiếp tục ở chế độ guest.",
                    username,
                    contextError.getMessage());
            user = null;
            recentHistory = List.of();
            businessContext = buildBusinessContext(normalizedMessage, null);
        }

        URI uri = buildGenerateContentUri();

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text",
                                                buildPrompt(normalizedMessage, recentHistory, businessContext))))),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 512));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<JsonNode> response = geminiRestTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    JsonNode.class);

            JsonNode body = response.getBody();
            String answer = extractAnswer(body);

            if (user != null) {
                try {
                    saveHistoryExchange(user, normalizedMessage, answer);
                } catch (Exception persistenceError) {
                    logger.warn("Không thể lưu lịch sử chatbot cho user {}: {}", user.getId(),
                            persistenceError.getMessage());
                }
            }

            return new ChatbotAskResponse(answer, geminiProperties.getModel());
        } catch (HttpStatusCodeException ex) {
            String messageText = "Gemini API phản hồi lỗi " + ex.getStatusCode().value();
            throw AppException.of(HttpStatus.BAD_GATEWAY, messageText);
        } catch (ResourceAccessException ex) {
            throw AppException.of(HttpStatus.GATEWAY_TIMEOUT, "Hết thời gian chờ phản hồi từ Gemini");
        }
    }

    // Chức năng: lấy lịch sử hội thoại của người dùng đăng nhập để dùng lại khi tái
    // khám.
    public List<ChatbotHistoryItemResponse> getHistory(String username) {
        User user = resolveUserRequired(username);
        List<ChatbotMessage> messages = loadRecentMessages(user.getId(), normalizeHistoryReturnLimit());
        Collections.reverse(messages);
        return messages.stream()
                .map(this::toHistoryItemResponse)
                .toList();
    }

    // Chức năng: xóa lịch sử hội thoại của người dùng đăng nhập.
    public long clearHistory(String username) {
        User user = resolveUserRequired(username);
        return chatbotMessageRepository.deleteByUser_Id(user.getId());
    }

    // Chức năng: kiểm tra trạng thái bật/tắt và thông tin xác thực của Gemini.
    private void validateGeminiConfig() {
        if (!geminiProperties.isEnabled()) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI Chatbot hiện đang tắt. Vui lòng bật ai.gemini.enabled");
        }

        String apiKey = trimToNull(geminiProperties.getApiKey());
        if (apiKey == null) {
            throw AppException.of(HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình GEMINI_API_KEY");
        }
    }

    // Chức năng: dựng URL đầy đủ cho endpoint generateContent của Gemini.
    private URI buildGenerateContentUri() {
        return UriComponentsBuilder
                .fromUriString(trimTrailingSlash(geminiProperties.getEndpoint()) + "/{model}:generateContent")
                .queryParam("key", geminiProperties.getApiKey())
                .buildAndExpand(geminiProperties.getModel())
                .encode()
                .toUri();
    }

    // Chức năng: trích xuất nội dung text đầu tiên từ phản hồi Gemini.
    private String extractAnswer(JsonNode body) {
        if (body == null) {
            throw AppException.of(HttpStatus.BAD_GATEWAY, "Phản hồi Gemini rỗng");
        }

        JsonNode textNode = body.at("/candidates/0/content/parts/0/text");
        String answer = trimToNull(textNode.isMissingNode() ? null : textNode.asText());
        if (answer == null) {
            throw AppException.of(HttpStatus.BAD_GATEWAY,
                    "Không nhận được nội dung trả lời hợp lệ từ Gemini");
        }
        return answer;
    }

    // Chức năng: ghép system prompt, lịch sử gần đây và câu hỏi hiện tại thành
    // prompt
    // gửi model.
    private String buildPrompt(String userMessage, List<ChatbotMessage> recentHistory, String businessContext) {
        StringBuilder builder = new StringBuilder(SYSTEM_PROMPT);

        if (recentHistory != null && !recentHistory.isEmpty()) {
            List<ChatbotMessage> chronologicalHistory = new ArrayList<>(recentHistory);
            Collections.reverse(chronologicalHistory);

            builder.append("\n\nLịch sử hội thoại gần đây:\n");
            for (ChatbotMessage historyMessage : chronologicalHistory) {
                builder.append(toPromptSpeaker(historyMessage.getRole()))
                        .append(": ")
                        .append(historyMessage.getContent())
                        .append('\n');
            }
        }

        if (businessContext != null) {
            builder.append("\n\nDữ liệu nghiệp vụ phòng khám liên quan:\n")
                    .append(businessContext);
        }

        builder.append("\nCâu hỏi hiện tại của người dùng: ")
                .append(userMessage);
        return builder.toString();
    }

    // Chức năng: tổng hợp dữ liệu nghiệp vụ để tăng độ chính xác trả lời.
    private String buildBusinessContext(String userMessage, User user) {
        StringBuilder context = new StringBuilder();

        appendClinicOperationalContext(context);
        appendCatalogContext(context, userMessage);
        appendPatientContext(context, user);

        return trimToNull(context.toString());
    }

    // Chức năng: thêm thông tin hoạt động cố định để AI trả lời đúng giờ làm việc.
    private void appendClinicOperationalContext(StringBuilder context) {
        String workingHours = trimToNull(geminiProperties.getClinicWorkingHours());
        String hotline = trimToNull(geminiProperties.getClinicHotline());
        String address = trimToNull(geminiProperties.getClinicAddress());

        if (workingHours == null && hotline == null && address == null) {
            return;
        }

        context.append("- Thông tin hoạt động phòng khám:\n");
        if (workingHours != null) {
            context.append("  + Giờ làm việc: ")
                    .append(workingHours)
                    .append('\n');
        }
        if (hotline != null) {
            context.append("  + Hotline: ")
                    .append(hotline)
                    .append('\n');
        }
        if (address != null) {
            context.append("  + Địa chỉ: ")
                    .append(address)
                    .append('\n');
        }
    }

    // Chức năng: tìm dịch vụ và thuốc theo từ khóa câu hỏi.
    private void appendCatalogContext(StringBuilder context, String userMessage) {
        Set<String> keywords = extractSearchKeywords(userMessage);
        List<MedicalService> matchedServices = new ArrayList<>();
        Set<Long> serviceIds = new LinkedHashSet<>();
        List<Medicine> matchedMedicines = new ArrayList<>();
        Set<Long> medicineIds = new LinkedHashSet<>();

        for (String keyword : keywords) {
            if (matchedServices.size() < CATALOG_CONTEXT_LIMIT) {
                List<MedicalService> services = medicalServiceRepository
                        .findByIsActiveTrueAndServiceNameContainingIgnoreCaseOrderByServiceNameAsc(keyword);
                for (MedicalService service : services) {
                    if (service.getId() != null && serviceIds.add(service.getId())) {
                        matchedServices.add(service);
                    }
                    if (matchedServices.size() >= CATALOG_CONTEXT_LIMIT) {
                        break;
                    }
                }
            }

            if (matchedMedicines.size() < CATALOG_CONTEXT_LIMIT) {
                List<Medicine> medicines = medicineRepository
                        .findByIsActiveTrueAndMedicineNameContainingIgnoreCaseOrderByMedicineNameAsc(keyword);
                for (Medicine medicine : medicines) {
                    if (medicine.getId() != null && medicineIds.add(medicine.getId())) {
                        matchedMedicines.add(medicine);
                    }
                    if (matchedMedicines.size() >= CATALOG_CONTEXT_LIMIT) {
                        break;
                    }
                }
            }

            if (matchedServices.size() >= CATALOG_CONTEXT_LIMIT && matchedMedicines.size() >= CATALOG_CONTEXT_LIMIT) {
                break;
            }
        }

        if (matchedServices.isEmpty() && matchedMedicines.isEmpty()) {
            matchedServices = medicalServiceRepository.findByIsActiveTrueOrderByServiceNameAsc().stream()
                    .limit(3)
                    .toList();
            matchedMedicines = medicineRepository.findByIsActiveTrueOrderByMedicineNameAsc().stream()
                    .limit(3)
                    .toList();
        }

        if (!matchedServices.isEmpty()) {
            context.append("- Dịch vụ phòng khám (một phần):\n");
            for (MedicalService service : matchedServices) {
                context.append("  + ")
                        .append(service.getServiceName())
                        .append(" - Giá hiện tại: ")
                        .append(formatMoney(service.getCurrentPrice()))
                        .append('\n');
            }
        }

        if (!matchedMedicines.isEmpty()) {
            if (context.length() > 0) {
                context.append('\n');
            }
            context.append("- Thuốc phòng khám (một phần):\n");
            for (Medicine medicine : matchedMedicines) {
                context.append("  + ")
                        .append(medicine.getMedicineName())
                        .append(" - Giá bán: ")
                        .append(formatMoney(medicine.getSellingPrice()))
                        .append(" - Tồn kho: ")
                        .append(medicine.getStockQuantity() == null ? 0 : medicine.getStockQuantity())
                        .append(medicine.getUnit() == null ? "" : " " + medicine.getUnit())
                        .append('\n');
            }
        }
    }

    // Chức năng: thêm ngữ cảnh cá nhân của bệnh nhân đang đăng nhập.
    private void appendPatientContext(StringBuilder context, User user) {
        if (user == null || user.getRole() != Role.PATIENT) {
            return;
        }

        Patient patient = patientRepository.findByUserId(user.getId()).orElse(null);
        if (patient == null) {
            return;
        }

        if (context.length() > 0) {
            context.append('\n');
        }

        context.append("- Hồ sơ người dùng đang đăng nhập:\n")
                .append("  + Mã bệnh nhân: ").append(patient.getId()).append('\n')
                .append("  + Họ tên: ")
                .append(trimToNull(patient.getFullName()) == null ? "Chưa cập nhật" : patient.getFullName())
                .append('\n');

        if (trimToNull(patient.getHealthInsuranceNumber()) != null) {
            context.append("  + Mã BHYT: ").append(patient.getHealthInsuranceNumber()).append('\n');
        }
        if (trimToNull(patient.getPhoneNumber()) != null) {
            context.append("  + Số điện thoại: ").append(patient.getPhoneNumber()).append('\n');
        }

        List<Appointment> appointments = appointmentRepository
                .findByPatient_IdOrderByAppointmentTimeDesc(patient.getId())
                .stream()
                .limit(APPOINTMENT_CONTEXT_LIMIT)
                .toList();
        if (!appointments.isEmpty()) {
            context.append("  + Lịch hẹn gần đây:\n");
            for (Appointment appointment : appointments) {
                context.append("    * #").append(appointment.getId())
                        .append(" - ").append(formatDateTime(appointment.getAppointmentTime()))
                        .append(" - Trạng thái: ").append(normalizeStatusLabel(appointment.getStatus()));

                if (appointment.getDoctor() != null) {
                    String doctorName = trimToNull(appointment.getDoctor().getFullName());
                    if (doctorName == null) {
                        doctorName = trimToNull(appointment.getDoctor().getUsername());
                    }
                    if (doctorName != null) {
                        context.append(" - Bác sĩ: ").append(doctorName);
                    }
                }

                if (trimToNull(appointment.getSymptoms()) != null) {
                    context.append(" - Triệu chứng: ")
                            .append(ellipsize(appointment.getSymptoms(), MAX_TEXT_PREVIEW_LENGTH));
                }

                context.append('\n');
            }
        }

        List<MedicalRecord> records = medicalRecordRepository.findByAppointment_Patient_Id(patient.getId()).stream()
                .sorted(Comparator.comparing(this::medicalRecordSortTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MEDICAL_RECORD_CONTEXT_LIMIT)
                .toList();
        if (!records.isEmpty()) {
            context.append("  + Bệnh án gần đây:\n");
            for (MedicalRecord record : records) {
                context.append("    * Mã bệnh án #").append(record.getId())
                        .append(" - Chẩn đoán: ").append(ellipsize(record.getDiagnosis(), MAX_TEXT_PREVIEW_LENGTH));
                if (trimToNull(record.getDoctorAdvice()) != null) {
                    context.append(" - Lời dặn: ").append(ellipsize(record.getDoctorAdvice(), MAX_TEXT_PREVIEW_LENGTH));
                }
                context.append('\n');
            }
        }

        List<Invoice> invoices = records.stream()
                .map(record -> invoiceRepository.findByMedicalRecord_Id(record.getId()).orElse(null))
                .filter(invoice -> invoice != null)
                .sorted(Comparator.comparing(Invoice::getPaidAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(INVOICE_CONTEXT_LIMIT)
                .toList();
        if (!invoices.isEmpty()) {
            context.append("  + Hóa đơn gần đây:\n");
            for (Invoice invoice : invoices) {
                context.append("    * Hóa đơn #").append(invoice.getId())
                        .append(" - Tổng tiền: ").append(formatMoney(invoice.getGrandTotal()))
                        .append(" - Trạng thái: ")
                        .append(Boolean.TRUE.equals(invoice.getIsPaid())
                                ? "Đã thanh toán"
                                : "Chưa thanh toán");
                if (invoice.getPaidAt() != null) {
                    context.append(" - Thanh toán lúc: ").append(formatDateTime(invoice.getPaidAt()));
                }
                context.append('\n');
            }
        }
    }

    // Chức năng: tách từ khóa từ câu hỏi để tìm dữ liệu nghiệp vụ gần nhất.
    private Set<String> extractSearchKeywords(String message) {
        String normalized = normalizeSearchText(message);
        if (normalized == null) {
            return Set.of();
        }

        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        if (normalized.length() <= 40) {
            keywords.add(normalized);
        }

        String[] tokens = normalized.split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.length() < 2 || SEARCH_STOP_WORDS.contains(token)) {
                continue;
            }
            keywords.add(token);
            if (keywords.size() >= 8) {
                break;
            }
        }

        return keywords;
    }

    // Chức năng: chuẩn hóa text để tìm kiếm không dấu.
    private String normalizeSearchText(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeStatusLabel(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return "Chưa cập nhật";
        }

        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "PENDING", "PENDING_CONFIRMATION", "DRAFT" -> "Đang chờ duyệt";
            case "WAITING", "APPROVED", "CONFIRMED" -> "Đã duyệt";
            case "IN_PROGRESS" -> "Đang khám";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED", "CANCELLED_BY_CLINIC" -> "Đã hủy";
            default -> normalized;
        };
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0đ";
        }
        return String.format(Locale.forLanguageTag("vi-VN"), "%,.0fđ", value.doubleValue());
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "Chưa cập nhật";
        }
        return DATE_TIME_FORMATTER.format(value);
    }

    private LocalDateTime medicalRecordSortTime(MedicalRecord record) {
        if (record.getCreatedAt() != null) {
            return record.getCreatedAt();
        }
        if (record.getAppointment() != null) {
            return record.getAppointment().getAppointmentTime();
        }
        return null;
    }

    private String ellipsize(String value, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "Chưa cập nhật";
        }

        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    // Chức năng: nạp lịch sử gần nhất theo giới hạn.
    private List<ChatbotMessage> loadRecentMessages(Long userId, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, limit);
        return new ArrayList<>(chatbotMessageRepository.findByUser_IdOrderByCreatedAtDescIdDesc(userId, pageable));
    }

    // Chức năng: lưu cặp hỏi/đáp vào lịch sử hội thoại theo user/patient.
    private void saveHistoryExchange(User user, String question, String answer) {
        Patient patient = patientRepository.findByUserId(user.getId()).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        ChatbotMessage userMessage = new ChatbotMessage();
        userMessage.setUser(user);
        userMessage.setPatient(patient);
        userMessage.setRole(MESSAGE_ROLE_USER);
        userMessage.setContent(question);
        userMessage.setCreatedAt(now);

        ChatbotMessage assistantMessage = new ChatbotMessage();
        assistantMessage.setUser(user);
        assistantMessage.setPatient(patient);
        assistantMessage.setRole(MESSAGE_ROLE_ASSISTANT);
        assistantMessage.setContent(answer);
        assistantMessage.setCreatedAt(now.plusNanos(1));

        chatbotMessageRepository.saveAll(List.of(userMessage, assistantMessage));
    }

    // Chức năng: ánh xạ entity lịch sử chatbot sang response DTO.
    private ChatbotHistoryItemResponse toHistoryItemResponse(ChatbotMessage message) {
        return new ChatbotHistoryItemResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt());
    }

    // Chức năng: chuyển role lịch sử về nhãn speaker cho prompt.
    private String toPromptSpeaker(String role) {
        if (MESSAGE_ROLE_ASSISTANT.equalsIgnoreCase(role)) {
            return "Trợ lý";
        }
        return "Người dùng";
    }

    // Chức năng: chuẩn hóa giới hạn số message dùng làm context prompt.
    private int normalizeHistoryContextLimit() {
        return Math.min(30, Math.max(0, geminiProperties.getHistoryContextMessages()));
    }

    // Chức năng: chuẩn hóa giới hạn số message trả về cho API lịch sử.
    private int normalizeHistoryReturnLimit() {
        return Math.min(500, Math.max(1, geminiProperties.getHistoryReturnMessages()));
    }

    // Chức năng: lấy user nếu có đăng nhập, không đăng nhập thì trả về null.
    private User resolveUserIfAuthenticated(String username) {
        String normalizedUsername = trimToNull(username);
        if (normalizedUsername == null || "anonymousUser".equalsIgnoreCase(normalizedUsername)) {
            return null;
        }

        return userRepository.findByUsername(normalizedUsername)
                .or(() -> userRepository.findByEmailIgnoreCase(normalizedUsername))
                .orElseThrow(
                        () -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng đăng nhập"));
    }

    // Chức năng: bắt buộc phải có user đăng nhập để truy cập lịch sử.
    private User resolveUserRequired(String username) {
        User user = resolveUserIfAuthenticated(username);
        if (user == null) {
            throw AppException.of(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để xem lịch sử chatbot");
        }
        return user;
    }

    // Chức năng: chuẩn hóa chuỗi đầu vào, rỗng thì trả về null.
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Chức năng: loại dấu / ở cuối URL để tránh sinh URL sai khi nối đường dẫn.
    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
