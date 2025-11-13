-- Create maintenance_records table
CREATE TABLE maintenance_records (
    id BIGSERIAL PRIMARY KEY,
    maintenance_id UUID NOT NULL UNIQUE,
    equipment_id BIGINT NOT NULL,
    maintenance_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    scheduled_date TIMESTAMP NOT NULL,
    started_date TIMESTAMP,
    completed_date TIMESTAMP,
    technician_id VARCHAR(100),
    estimated_duration_minutes INTEGER,
    actual_duration_minutes INTEGER,
    priority INTEGER NOT NULL DEFAULT 3,
    cost DECIMAL(10,2),
    parts_used VARCHAR(500),
    notes VARCHAR(2000),
    next_maintenance_date TIMESTAMP,
    version_field BIGINT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_maintenance_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE CASCADE,
    CONSTRAINT chk_maintenance_type CHECK (maintenance_type IN ('PREVENTIVE', 'CORRECTIVE', 'SAFETY_INSPECTION', 'CALIBRATION', 'CLEANING', 'REPAIR', 'REPLACEMENT')),
    CONSTRAINT chk_maintenance_status CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'POSTPONED')),
    CONSTRAINT chk_maintenance_priority CHECK (priority BETWEEN 1 AND 5)
);

-- Create inventory_alerts table
CREATE TABLE inventory_alerts (
    id BIGSERIAL PRIMARY KEY,
    alert_id UUID NOT NULL UNIQUE,
    equipment_id BIGINT,
    alert_type VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    threshold_value INTEGER,
    current_value INTEGER,
    assigned_to VARCHAR(100),
    acknowledged_at TIMESTAMP,
    acknowledged_by VARCHAR(100),
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    resolution_notes VARCHAR(1000),
    auto_generated BOOLEAN NOT NULL DEFAULT TRUE,
    notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    version_field BIGINT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_alert_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE CASCADE,
    CONSTRAINT chk_alert_type CHECK (alert_type IN ('LOW_STOCK', 'OUT_OF_STOCK', 'MAINTENANCE_DUE', 'MAINTENANCE_OVERDUE', 'EQUIPMENT_MALFUNCTION', 'RESERVATION_EXPIRING', 'SYNC_FAILURE', 'SYSTEM_ERROR', 'PERFORMANCE_ISSUE', 'SECURITY_ALERT')),
    CONSTRAINT chk_alert_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_alert_status CHECK (status IN ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED', 'DISMISSED'))
);

-- Create indexes for maintenance_records
CREATE INDEX idx_maintenance_equipment_id ON maintenance_records(equipment_id);
CREATE INDEX idx_maintenance_status ON maintenance_records(status);
CREATE INDEX idx_maintenance_scheduled_date ON maintenance_records(scheduled_date);
CREATE INDEX idx_maintenance_type ON maintenance_records(maintenance_type);
CREATE INDEX idx_maintenance_priority ON maintenance_records(priority);
CREATE INDEX idx_maintenance_technician ON maintenance_records(technician_id);

-- Create indexes for inventory_alerts
CREATE INDEX idx_alert_equipment_id ON inventory_alerts(equipment_id);
CREATE INDEX idx_alert_type ON inventory_alerts(alert_type);
CREATE INDEX idx_alert_severity ON inventory_alerts(severity);
CREATE INDEX idx_alert_status ON inventory_alerts(status);
CREATE INDEX idx_alert_created_at ON inventory_alerts(created_at);
CREATE INDEX idx_alert_assigned_to ON inventory_alerts(assigned_to);

-- Add trigger to update updated_at timestamp for maintenance_records
CREATE OR REPLACE FUNCTION update_maintenance_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER maintenance_updated_at_trigger
    BEFORE UPDATE ON maintenance_records
    FOR EACH ROW
    EXECUTE FUNCTION update_maintenance_updated_at();

-- Add trigger to update updated_at timestamp for inventory_alerts
CREATE OR REPLACE FUNCTION update_alert_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER alert_updated_at_trigger
    BEFORE UPDATE ON inventory_alerts
    FOR EACH ROW
    EXECUTE FUNCTION update_alert_updated_at();