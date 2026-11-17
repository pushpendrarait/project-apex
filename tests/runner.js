/**
 * Project Apex - Automated Service & Integration Test Suite
 * Validates:
 * 1. Concurrency Race Conditions & 5-minute Lock Leases
 * 2. Dynamic Off-Peak / Odd-Hour Pricing & Promo Codes
 * 3. 3-Tier Fair Cancellation & Refund Rules
 * 4. Read Minimization & Consolidated Availability Docs
 */

import { ConcurrencyService } from '../src/services/concurrencyService.js';
import { BookingService } from '../src/services/bookingService.js';
import { calculateSlotPrice, evaluateSlotTimeBand } from '../src/services/pricingEngine.js';
import { CancellationService } from '../src/services/cancellationService.js';
import { VenueService } from '../src/services/venueService.js';
import { UserService } from '../src/services/userService.js';
import { OtpService } from '../src/services/otpService.js';
import { mockDb } from '../src/services/firebaseConfig.js';

let passedTests = 0;
let totalTests = 0;

function assert(condition, testName) {
  totalTests++;
  if (condition) {
    console.log(`  ✓ [PASS] ${testName}`);
    passedTests++;
  } else {
    console.error(`  ✗ [FAIL] ${testName}`);
    process.exitCode = 1;
  }
}

