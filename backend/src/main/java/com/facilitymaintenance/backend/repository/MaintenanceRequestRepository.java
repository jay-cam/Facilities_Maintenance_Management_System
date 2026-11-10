package com.facilitymaintenance.backend.repository;

import com.facilitymaintenance.backend.model.MaintenanceRequest;
import org.apache.tomcat.util.http.fileupload.util.LimitedInputStream;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    List<MaintenanceRequest> findByDepartmentId(Long departmentId);

}
