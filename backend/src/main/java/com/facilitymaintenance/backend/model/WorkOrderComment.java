package com.facilitymaintenance.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "work_order_comments")
public class WorkOrderComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private MaintenanceRequest maintenanceRequest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WorkOrderComment(){}

    public WorkOrderComment(MaintenanceRequest maintenanceRequest, User author, String content){

        this.maintenanceRequest = maintenanceRequest;
        this.author = author;
        this.content = content;
    }
    @PrePersist
    public void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    public Long getId(){
        return id;
    }

    public MaintenanceRequest getMaintenanceRequest(){
        return maintenanceRequest;
    }

    public User getAuthor(){
        return author;
    }

    public String getContent(){
        return content;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }
}