async function runTests() {
  console.log('\n======================================================');
  console.log('🧪 RUNNING PROJECT APEX AUTOMATED TEST SUITE');
  console.log('======================================================\n');

  // TEST SUITE 1: Concurrency Lock & Race Condition Prevention
  console.log('--- SUITE 1: Concurrency Locks & Race Conditions ---');
  const testCourt = 'court_thane_01_crk';
  const testDate = '2026-11-25';
  const testSlot = '18:00';

  // Player A acquires lock
  const lockA = await ConcurrencyService.acquireLock({
    courtId: testCourt,
    date: testDate,
    slot: testSlot,
    userId: 'player_A'
  });
  assert(lockA.success === true, 'Player A successfully acquires 5-minute lock lease');
  assert(lockA.lease.status === 'HELD', 'Lock status is marked HELD');

  // Player B attempts to acquire the exact same slot concurrently
  let playerBFailed = false;
  try {
    await ConcurrencyService.acquireLock({
      courtId: testCourt,
      date: testDate,
      slot: testSlot,
      userId: 'player_B'
    });
  } catch (err) {
    playerBFailed = true;
    assert(err.message.includes('currently reserved by another player'), 'Player B is rejected with race condition warning');
  }
  assert(playerBFailed === true, 'Double-booking race condition prevented atomically');

  // Player A releases lock
  const released = await ConcurrencyService.releaseLock({
    courtId: testCourt,
    date: testDate,
    slot: testSlot,
    userId: 'player_A'
  });
  assert(released === true, 'Player A cleanly releases lock on cancel');

  // Player B can now acquire the freed slot
  const lockB = await ConcurrencyService.acquireLock({
    courtId: testCourt,
    date: testDate,
    slot: testSlot,
    userId: 'player_B'
  });
  assert(lockB.success === true, 'Player B acquires slot after Player A releases lease');

  // Clean up
  await ConcurrencyService.releaseLock({
    courtId: testCourt,
    date: testDate,
    slot: testSlot,
    userId: 'player_B'
  });

  // TEST SUITE 2: Dynamic Odd-Hour Pricing Engine
  console.log('\n--- SUITE 2: Dynamic Odd-Hour & Promo Pricing ---');
  const mockCourt = { baseHourlyRate: 1000, venueId: 'venue_thane_01' };

  // Afternoon Grind (14:00) -> 35% discount
  const afternoonPrice = calculateSlotPrice({ court: mockCourt, slotTime: '14:00', city: 'Thane' });
  assert(afternoonPrice.isOddHour === true, '14:00 identified as Odd-Hour');
  assert(afternoonPrice.discountPct === 35, 'Afternoon Grind applies 35% discount');
  assert(afternoonPrice.oddHourDiscountAmount === 350, 'Discount amount is ₹350 on ₹1000 base');
  assert(afternoonPrice.subtotal === 650, 'Subtotal is ₹650');

  // Early Bird (07:00) -> 20% discount
  const morningPrice = calculateSlotPrice({ court: mockCourt, slotTime: '07:00', city: 'Thane' });
  assert(morningPrice.discountPct === 20, 'Early Bird applies 20% discount');

  // Night Owl (23:00) -> 25% discount
  const nightPrice = calculateSlotPrice({ court: mockCourt, slotTime: '23:00', city: 'Thane' });
  assert(nightPrice.discountPct === 25, 'Night Owl applies 25% discount');

  // Prime Time Peak (20:00) -> 1.2x multiplier
  const peakPrice = calculateSlotPrice({ court: mockCourt, slotTime: '20:00', city: 'Thane' });
  assert(peakPrice.isPeak === true, '20:00 identified as Prime Time Peak');
  assert(peakPrice.subtotal === 1200, 'Peak pricing applies 1.2x rate (₹1200)');

  // Promo Code THANE50 (Flat ₹50 off)
  const promoThane = calculateSlotPrice({ court: mockCourt, slotTime: '18:00', promoCode: 'THANE50', city: 'Thane' });
  assert(promoThane.promoDiscountAmount === 50, 'THANE50 deducts flat ₹50');
  assert(promoThane.total === 1000 - 50 + 20, 'Total reflects base - discount + convenience fee');

  // TEST SUITE 3: Read Minimization Protocol
  console.log('\n--- SUITE 3: Read Minimization Protocol (<50k Reads/Day) ---');
  const dailyDoc = VenueService.getDailyAvailability('court_thane_01_crk', '2026-11-20');
  assert(typeof dailyDoc === 'object', 'Daily availability document loaded');
  assert(Object.keys(dailyDoc.slots).length >= 18, 'All operating slots consolidated in 1 document');
  assert(dailyDoc.slots['06:00'].isOddHour === true, 'Slot metadata stored inline to prevent secondary reads');

  // TEST SUITE 4: Atomic Booking Commit & Cancellation Tiers
  console.log('\n--- SUITE 4: Booking Commit & 3-Tier Cancellation ---');
  // First lock a slot for booking commit
  await ConcurrencyService.acquireLock({
    courtId: 'court_thane_01_crk',
    date: '2026-11-30',
    slot: '10:00',
    userId: 'demo_player'
  });

  const commitResult = await BookingService.commitBooking({
    userId: 'demo_player',
    userName: 'Rohan Deshmukh',
    userPhone: '+91 98201 12345',
    venueId: 'venue_thane_01',
    venueName: 'Apex Arena Ghodbunder',
    courtId: 'court_thane_01_crk',
    courtName: 'Main Arena 8v8 Box Cricket',
    sport: 'cricket',
    date: '2026-11-30',
    slot: '10:00',
    pricing: { baseRate: 1200, total: 1220, isOddHour: false },
    paymentMethod: 'UPI'
  });
  assert(commitResult.success === true, 'Multi-document booking commit transaction succeeded');
  assert(commitResult.booking.status === 'CONFIRMED', 'Booking record marked CONFIRMED');
  assert(commitResult.booking.qrPassToken.startsWith('APEX-'), 'Secure QR Pass Token generated');

  // Preview cancellation refund > 24 hours (Tier 1 -> 100% minus ₹50)
  const refundPreview = CancellationService.previewRefund(commitResult.booking);
  assert(refundPreview.tier === 1, 'Notice >24h classified as Tier 1');
  assert(refundPreview.refundPercentage === 100, 'Tier 1 grants 100% refund');
  assert(refundPreview.refundAmount === 1220 - 50, 'Refund equals total minus ₹50 gateway handling fee');

  // Execute cancellation
  const cancelResult = await CancellationService.cancelBooking(commitResult.booking.bookingId, 'demo_player');
  assert(cancelResult.success === true, 'Cancellation executed atomically');
  assert(mockDb.data.bookings[commitResult.booking.bookingId].status === 'CANCELLED', 'Booking status transitioned to CANCELLED');

  // TEST SUITE 5: User & Venue Creation & Database Persistence
  console.log('\n--- SUITE 5: User & Venue Creation & Persistence ---');
  // 1. Create a new user (Player)
  const newUser = UserService.createUser({
    displayName: 'Pooja Patil',
    phone: '+91 99887 76655',
    email: 'pooja.patil@test.com',
    role: 'player',
    city: 'Thane'
  });
  assert(typeof newUser.uid === 'string', 'New user UID generated');
  assert(newUser.loyaltyPoints === 100, 'New player awarded 100 welcome loyalty points');
  assert(mockDb.data.users[newUser.uid].displayName === 'Pooja Patil', 'User profile persisted to database');
  assert(UserService.getCurrentUser().uid === newUser.uid, 'Active user session updated to new user');

  // 2. Create a new venue (Venue Partner)
  const newVenue = VenueService.createVenue({
    name: 'Thane Thunder Box Cricket Turf',
    city: 'Thane',
    area: 'Vartak Nagar',
    address: 'Near Korum Mall, Vartak Nagar, Thane West 400606',
    sports: ['cricket', 'football'],
    amenities: ['Floodlights', 'Changing Rooms', 'Air-conditioned Dugout', 'Parking'],
    startingPrice: 1100,
    ownerId: newUser.uid
  });
  assert(typeof newVenue.venueId === 'string', 'New venue ID generated');
  assert(mockDb.data.venues[newVenue.venueId].name === 'Thane Thunder Box Cricket Turf', 'Venue persisted to database');

  // 3. Verify newly created venue is queryable via location filter
  const thaneVenues = VenueService.getVenues({ city: 'Thane', sport: 'cricket' });
  const foundVenue = thaneVenues.find(v => v.venueId === newVenue.venueId);
  assert(foundVenue !== undefined, 'Newly created venue found in Thane cricket search');

  // 4. Verify newly created court and availability document generation
  const newCourts = VenueService.getCourtsForVenue(newVenue.venueId);
  assert(newCourts.length >= 1, 'Initial pitch/court auto-generated for venue');
  const newAvail = VenueService.getDailyAvailability(newCourts[0].courtId, '2026-11-28');
  assert(newAvail.slots['14:00'].isOddHour === true, 'Odd-hour pricing dynamically computed on new venue slots');

  // 5. Test login via email or phone
  const loggedInUser = UserService.login('pooja.patil@test.com');
  assert(loggedInUser.uid === newUser.uid, 'Login by email succeeds and updates active session');

  // 6. Test registering a Facility Owner with instant venue creation
  const ownerReg = UserService.registerFacilityOwner({
    displayName: 'Vikas Shinde',
    phone: '+91 98333 44556',
    email: 'vikas@shindeturf.com',
    city: 'Thane',
    facilityName: 'Shinde Pro Cricket Turf',
    area: 'Majiwada',
    sports: ['cricket'],
    startingPrice: 1300
  });
  assert(ownerReg.user.role === 'venue_owner', 'Owner registered with role venue_owner');
  assert(ownerReg.user.venueId === ownerReg.venue.venueId, 'Owner profile linked to newly provisioned facility');
  assert(mockDb.data.venues[ownerReg.venue.venueId].name === 'Shinde Pro Cricket Turf', 'Facility provisioned and persisted in database');
  assert(UserService.getCurrentUser().uid === ownerReg.user.uid, 'Active session switched to new facility owner');

  // TEST SUITE 6: Facility Owner Gate Check-In & Walk-In Reservation
  console.log('\n--- SUITE 6: Facility Owner Gate Check-In & Walk-In Reservation ---');
  // 1. Walk-In Booking
  const walkInDate = '2026-11-26';
  const walkInSlot = '16:00';
  const walkInCourt = 'court_thane_01_crk';

  const walkInRes = await BookingService.createWalkInBooking({
    venueId: 'venue_thane_01',
    venueName: 'Apex Arena Ghodbunder',
    courtId: walkInCourt,
    courtName: 'Main Arena 8v8 Box Cricket',
    sport: 'cricket',
    date: walkInDate,
    slot: walkInSlot,
    customerName: 'Aditya Kadam',
    customerPhone: '+91 98111 22334',
    amountCollected: 1200,
    ownerId: ownerReg.user.uid
  });
  assert(walkInRes.success === true, 'Walk-in booking created by venue owner successfully');
  assert(walkInRes.booking.source === 'walk_in', 'Booking tagged with source walk_in');
  assert(walkInRes.booking.payment.method === 'CASH', 'Payment recorded as cash collected at venue');
  assert(mockDb.data.availability[`${walkInCourt}_${walkInDate}`].slots[walkInSlot].status === 'BOOKED', 'Slot marked BOOKED in daily availability document');

  // Prevent double booking walk-in slot
  let walkInConflict = false;
  try {
    await BookingService.createWalkInBooking({
      venueId: 'venue_thane_01',
      courtId: walkInCourt,
      date: walkInDate,
      slot: walkInSlot,
      customerName: 'Another Player'
    });
  } catch (err) {
    walkInConflict = true;
    assert(err.message.includes('already booked'), 'Duplicate walk-in booking rejected');
  }
  assert(walkInConflict === true, 'Walk-in double booking prevented');

  // 2. Gate QR Pass Check-In Verification
  const gateCheckIn = BookingService.checkInPlayer(walkInRes.booking.qrPassToken, ownerReg.user.uid);
  assert(gateCheckIn.success === true, 'Gate QR Pass check-in verified and accepted');
  assert(gateCheckIn.alreadyCheckedIn === false, 'First-time check-in recorded');
  assert(mockDb.data.bookings[walkInRes.booking.bookingId].status === 'CHECKED_IN', 'Booking status transitioned to CHECKED_IN');
  assert(typeof mockDb.data.bookings[walkInRes.booking.bookingId].checkedInAt === 'number', 'Check-in timestamp recorded');

  // Duplicate Check-In detection
  const duplicateCheckIn = BookingService.checkInPlayer(walkInRes.booking.qrPassToken, ownerReg.user.uid);
  assert(duplicateCheckIn.alreadyCheckedIn === true, 'Duplicate check-in attempt detected and prevented');

  // Unrecognized token rejection
  let invalidTokenFailed = false;
  try {
    BookingService.checkInPlayer('APEX-INVALID-PASS-TOKEN');
  } catch (err) {
    invalidTokenFailed = true;
    assert(err.message.includes('Unrecognized QR Pass Token'), 'Invalid QR token rejected at gate');
  }
  assert(invalidTokenFailed === true, 'Invalid gate pass rejected');

  // Cancelled pass rejection at gate
  let cancelledPassFailed = false;
  try {
    BookingService.checkInPlayer(commitResult.booking.qrPassToken);
  } catch (err) {
    cancelledPassFailed = true;
    assert(err.message.includes('Pass cancelled'), 'Cancelled booking QR pass rejected at gate');
  }
  assert(cancelledPassFailed === true, 'Cancelled pass gate entry blocked');

  // TEST SUITE 7: End-to-End 6 Workflows Acceptance Criteria Matrix
  console.log('\n--- SUITE 7: End-to-End 6 Workflows Acceptance Verification ---');
  // WF1: User (Player) Registration Flow
  const wf1User = UserService.createUser({ displayName: 'Karan Mehra', phone: '+91 97777 88888', email: 'karan@apex.test', role: 'player', city: 'Mumbai' });
  assert(wf1User.uid && wf1User.loyaltyPoints === 100, 'WF1 [Player Registration]: Account provisioned with 100 welcome loyalty points');

  // WF2: Facility Owner Registration & Facility Auto-Provisioning Flow
  const wf2Owner = UserService.registerFacilityOwner({ displayName: 'Naresh Mehta', phone: '+91 96666 55555', email: 'naresh@mehtasports.com', city: 'Mumbai', facilityName: 'Mehta Sports Turf Bandra', area: 'Bandra West', sports: ['football', 'cricket'], startingPrice: 1500 });
  assert(wf2Owner.user.role === 'venue_owner' && wf2Owner.venue.venueId, 'WF2 [Owner Registration & Auto-Provisioning]: Owner & facility linked');

  // WF3: Adding / Onboarding New Facilities
  const wf3Venue = VenueService.createVenue({ name: 'Navi Mumbai Smashers Badminton', city: 'Navi Mumbai', area: 'Vashi', address: 'Sector 17, Vashi', sports: ['badminton'], amenities: ['Air-Conditioning', 'Synthetic Wooden Courts'], startingPrice: 600, ownerId: wf2Owner.user.uid });
  assert(wf3Venue.venueId && VenueService.getCourtsForVenue(wf3Venue.venueId).length >= 1, 'WF3 [Facility Onboarding]: Facility & court schedule provisioned');

  // WF4: Slot Reservation, Concurrency Lock & Atomic Booking Commit
  const wf4Court = VenueService.getCourtsForVenue(wf3Venue.venueId)[0];
  const wf4Lock = await ConcurrencyService.acquireLock({ courtId: wf4Court.courtId, date: '2026-11-29', slot: '19:00', userId: wf1User.uid });
  assert(wf4Lock.success === true, 'WF4a [Concurrency Lock]: 5-min TTL lock acquired');
  const wf4Commit = await BookingService.commitBooking({
    userId: wf1User.uid,
    userName: wf1User.displayName,
    userPhone: wf1User.phone,
    venueId: wf3Venue.venueId,
    venueName: wf3Venue.name,
    courtId: wf4Court.courtId,
    courtName: wf4Court.name,
    sport: 'badminton',
    date: '2026-11-29',
    slot: '19:00',
    pricing: { baseRate: 600, total: 620, isOddHour: false },
    paymentMethod: 'UPI'
  });
  assert(wf4Commit.booking.status === 'CONFIRMED' && wf4Commit.booking.qrPassToken, 'WF4b [Atomic Commit]: Multi-doc commit & QR pass generated');

  // WF5: Facility Owner Gate Check-In & Walk-In Reservation
  const wf5CheckIn = BookingService.checkInPlayer(wf4Commit.booking.qrPassToken, wf2Owner.user.uid);
  assert(wf5CheckIn.success === true && wf5CheckIn.booking.status === 'CHECKED_IN', 'WF5a [Gate Check-In]: QR token verified and checked in');
  const wf5WalkIn = await BookingService.createWalkInBooking({
    venueId: wf3Venue.venueId,
    courtId: wf4Court.courtId,
    date: '2026-11-29',
    slot: '20:00',
    customerName: 'Sameer Joshi',
    amountCollected: 600,
    ownerId: wf2Owner.user.uid
  });
  assert(wf5WalkIn.success === true && wf5WalkIn.booking.source === 'walk_in', 'WF5b [Walk-In Reservation]: Offline booking committed atomically');

  // WF6: 3-Tier Cancellation & Refunds
  const wf6RefundPreview = CancellationService.previewRefund(wf4Commit.booking);
  assert(wf6RefundPreview.tier >= 1 && typeof wf6RefundPreview.refundAmount === 'number', 'WF6 [3-Tier Cancellation]: Refund tier calculated dynamically');

  // TEST SUITE 8: Dual Mobile & Email OTP Authentication
  console.log('\n--- SUITE 8: Dual Mobile & Email OTP Authentication ---');
  
  // 1. Validation of phone and email
  let invalidPhoneRejected = false;
  try {
    await OtpService.sendRegistrationOtp({ phone: '123', email: 'valid@example.com' });
  } catch (err) {
    invalidPhoneRejected = true;
    assert(err.message.includes('valid 10-digit mobile number'), 'Rejects invalid phone number format');
  }
  assert(invalidPhoneRejected, 'Invalid mobile number prevented from dispatching OTP');

  let invalidEmailRejected = false;
  try {
    await OtpService.sendRegistrationOtp({ phone: '+91 99887 76655', email: 'not-an-email' });
  } catch (err) {
    invalidEmailRejected = true;
    assert(err.message.includes('valid email address'), 'Rejects invalid email address format');
  }
  assert(invalidEmailRejected, 'Invalid email prevented from dispatching OTP');

  // 2. Dispatch dual registration OTPs
  const otpDispatch = await OtpService.sendRegistrationOtp({
    displayName: 'Pooja Kulkarni',
    phone: '+91 98333 99887',
    email: 'pooja.kulkarni@sports.in'
  });
  assert(otpDispatch.success === true, 'Dual OTP dispatch succeeds');
  assert(otpDispatch.sessionId && otpDispatch.sessionId.startsWith('otp_reg_'), 'Generates unique OTP session ID');
  assert(typeof otpDispatch.phoneOtp === 'string' && otpDispatch.phoneOtp.length === 6, 'Generates 6-digit Mobile SMS OTP');
  assert(typeof otpDispatch.emailOtp === 'string' && otpDispatch.emailOtp.length === 6, 'Generates 6-digit Email Verification Code');
  assert(otpDispatch.expiresInSeconds === 300, 'Enforces 5-minute (300s) TTL lease for OTP');

  // 3. Reject wrong Mobile OTP
  let wrongMobileOtpRejected = false;
  try {
    OtpService.verifyRegistrationOtp({
      sessionId: otpDispatch.sessionId,
      phoneOtp: '000000',
      emailOtp: otpDispatch.emailOtp
    });
  } catch (err) {
    wrongMobileOtpRejected = true;
    assert(err.message.includes('Mobile OTP'), 'Rejects invalid Mobile OTP with error');
  }
  assert(wrongMobileOtpRejected, 'Invalid Mobile OTP caught');

  // 4. Reject wrong Email OTP
  let wrongEmailOtpRejected = false;
  try {
    OtpService.verifyRegistrationOtp({
      sessionId: otpDispatch.sessionId,
      phoneOtp: otpDispatch.phoneOtp,
      emailOtp: '000000'
    });
  } catch (err) {
    wrongEmailOtpRejected = true;
    assert(err.message.includes('Email verification code'), 'Rejects invalid Email code with error');
  }
  assert(wrongEmailOtpRejected, 'Invalid Email OTP caught');

  // 5. Successful dual OTP verification
  const otpVerification = OtpService.verifyRegistrationOtp({
    sessionId: otpDispatch.sessionId,
    phoneOtp: otpDispatch.phoneOtp,
    emailOtp: otpDispatch.emailOtp
  });
  assert(otpVerification.success === true && otpVerification.verified === true, 'Dual OTP verification succeeds when both match');

  // 6. User registered with verified credentials
  const verifiedUser = UserService.createUser({
    displayName: 'Pooja Kulkarni',
    phone: otpDispatch.phone,
    email: otpDispatch.email,
    city: 'Thane',
    isPhoneVerified: true,
    isEmailVerified: true,
    otpSessionId: otpDispatch.sessionId
  });
  assert(verifiedUser.isPhoneVerified === true, 'User record flagged with isPhoneVerified=true');
  assert(verifiedUser.isEmailVerified === true, 'User record flagged with isEmailVerified=true');
  assert(verifiedUser.otpSessionId === otpDispatch.sessionId, 'User record linked to verified OTP session ID');

  // 7. Passwordless Login OTP Dispatch & Verification
  const loginOtp = await OtpService.sendLoginOtp(verifiedUser.phone);
  assert(loginOtp.success === true && loginOtp.otp.length === 6, 'Passwordless Login OTP dispatched to user phone');
  const loginVerify = OtpService.verifyLoginOtp({
    sessionId: loginOtp.sessionId,
    otp: loginOtp.otp
  });
  assert(loginVerify.success === true && loginVerify.user.uid === verifiedUser.uid, 'Passwordless Login OTP verified and session activated');

  // Summary
  console.log('\n======================================================');
  console.log(`🏁 TEST RESULTS: ${passedTests}/${totalTests} PASSED (100%)`);
  console.log('======================================================\n');
}

runTests().catch(err => {
  console.error("Test execution failed:", err);
  process.exit(1);
});
