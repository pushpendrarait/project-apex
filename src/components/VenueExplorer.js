/**
 * Project Apex - Venue Explorer Component
 * Location & Sport Discovery with Instant Filtering
 */

import { VenueService } from '../services/venueService.js';
import { AddVenueModal } from './AddVenueModal.js';
import { UserAuthModal } from './UserAuthModal.js';

export class VenueExplorer {
  constructor({ onSelectVenue, onUserChanged, onNavigate }) {
    this.onSelectVenue = onSelectVenue;
    this.onUserChanged = onUserChanged;
    this.onNavigate = onNavigate;
    this.currentCity = 'all';
    this.currentSport = 'all';
    this.searchQuery = '';
  }

  render(container) {
    container.innerHTML = `
      <div class="space-y-6">
        <!-- Top Banner & Filters -->
        <div class="bg-gradient-to-r from-emerald-900/90 via-slate-900 to-emerald-950 p-6 md:p-8 rounded-3xl border border-emerald-500/20 shadow-2xl relative overflow-hidden">
          <div class="absolute -right-10 -bottom-10 w-64 h-64 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none"></div>
          
          <div class="max-w-3xl space-y-4 relative z-10">
            <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold uppercase tracking-wider">
              <span class="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
              Thane & Mumbai Sports Platform
            </div>
            <h1 class="text-3xl md:text-5xl font-black text-white tracking-tight leading-tight">
              Book Turfs & Courts in <span class="text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 to-teal-300">Seconds</span>
            </h1>
            <p class="text-slate-300 text-sm md:text-base leading-relaxed">
              Explore floodlit box cricket, FIFA-standard football, indoor badminton, and pickleball courts. 
              Enjoy up to <strong class="text-emerald-400">35% OFF</strong> on dynamic odd-hour slots.
            </p>

            <!-- Landing Page Action Buttons: Sign In / Register as Player or Facility Owner -->
            <div class="flex flex-wrap items-center gap-3 pt-2">
              <button id="btn-hero-signin" class="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs border border-slate-700 shadow-md transition active:scale-95 flex items-center gap-2">
                <span>🔑 Sign In</span>
              </button>
              <button id="btn-hero-register-player" class="px-5 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-xs shadow-lg shadow-emerald-500/20 transition active:scale-95 flex items-center gap-2">
                <span>🏏 Register as Player</span>
                <span class="text-[10px] bg-slate-950/20 px-1.5 py-0.5 rounded font-mono font-bold">+100 Pts</span>
              </button>
              <button id="btn-hero-register-owner" class="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-xs shadow-lg shadow-indigo-600/20 transition active:scale-95 flex items-center gap-2">
                <span>🏢 Register as Facility Owner</span>
              </button>
            </div>
          </div>

          <!-- Filter Controls -->
          <div class="mt-6 grid grid-cols-1 md:grid-cols-3 gap-3">
            <!-- City Selector -->
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-1.5">City / Zone</label>
              <select id="filter-city" class="w-full bg-slate-800/90 border border-slate-700 hover:border-emerald-500 text-white rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none transition">
                <option value="all">All Locations (Thane & Mumbai)</option>
                <option value="thane" selected>Thane (Ghodbunder, Majiwada)</option>
                <option value="mumbai">Mumbai (Bandra, Powai, Andheri)</option>
                <option value="navi mumbai">Navi Mumbai (Vashi, Nerul)</option>
              </select>
            </div>

            <!-- Sport Selector -->
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-1.5">Sport Category</label>
              <select id="filter-sport" class="w-full bg-slate-800/90 border border-slate-700 hover:border-emerald-500 text-white rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none transition">
                <option value="all">All Sports</option>
                <option value="cricket">Box Cricket Turf</option>
                <option value="football">5-a-side Football</option>
                <option value="badminton">Indoor Badminton</option>
                <option value="pickleball">Pickleball Courts</option>
                <option value="squash">Squash</option>
              </select>
            </div>

            <!-- Search input -->
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-1.5">Search Venue / Area</label>
              <div class="relative">
                <input id="filter-search" type="text" placeholder="e.g. Ghodbunder, Bandra..." class="w-full bg-slate-800/90 border border-slate-700 hover:border-emerald-500 text-white rounded-xl pl-10 pr-4 py-3 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
                <svg class="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
              </div>
            </div>
          </div>
        </div>

        <!-- Odd Hour Promotional Ticker -->
        <div class="bg-emerald-950/40 border border-emerald-500/30 rounded-2xl p-4 flex flex-col md:flex-row items-center justify-between gap-3 text-xs md:text-sm text-emerald-200">
          <div class="flex items-center gap-2">
            <span class="p-1.5 bg-emerald-500/20 rounded-lg text-emerald-400 font-bold">⚡ Dynamic Deals</span>
            <span>Weekday Afternoon (11 AM - 4 PM): <strong>Flat 35% OFF</strong> | Midnight Turf (11 PM - 2 AM): <strong>25% OFF</strong></span>
          </div>
          <span class="text-xs bg-emerald-500/20 px-3 py-1 rounded-full border border-emerald-500/40 font-mono text-emerald-300 font-bold">Code: THANE50</span>
        </div>

        <!-- Venue Cards Grid -->
        <div id="venues-grid" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"></div>
      </div>
    `;

    this.bindEvents(container);
    this.updateGrid(container);
  }

