-- =============================================================================
-- Project Apex — Relational Database Schema (Neon Serverless PostgreSQL & H2)
-- Compatibility: PostgreSQL 15/16 (Neon) & H2 (MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE)
-- Conventions:
--   - All currency amounts stored as BIGINT in integer paise (INR * 100)
--   - All timestamps stored as BIGINT in UTC epoch milliseconds
--   - All nested collections/maps serialized as JSON in TEXT columns
--   - Idempotent DDL only (CREATE TABLE IF NOT EXISTS / CREATE INDEX IF NOT EXISTS)
-- =============================================================================

-- 1. apex_users (Maps UserRecord + embedded OwnerProfile)
CREATE TABLE IF NOT EXISTS apex_users (
    uid VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(64),
    role VARCHAR(64) NOT NULL DEFAULT 'ROLE_PLAYER',
    city VARCHAR(128) DEFAULT 'Thane',
    preferred_sports_json TEXT,
    owner_business_name VARCHAR(255),
    owner_business_address TEXT,
    owner_gst_number VARCHAR(64),
    owner_pan_number VARCHAR(32),
    owner_verification_status VARCHAR(64),
    owner_verified_at BIGINT,
    owner_payout_upi_or_bank_mask VARCHAR(255),
    owner_profile_json TEXT,
    password_hash VARCHAR(255),
    loyalty_points BIGINT NOT NULL DEFAULT 100,
    is_phone_verified BOOLEAN NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_apex_users_email ON apex_users (email);
CREATE INDEX IF NOT EXISTS idx_apex_users_phone ON apex_users (phone);
CREATE INDEX IF NOT EXISTS idx_apex_users_city ON apex_users (city);
CREATE INDEX IF NOT EXISTS idx_apex_users_role ON apex_users (role);
CREATE INDEX IF NOT EXISTS idx_apex_users_owner_verif ON apex_users (owner_verification_status);

-- 2. apex_venues (Maps VenueRecord)
CREATE TABLE IF NOT EXISTS apex_venues (
    venue_id VARCHAR(255) PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address_line TEXT,
    locality VARCHAR(255),
    city VARCHAR(128) NOT NULL,
    state VARCHAR(128) DEFAULT 'Maharashtra',
    pincode VARCHAR(16) DEFAULT '400601',
    latitude DOUBLE PRECISION NOT NULL DEFAULT 19.2183,
    longitude DOUBLE PRECISION NOT NULL DEFAULT 72.9781,
    contact_phone VARCHAR(64),
    amenities_json TEXT,
    sport_types_json TEXT,
    min_slot_price_paise BIGINT NOT NULL DEFAULT 80000,
    has_active_discounts BOOLEAN NOT NULL DEFAULT TRUE,
    image_urls_json TEXT,
    cancellation_policy_json TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    rating DOUBLE PRECISION NOT NULL DEFAULT 4.8,
    review_count INTEGER NOT NULL DEFAULT 142,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_apex_venues_city ON apex_venues (city);
CREATE INDEX IF NOT EXISTS idx_apex_venues_owner_id ON apex_venues (owner_id);
CREATE INDEX IF NOT EXISTS idx_apex_venues_status ON apex_venues (status);
CREATE INDEX IF NOT EXISTS idx_apex_venues_city_status ON apex_venues (city, status);

-- 3. apex_resources (Maps ResourceRecord)
CREATE TABLE IF NOT EXISTS apex_resources (
    resource_id VARCHAR(255) PRIMARY KEY,
    venue_id VARCHAR(255) NOT NULL,
    owner_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    sport_types_json TEXT,
    slot_duration_minutes INTEGER NOT NULL DEFAULT 60,
    base_price_paise BIGINT NOT NULL DEFAULT 100000,
    weekend_price_paise BIGINT NOT NULL DEFAULT 140000,
    peak_hour_price_paise BIGINT NOT NULL DEFAULT 130000,
    max_players INTEGER NOT NULL DEFAULT 14,
    surface_type VARCHAR(64) DEFAULT 'ARTIFICIAL_TURF',
    is_indoor BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_apex_resources_venue_id ON apex_resources (venue_id);
CREATE INDEX IF NOT EXISTS idx_apex_resources_owner_id ON apex_resources (owner_id);
CREATE INDEX IF NOT EXISTS idx_apex_resources_active ON apex_resources (venue_id, is_active);

-- 4. apex_schedule_rules (Maps ScheduleRuleRecord)
CREATE TABLE IF NOT EXISTS apex_schedule_rules (
    resource_id VARCHAR(255) PRIMARY KEY,
    venue_id VARCHAR(255) NOT NULL,
    owner_id VARCHAR(255) NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Kolkata',
    weekly_schedule_json TEXT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_apex_schedule_rules_venue_id ON apex_schedule_rules (venue_id);
CREATE INDEX IF NOT EXISTS idx_apex_schedule_rules_owner_id ON apex_schedule_rules (owner_id);

-- 5. apex_date_overrides (Maps DateOverrideRecord + optional AvailabilityDoc snapshot)
CREATE TABLE IF NOT EXISTS apex_date_overrides (
    override_id VARCHAR(255) PRIMARY KEY,
    resource_id VARCHAR(255) NOT NULL,
    venue_id VARCHAR(255) NOT NULL,
    override_date VARCHAR(32) NOT NULL,
    is_closed_full_day BOOLEAN NOT NULL DEFAULT FALSE,
    closure_reason TEXT,
    custom_open_time VARCHAR(16),
    custom_close_time VARCHAR(16),
    blocked_slots_json TEXT,
    slot_price_overrides_json TEXT,
    slots_snapshot_json TEXT,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uq_apex_date_override UNIQUE (resource_id, override_date)
);

CREATE INDEX IF NOT EXISTS idx_apex_date_overrides_venue_id ON apex_date_overrides (venue_id);
CREATE INDEX IF NOT EXISTS idx_apex_date_overrides_res_date ON apex_date_overrides (resource_id, override_date);

-- 6. apex_slot_locks (Unified ACID Concurrency Lock: Maps SlotLockRecord & ConcurrencyLock)
CREATE TABLE IF NOT EXISTS apex_slot_locks (
    lock_id VARCHAR(255) PRIMARY KEY,
    resource_id VARCHAR(255) NOT NULL,
    venue_id VARCHAR(255),
    booking_date VARCHAR(32) NOT NULL,
    start_time VARCHAR(16) NOT NULL,
    end_time VARCHAR(16),
    status VARCHAR(32) NOT NULL DEFAULT 'HELD',
    booking_id VARCHAR(255),
    user_id VARCHAR(255) NOT NULL,
    hold_expires_at BIGINT,
    acquired_at BIGINT,
    created_at BIGINT NOT NULL,
    CONSTRAINT uq_apex_slot_lock UNIQUE (resource_id, booking_date, start_time)
);

CREATE INDEX IF NOT EXISTS idx_apex_slot_locks_res_date ON apex_slot_locks (resource_id, booking_date);
CREATE INDEX IF NOT EXISTS idx_apex_slot_locks_venue_id ON apex_slot_locks (venue_id);
CREATE INDEX IF NOT EXISTS idx_apex_slot_locks_booking_id ON apex_slot_locks (booking_id);
CREATE INDEX IF NOT EXISTS idx_apex_slot_locks_user_id ON apex_slot_locks (user_id);
CREATE INDEX IF NOT EXISTS idx_apex_slot_locks_status_exp ON apex_slot_locks (status, hold_expires_at);

-- 7. apex_discounts (Maps DiscountRecord)
CREATE TABLE IF NOT EXISTS apex_discounts (
    discount_id VARCHAR(255) PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(64) NOT NULL DEFAULT 'PROMO_CODE',
    code VARCHAR(64),
    value_type VARCHAR(32) NOT NULL DEFAULT 'PERCENTAGE',
    discount_value DOUBLE PRECISION NOT NULL DEFAULT 20.0,
    max_discount_paise BIGINT NOT NULL DEFAULT 50000,
    min_order_paise BIGINT NOT NULL DEFAULT 100000,
    min_slots INTEGER,
    days_of_week_json TEXT,
    time_window_start VARCHAR(16),
    time_window_end VARCHAR(16),
    applicable_venue_ids_json TEXT,
    applicable_resource_ids_json TEXT,
    applicable_sports_json TEXT,
    valid_from BIGINT,
    valid_until BIGINT,
    max_total_uses INTEGER DEFAULT 1000,
    max_uses_per_user INTEGER DEFAULT 5,
    current_uses INTEGER NOT NULL DEFAULT 0,
    stackable_with_auto_discount BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_apex_discounts_owner_id ON apex_discounts (owner_id);
CREATE INDEX IF NOT EXISTS idx_apex_discounts_code ON apex_discounts (code);
CREATE INDEX IF NOT EXISTS idx_apex_discounts_active_type ON apex_discounts (is_active, type);

-- 8. apex_bookings (Maps BookingRecord + BookedSlotItem list)
CREATE TABLE IF NOT EXISTS apex_bookings (
    booking_id VARCHAR(255) PRIMARY KEY,
    booking_code VARCHAR(64),
    qr_pass_token VARCHAR(255),
    user_id VARCHAR(255) NOT NULL,
    user_name VARCHAR(255),
    user_phone VARCHAR(64),
    user_email VARCHAR(255),
    owner_id VARCHAR(255),
    venue_id VARCHAR(255),
    venue_name VARCHAR(255),
    venue_locality VARCHAR(255),
    resource_id VARCHAR(255) NOT NULL,
    resource_name VARCHAR(255),
    sport_type VARCHAR(64),
    booking_date VARCHAR(32) NOT NULL,
    slots_json TEXT NOT NULL,
    subtotal_paise BIGINT NOT NULL DEFAULT 0,
    discount_id VARCHAR(255),
    discount_code VARCHAR(64),
    discount_amount_paise BIGINT NOT NULL DEFAULT 0,
    tax_paise BIGINT NOT NULL DEFAULT 0,
    final_payable_paise BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(64) NOT NULL DEFAULT 'PENDING_PAYMENT',
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    refund_id VARCHAR(255),
    refund_amount_paise BIGINT,
    refund_status VARCHAR(64),
    cancellation_reason TEXT,
    hold_expires_at BIGINT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_apex_bookings_user_id ON apex_bookings (user_id);
CREATE INDEX IF NOT EXISTS idx_apex_bookings_owner_id ON apex_bookings (owner_id);
CREATE INDEX IF NOT EXISTS idx_apex_bookings_venue_id ON apex_bookings (venue_id);
CREATE INDEX IF NOT EXISTS idx_apex_bookings_booking_code ON apex_bookings (booking_code);
CREATE INDEX IF NOT EXISTS idx_apex_bookings_qr_pass_token ON apex_bookings (qr_pass_token);
CREATE INDEX IF NOT EXISTS idx_apex_bookings_res_date ON apex_bookings (resource_id, booking_date);
CREATE INDEX IF NOT EXISTS idx_apex_bookings_status ON apex_bookings (status);
CREATE INDEX IF NOT EXISTS idx_apex_bookings_rzp_order_id ON apex_bookings (razorpay_order_id);

-- 9. apex_payments (Maps PaymentRecord + legacy TransactionRecord)
CREATE TABLE IF NOT EXISTS apex_payments (
    payment_id VARCHAR(255) PRIMARY KEY,
    booking_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    owner_id VARCHAR(255),
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    amount_paise BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'CAPTURED',
    method VARCHAR(64) DEFAULT 'upi',
    tx_type VARCHAR(32) NOT NULL DEFAULT 'PAYMENT',
    gateway VARCHAR(64) DEFAULT 'Razorpay',
    gateway_ref VARCHAR(255),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_apex_payments_booking_id ON apex_payments (booking_id);
CREATE INDEX IF NOT EXISTS idx_apex_payments_user_id ON apex_payments (user_id);
CREATE INDEX IF NOT EXISTS idx_apex_payments_owner_id ON apex_payments (owner_id);
CREATE INDEX IF NOT EXISTS idx_apex_payments_rzp_order_id ON apex_payments (razorpay_order_id);
CREATE INDEX IF NOT EXISTS idx_apex_payments_rzp_payment_id ON apex_payments (razorpay_payment_id);
CREATE INDEX IF NOT EXISTS idx_apex_payments_status ON apex_payments (status);

-- 10. apex_otp_sessions (Maps OtpSession)
CREATE TABLE IF NOT EXISTS apex_otp_sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    phone VARCHAR(64),
    email VARCHAR(255),
    phone_otp VARCHAR(16),
    email_otp VARCHAR(16),
    login_otp VARCHAR(16),
    user_id VARCHAR(255),
    purpose VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    attempts_remaining INTEGER NOT NULL DEFAULT 3,
    verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_apex_otp_sessions_user_id ON apex_otp_sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_apex_otp_sessions_phone ON apex_otp_sessions (phone);
CREATE INDEX IF NOT EXISTS idx_apex_otp_sessions_email ON apex_otp_sessions (email);
CREATE INDEX IF NOT EXISTS idx_apex_otp_sessions_expires_at ON apex_otp_sessions (expires_at);

-- 11. apex_webhook_events (Maps WebhookEventRecord)
CREATE TABLE IF NOT EXISTS apex_webhook_events (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    received_at BIGINT NOT NULL,
    processed_status VARCHAR(64) NOT NULL DEFAULT 'PROCESSED',
    payload_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_apex_webhook_events_type ON apex_webhook_events (event_type);
CREATE INDEX IF NOT EXISTS idx_apex_webhook_events_status ON apex_webhook_events (processed_status);

-- 12. apex_facilities (Maps legacy compatibility FacilityRecord / courts)
CREATE TABLE IF NOT EXISTS apex_facilities (
    court_id VARCHAR(255) PRIMARY KEY,
    venue_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    sport VARCHAR(64) NOT NULL,
    facility_type VARCHAR(128),
    capacity INTEGER NOT NULL DEFAULT 10,
    surface VARCHAR(255),
    dimensions VARCHAR(128),
    base_hourly_rate_paise BIGINT NOT NULL DEFAULT 100000,
    peak_hourly_rate_paise BIGINT NOT NULL DEFAULT 140000,
    odd_hour_discount_pct INTEGER NOT NULL DEFAULT 30,
    has_floodlights BOOLEAN NOT NULL DEFAULT TRUE,
    amenities_json TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at BIGINT
);

CREATE INDEX IF NOT EXISTS idx_apex_facilities_venue_id ON apex_facilities (venue_id);
CREATE INDEX IF NOT EXISTS idx_apex_facilities_sport ON apex_facilities (sport);
CREATE INDEX IF NOT EXISTS idx_apex_facilities_status ON apex_facilities (status);

-- 13. apex_audit_logs (Maps AuditLog)
CREATE TABLE IF NOT EXISTS apex_audit_logs (
    log_id VARCHAR(255) PRIMARY KEY,
    action VARCHAR(128) NOT NULL,
    actor_id VARCHAR(255),
    resource_type VARCHAR(64),
    resource_id VARCHAR(255),
    metadata_json TEXT,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_apex_audit_logs_actor_id ON apex_audit_logs (actor_id);
CREATE INDEX IF NOT EXISTS idx_apex_audit_logs_resource ON apex_audit_logs (resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_apex_audit_logs_action ON apex_audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_apex_audit_logs_created_at ON apex_audit_logs (created_at);
