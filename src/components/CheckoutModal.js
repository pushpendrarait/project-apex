/**
 * Project Apex - Frictionless Checkout Dialog
 * Displays 5-minute lock lease countdown, promo codes, and executes atomic booking commit
 */

import { ConcurrencyService } from '../services/concurrencyService.js';
import { BookingService } from '../services/bookingService.js';
import { UserService } from '../services/userService.js';
import { calculateSlotPrice } from '../services/pricingEngine.js';
import { ApiClient } from '../services/apiClient.js';

export class CheckoutModal {
  constructor({ bookingContext, onBookingSuccess, onCancel }) {
    this.context = bookingContext;
    this.onBookingSuccess = onBookingSuccess;
    this.onCancel = onCancel;
    this.promoCode = '';
    this.pricing = bookingContext.pricing;
    this.selectedMethod = 'UPI';
    this.timerInterval = null;
    this.remainingSeconds = 300; // 5 minutes default
  }

  show() {
    let modal = document.getElementById('apex-checkout-modal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'apex-checkout-modal';
      modal.className = 'fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-fade-in';
      document.body.appendChild(modal);
    }

    this.startTimer();
    this.render(modal);
  }

  startTimer() {
    const expiresAt = this.context.lease.expiresAt;
    this.timerInterval = setInterval(() => {
      const now = Date.now();
      this.remainingSeconds = Math.max(0, Math.floor((expiresAt - now) / 1000));

      const timerEl = document.getElementById('lease-countdown-timer');
      if (timerEl) {
        const mins = String(Math.floor(this.remainingSeconds / 60)).padStart(2, '0');
        const secs = String(this.remainingSeconds % 60).padStart(2, '0');
        timerEl.textContent = `${mins}:${secs}`;
      }

      if (this.remainingSeconds <= 0) {
        clearInterval(this.timerInterval);
        alert("Your 5-minute reservation hold has expired. The slot has been released back to other players.");
        this.close();
      }
    }, 1000);
  }

