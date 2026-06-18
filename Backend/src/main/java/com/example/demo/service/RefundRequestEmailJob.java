package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.constants.PaymentStatus;
import com.example.demo.entity.Appointment;
import com.example.demo.repository.AppointmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundRequestEmailJob {

    private static final String STATUS_PENDING = "PENDING";
    private static final String CANCELLATION_REASON_NO_SHOW = "KHONG_DEN_KHAM";
    private static final String EMAIL_SENT_SUFFIX = "|DA GUI MAIL";

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    @Value("${payments.refund-request.enabled:true}")
    private boolean enabled;

    @Value("${payments.refund-request.cutoff-hour:18}")
    private int cutoffHour;

    @Value("${payments.refund-request.cutoff-minute:0}")
    private int cutoffMinute;

    @Scheduled(cron = "${payments.refund-request.cron:0 0 18 * * *}")
    public void sendRefundRequestEmails() {
        if (!enabled) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalTime cutoffTime = LocalTime.of(normalizeHour(cutoffHour), normalizeMinute(cutoffMinute));
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime cutoffDateTime = today.atTime(cutoffTime);

        List<Appointment> appointments = appointmentRepository
                .findByStatusAndPaymentStatusAndAppointmentTimeBetweenAndCancellationReason(
                        STATUS_PENDING,
                        PaymentStatus.FULLY_PAID,
                        startOfDay,
                        cutoffDateTime,
                        CANCELLATION_REASON_NO_SHOW);

        if (appointments.isEmpty()) {
            return;
        }

        int sentCount = 0;
        for (Appointment appointment : appointments) {
            if (appointment == null) {
                continue;
            }
            notificationService.notifyRefundRequestForNoShow(appointment);
            appointment.setCancellationReason(appendEmailSentSuffix(appointment.getCancellationReason()));
            appointmentRepository.save(appointment);
            sentCount++;
        }

        log.info("Refund request email job completed: sentCount={}", sentCount);
    }

    private int normalizeHour(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 23) {
            return 23;
        }
        return value;
    }

    private int normalizeMinute(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 59) {
            return 59;
        }
        return value;
    }

    private String appendEmailSentSuffix(String value) {
        String base = value == null ? "" : value.trim();
        if (base.isBlank()) {
            return CANCELLATION_REASON_NO_SHOW + EMAIL_SENT_SUFFIX;
        }
        if (base.endsWith(EMAIL_SENT_SUFFIX)) {
            return base;
        }
        return base + EMAIL_SENT_SUFFIX;
    }
}
