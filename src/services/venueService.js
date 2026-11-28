/**
 * Project Apex - Venue & Consolidated Availability Service
 * Implements Read Minimization Protocol: Exactly 1 document read per court per day
 */

import { mockDb } from './firebaseConfig.js';
import { evaluateSlotTimeBand } from './pricingEngine.js';
import { ApiClient } from './apiClient.js';

const SPORT_ENUM_MAP = {
  cricket: 'CRICKET_TURF',
  football: 'FOOTBALL_TURF',
  badminton: 'BADMINTON',
  pickleball: 'PICKLEBALL',
  squash: 'SQUASH',
  tennis: 'LAWN_TENNIS',
  pool_table: 'POOL_TABLE',
  multi_sport: 'MULTI_SPORT'
};

function normalizeSportSlug(s) {
  const clean = String(s || 'cricket').toLowerCase();
  if (clean.includes('cricket')) return 'cricket';
  if (clean.includes('football')) return 'football';
  if (clean.includes('badminton')) return 'badminton';
  if (clean.includes('pickleball')) return 'pickleball';
  if (clean.includes('squash')) return 'squash';
  if (clean.includes('tennis')) return 'tennis';
  if (clean.includes('pool')) return 'pool_table';
  if (clean.includes('multi')) return 'multi_sport';
  return clean;
}

export class VenueService {
  static mapBackendVenue(v) {
    if (!v || !v.venueId) return null;
    const rawSports = Array.isArray(v.sportTypes) ? v.sportTypes : (v.sports || ['cricket']);
    const sports = Array.from(new Set(rawSports.map(normalizeSportSlug)));
    return {
      venueId: v.venueId,
      ownerId: v.ownerId || 'owner_01',
      name: v.name,
      city: v.city ? v.city.charAt(0).toUpperCase() + v.city.slice(1) : 'Thane',
      area: v.locality || v.area || 'Central Area',
      address: v.addressLine || v.address || `${v.locality || v.area || ''}, ${v.city || 'Thane'}`,
      coordinates: { lat: v.latitude || 19.2183, lng: v.longitude || 72.9781 },
      sports: sports.length ? sports : ['cricket'],
      amenities: v.amenities || ['Floodlights', 'Changing Rooms'],
      rating: v.rating || 4.8,
      reviewsCount: v.reviewCount || v.reviewsCount || 120,
      startingPrice: v.minSlotPricePaise ? Math.round(v.minSlotPricePaise / 100) : (v.startingPrice || 1000),
      featured: true,
      status: v.status || 'ACTIVE',
      images: (v.imageUrls && v.imageUrls.length) ? v.imageUrls : (v.images || ['https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=1200&q=80']),
      createdAt: v.createdAt || Date.now(),
      source: 'backend'
    };
  }

  static mapBackendCourt(c) {
    if (!c) return null;
    const courtId = c.courtId || c.resourceId;
    if (!courtId) return null;
    const baseRate = c.baseHourlyRate ?? (c.basePricePaise ? Math.round(c.basePricePaise / 100) : 1000);
    const peakRate = c.peakHourlyRate ?? (c.peakHourPricePaise ? Math.round(c.peakHourPricePaise / 100) : Math.round(baseRate * 1.3));
    return {
      courtId,
      venueId: c.venueId,
      name: c.name || 'Main Pitch',
      sport: normalizeSportSlug(c.sport || (c.sportTypes && c.sportTypes[0])),
      capacity: c.capacity || c.maxPlayers || 16,
      surface: c.surface || c.surfaceType || 'Artificial Turf',
      dimensions: c.dimensions || '110ft x 65ft',
      isIndoor: !!(c.isIndoor ?? c.indoor),
      baseHourlyRate: Number(baseRate),
      peakHourlyRate: Number(peakRate),
      oddHourDiscountPct: Number(c.oddHourDiscountPct ?? 35),
      source: 'backend'
    };
  }

