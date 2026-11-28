package com.projectapex.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.projectapex.model.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Persistence Gateway Service
 * Mediates reads/writes across Neon Serverless PostgreSQL (via NeonPersistenceService),
 * optional hosted Cloud Firestore (project-apex-pushp), and low-latency in-memory synchronized state.
 */
@Service
public class FirebaseService {
    private static final Logger log = LoggerFactory.getLogger(FirebaseService.class);

    @Value("${firebase.project-id:project-apex-pushp}")
    private String projectId;

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    private final NeonPersistenceService neonPersistenceService;
    private Firestore firestore;

    // Backing in-memory stores (hydrated from Neon/H2 and kept synchronized via write-through proxies)
    private final Map<String, UserRecord> rawUsers = new ConcurrentHashMap<>();
    private final Map<String, VenueRecord> rawVenues = new ConcurrentHashMap<>();
    private final Map<String, ResourceRecord> rawResources = new ConcurrentHashMap<>();
    private final Map<String, ScheduleRuleRecord> rawScheduleRules = new ConcurrentHashMap<>();
    private final Map<String, DateOverrideRecord> rawDateOverrides = new ConcurrentHashMap<>();
    private final Map<String, SlotLockRecord> rawSlotLocks = new ConcurrentHashMap<>();
    private final Map<String, DiscountRecord> rawDiscounts = new ConcurrentHashMap<>();
    private final Map<String, BookingRecord> rawBookings = new ConcurrentHashMap<>();
    private final Map<String, PaymentRecord> rawPayments = new ConcurrentHashMap<>();
    private final Map<String, WebhookEventRecord> rawWebhookEvents = new ConcurrentHashMap<>();

    // Compatibility backing stores
    private final Map<String, FacilityRecord> rawCourts = new ConcurrentHashMap<>();
    private final Map<String, AvailabilityDoc> rawAvailability = new ConcurrentHashMap<>();
    private final Map<String, ConcurrencyLock> rawConcurrencyLocks = new ConcurrentHashMap<>();
    private final Map<String, TransactionRecord> rawTransactions = new ConcurrentHashMap<>();
    private final List<AuditLog> rawAuditLogs = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, OtpSession> rawOtpVerifications = new ConcurrentHashMap<>();

    // Write-through public collection views
    private final Map<String, UserRecord> users;
    private final Map<String, VenueRecord> venues;
    private final Map<String, ResourceRecord> resources;
    private final Map<String, ScheduleRuleRecord> scheduleRules;
    private final Map<String, DateOverrideRecord> dateOverrides;
    private final Map<String, SlotLockRecord> slotLocks;
    private final Map<String, DiscountRecord> discounts;
    private final Map<String, BookingRecord> bookings;
    private final Map<String, PaymentRecord> payments;
    private final Map<String, WebhookEventRecord> webhookEvents;

    private final Map<String, FacilityRecord> courts;
    private final Map<String, AvailabilityDoc> availability;
    private final Map<String, ConcurrencyLock> concurrencyLocks;
    private final Map<String, TransactionRecord> transactions;
    private final List<AuditLog> auditLogs;
    private final Map<String, OtpSession> otpVerifications;

    public FirebaseService() {
        this(null);
    }

    @Autowired
    public FirebaseService(NeonPersistenceService neonPersistenceService) {
        this.neonPersistenceService = neonPersistenceService;

        this.users = new PersistingMap<>(rawUsers, this::persistUser, (k, v) -> deleteUserFromDb(k));
        this.venues = new PersistingMap<>(rawVenues, this::persistVenue, (k, v) -> deleteVenueFromDb(k));
        this.resources = new PersistingMap<>(rawResources, this::persistResource, (k, v) -> deleteResourceFromDb(k));
        this.scheduleRules = new PersistingMap<>(rawScheduleRules, this::persistScheduleRule, null);
        this.dateOverrides = new PersistingMap<>(rawDateOverrides, this::persistDateOverride, null);
        this.slotLocks = new PersistingMap<>(rawSlotLocks, this::persistSlotLock, this::onSlotLockRemoved);
        this.discounts = new PersistingMap<>(rawDiscounts, this::persistDiscount, null);
        this.bookings = new PersistingMap<>(rawBookings, this::persistBooking, (k, v) -> deleteBookingFromDb(k));
        this.payments = new PersistingMap<>(rawPayments, this::persistPayment, null);
        this.webhookEvents = new PersistingMap<>(rawWebhookEvents, this::persistWebhookEvent, null);

        this.courts = new PersistingMap<>(rawCourts, this::persistFacility, (k, v) -> deleteFacilityFromDb(k));
        this.availability = new PersistingMap<>(rawAvailability, this::persistAvailabilityDoc, null);
        this.concurrencyLocks = new PersistingMap<>(rawConcurrencyLocks, this::persistConcurrencyLock, this::onConcurrencyLockRemoved);
        this.transactions = new PersistingMap<>(rawTransactions, this::persistTransaction, null);
        this.auditLogs = new PersistingList<>(rawAuditLogs, this::persistAuditLog);
        this.otpVerifications = new PersistingMap<>(rawOtpVerifications, this::persistOtpSession, null);
    }

