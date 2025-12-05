-- Standalone Database Indexes Script
-- Run this script manually if not using Flyway/Liquibase migration tools
-- Can be executed directly against your database

-- ================================================================================
-- TIME_SLOT TABLE INDEXES
-- ================================================================================

-- Index for filtering available slots
CREATE INDEX idx_timeslot_available ON time_slot(available);

-- Index for time range queries
CREATE INDEX idx_timeslot_time_range ON time_slot(start_time, end_time);

-- ================================================================================
-- APPOINTMENT TABLE INDEXES
-- ================================================================================

-- Unique index for cancellation tokens
CREATE UNIQUE INDEX idx_appointment_cancellation_token ON appointment(cancellation_token);

-- Index for user appointment history
CREATE INDEX idx_appointment_user ON appointment(user_id);

-- ================================================================================
-- USER TABLE INDEXES
-- ================================================================================

-- Index for username (login)
CREATE INDEX idx_user_username ON users(username);

-- Index for email lookups
CREATE INDEX idx_user_email ON users(email);

-- ================================================================================
-- VERIFICATION QUERIES
-- ================================================================================

-- Run these queries to verify indexes were created successfully:

-- Show all indexes on time_slot table
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'time_slot';

-- Show all indexes on appointment table
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'appointment';

-- Show all indexes on users table
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'users';

-- ================================================================================
-- INDEX STATISTICS (PostgreSQL)
-- ================================================================================

-- Check index usage statistics:
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
WHERE tablename IN ('time_slot', 'appointment', 'users')
ORDER BY idx_scan DESC;