  static async syncVenuesFromBackend() {
    if (typeof window === 'undefined') return [];
    try {
      const res = await ApiClient.getVenues();
      if (res && Array.isArray(res.data)) {
        const backendIds = new Set();
        const mappedList = [];
        for (const raw of res.data) {
          const mv = this.mapBackendVenue(raw);
          if (!mv) continue;
          backendIds.add(mv.venueId);
          mockDb.data.venues[mv.venueId] = mv;
          mappedList.push(mv);

          try {
            const cRes = await ApiClient.getCourtsForVenue(mv.venueId);
            if (cRes && Array.isArray(cRes.data)) {
              const serverCourtIds = new Set();
              cRes.data.forEach(rawCourt => {
                const mc = this.mapBackendCourt(rawCourt);
                if (mc) {
                  serverCourtIds.add(mc.courtId);
                  mockDb.data.courts[mc.courtId] = mc;
                }
              });
              Object.keys(mockDb.data.courts).forEach(cid => {
                if (mockDb.data.courts[cid].venueId === mv.venueId
                    && mockDb.data.courts[cid].source === 'backend'
                    && !serverCourtIds.has(cid)) {
                  delete mockDb.data.courts[cid];
                }
              });
            }
          } catch (ignored) {}
        }

        Object.keys(mockDb.data.venues).forEach(vid => {
          if (mockDb.data.venues[vid].source === 'backend' && !backendIds.has(vid)) {
            delete mockDb.data.venues[vid];
          }
        });
        mockDb.persist();
        return mappedList;
      }
    } catch (e) {
      console.warn('[VenueService] Backend sync fallback:', e.message);
    }
    return this.getVenues();
  }

  /**
   * Retrieves venues with optional location and sport filters
   */
  static getVenues({ city = 'all', sport = 'all' } = {}) {
    const venues = Object.values(mockDb.data.venues);

    return venues.filter(v => {
      const matchCity = city === 'all' || v.city.toLowerCase() === city.toLowerCase();
      const matchSport = sport === 'all' || v.sports.includes(sport.toLowerCase());
      return matchCity && matchSport;
    });
  }

  /**
   * Retrieves single venue by ID
   */
  static getVenueById(venueId) {
    return mockDb.data.venues[venueId] || null;
  }

  /**
   * Creates a new sports venue facility and persists it to the database
   */
  static createVenue({
    name,
    city = 'Thane',
    area,
    address,
    sports = ['cricket'],
    amenities = ['Floodlights', 'Changing Rooms', 'Parking'],
    startingPrice = 1000,
    images = [],
    ownerId = 'demo_owner',
    courts = []
  }) {
    if (!name || !name.trim()) {
      throw new Error("Venue Name is required.");
    }
    if (!area || !area.trim()) {
      throw new Error("Area / Locality is required.");
    }

    const cleanName = name.trim();
    const slug = cleanName.toLowerCase().replace(/[^a-z0-9]/g, '_');
    const venueId = `venue_${slug}_${Math.floor(100 + Math.random() * 900)}`;

    const defaultImages = [
      "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=800&q=80"
    ];

    const newVenue = {
      venueId,
      name: cleanName,
      city: city.charAt(0).toUpperCase() + city.slice(1),
      area: area.trim(),
      address: address ? address.trim() : `${area.trim()}, ${city}, Maharashtra`,
      coordinates: { lat: 19.2183, lng: 72.9781 },
      sports: sports.length > 0 ? sports : ['cricket'],
      amenities: amenities.length > 0 ? amenities : ['Floodlights', 'Changing Rooms'],
      rating: 5.0,
      reviewsCount: 1,
      startingPrice: Number(startingPrice) || 1000,
      featured: true,
      ownerId,
      status: 'ACTIVE',
      images: images.length > 0 ? images : defaultImages,
      createdAt: Date.now()
    };

    // Save venue to database
    mockDb.data.venues[venueId] = newVenue;

    // Create associated courts/pitches
    if (courts && courts.length > 0) {
      courts.forEach((c, index) => {
        this.addCourt({
          venueId,
          name: c.name || `Court ${index + 1}`,
          sport: c.sport || sports[0] || 'cricket',
          surface: c.surface || 'Artificial Grass Turf',
          dimensions: c.dimensions || '100ft x 60ft',
          baseHourlyRate: Number(c.baseHourlyRate) || Number(startingPrice) || 1000,
          peakHourlyRate: Number(c.peakHourlyRate) || Math.round((Number(startingPrice) || 1000) * 1.3),
          oddHourDiscountPct: Number(c.oddHourDiscountPct) || 30
        });
      });
    } else {
      // Default initial court
      this.addCourt({
        venueId,
        name: `${cleanName} Main Pitch`,
        sport: sports[0] || 'cricket',
        surface: 'Professional Artificial Turf',
        dimensions: '110ft x 65ft',
        baseHourlyRate: Number(startingPrice) || 1000,
        peakHourlyRate: Math.round((Number(startingPrice) || 1000) * 1.3),
        oddHourDiscountPct: 35
      });
    }

    // Append to audit logs
    mockDb.data.audit_logs.push({
      logId: `audit_${Date.now()}`,
      action: 'VENUE_CREATED',
      actorId: ownerId,
      resourceType: 'venue',
      resourceId: venueId,
      metadata: { name: cleanName, city, sports },
      timestamp: Date.now()
    });

    mockDb.persist();
    return newVenue;
  }

