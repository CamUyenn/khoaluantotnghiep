package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.RoomSchedule;

public interface RoomScheduleRepository extends JpaRepository<RoomSchedule, Long> {
    List<RoomSchedule> findByScheduleDateBetweenOrderByScheduleDateAscStartTimeAsc(LocalDate start, LocalDate end);

    List<RoomSchedule> findByScheduleDateOrderByStartTimeAsc(LocalDate scheduleDate);

    List<RoomSchedule> findByDoctor_IdAndScheduleDate(Long doctorId, LocalDate scheduleDate);

    List<RoomSchedule> findByRoom_IdAndScheduleDate(Long roomId, LocalDate scheduleDate);
}
