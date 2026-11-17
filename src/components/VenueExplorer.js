/**
 * Project Apex - Venue Explorer Component
 * BookMyShow + Snabbit & Playo Inspired Sports Turf Discovery Experience
 * High-conversion dark entertainment UI, hero banner carousel, sports categories ribbon,
 * quick filter pills, and rich venue cards with rating badges & odd-hour tags.
 */

import { VenueService } from '../services/venueService.js';
import { AddVenueModal } from './AddVenueModal.js';
import { UserAuthModal } from './UserAuthModal.js';
import { ScenarioRunnerModal } from './ScenarioRunnerModal.js';

export class VenueExplorer {
  constructor({ onSelectVenue, onUserChanged, onNavigate }) {
    this.onSelectVenue = onSelectVenue;
    this.onUserChanged = onUserChanged;
    this.onNavigate = onNavigate;
    this.currentCity = 'all';
    this.currentSport = 'all';
    this.searchQuery = '';
    
    // Quick filters state
    this.activeQuickFilters = {
      oddHourOnly: false,
      topRatedOnly: false,
      floodlightsOnly: false,
      freeParkingOnly: false,
      sortBy: 'featured' // 'featured', 'priceAsc', 'ratingDesc'
    };

    // Hero Carousel state
    this.currentSlide = 0;
    this.carouselTimer = null;
    this.slides = [
      {
        tag: '🌙 MIDNIGHT TURF LEAGUE • 11 PM - 5 AM',
        title: 'Midnight Box Cricket & Turf Football Under The Stars',
        highlight: 'Flat 35% OFF on odd hours across Thane, Bandra & Vashi',
        description: 'Compete under FIFA-grade floodlights with zero humidity and cool night breeze. Ideal for squad tournaments and weekend night owls.',
        code: 'MIDNIGHT35',
        gradient: 'from-purple-950/90 via-slate-900 to-[#090D16]',
        borderGlow: 'border-purple-500/30',
        actionLabel: '⚡ Explore Midnight Deals',
        actionFilter: 'oddHourOnly',
        bgImage: 'https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=1400&q=80'
      },
      {
        tag: '☀️ AFTERNOON GRIND SPECIAL • 12 PM - 4 PM',
        title: 'Beat The Rush: Premium Turfs Starting At Just ₹650/hr',
        highlight: 'Flat 35% OFF on weekday slots across all arenas',
        description: 'Prime box cricket pitches & indoor badminton courts at student and corporate friendly discount rates.',
        code: 'AFTERNOON35',
        gradient: 'from-amber-950/80 via-slate-900 to-[#090D16]',
        borderGlow: 'border-amber-500/30',
        actionLabel: '🏏 Book Box Cricket',
        actionSport: 'cricket',
        bgImage: 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=1400&q=80'
      },
      {
        tag: '⚡ PAPERLESS GATE PASS • ZERO WAITING',
        title: 'Cryptographic QR Gate Passes & Instant Turnstile Check-In',
        highlight: '100% Serverless Offline Verification in under 200ms',
        description: 'Say goodbye to queues and paper receipts. Dynamic digital QR passes with instant owner validation and fair 3-tier cancellation guarantees.',
        code: 'APEX CRYPTO PASS',
        gradient: 'from-emerald-950/90 via-slate-900 to-[#090D16]',
        borderGlow: 'border-emerald-500/30',
        actionLabel: '🎟️ View My Passes',
        actionNav: 'bookings',
        bgImage: 'https://images.unsplash.com/photo-1511193311914-0346f16efe90?auto=format&fit=crop&w=1400&q=80'
      }
    ];
  }

  setCity(city) {
    this.currentCity = city;
    const citySelect = document.getElementById('filter-city');
    if (citySelect) citySelect.value = city;
    const container = document.getElementById('app-root');
    if (container) this.updateGrid(container);
  }

  setSport(sport) {
    this.currentSport = sport;
    const container = document.getElementById('app-root');
    if (container) {
      this.updateCategoryRibbon(container);
      this.updateGrid(container);
    }
  }

  setSearch(query) {
    this.searchQuery = (query || '').toLowerCase().trim();
    const searchInput = document.getElementById('filter-search');
    if (searchInput) searchInput.value = query;
    const container = document.getElementById('app-root');
    if (container) this.updateGrid(container);
  }

