package com.facilitymaintenance.backend.service;

import com.facilitymaintenance.backend.dto.CreateMaintenanceRequest;
import com.facilitymaintenance.backend.model.*;
import com.facilitymaintenance.backend.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MaintenanceRequestService.
 *
 * These tests validate three core business workflows:
 *
 * 1. Creating a maintenance request.
 * 2. Approving a maintenance request.
 * 3. Automatically assigning the least-busy technician.
 *
 * Mockito replaces the database repositories with mock objects.
 *
 * No real database connections are made, and no production
 * or development data is modified.
 */
@ExtendWith(MockitoExtension.class)
class MaintenanceRequestServiceTest {

    // --------------------------------------------------
    // Mock Repositories
    // --------------------------------------------------

    @Mock
    private MaintenanceRequestRepository repository;

    @Mock
    private WorkOrderHistoryRepository historyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkOrderCommentRepository commentRepository;

    /**
     * Mockito constructs the service and injects
     * the mocked repository dependencies.
     */
    @InjectMocks
    private MaintenanceRequestService service;

    private Department department;
    private User requester;
    private User supervisor;

    /**
     * Prepare reusable test data before each test.
     *
     * IDs are assigned using reflection because our JPA
     * entities intentionally do not expose public ID setters.
     */
    @BeforeEach
    void setUp() {

        department = new Department("Waterfront Operations");
        setId(department, 1L);

        requester = new User(
                "John Smith",
                "john.smith@example.com",
                UserRole.REQUESTER
        );

        setId(requester, 1L);
        requester.setDepartment(department);

        supervisor = new User(
                "Robert Jones",
                "robert.jones@example.com",
                UserRole.SUPERVISOR
        );

        setId(supervisor, 3L);
        supervisor.setDepartment(department);
    }

