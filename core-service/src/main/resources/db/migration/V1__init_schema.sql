-- 1. USERS TABLE
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. PAYOUT BATCHES TABLE
CREATE TABLE payout_batches (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    total_records INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL REFERENCES users(id),
    approved_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. PAYOUT ITEMS TABLE
CREATE TABLE payout_items (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES payout_batches(id) ON DELETE CASCADE,
    vendor_name VARCHAR(200) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    ifsc_code VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    processed_at TIMESTAMP WITH TIME ZONE
);

-- 4. AUDIT LOGS TABLE
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_name VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by BIGINT NOT NULL REFERENCES users(id),
    old_state JSONB,
    new_state JSONB,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. PERFORMANCE INDEXES
CREATE INDEX idx_payout_batches_status ON payout_batches(status);
CREATE INDEX idx_payout_items_batch_id ON payout_items(batch_id);
CREATE INDEX idx_payout_items_batch_status ON payout_items(batch_id, status);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
