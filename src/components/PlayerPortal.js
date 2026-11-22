/**
 * Project Apex - Player Portal & Cancellation Manager
 * Shows ONLY the authenticated user's profile, loyalty points, and match bookings.
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

    // If guest or not signed in, show clean authentication prompt
    if (!user) {
      container.innerHTML = `
        <div class="max-w-xl mx-auto py-12 px-4 text-center space-y-6 animate-fade-in">
          <div class="w-20 h-20 mx-auto rounded-3xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 flex items-center justify-center text-4xl shadow-xl shadow-emerald-500/10">
            🎟️
          </div>
          <div class="space-y-2">
            <h2 class="text-2xl font-black text-white">Access Your Game Passes</h2>
            <p class="text-sm text-slate-400">
              Please sign in or create an account with verified Mobile (+91) & Email OTP to view your match bookings, QR gate passes, and loyalty points.
            </p>
          </div>
          <div class="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
            <button id="btn-portal-login" class="w-full sm:w-auto px-6 py-3 rounded-2xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-sm shadow-xl shadow-emerald-500/20 transition active:scale-95">
              🔑 Sign In / Sign Up
            </button>
            <button id="btn-portal-explore" class="w-full sm:w-auto px-6 py-3 rounded-2xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-sm transition">
              🏟️ Explore Turfs
            </button>
          </div>
        </div>
      `;

      container.querySelector('#btn-portal-login').addEventListener('click', () => {
        UserAuthModal.show({
          initialTab: 'signin',
          onUserChanged: (newUser) => {
            this.render(container);
            if (this.onUserChanged) this.onUserChanged(newUser);
          }
        });
      });

      container.querySelector('#btn-portal-explore').addEventListener('click', () => {
        if (this.onBack) this.onBack();
      });

      return;
    }

    this.userId = user.uid;
    const bookings = BookingService.getPlayerBookings(user.uid);
    const initials = (user.displayName || 'AP').split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();

    container.innerHTML = `
      <div class="space-y-6 animate-fade-in">
        <!-- Profile Header for Authenticated User -->
        <div class="bg-gradient-to-r from-slate-900 via-slate-900 to-emerald-950/80 p-6 rounded-3xl border border-slate-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 shadow-xl">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-2xl bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 flex items-center justify-center font-black text-xl shadow-inner">
              ${initials}
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h2 class="text-xl font-black text-white">${user.displayName}</h2>
                <span class="text-[10px] uppercase px-2 py-0.5 rounded font-mono font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                  ${user.role}
                </span>
                ${user.isPhoneVerified && user.isEmailVerified ? `
                  <span class="text-[9px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 hidden sm:inline-flex items-center gap-1">
                    <span>✓</span> Verified OTP
                  </span>
                ` : ''}
              </div>
              <p class="text-xs text-slate-400 font-mono mt-0.5">
                ${user.phone || 'No phone'} • ${user.email || 'No email'} • ${user.city || 'Thane'}
              </p>
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
            <button id="btn-switch-account" class="px-3.5 py-2.5 rounded-2xl bg-slate-800 hover:bg-slate-700 text-xs font-bold text-white border border-slate-700 transition flex items-center gap-1.5">
              <span>👤 Switch</span>
            </button>
            <button id="btn-portal-logout" class="px-3.5 py-2.5 rounded-2xl bg-red-950/40 hover:bg-red-900/60 text-red-300 border border-red-800/50 text-xs font-bold transition flex items-center gap-1.5">
              <span>🚪 Sign Out</span>
            </button>
          </div>
        </div>

        <!-- Bookings Section (SHOWING ONLY THIS PARTICULAR USER'S BOOKINGS) -->
        <div>
          <div class="flex items-center justify-between mb-4">
            <div>
              <h3 class="text-lg font-black text-white">My Game Bookings (${bookings.length})</h3>
              <p class="text-xs text-slate-400">Match passes and gate access tokens exclusively for ${user.displayName}</p>
            </div>
            <button id="btn-back-to-explore" class="px-4 py-2 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs font-bold transition">
              + Book New Turf
            </button>
          </div>

          ${bookings.length === 0 ? `
            <div class="text-center py-16 px-4 bg-slate-900/40 rounded-3xl border border-slate-800 text-slate-400 text-sm space-y-3">
              <div class="text-3xl">🏟️</div>
              <div class="font-bold text-white">No turf reservations yet</div>
              <p class="text-xs text-slate-400 max-w-sm mx-auto">
                You haven't made any match bookings yet. Explore our floodlit turfs in Thane, Mumbai, and Navi Mumbai to reserve your first slot!
              </p>
              <button id="btn-empty-explore" class="mt-2 px-5 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 text-xs font-black shadow-lg shadow-emerald-500/20 transition">
                Browse Available Turfs →
              </button>
            </div>
          ` : `
            <div class="space-y-3">
              ${bookings.map(b => {
                const isCancelled = b.status === 'CANCELLED';
                const refundPreview = !isCancelled ? CancellationService.previewRefund(b) : null;

                return `
                  <div class="bg-slate-900 border ${isCancelled ? 'border-red-950/60 opacity-60' : 'border-slate-800 hover:border-emerald-500/30'} p-5 rounded-2xl flex flex-col md:flex-row md:items-center justify-between gap-4 transition shadow-md">
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
    const btnBack = container.querySelector('#btn-back-to-explore');
    if (btnBack) {
      btnBack.addEventListener('click', () => {
        if (this.onBack) this.onBack();
      });
    }

    const btnEmptyExplore = container.querySelector('#btn-empty-explore');
    if (btnEmptyExplore) {
      btnEmptyExplore.addEventListener('click', () => {
        if (this.onBack) this.onBack();
      });
    }

    const btnSwitch = container.querySelector('#btn-switch-account');
    if (btnSwitch) {
      btnSwitch.addEventListener('click', () => {
        UserAuthModal.show({
          initialTab: 'signin',
          onUserChanged: (newUser) => {
            this.render(container);
            if (this.onUserChanged) this.onUserChanged(newUser);
          }
        });
      });
    }

    const btnLogout = container.querySelector('#btn-portal-logout');
    if (btnLogout) {
      btnLogout.addEventListener('click', () => {
        UserService.logout();
        this.render(container);
        if (this.onUserChanged) this.onUserChanged(null);
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
