package com.example.demo.config.seeder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Room;
import com.example.demo.repository.RoomRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoomSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomSeeder.class);

    private static final List<String> DEFAULT_ROOM_NAMES = List.of(
            "Phòng 101",
            "Phòng 102",
            "Phòng 201",
            "Phòng 202");

    @Value("${app.seed.rooms.enabled:true}")
    private boolean defaultRoomsEnabled;

    private final RoomRepository roomRepository;

    @Transactional
    public void seed() {
        if (!defaultRoomsEnabled) {
            return;
        }

        int inserted = 0;
        for (String roomName : DEFAULT_ROOM_NAMES) {
            if (roomRepository.existsByRoomNameIgnoreCase(roomName)) {
                continue;
            }

            Room room = new Room();
            room.setRoomName(roomName);
            roomRepository.save(room);
            inserted++;
        }

        LOGGER.info("[RoomSeeder] Seeded rooms: {} newly inserted", inserted);
    }
}
