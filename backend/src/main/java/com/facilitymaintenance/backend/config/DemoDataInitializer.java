package com.facilitymaintenance.backend.config;

import com.facilitymaintenance.backend.model.User;
import com.facilitymaintenance.backend.model.UserRole;
import com.facilitymaintenance.backend.repository.UserRepository;
import com.facilitymaintenance.backend.model.Department;
import com.facilitymaintenance.backend.repository.DepartmentRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.jackson.JacksonMixinModuleEntries;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoDataInitializer {

    @Bean
    CommandLineRunner initializeUsers(UserRepository userRepository, DepartmentRepository departmentRepository, JacksonMixinModuleEntries jacksonMixinModuleEntries){
        return args -> {

            // Initialize demo users.
            if (!userRepository.existsByEmail("john.smith@example.com")) {
                userRepository.save(new User(
                        "John Smith",
                        "john.smith@example.com",
                        UserRole.REQUESTER
                ));
            }

            if(!userRepository.existsByEmail("jane.doe@example.com")){
                userRepository.save(new User(
                        "Jane Doe",
                        "jane.doe@example.com",
                        UserRole.TECHNICIAN
                ));
            }

            if(!userRepository.existsByEmail("robert.jones@example.com")){
                userRepository.save(new User(
                        "Robert Jones",
                        "robert.jones@example.com",
                        UserRole.SUPERVISOR
                ));
            }

            // Initialize demo departments
            if (departmentRepository.count() == 0) {

                departmentRepository.save(new Department("Waterfront Operations"));
                departmentRepository.save(new Department("Port Operations"));
                departmentRepository.save(new Department("Ship Maintenance & Repair"));
                departmentRepository.save(new Department("Logistics & Supply"));
                departmentRepository.save(new Department("Security Operations"));
                departmentRepository.save(new Department("Administrative Services"));
            }

            // Assign departments to existing demo users.
            Department waterfront = departmentRepository.findByName("Waterfront Operations").orElseThrow();

            Department shipMaintenance = departmentRepository.findByName("Ship Maintenance & Repair").orElseThrow();

            // John Smith
            User john = userRepository.findByEmail("john.smith@example.com").orElseThrow();

            john.setDepartment(waterfront);
            userRepository.save(john);

            // Jane Doe
            User jane = userRepository.findByEmail("jane.doe@example.com").orElseThrow();

            jane.setDepartment(shipMaintenance);
            userRepository.save(jane);

            // Robert Jones
            User robert = userRepository.findByEmail("robert.jones@example.com").orElseThrow();

            robert.setDepartment(waterfront);
            userRepository.save(robert);
        };
    }
}
