/**
 * Project Apex - Interactive Slot Calendar & Grid Component
 * Real-time time-band rendering, odd-hour badges, and Phase 1 lock acquisition
 */

import { VenueService } from '../services/venueService.js';
import { ConcurrencyService } from '../services/concurrencyService.js';
import { calculateSlotPrice } from '../services/pricingEngine.js';

export class SlotCalendar {
  constructor({ venue, onProceedToCheckout, onBack }) {
    this.venue = venue;
    this.courts = VenueService.getCourtsForVenue(venue.venueId);
    this.selectedCourt = this.courts[0] || null;
    this.selectedDate = this.getFormattedDate(0); // Today YYYY-MM-DD
    this.selectedSlot = null;
    this.onProceedToCheckout = onProceedToCheckout;
    this.onBack = onBack;
    this.availability = null;
  }

  getFormattedDate(offsetDays) {
    const d = new Date();
    d.setDate(d.getDate() + offsetDays);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  render(container) {
    this.availability = VenueService.getDailyAvailability(this.selectedCourt.courtId, this.selectedDate);

    container.innerHTML = `
      <div class="space-y-6">
        <!-- Back Navigation & Venue Header -->
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-800">
          <div class="flex items-center gap-3">
            <button id="btn-back-venues" class="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition">
              ← Back
            </button>
            <div>
              <h2 class="text-2xl font-black text-white">${this.venue.name}</h2>
              <p class="text-xs text-slate-400">${this.venue.address}</p>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <span class="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
              ⚡ Live Dynamic Pricing Active
            </span>
          </div>
        </div>

        <!-- Court Selection Tabs -->
        <div>
          <label class="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">Select Court / Pitch</label>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            ${this.courts.map(court => `
              <button data-court-id="${court.courtId}" class="court-tab text-left p-4 rounded-2xl border transition-all ${this.selectedCourt.courtId === court.courtId ? 'bg-emerald-950/60 border-emerald-500 text-white shadow-lg shadow-emerald-950' : 'bg-slate-900 border-slate-800 text-slate-300 hover:border-slate-700'}">
                <div class="flex items-center justify-between">
                  <span class="font-bold text-sm">${court.name}</span>
                  <span class="text-xs font-bold px-2 py-0.5 rounded-md bg-slate-800 uppercase text-emerald-400">${court.sport}</span>
                </div>
                <div class="text-xs text-slate-400 mt-1">${court.surface} • ${court.dimensions}</div>
                <div class="text-xs font-semibold text-emerald-400 mt-2">Base: ₹${court.baseHourlyRate}/hr</div>
              </button>
            `).join('')}
          </div>
        </div>

        <!-- Date Strip (Next 7 Days) -->
        <div>
          <label class="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">Select Date</label>
          <div class="flex gap-2 overflow-x-auto pb-2 scrollbar-thin">
            ${[0, 1, 2, 3, 4, 5, 6].map(offset => {
              const dateStr = this.getFormattedDate(offset);
              const d = new Date();
              d.setDate(d.getDate() + offset);
              const dayName = offset === 0 ? 'Today' : offset === 1 ? 'Tomorrow' : d.toLocaleDateString('en-US', { weekday: 'short' });
              const dayNum = d.getDate();
              const monthName = d.toLocaleDateString('en-US', { month: 'short' });
              const isSelected = this.selectedDate === dateStr;

              return `
                <button data-date="${dateStr}" class="date-chip shrink-0 px-5 py-3 rounded-2xl border text-center transition-all ${isSelected ? 'bg-emerald-500 text-slate-950 border-emerald-400 font-black shadow-lg shadow-emerald-500/20' : 'bg-slate-900 border-slate-800 text-slate-300 hover:border-slate-700'}">
                  <div class="text-[11px] uppercase font-bold tracking-wider">${dayName}</div>
                  <div class="text-lg font-black mt-0.5">${dayNum} ${monthName}</div>
                </button>
              `;
            }).join('')}
          </div>
        </div>

        <!-- Slot Time Legend -->
        <div class="flex flex-wrap items-center gap-4 text-xs text-slate-400 bg-slate-900/60 p-3 rounded-xl border border-slate-800">
          <div class="flex items-center gap-1.5"><span class="w-3 h-3 rounded-md bg-slate-800 border border-slate-700"></span> Available</div>
          <div class="flex items-center gap-1.5"><span class="w-3 h-3 rounded-md bg-amber-400/20 border border-amber-400"></span> Odd-Hour Deal (Up to 35% OFF)</div>
          <div class="flex items-center gap-1.5"><span class="w-3 h-3 rounded-md bg-emerald-500"></span> Selected</div>
          <div class="flex items-center gap-1.5"><span class="w-3 h-3 rounded-md bg-amber-500/40 border border-amber-500"></span> Reserved (Checking Out)</div>
          <div class="flex items-center gap-1.5"><span class="w-3 h-3 rounded-md bg-red-950/60 border border-red-800"></span> Booked</div>
        </div>

        <!-- Time Slots Grid -->
        <div>
          <label class="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">Available 1-Hour Time Slots</label>
          <div id="slots-grid" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
            ${this.renderSlotsGrid()}
          </div>
        </div>

        <!-- Sticky Bottom Summary Bar -->
        <div id="checkout-cta-bar" class="p-5 rounded-2xl bg-gradient-to-r from-slate-900 via-slate-900 to-emerald-950 border border-emerald-500/30 flex flex-col sm:flex-row items-center justify-between gap-4 shadow-2xl">
          <div id="selected-slot-summary" class="text-sm text-slate-300">
            ${this.selectedSlot ? this.renderSlotSummary() : `
              <div class="flex items-center gap-2 text-slate-400">
                <span class="text-lg">👆</span> Select a time slot above to see dynamic pricing and reserve.
              </div>
            `}
          </div>

          <button id="btn-lock-proceed" ${!this.selectedSlot ? 'disabled' : ''} class="w-full sm:w-auto px-8 py-3.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 disabled:opacity-40 disabled:cursor-not-allowed text-slate-950 font-black text-sm shadow-xl shadow-emerald-500/20 transition active:scale-95 flex items-center justify-center gap-2">
            <span>🔒 Lock Slot & Proceed</span>
            <span class="text-xs bg-slate-950/20 px-2 py-0.5 rounded font-mono">5-min Lease</span>
          </button>
        </div>
      </div>
    `;

    this.bindEvents(container);
  }

  renderSlotsGrid() {
    const slots = Object.values(this.availability.slots).sort((a, b) => a.slotTime.localeCompare(b.slotTime));

    return slots.map(slot => {
      const isSelected = this.selectedSlot === slot.slotTime;
      const isBooked = slot.status === 'BOOKED';
      const isReserved = slot.status === 'RESERVED';

      let slotClasses = "p-3.5 rounded-xl border text-left transition-all relative flex flex-col justify-between h-24 ";

      if (isSelected) {
        slotClasses += "bg-emerald-500 text-slate-950 border-emerald-400 ring-2 ring-emerald-400 shadow-lg shadow-emerald-500/30";
      } else if (isBooked) {
        slotClasses += "bg-slate-900/40 border-red-950/60 text-slate-600 cursor-not-allowed opacity-50";
      } else if (isReserved) {
        slotClasses += "bg-amber-950/30 border-amber-500/50 text-amber-300 cursor-not-allowed";
      } else if (slot.isOddHour) {
        slotClasses += "bg-slate-900 hover:border-amber-400 border-amber-500/30 text-white";
      } else {
        slotClasses += "bg-slate-900 hover:border-emerald-500/60 border-slate-800 text-white";
      }

      return `
        <button data-slot="${slot.slotTime}" ${isBooked || isReserved ? 'disabled' : ''} class="slot-cell ${slotClasses}">
          <div class="flex items-center justify-between w-full">
            <span class="font-black text-sm ${isSelected ? 'text-slate-950' : 'text-white'}">${slot.slotTime}</span>
            ${slot.isOddHour && !isBooked ? `
              <span class="text-[10px] font-black px-1.5 py-0.5 rounded ${isSelected ? 'bg-slate-950 text-amber-300' : 'bg-amber-400/20 text-amber-300 border border-amber-400/40'}">
                -${slot.discountPct}%
              </span>
            ` : ''}
            ${slot.isPeak && !isBooked ? `
              <span class="text-[9px] font-bold px-1 rounded ${isSelected ? 'bg-slate-950 text-white' : 'bg-rose-500/20 text-rose-300'}">
                PEAK
              </span>
            ` : ''}
          </div>

          <div class="w-full mt-auto">
            ${isBooked ? `
              <span class="text-[11px] font-bold text-red-400 block">Booked</span>
            ` : isReserved ? `
              <span class="text-[11px] font-bold text-amber-400 block flex items-center gap-1">
                <span class="w-1.5 h-1.5 rounded-full bg-amber-400 animate-ping"></span> In Checkout
              </span>
            ` : `
              <div class="flex items-baseline justify-between">
                <span class="text-xs font-black ${isSelected ? 'text-slate-950' : 'text-emerald-400'}">₹${slot.currentPrice}</span>
                ${slot.isOddHour ? `
                  <span class="text-[10px] line-through ${isSelected ? 'text-slate-800' : 'text-slate-500'}">₹${slot.basePrice}</span>
                ` : ''}
              </div>
            `}
          </div>
        </button>
      `;
    }).join('');
  }

  renderSlotSummary() {
    const pricing = calculateSlotPrice({
      court: this.selectedCourt,
      slotTime: this.selectedSlot,
      city: this.venue.city
    });

    return `
      <div class="space-y-1">
        <div class="flex items-center gap-2">
          <span class="font-bold text-white text-base">${this.selectedCourt.name}</span>
          <span class="text-xs px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-mono font-bold">${this.selectedDate} @ ${this.selectedSlot}</span>
        </div>
        <div class="text-xs text-slate-400 flex items-center gap-3">
          <span>Base: ₹${pricing.baseRate}</span>
          ${pricing.isOddHour ? `<span class="text-amber-400 font-bold">⚡ Odd-Hour Discount: -₹${pricing.oddHourDiscountAmount} (${pricing.bandName})</span>` : ''}
          <span>Estimated Total: <strong class="text-white text-sm">₹${pricing.total}</strong></span>
        </div>
      </div>
    `;
  }

  bindEvents(container) {
    // Back to venues
    container.querySelector('#btn-back-venues').addEventListener('click', () => {
      if (this.onBack) this.onBack();
    });

    // Court tabs
    container.querySelectorAll('.court-tab').forEach(tab => {
      tab.addEventListener('click', (e) => {
        const courtId = e.currentTarget.getAttribute('data-court-id');
        this.selectedCourt = this.courts.find(c => c.courtId === courtId);
        this.selectedSlot = null;
        this.render(container);
      });
    });

    // Date chips
    container.querySelectorAll('.date-chip').forEach(chip => {
      chip.addEventListener('click', (e) => {
        this.selectedDate = e.currentTarget.getAttribute('data-date');
        this.selectedSlot = null;
        this.render(container);
      });
    });

    // Slot cells
    container.querySelectorAll('.slot-cell:not([disabled])').forEach(cell => {
      cell.addEventListener('click', (e) => {
        this.selectedSlot = e.currentTarget.getAttribute('data-slot');
        this.render(container);
      });
    });

    // Lock & Proceed
    const btnLock = container.querySelector('#btn-lock-proceed');
    if (btnLock) {
      btnLock.addEventListener('click', async () => {
        if (!this.selectedSlot) return;

        btnLock.disabled = true;
        btnLock.innerHTML = `<span class="animate-spin inline-block mr-2">⏳</span> Acquiring Atomic Lock...`;

        try {
          // Acquire Phase 1 5-minute atomic lock
          const lockResult = await ConcurrencyService.acquireLock({
            courtId: this.selectedCourt.courtId,
            date: this.selectedDate,
            slot: this.selectedSlot,
            userId: 'demo_player'
          });

          const pricing = calculateSlotPrice({
            court: this.selectedCourt,
            slotTime: this.selectedSlot,
            city: this.venue.city
          });

          if (this.onProceedToCheckout) {
            this.onProceedToCheckout({
              venue: this.venue,
              court: this.selectedCourt,
              date: this.selectedDate,
              slot: this.selectedSlot,
              pricing,
              lease: lockResult.lease
            });
          }
        } catch (err) {
          alert(`Lock Acquisition Error: ${err.message}`);
          this.render(container);
        }
      });
    }
  }
}