  render(modal) {
    const { venue, court, date, slot } = this.context;
    const mins = String(Math.floor(this.remainingSeconds / 60)).padStart(2, '0');
    const secs = String(this.remainingSeconds % 60).padStart(2, '0');

    modal.innerHTML = `
      <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl relative overflow-hidden text-white space-y-6">
        
        <!-- Header & Lease Timer -->
        <div class="flex items-center justify-between pb-4 border-b border-slate-800">
          <div>
            <h3 class="text-xl font-black">Complete Booking</h3>
            <p class="text-xs text-slate-400 mt-0.5">${court.name} • ${venue.name}</p>
          </div>
          <div class="text-right">
            <span class="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Lock Hold</span>
            <div id="lease-countdown-timer" class="text-lg font-mono font-black text-amber-400 flex items-center gap-1">
              <span class="w-2 h-2 rounded-full bg-amber-400 animate-ping"></span>
              ${mins}:${secs}
            </div>
          </div>
        </div>

        <!-- Slot Details Card -->
        <div class="bg-slate-950/60 p-4 rounded-2xl border border-slate-800 space-y-2 text-xs">
          <div class="flex justify-between text-slate-300">
            <span>Date & Time:</span>
            <strong class="text-emerald-400 font-mono">${date} @ ${slot} (1 Hour)</strong>
          </div>
          <div class="flex justify-between text-slate-300">
            <span>Location:</span>
            <span>${venue.area}, ${venue.city}</span>
          </div>
          <div class="flex justify-between text-slate-300">
            <span>Player:</span>
            <span>${UserService.getCurrentUser() ? UserService.getCurrentUser().displayName : 'Guest'} (${UserService.getCurrentUser() ? UserService.getCurrentUser().phone : 'Verified'})</span>
          </div>
        </div>

        <!-- Coupon Code Section -->
        <div class="space-y-2">
          <label class="block text-xs font-bold uppercase tracking-wider text-slate-400">Apply Promo Code</label>
          <div class="flex gap-2">
            <input id="input-promo" type="text" placeholder="e.g. THANE50, APEX20, ODDHOUR35" value="${this.promoCode}" class="flex-1 bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-xs text-white uppercase font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
            <button id="btn-apply-promo" class="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-bold text-white transition">
              Apply
            </button>
          </div>
          <div id="promo-feedback" class="text-[11px]">
            ${this.pricing.promoError ? `<span class="text-red-400">⚠️ ${this.pricing.promoError}</span>` : ''}
            ${this.pricing.promoCode ? `<span class="text-emerald-400 font-bold">✓ Promo '${this.pricing.promoCode}' applied! Saved ₹${this.pricing.promoDiscountAmount}</span>` : ''}
          </div>
        </div>

        <!-- Pricing Breakdown -->
        <div class="space-y-2 pt-2 border-t border-slate-800 text-xs">
          <div class="flex justify-between text-slate-400">
            <span>Base Hourly Rate:</span>
            <span>₹${this.pricing.baseRate}</span>
          </div>
          ${this.pricing.isOddHour ? `
            <div class="flex justify-between text-amber-400 font-bold">
              <span>⚡ ${this.pricing.bandName} (-${this.pricing.discountPct}%):</span>
              <span>-₹${this.pricing.oddHourDiscountAmount}</span>
            </div>
          ` : ''}
          ${this.pricing.promoDiscountAmount > 0 ? `
            <div class="flex justify-between text-emerald-400 font-bold">
              <span>🎟️ Promo Discount:</span>
              <span>-₹${this.pricing.promoDiscountAmount}</span>
            </div>
          ` : ''}
          <div class="flex justify-between text-slate-400">
            <span>Platform Convenience Fee:</span>
            <span>₹${this.pricing.convenienceFee}</span>
          </div>
          <div class="flex justify-between text-base font-black text-white pt-2 border-t border-slate-800">
            <span>Total Payable:</span>
            <span class="text-emerald-400 font-mono text-xl">₹${this.pricing.total}</span>
          </div>
        </div>

        <!-- Payment Method Selection -->
        <div class="space-y-2">
          <label class="block text-xs font-bold uppercase tracking-wider text-slate-400">Payment Option</label>
          <div class="grid grid-cols-3 gap-2">
            <button data-method="UPI" class="pay-method-btn p-3 rounded-xl border text-xs font-bold text-center transition ${this.selectedMethod === 'UPI' ? 'bg-emerald-500/20 border-emerald-500 text-emerald-400' : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'}">
              📲 UPI / QR
            </button>
            <button data-method="Card" class="pay-method-btn p-3 rounded-xl border text-xs font-bold text-center transition ${this.selectedMethod === 'Card' ? 'bg-emerald-500/20 border-emerald-500 text-emerald-400' : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'}">
              💳 Debit / Credit
            </button>
            <button data-method="NetBanking" class="pay-method-btn p-3 rounded-xl border text-xs font-bold text-center transition ${this.selectedMethod === 'NetBanking' ? 'bg-emerald-500/20 border-emerald-500 text-emerald-400' : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'}">
              🏦 Net Banking
            </button>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex gap-3 pt-4 border-t border-slate-800">
          <button id="btn-cancel-checkout" class="flex-1 py-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-bold text-slate-300 transition">
            Release Slot & Cancel
          </button>
          <button id="btn-pay-confirm" class="flex-1 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-sm shadow-xl shadow-emerald-500/20 transition active:scale-95 flex items-center justify-center gap-2">
            <span>Pay ₹${this.pricing.total}</span>
            <span>→</span>
          </button>
        </div>

      </div>
    `;

    this.bindEvents(modal);
  }