  /**
   * Adds a court/pitch to an existing venue
   */
  static addCourt({
    venueId,
    name,
    sport = 'cricket',
    surface = 'Artificial Turf',
    dimensions = '100ft x 60ft',
    baseHourlyRate = 1000,
    peakHourlyRate = 1400,
    oddHourDiscountPct = 30
  }) {
    const slug = name.toLowerCase().replace(/[^a-z0-9]/g, '_');
    const courtId = `court_${venueId}_${slug}_${Math.floor(100 + Math.random() * 900)}`;

    const newCourt = {
      courtId,
      venueId,
      name,
      sport: sport.toLowerCase(),
      surface,
      dimensions,
      baseHourlyRate: Number(baseHourlyRate),
      peakHourlyRate: Number(peakHourlyRate),
      oddHourDiscountPct: Number(oddHourDiscountPct)
    };

    mockDb.data.courts[courtId] = newCourt;
    mockDb.persist();
    return newCourt;
  }

  /**
   * Deletes a venue from the database
   */
  static deleteVenue(venueId) {
    if (mockDb.data.venues[venueId]) {
      delete mockDb.data.venues[venueId];

      // Remove associated courts
      Object.keys(mockDb.data.courts).forEach(courtId => {
        if (mockDb.data.courts[courtId].venueId === venueId) {
          delete mockDb.data.courts[courtId];
        }
      });

      mockDb.persist();
      return true;
    }
    return false;
  }

  static async createVenueAndSync(payload) {
    if (typeof window !== 'undefined' && ApiClient.getToken()) {
      const sportTypes = (payload.sports || ['cricket']).map(s => SPORT_ENUM_MAP[s.toLowerCase()] || s.toUpperCase());
      const reqBody = {
        name: payload.name,
        city: (payload.city || 'thane').toLowerCase(),
        locality: payload.area,
        addressLine: payload.address || `${payload.area}, ${payload.city}`,
        amenities: payload.amenities || ['Floodlights', 'Changing Rooms', 'Parking'],
        sportTypes,
        minSlotPricePaise: (Number(payload.startingPrice) || 1000) * 100,
        imageUrls: payload.images && payload.images.length ? payload.images : []
      };
      const res = await ApiClient.createOwnerVenue(reqBody);
      if (res && res.data && res.data.venueId) {
        const mappedVenue = this.mapBackendVenue(res.data);
        mockDb.data.venues[mappedVenue.venueId] = mappedVenue;

        // Also provision primary Facility / Court on backend so slots work immediately
        try {
          const facRes = await ApiClient.addFacility({
            venueId: mappedVenue.venueId,
            name: `${mappedVenue.name} Main Pitch`,
            sport: (payload.sports && payload.sports[0]) || 'cricket',
            capacity: 16,
            surface: '15mm Artificial Monofilament Turf',
            dimensions: '115ft x 65ft',
            baseHourlyRate: Math.max(200, Number(payload.startingPrice) || 1000),
            peakHourlyRate: Math.round(Math.max(200, Number(payload.startingPrice) || 1000) * 1.3),
            oddHourDiscountPct: 35,
            hasFloodlights: true,
            amenities: reqBody.amenities
          });
          if (facRes && facRes.data) {
            const mc = this.mapBackendCourt(facRes.data);
            mockDb.data.courts[mc.courtId] = mc;
          }
        } catch (facErr) {
          console.warn('[VenueService] Facility auto-create warning:', facErr.message);
        }

        mockDb.persist();
        return mappedVenue;
      }
    }
    return this.createVenue(payload);
  }