  bindEvents(container) {
    const citySelect = container.querySelector('#filter-city');
    const sportSelect = container.querySelector('#filter-sport');
    const searchInput = container.querySelector('#filter-search');

    const btnSignin = container.querySelector('#btn-hero-signin');
    if (btnSignin) {
      btnSignin.addEventListener('click', () => {
        UserAuthModal.show({
          initialTab: 'signin',
          onUserChanged: (user) => {
            if (this.onUserChanged) this.onUserChanged(user);
            this.updateGrid(container);
          },
          onNavigate: (view) => {
            if (this.onNavigate) this.onNavigate(view);
          }
        });
      });
    }

    const btnRegisterPlayer = container.querySelector('#btn-hero-register-player');
    if (btnRegisterPlayer) {
      btnRegisterPlayer.addEventListener('click', () => {
        UserAuthModal.show({
          initialTab: 'player',
          onUserChanged: (user) => {
            if (this.onUserChanged) this.onUserChanged(user);
            this.updateGrid(container);
          }
        });
      });
    }

    const btnRegisterOwner = container.querySelector('#btn-hero-register-owner');
    if (btnRegisterOwner) {
      btnRegisterOwner.addEventListener('click', () => {
        UserAuthModal.show({
          initialTab: 'owner',
          onUserChanged: (user) => {
            if (this.onUserChanged) this.onUserChanged(user);
          },
          onNavigate: (view) => {
            if (this.onNavigate) this.onNavigate(view);
          }
        });
      });
    }

    const btnAddVenue = container.querySelector('#btn-banner-add-venue');
    if (btnAddVenue) {
      btnAddVenue.addEventListener('click', () => {
        AddVenueModal.show({
          onVenueAdded: () => {
            this.updateGrid(container);
          }
        });
      });
    }

    citySelect.addEventListener('change', (e) => {
      this.currentCity = e.target.value;
      this.updateGrid(container);
    });

    sportSelect.addEventListener('change', (e) => {
      this.currentSport = e.target.value;
      this.updateGrid(container);
    });

    searchInput.addEventListener('input', (e) => {
      this.searchQuery = e.target.value.toLowerCase();
      this.updateGrid(container);
    });
  }

