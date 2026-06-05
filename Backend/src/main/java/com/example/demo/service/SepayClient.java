package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SepayClient {

    private static final DateTimeFormatter SEPAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${sepay.api.endpoint:}")
    private String endpoint;

    @Value("${sepay.api.auth-token:}")
    private String authToken;

    @Value("${sepay.api.query:}")
    private String extraQuery;

    public SepayClient(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder.build();
        this.objectMapper = objectMapper;
    }

    public List<SepayTransaction> fetchTransactions() {
        if (endpoint == null || endpoint.isBlank()) {
            return Collections.emptyList();
        }

        String requestUrl = buildUrlWithQuery(endpoint, extraQuery);
        if (requestUrl == null || requestUrl.isBlank()) {
            return Collections.emptyList();
        }
        HttpHeaders headers = new HttpHeaders();
        if (authToken != null && !authToken.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + authToken.trim());
        }

        try {
            java.net.URI requestUri = java.net.URI.create(requestUrl);
            requestUri = java.util.Objects.requireNonNull(requestUri, "requestUri");
            HttpMethod method = java.util.Objects.requireNonNull(HttpMethod.GET, "HttpMethod.GET");
            ResponseEntity<SepayResponse> response = restTemplate.exchange(
                    requestUri,
                    method,
                    new HttpEntity<>(headers),
                    SepayResponse.class);
            SepayResponse body = response.getBody();
            if (body == null || body.transactions == null) {
                return Collections.emptyList();
            }
            return body.transactions;
        } catch (RestClientException ex) {
            log.warn("SePay API request failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    public String toRawPayload(SepayTransaction tx) {
        if (tx == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tx);
        } catch (Exception ex) {
            return null;
        }
    }

    public LocalDateTime parseTransactionTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), SEPAY_TIME_FORMAT);
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildUrlWithQuery(String baseUrl, String query) {
        if (query == null || query.isBlank()) {
            return baseUrl;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return baseUrl;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        String cleaned = query.startsWith("?") ? query.substring(1) : query;
        for (String pair : cleaned.split("&")) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split("=", 2);
            String key = parts[0].trim();
            String value = parts.length > 1 ? parts[1].trim() : "";
            String safeKey = java.util.Objects.requireNonNull(key, "queryParam key");
            String safeValue = java.util.Objects.requireNonNull(value, "queryParam value");
            builder.queryParam(safeKey, safeValue);
        }
        return builder.build(true).toUriString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SepayResponse {
        public Integer status;
        public Object error;
        public SepayMessages messages;
        public List<SepayTransaction> transactions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SepayMessages {
        public Boolean success;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SepayTransaction {
        public String id;
        public String bank_brand_name;
        public String account_number;
        public String transaction_date;
        public String amount_out;
        public String amount_in;
        public String accumulated;
        public String transaction_content;
        public String reference_number;
        public String code;
        public String sub_account;
        public String bank_account_id;

        public BigDecimal getAmountIn() {
            return parseAmount(amount_in);
        }

        public BigDecimal getAmountOut() {
            return parseAmount(amount_out);
        }

        private BigDecimal parseAmount(String value) {
            if (value == null || value.isBlank()) {
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(value.trim());
            } catch (Exception ex) {
                return BigDecimal.ZERO;
            }
        }
    }
}