  static async updateVenueAndSync(venueId, updates) {
    if (typeof window !== 'undefined' && ApiClient.getToken()) {
      const existing = this.getVenueById(venueId) || {};
      const merged = { ...existing, ...updates };
      const sportTypes = (merged.sports || ['cricket']).map(s => SPORT_ENUM_MAP[s.toLowerCase()] || s.toUpperCase());
      const res = await ApiClient.updateOwnerVenue(venueId, {
        name: merged.name,
        city: (merged.city || 'thane').toLowerCase(),
        locality: merged.area,
        addressLine: merged.address,
        amenities: merged.amenities || [],
        sportTypes,
        minSlotPricePaise: (Number(merged.startingPrice) || 1000) * 100,
        imageUrls: merged.images || []
      });
      if (res && res.data) {
        const mv = this.mapBackendVenue(res.data);
        mockDb.data.venues[venueId] = mv;
        mockDb.persist();
        return mv;
      }
    }
    const localVenue = mockDb.data.venues[venueId];
    if (!localVenue) throw new Error('Venue not found');
    Object.assign(localVenue, updates, { updatedAt: Date.now() });
    mockDb.persist();
    return localVenue;
  }

  static async deleteVenueAndSync(venueId) {
    if (typeof window !== 'undefined' && ApiClient.getToken()) {
      await ApiClient.deleteOwnerVenue(venueId);
    }
    return this.deleteVenue(venueId);
  }

  static async addCourtAndSync(payload) {
    if (typeof window !== 'undefined' && ApiClient.getToken()) {
      const res = await ApiClient.addFacility({
        venueId: payload.venueId,
        name: payload.name,
        sport: payload.sport || 'cricket',
        facilityType: payload.isIndoor ? 'Indoor Court' : 'Outdoor Turf',
        capacity: Number(payload.capacity) || 16,
        surface: payload.surface || '50mm Synthetic AstroTurf',
        dimensions: payload.dimensions || '110ft x 65ft',
        baseHourlyRate: Number(payload.baseHourlyRate) || 1000,
        peakHourlyRate: Number(payload.peakHourlyRate) || 1400,
        oddHourDiscountPct: Number(payload.oddHourDiscountPct) || 30,
        hasFloodlights: true
      });
      if (res && res.data) {
        const mc = this.mapBackendCourt(res.data);
        mockDb.data.courts[mc.courtId] = mc;
        mockDb.persist();
        return mc;
      }
    }
    return this.addCourt(payload);
  }

  static async updateCourtAndSync(courtId, updates) {
    const existing = mockDb.data.courts[courtId] || {};
    const merged = { ...existing, ...updates };
    if (typeof window !== 'undefined' && ApiClient.getToken()) {
      const res = await ApiClient.updateFacility(courtId, {
        venueId: merged.venueId,
        name: merged.name,
        sport: merged.sport || 'cricket',
        capacity: Number(merged.capacity) || 16,
        surface: merged.surface || 'Synthetic Turf',
        dimensions: merged.dimensions || '110ft x 65ft',
        baseHourlyRate: Number(merged.baseHourlyRate) || 1000,
        peakHourlyRate: Number(merged.peakHourlyRate) || 1400,
        oddHourDiscountPct: Number(merged.oddHourDiscountPct) || 30
      });
      if (res && res.data) {
        const mc = this.mapBackendCourt(res.data);
        mockDb.data.courts[courtId] = mc;
        mockDb.persist();
        return mc;
      }
    }
    mockDb.data.courts[courtId] = merged;
    mockDb.persist();
    return merged;
  }

