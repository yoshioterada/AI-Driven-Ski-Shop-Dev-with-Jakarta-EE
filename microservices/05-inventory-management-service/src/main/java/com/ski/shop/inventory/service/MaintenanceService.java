package com.ski.shop.inventory.service;

import com.ski.shop.inventory.domain.MaintenanceRecord;
import com.ski.shop.inventory.domain.Equipment;
import com.ski.shop.inventory.dto.MaintenanceRecordDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing equipment maintenance records and scheduling
 */
@ApplicationScoped
public class MaintenanceService {

    @Inject
    AlertService alertService;

    /**
     * Create a new maintenance record
     */
    @Transactional
    public MaintenanceRecordDto createMaintenanceRecord(MaintenanceRecordDto.CreateMaintenanceRequest request) {
        // Validate equipment exists
        Equipment equipment = Equipment.findById(request.equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found: " + request.equipmentId);
        }

        MaintenanceRecord record = new MaintenanceRecord();
        record.equipmentId = request.equipmentId;
        record.maintenanceType = request.maintenanceType;
        record.title = request.title;
        record.description = request.description;
        record.scheduledDate = request.scheduledDate;
        record.technicianId = request.technicianId;
        record.estimatedDurationMinutes = request.estimatedDurationMinutes;
        record.priority = request.priority != null ? request.priority : 3;
        record.cost = request.cost;
        record.nextMaintenanceDate = request.nextMaintenanceDate;

        record.persist();

        // Create alert if maintenance is due soon
        checkAndCreateMaintenanceAlerts(record);

        return new MaintenanceRecordDto(record);
    }

    /**
     * Get maintenance record by ID
     */
    public Optional<MaintenanceRecordDto> getMaintenanceRecord(UUID maintenanceId) {
        return MaintenanceRecord.find("maintenanceId", maintenanceId)
                .firstResultOptional()
                .map(record -> new MaintenanceRecordDto((MaintenanceRecord) record));
    }

    /**
     * Update maintenance record
     */
    @Transactional
    public Optional<MaintenanceRecordDto> updateMaintenanceRecord(UUID maintenanceId, 
                                                                MaintenanceRecordDto.UpdateMaintenanceRequest request) {
        Optional<MaintenanceRecord> recordOpt = MaintenanceRecord.find("maintenanceId", maintenanceId)
                .firstResultOptional();
        
        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        MaintenanceRecord record = recordOpt.get();
        
        if (request.status != null) record.status = request.status;
        if (request.description != null) record.description = request.description;
        if (request.scheduledDate != null) record.scheduledDate = request.scheduledDate;
        if (request.technicianId != null) record.technicianId = request.technicianId;
        if (request.estimatedDurationMinutes != null) record.estimatedDurationMinutes = request.estimatedDurationMinutes;
        if (request.priority != null) record.priority = request.priority;
        if (request.cost != null) record.cost = request.cost;
        if (request.partsUsed != null) record.partsUsed = request.partsUsed;
        if (request.notes != null) record.notes = request.notes;
        if (request.nextMaintenanceDate != null) record.nextMaintenanceDate = request.nextMaintenanceDate;

        record.persist();

        return Optional.of(new MaintenanceRecordDto(record));
    }

    /**
     * Start maintenance work
     */
    @Transactional
    public Optional<MaintenanceRecordDto> startMaintenance(UUID maintenanceId, 
                                                         MaintenanceRecordDto.StartMaintenanceRequest request) {
        Optional<MaintenanceRecord> recordOpt = MaintenanceRecord.find("maintenanceId", maintenanceId)
                .firstResultOptional();
        
        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        MaintenanceRecord record = recordOpt.get();
        
        if (record.status != MaintenanceRecord.MaintenanceStatus.SCHEDULED) {
            throw new IllegalStateException("Maintenance is not in scheduled status");
        }

        record.startMaintenance(request.technicianId);
        record.persist();

        return Optional.of(new MaintenanceRecordDto(record));
    }

    /**
     * Complete maintenance work
     */
    @Transactional
    public Optional<MaintenanceRecordDto> completeMaintenance(UUID maintenanceId, 
                                                            MaintenanceRecordDto.CompleteMaintenanceRequest request) {
        Optional<MaintenanceRecord> recordOpt = MaintenanceRecord.find("maintenanceId", maintenanceId)
                .firstResultOptional();
        
        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        MaintenanceRecord record = recordOpt.get();
        
        if (record.status != MaintenanceRecord.MaintenanceStatus.IN_PROGRESS) {
            throw new IllegalStateException("Maintenance is not in progress");
        }

        record.completeMaintenance(request.notes, request.cost, request.partsUsed);
        
        // Schedule next maintenance if specified
        if (request.nextMaintenanceDate != null) {
            record.nextMaintenanceDate = request.nextMaintenanceDate;
            
            // Create future maintenance record
            scheduleNextMaintenance(record, request.nextMaintenanceDate);
        }

        record.persist();

        return Optional.of(new MaintenanceRecordDto(record));
    }

    /**
     * Get maintenance records for equipment
     */
    public List<MaintenanceRecordDto> getMaintenanceRecordsForEquipment(Long equipmentId, int page, int size) {
        return MaintenanceRecord.find("equipmentId = ?1 ORDER BY scheduledDate DESC", equipmentId)
                .page(Page.of(page, size))
                .list()
                .stream()
                .map(record -> new MaintenanceRecordDto((MaintenanceRecord) record))
                .collect(Collectors.toList());
    }

