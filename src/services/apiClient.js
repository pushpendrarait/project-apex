/**
 * Project Apex - Java Middleware REST API Client
 * Connects the frontend to the Java Spring Boot backend (port 8080)
 * with automatic fallback to local runtime if offline
 */

const API_BASE_URL = typeof window !== 'undefined' && window.APEX_API_URL 
  ? window.APEX_API_URL 
  : 'http://localhost:8080/api/v1';

const TOKEN_KEY = 'apex_jwt_token';
const ACTIVE_USER_KEY = 'apex_active_user_id';
let memoryToken = null;

export class ApiClient {
  static getToken() {
    if (typeof localStorage !== 'undefined') {
      return localStorage.getItem(TOKEN_KEY) || memoryToken;
    }
    return memoryToken;
  }

  static setToken(token) {
    memoryToken = token;
    if (typeof localStorage !== 'undefined') {
      if (token) localStorage.setItem(TOKEN_KEY, token);
      else localStorage.removeItem(TOKEN_KEY);
    }
  }

  static getHeaders() {
    const headers = {
      'Content-Type': 'application/json'
    };
    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    const storedUid = typeof localStorage !== 'undefined' ? localStorage.getItem(ACTIVE_USER_KEY) : null;
    if (storedUid && storedUid !== 'guest') {
      headers['X-User-Id'] = storedUid;
      headers['X-Owner-Id'] = storedUid;
    }
    return headers;
  }

  static async request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const config = {
      ...options,
      headers: {
        ...this.getHeaders(),
        ...(options.headers || {})
      }
    };

    try {
      const response = await fetch(url, config);
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || `HTTP ${response.status}: Request failed`);
      }
      return data;
    } catch (err) {
      // If network error / backend offline, log warning and let caller handle
      console.warn(`[Apex ApiClient] Request to ${url} failed:`, err.message);
      throw err;
    }
  }

  // --- Auth APIs ---
  static async sendRegistrationOtp({ phone, email, displayName }) {
    return this.request('/auth/register/send-otp', {
      method: 'POST',
      body: JSON.stringify({ phone, email, displayName })
    });
  }

  static async verifyRegistrationOtp(payload) {
    const res = await this.request('/auth/register/verify-otp', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    if (res.data?.token) {
      this.setToken(res.data.token);
    }
    return res;
  }

  static async sendLoginOtp(identifier) {
    return this.request('/auth/login/send-otp', {
      method: 'POST',
      body: JSON.stringify({ identifier })
    });
  }

  static async verifyLoginOtp({ sessionId, otp }) {
    const res = await this.request('/auth/login/verify-otp', {
      method: 'POST',
      body: JSON.stringify({ sessionId, otp })
    });
    if (res.data?.token) {
      this.setToken(res.data.token);
    }
    return res;
  }

  static async logout() {
    this.setToken(null);
    try {
      await this.request('/auth/logout', { method: 'POST' });
    } catch (ignored) {}
  }

  // --- Venues & Facilities APIs ---
  static async getVenues(city, sport) {
    const params = new URLSearchParams();
    if (city) params.append('city', city);
    if (sport) params.append('sport', sport);
    return this.request(`/venues?${params.toString()}`);
  }

  static async getVenueById(venueId) {
    return this.request(`/venues/${venueId}`);
  }

  static async getCourtsForVenue(venueId) {
    return this.request(`/venues/${venueId}/courts`);
  }

  static async addFacility(facilityPayload) {
    return this.request('/facilities', {
      method: 'POST',
      body: JSON.stringify(facilityPayload)
    });
  }

  static async createFacility(facilityPayload) {
    return this.addFacility(facilityPayload);
  }

  // --- Slots & Concurrency Locks ---
  static async getSlots(courtId, date) {
    return this.request(`/slots?courtId=${encodeURIComponent(courtId)}&date=${encodeURIComponent(date)}`);
  }

  static async acquireLock({ courtId, date, slot }) {
    return this.request('/slots/lock', {
      method: 'POST',
      body: JSON.stringify({ courtId, date, slot })
    });
  }

  static async releaseLock({ courtId, date, slot }) {
    return this.request(`/slots/lock?courtId=${encodeURIComponent(courtId)}&date=${encodeURIComponent(date)}&slot=${encodeURIComponent(slot)}`, {
      method: 'DELETE'
    });
  }

  // --- Razorpay Payment & Booking Commit ---
  static async createRazorpayOrder({ courtId, date, slot, amountInRupees, promoCode }) {
    return this.request('/payments/razorpay/create-order', {
      method: 'POST',
      body: JSON.stringify({ courtId, date, slot, amountInRupees, promoCode })
    });
  }

  static async commitBooking(bookingPayload) {
    return this.request('/bookings/commit', {
      method: 'POST',
      body: JSON.stringify(bookingPayload)
    });
  }

  static async getMyPasses() {
    return this.request('/bookings/my-passes');
  }

  static async cancelBooking(bookingId) {
    return this.request(`/bookings/${bookingId}/cancel`, {
      method: 'POST'
    });
  }

  // --- Gate & Walk-In ---
  static async verifyGatePass(qrPassToken) {
    return this.request('/gate/verify-pass', {
      method: 'POST',
      body: JSON.stringify({ qrPassToken })
    });
  }

  static async createWalkInBooking(walkInPayload) {
    return this.request('/gate/walk-in', {
      method: 'POST',
      body: JSON.stringify(walkInPayload)
    });
  }
}