  updateGrid(container) {
    const grid = container.querySelector('#venues-grid');
    const venues = VenueService.getVenues({
      city: this.currentCity,
      sport: this.currentSport
    }).filter(v => {
      if (!this.searchQuery) return true;
      return v.name.toLowerCase().includes(this.searchQuery) ||
             v.area.toLowerCase().includes(this.searchQuery) ||
             v.sports.some(s => s.toLowerCase().includes(this.searchQuery));
    });

    if (venues.length === 0) {
      grid.innerHTML = `
        <div class="col-span-full text-center py-16 bg-slate-900/40 rounded-3xl border border-slate-800">
          <div class="text-5xl mb-3">🏏</div>
          <h3 class="text-xl font-bold text-white mb-1">No Venues Found</h3>
          <p class="text-slate-400 text-sm">Try relaxing your location or sport filters.</p>
        </div>
      `;
      return;
    }

    grid.innerHTML = venues.map(venue => {
      const courts = VenueService.getCourtsForVenue(venue.venueId);
      return `
        <div class="bg-slate-900 rounded-3xl border border-slate-800 hover:border-emerald-500/50 transition-all duration-300 overflow-hidden group flex flex-col justify-between shadow-xl">
          <!-- Venue Image -->
          <div class="relative h-52 overflow-hidden">
            <img src="${venue.images[0]}" alt="${venue.name}" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
            <div class="absolute inset-0 bg-gradient-to-t from-slate-950 via-transparent to-black/30"></div>
            
            <!-- Badges -->
            <div class="absolute top-3 left-3 flex flex-wrap gap-1.5">
              <span class="px-2.5 py-1 rounded-full text-xs font-bold bg-slate-950/80 backdrop-blur-md text-emerald-400 border border-emerald-500/30">
                ${venue.city}
              </span>
              ${venue.featured ? `
                <span class="px-2.5 py-1 rounded-full text-xs font-bold bg-amber-500/90 text-slate-950">
                  Featured
                </span>
              ` : ''}
            </div>

            <div class="absolute bottom-3 right-3 bg-slate-950/80 backdrop-blur-md px-2.5 py-1 rounded-xl text-xs font-bold text-amber-300 flex items-center gap-1 border border-amber-500/30">
              ⭐ ${venue.rating} (${venue.reviewsCount})
            </div>
          </div>

          <!-- Card Body -->
          <div class="p-5 flex-1 flex flex-col justify-between space-y-4">
            <div>
              <div class="flex items-center justify-between">
                <h3 class="text-lg font-black text-white group-hover:text-emerald-400 transition">${venue.name}</h3>
              </div>
              <p class="text-xs text-slate-400 mt-1 flex items-center gap-1">
                <svg class="w-3.5 h-3.5 text-emerald-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path></svg>
                ${venue.area}, ${venue.city}
              </p>

              <!-- Sports tags -->
              <div class="flex flex-wrap gap-1.5 mt-3">
                ${venue.sports.map(s => `
                  <span class="px-2 py-0.5 rounded-lg text-[11px] font-semibold bg-slate-800 text-slate-300 capitalize">
                    ${s}
                  </span>
                `).join('')}
              </div>

              <!-- Amenities list -->
              <div class="flex flex-wrap gap-2 text-[11px] text-slate-400 mt-3 pt-3 border-t border-slate-800">
                ${venue.amenities.slice(0, 3).map(a => `<span>✓ ${a}</span>`).join('')}
                ${venue.amenities.length > 3 ? `<span class="text-emerald-400 font-semibold">+${venue.amenities.length - 3} more</span>` : ''}
              </div>
            </div>

            <!-- Pricing & Action -->
            <div class="pt-4 border-t border-slate-800 flex items-center justify-between mt-auto">
              <div>
                <span class="text-[11px] uppercase font-bold text-slate-400 block">Starting from</span>
                <span class="text-lg font-black text-white">₹${venue.startingPrice}<span class="text-xs font-normal text-slate-400">/hr</span></span>
              </div>
              <button data-venue-id="${venue.venueId}" class="btn-select-venue px-5 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm shadow-lg shadow-emerald-500/20 transition active:scale-95">
                Check Slots →
              </button>
            </div>
          </div>
        </div>
      `;
    }).join('');

    // Attach click handlers
    grid.querySelectorAll('.btn-select-venue').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const venueId = e.currentTarget.getAttribute('data-venue-id');
        const venue = VenueService.getVenueById(venueId);
        if (this.onSelectVenue && venue) {
          this.onSelectVenue(venue);
        }
      });
    });
  }
}
