package com.facilitymaintenance.backend.dto;

import com.facilitymaintenance.backend.model.EquipmentCondition;
import com.facilitymaintenance.backend.model.RequestPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMaintenanceRequest(
        @NotBlank(message = "Title is required.")
        @Size(max = 150, message = "Title cannot exceed 150 characters.")
        String title,

        @NotBlank(message = "Description is required.")
        String description,

        @NotBlank(message = "Building number is required.")
        @Size(max = 50, message = "Building number cannot exceed 50 characters.")
        String buildingNumber,

        @NotNull(message = "Priority is required.")
        RequestPriority priority,

        @NotNull(message = "Equipment condition is required.")
        EquipmentCondition condition

) {}