    /**
     * Get overdue maintenance records
     */
    public List<MaintenanceRecordDto> getOverdueMaintenanceRecords() {
        return MaintenanceRecord.find("status = ?1 AND scheduledDate < ?2", 
                MaintenanceRecord.MaintenanceStatus.SCHEDULED, LocalDateTime.now())
                .list()
                .stream()
                .map(record -> new MaintenanceRecordDto((MaintenanceRecord) record))
                .collect(Collectors.toList());
    }

    /**
     * Get upcoming maintenance records (within specified days)
     */
    public List<MaintenanceRecordDto> getUpcomingMaintenanceRecords(int days) {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(days);
        return MaintenanceRecord.find("status = ?1 AND scheduledDate BETWEEN ?2 AND ?3", 
                MaintenanceRecord.MaintenanceStatus.SCHEDULED, LocalDateTime.now(), futureDate)
                .list()
                .stream()
                .map(record -> new MaintenanceRecordDto((MaintenanceRecord) record))
                .collect(Collectors.toList());
    }

    /**
     * Generate maintenance schedule for equipment based on usage and type
     */
    @Transactional
    public void generateMaintenanceSchedule(Long equipmentId, int monthsAhead) {
        Equipment equipment = Equipment.findById(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found: " + equipmentId);
        }

        LocalDateTime startDate = LocalDateTime.now().plusMonths(1);
        
        // Generate preventive maintenance schedules based on equipment type
        for (int i = 0; i < monthsAhead; i++) {
            LocalDateTime maintenanceDate = startDate.plusMonths(i * 3); // Quarterly maintenance
            
            // Check if maintenance already scheduled for this period
            long existingCount = MaintenanceRecord.count(
                "equipmentId = ?1 AND scheduledDate BETWEEN ?2 AND ?3 AND status = ?4",
                equipmentId,
                maintenanceDate.minusDays(15),
                maintenanceDate.plusDays(15),
                MaintenanceRecord.MaintenanceStatus.SCHEDULED
            );
            
            if (existingCount == 0) {
                MaintenanceRecord record = new MaintenanceRecord();
                record.equipmentId = equipmentId;
                record.maintenanceType = MaintenanceRecord.MaintenanceType.PREVENTIVE;
                record.title = "Scheduled Preventive Maintenance - " + equipment.cachedName;
                record.description = "Quarterly preventive maintenance for " + equipment.cachedEquipmentType;
                record.scheduledDate = maintenanceDate;
                record.estimatedDurationMinutes = 120; // 2 hours default
                record.priority = 2; // Medium priority
                
                record.persist();
            }
        }
    }

    /**
     * Check and create maintenance alerts
     */
    private void checkAndCreateMaintenanceAlerts(MaintenanceRecord record) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime alertThreshold = now.plusDays(7); // Alert 7 days before
        
        if (record.scheduledDate.isBefore(alertThreshold)) {
            // Create maintenance due alert
            alertService.createMaintenanceDueAlert(record.equipmentId, record.maintenanceId, record.scheduledDate);
        }
    }

    /**
     * Schedule next maintenance
     */
    private void scheduleNextMaintenance(MaintenanceRecord completedRecord, LocalDateTime nextDate) {
        MaintenanceRecord nextRecord = new MaintenanceRecord();
        nextRecord.equipmentId = completedRecord.equipmentId;
        nextRecord.maintenanceType = completedRecord.maintenanceType;
        nextRecord.title = "Follow-up " + completedRecord.title;
        nextRecord.description = "Scheduled follow-up maintenance";
        nextRecord.scheduledDate = nextDate;
        nextRecord.estimatedDurationMinutes = completedRecord.estimatedDurationMinutes;
        nextRecord.priority = completedRecord.priority;
        
        nextRecord.persist();
    }

    /**
     * Get maintenance statistics
     */
    public MaintenanceStatistics getMaintenanceStatistics() {
        MaintenanceStatistics stats = new MaintenanceStatistics();
        
        stats.totalRecords = MaintenanceRecord.count();
        stats.scheduledRecords = MaintenanceRecord.count("status", MaintenanceRecord.MaintenanceStatus.SCHEDULED);
        stats.inProgressRecords = MaintenanceRecord.count("status", MaintenanceRecord.MaintenanceStatus.IN_PROGRESS);
        stats.completedRecords = MaintenanceRecord.count("status", MaintenanceRecord.MaintenanceStatus.COMPLETED);
        stats.overdueRecords = MaintenanceRecord.count("status = ?1 AND scheduledDate < ?2", 
                MaintenanceRecord.MaintenanceStatus.SCHEDULED, LocalDateTime.now());
        stats.upcomingRecords = MaintenanceRecord.count("status = ?1 AND scheduledDate BETWEEN ?2 AND ?3",
                MaintenanceRecord.MaintenanceStatus.SCHEDULED, LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        
        return stats;
    }

    /**
     * Maintenance statistics DTO
     */
    public static class MaintenanceStatistics {
        public long totalRecords;
        public long scheduledRecords;
        public long inProgressRecords;
        public long completedRecords;
        public long overdueRecords;
        public long upcomingRecords;
    }
}