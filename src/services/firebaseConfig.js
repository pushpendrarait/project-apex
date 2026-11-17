/**
 * Project Apex - Firebase Configuration & SDK Initializer
 * Optimized for Firebase Spark Free Tier
 */

// Production Firebase Configuration for project-apex-pushp
export const firebaseConfig = {
  apiKey: "AIzaSyD-MOCK-API-KEY-APEX-SPORTS-THANE",
  authDomain: "project-apex-pushp.firebaseapp.com",
  projectId: "project-apex-pushp",
  storageBucket: "project-apex-pushp.appspot.com",
  messagingSenderId: "77182910291",
  appId: "1:77182910291:web:98ab234c910123"
};

// State store for client runtime simulation / live mode
class MockFirestoreRuntime {
  constructor() {
    this.storageKey = 'apex_firestore_db_v1';
    this.data = this.loadInitialData();
  }

  loadInitialData() {
    try {
      const saved = localStorage.getItem(this.storageKey);
      if (saved) return JSON.parse(saved);
    } catch (e) {
      console.warn("Storage error, fallback to seed data", e);
    }
    return this.getSeedData();
  }

  persist() {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(this.data));
    } catch (e) {
      console.error("Failed to persist to localStorage", e);
    }
  }

  getSeedData() {
    return {
      venues: {
        "venue_thane_01": {
          venueId: "venue_thane_01",
          name: "Apex Arena Ghodbunder",
          city: "Thane",
          area: "Ghodbunder Road",
          address: "Opp. Hypercity, Kasarvadavali, Ghodbunder Rd, Thane West 400615",
          coordinates: { lat: 19.2625, lng: 72.9723 },
          sports: ["cricket", "football", "badminton"],
          amenities: ["FIFA-grade Turf", "LED Floodlights", "Air-conditioned Dugout", "Locker Rooms", "Cafeteria", "Valet Parking"],
          rating: 4.9,
          reviewsCount: 312,
          startingPrice: 900,
          featured: true,
          images: [
            "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=800&q=80"
          ]
        },
        "venue_thane_02": {
          venueId: "venue_thane_02",
          name: "Majiwada Sports Hub",
          city: "Thane",
          area: "Majiwada",
          address: "Near Lodha Paradise, Majiwada Junction, Thane West 400601",
          coordinates: { lat: 19.2081, lng: 72.9812 },
          sports: ["cricket", "pickleball", "badminton"],
          amenities: ["Box Cricket Turf", "4 Pickleball Courts", "Pro Equipment Rental", "Shower Cabins", "Free Parking"],
          rating: 4.7,
          reviewsCount: 188,
          startingPrice: 750,
          featured: false,
          images: [
            "https://images.unsplash.com/photo-1511193311914-0346f16efe90?auto=format&fit=crop&w=1200&q=80"
          ]
        },
        "venue_mumbai_01": {
          venueId: "venue_mumbai_01",
          name: "Bandra Turf Club Pro",
          city: "Mumbai",
          area: "Bandra West",
          address: "Pali Hill Road, Near Carter Road Promenade, Bandra West, Mumbai 400050",
          coordinates: { lat: 19.0596, lng: 72.8295 },
          sports: ["football", "cricket", "tennis"],
          amenities: ["Ocean Breeze Setting", "Stadium Floodlights", "Physio Lounge", "Live Match Streaming", "Juice Bar"],
          rating: 4.9,
          reviewsCount: 540,
          startingPrice: 1400,
          featured: true,
          images: [
            "https://images.unsplash.com/photo-1529900748604-07564a03e7a6?auto=format&fit=crop&w=1200&q=80"
          ]
        },
        "venue_mumbai_02": {
          venueId: "venue_mumbai_02",
          name: "Powai Lakeside Smash Arena",
          city: "Mumbai",
          area: "Powai",
          address: "Hiranandani Gardens, Powai, Mumbai 400076",
          coordinates: { lat: 19.1176, lng: 72.9060 },
          sports: ["badminton", "squash", "pickleball"],
          amenities: ["Wooden Badminton Courts", "Air-conditioned Squash", "Electronic Scoreboards", "Pro Shop"],
          rating: 4.8,
          reviewsCount: 220,
          startingPrice: 600,
          featured: true,
          images: [
            "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=1200&q=80"
          ]
        },
        "venue_navimumbai_01": {
          venueId: "venue_navimumbai_01",
          name: "Vashi Prime Sports Center",
          city: "Navi Mumbai",
          area: "Vashi Sector 17",
          address: "Sector 17, Behind Center One Mall, Vashi, Navi Mumbai 400703",
          coordinates: { lat: 19.0771, lng: 72.9986 },
          sports: ["cricket", "football", "badminton"],
          amenities: ["Multi-sport Turf", "Day/Night Lighting", "Drinking Water Stations", "Security Cams"],
          rating: 4.6,
          reviewsCount: 165,
          startingPrice: 700,
          featured: false,
          images: [
            "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=1200&q=80"
          ]
        }
      },
      courts: {
        "court_thane_01_crk": {
          courtId: "court_thane_01_crk",
          venueId: "venue_thane_01",
          name: "Main Arena 8v8 Box Cricket",
          sport: "cricket",
          surface: "15mm Artificial Monofilament Turf",
          dimensions: "115ft x 65ft",
          baseHourlyRate: 1200,
          peakHourlyRate: 1600,
          oddHourDiscountPct: 35
        },
        "court_thane_01_fb": {
          courtId: "court_thane_01_fb",
          venueId: "venue_thane_01",
          name: "Championship 5-a-Side Football",
          sport: "football",
          surface: "50mm Rubber Infill AstroTurf",
          dimensions: "120ft x 70ft",
          baseHourlyRate: 1400,
          peakHourlyRate: 1800,
          oddHourDiscountPct: 30
        },
        "court_thane_02_pk": {
          courtId: "court_thane_02_pk",
          venueId: "venue_thane_02",
          name: "Pickleball Court Alpha",
          sport: "pickleball",
          surface: "Cushioned Acrylic Hardcourt",
          dimensions: "44ft x 20ft",
          baseHourlyRate: 750,
          peakHourlyRate: 950,
          oddHourDiscountPct: 25
        },
        "court_mumbai_01_fb": {
          courtId: "court_mumbai_01_fb",
          venueId: "venue_mumbai_01",
          name: "Bandra Seafront Football Arena",
          sport: "football",
          surface: "European Synthetic Grass",
          dimensions: "125ft x 75ft",
          baseHourlyRate: 1600,
          peakHourlyRate: 2200,
          oddHourDiscountPct: 30
        },
        "court_mumbai_02_bm": {
          courtId: "court_mumbai_02_bm",
          venueId: "venue_mumbai_02",
          name: "Indoor Teakwood Badminton Court 1",
          sport: "badminton",
          surface: "BWF Standard Teakwood Flooring",
          dimensions: "44ft x 20ft",
          baseHourlyRate: 650,
          peakHourlyRate: 850,
          oddHourDiscountPct: 30
        }
      },
      availability: {},
      concurrency_locks: {},
      bookings: {},
      transactions: {},
      audit_logs: [],
      users: {
        "demo_player": {
          uid: "demo_player",
          displayName: "Rohan Deshmukh",
          email: "rohan.deshmukh@apex.dev",
          phone: "+91 98201 12345",
          role: "player",
          city: "Thane",
          loyaltyPoints: 350,
          referralCode: "ROHAN50"
        },
        "demo_owner": {
          uid: "demo_owner",
          displayName: "Santosh Kadam (Owner)",
          email: "santosh@apexarena.com",
          phone: "+91 98190 54321",
          role: "venue_owner",
          venueId: "venue_thane_01"
        }
      }
    };
  }
}

export const mockDb = new MockFirestoreRuntime();
