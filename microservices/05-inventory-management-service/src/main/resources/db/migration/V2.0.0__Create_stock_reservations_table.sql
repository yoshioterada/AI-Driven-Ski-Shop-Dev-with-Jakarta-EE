-- V2.0.0__Create_stock_reservations_table.sql
-- Phase 2: Stock reservation system with timeout management

-- Stock reservations table
CREATE TABLE stock_reservations (
    id BIGSERIAL PRIMARY KEY,
    reservation_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    equipment_id BIGINT NOT NULL REFERENCES equipment(id),
    product_id UUID NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reservation_type VARCHAR(50) NOT NULL DEFAULT 'RENTAL',
    
    -- Timeout management
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    
    -- Rental period (for future use)
    planned_start_date TIMESTAMP,
    planned_end_date TIMESTAMP,
    
    -- Business context
    reference_number VARCHAR(100),
    notes TEXT,
    
    -- Optimistic locking
    version_field BIGINT DEFAULT 1,
    
    -- Audit fields
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_stock_reservations_reservation_id ON stock_reservations(reservation_id);
CREATE INDEX idx_stock_reservations_equipment_id ON stock_reservations(equipment_id);
CREATE INDEX idx_stock_reservations_product_id ON stock_reservations(product_id);
CREATE INDEX idx_stock_reservations_customer_id ON stock_reservations(customer_id);
CREATE INDEX idx_stock_reservations_status ON stock_reservations(status);
CREATE INDEX idx_stock_reservations_expires_at ON stock_reservations(expires_at);
CREATE INDEX idx_stock_reservations_created_at ON stock_reservations(created_at);

-- Composite indexes for common queries
CREATE INDEX idx_stock_reservations_equipment_status ON stock_reservations(equipment_id, status);
CREATE INDEX idx_stock_reservations_customer_status ON stock_reservations(customer_id, status);
CREATE INDEX idx_stock_reservations_status_expires ON stock_reservations(status, expires_at);

-- Constraint to ensure only one status timestamp is set
CREATE OR REPLACE FUNCTION check_reservation_status_consistency()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate status consistency
    IF NEW.status = 'PENDING' THEN
        NEW.confirmed_at = NULL;
        NEW.cancelled_at = NULL;
    ELSIF NEW.status = 'CONFIRMED' THEN
        NEW.cancelled_at = NULL;
        IF NEW.confirmed_at IS NULL THEN
            NEW.confirmed_at = CURRENT_TIMESTAMP;
        END IF;
    ELSIF NEW.status IN ('CANCELLED', 'EXPIRED') THEN
        NEW.confirmed_at = NULL;
        IF NEW.cancelled_at IS NULL THEN
            NEW.cancelled_at = CURRENT_TIMESTAMP;
        END IF;
    END IF;
    
    -- Update timestamp
    NEW.updated_at = CURRENT_TIMESTAMP;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER check_stock_reservations_status_consistency 
    BEFORE INSERT OR UPDATE ON stock_reservations
    FOR EACH ROW EXECUTE FUNCTION check_reservation_status_consistency();

-- Add reservation tracking to equipment (for faster queries)
ALTER TABLE equipment ADD COLUMN IF NOT EXISTS pending_reservations INTEGER NOT NULL DEFAULT 0 CHECK (pending_reservations >= 0);

-- Update equipment reservation counter trigger
CREATE OR REPLACE FUNCTION update_equipment_reservation_counters()
RETURNS TRIGGER AS $$
BEGIN
    -- Handle INSERT
    IF TG_OP = 'INSERT' THEN
        IF NEW.status = 'PENDING' THEN
            UPDATE equipment 
            SET pending_reservations = pending_reservations + NEW.quantity
            WHERE id = NEW.equipment_id;
        END IF;
        RETURN NEW;
    END IF;
    
    -- Handle UPDATE
    IF TG_OP = 'UPDATE' THEN
        -- If status changed from PENDING to something else
        IF OLD.status = 'PENDING' AND NEW.status != 'PENDING' THEN
            UPDATE equipment 
            SET pending_reservations = pending_reservations - OLD.quantity
            WHERE id = OLD.equipment_id;
        END IF;
        
        -- If status changed from something else to PENDING
        IF OLD.status != 'PENDING' AND NEW.status = 'PENDING' THEN
            UPDATE equipment 
            SET pending_reservations = pending_reservations + NEW.quantity
            WHERE id = NEW.equipment_id;
        END IF;
        
        -- If quantity changed while status is PENDING
        IF OLD.status = 'PENDING' AND NEW.status = 'PENDING' AND OLD.quantity != NEW.quantity THEN
            UPDATE equipment 
            SET pending_reservations = pending_reservations - OLD.quantity + NEW.quantity
            WHERE id = NEW.equipment_id;
        END IF;
        
        RETURN NEW;
    END IF;
    
    -- Handle DELETE
    IF TG_OP = 'DELETE' THEN
        IF OLD.status = 'PENDING' THEN
            UPDATE equipment 
            SET pending_reservations = pending_reservations - OLD.quantity
            WHERE id = OLD.equipment_id;
        END IF;
        RETURN OLD;
    END IF;
    
    RETURN NULL;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_equipment_reservation_counters_trigger
    AFTER INSERT OR UPDATE OR DELETE ON stock_reservations
    FOR EACH ROW EXECUTE FUNCTION update_equipment_reservation_counters();

-- Initial data consistency check
UPDATE equipment 
SET pending_reservations = COALESCE((
    SELECT SUM(quantity) 
    FROM stock_reservations 
    WHERE equipment_id = equipment.id 
    AND status = 'PENDING'
), 0)
WHERE pending_reservations != COALESCE((
    SELECT SUM(quantity) 
    FROM stock_reservations 
    WHERE equipment_id = equipment.id 
    AND status = 'PENDING'
), 0);