package com.koicoffee.backend.model;

import jakarta.persistence.*; // Nếu bạn dùng Spring Boot 2.x, đổi chữ 'jakarta' thành 'javax'
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shift_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_name")
    private String staffName;

    @Column(name = "initial_cash")
    private Integer initialCash;

    @Column(name = "cash_revenue")
    private Integer cashRevenue;

    @Column(name = "transfer_revenue")
    private Integer transferRevenue;

    @Column(name = "total_revenue")
    private Integer totalRevenue;

    @Column(name = "actual_cash")
    private Integer actualCash;

    @Column(name = "variance")
    private Integer variance;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "report_time")
    private LocalDateTime reportTime;
}