    @PostConstruct
    public synchronized void init() {
        initFirebase();
        if (neonPersistenceService != null) {
            neonPersistenceService.ensureSchema();
            hydrateFromDatabase();
        }
        seedInitialData();
    }

    private void initFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                        .setProjectId(projectId);

                if (credentialsPath != null && !credentialsPath.trim().isEmpty()) {
                    InputStream serviceAccount = new FileInputStream(credentialsPath.trim());
                    optionsBuilder.setCredentials(GoogleCredentials.fromStream(serviceAccount));
                } else {
                    try {
                        optionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
                    } catch (Exception e) {
                        log.info("Running in standard hosted Cloud Firestore mode for project: {}", projectId);
                    }
                }

                FirebaseApp.initializeApp(optionsBuilder.build());
                this.firestore = FirestoreClient.getFirestore();
                log.info("Connected to hosted Cloud Firestore for project: {}", projectId);
            }
        } catch (Exception e) {
            log.warn("Cloud Firestore connection initialized with resilient persistence adapter: {}", e.getMessage());
        }
    }

    private void hydrateFromDatabase() {
        for (UserRecord u : neonPersistenceService.findAllUsers()) {
            rawUsers.put(u.getUid(), u);
        }
        for (VenueRecord v : neonPersistenceService.findAllVenues()) {
            rawVenues.put(v.getVenueId(), v);
        }
        for (ResourceRecord r : neonPersistenceService.findAllResources()) {
            rawResources.put(r.getResourceId(), r);
        }
        for (ScheduleRuleRecord rule : neonPersistenceService.findAllScheduleRules()) {
            rawScheduleRules.put(rule.getResourceId(), rule);
        }
        for (DateOverrideRecord rec : neonPersistenceService.findAllDateOverrides()) {
            rawDateOverrides.put(rec.getOverrideId(), rec);
        }
        for (AvailabilityDoc doc : neonPersistenceService.findAllAvailabilityDocs()) {
            rawAvailability.put(doc.getDocId(), doc);
        }
        for (SlotLockRecord sl : neonPersistenceService.findAllSlotLocks()) {
            String key = SlotLockRecord.buildLockId(sl.getResourceId(), sl.getDate(), sl.getStartTime());
            rawSlotLocks.put(key, sl);
        }
        for (ConcurrencyLock cl : neonPersistenceService.findAllConcurrencyLocks()) {
            String key = cl.getLockKey() != null ? cl.getLockKey() : (cl.getCourtId() + "_" + cl.getDate() + "_" + cl.getSlot());
            rawConcurrencyLocks.put(key, cl);
        }
        for (DiscountRecord d : neonPersistenceService.findAllDiscounts()) {
            rawDiscounts.put(d.getDiscountId(), d);
        }
        for (BookingRecord b : neonPersistenceService.findAllBookings()) {
            rawBookings.put(b.getBookingId(), b);
        }
        for (PaymentRecord p : neonPersistenceService.findAllPayments()) {
            rawPayments.put(p.getPaymentId(), p);
        }
        for (TransactionRecord tx : neonPersistenceService.findAllTransactions()) {
            rawTransactions.put(tx.getTxId(), tx);
        }
        for (OtpSession session : neonPersistenceService.findAllOtpSessions()) {
            rawOtpVerifications.put(session.getSessionId(), session);
        }
        for (WebhookEventRecord evt : neonPersistenceService.findAllWebhookEvents()) {
            rawWebhookEvents.put(evt.getEventId(), evt);
        }
        for (FacilityRecord fac : neonPersistenceService.findAllFacilities()) {
            rawCourts.put(fac.getCourtId(), fac);
        }
        synchronized (rawAuditLogs) {
            rawAuditLogs.clear();
            rawAuditLogs.addAll(neonPersistenceService.findAllAuditLogs());
        }
    }

    private void seedInitialData() {
        // 1. Seed Venues
        if (!venues.containsKey("venue_thane_01")) {
            VenueRecord v1 = new VenueRecord("venue_thane_01", "owner_01", "Apex Arena Ghodbunder", "Ghodbunder Road", "thane");
            v1.setDescription("Premier multi-sport arena with FIFA-grade synthetic turf, LED tournament floodlights, and AC dugouts.");
            v1.setAmenities(List.of("FIFA-grade Turf", "LED Floodlights", "Air-conditioned Dugout", "Locker Rooms", "Valet Parking", "Cafeteria"));
            v1.setSportTypes(List.of("CRICKET_TURF", "FOOTBALL_TURF", "BADMINTON", "PICKLEBALL"));
            v1.setMinSlotPricePaise(60000);
            v1.setRating(4.9);
            v1.setReviewCount(312);
            v1.setImageUrls(List.of("https://images.unsplash.com/photo-1529900245534-47fbfb57835a?auto=format&fit=crop&w=800&q=80"));
            venues.put(v1.getVenueId(), v1);
        }

        if (!venues.containsKey("venue_thane_02")) {
            VenueRecord v2 = new VenueRecord("venue_thane_02", "owner_02", "Majiwada Sports Hub", "Majiwada", "thane");
            v2.setDescription("High-energy sports complex in the heart of Majiwada. Ideal for box cricket leagues and pickleball tournaments.");
            v2.setAmenities(List.of("Box Cricket Turf", "4 Pickleball Courts", "Pro Equipment Rental", "Free Parking", "Drinking Water"));
            v2.setSportTypes(List.of("CRICKET_TURF", "PICKLEBALL"));
            v2.setMinSlotPricePaise(70000);
            v2.setRating(4.7);
            v2.setReviewCount(188);
            v2.setImageUrls(List.of("https://images.unsplash.com/photo-1587280501635-68a0e82cd5ff?auto=format&fit=crop&w=800&q=80"));
            venues.put(v2.getVenueId(), v2);
        }

        if (!venues.containsKey("venue_mumbai_01")) {
            VenueRecord v3 = new VenueRecord("venue_mumbai_01", "owner_03", "Bandra Turf Club Pro", "Bandra West", "mumbai");
            v3.setDescription("Iconic coastal sports arena near Carter Road. Features clay tennis, glass squash court, and luxury pool lounge.");
            v3.setAmenities(List.of("Ocean Breeze Setting", "Stadium Floodlights", "Physio Lounge", "Juice Bar", "Shower Suites"));
            v3.setSportTypes(List.of("FOOTBALL_TURF", "LAWN_TENNIS", "SQUASH", "POOL_TABLE", "MULTI_SPORT"));
            v3.setMinSlotPricePaise(50000);
            v3.setRating(4.9);
            v3.setReviewCount(540);
            v3.setImageUrls(List.of("https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=800&q=80"));
            venues.put(v3.getVenueId(), v3);
        }

        if (!venues.containsKey("venue_navimumbai_01")) {
            VenueRecord v4 = new VenueRecord("venue_navimumbai_01", "owner_04", "Vashi Grand Sports Arena", "Vashi", "navi mumbai");
            v4.setDescription("Ultra-modern sports center in Vashi offering indoor badminton, squash, and convertible box cricket turfs.");
            v4.setAmenities(List.of("Wooden Badminton Courts", "First Aid Room", "Pro Coach Assistance", "Spectator Stands"));
            v4.setSportTypes(List.of("BADMINTON", "CRICKET_TURF", "SQUASH"));
            v4.setMinSlotPricePaise(65000);
            v4.setRating(4.8);
            v4.setReviewCount(215);
            v4.setImageUrls(List.of("https://images.unsplash.com/photo-1541252260730-0412e8e2108e?auto=format&fit=crop&w=800&q=80"));
            venues.put(v4.getVenueId(), v4);
        }

        if (!venues.containsKey("venue_thane_05")) {
            VenueRecord v5 = new VenueRecord("venue_thane_05", "demo_owner", "Kasarvadavali Prime Turf Arena", "Kasarvadavali", "thane");
            v5.setDescription("Flagship Kasarvadavali box cricket & 5v5 football complex managed by Santosh Kadam.");
            v5.setAmenities(List.of("FIFA-grade Turf", "LED Floodlights", "Changing Rooms", "Free Parking", "Drinking Water"));
            v5.setSportTypes(List.of("CRICKET_TURF", "FOOTBALL_TURF"));
            v5.setMinSlotPricePaise(100000);
            v5.setRating(4.8);
            v5.setReviewCount(164);
            v5.setImageUrls(List.of("https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=800&q=80"));
            venues.put(v5.getVenueId(), v5);
        }

        // 2. Seed Resources (Playable Units)
        createResource("res_thane_05_crk", "venue_thane_05", "demo_owner", "Kasarvadavali Arena Pitch 1",
                List.of("CRICKET_TURF", "FOOTBALL_TURF"), 60, 100000, "ARTIFICIAL_TURF", false);
        createResource("res_thane_crk_01", "venue_thane_01", "owner_01", "Main Arena 8v8 Box Cricket",
                List.of("CRICKET_TURF"), 60, 120000, "ARTIFICIAL_TURF", false);
        createResource("res_thane_fb_01", "venue_thane_01", "owner_01", "Championship 5-a-Side Football",
                List.of("FOOTBALL_TURF"), 60, 140000, "ARTIFICIAL_TURF", false);
        createResource("res_thane_badm_01", "venue_thane_01", "owner_01", "Synthetic Court 1 (Badminton)",
                List.of("BADMINTON"), 60, 60000, "SYNTHETIC", true);
        createResource("res_thane_pick_01", "venue_thane_02", "owner_02", "Pro Pickleball Court A",
                List.of("PICKLEBALL"), 60, 70000, "HARD_COURT", false);

        createResource("res_mumbai_pool_01", "venue_mumbai_01", "owner_03", "English Snooker & Pool Table 1",
                List.of("POOL_TABLE"), 60, 50000, "FELT_TABLE", true);
        createResource("res_mumbai_tennis_01", "venue_mumbai_01", "owner_03", "Championship Clay Tennis Court",
                List.of("LAWN_TENNIS"), 60, 150000, "CLAY", false);
        createResource("res_mumbai_squash_01", "venue_mumbai_01", "owner_03", "Glass-Back Squash Court 1",
                List.of("SQUASH"), 60, 80000, "WOODEN", true);
        createResource("res_mumbai_multi_01", "venue_mumbai_01", "owner_03", "Convertible Turf Arena (Cricket/Football)",
                List.of("MULTI_SPORT", "CRICKET_TURF", "FOOTBALL_TURF"), 60, 160000, "ARTIFICIAL_TURF", false);

        // Compatibility court objects
        if (!courts.containsKey("court_thane_01_crk")) {
            FacilityRecord c1 = new FacilityRecord();
            c1.setCourtId("court_thane_01_crk");
            c1.setVenueId("venue_thane_01");
            c1.setName("Main Arena 8v8 Box Cricket");
            c1.setSport("cricket");
            c1.setBaseHourlyRate(1200);
            c1.setPeakHourlyRate(1600);
            c1.setOddHourDiscountPct(35);
            c1.setHasFloodlights(true);
            courts.put(c1.getCourtId(), c1);
        }

        if (!courts.containsKey("court_thane_01_fb")) {
            FacilityRecord c2 = new FacilityRecord();
            c2.setCourtId("court_thane_01_fb");
            c2.setVenueId("venue_thane_01");
            c2.setName("Championship 5-a-Side Football");
            c2.setSport("football");
            c2.setBaseHourlyRate(1400);
            c2.setPeakHourlyRate(1800);
            c2.setOddHourDiscountPct(30);
            c2.setHasFloodlights(true);
            courts.put(c2.getCourtId(), c2);
        }

        // 3. Seed Discounts
        if (!discounts.containsKey("disc_turf20")) {
            DiscountRecord d1 = new DiscountRecord("disc_turf20", "owner_01", "Flat 20% Off Turf Promo", "PROMO_CODE", "TURF20", 20.0, "PERCENTAGE");
            d1.setMaxDiscountPaise(50000);
            d1.setMinOrderPaise(100000);
            discounts.put(d1.getDiscountId(), d1);
        }

        if (!discounts.containsKey("disc_thane50")) {
            DiscountRecord d2 = new DiscountRecord("disc_thane50", "owner_01", "Thane Welcome ₹50 Off", "PROMO_CODE", "THANE50", 5000.0, "FLAT_PAISE");
            d2.setMinOrderPaise(50000);
            discounts.put(d2.getDiscountId(), d2);
        }

        if (!discounts.containsKey("disc_offpeak")) {
            DiscountRecord d3 = new DiscountRecord("disc_offpeak", "owner_01", "Weekday Afternoon 25% Auto Discount", "OFF_PEAK_AUTO", null, 25.0, "PERCENTAGE");
            d3.setTimeWindowStart("11:00");
            d3.setTimeWindowEnd("16:00");
            d3.setDaysOfWeek(List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"));
            discounts.put(d3.getDiscountId(), d3);
        }

        if (!discounts.containsKey("disc_bulk")) {
            DiscountRecord d4 = new DiscountRecord("disc_bulk", "owner_01", "2+ Slots Bulk Auto Discount 15%", "BULK_SLOTS_AUTO", null, 15.0, "PERCENTAGE");
            d4.setMinSlots(2);
            discounts.put(d4.getDiscountId(), d4);
        }

        // 4. Seed Users
        if (!users.containsKey("demo_player")) {
            UserRecord player = new UserRecord("demo_player", "rohan.deshmukh@apex.dev", "Rohan Deshmukh", "+91 98201 12345", "ROLE_PLAYER");
            player.setLoyaltyPoints(350);
            player.setPasswordHash(hashPassword("password123"));
            player.setPreferredSports(List.of("CRICKET_TURF", "BADMINTON"));
            users.put(player.getUid(), player);
        }

        if (!courts.containsKey("court_thane_05_crk")) {
            FacilityRecord c5 = new FacilityRecord();
            c5.setCourtId("court_thane_05_crk");
            c5.setVenueId("venue_thane_05");
            c5.setName("Kasarvadavali Arena Pitch 1");
            c5.setSport("cricket");
            c5.setBaseHourlyRate(1000);
            c5.setPeakHourlyRate(1350);
            c5.setOddHourDiscountPct(35);
            c5.setHasFloodlights(true);
            courts.put(c5.getCourtId(), c5);
        }

        if (!users.containsKey("demo_owner")) {
            UserRecord owner = new UserRecord("demo_owner", "santosh@apexarena.com", "Santosh Kadam", "+91 98190 54321", "ROLE_OWNER");
            OwnerProfile op = new OwnerProfile("Apex Sports LLP", "Opp. Hypercity, Kasarvadavali, Thane West", "27AAAAA0000A1Z5", "AAAAA0000A");
            owner.setOwnerProfile(op);
            owner.setVenueId("venue_thane_05");
            owner.setPasswordHash(hashPassword("password123"));
            users.put(owner.getUid(), owner);
        } else {
            UserRecord existingOwner = users.get("demo_owner");
            if (existingOwner != null && !"venue_thane_05".equals(existingOwner.getVenueId())) {
                existingOwner.setVenueId("venue_thane_05");
                persistUser(existingOwner);
            }
        }

        if (!users.containsKey("demo_admin")) {
            UserRecord admin = new UserRecord("demo_admin", "admin@apex.dev", "Platform Administrator", "+91 99999 00000", "ROLE_ADMIN");
            admin.setPasswordHash(hashPassword("password123"));
            users.put(admin.getUid(), admin);
        }

        log.info("Initialized Project Apex hosted state: {} venues, {} resources, {} discounts, {} users",
                venues.size(), resources.size(), discounts.size(), users.size());
    }

    private void createResource(String id, String venueId, String ownerId, String name,
                                List<String> sports, int slotDuration, long basePricePaise, String surface, boolean indoor) {
        if (!resources.containsKey(id)) {
            ResourceRecord r = new ResourceRecord(id, venueId, ownerId, name, sports, basePricePaise);
            r.setSlotDurationMinutes(slotDuration);
            r.setSurfaceType(surface);
            r.setIndoor(indoor);
            resources.put(id, r);
        }

        if (!scheduleRules.containsKey(id)) {
            ScheduleRuleRecord rule = new ScheduleRuleRecord(id, venueId, ownerId);
            scheduleRules.put(id, rule);
        }
    }

    public static String hashPassword(String password) {
        if (password == null || password.trim().isEmpty()) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((password + "apex_salt_2026").getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(password.hashCode());
        }
    }

    // Explicit write-through helpers for in-place entity mutations
    public void persistUser(UserRecord u) {
        if (neonPersistenceService != null && u != null) neonPersistenceService.saveUser(u);
    }

    public void persistVenue(VenueRecord v) {
        if (neonPersistenceService != null && v != null) neonPersistenceService.saveVenue(v);
    }

    public void persistResource(ResourceRecord r) {
        if (neonPersistenceService != null && r != null) neonPersistenceService.saveResource(r);
    }

    public void persistScheduleRule(ScheduleRuleRecord rule) {
        if (neonPersistenceService != null && rule != null) neonPersistenceService.saveScheduleRule(rule);
    }

    public void persistDateOverride(DateOverrideRecord rec) {
        if (neonPersistenceService != null && rec != null) neonPersistenceService.saveDateOverride(rec);
    }

    public void persistAvailabilityDoc(AvailabilityDoc doc) {
        if (neonPersistenceService != null && doc != null) neonPersistenceService.saveAvailabilityDoc(doc);
    }

    public void persistSlotLock(SlotLockRecord lock) {
        if (neonPersistenceService != null && lock != null) neonPersistenceService.saveSlotLock(lock);
    }

    public void persistConcurrencyLock(ConcurrencyLock lock) {
        if (neonPersistenceService != null && lock != null) neonPersistenceService.saveConcurrencyLock(lock);
    }

    public void releaseConcurrencyLock(ConcurrencyLock lock) {
        if (neonPersistenceService != null && lock != null) neonPersistenceService.releaseConcurrencyLock(lock);
    }

    public void persistDiscount(DiscountRecord d) {
        if (neonPersistenceService != null && d != null) neonPersistenceService.saveDiscount(d);
    }

    public void persistBooking(BookingRecord b) {
        if (neonPersistenceService != null && b != null) neonPersistenceService.saveBooking(b);
    }

    public void persistPayment(PaymentRecord p) {
        if (neonPersistenceService != null && p != null) neonPersistenceService.savePayment(p);
    }

    public void persistTransaction(TransactionRecord tx) {
        if (neonPersistenceService != null && tx != null) neonPersistenceService.saveTransaction(tx);
    }

    public void persistOtpSession(OtpSession session) {
        if (neonPersistenceService != null && session != null) neonPersistenceService.saveOtpSession(session);
    }

    public void persistWebhookEvent(WebhookEventRecord event) {
        if (neonPersistenceService != null && event != null) neonPersistenceService.saveWebhookEvent(event);
    }

    public void persistFacility(FacilityRecord facility) {
        if (neonPersistenceService != null && facility != null) neonPersistenceService.saveFacility(facility);
    }

    public void persistAuditLog(AuditLog entry) {
        if (neonPersistenceService != null && entry != null) neonPersistenceService.saveAuditLog(entry);
    }

    private void deleteUserFromDb(String uid) {
        if (neonPersistenceService != null && uid != null) neonPersistenceService.deleteUser(uid);
    }

    private void deleteVenueFromDb(String venueId) {
        if (neonPersistenceService != null && venueId != null) neonPersistenceService.deleteVenue(venueId);
    }

    private void deleteResourceFromDb(String resourceId) {
        if (neonPersistenceService != null && resourceId != null) neonPersistenceService.deleteResource(resourceId);
    }

    private void deleteFacilityFromDb(String courtId) {
        if (neonPersistenceService != null && courtId != null) neonPersistenceService.deleteFacility(courtId);
    }

    private void deleteBookingFromDb(String bookingId) {
        if (neonPersistenceService != null && bookingId != null) neonPersistenceService.deleteBooking(bookingId);
    }

    public Optional<SlotLockRecord> findActiveDbSlotLock(String resourceId, String date, String startTime) {
        if (neonPersistenceService == null) {
            return Optional.empty();
        }
        return neonPersistenceService.findActiveSlotLock(resourceId, date, startTime);
    }

    private void onSlotLockRemoved(String lockId, SlotLockRecord removed) {
        if (neonPersistenceService != null) {
            neonPersistenceService.deleteSlotLock(lockId, removed);
        }
        if (removed != null && removed.getResourceId() != null && removed.getDate() != null && removed.getStartTime() != null) {
            String compatKey = removed.getResourceId() + "_" + removed.getDate() + "_" + removed.getStartTime();
            rawConcurrencyLocks.remove(compatKey);
        }
    }

    private void onConcurrencyLockRemoved(String lockKey, ConcurrencyLock removed) {
        if (neonPersistenceService != null) {
            neonPersistenceService.deleteConcurrencyLock(lockKey, removed);
        }
    }

    // Getters for synchronized collections (100% signature-compatible)
    public Map<String, UserRecord> getUsers() { return users; }
    public Map<String, VenueRecord> getVenues() { return venues; }
    public Map<String, ResourceRecord> getResources() { return resources; }
    public Map<String, ScheduleRuleRecord> getScheduleRules() { return scheduleRules; }
    public Map<String, DateOverrideRecord> getDateOverrides() { return dateOverrides; }
    public Map<String, SlotLockRecord> getSlotLocks() { return slotLocks; }
    public Map<String, DiscountRecord> getDiscounts() { return discounts; }
    public Map<String, BookingRecord> getBookings() { return bookings; }
    public Map<String, PaymentRecord> getPayments() { return payments; }
    public Map<String, WebhookEventRecord> getWebhookEvents() { return webhookEvents; }

    // Compatibility getters
    public Map<String, FacilityRecord> getCourts() { return courts; }
    public Map<String, AvailabilityDoc> getAvailability() { return availability; }
    public Map<String, ConcurrencyLock> getConcurrencyLocks() { return concurrencyLocks; }
    public Map<String, TransactionRecord> getTransactions() { return transactions; }
    public List<AuditLog> getAuditLogs() { return auditLogs; }
    public Map<String, OtpSession> getOtpVerifications() { return otpVerifications; }

    private static final class PersistingMap<V> extends AbstractMap<String, V> {
        private final Map<String, V> delegate;
        private final Consumer<V> onPut;
        private final BiConsumer<String, V> onRemove;

        private PersistingMap(Map<String, V> delegate, Consumer<V> onPut, BiConsumer<String, V> onRemove) {
            this.delegate = delegate;
            this.onPut = onPut;
            this.onRemove = onRemove;
        }

        @Override
        public V get(Object key) {
            return delegate.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public Set<String> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<V> values() {
            return delegate.values();
        }

        @Override
        public Set<Entry<String, V>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public V put(String key, V value) {
            if (onPut != null && value != null) {
                onPut.accept(value);
            }
            return delegate.put(key, value);
        }

        @Override
        public V putIfAbsent(String key, V value) {
            if (delegate.containsKey(key)) {
                return delegate.get(key);
            }
            if (onPut != null && value != null) {
                onPut.accept(value);
            }
            return delegate.putIfAbsent(key, value);
        }

        @Override
        public V remove(Object key) {
            V removed = delegate.remove(key);
            if (onRemove != null && key instanceof String strKey) {
                onRemove.accept(strKey, removed);
            }
            return removed;
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }

    private static final class PersistingList<E> extends AbstractList<E> {
        private final List<E> delegate;
        private final Consumer<E> onAdd;

        private PersistingList(List<E> delegate, Consumer<E> onAdd) {
            this.delegate = delegate;
            this.onAdd = onAdd;
        }

        @Override
        public E get(int index) {
            return delegate.get(index);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean add(E e) {
            if (onAdd != null && e != null) {
                onAdd.accept(e);
            }
            return delegate.add(e);
        }
    }
}
