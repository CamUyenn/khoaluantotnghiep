package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomName(String roomName);

    Optional<Room> findByRoomNameIgnoreCase(String roomName);

    boolean existsByRoomNameIgnoreCase(String roomName);

    List<Room> findAllByOrderByRoomNameAsc();

    List<Room> findByCurrentDoctor_Id(Long doctorUserId);
}
