/**
 * Project Apex - Booking Confirmation & QR Entry Pass
 */

import { OfflineStorage } from '../services/offlineStorage.js';

export class BookingConfirmation {
  constructor({ booking, onViewBookings, onHome }) {
    this.booking = booking;
    this.onViewBookings = onViewBookings;
    this.onHome = onHome;

    // Cache pass for offline gate entry resilience
    OfflineStorage.savePass(booking);
  }

  render(container) {
    container.innerHTML = `
      <div class="max-w-xl mx-auto space-y-6 animate-fade-in">
        <!-- Success Banner -->
        <div class="bg-gradient-to-b from-emerald-950/80 to-slate-900 border border-emerald-500/40 rounded-3xl p-6 md:p-8 text-center relative overflow-hidden shadow-2xl">
          <div class="w-16 h-16 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 flex items-center justify-center text-3xl mx-auto mb-4 animate-bounce">
            ✓
          </div>
          <span class="text-xs font-bold uppercase tracking-wider text-emerald-400">Transaction Confirmed</span>
          <h2 class="text-2xl md:text-3xl font-black text-white mt-1">You're Ready to Play!</h2>
          <p class="text-xs text-slate-400 mt-2">Booking ID: <span class="font-mono text-emerald-400 font-bold">${this.booking.bookingId}</span></p>

          <!-- Gate QR Code Pass Card -->
          <div class="mt-6 bg-white p-6 rounded-2xl max-w-xs mx-auto text-slate-950 shadow-xl space-y-3">
            <div class="text-[11px] font-bold tracking-widest uppercase text-slate-500">Official Entry Pass</div>
            
            <!-- Simulated QR Pattern -->
            <div class="w-44 h-44 mx-auto bg-slate-950 p-2.5 rounded-xl flex items-center justify-center">
              <div class="w-full h-full border-4 border-dashed border-emerald-400 rounded-lg p-2 flex flex-col items-center justify-center text-emerald-400 text-center">
                <div class="text-2xl font-mono font-black mb-1">APEX</div>
                <div class="text-[9px] font-mono tracking-tighter text-slate-300 break-all px-1">${this.booking.qrPassToken}</div>
                <div class="text-[8px] uppercase tracking-widest text-emerald-300 mt-2">Scan at Gate</div>
              </div>
            </div>

            <div class="font-mono text-xs font-black tracking-wider text-slate-800">${this.booking.qrPassToken}</div>
            <div class="text-[10px] text-slate-500">Show this pass to security / turf manager at gate</div>
          </div>

          <div class="mt-4 flex items-center justify-center gap-2 text-xs text-emerald-400">
            <span class="w-2 h-2 rounded-full bg-emerald-400"></span>
            Pass saved to Offline Vault (accessible without internet)
          </div>
        </div>

        <!-- Details Card -->
        <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3 text-xs text-slate-300">
          <div class="flex justify-between border-b border-slate-800 pb-2">
            <span class="text-slate-400">Venue:</span>
            <span class="font-bold text-white text-right">${this.booking.venueName}</span>
          </div>
          <div class="flex justify-between border-b border-slate-800 pb-2">
            <span class="text-slate-400">Court / Pitch:</span>
            <span class="font-bold text-white">${this.booking.courtName}</span>
          </div>
          <div class="flex justify-between border-b border-slate-800 pb-2">
            <span class="text-slate-400">Time Slot:</span>
            <span class="font-bold text-emerald-400 font-mono">${this.booking.date} @ ${this.booking.slot}</span>
          </div>
          <div class="flex justify-between border-b border-slate-800 pb-2">
            <span class="text-slate-400">Payment Reference:</span>
            <span class="font-mono text-white">${this.booking.payment.paymentId} (${this.booking.payment.method})</span>
          </div>
          <div class="flex justify-between pt-1 text-sm font-bold text-white">
            <span>Total Paid:</span>
            <span class="text-emerald-400 font-mono">₹${this.booking.pricing.total}</span>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex flex-col sm:flex-row gap-3">
          <button id="btn-view-my-bookings" class="flex-1 py-3.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs transition">
            View My Bookings & Passes
          </button>
          <button id="btn-back-home" class="flex-1 py-3.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-xs shadow-lg shadow-emerald-500/20 transition">
            Book Another Slot →
          </button>
        </div>
      </div>
    `;

    container.querySelector('#btn-view-my-bookings').addEventListener('click', () => {
      if (this.onViewBookings) this.onViewBookings();
    });

    container.querySelector('#btn-back-home').addEventListener('click', () => {
      if (this.onHome) this.onHome();
    });
  }
}
