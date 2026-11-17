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
