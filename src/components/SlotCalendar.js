/**
 * Project Apex - Interactive Slot Calendar & Grid Component
 * BookMyShow Showtime Selection & Snabbit Turf Experience
 * Features:
 * - Time-of-day tabs (Morning, Afternoon 35% OFF, Prime Evening, Night Owl)
 * - BookMyShow showtime pills with color-coded live availability status
 * - Sticky mobile/desktop checkout summary bar with 5-minute atomic lock lease
 */

import { VenueService } from '../services/venueService.js';
import { ConcurrencyService } from '../services/concurrencyService.js';
import { UserService } from '../services/userService.js';
import { calculateSlotPrice } from '../services/pricingEngine.js';

export class SlotCalendar {
  constructor({ venue, onProceedToCheckout, onBack }) {
    this.venue = venue;
    this.courts = VenueService.getCourtsForVenue(venue.venueId);
    this.selectedCourt = this.courts[0] || null;
    this.selectedDate = this.getFormattedDate(0); // Today YYYY-MM-DD
    this.selectedSlot = null;
    this.selectedTimeBand = 'all'; // 'all', 'morning', 'afternoon', 'evening', 'night'
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
    if (!this.selectedCourt) {
      container.innerHTML = `
        <div class="bg-[#131B2E] border border-white/10 rounded-3xl p-8 text-center space-y-4">
          <div class="text-3xl">🏟️</div>
          <h3 class="text-xl font-black text-white">${this.venue.name}</h3>
          <p class="text-xs text-slate-400">No courts or turfs are configured for this facility yet.</p>
          <button id="btn-back-venues" class="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-bold text-white">← Back to Venues</button>
        </div>`;
      container.querySelector('#btn-back-venues')?.addEventListener('click', () => this.onBack && this.onBack());
      return;
    }
    this.availability = VenueService.getDailyAvailability(this.selectedCourt.courtId, this.selectedDate);

    container.innerHTML = `
      <div class="space-y-6 animate-fade-in pb-28 md:pb-24">
        
        <!-- 1. BACK NAVIGATION & VENUE BANNER (BookMyShow Showtime Header) -->
        <div class="bg-[#131B2E] border border-white/10 rounded-3xl p-5 md:p-6 shadow-2xl flex flex-col md:flex-row md:items-center justify-between gap-4 relative overflow-hidden">
          <div class="flex items-center gap-4 z-10">
            <button id="btn-back-venues" class="p-2.5 px-3.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-300 hover:text-white border border-white/10 transition active:scale-95 flex items-center gap-1.5 text-xs font-bold">
              <span>←</span>
              <span>Back to Venues</span>
            </button>
            <div>
              <div class="flex items-center gap-2">
                <h2 class="text-xl md:text-2xl font-black text-white">${this.venue.name}</h2>
                <span class="text-xs font-black text-amber-300 bg-slate-950/80 px-2 py-0.5 rounded-md border border-amber-500/30">
                  ★ ${this.venue.rating}
                </span>
              </div>
              <p class="text-xs text-slate-400 mt-0.5 flex items-center gap-1">
                <span>📍</span> ${this.venue.address}
              </p>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-2 z-10">
            <span class="px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 flex items-center gap-1.5">
              <span class="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
              ⚡ Live Dynamic Pricing Active
            </span>
            <span class="px-3 py-1 rounded-full text-xs font-bold bg-purple-500/10 text-purple-300 border border-purple-500/30">
              Odd-Hour 35% OFF
            </span>
          </div>
        </div>

        <!-- 2. COURT / PITCH SELECTION TABS -->
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <label class="block text-xs font-black uppercase tracking-wider text-slate-400">Select Pitch / Court</label>
            <span class="text-xs text-slate-400">${this.courts.length} court${this.courts.length === 1 ? '' : 's'} available</span>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            ${this.courts.map(court => {
              const isSelected = this.selectedCourt.courtId === court.courtId;
              const sportIcon = court.sport === 'cricket' ? '🏏' : court.sport === 'football' ? '⚽' : court.sport === 'badminton' ? '🏸' : court.sport === 'pickleball' ? '🏓' : '🎯';
              return `
                <button data-court-id="${court.courtId}" class="court-tab text-left p-4 rounded-2xl border transition-all duration-200 relative ${isSelected ? 'bg-emerald-500/15 border-emerald-500 text-white shadow-xl ring-2 ring-emerald-500/30' : 'bg-[#131B2E] border-white/10 text-slate-300 hover:border-slate-700 hover:bg-slate-800/80'}">
                  <div class="flex items-center justify-between">
                    <span class="font-black text-sm flex items-center gap-1.5">
                      <span>${sportIcon}</span>
                      <span>${court.name}</span>
                    </span>
                    <span class="text-[10px] font-bold px-2 py-0.5 rounded-md bg-slate-900 border border-white/10 uppercase text-emerald-400">${court.sport}</span>
                  </div>
                  <div class="text-xs text-slate-400 mt-1">${court.surface} • ${court.dimensions}</div>
                  <div class="flex items-center justify-between mt-2 pt-2 border-t border-white/[0.06]">
                    <span class="text-xs font-bold text-white">Base: ₹${court.baseHourlyRate}/hr</span>
                    <span class="text-[11px] font-bold text-purple-400">Odd-Hour: ₹${Math.round(court.baseHourlyRate * 0.65)}/hr</span>
                  </div>
                </button>
              `;
            }).join('')}
          </div>
        </div>

        <!-- 3. DATE SELECTOR STRIP (Next 7 Days, BookMyShow Style) -->
        <div class="space-y-2">
          <label class="block text-xs font-black uppercase tracking-wider text-slate-400">Select Date</label>
          <div class="flex gap-2.5 overflow-x-auto pb-2 scrollbar-none">
            ${[0, 1, 2, 3, 4, 5, 6].map(offset => {
              const dateStr = this.getFormattedDate(offset);
              const d = new Date();
              d.setDate(d.getDate() + offset);
              const dayName = offset === 0 ? 'Today' : offset === 1 ? 'Tomorrow' : d.toLocaleDateString('en-US', { weekday: 'short' });
              const dayNum = d.getDate();
              const monthName = d.toLocaleDateString('en-US', { month: 'short' });
              const isSelected = this.selectedDate === dateStr;

              return `
                <button data-date="${dateStr}" class="date-chip shrink-0 min-w-[90px] px-4 py-3 rounded-2xl border text-center transition-all duration-200 ${isSelected ? 'bg-gradient-to-tr from-emerald-500 to-teal-400 text-slate-950 border-emerald-400 font-black shadow-lg shadow-emerald-500/25 ring-2 ring-emerald-400/40' : 'bg-[#131B2E] border-white/10 text-slate-300 hover:border-slate-700 hover:bg-slate-800'}">
                  <div class="text-[10px] uppercase font-bold tracking-wider ${isSelected ? 'text-slate-900' : 'text-slate-400'}">${dayName}</div>
                  <div class="text-base font-black mt-0.5 leading-tight">${dayNum} ${monthName}</div>
                </button>
              `;
            }).join('')}
          </div>
        </div>

        <!-- 4. BOOKMYSHOW SHOWTIME TIME-OF-DAY TABS -->
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <label class="block text-xs font-black uppercase tracking-wider text-slate-400">Time-of-Day Filter</label>
            <span class="text-[11px] text-purple-400 font-bold">⚡ Odd-Hours automatically discounted</span>
          </div>

          <div class="flex flex-wrap gap-2 text-xs">
            <button data-band="all" class="time-band-tab px-3.5 py-2 rounded-xl border font-bold transition ${this.selectedTimeBand === 'all' ? 'bg-emerald-500 text-slate-950 border-emerald-400 shadow-md' : 'bg-[#131B2E] border-white/10 text-slate-300 hover:bg-slate-800'}">
              🕒 All Slots
            </button>
            <button data-band="morning" class="time-band-tab px-3.5 py-2 rounded-xl border font-bold transition flex items-center gap-1.5 ${this.selectedTimeBand === 'morning' ? 'bg-emerald-500 text-slate-950 border-emerald-400 shadow-md' : 'bg-[#131B2E] border-white/10 text-slate-300 hover:bg-slate-800'}">
              <span>🌅 Morning</span>
              <span class="text-[10px] text-slate-400 font-mono">(06:00 - 12:00)</span>
            </button>
            <button data-band="afternoon" class="time-band-tab px-3.5 py-2 rounded-xl border font-bold transition flex items-center gap-1.5 ${this.selectedTimeBand === 'afternoon' ? 'bg-purple-600 text-white border-purple-400 shadow-md shadow-purple-600/30' : 'bg-[#131B2E] border-white/10 text-slate-300 hover:bg-slate-800'}">
              <span>☀️ Afternoon</span>
              <span class="text-[10px] px-1.5 py-0.2 rounded bg-purple-500/30 text-purple-300 font-bold">35% OFF</span>
            </button>
            <button data-band="evening" class="time-band-tab px-3.5 py-2 rounded-xl border font-bold transition flex items-center gap-1.5 ${this.selectedTimeBand === 'evening' ? 'bg-rose-600 text-white border-rose-400 shadow-md' : 'bg-[#131B2E] border-white/10 text-slate-300 hover:bg-slate-800'}">
              <span>🌆 Prime Evening</span>
              <span class="text-[10px] px-1.5 py-0.2 rounded bg-rose-500/30 text-rose-300 font-bold">PEAK</span>
            </button>
            <button data-band="night" class="time-band-tab px-3.5 py-2 rounded-xl border font-bold transition flex items-center gap-1.5 ${this.selectedTimeBand === 'night' ? 'bg-purple-600 text-white border-purple-400 shadow-md' : 'bg-[#131B2E] border-white/10 text-slate-300 hover:bg-slate-800'}">
              <span>🌙 Night Owl</span>
              <span class="text-[10px] px-1.5 py-0.2 rounded bg-purple-500/30 text-purple-300 font-bold">25% OFF</span>
            </button>
          </div>
        </div>

        <!-- 5. SLOT STATUS LEGEND (BookMyShow Showtime Legend) -->
        <div class="flex flex-wrap items-center gap-4 text-xs text-slate-400 bg-[#131B2E] p-3 rounded-2xl border border-white/10">
          <div class="flex items-center gap-1.5">
            <span class="w-3 h-3 rounded-md bg-slate-900 border border-emerald-500/60"></span>
            <span>Available</span>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="w-3 h-3 rounded-md bg-purple-950 border border-purple-400"></span>
            <span>Odd-Hour (35% OFF)</span>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="w-3 h-3 rounded-md bg-rose-950 border border-rose-500"></span>
            <span>Prime Peak</span>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="w-3 h-3 rounded-md bg-emerald-500"></span>
            <span class="text-emerald-400 font-bold">Selected</span>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="w-3 h-3 rounded-md bg-amber-500/40 border border-amber-500"></span>
            <span>In Checkout</span>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="w-3 h-3 rounded-md bg-slate-900 border border-slate-800 opacity-50"></span>
            <span class="line-through text-slate-500">Booked</span>
          </div>
        </div>

        <!-- 6. TIME SLOTS GRID (BookMyShow Showtime Pills) -->
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <label class="block text-xs font-black uppercase tracking-wider text-slate-400">Select 1-Hour Time Slot</label>
            <span class="text-xs text-slate-400">Click any open slot to preview price</span>
          </div>

          <div id="slots-grid" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
            ${this.renderSlotsGrid()}
          </div>
        </div>

        <!-- 7. STICKY BOTTOM CHECKOUT SUMMARY BAR (BookMyShow Style) -->
        <div id="checkout-cta-bar" class="fixed bottom-0 left-0 right-0 z-40 bg-slate-950/95 backdrop-blur-xl border-t border-white/10 px-4 md:px-8 py-3.5 shadow-2xl">
          <div class="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
            
            <!-- Summary Info -->
            <div id="selected-slot-summary" class="text-xs md:text-sm text-slate-300 w-full sm:w-auto">
              ${this.selectedSlot ? this.renderSlotSummary() : `
                <div class="flex items-center gap-2 text-slate-400">
                  <span class="text-xl">👆</span>
                  <div>
                    <span class="font-bold text-white text-xs md:text-sm">No slot selected yet</span>
                    <p class="text-[11px] text-slate-400">Pick a time slot above to calculate dynamic pricing and reserve.</p>
                  </div>
                </div>
              `}
            </div>

            <!-- Proceed Button -->
            <button id="btn-lock-proceed" ${!this.selectedSlot ? 'disabled' : ''} class="w-full sm:w-auto px-8 py-3.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-400 hover:from-emerald-400 hover:to-teal-300 disabled:opacity-40 disabled:cursor-not-allowed text-slate-950 font-black text-xs md:text-sm shadow-xl shadow-emerald-500/20 transition active:scale-95 flex items-center justify-center gap-2 shrink-0">
              <span>🔒 Lock Slot & Proceed</span>
              <span class="text-[10px] bg-slate-950/20 px-2 py-0.5 rounded font-mono font-bold">5-min Lease</span>
            </button>

          </div>
        </div>

      </div>
    `;

    this.bindEvents(container);
  }

