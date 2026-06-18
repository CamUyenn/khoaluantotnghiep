package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {
	Optional<Patient> findByNationalId(String nationalId);

	Optional<Patient> findByUserId(Long userId);

	Optional<Patient> findByGmailIgnoreCase(String gmail);

	boolean existsByNationalId(String nationalId);

	boolean existsByPhoneNumber(String phoneNumber);

	boolean existsByGmail(String gmail);
}