  static async deleteCourtAndSync(courtId) {
    if (typeof window !== 'undefined' && ApiClient.getToken()) {
      await ApiClient.deleteFacility(courtId);
    }
    if (mockDb.data.courts[courtId]) {
      delete mockDb.data.courts[courtId];
      mockDb.persist();
      return true;
    }
    return false;
  }

  /**
   * Retrieves courts for a given venue
   */
  static getCourtsForVenue(venueId) {
    return Object.values(mockDb.data.courts).filter(c => c.venueId === venueId);
  }

  /**
   * READ MINIMIZATION PROTOCOL:
   * Retrieves the consolidated daily availability document for courtId and date.
   * Consumes only 1 read per day instead of 24 reads per slot.
   */
  static getDailyAvailability(courtId, dateStr) {
    const docKey = `${courtId}_${dateStr}`;
    let dailyDoc = mockDb.data.availability[docKey];

    // If not yet generated for this day, initialize it
    if (!dailyDoc) {
      dailyDoc = this.generateInitialDailyDoc(courtId, dateStr);
      mockDb.data.availability[docKey] = dailyDoc;
      mockDb.persist();
    }

    // Refresh dynamic lock statuses
    const now = Date.now();
    const refreshedSlots = { ...dailyDoc.slots };

    Object.keys(refreshedSlots).forEach(slotTime => {
      const lockKey = `${courtId}_${dateStr}_${slotTime}`;
      const lock = mockDb.data.concurrency_locks[lockKey];

      if (refreshedSlots[slotTime].status === 'BOOKED') {
        // Kept booked
      } else if (lock && lock.status === 'HELD' && lock.expiresAt > now) {
        refreshedSlots[slotTime].status = 'RESERVED';
        refreshedSlots[slotTime].reservedUntil = lock.expiresAt;
      } else if (refreshedSlots[slotTime].status === 'RESERVED') {
        // Expired lock: restore available
        refreshedSlots[slotTime].status = 'AVAILABLE';
        delete refreshedSlots[slotTime].reservedUntil;
      }
    });

    return {
      ...dailyDoc,
      slots: refreshedSlots
    };
  }

  /**
   * Generates initial 24-slot map for a given court and day
   */
  static generateInitialDailyDoc(courtId, dateStr) {
    const court = mockDb.data.courts[courtId] || { baseHourlyRate: 1000, venueId: 'venue_thane_01' };
    const slots = {};

    // Hours from 06:00 to 23:00
    for (let h = 6; h <= 23; h++) {
      const slotTime = `${String(h).padStart(2, '0')}:00`;
      const band = evaluateSlotTimeBand(slotTime);

      let slotPrice = court.baseHourlyRate;
      if (band.isPeak) {
        slotPrice = Math.round(court.baseHourlyRate * 1.2);
      } else if (band.isOddHour) {
        slotPrice = Math.round(court.baseHourlyRate * (1 - band.discountPct / 100));
      }

      // Seed a few booked slots for realistic demo
      let status = 'AVAILABLE';
      if ((h === 19 || h === 20) && dateStr.endsWith('20')) {
        status = 'BOOKED';
      }

      slots[slotTime] = {
        slotTime,
        status,
        basePrice: court.baseHourlyRate,
        currentPrice: slotPrice,
        isOddHour: band.isOddHour,
        isPeak: band.isPeak,
        discountPct: band.discountPct || 0,
        bandName: band.bandName
      };
    }

    return {
      courtId,
      venueId: court.venueId,
      date: dateStr,
      slots,
      lastUpdated: Date.now()
    };
  }
}