  render(container) {
    clearInterval(this.carouselTimer);

    container.innerHTML = `
      <div class="space-y-7 animate-fade-in">

        <!-- 1. BOOKMYSHOW STYLE HERO PROMOTIONAL CAROUSEL -->
        <div id="hero-carousel-container" class="relative rounded-3xl overflow-hidden border border-white/10 shadow-2xl group">
          <div id="hero-carousel-track" class="relative min-h-[300px] md:min-h-[340px] flex transition-all duration-700 ease-out">
            ${this.renderCarouselSlides()}
          </div>

          <!-- Carousel Controls: Previous / Next Arrows -->
          <button id="carousel-btn-prev" class="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-slate-950/70 hover:bg-slate-900 border border-white/10 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition shadow-xl z-20" title="Previous Slide">
            ‹
          </button>
          <button id="carousel-btn-next" class="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-slate-950/70 hover:bg-slate-900 border border-white/10 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition shadow-xl z-20" title="Next Slide">
            ›
          </button>

          <!-- Carousel Indicators / Pagination Dots -->
          <div class="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-2 z-20">
            ${this.slides.map((_, i) => `
              <button data-slide-index="${i}" class="carousel-dot h-2 rounded-full transition-all duration-300 ${i === this.currentSlide ? 'w-8 bg-emerald-400' : 'w-2 bg-white/40 hover:bg-white/80'}"></button>
            `).join('')}
          </div>
        </div>

        <!-- 2. QUICK CALL-TO-ACTION ONBOARDING BAR -->
        <div class="bg-gradient-to-r from-slate-900 via-[#131B2E] to-slate-900 p-4 rounded-2xl border border-white/[0.08] flex flex-col md:flex-row items-center justify-between gap-4 shadow-lg">
          <div class="flex items-center gap-3 text-xs md:text-sm">
            <span class="w-8 h-8 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-black text-sm shrink-0">⚡</span>
            <div>
              <span class="font-bold text-white">Join Thane & Mumbai's Premier Sports Network</span>
              <p class="text-slate-400 text-xs">Instant booking, 100 welcome loyalty points, and zero double-booking guarantee.</p>
            </div>
          </div>
          <div class="flex flex-wrap items-center gap-2 w-full md:w-auto shrink-0">
            <button id="btn-hero-signin" class="flex-1 md:flex-none px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs border border-white/10 shadow-sm transition active:scale-95 flex items-center justify-center gap-1.5">
              <span>🔑 Sign In</span>
            </button>
            <button id="btn-hero-register-player" class="flex-1 md:flex-none px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-400 hover:from-emerald-400 hover:to-teal-300 text-slate-950 font-black text-xs shadow-md shadow-emerald-500/20 transition active:scale-95 flex items-center justify-center gap-1.5">
              <span>🏏 Register Player</span>
              <span class="text-[9px] bg-slate-950/20 px-1.5 py-0.5 rounded font-mono font-bold">+100 Pts</span>
            </button>
            <button id="btn-hero-register-owner" class="flex-1 md:flex-none px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-xs shadow-md shadow-indigo-600/20 transition active:scale-95 flex items-center justify-center gap-1.5">
              <span>🏢 Facility Owner</span>
            </button>
          </div>
        </div>

        <!-- 3. SNABBIT / PLAYO STYLE SPORTS & CATEGORIES RIBBON -->
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <span class="text-xs font-black uppercase tracking-wider text-slate-400">Categories</span>
              <span class="text-[10px] px-2 py-0.5 rounded-full bg-slate-800 text-emerald-400 font-mono">Quick Filter</span>
            </div>
            <span class="text-xs text-slate-400 hidden sm:inline">Scroll to explore all sports →</span>
          </div>

          <div id="sport-ribbon-container" class="flex gap-2.5 overflow-x-auto pb-1.5 scrollbar-none">
            ${this.renderSportChips()}
          </div>
        </div>

        <!-- 4. QUICK FILTERS, LOCATION & SEARCH BAR -->
        <div class="bg-[#131B2E] p-4 md:p-5 rounded-3xl border border-white/10 shadow-xl space-y-4">
          
          <!-- Top Row: City Selector, Search & Reset -->
          <div class="grid grid-cols-1 md:grid-cols-12 gap-3 items-center">
            
            <!-- City Dropdown -->
            <div class="md:col-span-3">
              <label class="block text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-1">Region / City</label>
              <div class="relative">
                <select id="filter-city" class="w-full bg-[#090D16] border border-white/10 hover:border-emerald-500 text-white rounded-xl px-3.5 py-2.5 text-xs font-semibold focus:ring-2 focus:ring-emerald-500 focus:outline-none transition appearance-none cursor-pointer">
                  <option value="all" ${this.currentCity === 'all' ? 'selected' : ''}>🌐 All Cities (Thane & Mumbai)</option>
                  <option value="thane" ${this.currentCity === 'thane' ? 'selected' : ''}>📍 Thane (Ghodbunder, Majiwada)</option>
                  <option value="mumbai" ${this.currentCity === 'mumbai' ? 'selected' : ''}>📍 Mumbai (Bandra, Powai, BKC)</option>
                  <option value="navi mumbai" ${this.currentCity === 'navi mumbai' ? 'selected' : ''}>📍 Navi Mumbai (Vashi, Nerul)</option>
                </select>
                <span class="absolute right-3.5 top-3 text-[10px] text-slate-400 pointer-events-none">▾</span>
              </div>
            </div>

            <!-- Global Omni-Search Input -->
            <div class="md:col-span-6">
              <label class="block text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-1">Search Turfs, Sports or Localities</label>
              <div class="relative">
                <input id="filter-search" type="text" placeholder="e.g. Box Cricket, Ghodbunder Rd, Bandra, Powai..." value="${this.searchQuery}" class="w-full bg-[#090D16] border border-white/10 hover:border-emerald-500 text-white placeholder-slate-500 rounded-xl pl-9 pr-4 py-2.5 text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
                <span class="text-slate-400 absolute left-3 top-2.5 text-xs">🔍</span>
              </div>
            </div>

            <!-- Sort By Selector -->
            <div class="md:col-span-3">
              <label class="block text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-1">Sort Venues</label>
              <div class="relative">
                <select id="filter-sort" class="w-full bg-[#090D16] border border-white/10 hover:border-emerald-500 text-white rounded-xl px-3.5 py-2.5 text-xs font-semibold focus:ring-2 focus:ring-emerald-500 focus:outline-none transition appearance-none cursor-pointer">
                  <option value="featured">⭐ Featured & Popular</option>
                  <option value="priceAsc">💰 Price: Low to High</option>
                  <option value="ratingDesc">🏆 Highest Rated (4.8+)</option>
                </select>
                <span class="absolute right-3.5 top-3 text-[10px] text-slate-400 pointer-events-none">▾</span>
              </div>
            </div>

          </div>

          <!-- Bottom Row: Snabbit Filter Pills Strip -->
          <div class="flex flex-wrap items-center gap-2 pt-2 border-t border-white/[0.06] text-xs">
            <span class="text-slate-400 text-[11px] font-bold uppercase tracking-wide mr-1">Quick Tags:</span>

            <button id="pill-odd-hour" class="filter-pill px-3 py-1.5 rounded-full border text-xs font-bold transition flex items-center gap-1.5 ${this.activeQuickFilters.oddHourOnly ? 'bg-purple-600 text-white border-purple-400 shadow-lg shadow-purple-600/30' : 'bg-slate-900 border-white/10 text-slate-300 hover:text-white hover:bg-slate-800'}">
              <span>⚡</span>
              <span>Odd-Hour Deals (35% OFF)</span>
            </button>

            <button id="pill-top-rated" class="filter-pill px-3 py-1.5 rounded-full border text-xs font-bold transition flex items-center gap-1.5 ${this.activeQuickFilters.topRatedOnly ? 'bg-amber-500 text-slate-950 border-amber-400 shadow-lg shadow-amber-500/30' : 'bg-slate-900 border-white/10 text-slate-300 hover:text-white hover:bg-slate-800'}">
              <span>⭐</span>
              <span>Top Rated (4.8+)</span>
            </button>

            <button id="pill-floodlights" class="filter-pill px-3 py-1.5 rounded-full border text-xs font-bold transition flex items-center gap-1.5 ${this.activeQuickFilters.floodlightsOnly ? 'bg-emerald-500 text-slate-950 border-emerald-400 shadow-lg shadow-emerald-500/30' : 'bg-slate-900 border-white/10 text-slate-300 hover:text-white hover:bg-slate-800'}">
              <span>💡</span>
              <span>Pro Floodlights</span>
            </button>

            <button id="pill-parking" class="filter-pill px-3 py-1.5 rounded-full border text-xs font-bold transition flex items-center gap-1.5 ${this.activeQuickFilters.freeParkingOnly ? 'bg-emerald-500 text-slate-950 border-emerald-400 shadow-lg shadow-emerald-500/30' : 'bg-slate-900 border-white/10 text-slate-300 hover:text-white hover:bg-slate-800'}">
              <span>🅿️</span>
              <span>Free Parking</span>
            </button>

            <button id="pill-reset-filters" class="hidden px-2.5 py-1.5 rounded-full text-[11px] font-bold text-rose-400 hover:text-rose-300 hover:bg-rose-950/40 transition">
              ✕ Reset Filters
            </button>
          </div>

        </div>

        <!-- 5. DYNAMIC ODD-HOUR PROMOTIONAL BANNER TICKER -->
        <div class="bg-gradient-to-r from-purple-950/60 via-slate-900 to-emerald-950/60 border border-purple-500/30 rounded-2xl p-3.5 flex flex-col md:flex-row items-center justify-between gap-3 text-xs text-slate-200">
          <div class="flex items-center gap-2">
            <span class="p-1 px-2 bg-purple-500/20 text-purple-300 rounded-lg font-black text-xs border border-purple-500/40">⚡ ODD-HOURS</span>
            <span>Weekday Afternoon (11 AM - 4 PM): <strong class="text-emerald-400">Flat 35% OFF</strong> | Midnight Turf (11 PM - 3 AM): <strong class="text-purple-300">25% OFF</strong></span>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-[11px] text-slate-400">Coupon:</span>
            <span class="bg-slate-950 px-2.5 py-1 rounded-md border border-white/10 font-mono text-emerald-400 font-bold">THANE50</span>
          </div>
        </div>

        <!-- 6. VENUES GRID CONTAINER -->
        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <h2 class="text-xl font-black text-white">Available Sports Turfs & Arenas</h2>
              <span id="venue-count-badge" class="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-emerald-400 font-mono font-bold">...</span>
            </div>
          </div>

          <div id="venues-grid" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"></div>
        </div>

      </div>
    `;

    this.bindEvents(container);
    this.startCarousel(container);
    this.updateGrid(container);
  }

