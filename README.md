Facilities Maintenance Management System

A full-stack web application for managing facility maintenance requests, work orders, technician assignments, and maintenance operations.

The system provides a centralized platform where users can submit maintenance requests, supervisors can review and approve work, and technicians can manage assigned work orders. It supports request tracking, automated technician assignment, and role-based maintenance workflows.

Features

* Maintenance Requests: Submit maintenance requests with facility information, descriptions, and building conditions.
* Role-Based Workflows: Separate functionality for requesters, supervisors, and technicians.
* Supervisor Approvals: Review and approve maintenance requests before work begins.
* Automated Technician Assignment: Assign work orders based on technician workload.
* Work Order Management: Update work order statuses and record technician notes.
* Request Tracking: Monitor maintenance requests and review their histories.
* Building Condition Assessment: Select from predefined building-condition options when submitting a request.

Technology Stack

Component	Technology
Frontend	Vue 3, TypeScript, Vuetify, Vite
Backend	Java 21, Spring Boot
Database	PostgreSQL 17
Persistence	Spring Data JPA, Hibernate
API	REST, JSON
Testing	JUnit 5, Mockito
Build Tools	Maven Wrapper, npm
Containerization	Docker Compose
CI/CD	GitHub Actions
Deployment	DigitalOcean, Nginx, HTTPS, systemd

Architecture

The application follows a three-tier architecture:

Frontend: A Vue 3 single-page application provides the user interface and communicates with the backend through HTTP requests.

Backend: A Java Spring Boot application exposes REST endpoints, processes maintenance workflows, implements business logic, and manages database operations.

Database: PostgreSQL stores maintenance requests, work orders, technician information, and related records. Spring Data JPA and Hibernate provide the persistence layer.

Application communication follows this structure:

Vue 3 + TypeScript Frontend
|
| HTTP / JSON
v
Spring Boot REST API
|
| Spring Data JPA / Hibernate
v
PostgreSQL Database

Project Structure

Facilities_Maintenance_Management_System/
├── .github/
│   └── workflows/
│       └── ci.yml
├── backend/
│   ├── .mvn/
│   ├── src/
│   ├── mvnw
│   └── pom.xml
├── frontend/
│   ├── src/
│   ├── package.json
│   └── package-lock.json
├── .gitignore
├── compose.yaml
└── README.md

Getting Started

Prerequisites

The following software is required to run the application locally:

* Java Development Kit (JDK) 21
* Node.js 22
* npm
* Docker with Docker Compose
* Git

Maven does not need to be installed separately because the project includes the Maven Wrapper.

1. Clone the Repository

Clone the repository and navigate to the project directory:

git clone https://github.com/jay-cam/Facilities_Maintenance_Management_System

cd Facilities_Maintenance_Management_System

2. Start PostgreSQL

The project includes a Docker Compose configuration for PostgreSQL 17.

From the project root, run:

docker compose up -d

Docker Compose creates a PostgreSQL container named facility-maintenance-db with the following local development configuration:

Setting	Value
Database	facility_maintenance
Username	facility_admin
Password	dev_password
Host	localhost
Port	5433

PostgreSQL data is persisted using a Docker volume named postgres_data.

The database configuration is intended for local development. Production deployments should use separate credentials and environment-specific configuration.

3. Start the Backend

Open a terminal and navigate to the backend directory:

cd backend

Start the Spring Boot application using the Maven Wrapper:

./mvnw spring-boot:run

The backend uses the following default database configuration:

spring.datasource.url=jdbc:postgresql://localhost:5433/facility_maintenance
spring.datasource.username=facility_admin
spring.datasource.password=dev_password

These defaults can be overridden using environment variables:

Variable	Purpose
DB_URL	PostgreSQL JDBC connection URL
DB_USERNAME	Database username
DB_PASSWORD	Database password

For example:

export DB_URL=jdbc:postgresql://localhost:5433/facility_maintenance
export DB_USERNAME=facility_admin
export DB_PASSWORD=dev_password

The backend uses Spring Data JPA and Hibernate to manage database persistence.

The application is configured with:

spring.jpa.hibernate.ddl-auto=update

This allows Hibernate to update the database schema during development.

By default, Spring Boot serves the backend on:

http://localhost:8080

4. Start the Frontend

Open a separate terminal and navigate to the frontend directory:

cd frontend

Install dependencies:

npm install

Start the Vite development server:

npm run dev

By default, Vite serves the frontend on:

http://localhost:5173

Open this address in your browser to access the application.

5. Stop the Application

Stop the frontend and backend development servers using Ctrl + C in their respective terminals.

To stop the PostgreSQL container, run the following from the project root:

docker compose down

The PostgreSQL data volume is preserved unless explicitly removed.

Testing

Backend unit tests use JUnit 5 and Mockito.

The project includes tests for maintenance request service behavior that can run without an active PostgreSQL connection.

To run the verified service tests:

cd backend
./mvnw -Dtest=MaintenanceRequestServiceTest test

To package the backend application:

./mvnw -DskipTests package

To build the frontend:

cd frontend
npm ci
npm run build

Continuous Integration

GitHub Actions provides automated validation through the workflow defined in:

.github/workflows/ci.yml

The workflow runs when code is pushed to the main branch or when a pull request targets main.

Backend workflow:

* Configures Java 21.
* Runs JUnit unit tests.
* Packages the Spring Boot application using Maven.

Frontend workflow:

* Configures Node.js 22.
* Installs dependencies using npm ci.
* Builds the Vue application using Vite and TypeScript.

These checks help identify build failures and test regressions before changes are merged.

Deployment

The application is deployed on a DigitalOcean server.

The production environment uses:

* Nginx: Reverse proxy and frontend hosting.
* Spring Boot: Backend application managed through systemd.
* PostgreSQL: Relational database.
* HTTPS: Encrypted communication.
* DigitalOcean: Application hosting infrastructure.

The deployed application is available at:

https://facilities.jaycampbell.dev

Project Purpose

This project was developed to demonstrate full-stack application development using technologies commonly found in enterprise environments.

It emphasizes REST API development, relational database integration, maintainable application architecture, automated testing, and deployment.

The application also demonstrates adapting an existing system to accommodate changing functional requirements.