package com.facilitymaintenance.backend.repository;

import com.facilitymaintenance.backend.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.SplittableRandom;


public interface DepartmentRepository extends JpaRepository<Department, Long>{

    Optional<Department> findByName(String name);

}
