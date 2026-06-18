package com.example.demo.config;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.example.demo.repository.RoomRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SpecialtyRoomMappingConfig {

    private final Map<String, Long> normalizedToRoomId = new HashMap<>();
    private final RoomRepository roomRepository;

    public SpecialtyRoomMappingConfig(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
        load();
    }

    public void load() {
        try (InputStream in = new ClassPathResource("specialty-room-mapping.json").getInputStream()) {
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> raw = om.readValue(in, new TypeReference<Map<String, Object>>() {
            });
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                String key = normalizeComparableKey(e.getKey());
                Object val = e.getValue();
                Long roomId = null;
                if (val instanceof Number) {
                    roomId = ((Number) val).longValue();
                } else if (val instanceof String) {
                    String s = ((String) val).trim();
                    try {
                        roomId = Long.parseLong(s);
                    } catch (NumberFormatException ex) {
                        // treat as room name, try to resolve
                        roomId = roomRepository.findByRoomNameIgnoreCase(s).map(r -> r.getId()).orElse(null);
                    }
                }

                if (roomId != null) {
                    normalizedToRoomId.put(key, roomId);
                }
            }
        } catch (IOException ex) {
            // Nếu không có mapping hoặc mapping không hợp lệ, hãy để trống mapping.
        }
    }

    public Map<String, Long> getMappings() {
        return Collections.unmodifiableMap(normalizedToRoomId);
    }

    public Long getRoomIdForSpecialty(String specialty) {
        if (specialty == null)
            return null;
        return normalizedToRoomId.get(normalizeComparableKey(specialty));
    }

    private String normalizeComparableKey(String status) {
        if (status == null)
            return null;
        String compact = status.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        String withoutAccents = Normalizer.normalize(compact, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return withoutAccents;
    }
}
