/**
 * Project Apex - Venue & Consolidated Availability Service
 * Implements Read Minimization Protocol: Exactly 1 document read per court per day
 */

import { mockDb } from './firebaseConfig.js';
import { evaluateSlotTimeBand } from './pricingEngine.js';

export class VenueService {
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
