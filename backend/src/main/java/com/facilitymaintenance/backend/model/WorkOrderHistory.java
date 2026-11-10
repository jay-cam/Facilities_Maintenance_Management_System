package com.facilitymaintenance.backend.model;

import com.sun.tools.javac.Main;
import jakarta.persistence.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_order_history")
public class WorkOrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private MaintenanceRequest maintenanceRequest;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public WorkOrderHistory(){}

    public WorkOrderHistory(MaintenanceRequest maintenanceRequest, User actor, RequestStatus status, String note){
        this.maintenanceRequest = maintenanceRequest;
        this.actor = actor;
        this.status = status;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId(){
        return id;
    }

    public MaintenanceRequest getMaintenanceRequest() {
        return maintenanceRequest;
    }

    public RequestStatus getStatus(){
        return status;
    }

    public String getNote(){
        return note;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public User getActor(){
        return actor;
    }
}
