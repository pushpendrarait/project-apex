/**
 * Project Apex - Venue Partner Portal
 * Manages daily court schedules, walk-in reservations, and QR gate verification
 */

import { BookingService } from '../services/bookingService.js';
import { VenueService } from '../services/venueService.js';
import { AddVenueModal } from './AddVenueModal.js';
import { mockDb } from '../services/firebaseConfig.js';
import { UserService } from '../services/userService.js';

export class VenueOwnerPortal {
  constructor({ onBack }) {
    const currentUser = UserService.getCurrentUser();
    this.ownerId = currentUser ? currentUser.uid : 'owner_01';
    this.venueId = (currentUser && currentUser.venueId) ? currentUser.venueId : 'venue_thane_01';
    this.onBack = onBack;
  }

  render(container) {
    const currentUser = UserService.getCurrentUser();

    // If not signed in
    if (!currentUser) {
      container.innerHTML = `
        <div class="max-w-xl mx-auto py-12 px-4 text-center space-y-6 animate-fade-in">
          <div class="w-20 h-20 mx-auto rounded-3xl bg-indigo-500/10 border border-indigo-500/30 text-indigo-400 flex items-center justify-center text-4xl shadow-xl shadow-indigo-500/10">
            🏢
          </div>
          <div class="space-y-2">
            <h2 class="text-2xl font-black text-white">Partner Portal Access</h2>
            <p class="text-sm text-slate-400">
              Please sign in as a Facility Owner or register your turf venue to access gate check-in, match ledgers, and walk-in reservations.
            </p>
          </div>
          <div class="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
            <button id="btn-partner-login" class="w-full sm:w-auto px-6 py-3 rounded-2xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-sm shadow-xl shadow-indigo-600/20 transition active:scale-95">
              🏢 Partner Sign In / Register
            </button>
            <button id="btn-partner-explore" class="w-full sm:w-auto px-6 py-3 rounded-2xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-sm transition">
              ← Switch to Player View
            </button>
          </div>
        </div>
      `;

      container.querySelector('#btn-partner-login').addEventListener('click', () => {
        import('./UserAuthModal.js').then(({ UserAuthModal }) => {
          UserAuthModal.show({
            initialTab: 'owner',
            onUserChanged: () => {
              this.render(container);
            }
          });
        });
      });

      container.querySelector('#btn-partner-explore').addEventListener('click', () => {
        if (this.onBack) this.onBack();
      });

      return;
    }

    // If signed in as player
    if (currentUser.role !== 'venue_owner') {
      container.innerHTML = `
        <div class="max-w-xl mx-auto py-12 px-4 text-center space-y-6 animate-fade-in">
          <div class="w-20 h-20 mx-auto rounded-3xl bg-indigo-500/10 border border-indigo-500/30 text-indigo-400 flex items-center justify-center text-4xl shadow-xl shadow-indigo-500/10">
            🏢
          </div>
          <div class="space-y-2">
            <h2 class="text-2xl font-black text-white">Partner Access Required</h2>
            <p class="text-sm text-slate-400">
              You are currently signed in as a Player (<strong class="text-emerald-400">${currentUser.displayName}</strong>). To manage turf schedules and scan gate passes, please register as a Facility Owner.
            </p>
          </div>
          <div class="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
            <button id="btn-partner-register" class="w-full sm:w-auto px-6 py-3 rounded-2xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-sm shadow-xl shadow-indigo-600/20 transition active:scale-95">
              + Register as Facility Owner
            </button>
            <button id="btn-partner-explore" class="w-full sm:w-auto px-6 py-3 rounded-2xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-sm transition">
              ← Back to Player View
            </button>
          </div>
        </div>
      `;

      container.querySelector('#btn-partner-register').addEventListener('click', () => {
        import('./UserAuthModal.js').then(({ UserAuthModal }) => {
          UserAuthModal.show({
            initialTab: 'owner',
            onUserChanged: () => {
              this.render(container);
            }
          });
        });
      });

      container.querySelector('#btn-partner-explore').addEventListener('click', () => {
        if (this.onBack) this.onBack();
      });

      return;
    }

    this.ownerId = currentUser.uid;
    this.venueId = currentUser.venueId || this.venueId;
    const venue = VenueService.getVenueById(this.venueId) || Object.values(mockDb.data.venues)[0];
    this.venueId = venue.venueId;
    const courts = VenueService.getCourtsForVenue(this.venueId);
    const bookings = BookingService.getVenueBookings(this.venueId);

    const totalRevenue = bookings.reduce((sum, b) => (b.status === 'CONFIRMED' || b.status === 'CHECKED_IN') ? sum + (b.pricing?.total || 0) : sum, 0);
    const activeBookingsCount = bookings.filter(b => b.status === 'CONFIRMED' || b.status === 'CHECKED_IN').length;

    container.innerHTML = `
      <div class="space-y-6 animate-fade-in">
        <!-- Header -->
        <div class="bg-gradient-to-r from-slate-900 via-slate-900 to-indigo-950 p-6 rounded-3xl border border-slate-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <div class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/30 text-xs font-bold uppercase tracking-wider mb-2">
              🏢 Partner Portal & Gate Management
            </div>
            <h2 class="text-2xl font-black text-white">${venue.name}</h2>
            <p class="text-xs text-slate-400">${venue.area}, ${venue.city} • Managed by ${venue.ownerId === this.ownerId ? 'You' : 'Partner'}</p>
          </div>

          <div class="flex flex-wrap items-center gap-3">
            <button id="btn-owner-walkin" class="px-4 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-black text-xs shadow-lg shadow-amber-500/20 transition flex items-center gap-1.5">
              <span>⚡ + Walk-In Reservation</span>
            </button>
            <button id="btn-owner-add-venue" class="px-4 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-xs shadow-lg shadow-emerald-500/20 transition flex items-center gap-1.5">
              <span>+ Add New Facility</span>
            </button>
            <button id="btn-back-owner" class="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs transition">
              ← Switch to Player View
            </button>
          </div>
        </div>

        <!-- Metrics Grid -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div class="bg-slate-900 border border-slate-800 p-5 rounded-2xl">
            <span class="text-xs font-bold uppercase text-slate-400 block">Total Revenue</span>
            <span class="text-2xl font-black text-emerald-400 font-mono mt-1 block">₹${totalRevenue.toLocaleString('en-IN')}</span>
            <span class="text-[11px] text-slate-500">Gross Online + Walk-in Cash</span>
          </div>

          <div class="bg-slate-900 border border-slate-800 p-5 rounded-2xl">
            <span class="text-xs font-bold uppercase text-slate-400 block">Active Bookings</span>
            <span class="text-2xl font-black text-white font-mono mt-1 block">${activeBookingsCount}</span>
            <span class="text-[11px] text-emerald-400">0 Double-Booking Errors</span>
          </div>

          <div class="bg-slate-900 border border-slate-800 p-5 rounded-2xl">
            <span class="text-xs font-bold uppercase text-slate-400 block">Active Courts / Pitches</span>
            <span class="text-2xl font-black text-amber-400 font-mono mt-1 block">${courts.length}</span>
            <span class="text-[11px] text-slate-500">All Floodlights Active</span>
          </div>
        </div>

        <!-- Courts & Sports Facilities Inventory -->
        <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-xl">
          <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <div>
              <h3 class="text-base font-black text-white flex items-center gap-2">
                <span>🏟️ Active Turf & Court Facilities (${courts.length})</span>
              </h3>
              <p class="text-xs text-slate-400 mt-0.5">Specifications, capacity, surface details and hourly rates for ${venue.name}</p>
            </div>
            <button id="btn-owner-add-court" class="px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-400 hover:from-emerald-400 hover:to-teal-300 text-slate-950 font-black text-xs shadow-md shadow-emerald-500/20 transition flex items-center gap-1.5 shrink-0">
              <span>+ Add Court / Facility</span>
            </button>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            ${courts.map(c => `
              <div class="bg-slate-950/80 border border-slate-800/80 rounded-xl p-4 space-y-2">
                <div class="flex items-center justify-between">
                  <span class="font-bold text-white text-xs">${c.name}</span>
                  <span class="text-[9px] uppercase font-mono px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">${c.sport}</span>
                </div>
                <div class="grid grid-cols-2 gap-2 text-[11px] text-slate-400">
                  <div>Capacity: <strong class="text-slate-200">${c.capacity || 16} Players</strong></div>
                  <div>Surface: <strong class="text-slate-200">${c.surface || 'Turf'}</strong></div>
                  <div>Dimensions: <strong class="text-slate-200">${c.dimensions || 'Standard'}</strong></div>
                  <div>Type: <strong class="text-slate-200">${c.isIndoor ? 'Indoor AC' : 'Outdoor Floodlit'}</strong></div>
                </div>
                <div class="pt-2 border-t border-slate-800 flex items-center justify-between text-xs font-mono">
                  <span class="text-emerald-400 font-bold">Base: ₹${c.baseHourlyRate}/hr</span>
                  <span class="text-amber-400 font-bold">Peak: ₹${c.peakHourlyRate}/hr</span>
                </div>
              </div>
            `).join('')}
          </div>
        </div>

        <!-- Gate Check-In Pass Verification Simulator -->
        <div class="bg-slate-900 border border-indigo-500/30 rounded-2xl p-5 space-y-4 shadow-xl">
          <div class="flex items-center justify-between">
            <h3 class="text-base font-black text-white flex items-center gap-2">
              <span>🛡️ Gate QR Code Check-In Scanner</span>
            </h3>
            <span class="text-[11px] text-indigo-400 font-mono">Real-Time Access Control</span>
          </div>
          <div class="flex flex-col sm:flex-row gap-3">
            <input id="input-gate-token" type="text" placeholder="Enter or scan player QR token (e.g. APEX-TH-98210-...)" class="flex-1 bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-xs text-white font-mono focus:ring-2 focus:ring-indigo-500 focus:outline-none" />
            <button id="btn-verify-token" class="px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs transition shadow-md">
              Verify QR Token
            </button>
          </div>
          <div id="token-verify-result" class="text-xs hidden p-4 rounded-xl"></div>
        </div>

        <!-- Confirmed Reservations Ledger -->
        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-black text-white">Facility Reservations & Match Ledger</h3>
            <span class="text-xs text-slate-400 font-mono">${bookings.length} total entries</span>
          </div>
          <div class="space-y-2">
            ${bookings.length === 0 ? `
              <div class="text-center py-8 text-slate-500 text-xs bg-slate-900/40 rounded-2xl border border-slate-800">
                No active bookings recorded yet today for this facility.
              </div>
            ` : bookings.map(b => {
              const isCheckedIn = b.status === 'CHECKED_IN';
              const isCancelled = b.status === 'CANCELLED';
              const isWalkIn = b.source === 'walk_in' || b.payment?.method === 'CASH';

              let statusBadge = `<span class="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">CONFIRMED</span>`;
              if (isCheckedIn) {
                statusBadge = `<span class="px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-500/10 text-indigo-400 border border-indigo-500/30">CHECKED IN</span>`;
              } else if (isCancelled) {
                statusBadge = `<span class="px-2 py-0.5 rounded text-[10px] font-bold bg-red-500/10 text-red-400 border border-red-500/30">CANCELLED</span>`;
              }

              const sourceBadge = isWalkIn
                ? `<span class="px-1.5 py-0.2 rounded text-[9px] font-mono bg-amber-500/10 text-amber-400 border border-amber-500/30">WALK-IN (CASH)</span>`
                : `<span class="px-1.5 py-0.2 rounded text-[9px] font-mono bg-blue-500/10 text-blue-400 border border-blue-500/30">ONLINE</span>`;

              return `
                <div class="bg-slate-900 border border-slate-800 hover:border-slate-700 transition p-4 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
                  <div class="space-y-1">
                    <div class="flex items-center gap-2">
                      <span class="font-bold text-white text-sm">${b.userName}</span>
                      <span class="text-slate-400 font-mono">(${b.userPhone})</span>
                      ${sourceBadge}
                      ${statusBadge}
                    </div>
                    <div class="text-slate-400">
                      ${b.courtName} • <span class="text-emerald-400 font-mono font-bold">${b.date} @ ${b.slot}</span>
                      ${isCheckedIn ? `<span class="text-indigo-400 text-[11px] ml-2 font-mono">Checked in at ${new Date(b.checkedInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>` : ''}
                    </div>
                  </div>
                  <div class="flex items-center justify-between sm:justify-end gap-4">
                    <div class="text-right">
                      <div class="font-mono font-bold text-white text-sm">₹${b.pricing?.total || 0}</div>
                      <div class="text-[10px] text-slate-400 font-mono">${b.qrPassToken}</div>
                    </div>
                    ${(!isCheckedIn && !isCancelled) ? `
                      <button data-token="${b.qrPassToken}" class="btn-quick-checkin px-3 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs transition shadow-sm">
                        Check In
                      </button>
                    ` : ''}
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        </div>
      </div>

      <!-- Walk-In Reservation Modal Container -->
      <div id="walkin-modal-container"></div>
    `;

    this.bindEvents(container);
  }

  bindEvents(container) {
    container.querySelector('#btn-back-owner').addEventListener('click', () => {
      if (this.onBack) this.onBack();
    });

    const btnAddVenue = container.querySelector('#btn-owner-add-venue');
    if (btnAddVenue) {
      btnAddVenue.addEventListener('click', () => {
        AddVenueModal.show({
          onVenueAdded: (newVenue) => {
            this.venueId = newVenue.venueId;
            this.render(container);
          }
        });
      });
    }

    const btnAddCourt = container.querySelector('#btn-owner-add-court');
    if (btnAddCourt) {
      btnAddCourt.addEventListener('click', () => {
        this.showAddCourtModal(container);
      });
    }

    const btnWalkIn = container.querySelector('#btn-owner-walkin');
    if (btnWalkIn) {
      btnWalkIn.addEventListener('click', () => {
        this.showWalkInModal(container);
      });
    }

    const btnVerify = container.querySelector('#btn-verify-token');
    const inputToken = container.querySelector('#input-gate-token');
    const resultBox = container.querySelector('#token-verify-result');

    btnVerify.addEventListener('click', () => {
      const token = inputToken.value.trim().toUpperCase();
      if (!token) {
        inputToken.focus();
        return;
      }
      this.handleGateCheckIn(token, resultBox, container);
    });

    // Quick check-in buttons from the ledger list
    container.querySelectorAll('.btn-quick-checkin').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const token = e.currentTarget.getAttribute('data-token');
        if (token) {
          inputToken.value = token;
          this.handleGateCheckIn(token, resultBox, container);
        }
      });
    });
  }

  handleGateCheckIn(token, resultBox, container) {
    try {
      const checkInRes = BookingService.checkInPlayer(token, this.ownerId);
      const b = checkInRes.booking;

      resultBox.classList.remove('hidden');
      if (checkInRes.alreadyCheckedIn) {
        resultBox.className = 'text-xs p-4 rounded-xl bg-amber-950/80 border border-amber-500/50 text-amber-300 space-y-1';
        resultBox.innerHTML = `
          <div class="font-bold text-sm">⚠️ Player Already Checked In</div>
          <div>Player: <strong>${b.userName}</strong> (${b.userPhone})</div>
          <div>Slot: <strong>${b.date} @ ${b.slot}</strong> on ${b.courtName}</div>
          <div class="text-[11px] font-mono text-amber-400">${checkInRes.message}</div>
        `;
      } else {
        resultBox.className = 'text-xs p-4 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 space-y-1';
        resultBox.innerHTML = `
          <div class="font-bold text-sm">✓ Gate Pass Verified & Attendance Marked</div>
          <div>Player: <strong>${b.userName}</strong> (${b.userPhone})</div>
          <div>Slot: <strong>${b.date} @ ${b.slot}</strong> on ${b.courtName}</div>
          <div class="text-[11px] font-mono text-emerald-400">Status: CHECKED_IN • Token: ${b.qrPassToken}</div>
        `;
        // Refresh schedule after brief pause to show updated state
        setTimeout(() => this.render(container), 1200);
      }
    } catch (err) {
      resultBox.classList.remove('hidden');
      resultBox.className = 'text-xs p-4 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300';
      resultBox.innerHTML = `❌ Verification Failed: ${err.message}`;
    }
  }

  showWalkInModal(container) {
    const modalContainer = container.querySelector('#walkin-modal-container');
    const venue = VenueService.getVenueById(this.venueId);
    const courts = VenueService.getCourtsForVenue(this.venueId);
    const today = new Date().toISOString().split('T')[0];

    const slots = [
      "06:00", "07:00", "08:00", "09:00", "10:00", "11:00",
      "12:00", "13:00", "14:00", "15:00", "16:00", "17:00",
      "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"
    ];

    modalContainer.innerHTML = `
      <div id="walkin-backdrop" class="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
        <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-lg w-full p-6 shadow-2xl space-y-5 animate-fade-in">
          
          <div class="flex items-center justify-between pb-3 border-b border-slate-800">
            <div>
              <div class="text-[10px] uppercase font-bold text-amber-400 font-mono tracking-wider">Instant On-Turf Entry</div>
              <h3 class="text-xl font-black text-white">Create Walk-In Reservation</h3>
            </div>
            <button id="btn-close-walkin" class="w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white flex items-center justify-center text-sm">✕</button>
          </div>

          <form id="form-walkin" class="space-y-4 text-xs">
            <div>
              <label class="block text-slate-400 font-bold mb-1">Select Court / Pitch</label>
              <select id="walkin-court" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2.5 text-white focus:ring-2 focus:ring-amber-500 focus:outline-none">
                ${courts.map(c => `<option value="${c.courtId}" data-name="${c.name}" data-sport="${c.sport}">${c.name} (${c.sport}) - ₹${c.baseHourlyRate}/hr</option>`).join('')}
              </select>
            </div>

            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-slate-400 font-bold mb-1">Match Date</label>
                <input id="walkin-date" type="date" value="${today}" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2.5 text-white focus:ring-2 focus:ring-amber-500 focus:outline-none" />
              </div>
              <div>
                <label class="block text-slate-400 font-bold mb-1">Time Slot</label>
                <select id="walkin-slot" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2.5 text-white focus:ring-2 focus:ring-amber-500 focus:outline-none">
                  ${slots.map(s => `<option value="${s}">${s} (60 mins)</option>`).join('')}
                </select>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-slate-400 font-bold mb-1">Customer Name</label>
                <input id="walkin-name" type="text" placeholder="e.g. Rahul Sharma" required class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2.5 text-white focus:ring-2 focus:ring-amber-500 focus:outline-none" />
              </div>
              <div>
                <label class="block text-slate-400 font-bold mb-1">Customer Mobile</label>
                <input id="walkin-phone" type="tel" placeholder="+91 98200 11223" required class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2.5 text-white focus:ring-2 focus:ring-amber-500 focus:outline-none" />
              </div>
            </div>

            <div>
              <label class="block text-slate-400 font-bold mb-1">Cash / Offline Amount Collected (₹)</label>
              <input id="walkin-amount" type="number" value="${venue.startingPrice || 1000}" required min="0" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2.5 text-white font-mono font-bold focus:ring-2 focus:ring-amber-500 focus:outline-none" />
            </div>

            <div id="walkin-error" class="hidden text-red-400 bg-red-950/60 p-3 rounded-xl border border-red-500/30"></div>

            <div class="pt-2">
              <button type="submit" id="btn-submit-walkin" class="w-full py-3 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-black text-sm shadow-lg shadow-amber-500/20 transition">
                ⚡ Commit Walk-In Booking & Issue Pass
              </button>
            </div>
          </form>
        </div>
      </div>
    `;

    const closeBtn = modalContainer.querySelector('#btn-close-walkin');
    const backdrop = modalContainer.querySelector('#walkin-backdrop');
    const form = modalContainer.querySelector('#form-walkin');
    const errBox = modalContainer.querySelector('#walkin-error');

    const closeModal = () => {
      modalContainer.innerHTML = '';
    };

    closeBtn.addEventListener('click', closeModal);
    backdrop.addEventListener('click', (e) => {
      if (e.target === backdrop) closeModal();
    });

    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      errBox.classList.add('hidden');

      const courtSelect = modalContainer.querySelector('#walkin-court');
      const selectedOption = courtSelect.options[courtSelect.selectedIndex];
      const courtId = courtSelect.value;
      const courtName = selectedOption.getAttribute('data-name');
      const sport = selectedOption.getAttribute('data-sport');
      const date = modalContainer.querySelector('#walkin-date').value;
      const slot = modalContainer.querySelector('#walkin-slot').value;
      const customerName = modalContainer.querySelector('#walkin-name').value.trim();
      const customerPhone = modalContainer.querySelector('#walkin-phone').value.trim();
      const amount = Number(modalContainer.querySelector('#walkin-amount').value) || 0;

      try {
        const res = await BookingService.createWalkInBooking({
          venueId: this.venueId,
          venueName: venue.name,
          courtId,
          courtName,
          sport,
          date,
          slot,
          customerName,
          customerPhone,
          amountCollected: amount,
          ownerId: this.ownerId
        });

        closeModal();
        this.render(container);
      } catch (err) {
        errBox.textContent = err.message;
        errBox.classList.remove('hidden');
      }
    });
  }

  showAddCourtModal(container) {
    const modalContainer = container.querySelector('#walkin-modal-container');
    modalContainer.innerHTML = `
      <div id="add-court-backdrop" class="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
        <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-lg w-full p-6 shadow-2xl space-y-4 animate-fade-in text-xs text-white">
          <div class="flex items-center justify-between pb-3 border-b border-slate-800">
            <div>
              <div class="text-[10px] uppercase font-bold text-emerald-400 font-mono tracking-wider">Facility Provisioning</div>
              <h3 class="text-xl font-black text-white">Add New Court / Pitch</h3>
            </div>
            <button id="btn-close-court-modal" class="w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white flex items-center justify-center text-sm">✕</button>
          </div>

          <form id="form-add-court" class="space-y-3.5">
            <div>
              <label class="block text-slate-400 font-bold mb-1">Facility / Court Name *</label>
              <input id="input-court-name" type="text" required placeholder="e.g. Main 8v8 Box Cricket Turf" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
            </div>

            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-slate-400 font-bold mb-1">Sport Category *</label>
                <select id="input-court-sport" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none">
                  <option value="cricket">🏏 Box Cricket</option>
                  <option value="football">⚽ Football Turf</option>
                  <option value="badminton">🏸 Badminton</option>
                  <option value="pickleball">🏓 Pickleball</option>
                  <option value="pool_table">🎱 Pool & Snooker</option>
                  <option value="tennis">🎾 Lawn Tennis</option>
                  <option value="squash">🎾 Squash</option>
                  <option value="multi_sport">🏟️ Multi-Sport</option>
                </select>
              </div>

              <div>
                <label class="block text-slate-400 font-bold mb-1">Max Capacity (Players) *</label>
                <input id="input-court-capacity" type="number" required min="2" max="50" value="16" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
              </div>
            </div>

            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-slate-400 font-bold mb-1">Surface Type *</label>
                <select id="input-court-surface" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none">
                  <option value="50mm Synthetic AstroTurf">50mm Synthetic AstroTurf</option>
                  <option value="BWF Approved Synthetic Mat">BWF Approved Synthetic Mat</option>
                  <option value="Cushioned Acrylic Hardcourt">Cushioned Acrylic Hardcourt</option>
                  <option value="ASB Beechwood Flooring">ASB Beechwood Flooring</option>
                  <option value="Natural Red Clay">Natural Red Clay</option>
                  <option value="English Snooker Felt">English Snooker Felt</option>
                </select>
              </div>

              <div>
                <label class="block text-slate-400 font-bold mb-1">Environment *</label>
                <select id="input-court-indoor" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none">
                  <option value="false">Outdoor Floodlit</option>
                  <option value="true">Indoor Air-Conditioned</option>
                </select>
              </div>
            </div>

            <div class="grid grid-cols-3 gap-2">
              <div>
                <label class="block text-slate-400 font-bold mb-1">Base (₹/hr) *</label>
                <input id="input-court-base" type="number" required min="300" step="50" value="1200" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
              </div>
              <div>
                <label class="block text-slate-400 font-bold mb-1">Peak (₹/hr) *</label>
                <input id="input-court-peak" type="number" required min="300" step="50" value="1600" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
              </div>
              <div>
                <label class="block text-slate-400 font-bold mb-1">Off-Peak %</label>
                <input id="input-court-discount" type="number" min="0" max="60" value="30" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
              </div>
            </div>

            <div class="pt-3 border-t border-slate-800 flex justify-end gap-2">
              <button type="button" id="btn-cancel-court-modal" class="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold transition">Cancel</button>
              <button type="submit" class="px-5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black transition shadow-md shadow-emerald-500/20">+ Add Facility</button>
            </div>
          </form>
        </div>
      </div>
    `;

    const close = () => { modalContainer.innerHTML = ''; };
    modalContainer.querySelector('#btn-close-court-modal').addEventListener('click', close);
    modalContainer.querySelector('#btn-cancel-court-modal').addEventListener('click', close);

    modalContainer.querySelector('#form-add-court').addEventListener('submit', (e) => {
      e.preventDefault();
      const name = modalContainer.querySelector('#input-court-name').value.trim();
      const sport = modalContainer.querySelector('#input-court-sport').value;
      const capacity = modalContainer.querySelector('#input-court-capacity').value;
      const surface = modalContainer.querySelector('#input-court-surface').value;
      const isIndoor = modalContainer.querySelector('#input-court-indoor').value === 'true';
      const baseHourlyRate = modalContainer.querySelector('#input-court-base').value;
      const peakHourlyRate = modalContainer.querySelector('#input-court-peak').value;
      const oddHourDiscountPct = modalContainer.querySelector('#input-court-discount').value;

      VenueService.addCourt({
        venueId: this.venueId,
        name,
        sport,
        capacity: Number(capacity),
        surface,
        isIndoor,
        baseHourlyRate: Number(baseHourlyRate),
        peakHourlyRate: Number(peakHourlyRate),
        oddHourDiscountPct: Number(oddHourDiscountPct)
      });

      close();
      this.render(container);
    });
  }
}