  renderCarouselSlides() {
    return this.slides.map((slide, idx) => `
      <div class="carousel-slide min-w-full flex-shrink-0 relative overflow-hidden p-6 md:p-10 flex flex-col justify-between" style="background-image: linear-gradient(to right, rgba(9, 13, 22, 0.95) 45%, rgba(9, 13, 22, 0.5) 80%, rgba(9, 13, 22, 0.95)), url('${slide.bgImage}'); background-size: cover; background-position: center;">
        
        <!-- Top Tags -->
        <div class="flex items-center justify-between z-10">
          <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-950/80 backdrop-blur-md border ${slide.borderGlow} text-xs font-bold text-white shadow-lg">
            <span class="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
            ${slide.tag}
          </div>
          <span class="text-xs font-mono font-black bg-slate-950/80 border border-white/10 px-3 py-1 rounded-full text-emerald-400">
            ${slide.code}
          </span>
        </div>

        <!-- Main Slide Content -->
        <div class="max-w-2xl my-4 space-y-2.5 z-10">
          <h2 class="text-2xl md:text-4xl font-black text-white tracking-tight leading-tight">
            ${slide.title}
          </h2>
          <p class="text-sm md:text-base text-emerald-300 font-semibold">
            ${slide.highlight}
          </p>
          <p class="text-xs md:text-sm text-slate-300 line-clamp-2">
            ${slide.description}
          </p>
        </div>

        <!-- Action Button on Slide -->
        <div class="z-10">
          <button data-slide-action="${idx}" class="btn-slide-cta px-6 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-400 hover:from-emerald-400 hover:to-teal-300 text-slate-950 font-black text-xs shadow-lg shadow-emerald-500/30 transition active:scale-95 inline-flex items-center gap-2">
            <span>${slide.actionLabel}</span>
            <span>→</span>
          </button>
        </div>

      </div>
    `).join('');
  }

