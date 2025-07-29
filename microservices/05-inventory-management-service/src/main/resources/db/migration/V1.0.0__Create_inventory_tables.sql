-- V1.0.0__Create_inventory_tables.sql
-- Initial schema for inventory management service

-- Equipment table
CREATE TABLE equipment (
    id BIGSERIAL PRIMARY KEY,
    product_id UUID NOT NULL UNIQUE,
    cached_sku VARCHAR(100),
    cached_name VARCHAR(255),
    cached_category VARCHAR(100),
    cached_brand VARCHAR(100),
    cached_equipment_type VARCHAR(50),
    cached_base_price DECIMAL(10,2),
    daily_rate DECIMAL(10,2) NOT NULL,
    is_rental_available BOOLEAN NOT NULL DEFAULT true,
    warehouse_id VARCHAR(50) NOT NULL,
    available_quantity INTEGER NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    cache_updated_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_field BIGINT DEFAULT 1
);

-- Inventory items table
CREATE TABLE inventory_items (
    id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL REFERENCES equipment(id),
    serial_number VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    location VARCHAR(100) NOT NULL,
    size VARCHAR(20),
    condition_rating INTEGER DEFAULT 10 CHECK (condition_rating BETWEEN 1 AND 10),
    purchase_date DATE,
    last_maintenance_date DATE,
    next_maintenance_date DATE,
    total_rental_count INTEGER NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for equipment
CREATE INDEX idx_equipment_product_id ON equipment(product_id);
CREATE INDEX idx_equipment_cached_sku ON equipment(cached_sku);
CREATE INDEX idx_equipment_cached_category ON equipment(cached_category);
CREATE INDEX idx_equipment_warehouse_id ON equipment(warehouse_id);
CREATE INDEX idx_equipment_active ON equipment(is_active);
CREATE INDEX idx_equipment_category_warehouse ON equipment(cached_category, warehouse_id);

-- Indexes for inventory_items
CREATE INDEX idx_inventory_items_equipment_id ON inventory_items(equipment_id);
CREATE INDEX idx_inventory_items_serial_number ON inventory_items(serial_number);
CREATE INDEX idx_inventory_items_status ON inventory_items(status);
CREATE INDEX idx_inventory_items_location ON inventory_items(location);
CREATE INDEX idx_inventory_items_equipment_status ON inventory_items(equipment_id, status);

-- Trigger to update updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_equipment_updated_at BEFORE UPDATE ON equipment
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_inventory_items_updated_at BEFORE UPDATE ON inventory_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();