  renderSlotsGrid() {
    let slots = Object.values(this.availability.slots).sort((a, b) => a.slotTime.localeCompare(b.slotTime));

    // Filter by time-of-day band if selected
    if (this.selectedTimeBand !== 'all') {
      slots = slots.filter(s => {
        const hour = parseInt(s.slotTime.split(':')[0], 10);
        if (this.selectedTimeBand === 'morning') return hour >= 6 && hour < 12;
        if (this.selectedTimeBand === 'afternoon') return hour >= 12 && hour < 16;
        if (this.selectedTimeBand === 'evening') return hour >= 16 && hour < 22;
        if (this.selectedTimeBand === 'night') return hour >= 22 || hour < 6;
        return true;
      });
    }

    if (slots.length === 0) {
      return `
        <div class="col-span-full py-10 text-center bg-[#131B2E] rounded-2xl border border-white/10 text-slate-400 text-xs">
          No operating slots found for this time-of-day band. Try switching to "All Slots".
        </div>
      `;
    }

    return slots.map(slot => {
      const isSelected = this.selectedSlot === slot.slotTime;
      const isBooked = slot.status === 'BOOKED';
      const isReserved = slot.status === 'RESERVED';

      let slotClasses = "p-3.5 rounded-2xl border text-left transition-all duration-200 relative flex flex-col justify-between h-24 ";

      if (isSelected) {
        slotClasses += "bg-gradient-to-tr from-emerald-500 to-teal-400 text-slate-950 border-emerald-300 ring-2 ring-emerald-400 shadow-xl shadow-emerald-500/30 scale-[1.02]";
      } else if (isBooked) {
        slotClasses += "bg-slate-900/30 border-slate-800 text-slate-600 cursor-not-allowed opacity-40";
      } else if (isReserved) {
        slotClasses += "bg-amber-950/30 border-amber-500/50 text-amber-300 cursor-not-allowed";
      } else if (slot.isOddHour) {
        slotClasses += "bg-[#131B2E] hover:border-purple-400 border-purple-500/40 text-white shadow-md hover:shadow-purple-950/40";
      } else if (slot.isPeak) {
        slotClasses += "bg-[#131B2E] hover:border-rose-400 border-rose-500/40 text-white shadow-md";
      } else {
        slotClasses += "bg-[#131B2E] hover:border-emerald-500/60 border-white/10 text-white shadow-md";
      }

      return `
        <button data-slot="${slot.slotTime}" ${isBooked || isReserved ? 'disabled' : ''} class="slot-cell ${slotClasses}">
          <div class="flex items-center justify-between w-full">
            <span class="font-black text-sm ${isSelected ? 'text-slate-950' : 'text-white'}">${slot.slotTime}</span>
            ${slot.isOddHour && !isBooked ? `
              <span class="text-[9px] font-black px-1.5 py-0.5 rounded ${isSelected ? 'bg-slate-950 text-purple-300' : 'bg-purple-500/20 text-purple-300 border border-purple-500/40'}">
                -${slot.discountPct}%
              </span>
            ` : ''}
            ${slot.isPeak && !isBooked ? `
              <span class="text-[9px] font-bold px-1.5 py-0.5 rounded ${isSelected ? 'bg-slate-950 text-white' : 'bg-rose-500/20 text-rose-300 border border-rose-500/40'}">
                PEAK
              </span>
            ` : ''}
          </div>

          <div class="w-full mt-auto">
            ${isBooked ? `
              <span class="text-[10px] font-bold text-slate-500 block">Sold Out</span>
            ` : isReserved ? `
              <span class="text-[10px] font-bold text-amber-400 block flex items-center gap-1">
                <span class="w-1.5 h-1.5 rounded-full bg-amber-400 animate-ping"></span> In Checkout
              </span>
            ` : `
              <div class="flex items-baseline justify-between">
                <span class="text-xs font-black ${isSelected ? 'text-slate-950' : slot.isOddHour ? 'text-purple-300' : 'text-emerald-400'}">₹${slot.currentPrice}</span>
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
        <div class="flex flex-wrap items-center gap-2">
          <span class="font-black text-white text-sm md:text-base">${this.selectedCourt.name}</span>
          <span class="text-xs px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 font-mono font-bold border border-emerald-500/30">
            ${this.selectedDate} @ ${this.selectedSlot}
          </span>
        </div>
        <div class="text-xs text-slate-400 flex flex-wrap items-center gap-3">
          <span>Base Rate: ₹${pricing.baseRate}</span>
          ${pricing.isOddHour ? `<span class="text-purple-400 font-bold">⚡ Odd-Hour Discount: -₹${pricing.oddHourDiscountAmount} (${pricing.bandName})</span>` : ''}
          ${pricing.isPeak ? `<span class="text-rose-400 font-bold">Peak Pricing Active</span>` : ''}
          <span class="text-slate-300">Total: <strong class="text-emerald-400 font-black text-sm">₹${pricing.total}</strong></span>
        </div>
      </div>
    `;
  }

  bindEvents(container) {
    // Back to venues
    const btnBack = container.querySelector('#btn-back-venues');
    if (btnBack) {
      btnBack.addEventListener('click', () => {
        if (this.onBack) this.onBack();
      });
    }

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

    // Time-of-day tabs
    container.querySelectorAll('.time-band-tab').forEach(tab => {
      tab.addEventListener('click', (e) => {
        this.selectedTimeBand = e.currentTarget.getAttribute('data-band');
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
          let currentUser = UserService.getCurrentUser();
          if (!currentUser) {
            import('./UserAuthModal.js').then(({ UserAuthModal }) => {
              UserAuthModal.show({
                initialTab: 'signin',
                onUserChanged: (newUser) => {
                  if (newUser && btnLock) {
                    btnLock.click();
                  }
                }
              });
            });
            btnLock.disabled = false;
            btnLock.innerHTML = `<span>🔒 Lock Slot & Proceed (5-min Lease)</span> <span>→</span>`;
            return;
          }

          // Acquire Phase 1 5-minute atomic lock lease
          const lockResult = await ConcurrencyService.acquireLock({
            courtId: this.selectedCourt.courtId,
            date: this.selectedDate,
            slot: this.selectedSlot,
            userId: currentUser.uid
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