  renderSportChips() {
    const sportsData = [
      { id: 'all', label: 'All Sports', icon: '🌟', count: 'Explore All' },
      { id: 'cricket', label: 'Box Cricket', icon: '🏏', count: '8 Turfs' },
      { id: 'football', label: 'Turf Football', icon: '⚽', count: '6 Pitches' },
      { id: 'badminton', label: 'Badminton', icon: '🏸', count: '5 Courts' },
      { id: 'pickleball', label: 'Pickleball', icon: '🏓', count: '4 Courts' },
      { id: 'tennis', label: 'Tennis', icon: '🎾', count: '3 Courts' },
      { id: 'squash', label: 'Squash', icon: '🏊', count: '2 Arenas' }
    ];

    return sportsData.map(sport => {
      const isActive = this.currentSport === sport.id;
      return `
        <button data-sport-id="${sport.id}" class="sport-chip shrink-0 px-4 py-3 rounded-2xl border transition-all duration-200 flex items-center gap-3 ${isActive ? 'bg-emerald-500/15 border-emerald-500 ring-2 ring-emerald-500/30 text-white shadow-lg shadow-emerald-950' : 'bg-slate-900 border-white/10 text-slate-300 hover:border-slate-700 hover:bg-slate-800'}">
          <div class="w-9 h-9 rounded-xl flex items-center justify-center text-lg ${isActive ? 'bg-emerald-500 text-slate-950' : 'bg-slate-800 text-slate-200'}">
            ${sport.icon}
          </div>
          <div class="text-left">
            <div class="text-xs font-black leading-tight ${isActive ? 'text-white' : 'text-slate-200'}">${sport.label}</div>
            <div class="text-[10px] text-slate-400 font-semibold">${sport.count}</div>
          </div>
        </button>
      `;
    }).join('');
  }

