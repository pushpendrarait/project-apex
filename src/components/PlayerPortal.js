/**
 * Project Apex - Player Portal & Cancellation Manager
 * Shows ONLY the authenticated user's profile, loyalty points, and match bookings.
 */

import { BookingService } from '../services/bookingService.js';
import { CancellationService } from '../services/cancellationService.js';
import { UserService } from '../services/userService.js';
import { ApiClient } from '../services/apiClient.js';
import { UserAuthModal } from './UserAuthModal.js';
import { mockDb } from '../services/firebaseConfig.js';

export class PlayerPortal {
  constructor({ onBack, onShowPass, onUserChanged }) {
    this.onBack = onBack;
    this.onShowPass = onShowPass;
    this.onUserChanged = onUserChanged;
    this._syncedUid = null;
    this._statusMessage = null;
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
    if (typeof window !== 'undefined' && ApiClient.getToken() && this._syncedUid !== user.uid) {
      this._syncedUid = user.uid;
      BookingService.syncMyBookingsFromBackend().then(() => {
        if (container.isConnected) {
          this.render(container);
        }
      });
    }

    const bookings = BookingService.getPlayerBookings(user.uid);
    const initials = (user.displayName || 'AP').split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
    const isOwner = ApiClient.isOwnerRole(user.role);

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
                  ${isOwner ? 'Facility Owner' : user.role}
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

          <div class="flex flex-wrap items-center gap-2.5">
            <div class="bg-slate-950/80 border border-slate-800 px-4 py-2 rounded-2xl">
              <span class="text-[10px] uppercase font-bold text-slate-400 block">Apex Loyalty Points</span>
              <span class="text-base font-black text-emerald-400 font-mono">${user.loyaltyPoints || 0} Pts</span>
            </div>
            <div class="bg-slate-950/80 border border-slate-800 px-4 py-2 rounded-2xl">
              <span class="text-[10px] uppercase font-bold text-slate-400 block">Referral Code</span>
              <span class="text-base font-black text-amber-400 font-mono">${user.referralCode || 'APEX50'}</span>
            </div>
            <button id="btn-edit-profile" class="px-3.5 py-2.5 rounded-2xl bg-indigo-500/20 hover:bg-indigo-500/30 text-indigo-300 border border-indigo-500/40 text-xs font-bold transition flex items-center gap-1.5">
              <span>✏️ Edit Profile</span>
            </button>
            <button id="btn-switch-account" class="px-3.5 py-2.5 rounded-2xl bg-slate-800 hover:bg-slate-700 text-xs font-bold text-white border border-slate-700 transition flex items-center gap-1.5">
              <span>👤 Switch</span>
            </button>
            <button id="btn-portal-logout" class="px-3.5 py-2.5 rounded-2xl bg-red-950/40 hover:bg-red-900/60 text-red-300 border border-red-800/50 text-xs font-bold transition flex items-center gap-1.5">
              <span>🚪 Sign Out</span>
            </button>
          </div>
        </div>

        ${this._statusMessage ? `
          <div id="player-portal-status" class="text-xs p-3.5 rounded-2xl border ${this._statusMessage.isError ? 'bg-red-950/80 border-red-500/40 text-red-300' : 'bg-emerald-950/80 border-emerald-500/40 text-emerald-300'} flex items-center justify-between">
            <span>${this._statusMessage.text}</span>
            <button id="btn-dismiss-player-status" class="text-slate-400 hover:text-white ml-4">✕</button>
          </div>
        ` : ''}

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
                const isCancelled = b.status === 'CANCELLED' || (b.status && b.status.startsWith('CANCELLED'));
                const isCheckedIn = b.status === 'CHECKED_IN' || b.status === 'COMPLETED';
                const refundPreview = !isCancelled ? CancellationService.previewRefund(b) : null;

                return `
                  <div data-booking-card="${b.bookingId}" class="bg-slate-900 border ${isCancelled ? 'border-red-950/60 opacity-75' : 'border-slate-800 hover:border-emerald-500/30'} p-5 rounded-2xl flex flex-col md:flex-row md:items-center justify-between gap-4 transition shadow-md">
                    <div class="space-y-1">
                      <div class="flex items-center gap-2">
                        <span class="font-bold text-white text-base">${b.venueName}</span>
                        <span class="px-2 py-0.5 rounded text-[10px] font-bold uppercase ${isCancelled ? 'bg-red-950 text-red-400 border border-red-800' : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'}">
                          ${b.status}
                        </span>
                      </div>
                      <p class="text-xs text-slate-400">${b.courtName} • <span class="text-emerald-400 font-mono font-bold">${b.date} @ ${b.slot}</span></p>
                      <p class="text-[11px] text-slate-500 font-mono">Pass: ${b.qrPassToken}</p>
                      ${b.notes ? `<p class="text-[11px] text-amber-300/90">Note: ${b.notes}</p>` : ''}
                    </div>

                    <div class="flex flex-wrap items-center gap-3">
                      <div class="text-right mr-2">
                        <span class="text-xs text-slate-400 block">Total</span>
                        <span class="font-mono font-bold text-white text-sm">₹${b.pricing.total}</span>
                      </div>

                      <div class="flex flex-wrap gap-2">
                        ${!isCancelled ? `
                          <button data-booking-id="${b.bookingId}" class="btn-show-pass px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-bold text-white transition">
                            🎟️ Pass
                          </button>
                          ${!isCheckedIn ? `
                            <button data-booking-id="${b.bookingId}" class="btn-edit-booking px-3 py-2 rounded-xl bg-indigo-500/20 hover:bg-indigo-500/30 text-indigo-300 border border-indigo-500/40 text-xs font-bold transition">
                              ✏️ Reschedule
                            </button>
                            <button data-booking-id="${b.bookingId}" class="btn-cancel-booking px-3 py-2 rounded-xl bg-amber-950/50 hover:bg-amber-900/60 text-amber-300 border border-amber-700/50 text-xs font-bold transition">
                              Cancel (₹${refundPreview.refundAmount})
                            </button>
                          ` : ''}
                        ` : `
                          <span class="text-xs text-red-400 self-center mr-1">Cancelled${b.refundDetails?.refundAmount != null ? ` (₹${b.refundDetails.refundAmount} refund)` : ''}</span>
                        `}
                        <button data-booking-id="${b.bookingId}" class="btn-delete-booking px-3 py-2 rounded-xl bg-red-950/50 hover:bg-red-900/60 text-red-300 border border-red-800/40 text-xs font-bold transition">
                          🗑️ Delete
                        </button>
                      </div>
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          `}
        </div>
      </div>
      <div id="player-modal-container"></div>
    `;

    this.bindEvents(container, user);
  }

  bindEvents(container, currentUser) {
    const dismissBtn = container.querySelector('#btn-dismiss-player-status');
    if (dismissBtn) {
      dismissBtn.addEventListener('click', () => {
        this._statusMessage = null;
        this.render(container);
      });
    }

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

    const btnEditProfile = container.querySelector('#btn-edit-profile');
    if (btnEditProfile && currentUser) {
      btnEditProfile.addEventListener('click', () => this.showEditProfileModal(container, currentUser));
    }

    const btnSwitch = container.querySelector('#btn-switch-account');
    if (btnSwitch) {
      btnSwitch.addEventListener('click', () => {
        UserAuthModal.show({
          initialTab: 'signin',
          onUserChanged: (newUser) => {
            this._syncedUid = null;
            this.render(container);
            if (this.onUserChanged) this.onUserChanged(newUser);
          }
        });
      });
    }

    const btnLogout = container.querySelector('#btn-portal-logout');
    if (btnLogout) {
      btnLogout.addEventListener('click', () => {
        this._syncedUid = null;
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

    container.querySelectorAll('.btn-edit-booking').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const bookingId = e.currentTarget.getAttribute('data-booking-id');
        const booking = mockDb.data.bookings[bookingId];
        if (booking) {
          this.showRescheduleModal(container, booking);
        }
      });
    });

    container.querySelectorAll('.btn-cancel-booking').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const bookingId = e.currentTarget.getAttribute('data-booking-id');
        const booking = mockDb.data.bookings[bookingId];
        const refundPreview = CancellationService.previewRefund(booking);
        this.showConfirmActionModal(container, {
          title: 'Cancel Booking (3-Tier Refund)',
          body: `Slot: ${booking.date} @ ${booking.slot}\nPolicy: ${refundPreview.reason}\nRefund Amount: ₹${refundPreview.refundAmount}`,
          confirmLabel: `Confirm Cancel (Refund ₹${refundPreview.refundAmount})`,
          onConfirm: async () => {
            await CancellationService.cancelBooking(bookingId, this.userId);
            this._statusMessage = { isError: false, text: `✓ Booking ${bookingId} cancelled. ₹${refundPreview.refundAmount} refund initiated.` };
            this.render(container);
          }
        });
      });
    });

    container.querySelectorAll('.btn-delete-booking').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const bookingId = e.currentTarget.getAttribute('data-booking-id');
        const booking = mockDb.data.bookings[bookingId];
        this.showConfirmActionModal(container, {
          title: 'Delete Booking Record',
          body: `Delete reservation ${booking.venueName} (${booking.date} @ ${booking.slot}) and release its slot lock?`,
          confirmLabel: 'Confirm Delete Booking',
          onConfirm: async () => {
            await BookingService.deleteBookingAndSync(bookingId);
            this._statusMessage = { isError: false, text: `✓ Booking ${bookingId} deleted and slot released.` };
            this.render(container);
          }
        });
      });
    });
  }

  showEditProfileModal(container, user) {
    const host = container.querySelector('#player-modal-container');
    host.innerHTML = `
      <div id="edit-profile-backdrop" class="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 text-xs">
        <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl">
          <div class="flex items-center justify-between pb-2 border-b border-slate-800">
            <h3 class="text-lg font-black text-white">Edit Profile</h3>
            <button id="btn-close-edit-profile" class="w-8 h-8 rounded-full bg-slate-800 text-slate-300">✕</button>
          </div>
          <form id="form-edit-profile" class="space-y-3">
            <div>
              <label class="block text-slate-400 font-bold mb-1">Full Name</label>
              <input id="edit-profile-name" type="text" required value="${user.displayName || ''}" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white" />
            </div>
            <div>
              <label class="block text-slate-400 font-bold mb-1">Phone</label>
              <input id="edit-profile-phone" type="text" required value="${user.phone || ''}" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono" />
            </div>
            <div>
              <label class="block text-slate-400 font-bold mb-1">City</label>
              <select id="edit-profile-city" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white">
                <option value="Thane" ${user.city === 'Thane' ? 'selected' : ''}>Thane</option>
                <option value="Mumbai" ${user.city === 'Mumbai' ? 'selected' : ''}>Mumbai</option>
                <option value="Navi Mumbai" ${user.city === 'Navi Mumbai' ? 'selected' : ''}>Navi Mumbai</option>
              </select>
            </div>
            <div class="flex justify-end gap-2 pt-2">
              <button type="button" id="btn-cancel-edit-profile" class="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 font-bold">Cancel</button>
              <button type="submit" id="btn-save-profile" class="px-5 py-2 rounded-xl bg-emerald-500 text-slate-950 font-black">Save Profile</button>
            </div>
          </form>
        </div>
      </div>
    `;
    const close = () => { host.innerHTML = ''; };
    host.querySelector('#btn-close-edit-profile').addEventListener('click', close);
    host.querySelector('#btn-cancel-edit-profile').addEventListener('click', close);
    host.querySelector('#form-edit-profile').addEventListener('submit', async (e) => {
      e.preventDefault();
      try {
        const updated = await UserService.updateProfileAsync({
          fullName: host.querySelector('#edit-profile-name').value.trim(),
          phone: host.querySelector('#edit-profile-phone').value.trim(),
          city: host.querySelector('#edit-profile-city').value
        });
        this._statusMessage = { isError: false, text: `✓ Profile updated for ${updated.displayName} (${updated.city}).` };
        close();
        if (this.onUserChanged) this.onUserChanged(updated);
        this.render(container);
      } catch (err) {
        this._statusMessage = { isError: true, text: `❌ Profile update error: ${err.message}` };
        close();
        this.render(container);
      }
    });
  }

  showRescheduleModal(container, booking) {
    const host = container.querySelector('#player-modal-container');
    const slots = ["06:00","07:00","08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00","17:00","18:00","19:00","20:00","21:00","22:00"];
    host.innerHTML = `
      <div class="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 text-xs">
        <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl">
          <div class="flex items-center justify-between pb-2 border-b border-slate-800">
            <h3 class="text-lg font-black text-white">Reschedule / Update Booking</h3>
            <button id="btn-close-reschedule" class="w-8 h-8 rounded-full bg-slate-800 text-slate-300">✕</button>
          </div>
          <form id="form-reschedule-booking" class="space-y-3">
            <div>
              <label class="block text-slate-400 font-bold mb-1">Match Date</label>
              <input id="edit-booking-date" type="date" required value="${booking.date}" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono" />
            </div>
            <div>
              <label class="block text-slate-400 font-bold mb-1">Time Slot</label>
              <select id="edit-booking-slot" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono">
                ${slots.map(s => `<option value="${s}" ${s === booking.slot ? 'selected' : ''}>${s}</option>`).join('')}
              </select>
            </div>
            <div>
              <label class="block text-slate-400 font-bold mb-1">Notes</label>
              <input id="edit-booking-notes" type="text" placeholder="Optional team or equipment note" value="${booking.notes || ''}" class="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white" />
            </div>
            <div class="flex justify-end gap-2 pt-2">
              <button type="button" id="btn-cancel-reschedule" class="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 font-bold">Cancel</button>
              <button type="submit" id="btn-save-reschedule" class="px-5 py-2 rounded-xl bg-indigo-500 text-white font-black">Update Booking</button>
            </div>
          </form>
        </div>
      </div>
    `;
    const close = () => { host.innerHTML = ''; };
    host.querySelector('#btn-close-reschedule').addEventListener('click', close);
    host.querySelector('#btn-cancel-reschedule').addEventListener('click', close);
    host.querySelector('#form-reschedule-booking').addEventListener('submit', async (e) => {
      e.preventDefault();
      try {
        const res = await BookingService.updateBookingAndSync(booking.bookingId, {
          bookingDate: host.querySelector('#edit-booking-date').value,
          slotTime: host.querySelector('#edit-booking-slot').value,
          notes: host.querySelector('#edit-booking-notes').value.trim()
        });
        this._statusMessage = { isError: false, text: `✓ Booking updated to ${res.booking.date} @ ${res.booking.slot}.` };
        close();
        this.render(container);
      } catch (err) {
        this._statusMessage = { isError: true, text: `❌ Update failed: ${err.message}` };
        close();
        this.render(container);
      }
    });
  }

  showConfirmActionModal(container, { title, body, confirmLabel, onConfirm }) {
    const host = container.querySelector('#player-modal-container');
    host.innerHTML = `
      <div class="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 text-xs">
        <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl">
          <h3 class="text-lg font-black text-white">${title}</h3>
          <p class="text-slate-300 whitespace-pre-line">${body}</p>
          <div class="flex justify-end gap-2 pt-2">
            <button id="btn-confirm-dismiss" class="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 font-bold">Back</button>
            <button id="btn-confirm-proceed" class="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-500 text-white font-black">${confirmLabel}</button>
          </div>
        </div>
      </div>
    `;
    const close = () => { host.innerHTML = ''; };
    host.querySelector('#btn-confirm-dismiss').addEventListener('click', close);
    host.querySelector('#btn-confirm-proceed').addEventListener('click', async () => {
      try {
        await onConfirm();
      } catch (err) {
        this._statusMessage = { isError: true, text: `❌ ${err.message}` };
        this.render(container);
      } finally {
        if (host.isConnected) host.innerHTML = '';
      }
    });
  }
}
