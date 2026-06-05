package com.example.demo.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "MEDICINES")
@Data
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "medicine_name", nullable = false, length = 200)
    private String medicineName;

    @Column(name = "medicine_type", nullable = false, length = 100)
    private String medicineType = "Khác";

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "selling_price", precision = 18, scale = 0)
    private BigDecimal sellingPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