  updateCategoryRibbon(container) {
    const ribbonContainer = container.querySelector('#sport-ribbon-container');
    if (ribbonContainer) {
      ribbonContainer.innerHTML = this.renderSportChips();
      this.bindCategoryEvents(container);
    }
  }

  startCarousel(container) {
    const track = container.querySelector('#hero-carousel-track');
    const dots = container.querySelectorAll('.carousel-dot');

    const goToSlide = (index) => {
      this.currentSlide = (index + this.slides.length) % this.slides.length;
      if (track) {
        track.style.transform = `translateX(-${this.currentSlide * 100}%)`;
      }
      dots.forEach((dot, i) => {
        if (i === this.currentSlide) {
          dot.className = 'carousel-dot h-2 rounded-full transition-all duration-300 w-8 bg-emerald-400';
        } else {
          dot.className = 'carousel-dot h-2 rounded-full transition-all duration-300 w-2 bg-white/40 hover:bg-white/80';
        }
      });
    };

    const nextSlide = () => goToSlide(this.currentSlide + 1);
    const prevSlide = () => goToSlide(this.currentSlide - 1);

    const prevBtn = container.querySelector('#carousel-btn-prev');
    const nextBtn = container.querySelector('#carousel-btn-next');
    if (prevBtn) prevBtn.addEventListener('click', prevSlide);
    if (nextBtn) nextBtn.addEventListener('click', nextSlide);

    dots.forEach(dot => {
      dot.addEventListener('click', (e) => {
        const idx = Number(e.currentTarget.getAttribute('data-slide-index'));
        goToSlide(idx);
      });
    });

    // Auto-advance every 6 seconds
    this.carouselTimer = setInterval(nextSlide, 6000);

    const carouselContainer = container.querySelector('#hero-carousel-container');
    if (carouselContainer) {
      carouselContainer.addEventListener('mouseenter', () => clearInterval(this.carouselTimer));
      carouselContainer.addEventListener('mouseleave', () => {
        clearInterval(this.carouselTimer);
        this.carouselTimer = setInterval(nextSlide, 6000);
      });
    }

    // Slide CTA Actions
    container.querySelectorAll('.btn-slide-cta').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const idx = Number(e.currentTarget.getAttribute('data-slide-action'));
        const slide = this.slides[idx];
        if (slide.actionFilter === 'oddHourOnly') {
          this.activeQuickFilters.oddHourOnly = true;
          this.updateGrid(container);
        } else if (slide.actionSport) {
          this.setSport(slide.actionSport);
        } else if (slide.actionNav) {
          if (this.onNavigate) this.onNavigate(slide.actionNav);
        }
      });
    });
  }

  bindCategoryEvents(container) {
    container.querySelectorAll('.sport-chip').forEach(chip => {
      chip.addEventListener('click', (e) => {
        const sportId = e.currentTarget.getAttribute('data-sport-id');
        this.setSport(sportId);
      });
    });
  }

  bindEvents(container) {
    this.bindCategoryEvents(container);

    const citySelect = container.querySelector('#filter-city');
    const searchInput = container.querySelector('#filter-search');
    const sortSelect = container.querySelector('#filter-sort');

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
            this.updateGrid(container);
          },
          onNavigate: (view) => {
            if (this.onNavigate) this.onNavigate(view);
          }
        });
      });
    }

    if (citySelect) {
      citySelect.addEventListener('change', (e) => {
        this.currentCity = e.target.value;
        const locLabel = document.getElementById('current-location-label');
        if (locLabel) {
          locLabel.textContent = this.currentCity === 'all' ? 'All Cities' : this.currentCity.charAt(0).toUpperCase() + this.currentCity.slice(1);
        }
        this.updateGrid(container);
      });
    }

    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        this.searchQuery = e.target.value.toLowerCase().trim();
        this.updateGrid(container);
      });
    }

    if (sortSelect) {
      sortSelect.addEventListener('change', (e) => {
        this.activeQuickFilters.sortBy = e.target.value;
        this.updateGrid(container);
      });
    }

    // Quick filter pills toggle
    const pillOddHour = container.querySelector('#pill-odd-hour');
    if (pillOddHour) {
      pillOddHour.addEventListener('click', () => {
        this.activeQuickFilters.oddHourOnly = !this.activeQuickFilters.oddHourOnly;
        pillOddHour.className = this.activeQuickFilters.oddHourOnly
          ? 'filter-pill px-3 py-1.5 rounded-full border text-xs font-bold transition flex items-center gap-1.5 bg-purple-600 text-white border-purple-400 shadow-lg shadow-purple-600/30'
          : 'filter-pill px-3 py-1.5 rounded-full border text-xs font-bold transition flex items-center gap-1.5 bg-slate-900 border-white/10 text-slate-300 hover:text-white hover:bg-slate-800';
        this.updateGrid(container);
      });
    }

    const pillTopRated = container.querySelector('#pill-top-rated');
    if (pillTopRated) {
      pillTopRated.addEventListener('click', () => {
        this.activeQuickFilters.topRatedOnly = !this.activeQuickFilters.topRatedOnly;
        pillTopRated.className = this.activeQuickFilters.topRatedOnly
          ? 'filter-pill px-3 py-1.5 rounded-full border text-xs font-bold transition flex items-center gap-1.5 bg-amber-500 text-slate-950 border-amber-400 shadow-lg shadow-amber-500/30'
          : 'filter-pill px-3 py-1.5 rounded-full border text-xs font-bold transition flex items-center gap-1.5 bg-slate-900 border-white/10 text-slate-300 hover:text-white hover:bg-slate-800';
        this.updateGrid(container);
      });
    }

    const pillFloodlights = container.querySelector('#pill-floodlights');
    if (pillFloodlights) {
      pillFloodlights.addEventListener('click', () => {
        this.activeQuickFilters.floodlightsOnly = !this.activeQuickFilters.floodlightsOnly;
        pillFloodlights.className = this.activeQuickFilters.floodlightsOnly
          ? 'filter-pill px-3 py-1.5 rounded-full border text-xs font-bold transition flex items-center gap-1.5 bg-emerald-500 text-slate-950 border-emerald-400 shadow-lg shadow-emerald-500/30'
          : 'filter-pill px-3 py-1.5 rounded-full border text-xs font-bold transition flex items-center gap-1.5 bg-slate-900 border-white/10 text-slate-300 hover:text-white hover:bg-slate-800';
        this.updateGrid(container);
      });
    }

    const pillParking = container.querySelector('#pill-parking');
    if (pillParking) {
      pillParking.addEventListener('click', () => {
        this.activeQuickFilters.freeParkingOnly = !this.activeQuickFilters.freeParkingOnly;
        pillParking.className = this.activeQuickFilters.freeParkingOnly
          ? 'filter-pill px-3 py-1.5 rounded-full border text-xs font-bold transition flex items-center gap-1.5 bg-emerald-500 text-slate-950 border-emerald-400 shadow-lg shadow-emerald-500/30'
          : 'filter-pill px-3 py-1.5 rounded-full border text-xs font-bold transition flex items-center gap-1.5 bg-slate-900 border-white/10 text-slate-300 hover:text-white hover:bg-slate-800';
        this.updateGrid(container);
      });
    }

    const pillReset = container.querySelector('#pill-reset-filters');
    if (pillReset) {
      pillReset.addEventListener('click', () => {
        this.activeQuickFilters = {
          oddHourOnly: false,
          topRatedOnly: false,
          floodlightsOnly: false,
          freeParkingOnly: false,
          sortBy: 'featured'
        };
        this.currentSport = 'all';
        this.searchQuery = '';
        if (searchInput) searchInput.value = '';
        this.updateCategoryRibbon(container);
        this.updateGrid(container);
      });
    }
  }

  updateGrid(container) {
    const grid = container.querySelector('#venues-grid');
    const countBadge = container.querySelector('#venue-count-badge');
    const pillReset = container.querySelector('#pill-reset-filters');

    if (!grid) return;

    let venues = VenueService.getVenues({
      city: this.currentCity,
      sport: this.currentSport
    }).filter(v => {
      if (this.searchQuery) {
        const matchesName = v.name.toLowerCase().includes(this.searchQuery);
        const matchesArea = v.area.toLowerCase().includes(this.searchQuery);
        const matchesSports = v.sports.some(s => s.toLowerCase().includes(this.searchQuery));
        const matchesCity = v.city.toLowerCase().includes(this.searchQuery);
        if (!matchesName && !matchesArea && !matchesSports && !matchesCity) return false;
      }

      if (this.activeQuickFilters.topRatedOnly && v.rating < 4.8) {
        return false;
      }

      if (this.activeQuickFilters.floodlightsOnly && !v.amenities.some(a => a.toLowerCase().includes('floodlight') || a.toLowerCase().includes('lighting'))) {
        return false;
      }

      if (this.activeQuickFilters.freeParkingOnly && !v.amenities.some(a => a.toLowerCase().includes('parking'))) {
        return false;
      }

      return true;
    });

    // Sorting
    if (this.activeQuickFilters.sortBy === 'priceAsc') {
      venues.sort((a, b) => a.startingPrice - b.startingPrice);
    } else if (this.activeQuickFilters.sortBy === 'ratingDesc') {
      venues.sort((a, b) => b.rating - a.rating);
    }

    // Toggle reset filters visibility
    const isFiltered = this.activeQuickFilters.oddHourOnly || this.activeQuickFilters.topRatedOnly || this.activeQuickFilters.floodlightsOnly || this.activeQuickFilters.freeParkingOnly || this.currentSport !== 'all' || this.searchQuery;
    if (pillReset) {
      pillReset.classList.toggle('hidden', !isFiltered);
    }

    if (countBadge) {
      countBadge.textContent = `${venues.length} ${venues.length === 1 ? 'Venue' : 'Venues'} Live`;
    }

    if (venues.length === 0) {
      grid.innerHTML = `
        <div class="col-span-full text-center py-16 bg-[#131B2E] rounded-3xl border border-white/10 shadow-xl space-y-3">
          <div class="text-5xl">🏏</div>
          <h3 class="text-xl font-bold text-white">No Matching Sports Facilities Found</h3>
          <p class="text-slate-400 text-xs max-w-md mx-auto">
            Try resetting your active filters, changing your selected region, or searching for other sports like Box Cricket or Football.
          </p>
          <button id="btn-empty-reset" class="px-5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs shadow-md transition">
            Reset All Filters
          </button>
        </div>
      `;

      const emptyReset = grid.querySelector('#btn-empty-reset');
      if (emptyReset) {
        emptyReset.addEventListener('click', () => {
          this.currentCity = 'all';
          this.currentSport = 'all';
          this.searchQuery = '';
          this.activeQuickFilters = {
            oddHourOnly: false,
            topRatedOnly: false,
            floodlightsOnly: false,
            freeParkingOnly: false,
            sortBy: 'featured'
          };
          this.updateCategoryRibbon(container);
          this.updateGrid(container);
        });
      }
      return;
    }

    grid.innerHTML = venues.map(venue => {
      // Calculate odd-hour dynamic discounted price (35% OFF)
      const oddHourRate = Math.round(venue.startingPrice * 0.65);
      const distance = venue.city.toLowerCase() === 'thane' ? '2.5 km away' : venue.city.toLowerCase() === 'mumbai' ? '4.8 km away' : '6.2 km away';

      return `
        <div class="bg-[#131B2E] rounded-3xl border border-white/10 hover:border-emerald-500/50 transition-all duration-300 overflow-hidden group flex flex-col justify-between shadow-2xl hover:shadow-emerald-950/40">
          
          <!-- Venue Image with Poster Presentation & Badges (BookMyShow / Snabbit style) -->
          <div class="relative h-56 overflow-hidden">
            <img src="${venue.images[0]}" alt="${venue.name}" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" loading="lazy" />
            <div class="absolute inset-0 bg-gradient-to-t from-[#131B2E] via-black/20 to-black/40"></div>
            
            <!-- Top Badges Overlay -->
            <div class="absolute top-3 left-3 flex flex-wrap gap-1.5 z-10">
              <span class="px-2.5 py-1 rounded-full text-xs font-black bg-slate-950/85 backdrop-blur-md text-emerald-400 border border-emerald-500/40 shadow-md">
                ${venue.city}
              </span>
              <span class="px-2 py-1 rounded-full text-[10px] font-bold bg-slate-900/80 backdrop-blur-md text-slate-300 border border-white/10 flex items-center gap-1">
                <span class="text-emerald-400">✓</span> Verified
              </span>
            </div>

            <!-- Rating Badge (BookMyShow Gold Star Style) -->
            <div class="absolute top-3 right-3 bg-slate-950/90 backdrop-blur-md px-2.5 py-1 rounded-xl text-xs font-black text-amber-300 flex items-center gap-1 border border-amber-500/40 shadow-lg z-10">
              <span class="text-amber-400">★</span> ${venue.rating}
              <span class="text-[10px] text-slate-400 font-normal">(${venue.reviewsCount})</span>
            </div>

            <!-- Bottom Image Overlay: Distance & Odd-Hour Tag -->
            <div class="absolute bottom-2.5 left-3 right-3 flex items-center justify-between text-xs z-10">
              <span class="px-2 py-0.5 rounded-lg bg-slate-950/80 backdrop-blur-md text-slate-300 text-[11px] font-semibold border border-white/10 flex items-center gap-1">
                <span>📍</span> ${venue.area} • ${distance}
              </span>
              <span class="px-2 py-0.5 rounded-lg bg-purple-950/90 backdrop-blur-md text-purple-300 text-[10px] font-black border border-purple-500/40 flex items-center gap-1">
                <span>⚡</span> 35% OFF Deals
              </span>
            </div>
          </div>

          <!-- Card Body -->
          <div class="p-5 flex-1 flex flex-col justify-between space-y-4">
            <div>
              <div class="flex items-start justify-between gap-2">
                <h3 class="text-lg font-black text-white group-hover:text-emerald-400 transition leading-snug">
                  ${venue.name}
                </h3>
              </div>
              <p class="text-xs text-slate-400 mt-1 flex items-center gap-1.5">
                <span class="text-slate-500">📍</span> ${venue.address}
              </p>

              <!-- Sports Chips (Snabbit Style) -->
              <div class="flex flex-wrap gap-1.5 mt-3">
                ${venue.sports.map(s => {
                  const icon = s === 'cricket' ? '🏏' : s === 'football' ? '⚽' : s === 'badminton' ? '🏸' : s === 'pickleball' ? '🏓' : '🎯';
                  return `
                    <span class="px-2.5 py-1 rounded-lg text-[11px] font-bold bg-slate-900/90 border border-white/10 text-slate-200 capitalize flex items-center gap-1">
                      <span>${icon}</span>
                      <span>${s}</span>
                    </span>
                  `;
                }).join('')}
              </div>

              <!-- Amenities Icon Bar -->
              <div class="flex flex-wrap gap-2 text-[11px] text-slate-400 mt-3 pt-3 border-t border-white/[0.08]">
                ${venue.amenities.slice(0, 3).map(a => `
                  <span class="flex items-center gap-1 text-slate-300">
                    <span class="text-emerald-400">✓</span> ${a}
                  </span>
                `).join('')}
                ${venue.amenities.length > 3 ? `<span class="text-emerald-400 font-bold">+${venue.amenities.length - 3} more</span>` : ''}
              </div>
            </div>

            <!-- Dynamic Pricing & Book Slots CTA (BookMyShow / Snabbit Style) -->
            <div class="pt-4 border-t border-white/[0.08] flex items-center justify-between mt-auto">
              <div>
                <span class="text-[10px] uppercase font-bold text-slate-400 block tracking-wider">Starting from</span>
                <div class="flex items-baseline gap-1.5">
                  <span class="text-xl font-black text-white">₹${venue.startingPrice}</span>
                  <span class="text-xs text-slate-400 font-normal">/hr</span>
                </div>
                <div class="text-[10px] font-bold text-purple-400 flex items-center gap-1">
                  <span>⚡</span> Odd-Hours from ₹${oddHourRate}/hr
                </div>
              </div>

              <button data-venue-id="${venue.venueId}" class="btn-select-venue px-5 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-400 hover:from-emerald-400 hover:to-teal-300 text-slate-950 font-black text-xs shadow-lg shadow-emerald-500/20 transition active:scale-95 flex items-center gap-1.5">
                <span>Book Slots</span>
                <span>→</span>
              </button>
            </div>

          </div>
        </div>
      `;
    }).join('');

    // Attach click handlers to Book Slots buttons
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
