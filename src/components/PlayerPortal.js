/**
 * Project Apex - Player Portal & Cancellation Manager
 */

import { BookingService } from '../services/bookingService.js';
import { CancellationService } from '../services/cancellationService.js';
import { UserService } from '../services/userService.js';
import { UserAuthModal } from './UserAuthModal.js';
import { mockDb } from '../services/firebaseConfig.js';

export class PlayerPortal {
  constructor({ onBack, onShowPass, onUserChanged }) {
    this.onBack = onBack;
    this.onShowPass = onShowPass;
    this.onUserChanged = onUserChanged;
  }

  render(container) {
    const user = UserService.getCurrentUser();
    this.userId = user.uid;
    const bookings = BookingService.getPlayerBookings(user.uid);
    const initials = user.displayName.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();

    container.innerHTML = `
      <div class="space-y-6">
        <!-- Profile Header -->
        <div class="bg-gradient-to-r from-slate-900 via-slate-900 to-emerald-950 p-6 rounded-3xl border border-slate-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-2xl bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 flex items-center justify-center font-black text-xl">
              ${initials}
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h2 class="text-xl font-black text-white">${user.displayName}</h2>
                <span class="text-[10px] uppercase px-2 py-0.5 rounded font-mono font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                  ${user.role}
                </span>
              </div>
              <p class="text-xs text-slate-400">${user.phone} • ${user.city || 'Thane'}, Maharashtra</p>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-3">
            <div class="bg-slate-950/80 border border-slate-800 px-4 py-2 rounded-2xl">
              <span class="text-[10px] uppercase font-bold text-slate-400 block">Apex Loyalty Points</span>
              <span class="text-base font-black text-emerald-400 font-mono">${user.loyaltyPoints || 0} Pts</span>
            </div>
            <div class="bg-slate-950/80 border border-slate-800 px-4 py-2 rounded-2xl">
              <span class="text-[10px] uppercase font-bold text-slate-400 block">Referral Code</span>
              <span class="text-base font-black text-amber-400 font-mono">${user.referralCode || 'APEX50'}</span>
            </div>
            <button id="btn-switch-account" class="px-4 py-2.5 rounded-2xl bg-slate-800 hover:bg-slate-700 text-xs font-bold text-white border border-slate-700 transition flex items-center gap-1.5">
              <span>👤 Switch / Create User</span>
            </button>
          </div>
        </div>
              <span class="text-[10px] uppercase font-bold text-slate-400 block">Referral Code</span>
              <span class="text-base font-black text-amber-400 font-mono">${user.referralCode}</span>
            </div>
          </div>
        </div>

        <!-- Bookings Section -->
        <div>
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-black text-white">My Game Bookings (${bookings.length})</h3>
            <button id="btn-back-to-explore" class="text-xs text-emerald-400 hover:text-emerald-300 font-bold">
              ← Book New Turf
            </button>
          </div>

          ${bookings.length === 0 ? `
            <div class="text-center py-12 bg-slate-900/40 rounded-3xl border border-slate-800 text-slate-400 text-sm">
              You haven't made any turf reservations yet.
            </div>
          ` : `
            <div class="space-y-3">
              ${bookings.map(b => {
                const isCancelled = b.status === 'CANCELLED';
                const refundPreview = !isCancelled ? CancellationService.previewRefund(b) : null;

                return `
                  <div class="bg-slate-900 border ${isCancelled ? 'border-red-950/60 opacity-60' : 'border-slate-800 hover:border-emerald-500/30'} p-5 rounded-2xl flex flex-col md:flex-row md:items-center justify-between gap-4 transition">
                    <div class="space-y-1">
                      <div class="flex items-center gap-2">
                        <span class="font-bold text-white text-base">${b.venueName}</span>
                        <span class="px-2 py-0.5 rounded text-[10px] font-bold uppercase ${isCancelled ? 'bg-red-950 text-red-400 border border-red-800' : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'}">
                          ${b.status}
                        </span>
                      </div>
                      <p class="text-xs text-slate-400">${b.courtName} • <span class="text-emerald-400 font-mono font-bold">${b.date} @ ${b.slot}</span></p>
                      <p class="text-[11px] text-slate-500 font-mono">Pass: ${b.qrPassToken}</p>
                    </div>

                    <div class="flex items-center gap-4">
                      <div class="text-right">
                        <span class="text-xs text-slate-400 block">Total</span>
                        <span class="font-mono font-bold text-white text-sm">₹${b.pricing.total}</span>
                      </div>

                      <div class="flex gap-2">
                        ${!isCancelled ? `
                          <button data-booking-id="${b.bookingId}" class="btn-show-pass px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-bold text-white transition">
                            🎟️ View Pass
                          </button>
                          <button data-booking-id="${b.bookingId}" class="btn-cancel-booking px-3.5 py-2 rounded-xl bg-red-950/40 hover:bg-red-900/60 text-red-400 border border-red-800/40 text-xs font-bold transition">
                            Cancel (Refund: ₹${refundPreview.refundAmount})
                          </button>
                        ` : `
                          <span class="text-xs text-red-400">Cancelled</span>
                        `}
                      </div>
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          `}
        </div>
      </div>
    `;

    this.bindEvents(container);
  }

  bindEvents(container) {
    container.querySelector('#btn-back-to-explore').addEventListener('click', () => {
      if (this.onBack) this.onBack();
    });

    const btnSwitch = container.querySelector('#btn-switch-account');
    if (btnSwitch) {
      btnSwitch.addEventListener('click', () => {
        UserAuthModal.show({
          onUserChanged: (user) => {
            this.render(container);
            if (this.onUserChanged) this.onUserChanged(user);
          }
        });
      });
    }

    container.querySelectorAll('.btn-show-pass').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const bookingId = e.currentTarget.getAttribute('data-booking-id');
        const booking = mockDb.data.bookings[bookingId];
        if (this.onShowPass && booking) {
          this.onShowPass(booking);
        }
      });
    });

    container.querySelectorAll('.btn-cancel-booking').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const bookingId = e.currentTarget.getAttribute('data-booking-id');
        const booking = mockDb.data.bookings[bookingId];
        const refundPreview = CancellationService.previewRefund(booking);

        const confirmMsg = `Are you sure you want to cancel this booking?\n\n• Slot Time: ${booking.date} @ ${booking.slot}\n• Policy Window: ${refundPreview.reason}\n• Refund Percentage: ${refundPreview.refundPercentage}%\n• Refund Amount: ₹${refundPreview.refundAmount}\n\nProceed with cancellation?`;

        if (confirm(confirmMsg)) {
          try {
            await CancellationService.cancelBooking(bookingId, this.userId);
            alert(`Booking successfully cancelled! ₹${refundPreview.refundAmount} has been credited to your original payment method.`);
            this.render(container);
          } catch (err) {
            alert(`Cancellation error: ${err.message}`);
          }
        }
      });
    });
  }
}
