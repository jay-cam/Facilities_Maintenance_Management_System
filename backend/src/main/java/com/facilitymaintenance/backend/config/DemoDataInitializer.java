package com.facilitymaintenance.backend.config;

import com.facilitymaintenance.backend.model.Department;
import com.facilitymaintenance.backend.model.User;
import com.facilitymaintenance.backend.model.UserRole;
import com.facilitymaintenance.backend.repository.DepartmentRepository;
import com.facilitymaintenance.backend.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoDataInitializer {

    @Bean
    CommandLineRunner initializeUsers(
            UserRepository userRepository,
            DepartmentRepository departmentRepository
    ) {
        return args -> {

            // --------------------------------------------------
            // 1. Initialize departments
            // --------------------------------------------------

            String[] departmentNames = {
                    "Waterfront Operations",
                    "Port Operations",
                    "Ship Maintenance & Repair",
                    "Logistics & Supply",
                    "Security Operations",
                    "Administrative Services",
                    "Service Technicians"
            };

            for (String departmentName : departmentNames) {
                if (departmentRepository.findByName(departmentName).isEmpty()) {
                    departmentRepository.save(
                            new Department(departmentName)
                    );
                }
            }

            // --------------------------------------------------
            // 2. Retrieve departments
            // --------------------------------------------------

            Department waterfront = departmentRepository
                    .findByName("Waterfront Operations")
                    .orElseThrow();

            Department portOperations = departmentRepository
                    .findByName("Port Operations")
                    .orElseThrow();

            Department logistics = departmentRepository
                    .findByName("Logistics & Supply")
                    .orElseThrow();

            Department serviceTechnicians = departmentRepository
                    .findByName("Service Technicians")
                    .orElseThrow();

            // --------------------------------------------------
            // 3. Waterfront Operations
            // --------------------------------------------------

            createOrUpdateUser(
                    userRepository,
                    "John Smith",
                    "john.smith@example.com",
                    UserRole.REQUESTER,
                    waterfront
            );

            createOrUpdateUser(
                    userRepository,
                    "Robert Jones",
                    "robert.jones@example.com",
                    UserRole.SUPERVISOR,
                    waterfront
            );

            // --------------------------------------------------
            // 4. Port Operations
            // --------------------------------------------------

            createOrUpdateUser(
                    userRepository,
                    "Sarah Mitchell",
                    "sarah.mitchell@example.com",
                    UserRole.REQUESTER,
                    portOperations
            );

            createOrUpdateUser(
                    userRepository,
                    "Michael Torres",
                    "michael.torres@example.com",
                    UserRole.SUPERVISOR,
                    portOperations
            );

            // --------------------------------------------------
            // 5. Logistics & Supply
            // --------------------------------------------------

            createOrUpdateUser(
                    userRepository,
                    "Emily Carter",
                    "emily.carter@example.com",
                    UserRole.REQUESTER,
                    logistics
            );

            createOrUpdateUser(
                    userRepository,
                    "David Chen",
                    "david.chen@example.com",
                    UserRole.SUPERVISOR,
                    logistics
            );

            // --------------------------------------------------
            // 6. Service Technicians
            // --------------------------------------------------

            createOrUpdateUser(
                    userRepository,
                    "Jane Doe",
                    "jane.doe@example.com",
                    UserRole.TECHNICIAN,
                    serviceTechnicians
            );

            createOrUpdateUser(
                    userRepository,
                    "James Wilson",
                    "james.wilson@example.com",
                    UserRole.TECHNICIAN,
                    serviceTechnicians
            );

            createOrUpdateUser(
                    userRepository,
                    "Daniel Rodriguez",
                    "daniel.rodriguez@example.com",
                    UserRole.TECHNICIAN,
                    serviceTechnicians
            );

        };
    }

    private void createOrUpdateUser(
            UserRepository userRepository,
            String name,
            String email,
            UserRole role,
            Department department
    ) {

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> new User(name, email, role));

        user.setName(name);
        user.setRole(role);
        user.setDepartment(department);

        userRepository.save(user);
    }
}