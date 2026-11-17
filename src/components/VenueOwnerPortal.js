/**
 * Project Apex - Venue Partner Portal
 * Manages daily court schedules, walk-in reservations, and QR gate verification
 */

import { BookingService } from '../services/bookingService.js';
import { VenueService } from '../services/venueService.js';
import { mockDb } from '../services/firebaseConfig.js';

export class VenueOwnerPortal {
  constructor({ onBack }) {
    this.venueId = 'venue_thane_01';
    this.onBack = onBack;
  }

  render(container) {
    const venue = VenueService.getVenueById(this.venueId);
    const courts = VenueService.getCourtsForVenue(this.venueId);
    const bookings = BookingService.getVenueBookings(this.venueId);

    const totalRevenue = bookings.reduce((sum, b) => b.status === 'CONFIRMED' ? sum + b.pricing.total : sum, 0);

    container.innerHTML = `
      <div class="space-y-6">
        <!-- Header -->
        <div class="bg-gradient-to-r from-slate-900 via-slate-900 to-indigo-950 p-6 rounded-3xl border border-slate-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <div class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/30 text-xs font-bold uppercase tracking-wider mb-2">
              🏢 Partner Portal
            </div>
            <h2 class="text-2xl font-black text-white">${venue.name}</h2>
            <p class="text-xs text-slate-400">${venue.area}, ${venue.city} • Managed by Santosh Kadam</p>
          </div>

          <div class="flex items-center gap-3">
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
            <span class="text-[11px] text-slate-500">24h Net Settled</span>
          </div>

          <div class="bg-slate-900 border border-slate-800 p-5 rounded-2xl">
            <span class="text-xs font-bold uppercase text-slate-400 block">Active Bookings</span>
            <span class="text-2xl font-black text-white font-mono mt-1 block">${bookings.filter(b => b.status === 'CONFIRMED').length}</span>
            <span class="text-[11px] text-emerald-400">0 Double-Booking Errors</span>
          </div>

          <div class="bg-slate-900 border border-slate-800 p-5 rounded-2xl">
            <span class="text-xs font-bold uppercase text-slate-400 block">Active Courts / Pitches</span>
            <span class="text-2xl font-black text-amber-400 font-mono mt-1 block">${courts.length}</span>
            <span class="text-[11px] text-slate-500">All Floodlights Active</span>
          </div>
        </div>

        <!-- Gate Check-In Pass Verification Simulator -->
        <div class="bg-slate-900 border border-indigo-500/30 rounded-2xl p-5 space-y-4">
          <h3 class="text-base font-black text-white flex items-center gap-2">
            <span>🛡️ Gate QR Code Check-In Scanner</span>
          </h3>
          <div class="flex flex-col sm:flex-row gap-3">
            <input id="input-gate-token" type="text" placeholder="Enter or scan player QR token (e.g. APEX-TH-...)" class="flex-1 bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-xs text-white font-mono focus:ring-2 focus:ring-indigo-500 focus:outline-none" />
            <button id="btn-verify-token" class="px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs transition">
              Verify & Check In
            </button>
          </div>
          <div id="token-verify-result" class="text-xs hidden p-3 rounded-xl"></div>
        </div>

        <!-- Today's Schedule -->
        <div class="space-y-3">
          <h3 class="text-lg font-black text-white">Confirmed Match Reservations</h3>
          <div class="space-y-2">
            ${bookings.length === 0 ? `
              <div class="text-center py-8 text-slate-500 text-xs bg-slate-900/40 rounded-2xl border border-slate-800">
                No active bookings recorded yet today.
              </div>
            ` : bookings.map(b => `
              <div class="bg-slate-900 border border-slate-800 p-4 rounded-xl flex items-center justify-between text-xs">
                <div>
                  <div class="font-bold text-white">${b.userName} (${b.userPhone})</div>
                  <div class="text-slate-400">${b.courtName} • <span class="text-emerald-400 font-mono">${b.date} @ ${b.slot}</span></div>
                </div>
                <div class="text-right">
                  <div class="font-mono font-bold text-white">₹${b.pricing.total}</div>
                  <div class="text-[10px] text-emerald-400 font-mono">${b.qrPassToken}</div>
                </div>
              </div>
            `).join('')}
          </div>
        </div>
      </div>
    `;

    this.bindEvents(container);
  }

  bindEvents(container) {
    container.querySelector('#btn-back-owner').addEventListener('click', () => {
      if (this.onBack) this.onBack();
    });

    const btnVerify = container.querySelector('#btn-verify-token');
    const inputToken = container.querySelector('#input-gate-token');
    const resultBox = container.querySelector('#token-verify-result');

    btnVerify.addEventListener('click', () => {
      const token = inputToken.value.trim().toUpperCase();
      if (!token) return;

      const matchingBooking = Object.values(mockDb.data.bookings).find(b => b.qrPassToken === token);

      resultBox.classList.remove('hidden');
      if (matchingBooking) {
        resultBox.className = 'text-xs p-4 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 space-y-1';
        resultBox.innerHTML = `
          <div class="font-bold text-sm">✓ Valid Gate Pass: Access Approved</div>
          <div>Player: <strong>${matchingBooking.userName}</strong> (${matchingBooking.userPhone})</div>
          <div>Slot: <strong>${matchingBooking.date} @ ${matchingBooking.slot}</strong> on ${matchingBooking.courtName}</div>
          <div class="text-[10px] text-emerald-400 font-mono">Payment ID: ${matchingBooking.payment.paymentId}</div>
        `;
      } else {
        resultBox.className = 'text-xs p-4 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300';
        resultBox.innerHTML = `❌ Invalid or Unrecognized Pass Token. Please check reservation details with player.`;
      }
    });
  }
}
