package com.koicoffee.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "staff_name")
    private String staffName;

    @Column(name = "status")
    private String status = "OPEN";

    @Column(name = "initial_cash")
    private Integer initialCash = 0;

    @Column(name = "batch_cash_revenue")
    private Integer batchCashRevenue = 0;

    @Column(name = "transfer_revenue")
    private Integer transferRevenue = 0;

    @Column(name = "actual_cash")
    private Integer actualCash = 0;

    @Column(name = "variance")
    private Integer variance = 0;

    @Column(name = "total_revenue")
    private Integer totalRevenue = 0;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;
}