package com.projectapex.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectapex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * Relational persistence engine backed by Neon Serverless PostgreSQL
 * (or embedded H2 PostgreSQL-Mode in local/offline/test environments).
 * Maps all 13 tables in classpath:db/schema.sql cleanly using JdbcTemplate.
 */
@Service
public class NeonPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(NeonPersistenceService.class);

    private static final TypeReference<List<String>> LIST_STRING_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> MAP_OBJECT_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, ScheduleRuleRecord.DayScheduleRule>> WEEKLY_SCHEDULE_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, DateOverrideRecord.BlockedSlotInfo>> BLOCKED_SLOTS_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Long>> PRICE_OVERRIDES_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, SlotInfo>> SLOTS_SNAPSHOT_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<BookingRecord.BookedSlotItem>> BOOKED_SLOTS_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private volatile boolean schemaInitialized = false;

    @Autowired
    public NeonPersistenceService(JdbcTemplate jdbcTemplate, DataSource dataSource, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public synchronized void ensureSchema() {
        if (schemaInitialized) {
            return;
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/schema.sql"));
        populator.setContinueOnError(false);
        populator.execute(dataSource);
        schemaInitialized = true;
        log.info("Verified relational schema via db/schema.sql (13 tables)");
    }

    public boolean pingDatabase() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (Exception e) {
            log.warn("Database health ping failed: {}", e.getMessage());
            return false;
        }
    }

    public long countUsers() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM apex_users", Long.class);
        return count != null ? count : 0L;
    }

    public long countVenues() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM apex_venues", Long.class);
        return count != null ? count : 0L;
    }

    public long countBookings() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM apex_bookings", Long.class);
        return count != null ? count : 0L;
    }

    // =========================================================================
    // 1. apex_users
    // =========================================================================

    public void saveUser(UserRecord u) {
        if (u == null || u.getUid() == null) return;
        OwnerProfile op = u.getOwnerProfile();
        String preferredSportsJson = toJson(u.getPreferredSports());
        String ownerProfileJson = op != null ? toJson(op) : null;
        long createdAt = u.getCreatedAt() > 0 ? u.getCreatedAt() : System.currentTimeMillis();
        long updatedAt = u.getUpdatedAt() > 0 ? u.getUpdatedAt() : createdAt;

        String updateSql = """
                UPDATE apex_users SET
                    email = ?, full_name = ?, phone = ?, role = ?, city = ?, preferred_sports_json = ?,
                    owner_business_name = ?, owner_business_address = ?, owner_gst_number = ?, owner_pan_number = ?,
                    owner_verification_status = ?, owner_verified_at = ?, owner_payout_upi_or_bank_mask = ?,
                    owner_profile_json = ?, password_hash = ?, loyalty_points = ?,
                    is_phone_verified = ?, is_email_verified = ?, created_at = ?, updated_at = ?
                WHERE uid = ?
                """;
        String insertSql = """
                INSERT INTO apex_users (
                    uid, email, full_name, phone, role, city, preferred_sports_json,
                    owner_business_name, owner_business_address, owner_gst_number, owner_pan_number,
                    owner_verification_status, owner_verified_at, owner_payout_upi_or_bank_mask,
                    owner_profile_json, password_hash, loyalty_points,
                    is_phone_verified, is_email_verified, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    int i = 1;
                    setNullableString(ps, i++, u.getEmail());
                    ps.setString(i++, nonEmpty(u.getFullName(), "Apex User"));
                    setNullableString(ps, i++, u.getPhone());
                    ps.setString(i++, nonEmpty(u.getRole(), "ROLE_PLAYER"));
                    setNullableString(ps, i++, nonEmpty(u.getCity(), "Thane"));
                    setNullableString(ps, i++, preferredSportsJson);
                    setNullableString(ps, i++, op != null ? op.getBusinessName() : null);
                    setNullableString(ps, i++, op != null ? op.getBusinessAddress() : null);
                    setNullableString(ps, i++, op != null ? op.getGstNumber() : null);
                    setNullableString(ps, i++, op != null ? op.getPanNumber() : null);
                    setNullableString(ps, i++, op != null ? op.getVerificationStatus() : null);
                    setNullableLong(ps, i++, op != null ? op.getVerifiedAt() : null);
                    setNullableString(ps, i++, op != null ? op.getPayoutUpiOrBankMask() : null);
                    setNullableString(ps, i++, ownerProfileJson);
                    setNullableString(ps, i++, u.getPasswordHash());
                    ps.setLong(i++, u.getLoyaltyPoints());
                    ps.setBoolean(i++, u.getIsPhoneVerified());
                    ps.setBoolean(i++, u.getIsEmailVerified());
                    ps.setLong(i++, createdAt);
                    ps.setLong(i++, updatedAt);
                    ps.setString(i, u.getUid());
                },
                insertSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, u.getUid());
                    setNullableString(ps, i++, u.getEmail());
                    ps.setString(i++, nonEmpty(u.getFullName(), "Apex User"));
                    setNullableString(ps, i++, u.getPhone());
                    ps.setString(i++, nonEmpty(u.getRole(), "ROLE_PLAYER"));
                    setNullableString(ps, i++, nonEmpty(u.getCity(), "Thane"));
                    setNullableString(ps, i++, preferredSportsJson);
                    setNullableString(ps, i++, op != null ? op.getBusinessName() : null);
                    setNullableString(ps, i++, op != null ? op.getBusinessAddress() : null);
                    setNullableString(ps, i++, op != null ? op.getGstNumber() : null);
                    setNullableString(ps, i++, op != null ? op.getPanNumber() : null);
                    setNullableString(ps, i++, op != null ? op.getVerificationStatus() : null);
                    setNullableLong(ps, i++, op != null ? op.getVerifiedAt() : null);
                    setNullableString(ps, i++, op != null ? op.getPayoutUpiOrBankMask() : null);
                    setNullableString(ps, i++, ownerProfileJson);
                    setNullableString(ps, i++, u.getPasswordHash());
                    ps.setLong(i++, u.getLoyaltyPoints());
                    ps.setBoolean(i++, u.getIsPhoneVerified());
                    ps.setBoolean(i++, u.getIsEmailVerified());
                    ps.setLong(i++, createdAt);
                    ps.setLong(i, updatedAt);
                }
        );
    }

    public List<UserRecord> findAllUsers() {
        return jdbcTemplate.query("SELECT * FROM apex_users", (rs, rowNum) -> mapUser(rs));
    }

    private UserRecord mapUser(ResultSet rs) throws SQLException {
        UserRecord u = new UserRecord();
        u.setUid(rs.getString("uid"));
        u.setEmail(rs.getString("email"));
        u.setFullName(rs.getString("full_name"));
        u.setPhone(rs.getString("phone"));
        u.setRole(rs.getString("role"));
        u.setCity(rs.getString("city"));
        u.setPreferredSports(fromJson(rs.getString("preferred_sports_json"), LIST_STRING_TYPE, ArrayList::new));

        String opJson = rs.getString("owner_profile_json");
        if (opJson != null && !opJson.isBlank()) {
            u.setOwnerProfile(fromJson(opJson, OwnerProfile.class));
        } else if (rs.getString("owner_business_name") != null || rs.getString("owner_verification_status") != null) {
            OwnerProfile op = new OwnerProfile();
            op.setBusinessName(rs.getString("owner_business_name"));
            op.setBusinessAddress(rs.getString("owner_business_address"));
            op.setGstNumber(rs.getString("owner_gst_number"));
            op.setPanNumber(rs.getString("owner_pan_number"));
            op.setVerificationStatus(rs.getString("owner_verification_status"));
            op.setVerifiedAt(getNullableLong(rs, "owner_verified_at"));
            op.setPayoutUpiOrBankMask(rs.getString("owner_payout_upi_or_bank_mask"));
            u.setOwnerProfile(op);
        }

        u.setPasswordHash(rs.getString("password_hash"));
        u.setLoyaltyPoints(rs.getLong("loyalty_points"));
        u.setPhoneVerified(rs.getBoolean("is_phone_verified"));
        u.setEmailVerified(rs.getBoolean("is_email_verified"));
        u.setCreatedAt(rs.getLong("created_at"));
        u.setUpdatedAt(rs.getLong("updated_at"));
        return u;
    }

    // =========================================================================
    // 2. apex_venues
    // =========================================================================

    public void saveVenue(VenueRecord v) {
        if (v == null || v.getVenueId() == null) return;
        String amenitiesJson = toJson(v.getAmenities());
        String sportsJson = toJson(v.getSportTypes());
        String imagesJson = toJson(v.getImageUrls());
        String policyJson = toJson(v.getCancellationPolicy());
        long createdAt = v.getCreatedAt() > 0 ? v.getCreatedAt() : System.currentTimeMillis();
        long updatedAt = v.getUpdatedAt() > 0 ? v.getUpdatedAt() : createdAt;

        String updateSql = """
                UPDATE apex_venues SET
                    owner_id = ?, name = ?, description = ?, address_line = ?, locality = ?, city = ?,
                    state = ?, pincode = ?, latitude = ?, longitude = ?, contact_phone = ?,
                    amenities_json = ?, sport_types_json = ?, min_slot_price_paise = ?, has_active_discounts = ?,
                    image_urls_json = ?, cancellation_policy_json = ?, status = ?, rating = ?, review_count = ?,
                    created_at = ?, updated_at = ?
                WHERE venue_id = ?
                """;
        String insertSql = """
                INSERT INTO apex_venues (
                    venue_id, owner_id, name, description, address_line, locality, city,
                    state, pincode, latitude, longitude, contact_phone,
                    amenities_json, sport_types_json, min_slot_price_paise, has_active_discounts,
                    image_urls_json, cancellation_policy_json, status, rating, review_count,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, nonEmpty(v.getOwnerId(), "demo_owner"));
                    ps.setString(i++, nonEmpty(v.getName(), "Apex Venue"));
                    setNullableString(ps, i++, v.getDescription());
                    setNullableString(ps, i++, v.getAddressLine());
                    setNullableString(ps, i++, v.getLocality());
                    ps.setString(i++, nonEmpty(v.getCity(), "thane"));
                    setNullableString(ps, i++, nonEmpty(v.getState(), "Maharashtra"));
                    setNullableString(ps, i++, nonEmpty(v.getPincode(), "400601"));
                    ps.setDouble(i++, v.getLatitude());
                    ps.setDouble(i++, v.getLongitude());
                    setNullableString(ps, i++, v.getContactPhone());
                    setNullableString(ps, i++, amenitiesJson);
                    setNullableString(ps, i++, sportsJson);
                    ps.setLong(i++, v.getMinSlotPricePaise());
                    ps.setBoolean(i++, v.isHasActiveDiscounts());
                    setNullableString(ps, i++, imagesJson);
                    setNullableString(ps, i++, policyJson);
                    ps.setString(i++, nonEmpty(v.getStatus(), "ACTIVE"));
                    ps.setDouble(i++, v.getRating());
                    ps.setInt(i++, v.getReviewCount());
                    ps.setLong(i++, createdAt);
                    ps.setLong(i++, updatedAt);
                    ps.setString(i, v.getVenueId());
                },
                insertSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, v.getVenueId());
                    ps.setString(i++, nonEmpty(v.getOwnerId(), "demo_owner"));
                    ps.setString(i++, nonEmpty(v.getName(), "Apex Venue"));
                    setNullableString(ps, i++, v.getDescription());
                    setNullableString(ps, i++, v.getAddressLine());
                    setNullableString(ps, i++, v.getLocality());
                    ps.setString(i++, nonEmpty(v.getCity(), "thane"));
                    setNullableString(ps, i++, nonEmpty(v.getState(), "Maharashtra"));
                    setNullableString(ps, i++, nonEmpty(v.getPincode(), "400601"));
                    ps.setDouble(i++, v.getLatitude());
                    ps.setDouble(i++, v.getLongitude());
                    setNullableString(ps, i++, v.getContactPhone());
                    setNullableString(ps, i++, amenitiesJson);
                    setNullableString(ps, i++, sportsJson);
                    ps.setLong(i++, v.getMinSlotPricePaise());
                    ps.setBoolean(i++, v.isHasActiveDiscounts());
                    setNullableString(ps, i++, imagesJson);
                    setNullableString(ps, i++, policyJson);
                    ps.setString(i++, nonEmpty(v.getStatus(), "ACTIVE"));
                    ps.setDouble(i++, v.getRating());
                    ps.setInt(i++, v.getReviewCount());
                    ps.setLong(i++, createdAt);
                    ps.setLong(i, updatedAt);
                }
        );
    }

    public List<VenueRecord> findAllVenues() {
        return jdbcTemplate.query("SELECT * FROM apex_venues", (rs, rowNum) -> {
            VenueRecord v = new VenueRecord();
            v.setVenueId(rs.getString("venue_id"));
            v.setOwnerId(rs.getString("owner_id"));
            v.setName(rs.getString("name"));
            v.setDescription(rs.getString("description"));
            v.setAddressLine(rs.getString("address_line"));
            v.setLocality(rs.getString("locality"));
            v.setCity(rs.getString("city"));
            v.setState(rs.getString("state"));
            v.setPincode(rs.getString("pincode"));
            v.setLatitude(rs.getDouble("latitude"));
            v.setLongitude(rs.getDouble("longitude"));
            v.setContactPhone(rs.getString("contact_phone"));
            v.setAmenities(fromJson(rs.getString("amenities_json"), LIST_STRING_TYPE, ArrayList::new));
            v.setSportTypes(fromJson(rs.getString("sport_types_json"), LIST_STRING_TYPE, ArrayList::new));
            v.setMinSlotPricePaise(rs.getLong("min_slot_price_paise"));
            v.setHasActiveDiscounts(rs.getBoolean("has_active_discounts"));
            v.setImageUrls(fromJson(rs.getString("image_urls_json"), LIST_STRING_TYPE, ArrayList::new));
            Map<String, Object> policy = fromJson(rs.getString("cancellation_policy_json"), MAP_OBJECT_TYPE, HashMap::new);
            if (!policy.isEmpty()) {
                v.setCancellationPolicy(policy);
            }
            v.setStatus(rs.getString("status"));
            v.setRating(rs.getDouble("rating"));
            v.setReviewCount(rs.getInt("review_count"));
            v.setCreatedAt(rs.getLong("created_at"));
            v.setUpdatedAt(rs.getLong("updated_at"));
            return v;
        });
    }

    // =========================================================================
    // 3. apex_resources
    // =========================================================================

    public void saveResource(ResourceRecord r) {
        if (r == null || r.getResourceId() == null) return;
        String sportsJson = toJson(r.getSportTypes());
        long createdAt = r.getCreatedAt() > 0 ? r.getCreatedAt() : System.currentTimeMillis();
        long updatedAt = r.getUpdatedAt() > 0 ? r.getUpdatedAt() : createdAt;

        String updateSql = """
                UPDATE apex_resources SET
                    venue_id = ?, owner_id = ?, name = ?, sport_types_json = ?,
                    slot_duration_minutes = ?, base_price_paise = ?, weekend_price_paise = ?,
                    peak_hour_price_paise = ?, max_players = ?, surface_type = ?,
                    is_indoor = ?, is_active = ?, created_at = ?, updated_at = ?
                WHERE resource_id = ?
                """;
        String insertSql = """
                INSERT INTO apex_resources (
                    resource_id, venue_id, owner_id, name, sport_types_json,
                    slot_duration_minutes, base_price_paise, weekend_price_paise,
                    peak_hour_price_paise, max_players, surface_type,
                    is_indoor, is_active, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, nonEmpty(r.getVenueId(), "venue_01"));
                    ps.setString(i++, nonEmpty(r.getOwnerId(), "demo_owner"));
                    ps.setString(i++, nonEmpty(r.getName(), "Turf Court"));
                    setNullableString(ps, i++, sportsJson);
                    ps.setInt(i++, r.getSlotDurationMinutes());
                    ps.setLong(i++, r.getBasePricePaise());
                    ps.setLong(i++, r.getWeekendPricePaise());
                    ps.setLong(i++, r.getPeakHourPricePaise());
                    ps.setInt(i++, r.getMaxPlayers());
                    setNullableString(ps, i++, nonEmpty(r.getSurfaceType(), "ARTIFICIAL_TURF"));
                    ps.setBoolean(i++, r.isIndoor());
                    ps.setBoolean(i++, r.isActive());
                    ps.setLong(i++, createdAt);
                    ps.setLong(i++, updatedAt);
                    ps.setString(i, r.getResourceId());
                },
                insertSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, r.getResourceId());
                    ps.setString(i++, nonEmpty(r.getVenueId(), "venue_01"));
                    ps.setString(i++, nonEmpty(r.getOwnerId(), "demo_owner"));
                    ps.setString(i++, nonEmpty(r.getName(), "Turf Court"));
                    setNullableString(ps, i++, sportsJson);
                    ps.setInt(i++, r.getSlotDurationMinutes());
                    ps.setLong(i++, r.getBasePricePaise());
                    ps.setLong(i++, r.getWeekendPricePaise());
                    ps.setLong(i++, r.getPeakHourPricePaise());
                    ps.setInt(i++, r.getMaxPlayers());
                    setNullableString(ps, i++, nonEmpty(r.getSurfaceType(), "ARTIFICIAL_TURF"));
                    ps.setBoolean(i++, r.isIndoor());
                    ps.setBoolean(i++, r.isActive());
                    ps.setLong(i++, createdAt);
                    ps.setLong(i, updatedAt);
                }
        );
    }

    public List<ResourceRecord> findAllResources() {
        return jdbcTemplate.query("SELECT * FROM apex_resources", (rs, rowNum) -> {
            ResourceRecord r = new ResourceRecord();
            r.setResourceId(rs.getString("resource_id"));
            r.setVenueId(rs.getString("venue_id"));
            r.setOwnerId(rs.getString("owner_id"));
            r.setName(rs.getString("name"));
            r.setSportTypes(fromJson(rs.getString("sport_types_json"), LIST_STRING_TYPE, ArrayList::new));
            r.setSlotDurationMinutes(rs.getInt("slot_duration_minutes"));
            r.setBasePricePaise(rs.getLong("base_price_paise"));
            r.setWeekendPricePaise(rs.getLong("weekend_price_paise"));
            r.setPeakHourPricePaise(rs.getLong("peak_hour_price_paise"));
            r.setMaxPlayers(rs.getInt("max_players"));
            r.setSurfaceType(rs.getString("surface_type"));
            r.setIndoor(rs.getBoolean("is_indoor"));
            r.setActive(rs.getBoolean("is_active"));
            r.setCreatedAt(rs.getLong("created_at"));
            r.setUpdatedAt(rs.getLong("updated_at"));
            return r;
        });
    }

    // =========================================================================
    // 4. apex_schedule_rules
    // =========================================================================

    public void saveScheduleRule(ScheduleRuleRecord rule) {
        if (rule == null || rule.getResourceId() == null) return;
        String scheduleJson = nonEmpty(toJson(rule.getWeeklySchedule()), "{}");
        long updatedAt = rule.getUpdatedAt() > 0 ? rule.getUpdatedAt() : System.currentTimeMillis();

        String updateSql = """
                UPDATE apex_schedule_rules SET
                    venue_id = ?, owner_id = ?, timezone = ?, weekly_schedule_json = ?, updated_at = ?
                WHERE resource_id = ?
                """;
        String insertSql = """
                INSERT INTO apex_schedule_rules (
                    resource_id, venue_id, owner_id, timezone, weekly_schedule_json, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, nonEmpty(rule.getVenueId(), "venue_01"));
                    ps.setString(i++, nonEmpty(rule.getOwnerId(), "demo_owner"));
                    ps.setString(i++, nonEmpty(rule.getTimezone(), "Asia/Kolkata"));
                    ps.setString(i++, scheduleJson);
                    ps.setLong(i++, updatedAt);
                    ps.setString(i, rule.getResourceId());
                },
                insertSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, rule.getResourceId());
                    ps.setString(i++, nonEmpty(rule.getVenueId(), "venue_01"));
                    ps.setString(i++, nonEmpty(rule.getOwnerId(), "demo_owner"));
                    ps.setString(i++, nonEmpty(rule.getTimezone(), "Asia/Kolkata"));
                    ps.setString(i++, scheduleJson);
                    ps.setLong(i, updatedAt);
                }
        );
    }

    public List<ScheduleRuleRecord> findAllScheduleRules() {
        return jdbcTemplate.query("SELECT * FROM apex_schedule_rules", (rs, rowNum) -> {
            ScheduleRuleRecord rule = new ScheduleRuleRecord();
            rule.setResourceId(rs.getString("resource_id"));
            rule.setVenueId(rs.getString("venue_id"));
            rule.setOwnerId(rs.getString("owner_id"));
            rule.setTimezone(rs.getString("timezone"));
            rule.setWeeklySchedule(fromJson(rs.getString("weekly_schedule_json"), WEEKLY_SCHEDULE_TYPE, HashMap::new));
            rule.setUpdatedAt(rs.getLong("updated_at"));
            return rule;
        });
    }

    // =========================================================================
    // 5. apex_date_overrides (DateOverrideRecord + AvailabilityDoc snapshot)
    // =========================================================================

    public void saveDateOverride(DateOverrideRecord rec) {
        if (rec == null || rec.getOverrideId() == null) return;
        String blockedJson = toJson(rec.getBlockedSlots());
        String priceJson = toJson(rec.getSlotPriceOverrides());
        long updatedAt = rec.getUpdatedAt() > 0 ? rec.getUpdatedAt() : System.currentTimeMillis();

        String updateSql = """
                UPDATE apex_date_overrides SET
                    resource_id = ?, venue_id = ?, override_date = ?, is_closed_full_day = ?,
                    closure_reason = ?, custom_open_time = ?, custom_close_time = ?,
                    blocked_slots_json = ?, slot_price_overrides_json = ?, updated_at = ?
                WHERE override_id = ?
                """;
        String insertSql = """
                INSERT INTO apex_date_overrides (
                    override_id, resource_id, venue_id, override_date, is_closed_full_day,
                    closure_reason, custom_open_time, custom_close_time,
                    blocked_slots_json, slot_price_overrides_json, slots_snapshot_json, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, rec.getResourceId());
                    ps.setString(i++, nonEmpty(rec.getVenueId(), "venue_01"));
                    ps.setString(i++, rec.getDate());
                    ps.setBoolean(i++, rec.isClosedFullDay());
                    setNullableString(ps, i++, rec.getClosureReason());
                    setNullableString(ps, i++, rec.getCustomOpenTime());
                    setNullableString(ps, i++, rec.getCustomCloseTime());
                    setNullableString(ps, i++, blockedJson);
                    setNullableString(ps, i++, priceJson);
                    ps.setLong(i++, updatedAt);
                    ps.setString(i, rec.getOverrideId());
                },
                insertSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, rec.getOverrideId());
                    ps.setString(i++, rec.getResourceId());
                    ps.setString(i++, nonEmpty(rec.getVenueId(), "venue_01"));
                    ps.setString(i++, rec.getDate());
                    ps.setBoolean(i++, rec.isClosedFullDay());
                    setNullableString(ps, i++, rec.getClosureReason());
                    setNullableString(ps, i++, rec.getCustomOpenTime());
                    setNullableString(ps, i++, rec.getCustomCloseTime());
                    setNullableString(ps, i++, blockedJson);
                    setNullableString(ps, i++, priceJson);
                    setNullableString(ps, i++, null);
                    ps.setLong(i, updatedAt);
                }
        );
    }

    public void saveAvailabilityDoc(AvailabilityDoc doc) {
        if (doc == null || doc.getDocId() == null) return;
        String snapshotJson = toJson(doc.getSlots());
        long updatedAt = doc.getLastUpdated() != null && doc.getLastUpdated() > 0
                ? doc.getLastUpdated()
                : System.currentTimeMillis();

        String updateSql = """
                UPDATE apex_date_overrides SET
                    slots_snapshot_json = ?, updated_at = ?
                WHERE override_id = ?
                """;
        String insertSql = """
                INSERT INTO apex_date_overrides (
                    override_id, resource_id, venue_id, override_date,
                    is_closed_full_day, slots_snapshot_json, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    ps.setString(1, snapshotJson);
                    ps.setLong(2, updatedAt);
                    ps.setString(3, doc.getDocId());
                },
                insertSql,
                ps -> {
                    ps.setString(1, doc.getDocId());
                    ps.setString(2, nonEmpty(doc.getCourtId(), doc.getDocId()));
                    ps.setString(3, nonEmpty(doc.getVenueId(), "venue_01"));
                    ps.setString(4, nonEmpty(doc.getDate(), "2026-11-28"));
                    ps.setBoolean(5, false);
                    ps.setString(6, snapshotJson);
                    ps.setLong(7, updatedAt);
                }
        );
    }

    public List<DateOverrideRecord> findAllDateOverrides() {
        return jdbcTemplate.query(
                "SELECT * FROM apex_date_overrides WHERE slots_snapshot_json IS NULL OR is_closed_full_day = TRUE OR closure_reason IS NOT NULL OR custom_open_time IS NOT NULL OR custom_close_time IS NOT NULL OR blocked_slots_json IS NOT NULL OR slot_price_overrides_json IS NOT NULL",
                (rs, rowNum) -> {
                    DateOverrideRecord rec = new DateOverrideRecord();
                    rec.setOverrideId(rs.getString("override_id"));
                    rec.setResourceId(rs.getString("resource_id"));
                    rec.setVenueId(rs.getString("venue_id"));
                    rec.setDate(rs.getString("override_date"));
                    rec.setClosedFullDay(rs.getBoolean("is_closed_full_day"));
                    rec.setClosureReason(rs.getString("closure_reason"));
                    rec.setCustomOpenTime(rs.getString("custom_open_time"));
                    rec.setCustomCloseTime(rs.getString("custom_close_time"));
                    rec.setBlockedSlots(fromJson(rs.getString("blocked_slots_json"), BLOCKED_SLOTS_TYPE, HashMap::new));
                    rec.setSlotPriceOverrides(fromJson(rs.getString("slot_price_overrides_json"), PRICE_OVERRIDES_TYPE, HashMap::new));
                    rec.setUpdatedAt(rs.getLong("updated_at"));
                    return rec;
                }
        );
    }

    public List<AvailabilityDoc> findAllAvailabilityDocs() {
        return jdbcTemplate.query(
                "SELECT override_id, resource_id, venue_id, override_date, slots_snapshot_json, updated_at FROM apex_date_overrides WHERE slots_snapshot_json IS NOT NULL",
                (rs, rowNum) -> {
                    AvailabilityDoc doc = new AvailabilityDoc();
                    doc.setDocId(rs.getString("override_id"));
                    doc.setCourtId(rs.getString("resource_id"));
                    doc.setVenueId(rs.getString("venue_id"));
                    doc.setDate(rs.getString("override_date"));
                    doc.setSlots(fromJson(rs.getString("slots_snapshot_json"), SLOTS_SNAPSHOT_TYPE, HashMap::new));
                    doc.setLastUpdated(rs.getLong("updated_at"));
                    return doc;
                }
        );
    }

    // =========================================================================
    // 6. apex_slot_locks (Unified ACID Slot Lock & Concurrency Lock)
    // =========================================================================

    public void saveSlotLock(SlotLockRecord lock) {
        if (lock == null || lock.getResourceId() == null || lock.getDate() == null || lock.getStartTime() == null) {
            return;
        }
        String lockId = nonEmpty(lock.getLockId(), SlotLockRecord.buildLockId(lock.getResourceId(), lock.getDate(), lock.getStartTime()));
        String status = nonEmpty(lock.getStatus(), "HELD");
        String userId = nonEmpty(lock.getUserId(), "demo_player");
        long now = System.currentTimeMillis();
        long createdAt = lock.getCreatedAt() > 0 ? lock.getCreatedAt() : now;

        if ("RELEASED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status)) {
            deleteSlotLock(lockId, lock);
            return;
        }

        purgeStaleSlotLocks(lock.getResourceId(), lock.getDate(), lock.getStartTime(), now);

        String updateByIdSql = """
                UPDATE apex_slot_locks SET
                    resource_id = ?, venue_id = ?, booking_date = ?, start_time = ?, end_time = ?,
                    status = ?, booking_id = ?, user_id = ?, hold_expires_at = ?, acquired_at = NULL, created_at = ?
                WHERE lock_id = ?
                  AND (user_id = ? OR status IN ('RELEASED', 'EXPIRED') OR (status = 'HELD' AND hold_expires_at IS NOT NULL AND hold_expires_at < ?))
                """;
        int updated = jdbcTemplate.update(updateByIdSql, ps -> {
            int i = 1;
            ps.setString(i++, lock.getResourceId());
            setNullableString(ps, i++, lock.getVenueId());
            ps.setString(i++, lock.getDate());
            ps.setString(i++, lock.getStartTime());
            setNullableString(ps, i++, lock.getEndTime());
            ps.setString(i++, status);
            setNullableString(ps, i++, lock.getBookingId());
            ps.setString(i++, userId);
            setNullableLong(ps, i++, lock.getHoldExpiresAt());
            ps.setLong(i++, createdAt);
            ps.setString(i++, lockId);
            ps.setString(i++, userId);
            ps.setLong(i, now);
        });

        if (updated > 0) {
            return;
        }

        // Allow replacing a HELD ConcurrencyLock (whose lock_id contains ':') owned by the SAME user
        jdbcTemplate.update(
                "DELETE FROM apex_slot_locks WHERE resource_id = ? AND booking_date = ? AND start_time = ? AND status = 'HELD' AND user_id = ?",
                ps -> {
                    ps.setString(1, lock.getResourceId());
                    ps.setString(2, lock.getDate());
                    ps.setString(3, lock.getStartTime());
                    ps.setString(4, userId);
                }
        );

        String insertSql = """
                INSERT INTO apex_slot_locks (
                    lock_id, resource_id, venue_id, booking_date, start_time, end_time,
                    status, booking_id, user_id, hold_expires_at, acquired_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?)
                """;
        try {
            jdbcTemplate.update(insertSql, ps -> {
                int i = 1;
                ps.setString(i++, lockId);
                ps.setString(i++, lock.getResourceId());
                setNullableString(ps, i++, lock.getVenueId());
                ps.setString(i++, lock.getDate());
                ps.setString(i++, lock.getStartTime());
                setNullableString(ps, i++, lock.getEndTime());
                ps.setString(i++, status);
                setNullableString(ps, i++, lock.getBookingId());
                ps.setString(i++, userId);
                setNullableLong(ps, i++, lock.getHoldExpiresAt());
                ps.setLong(i, createdAt);
            });
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Slot " + lock.getStartTime() + " is currently held or booked by another user.", e);
        }
    }

    public void deleteSlotLock(String lockId, SlotLockRecord knownLock) {
        if (knownLock != null && knownLock.getResourceId() != null && knownLock.getDate() != null && knownLock.getStartTime() != null) {
            jdbcTemplate.update(
                    "DELETE FROM apex_slot_locks WHERE lock_id = ? OR (resource_id = ? AND booking_date = ? AND start_time = ?)",
                    ps -> {
                        ps.setString(1, nonEmpty(lockId, knownLock.getLockId()));
                        ps.setString(2, knownLock.getResourceId());
                        ps.setString(3, knownLock.getDate());
                        ps.setString(4, knownLock.getStartTime());
                    }
            );
        } else if (lockId != null) {
            jdbcTemplate.update("DELETE FROM apex_slot_locks WHERE lock_id = ?", ps -> ps.setString(1, lockId));
        }
    }

    public Optional<SlotLockRecord> findActiveSlotLock(String resourceId, String bookingDate, String startTime) {
        List<SlotLockRecord> rows = jdbcTemplate.query(
                "SELECT * FROM apex_slot_locks WHERE resource_id = ? AND booking_date = ? AND start_time = ?",
                ps -> {
                    ps.setString(1, resourceId);
                    ps.setString(2, bookingDate);
                    ps.setString(3, startTime);
                },
                (rs, rowNum) -> mapSlotLock(rs)
        );
        return rows.stream().findFirst();
    }

    public List<SlotLockRecord> findAllSlotLocks() {
        return jdbcTemplate.query("SELECT * FROM apex_slot_locks", (rs, rowNum) -> mapSlotLock(rs));
    }

    public void saveConcurrencyLock(ConcurrencyLock lock) {
        if (lock == null || lock.getCourtId() == null || lock.getDate() == null || lock.getSlot() == null) {
            return;
        }
        String lockId = nonEmpty(lock.getLockKey(), lock.getCourtId() + "_" + lock.getDate() + "_" + lock.getSlot());
        String status = nonEmpty(lock.getStatus(), "HELD");
        String userId = nonEmpty(lock.getUserId(), "demo_player");
        long now = System.currentTimeMillis();
        long acquiredAt = lock.getAcquiredAt() != null ? lock.getAcquiredAt() : now;

        if ("RELEASED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status)) {
            releaseConcurrencyLock(lock);
            return;
        }

        purgeStaleSlotLocks(lock.getCourtId(), lock.getDate(), lock.getSlot(), now);

        String dbStatus = "COMMITTED".equalsIgnoreCase(status) ? "BOOKED" : status;

        String updateSql = """
                UPDATE apex_slot_locks SET
                    resource_id = ?, booking_date = ?, start_time = ?, end_time = ?,
                    status = ?, booking_id = ?, user_id = ?, hold_expires_at = ?, acquired_at = ?, created_at = ?
                WHERE lock_id = ?
                  AND (user_id = ? OR status IN ('RELEASED', 'EXPIRED') OR (status = 'HELD' AND hold_expires_at IS NOT NULL AND hold_expires_at < ?))
                """;
        int updated = jdbcTemplate.update(updateSql, ps -> {
            int i = 1;
            ps.setString(i++, lock.getCourtId());
            ps.setString(i++, lock.getDate());
            ps.setString(i++, lock.getSlot());
            ps.setString(i++, lock.getSlot());
            ps.setString(i++, dbStatus);
            setNullableString(ps, i++, lock.getBookingId());
            ps.setString(i++, userId);
            setNullableLong(ps, i++, lock.getExpiresAt());
            ps.setLong(i++, acquiredAt);
            ps.setLong(i++, acquiredAt);
            ps.setString(i++, lockId);
            ps.setString(i++, userId);
            ps.setLong(i, now);
        });

        if (updated > 0) {
            return;
        }

        // Allow same user to renew/replace their own HELD lock even if lock_id format differed
        jdbcTemplate.update(
                "DELETE FROM apex_slot_locks WHERE resource_id = ? AND booking_date = ? AND start_time = ? AND status = 'HELD' AND user_id = ?",
                ps -> {
                    ps.setString(1, lock.getCourtId());
                    ps.setString(2, lock.getDate());
                    ps.setString(3, lock.getSlot());
                    ps.setString(4, userId);
                }
        );

        String insertSql = """
                INSERT INTO apex_slot_locks (
                    lock_id, resource_id, venue_id, booking_date, start_time, end_time,
                    status, booking_id, user_id, hold_expires_at, acquired_at, created_at
                ) VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try {
            jdbcTemplate.update(insertSql, ps -> {
                int i = 1;
                ps.setString(i++, lockId);
                ps.setString(i++, lock.getCourtId());
                ps.setString(i++, lock.getDate());
                ps.setString(i++, lock.getSlot());
                ps.setString(i++, lock.getSlot());
                ps.setString(i++, dbStatus);
                setNullableString(ps, i++, lock.getBookingId());
                ps.setString(i++, userId);
                setNullableLong(ps, i++, lock.getExpiresAt());
                ps.setLong(i++, acquiredAt);
                ps.setLong(i, acquiredAt);
            });
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Double-booking conflict: Slot is temporarily reserved or booked by another player.", e);
        }
    }

    public void releaseConcurrencyLock(ConcurrencyLock lock) {
        if (lock == null) return;
        String lockId = nonEmpty(lock.getLockKey(), lock.getCourtId() + "_" + lock.getDate() + "_" + lock.getSlot());
        jdbcTemplate.update(
                "DELETE FROM apex_slot_locks WHERE (lock_id = ? OR (resource_id = ? AND booking_date = ? AND start_time = ?)) AND status = 'HELD'",
                ps -> {
                    ps.setString(1, lockId);
                    ps.setString(2, lock.getCourtId());
                    ps.setString(3, lock.getDate());
                    ps.setString(4, lock.getSlot());
                }
        );
    }

    public void deleteConcurrencyLock(String lockKey, ConcurrencyLock knownLock) {
        if (knownLock != null) {
            jdbcTemplate.update(
                    "DELETE FROM apex_slot_locks WHERE lock_id = ? OR (resource_id = ? AND booking_date = ? AND start_time = ?)",
                    ps -> {
                        ps.setString(1, nonEmpty(lockKey, knownLock.getLockKey()));
                        ps.setString(2, knownLock.getCourtId());
                        ps.setString(3, knownLock.getDate());
                        ps.setString(4, knownLock.getSlot());
                    }
            );
        } else if (lockKey != null) {
            jdbcTemplate.update("DELETE FROM apex_slot_locks WHERE lock_id = ?", ps -> ps.setString(1, lockKey));
        }
    }

    public List<ConcurrencyLock> findAllConcurrencyLocks() {
        return jdbcTemplate.query(
                "SELECT * FROM apex_slot_locks WHERE acquired_at IS NOT NULL",
                (rs, rowNum) -> {
                    ConcurrencyLock cl = new ConcurrencyLock();
                    cl.setLockKey(rs.getString("lock_id"));
                    cl.setCourtId(rs.getString("resource_id"));
                    cl.setDate(rs.getString("booking_date"));
                    cl.setSlot(rs.getString("start_time"));
                    cl.setUserId(rs.getString("user_id"));
                    cl.setAcquiredAt(getNullableLong(rs, "acquired_at"));
                    cl.setExpiresAt(getNullableLong(rs, "hold_expires_at"));
                    String st = rs.getString("status");
                    cl.setStatus("BOOKED".equalsIgnoreCase(st) ? "COMMITTED" : st);
                    cl.setBookingId(rs.getString("booking_id"));
                    return cl;
                }
        );
    }

    private void purgeStaleSlotLocks(String resourceId, String bookingDate, String startTime, long now) {
        jdbcTemplate.update(
                """
                DELETE FROM apex_slot_locks
                WHERE resource_id = ? AND booking_date = ? AND start_time = ?
                  AND (status IN ('RELEASED', 'EXPIRED') OR (status = 'HELD' AND hold_expires_at IS NOT NULL AND hold_expires_at < ?))
                """,
                ps -> {
                    ps.setString(1, resourceId);
                    ps.setString(2, bookingDate);
                    ps.setString(3, startTime);
                    ps.setLong(4, now);
                }
        );
    }

    private SlotLockRecord mapSlotLock(ResultSet rs) throws SQLException {
        SlotLockRecord sl = new SlotLockRecord();
        sl.setLockId(rs.getString("lock_id"));
        sl.setResourceId(rs.getString("resource_id"));
        sl.setVenueId(rs.getString("venue_id"));
        sl.setDate(rs.getString("booking_date"));
        sl.setStartTime(rs.getString("start_time"));
        sl.setEndTime(rs.getString("end_time"));
        sl.setStatus(rs.getString("status"));
        sl.setBookingId(rs.getString("booking_id"));
        sl.setUserId(rs.getString("user_id"));
        sl.setHoldExpiresAt(getNullableLong(rs, "hold_expires_at"));
        sl.setCreatedAt(rs.getLong("created_at"));
        return sl;
    }

    // =========================================================================
    // 7. apex_discounts
    // =========================================================================

    public void saveDiscount(DiscountRecord d) {
        if (d == null || d.getDiscountId() == null) return;
        String daysJson = toJson(d.getDaysOfWeek());
        String venueIdsJson = toJson(d.getApplicableVenueIds());
        String resIdsJson = toJson(d.getApplicableResourceIds());
        String sportsJson = toJson(d.getApplicableSports());
        long createdAt = d.getCreatedAt() > 0 ? d.getCreatedAt() : System.currentTimeMillis();

        String updateSql = """
                UPDATE apex_discounts SET
                    owner_id = ?, name = ?, type = ?, code = ?, value_type = ?, discount_value = ?,
                    max_discount_paise = ?, min_order_paise = ?, min_slots = ?, days_of_week_json = ?,
                    time_window_start = ?, time_window_end = ?, applicable_venue_ids_json = ?,
                    applicable_resource_ids_json = ?, applicable_sports_json = ?, valid_from = ?, valid_until = ?,
                    max_total_uses = ?, max_uses_per_user = ?, current_uses = ?,
                    stackable_with_auto_discount = ?, is_active = ?, created_at = ?
                WHERE discount_id = ?
                """;
        String insertSql = """
                INSERT INTO apex_discounts (
                    discount_id, owner_id, name, type, code, value_type, discount_value,
                    max_discount_paise, min_order_paise, min_slots, days_of_week_json,
                    time_window_start, time_window_end, applicable_venue_ids_json,
                    applicable_resource_ids_json, applicable_sports_json, valid_from, valid_until,
                    max_total_uses, max_uses_per_user, current_uses,
                    stackable_with_auto_discount, is_active, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, nonEmpty(d.getOwnerId(), "owner_01"));
                    ps.setString(i++, nonEmpty(d.getName(), "Promo Discount"));
                    ps.setString(i++, nonEmpty(d.getType(), "PROMO_CODE"));
                    setNullableString(ps, i++, d.getCode());
                    ps.setString(i++, nonEmpty(d.getValueType(), "PERCENTAGE"));
                    ps.setDouble(i++, d.getValue());
                    ps.setLong(i++, d.getMaxDiscountPaise());
                    ps.setLong(i++, d.getMinOrderPaise());
                    setNullableInt(ps, i++, d.getMinSlots());
                    setNullableString(ps, i++, daysJson);
                    setNullableString(ps, i++, d.getTimeWindowStart());
                    setNullableString(ps, i++, d.getTimeWindowEnd());
                    setNullableString(ps, i++, venueIdsJson);
                    setNullableString(ps, i++, resIdsJson);
                    setNullableString(ps, i++, sportsJson);
                    setNullableLong(ps, i++, d.getValidFrom());
                    setNullableLong(ps, i++, d.getValidUntil());
                    setNullableInt(ps, i++, d.getMaxTotalUses());
                    setNullableInt(ps, i++, d.getMaxUsesPerUser());
                    ps.setInt(i++, d.getCurrentUses());
                    ps.setBoolean(i++, d.isStackableWithAutoDiscount());
                    ps.setBoolean(i++, d.isActive());
                    ps.setLong(i++, createdAt);
                    ps.setString(i, d.getDiscountId());
                },
                insertSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, d.getDiscountId());
                    ps.setString(i++, nonEmpty(d.getOwnerId(), "owner_01"));
                    ps.setString(i++, nonEmpty(d.getName(), "Promo Discount"));
                    ps.setString(i++, nonEmpty(d.getType(), "PROMO_CODE"));
                    setNullableString(ps, i++, d.getCode());
                    ps.setString(i++, nonEmpty(d.getValueType(), "PERCENTAGE"));
                    ps.setDouble(i++, d.getValue());
                    ps.setLong(i++, d.getMaxDiscountPaise());
                    ps.setLong(i++, d.getMinOrderPaise());
                    setNullableInt(ps, i++, d.getMinSlots());
                    setNullableString(ps, i++, daysJson);
                    setNullableString(ps, i++, d.getTimeWindowStart());
                    setNullableString(ps, i++, d.getTimeWindowEnd());
                    setNullableString(ps, i++, venueIdsJson);
                    setNullableString(ps, i++, resIdsJson);
                    setNullableString(ps, i++, sportsJson);
                    setNullableLong(ps, i++, d.getValidFrom());
                    setNullableLong(ps, i++, d.getValidUntil());
                    setNullableInt(ps, i++, d.getMaxTotalUses());
                    setNullableInt(ps, i++, d.getMaxUsesPerUser());
                    ps.setInt(i++, d.getCurrentUses());
                    ps.setBoolean(i++, d.isStackableWithAutoDiscount());
                    ps.setBoolean(i++, d.isActive());
                    ps.setLong(i, createdAt);
                }
        );
    }

    public List<DiscountRecord> findAllDiscounts() {
        return jdbcTemplate.query("SELECT * FROM apex_discounts", (rs, rowNum) -> {
            DiscountRecord d = new DiscountRecord();
            d.setDiscountId(rs.getString("discount_id"));
            d.setOwnerId(rs.getString("owner_id"));
            d.setName(rs.getString("name"));
            d.setType(rs.getString("type"));
            d.setCode(rs.getString("code"));
            d.setValueType(rs.getString("value_type"));
            d.setValue(rs.getDouble("discount_value"));
            d.setMaxDiscountPaise(rs.getLong("max_discount_paise"));
            d.setMinOrderPaise(rs.getLong("min_order_paise"));
            d.setMinSlots(getNullableInt(rs, "min_slots"));
            d.setDaysOfWeek(fromJson(rs.getString("days_of_week_json"), LIST_STRING_TYPE, ArrayList::new));
            d.setTimeWindowStart(rs.getString("time_window_start"));
            d.setTimeWindowEnd(rs.getString("time_window_end"));
            d.setApplicableVenueIds(fromJson(rs.getString("applicable_venue_ids_json"), LIST_STRING_TYPE, ArrayList::new));
            d.setApplicableResourceIds(fromJson(rs.getString("applicable_resource_ids_json"), LIST_STRING_TYPE, ArrayList::new));
            d.setApplicableSports(fromJson(rs.getString("applicable_sports_json"), LIST_STRING_TYPE, ArrayList::new));
            d.setValidFrom(getNullableLong(rs, "valid_from"));
            d.setValidUntil(getNullableLong(rs, "valid_until"));
            d.setMaxTotalUses(getNullableInt(rs, "max_total_uses"));
            d.setMaxUsesPerUser(getNullableInt(rs, "max_uses_per_user"));
            d.setCurrentUses(rs.getInt("current_uses"));
            d.setStackableWithAutoDiscount(rs.getBoolean("stackable_with_auto_discount"));
            d.setActive(rs.getBoolean("is_active"));
            d.setCreatedAt(rs.getLong("created_at"));
            return d;
        });
    }

    // =========================================================================
    // 8. apex_bookings
    // =========================================================================

    public void saveBooking(BookingRecord b) {
        if (b == null || b.getBookingId() == null) return;
        String slotsJson = nonEmpty(toJson(b.getSlots()), "[]");
        long createdAt = b.getCreatedAt() > 0 ? b.getCreatedAt() : System.currentTimeMillis();
        long updatedAt = b.getUpdatedAt() > 0 ? b.getUpdatedAt() : createdAt;

        String updateSql = """
                UPDATE apex_bookings SET
                    booking_code = ?, qr_pass_token = ?, user_id = ?, user_name = ?, user_phone = ?, user_email = ?,
                    owner_id = ?, venue_id = ?, venue_name = ?, venue_locality = ?, resource_id = ?, resource_name = ?,
                    sport_type = ?, booking_date = ?, slots_json = ?, subtotal_paise = ?, discount_id = ?,
                    discount_code = ?, discount_amount_paise = ?, tax_paise = ?, final_payable_paise = ?, status = ?,
                    razorpay_order_id = ?, razorpay_payment_id = ?, refund_id = ?, refund_amount_paise = ?,
                    refund_status = ?, cancellation_reason = ?, hold_expires_at = ?, created_at = ?, updated_at = ?
                WHERE booking_id = ?
                """;
        String insertSql = """
                INSERT INTO apex_bookings (
                    booking_id, booking_code, qr_pass_token, user_id, user_name, user_phone, user_email,
                    owner_id, venue_id, venue_name, venue_locality, resource_id, resource_name,
                    sport_type, booking_date, slots_json, subtotal_paise, discount_id,
                    discount_code, discount_amount_paise, tax_paise, final_payable_paise, status,
                    razorpay_order_id, razorpay_payment_id, refund_id, refund_amount_paise,
                    refund_status, cancellation_reason, hold_expires_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    int i = 1;
                    setNullableString(ps, i++, b.getBookingCode());
                    setNullableString(ps, i++, b.getQrPassToken());
                    ps.setString(i++, nonEmpty(b.getUserId(), "demo_player"));
                    setNullableString(ps, i++, b.getUserName());
                    setNullableString(ps, i++, b.getUserPhone());
                    setNullableString(ps, i++, b.getUserEmail());
                    setNullableString(ps, i++, b.getOwnerId());
                    setNullableString(ps, i++, b.getVenueId());
                    setNullableString(ps, i++, b.getVenueName());
                    setNullableString(ps, i++, b.getVenueLocality());
                    ps.setString(i++, nonEmpty(b.getResourceId(), "res_default"));
                    setNullableString(ps, i++, b.getResourceName());
                    setNullableString(ps, i++, b.getSportType());
                    ps.setString(i++, nonEmpty(b.getBookingDate(), "2026-11-28"));
                    ps.setString(i++, slotsJson);
                    ps.setLong(i++, b.getSubtotalPaise());
                    setNullableString(ps, i++, b.getDiscountId());
                    setNullableString(ps, i++, b.getDiscountCode());
                    ps.setLong(i++, b.getDiscountAmountPaise());
                    ps.setLong(i++, b.getTaxPaise());
                    ps.setLong(i++, b.getFinalPayablePaise());
                    ps.setString(i++, nonEmpty(b.getStatus(), "PENDING_PAYMENT"));
                    setNullableString(ps, i++, b.getRazorpayOrderId());
                    setNullableString(ps, i++, b.getRazorpayPaymentId());
                    setNullableString(ps, i++, b.getRefundId());
                    setNullableLong(ps, i++, b.getRefundAmountPaise());
                    setNullableString(ps, i++, b.getRefundStatus());
                    setNullableString(ps, i++, b.getCancellationReason());
                    setNullableLong(ps, i++, b.getHoldExpiresAt());
                    ps.setLong(i++, createdAt);
                    ps.setLong(i++, updatedAt);
                    ps.setString(i, b.getBookingId());
                },
                insertSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, b.getBookingId());
                    setNullableString(ps, i++, b.getBookingCode());
                    setNullableString(ps, i++, b.getQrPassToken());
                    ps.setString(i++, nonEmpty(b.getUserId(), "demo_player"));
                    setNullableString(ps, i++, b.getUserName());
                    setNullableString(ps, i++, b.getUserPhone());
                    setNullableString(ps, i++, b.getUserEmail());
                    setNullableString(ps, i++, b.getOwnerId());
                    setNullableString(ps, i++, b.getVenueId());
                    setNullableString(ps, i++, b.getVenueName());
                    setNullableString(ps, i++, b.getVenueLocality());
                    ps.setString(i++, nonEmpty(b.getResourceId(), "res_default"));
                    setNullableString(ps, i++, b.getResourceName());
                    setNullableString(ps, i++, b.getSportType());
                    ps.setString(i++, nonEmpty(b.getBookingDate(), "2026-11-28"));
                    ps.setString(i++, slotsJson);
                    ps.setLong(i++, b.getSubtotalPaise());
                    setNullableString(ps, i++, b.getDiscountId());
                    setNullableString(ps, i++, b.getDiscountCode());
                    ps.setLong(i++, b.getDiscountAmountPaise());
                    ps.setLong(i++, b.getTaxPaise());
                    ps.setLong(i++, b.getFinalPayablePaise());
                    ps.setString(i++, nonEmpty(b.getStatus(), "PENDING_PAYMENT"));
                    setNullableString(ps, i++, b.getRazorpayOrderId());
                    setNullableString(ps, i++, b.getRazorpayPaymentId());
                    setNullableString(ps, i++, b.getRefundId());
                    setNullableLong(ps, i++, b.getRefundAmountPaise());
                    setNullableString(ps, i++, b.getRefundStatus());
                    setNullableString(ps, i++, b.getCancellationReason());
                    setNullableLong(ps, i++, b.getHoldExpiresAt());
                    ps.setLong(i++, createdAt);
                    ps.setLong(i, updatedAt);
                }
        );
    }

    public List<BookingRecord> findAllBookings() {
        return jdbcTemplate.query("SELECT * FROM apex_bookings", (rs, rowNum) -> {
            BookingRecord b = new BookingRecord();
            b.setBookingId(rs.getString("booking_id"));
            b.setBookingCode(rs.getString("booking_code"));
            b.setUserId(rs.getString("user_id"));
            b.setUserName(rs.getString("user_name"));
            b.setUserPhone(rs.getString("user_phone"));
            b.setUserEmail(rs.getString("user_email"));
            b.setOwnerId(rs.getString("owner_id"));
            b.setVenueId(rs.getString("venue_id"));
            b.setVenueName(rs.getString("venue_name"));
            b.setVenueLocality(rs.getString("venue_locality"));
            b.setResourceId(rs.getString("resource_id"));
            b.setResourceName(rs.getString("resource_name"));
            b.setSportType(rs.getString("sport_type"));
            b.setBookingDate(rs.getString("booking_date"));
            b.setSlots(fromJson(rs.getString("slots_json"), BOOKED_SLOTS_TYPE, ArrayList::new));
            b.setSubtotalPaise(rs.getLong("subtotal_paise"));
            b.setDiscountId(rs.getString("discount_id"));
            b.setDiscountCode(rs.getString("discount_code"));
            b.setDiscountAmountPaise(rs.getLong("discount_amount_paise"));
            b.setTaxPaise(rs.getLong("tax_paise"));
            b.setFinalPayablePaise(rs.getLong("final_payable_paise"));
            b.setStatus(rs.getString("status"));
            b.setRazorpayOrderId(rs.getString("razorpay_order_id"));
            b.setRazorpayPaymentId(rs.getString("razorpay_payment_id"));
            b.setRefundId(rs.getString("refund_id"));
            b.setRefundAmountPaise(getNullableLong(rs, "refund_amount_paise"));
            b.setRefundStatus(rs.getString("refund_status"));
            b.setCancellationReason(rs.getString("cancellation_reason"));
            b.setHoldExpiresAt(getNullableLong(rs, "hold_expires_at"));
            b.setCreatedAt(rs.getLong("created_at"));
            b.setUpdatedAt(rs.getLong("updated_at"));
            return b;
        });
    }

    // =========================================================================
    // 9. apex_payments (PaymentRecord + legacy TransactionRecord)
    // =========================================================================

    public void savePayment(PaymentRecord p) {
        if (p == null || p.getPaymentId() == null) return;
        long createdAt = p.getCreatedAt() > 0 ? p.getCreatedAt() : System.currentTimeMillis();
        long updatedAt = p.getUpdatedAt() > 0 ? p.getUpdatedAt() : createdAt;

        savePaymentRow(
                p.getPaymentId(),
                nonEmpty(p.getBookingId(), "bk_unknown"),
                p.getUserId(),
                p.getOwnerId(),
                p.getRazorpayOrderId(),
                p.getRazorpayPaymentId(),
                p.getAmountPaise(),
                nonEmpty(p.getCurrency(), "INR"),
                nonEmpty(p.getStatus(), "CAPTURED"),
                nonEmpty(p.getMethod(), "upi"),
                "PAYMENT",
                "Razorpay",
                p.getRazorpayPaymentId(),
                createdAt,
                updatedAt
        );
    }

    public void saveTransaction(TransactionRecord tx) {
        if (tx == null || tx.getTxId() == null) return;
        long ts = tx.getTimestamp() != null ? tx.getTimestamp() : System.currentTimeMillis();
        long amountPaise = tx.getAmount() != null ? (long) tx.getAmount() * 100L : 0L;

        savePaymentRow(
                tx.getTxId(),
                nonEmpty(tx.getBookingId(), "bk_unknown"),
                tx.getUserId(),
                null,
                null,
                null,
                amountPaise,
                "INR",
                nonEmpty(tx.getStatus(), "SUCCESS"),
                "LEGACY",
                nonEmpty(tx.getType(), "PAYMENT"),
                nonEmpty(tx.getGateway(), "Razorpay"),
                tx.getGatewayRef(),
                ts,
                ts
        );
    }

    private void savePaymentRow(String paymentId, String bookingId, String userId, String ownerId,
                                String rzpOrderId, String rzpPaymentId, long amountPaise, String currency,
                                String status, String method, String txType, String gateway, String gatewayRef,
                                long createdAt, long updatedAt) {
        String updateSql = """
                UPDATE apex_payments SET
                    booking_id = ?, user_id = ?, owner_id = ?, razorpay_order_id = ?, razorpay_payment_id = ?,
                    amount_paise = ?, currency = ?, status = ?, method = ?, tx_type = ?,
                    gateway = ?, gateway_ref = ?, created_at = ?, updated_at = ?
                WHERE payment_id = ?
                """;
        String insertSql = """
                INSERT INTO apex_payments (
                    payment_id, booking_id, user_id, owner_id, razorpay_order_id, razorpay_payment_id,
                    amount_paise, currency, status, method, tx_type, gateway, gateway_ref, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, bookingId);
                    setNullableString(ps, i++, userId);
                    setNullableString(ps, i++, ownerId);
                    setNullableString(ps, i++, rzpOrderId);
                    setNullableString(ps, i++, rzpPaymentId);
                    ps.setLong(i++, amountPaise);
                    ps.setString(i++, currency);
                    ps.setString(i++, status);
                    setNullableString(ps, i++, method);
                    ps.setString(i++, txType);
                    setNullableString(ps, i++, gateway);
                    setNullableString(ps, i++, gatewayRef);
                    ps.setLong(i++, createdAt);
                    ps.setLong(i++, updatedAt);
                    ps.setString(i, paymentId);
                },
                insertSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, paymentId);
                    ps.setString(i++, bookingId);
                    setNullableString(ps, i++, userId);
                    setNullableString(ps, i++, ownerId);
                    setNullableString(ps, i++, rzpOrderId);
                    setNullableString(ps, i++, rzpPaymentId);
                    ps.setLong(i++, amountPaise);
                    ps.setString(i++, currency);
                    ps.setString(i++, status);
                    setNullableString(ps, i++, method);
                    ps.setString(i++, txType);
                    setNullableString(ps, i++, gateway);
                    setNullableString(ps, i++, gatewayRef);
                    ps.setLong(i++, createdAt);
                    ps.setLong(i, updatedAt);
                }
        );
    }

    public List<PaymentRecord> findAllPayments() {
        return jdbcTemplate.query(
                "SELECT * FROM apex_payments WHERE method IS NULL OR method <> 'LEGACY'",
                (rs, rowNum) -> {
                    PaymentRecord p = new PaymentRecord();
                    p.setPaymentId(rs.getString("payment_id"));
                    p.setBookingId(rs.getString("booking_id"));
                    p.setUserId(rs.getString("user_id"));
                    p.setOwnerId(rs.getString("owner_id"));
                    p.setRazorpayOrderId(rs.getString("razorpay_order_id"));
                    p.setRazorpayPaymentId(rs.getString("razorpay_payment_id"));
                    p.setAmountPaise(rs.getLong("amount_paise"));
                    p.setCurrency(rs.getString("currency"));
                    p.setStatus(rs.getString("status"));
                    p.setMethod(rs.getString("method"));
                    p.setCreatedAt(rs.getLong("created_at"));
                    p.setUpdatedAt(rs.getLong("updated_at"));
                    return p;
                }
        );
    }

    public List<TransactionRecord> findAllTransactions() {
        return jdbcTemplate.query(
                "SELECT * FROM apex_payments WHERE method = 'LEGACY'",
                (rs, rowNum) -> {
                    TransactionRecord tx = new TransactionRecord();
                    tx.setTxId(rs.getString("payment_id"));
                    tx.setBookingId(rs.getString("booking_id"));
                    tx.setUserId(rs.getString("user_id"));
                    tx.setType(rs.getString("tx_type"));
                    tx.setAmount((int) (rs.getLong("amount_paise") / 100L));
                    tx.setGateway(rs.getString("gateway"));
                    tx.setGatewayRef(rs.getString("gateway_ref"));
                    tx.setStatus(rs.getString("status"));
                    tx.setTimestamp(rs.getLong("created_at"));
                    return tx;
                }
        );
    }

    // =========================================================================
    // 10. apex_otp_sessions
    // =========================================================================

    public void saveOtpSession(OtpSession s) {
        if (s == null || s.getSessionId() == null) return;
        long now = System.currentTimeMillis();
        long createdAt = s.getCreatedAt() != null ? s.getCreatedAt() : now;
        long expiresAt = s.getExpiresAt() != null ? s.getExpiresAt() : (createdAt + 300_000L);

        String updateSql = """
                UPDATE apex_otp_sessions SET
                    phone = ?, email = ?, phone_otp = ?, email_otp = ?, login_otp = ?,
                    user_id = ?, purpose = ?, created_at = ?, expires_at = ?,
                    attempts_remaining = ?, verified = ?
                WHERE session_id = ?
                """;
        String insertSql = """
                INSERT INTO apex_otp_sessions (
                    session_id, phone, email, phone_otp, email_otp, login_otp,
                    user_id, purpose, created_at, expires_at, attempts_remaining, verified
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    int i = 1;
                    setNullableString(ps, i++, s.getPhone());
                    setNullableString(ps, i++, s.getEmail());
                    setNullableString(ps, i++, s.getPhoneOtp());
                    setNullableString(ps, i++, s.getEmailOtp());
                    setNullableString(ps, i++, s.getLoginOtp());
                    setNullableString(ps, i++, s.getUserId());
                    ps.setString(i++, nonEmpty(s.getPurpose(), "REGISTRATION"));
                    ps.setLong(i++, createdAt);
                    ps.setLong(i++, expiresAt);
                    ps.setInt(i++, s.getAttemptsRemaining());
                    ps.setBoolean(i++, s.getVerified());
                    ps.setString(i, s.getSessionId());
                },
                insertSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, s.getSessionId());
                    setNullableString(ps, i++, s.getPhone());
                    setNullableString(ps, i++, s.getEmail());
                    setNullableString(ps, i++, s.getPhoneOtp());
                    setNullableString(ps, i++, s.getEmailOtp());
                    setNullableString(ps, i++, s.getLoginOtp());
                    setNullableString(ps, i++, s.getUserId());
                    ps.setString(i++, nonEmpty(s.getPurpose(), "REGISTRATION"));
                    ps.setLong(i++, createdAt);
                    ps.setLong(i++, expiresAt);
                    ps.setInt(i++, s.getAttemptsRemaining());
                    ps.setBoolean(i, s.getVerified());
                }
        );
    }

    public List<OtpSession> findAllOtpSessions() {
        return jdbcTemplate.query("SELECT * FROM apex_otp_sessions", (rs, rowNum) -> {
            OtpSession s = new OtpSession();
            s.setSessionId(rs.getString("session_id"));
            s.setPhone(rs.getString("phone"));
            s.setEmail(rs.getString("email"));
            s.setPhoneOtp(rs.getString("phone_otp"));
            s.setEmailOtp(rs.getString("email_otp"));
            s.setLoginOtp(rs.getString("login_otp"));
            s.setUserId(rs.getString("user_id"));
            s.setPurpose(rs.getString("purpose"));
            s.setCreatedAt(rs.getLong("created_at"));
            s.setExpiresAt(rs.getLong("expires_at"));
            s.setAttemptsRemaining(rs.getInt("attempts_remaining"));
            s.setVerified(rs.getBoolean("verified"));
            return s;
        });
    }

    // =========================================================================
    // 11. apex_webhook_events
    // =========================================================================

    public void saveWebhookEvent(WebhookEventRecord e) {
        if (e == null || e.getEventId() == null) return;
        long receivedAt = e.getReceivedAt() > 0 ? e.getReceivedAt() : System.currentTimeMillis();

        String updateSql = """
                UPDATE apex_webhook_events SET
                    event_type = ?, received_at = ?, processed_status = ?
                WHERE event_id = ?
                """;
        String insertSql = """
                INSERT INTO apex_webhook_events (
                    event_id, event_type, received_at, processed_status, payload_json
                ) VALUES (?, ?, ?, ?, NULL)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    ps.setString(1, nonEmpty(e.getEventType(), "unknown"));
                    ps.setLong(2, receivedAt);
                    ps.setString(3, nonEmpty(e.getProcessedStatus(), "PROCESSED"));
                    ps.setString(4, e.getEventId());
                },
                insertSql,
                ps -> {
                    ps.setString(1, e.getEventId());
                    ps.setString(2, nonEmpty(e.getEventType(), "unknown"));
                    ps.setLong(3, receivedAt);
                    ps.setString(4, nonEmpty(e.getProcessedStatus(), "PROCESSED"));
                }
        );
    }

    public List<WebhookEventRecord> findAllWebhookEvents() {
        return jdbcTemplate.query("SELECT * FROM apex_webhook_events", (rs, rowNum) -> {
            WebhookEventRecord e = new WebhookEventRecord();
            e.setEventId(rs.getString("event_id"));
            e.setEventType(rs.getString("event_type"));
            e.setReceivedAt(rs.getLong("received_at"));
            e.setProcessedStatus(rs.getString("processed_status"));
            return e;
        });
    }

    // =========================================================================
    // 12. apex_facilities (Legacy FacilityRecord compatibility)
    // =========================================================================

    public void saveFacility(FacilityRecord f) {
        if (f == null || f.getCourtId() == null) return;
        String amenitiesJson = toJson(f.getAmenities());
        long basePaise = (long) f.getBaseHourlyRate() * 100L;
        long peakPaise = (long) f.getPeakHourlyRate() * 100L;
        long createdAt = f.getCreatedAt() != null ? f.getCreatedAt() : System.currentTimeMillis();

        String updateSql = """
                UPDATE apex_facilities SET
                    venue_id = ?, name = ?, sport = ?, facility_type = ?, capacity = ?,
                    surface = ?, dimensions = ?, base_hourly_rate_paise = ?, peak_hourly_rate_paise = ?,
                    odd_hour_discount_pct = ?, has_floodlights = ?, amenities_json = ?, status = ?, created_at = ?
                WHERE court_id = ?
                """;
        String insertSql = """
                INSERT INTO apex_facilities (
                    court_id, venue_id, name, sport, facility_type, capacity,
                    surface, dimensions, base_hourly_rate_paise, peak_hourly_rate_paise,
                    odd_hour_discount_pct, has_floodlights, amenities_json, status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, nonEmpty(f.getVenueId(), "venue_01"));
                    ps.setString(i++, nonEmpty(f.getName(), "Court"));
                    ps.setString(i++, nonEmpty(f.getSport(), "cricket"));
                    setNullableString(ps, i++, f.getFacilityType());
                    ps.setInt(i++, f.getCapacity());
                    setNullableString(ps, i++, f.getSurface());
                    setNullableString(ps, i++, f.getDimensions());
                    ps.setLong(i++, basePaise);
                    ps.setLong(i++, peakPaise);
                    ps.setInt(i++, f.getOddHourDiscountPct());
                    ps.setBoolean(i++, f.getHasFloodlights());
                    setNullableString(ps, i++, amenitiesJson);
                    ps.setString(i++, nonEmpty(f.getStatus(), "ACTIVE"));
                    ps.setLong(i++, createdAt);
                    ps.setString(i, f.getCourtId());
                },
                insertSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, f.getCourtId());
                    ps.setString(i++, nonEmpty(f.getVenueId(), "venue_01"));
                    ps.setString(i++, nonEmpty(f.getName(), "Court"));
                    ps.setString(i++, nonEmpty(f.getSport(), "cricket"));
                    setNullableString(ps, i++, f.getFacilityType());
                    ps.setInt(i++, f.getCapacity());
                    setNullableString(ps, i++, f.getSurface());
                    setNullableString(ps, i++, f.getDimensions());
                    ps.setLong(i++, basePaise);
                    ps.setLong(i++, peakPaise);
                    ps.setInt(i++, f.getOddHourDiscountPct());
                    ps.setBoolean(i++, f.getHasFloodlights());
                    setNullableString(ps, i++, amenitiesJson);
                    ps.setString(i++, nonEmpty(f.getStatus(), "ACTIVE"));
                    ps.setLong(i, createdAt);
                }
        );
    }

    public List<FacilityRecord> findAllFacilities() {
        return jdbcTemplate.query("SELECT * FROM apex_facilities", (rs, rowNum) -> {
            FacilityRecord f = new FacilityRecord();
            f.setCourtId(rs.getString("court_id"));
            f.setVenueId(rs.getString("venue_id"));
            f.setName(rs.getString("name"));
            f.setSport(rs.getString("sport"));
            f.setFacilityType(rs.getString("facility_type"));
            f.setCapacity(rs.getInt("capacity"));
            f.setSurface(rs.getString("surface"));
            f.setDimensions(rs.getString("dimensions"));
            f.setBaseHourlyRate((int) (rs.getLong("base_hourly_rate_paise") / 100L));
            f.setPeakHourlyRate((int) (rs.getLong("peak_hourly_rate_paise") / 100L));
            f.setOddHourDiscountPct(rs.getInt("odd_hour_discount_pct"));
            f.setHasFloodlights(rs.getBoolean("has_floodlights"));
            f.setAmenities(fromJson(rs.getString("amenities_json"), LIST_STRING_TYPE, ArrayList::new));
            f.setStatus(rs.getString("status"));
            f.setCreatedAt(getNullableLong(rs, "created_at"));
            return f;
        });
    }

    // =========================================================================
    // 13. apex_audit_logs
    // =========================================================================

    public void saveAuditLog(AuditLog entry) {
        if (entry == null || entry.getLogId() == null) return;
        String metaJson = toJson(entry.getMetadata());
        long createdAt = entry.getTimestamp() != null ? entry.getTimestamp() : System.currentTimeMillis();

        String updateSql = """
                UPDATE apex_audit_logs SET
                    action = ?, actor_id = ?, resource_type = ?, resource_id = ?, metadata_json = ?, created_at = ?
                WHERE log_id = ?
                """;
        String insertSql = """
                INSERT INTO apex_audit_logs (
                    log_id, action, actor_id, resource_type, resource_id, metadata_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        executeUpsert(
                updateSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, nonEmpty(entry.getAction(), "SYSTEM_EVENT"));
                    setNullableString(ps, i++, entry.getActorId());
                    setNullableString(ps, i++, entry.getResourceType());
                    setNullableString(ps, i++, entry.getResourceId());
                    setNullableString(ps, i++, metaJson);
                    ps.setLong(i++, createdAt);
                    ps.setString(i, entry.getLogId());
                },
                insertSql,
                ps -> {
                    int i = 1;
                    ps.setString(i++, entry.getLogId());
                    ps.setString(i++, nonEmpty(entry.getAction(), "SYSTEM_EVENT"));
                    setNullableString(ps, i++, entry.getActorId());
                    setNullableString(ps, i++, entry.getResourceType());
                    setNullableString(ps, i++, entry.getResourceId());
                    setNullableString(ps, i++, metaJson);
                    ps.setLong(i, createdAt);
                }
        );
    }

    public List<AuditLog> findAllAuditLogs() {
        return jdbcTemplate.query("SELECT * FROM apex_audit_logs ORDER BY created_at ASC", (rs, rowNum) -> {
            AuditLog entry = new AuditLog();
            entry.setLogId(rs.getString("log_id"));
            entry.setAction(rs.getString("action"));
            entry.setActorId(rs.getString("actor_id"));
            entry.setResourceType(rs.getString("resource_type"));
            entry.setResourceId(rs.getString("resource_id"));
            entry.setMetadata(fromJson(rs.getString("metadata_json"), MAP_OBJECT_TYPE, HashMap::new));
            entry.setTimestamp(rs.getLong("created_at"));
            return entry;
        });
    }

    // =========================================================================
    // Shared SQL & JSON Helpers
    // =========================================================================

    private void executeUpsert(String updateSql, PreparedStatementSetter updateSetter,
                               String insertSql, PreparedStatementSetter insertSetter) {
        int updated = jdbcTemplate.update(updateSql, updateSetter);
        if (updated == 0) {
            try {
                jdbcTemplate.update(insertSql, insertSetter);
            } catch (DuplicateKeyException race) {
                jdbcTemplate.update(updateSql, updateSetter);
            }
        }
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("JSON serialization warning: {}", e.getMessage());
            return null;
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef, java.util.function.Supplier<T> fallback) {
        if (json == null || json.isBlank()) {
            return fallback.get();
        }
        try {
            T parsed = objectMapper.readValue(json, typeRef);
            return parsed != null ? parsed : fallback.get();
        } catch (Exception e) {
            log.warn("JSON deserialization warning: {}", e.getMessage());
            return fallback.get();
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("JSON deserialization warning for {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private static void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private static Long getNullableLong(ResultSet rs, String col) throws SQLException {
        long val = rs.getLong(col);
        return rs.wasNull() ? null : val;
    }

    private static Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int val = rs.getInt(col);
        return rs.wasNull() ? null : val;
    }

    private static String nonEmpty(String value, String fallback) {
        return (value != null && !value.isEmpty()) ? value : fallback;
    }
}