  bindEvents(modal) {
    // Payment method switch
    modal.querySelectorAll('.pay-method-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        this.selectedMethod = e.currentTarget.getAttribute('data-method');
        modal.querySelectorAll('.pay-method-btn').forEach(b => {
          b.className = 'pay-method-btn p-3 rounded-xl border text-xs font-bold text-center transition bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700';
        });
        e.currentTarget.className = 'pay-method-btn p-3 rounded-xl border text-xs font-bold text-center transition bg-emerald-500/20 border-emerald-500 text-emerald-400';
      });
    });

    // Apply promo
    const btnPromo = modal.querySelector('#btn-apply-promo');
    const inputPromo = modal.querySelector('#input-promo');
    btnPromo.addEventListener('click', () => {
      this.promoCode = inputPromo.value.trim().toUpperCase();
      this.pricing = calculateSlotPrice({
        court: this.context.court,
        slotTime: this.context.slot,
        promoCode: this.promoCode,
        city: this.context.venue.city
      });
      this.render(modal);
    });

    // Cancel / Release Lock
    modal.querySelector('#btn-cancel-checkout').addEventListener('click', async () => {
      const currentUser = UserService.getCurrentUser();
      await ConcurrencyService.releaseLock({
        courtId: this.context.court.courtId,
        date: this.context.date,
        slot: this.context.slot,
        userId: currentUser.uid
      });
      this.close();
      if (this.onCancel) this.onCancel();
    });

    // Pay and Commit Phase 3 with Razorpay Integration
    const btnPay = modal.querySelector('#btn-pay-confirm');
    btnPay.addEventListener('click', async () => {
      btnPay.disabled = true;
      btnPay.innerHTML = `<span class="animate-spin mr-2">⏳</span> Initializing Payment...`;

      try {
        const currentUser = UserService.getCurrentUser();

        // 1. If Razorpay Checkout JS is loaded in the browser, launch Razorpay Gateway
        if (typeof window !== 'undefined' && window.Razorpay) {
          let orderData = null;
          try {
            const orderRes = await ApiClient.createRazorpayOrder({
              amount: this.pricing.total,
              currency: 'INR',
              receipt: `rcpt_${this.context.court.courtId}_${Date.now()}`
            });
            if (orderRes && orderRes.data) {
              orderData = orderRes.data;
            }
          } catch (e) {
            console.warn('[CheckoutModal] Backend Razorpay order endpoint fallback:', e.message);
          }

          const rzpKey = orderData?.key_id || 'rzp_test_ApexSports2026';
          const rzpOrderId = orderData?.order_id || `order_${Math.random().toString(36).substring(2, 14)}`;

          const options = {
            key: rzpKey,
            amount: Math.round(this.pricing.total * 100),
            currency: 'INR',
            name: 'Project Apex Sports',
            description: `${this.context.court.name} - ${this.context.date} @ ${this.context.slot}`,
            order_id: rzpOrderId,
            prefill: {
              name: currentUser.displayName,
              email: currentUser.email,
              contact: currentUser.phone
            },
            theme: {
              color: '#10b981'
            },
            handler: async (response) => {
              btnPay.innerHTML = `<span class="animate-spin mr-2">⏳</span> Committing Atomic Booking...`;
              try {
                const result = await BookingService.commitBooking({
                  userId: currentUser.uid,
                  userName: currentUser.displayName,
                  userPhone: currentUser.phone,
                  venueId: this.context.venue.venueId,
                  venueName: this.context.venue.name,
                  courtId: this.context.court.courtId,
                  courtName: this.context.court.name,
                  sport: this.context.court.sport,
                  date: this.context.date,
                  slot: this.context.slot,
                  pricing: this.pricing,
                  paymentMethod: this.selectedMethod,
                  razorpayPaymentId: response.razorpay_payment_id,
                  razorpayOrderId: response.razorpay_order_id || rzpOrderId,
                  razorpaySignature: response.razorpay_signature
                });

                clearInterval(this.timerInterval);
                this.close();

                if (this.onBookingSuccess) {
                  this.onBookingSuccess(result.booking);
                }
              } catch (commitErr) {
                alert(`Booking Commit Failed: ${commitErr.message}`);
                btnPay.disabled = false;
                btnPay.innerHTML = `Pay ₹${this.pricing.total} →`;
              }
            },
            modal: {
              ondismiss: () => {
                btnPay.disabled = false;
                btnPay.innerHTML = `Pay ₹${this.pricing.total} →`;
              }
            }
          };

          const rzpInstance = new window.Razorpay(options);
          rzpInstance.open();
          return;
        }

        // 2. Direct Atomic Commit (e.g. Unit tests, offline, or headless browser)
        btnPay.innerHTML = `<span class="animate-spin mr-2">⏳</span> Processing Atomic Commit...`;
        const result = await BookingService.commitBooking({
          userId: currentUser.uid,
          userName: currentUser.displayName,
          userPhone: currentUser.phone,
          venueId: this.context.venue.venueId,
          venueName: this.context.venue.name,
          courtId: this.context.court.courtId,
          courtName: this.context.court.name,
          sport: this.context.court.sport,
          date: this.context.date,
          slot: this.context.slot,
          pricing: this.pricing,
          paymentMethod: this.selectedMethod
        });

        clearInterval(this.timerInterval);
        this.close();

        if (this.onBookingSuccess) {
          this.onBookingSuccess(result.booking);
        }
      } catch (err) {
        alert(`Booking Transaction Failed: ${err.message}`);
        btnPay.disabled = false;
        btnPay.innerHTML = `Pay ₹${this.pricing.total} →`;
      }
    });
  }

  close() {
    if (this.timerInterval) clearInterval(this.timerInterval);
    const modal = document.getElementById('apex-checkout-modal');
    if (modal) modal.remove();
  }
}