    /**
     * Assigns an ID to a JPA entity for testing.
     *
     * In production, PostgreSQL generates these IDs.
     * In unit tests, we simulate existing database records.
     */
    private void setId(Object entity, Long id) {

        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);

        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(
                    "Unable to assign test entity ID.",
                    exception
            );
        }
    }

    /**
     * Creates a technician with a known ID.
     */
    private User createTechnician(
            Long id,
            String name
    ) {

        User technician = new User(
                name,
                name.toLowerCase().replace(" ", ".") + "@example.com",
                UserRole.TECHNICIAN
        );

        setId(technician, id);

        return technician;
    }

    /**
     * Creates a maintenance request awaiting approval.
     *
     * This helper is reused by our approval and
     * automatic assignment tests.
     */
    private MaintenanceRequest createPendingRequest() {

        MaintenanceRequest request = new MaintenanceRequest();

        setId(request, 10L);

        request.setRequester(requester);
        request.setDepartment(department);

        request.setStatus(RequestStatus.OPEN);
        request.setApprovalStatus(ApprovalStatus.PENDING_APPROVAL);

        return request;
    }


    // --------------------------------------------------
    // TEST 1: Create Maintenance Request
    // --------------------------------------------------

    /**
     * Verifies that submitting a new maintenance request:
     *
     * - Assigns the correct requester.
     * - Associates the requester's department.
     * - Sets the initial status to OPEN.
     * - Sets approval status to PENDING_APPROVAL.
     * - Leaves the technician unassigned.
     * - Creates an audit history entry.
     */
    @Test
    void createRequest_shouldInitializeCorrectly() {

        // Arrange: Simulate an existing requester.
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(requester));

        // Simulate PostgreSQL returning the saved request.
        when(repository.save(any(MaintenanceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Prepare the request submission.
        CreateMaintenanceRequest requestData =
                new CreateMaintenanceRequest(
                        "Broken Air Conditioning",
                        "The air conditioning unit is not cooling.",
                        "Building 100",
                        RequestPriority.URGENT,
                        EquipmentCondition.POOR
                );

        // Act: Submit the maintenance request.
        MaintenanceRequest result =
                service.createRequest(requestData, 1L);

        // Assert: Verify the initial workflow state.
        assertEquals(RequestStatus.OPEN, result.getStatus());

        assertEquals(
                ApprovalStatus.PENDING_APPROVAL,
                result.getApprovalStatus()
        );

        assertEquals(requester, result.getRequester());
        assertEquals(department, result.getDepartment());

        assertNull(result.getAssignedTechnician());

        // Verify that the request was saved.
        verify(repository).save(any(MaintenanceRequest.class));

        // Verify that an audit history entry was created.
        verify(historyRepository).save(any(WorkOrderHistory.class));
    }


    // --------------------------------------------------
    // TEST 2: Supervisor Approval
    // --------------------------------------------------

    /**
     * Verifies that a supervisor can approve a pending
     * maintenance request belonging to their department.
     *
     * Approval should:
     *
     * - Change approval status to APPROVED.
     * - Record the approving supervisor.
     * - Record the approval timestamp.
     * - Assign an available technician.
     * - Create an audit history entry.
     */
    @Test
    void approveRequest_shouldApproveAndRecordSupervisor() {

        // Arrange: Create a pending request.
        MaintenanceRequest request = createPendingRequest();

        // Create an available technician.
        User technician = createTechnician(2L, "Jane Doe");

        when(repository.findById(10L))
                .thenReturn(Optional.of(request));

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(supervisor));

        when(userRepository.findByRole(UserRole.TECHNICIAN))
                .thenReturn(List.of(technician));


        when(repository.save(any(MaintenanceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Supervisor approves the request.
        MaintenanceRequest result =
                service.approveRequest(10L, 3L);

        // Assert: Verify approval.
        assertEquals(
                ApprovalStatus.APPROVED,
                result.getApprovalStatus()
        );

        assertEquals(supervisor, result.getApprovedBy());

        assertNotNull(result.getApprovedAt());

        assertEquals(technician, result.getAssignedTechnician());

        // Verify that the approved request was saved.
        verify(repository).save(request);

        // Verify that an audit history entry was created.
        verify(historyRepository).save(any(WorkOrderHistory.class));
    }


    // --------------------------------------------------
    // TEST 3: Automatic Technician Assignment
    // --------------------------------------------------

    /**
     * Verifies that the workload-balancing algorithm
     * selects the technician with the fewest active
     * maintenance requests.
     *
     * Example workloads:
     *
     * Jane:   3 active work orders
     * James:  1 active work order
     * Daniel: 2 active work orders
     *
     * Expected assignment: James.
     *
     * The test also verifies that each technician's
     * active workload is queried.
     */
    @Test
    void approveRequest_shouldAssignLeastBusyTechnician() {

        // Arrange: Create a pending request.
        MaintenanceRequest request = createPendingRequest();

        User jane = createTechnician(2L, "Jane Doe");
        User james = createTechnician(8L, "James Wilson");
        User daniel = createTechnician(9L, "Daniel Rodriguez");

        when(repository.findById(10L))
                .thenReturn(Optional.of(request));

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(supervisor));

        when(userRepository.findByRole(UserRole.TECHNICIAN))
                .thenReturn(List.of(jane, james, daniel));

        // Simulate each technician's active workload.
        when(repository.countByAssignedTechnicianIdAndStatusNot(
                2L,
                RequestStatus.COMPLETED
        )).thenReturn(3L);

        when(repository.countByAssignedTechnicianIdAndStatusNot(
                8L,
                RequestStatus.COMPLETED
        )).thenReturn(1L);

        when(repository.countByAssignedTechnicianIdAndStatusNot(
                9L,
                RequestStatus.COMPLETED
        )).thenReturn(2L);

        when(repository.save(any(MaintenanceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Approve the request and trigger assignment.
        MaintenanceRequest result =
                service.approveRequest(10L, 3L);

        // Assert: James should receive the work order.
        assertEquals(
                8L,
                result.getAssignedTechnician().getId()
        );

        assertEquals(
                "James Wilson",
                result.getAssignedTechnician().getName()
        );

        // Verify the workload of each technician was checked.
        verify(repository, atLeastOnce()).countByAssignedTechnicianIdAndStatusNot(
                2L,
                RequestStatus.COMPLETED
        );

        verify(repository, atLeastOnce()).countByAssignedTechnicianIdAndStatusNot(
                8L,
                RequestStatus.COMPLETED
        );

        verify(repository, atLeastOnce()).countByAssignedTechnicianIdAndStatusNot(
                9L,
                RequestStatus.COMPLETED
        );

        // Verify that the assignment was saved.
        verify(repository).save(request);
    }
}