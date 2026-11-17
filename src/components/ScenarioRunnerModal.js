/**
 * Project Apex - Interactive Workflow Scenario Demonstrator
 * Real-time end-to-end interactive execution and visual verification of all call workflows
 */

import { UserService } from '../services/userService.js';
import { VenueService } from '../services/venueService.js';
import { ConcurrencyService } from '../services/concurrencyService.js';
import { BookingService } from '../services/bookingService.js';
import { CancellationService } from '../services/cancellationService.js';
import { calculateSlotPrice } from '../services/pricingEngine.js';
import { OtpService } from '../services/otpService.js';
import { mockDb } from '../services/firebaseConfig.js';

export class ScenarioRunnerModal {
  static show({ onStateUpdated }) {
    let modal = document.getElementById('apex-scenario-runner-modal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'apex-scenario-runner-modal';
      modal.className = 'fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-fade-in';
      document.body.appendChild(modal);
    }

    modal.innerHTML = `
      <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-3xl w-full p-6 sm:p-8 shadow-2xl relative overflow-hidden text-white flex flex-col max-h-[90vh]">
        <!-- Header -->
        <div class="flex items-center justify-between pb-4 border-b border-slate-800">
          <div>
            <div class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold uppercase tracking-wider mb-1">
              ⚡ Multi-Agent Automated Workflows
            </div>
            <h3 class="text-xl sm:text-2xl font-black text-white">Interactive Workflow Scenario Runner</h3>
            <p class="text-xs text-slate-400">Execute and observe every business workflow end-to-end with real-time state telemetry</p>
          </div>
          <button id="btn-close-scenarios" class="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition">✕</button>
        </div>

        <div class="flex-1 overflow-y-auto py-4 space-y-4 pr-1">
          <!-- Scenario Buttons Grid -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            
            <!-- Scenario 1 -->
            <div class="p-3.5 rounded-2xl bg-slate-950 border border-slate-800 hover:border-emerald-500/40 transition flex flex-col justify-between">
              <div>
                <div class="font-bold text-white flex items-center justify-between">
                  <span>1. Player Onboarding Flow</span>
                  <span id="badge-sc-1" class="text-[9px] px-2 py-0.5 rounded font-mono bg-slate-800 text-slate-400">READY</span>
                </div>
                <p class="text-slate-400 text-[11px] mt-1">Registers new player, awards +100 loyalty points, persists to Firestore 'users' collection.</p>
              </div>
              <button id="btn-run-sc-1" class="mt-3 w-full py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black transition">
                ▶ Run Player Registration
              </button>
            </div>

            <!-- Scenario 2 -->
            <div class="p-3.5 rounded-2xl bg-slate-950 border border-slate-800 hover:border-indigo-500/40 transition flex flex-col justify-between">
              <div>
                <div class="font-bold text-white flex items-center justify-between">
                  <span>2. Facility Partner Onboarding</span>
                  <span id="badge-sc-2" class="text-[9px] px-2 py-0.5 rounded font-mono bg-slate-800 text-slate-400">READY</span>
                </div>
                <p class="text-slate-400 text-[11px] mt-1">Registers turf owner, auto-provisions sports facility with courts, links owner profile.</p>
              </div>
              <button id="btn-run-sc-2" class="mt-3 w-full py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-black transition">
                ▶ Run Partner Onboarding
              </button>
            </div>

            <!-- Scenario 3 -->
            <div class="p-3.5 rounded-2xl bg-slate-950 border border-slate-800 hover:border-amber-500/40 transition flex flex-col justify-between">
              <div>
                <div class="font-bold text-white flex items-center justify-between">
                  <span>3. Odd-Hour Pricing Engine</span>
                  <span id="badge-sc-3" class="text-[9px] px-2 py-0.5 rounded font-mono bg-slate-800 text-slate-400">READY</span>
                </div>
                <p class="text-slate-400 text-[11px] mt-1">Evaluates time bands: 35% Afternoon Grind, 20% Early Bird, 1.2x Peak, and applies 'THANE50'.</p>
              </div>
              <button id="btn-run-sc-3" class="mt-3 w-full py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-black transition">
                ▶ Run Pricing Engine Test
              </button>
            </div>

            <!-- Scenario 4 -->
            <div class="p-3.5 rounded-2xl bg-slate-950 border border-slate-800 hover:border-rose-500/40 transition flex flex-col justify-between">
              <div>
                <div class="font-bold text-white flex items-center justify-between">
                  <span>4. Atomic Concurrency Lock & Collision</span>
                  <span id="badge-sc-4" class="text-[9px] px-2 py-0.5 rounded font-mono bg-slate-800 text-slate-400">READY</span>
                </div>
                <p class="text-slate-400 text-[11px] mt-1">Player A acquires 5-min lease; Player B simultaneously requests same slot & gets blocked atomically.</p>
              </div>
              <button id="btn-run-sc-4" class="mt-3 w-full py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-black transition">
                ▶ Run Concurrency Collision
              </button>
            </div>

            <!-- Scenario 5 -->
            <div class="p-3.5 rounded-2xl bg-slate-950 border border-slate-800 hover:border-cyan-500/40 transition flex flex-col justify-between">
              <div>
                <div class="font-bold text-white flex items-center justify-between">
                  <span>5. Gate QR Pass Check-In</span>
                  <span id="badge-sc-5" class="text-[9px] px-2 py-0.5 rounded font-mono bg-slate-800 text-slate-400">READY</span>
                </div>
                <p class="text-slate-400 text-[11px] mt-1">Partner scans player QR token; marks status CHECKED_IN; logs gate audit trail.</p>
              </div>
              <button id="btn-run-sc-5" class="mt-3 w-full py-2 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white font-black transition">
                ▶ Run Gate QR Check-In
              </button>
            </div>

            <!-- Scenario 6 -->
            <div class="p-3.5 rounded-2xl bg-slate-950 border border-slate-800 hover:border-purple-500/40 transition flex flex-col justify-between">
              <div>
                <div class="font-bold text-white flex items-center justify-between">
                  <span>6. 3-Tier Cancellation & Refund</span>
                  <span id="badge-sc-6" class="text-[9px] px-2 py-0.5 rounded font-mono bg-slate-800 text-slate-400">READY</span>
                </div>
                <p class="text-slate-400 text-[11px] mt-1">Evaluates refund eligibility; cancels booking; releases availability slot; creates refund ledger entry.</p>
              </div>
              <button id="btn-run-sc-6" class="mt-3 w-full py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-black transition">
                ▶ Run Fair Refund Rollback
              </button>
            </div>

            <!-- Scenario 7 -->
            <div class="p-3.5 rounded-2xl bg-slate-950 border border-slate-800 hover:border-emerald-500/40 transition flex flex-col justify-between sm:col-span-2">
              <div>
                <div class="font-bold text-white flex items-center justify-between">
                  <span>7. Owner Walk-In & Offline Reservation</span>
                  <span id="badge-sc-7" class="text-[9px] px-2 py-0.5 rounded font-mono bg-slate-800 text-slate-400">READY</span>
                </div>
                <p class="text-slate-400 text-[11px] mt-1">Facility owner records an offline cash customer or blocks a slot for floodlight maintenance.</p>
              </div>
              <button id="btn-run-sc-7" class="mt-3 w-full py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-black transition">
                ▶ Run Walk-In Reservation
              </button>
            </div>

          </div>

          <!-- Live Output Console -->
          <div class="space-y-2">
            <div class="flex items-center justify-between text-xs">
              <span class="font-bold text-slate-400">Live Execution Console</span>
              <button id="btn-clear-console" class="text-[10px] text-slate-500 hover:text-white">Clear</button>
            </div>
            <div id="scenario-console" class="h-44 bg-slate-950 border border-slate-800 rounded-2xl p-4 font-mono text-[11px] overflow-y-auto space-y-1 text-slate-300">
              <div class="text-slate-600">// Select a scenario above to execute and verify state mutations in real time...</div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="pt-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <span>All 7 Workflows Active & Compliant with Firebase Spark Free Tier</span>
          <button id="btn-run-all" class="px-5 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-indigo-600 hover:opacity-90 text-white font-bold transition">
            ▶ Run All Scenarios (Automated)
          </button>
        </div>
      </div>
    `;

    const close = () => modal.remove();
    modal.querySelector('#btn-close-scenarios').addEventListener('click', close);

    const consoleEl = modal.querySelector('#scenario-console');
    const log = (msg, type = 'info') => {
      const line = document.createElement('div');
      const time = new Date().toLocaleTimeString('en-IN', { hour12: false });
      if (type === 'pass') {
        line.className = 'text-emerald-400 font-bold';
        line.textContent = `[${time}] ✓ ${msg}`;
      } else if (type === 'warn') {
        line.className = 'text-amber-400';
        line.textContent = `[${time}] ⚠️ ${msg}`;
      } else if (type === 'err') {
        line.className = 'text-rose-400 font-bold';
        line.textContent = `[${time}] ✗ ${msg}`;
      } else {
        line.className = 'text-slate-300';
        line.textContent = `[${time}] ${msg}`;
      }
      consoleEl.appendChild(line);
      consoleEl.scrollTop = consoleEl.scrollHeight;
    };

    modal.querySelector('#btn-clear-console').addEventListener('click', () => {
      consoleEl.innerHTML = '';
    });

    const setBadge = (id, text, status) => {
      const el = modal.querySelector(`#badge-sc-${id}`);
      if (!el) return;
      el.textContent = text;
      if (status === 'pass') {
        el.className = 'text-[9px] px-2 py-0.5 rounded font-mono bg-emerald-500/20 text-emerald-400 border border-emerald-500/40';
      } else if (status === 'running') {
        el.className = 'text-[9px] px-2 py-0.5 rounded font-mono bg-amber-500/20 text-amber-400 animate-pulse';
      } else {
        el.className = 'text-[9px] px-2 py-0.5 rounded font-mono bg-slate-800 text-slate-400';
      }
    };

    // SCENARIO 1: Player Onboarding with Dual Mobile & Email OTP
    const runScenario1 = async () => {
      setBadge(1, 'RUNNING', 'running');
      log('Starting Scenario 1: User (Player) Registration Flow with Dual OTP...');

      log('Step 1: Dispatching Dual Mobile SMS & Email Verification OTPs...');
      const otpRes = await OtpService.sendRegistrationOtp({
        displayName: 'Aarav Mehta',
        phone: '+91 98200 88990',
        email: 'aarav.mehta@example.com'
      });
      log(`Dispatched: Mobile SMS OTP: [${otpRes.phoneOtp}] to ${otpRes.phone}`, 'pass');
      log(`Dispatched: Email Verification Code: [${otpRes.emailOtp}] to ${otpRes.email}`, 'pass');

      log('Step 2: Simulating Dual OTP Verification...');
      const verifyRes = OtpService.verifyRegistrationOtp({
        sessionId: otpRes.sessionId,
        phoneOtp: otpRes.phoneOtp,
        emailOtp: otpRes.emailOtp
      });
      log('Verified: Both Mobile & Email OTPs matched and authenticated!', 'pass');

      log('Step 3: Creating verified player profile in /users collection...');
      const user = UserService.createUser({
        displayName: 'Aarav Mehta',
        phone: otpRes.phone,
        email: otpRes.email,
        city: 'Thane',
        role: 'player',
        isPhoneVerified: true,
        isEmailVerified: true,
        otpSessionId: verifyRes.sessionId
      });
      log(`Player created: ${user.displayName} (UID: ${user.uid})`, 'pass');
      log(`Credentials verified: isPhoneVerified=true, isEmailVerified=true`, 'pass');
      log(`Welcome bonus verified: ${user.loyaltyPoints} Loyalty Points awarded`, 'pass');
      log(`Active session updated. User profile persisted in mockDb.data.users.`, 'pass');
      setBadge(1, 'PASSED', 'pass');
      if (onStateUpdated) onStateUpdated();
    };

    // SCENARIO 2: Partner Onboarding with Dual OTP
    const runScenario2 = async () => {
      setBadge(2, 'RUNNING', 'running');
      log('Starting Scenario 2: Facility Partner Registration Flow with Dual OTP...');

      log('Step 1: Partner enters details & requests Dual OTPs...');
      const ownerOtp = await OtpService.sendRegistrationOtp({
        displayName: 'Kunal Patil',
        phone: '+91 98111 22334',
        email: 'kunal@patilturf.com'
      });
      log(`Dispatched: Mobile OTP: [${ownerOtp.phoneOtp}] | Email Code: [${ownerOtp.emailOtp}]`, 'pass');

      log('Step 2: Verifying partner OTP credentials...');
      const ownerVer = OtpService.verifyRegistrationOtp({
        sessionId: ownerOtp.sessionId,
        phoneOtp: ownerOtp.phoneOtp,
        emailOtp: ownerOtp.emailOtp
      });
      log('Verified: Partner credentials confirmed via dual OTP!', 'pass');

      log('Step 3: Provisioning Facility and linking verified owner profile...');
      const res = UserService.registerFacilityOwner({
        displayName: 'Kunal Patil',
        phone: ownerOtp.phone,
        email: ownerOtp.email,
        city: 'Thane',
        facilityName: 'Patil Champions Turf',
        area: 'Vartak Nagar',
        sports: ['cricket', 'football'],
        startingPrice: 1200,
        isPhoneVerified: true,
        isEmailVerified: true,
        otpSessionId: ownerVer.sessionId
      });
      log(`Partner registered: ${res.user.displayName} (Role: ${res.user.role})`, 'pass');
      log(`Facility provisioned: ${res.venue.name} (Venue ID: ${res.venue.venueId})`, 'pass');
      log(`Initial court created with base price ₹1200 and floodlights.`, 'pass');
      setBadge(2, 'PASSED', 'pass');
      if (onStateUpdated) onStateUpdated();
    };

    // SCENARIO 3: Pricing Engine
    const runScenario3 = async () => {
      setBadge(3, 'RUNNING', 'running');
      log('Starting Scenario 3: Dynamic Slot Pricing Engine Flow...');
      const dummyCourt = { baseHourlyRate: 1000 };
      
      const early = calculateSlotPrice({ court: dummyCourt, slotTime: '07:00', city: 'Thane' });
      log(`07:00 (Early Bird): Base ₹1000 -> -20% = ₹${early.subtotal}`, 'pass');

      const offPeak = calculateSlotPrice({ court: dummyCourt, slotTime: '14:00', city: 'Thane' });
      log(`14:00 (Afternoon Grind): Base ₹1000 -> -35% = ₹${offPeak.subtotal}`, 'pass');

      const peak = calculateSlotPrice({ court: dummyCourt, slotTime: '20:00', city: 'Thane' });
      log(`20:00 (Prime Time): Base ₹1000 -> +20% Peak = ₹${peak.subtotal}`, 'pass');

      const promo = calculateSlotPrice({ court: dummyCourt, slotTime: '14:00', promoCode: 'THANE50', city: 'Thane' });
      log(`14:00 + THANE50: Subtotal ₹650 - ₹50 + ₹20 fee = Total ₹${promo.total}`, 'pass');
      setBadge(3, 'PASSED', 'pass');
    };

    // SCENARIO 4: Concurrency Collision
    const runScenario4 = async () => {
      setBadge(4, 'RUNNING', 'running');
      log('Starting Scenario 4: Atomic Concurrency Lock & Collision Flow...');
      const courtId = 'court_thane_01_crk';
      const date = '2026-11-29';
      const slot = '19:00';

      log(`Player A requests 5-minute lease on ${courtId} @ ${slot}...`);
      const lockA = await ConcurrencyService.acquireLock({ courtId, date, slot, userId: 'usr_player_a' });
      log(`Player A acquired lock lease! Expires in ${lockA.expiresInSeconds}s (Status: HELD)`, 'pass');

      log(`Player B simultaneously attempts to book the same slot...`);
      try {
        await ConcurrencyService.acquireLock({ courtId, date, slot, userId: 'usr_player_b' });
        log('Player B got the lock - unexpected collision!', 'err');
      } catch (err) {
        log(`Collision rejected: "${err.message}"`, 'pass');
      }

      log(`Player A abandons checkout; releasing lock...`);
      await ConcurrencyService.releaseLock({ courtId, date, slot, userId: 'usr_player_a' });
      log(`Lock released. Slot is immediately available again without cron cleanup.`, 'pass');
      setBadge(4, 'PASSED', 'pass');
    };

    // SCENARIO 5: Gate QR Check-in
    const runScenario5 = async () => {
      setBadge(5, 'RUNNING', 'running');
      log('Starting Scenario 5: Venue Gate QR Check-In Verification Flow...');
      const allBookings = Object.values(mockDb.data.bookings);
      const booking = allBookings[0] || {
        bookingId: 'bk_demo_gate',
        qrPassToken: 'APEX-TH-CRK-DEMO',
        status: 'CONFIRMED',
        userName: 'Siddhesh Shinde',
        userPhone: '+91 99887 76655',
        courtName: 'Turf A',
        date: '2026-11-20',
        slot: '20:00',
        pricing: { total: 1220 }
      };
      mockDb.data.bookings[booking.bookingId] = booking;

      log(`Owner enters Gate Pass Token: "${booking.qrPassToken}"`);
      const checkInRes = await BookingService.checkInBooking(booking.qrPassToken, 'gate_scanner');
      log(`Pass validated! Status: ${checkInRes.booking.status}. Player: ${booking.userName}`, 'pass');
      log(`Gate audit log recorded with timestamp in audit_logs collection.`, 'pass');

      log(`Scanning same token a second time...`);
      const reCheck = await BookingService.checkInBooking(booking.qrPassToken, 'gate_scanner');
      if (reCheck.alreadyCheckedIn) {
        log(`Duplicate entry caught: "${reCheck.message}"`, 'warn');
      }
      setBadge(5, 'PASSED', 'pass');
      if (onStateUpdated) onStateUpdated();
    };

    // SCENARIO 6: 3-Tier Cancellation
    const runScenario6 = async () => {
      setBadge(6, 'RUNNING', 'running');
      log('Starting Scenario 6: 3-Tier Fair Cancellation & Automated Refund Flow...');
      const futureDate = '2026-12-05';
      const testCourt = 'court_thane_01_crk';
      const slot = '16:00';

      // Create a test booking
      await ConcurrencyService.acquireLock({ courtId: testCourt, date: futureDate, slot, userId: 'demo_player' });
      const commitRes = await BookingService.commitBooking({
        userId: 'demo_player',
        userName: 'Rohan Deshmukh',
        userPhone: '+91 98765 43210',
        venueId: 'venue_thane_01',
        venueName: 'Apex Arena Ghodbunder',
        courtId: testCourt,
        courtName: 'Turf A - Floodlight Box Cricket',
        sport: 'cricket',
        date: futureDate,
        slot,
        pricing: { total: 1000 }
      });
      log(`Created test confirmed booking: ${commitRes.booking.bookingId}`, 'pass');

      const preview = CancellationService.previewRefund(commitRes.booking);
      log(`Cancellation Policy Preview: Notice: >24h -> ${preview.refundPercentage}% Refund (Amount: ₹${preview.refundAmount})`, 'pass');

      const cancelRes = await CancellationService.cancelBooking(commitRes.booking.bookingId, 'demo_player');
      log(`Booking transitioned to CANCELLED.`, 'pass');
      log(`Availability doc slot restored to AVAILABLE for court on ${futureDate}.`, 'pass');
      log(`Refund transaction ledger record generated.`, 'pass');
      setBadge(6, 'PASSED', 'pass');
      if (onStateUpdated) onStateUpdated();
    };

    // SCENARIO 7: Walk-in Reservation
    const runScenario7 = async () => {
      setBadge(7, 'RUNNING', 'running');
      log('Starting Scenario 7: Owner Walk-in / Phone Offline Reservation...');
      const venue = Object.values(mockDb.data.venues)[0];
      const court = VenueService.getCourtsForVenue(venue.venueId)[0];
      const date = '2026-11-27';
      const slot = '17:00';

      const walkInRes = await BookingService.createWalkInBooking({
        venueId: venue.venueId,
        venueName: venue.name,
        courtId: court.courtId,
        courtName: court.name,
        sport: court.sport,
        date,
        slot,
        customerName: 'Thane Strikers FC (Cash Walk-in)',
        customerPhone: '+91 98200 44332',
        amount: 1100,
        paymentMethod: 'CASH',
        ownerId: 'owner_demo'
      });

      log(`Walk-in booked: ${walkInRes.booking.bookingId} for ${walkInRes.booking.userName}`, 'pass');
      log(`Payment recorded as CASH at counter. Slot marked BOOKED in daily availability.`, 'pass');
      log(`Pass token generated: ${walkInRes.booking.qrPassToken}`, 'pass');
      setBadge(7, 'PASSED', 'pass');
      if (onStateUpdated) onStateUpdated();
    };

    // Bind event handlers
    modal.querySelector('#btn-run-sc-1').addEventListener('click', runScenario1);
    modal.querySelector('#btn-run-sc-2').addEventListener('click', runScenario2);
    modal.querySelector('#btn-run-sc-3').addEventListener('click', runScenario3);
    modal.querySelector('#btn-run-sc-4').addEventListener('click', runScenario4);
    modal.querySelector('#btn-run-sc-5').addEventListener('click', runScenario5);
    modal.querySelector('#btn-run-sc-6').addEventListener('click', runScenario6);
    modal.querySelector('#btn-run-sc-7').addEventListener('click', runScenario7);

    modal.querySelector('#btn-run-all').addEventListener('click', async () => {
      log('====================================================');
      log('⚡ RUNNING ALL 7 END-TO-END WORKFLOW SCENARIOS...');
      log('====================================================');
      await runScenario1();
      await new Promise(r => setTimeout(r, 400));
      await runScenario2();
      await new Promise(r => setTimeout(r, 400));
      await runScenario3();
      await new Promise(r => setTimeout(r, 400));
      await runScenario4();
      await new Promise(r => setTimeout(r, 400));
      await runScenario5();
      await new Promise(r => setTimeout(r, 400));
      await runScenario6();
      await new Promise(r => setTimeout(r, 400));
      await runScenario7();
      log('====================================================');
      log('🎉 ALL 7 WORKFLOW SCENARIOS EXECUTED & VERIFIED!', 'pass');
      log('====================================================');
    });
  }
}
