/**
 * Project Apex - Java Middleware REST API Client
 * Connects the Firebase Hosting frontend to the Render Java Spring Boot Middleware
 * and Neon PostgreSQL database, with automatic fallback to local runtime if offline.
 */

const LOCAL_API_BASE_URL = 'http://localhost:8080/api/v1';
const RENDER_API_BASE_URL = 'https://project-apex-backend-fqe3.onrender.com/api/v1';
const CUSTOM_API_URL_KEY = 'apex_custom_api_url';
const TOKEN_KEY = 'apex_jwt_token';
const ACTIVE_USER_KEY = 'apex_active_user_id';
const HEALTH_TIMEOUT_MS = 8000;

function normalizeBaseUrl(url) {
  if (!url || typeof url !== 'string') return '';
  let cleaned = url.trim();
  if (!cleaned) return '';
  cleaned = cleaned.replace(/\/+$/, '');
  if (/\/api\/v1$/i.test(cleaned)) {
    cleaned = cleaned.replace(/\/api\/v1$/i, '/api/v1');
  } else {
    cleaned = `${cleaned}/api/v1`;
  }
  return cleaned;
}

function getDefaultBaseUrl() {
  if (
    typeof window !== 'undefined' &&
    (window.location?.hostname === 'localhost' || window.location?.hostname === '127.0.0.1')
  ) {
    return LOCAL_API_BASE_URL;
  }
  return RENDER_API_BASE_URL;
}

function readStoredCustomUrl() {
  if (typeof localStorage !== 'undefined') {
    return localStorage.getItem(CUSTOM_API_URL_KEY);
  }
  if (typeof window !== 'undefined') {
    return window.localStorage?.getItem(CUSTOM_API_URL_KEY) || null;
  }
  return null;
}

function resolveInitialBaseUrl() {
  if (typeof window !== 'undefined' && typeof window.APEX_API_URL === 'string' && window.APEX_API_URL.trim()) {
    return normalizeBaseUrl(window.APEX_API_URL);
  }
  const storedCustomUrl = readStoredCustomUrl();
  if (storedCustomUrl && storedCustomUrl.trim()) {
    return normalizeBaseUrl(storedCustomUrl);
  }
  return getDefaultBaseUrl();
}

let customApiBaseUrl = resolveInitialBaseUrl();
let memoryToken = null;

export class ApiClient {
  static normalizeBaseUrl(url) {
    return normalizeBaseUrl(url);
  }

  static getDefaultBaseUrl() {
    return getDefaultBaseUrl();
  }

  static isOwnerRole(role) {
    const r = String(role || '').trim().toUpperCase();
    return r === 'VENUE_OWNER' || r === 'ROLE_OWNER' || r === 'OWNER';
  }

  static getBaseUrl() {
    return customApiBaseUrl;
  }

  static setBaseUrl(url) {
    const normalized = normalizeBaseUrl(url);
    if (!normalized) {
      return this.resetBaseUrl();
    }
    customApiBaseUrl = normalized;
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(CUSTOM_API_URL_KEY, customApiBaseUrl);
    }
    return customApiBaseUrl;
  }

  static resetBaseUrl() {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(CUSTOM_API_URL_KEY);
    }
    customApiBaseUrl =
      typeof window !== 'undefined' && typeof window.APEX_API_URL === 'string' && window.APEX_API_URL.trim()
        ? normalizeBaseUrl(window.APEX_API_URL)
        : getDefaultBaseUrl();
    return customApiBaseUrl;
  }

  static async checkHealth(targetUrl = null) {
    const base = targetUrl ? normalizeBaseUrl(targetUrl) : this.getBaseUrl();
    if (!base) {
      return { isOnline: false, error: 'Invalid backend URL', url: base };
    }
    const startedAt = Date.now();
    try {
      const url = `${base}/health`;
      const signal =
        typeof AbortSignal !== 'undefined' && typeof AbortSignal.timeout === 'function'
          ? AbortSignal.timeout(HEALTH_TIMEOUT_MS)
          : undefined;
      const res = await fetch(url, { method: 'GET', signal });
      const latencyMs = Date.now() - startedAt;
      if (res.ok) {
        const data = await res.json();
        return { isOnline: true, data, url: base, latencyMs };
      }
      return { isOnline: false, error: `HTTP ${res.status}`, url: base, latencyMs };
    } catch (e) {
      return { isOnline: false, error: e.message, url: base, latencyMs: Date.now() - startedAt };
    }
  }

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
    const url = `${this.getBaseUrl()}${endpoint}`;
    const config = {
      ...options,
      headers: {
        ...this.getHeaders(),
        ...(options.headers || {})
      }
    };

    try {
      const response = await fetch(url, config);
      const rawText = await response.text();
      let data = {};
      if (rawText) {
        try {
          data = JSON.parse(rawText);
        } catch {
          data = {};
        }
      }
      if (!response.ok) {
        throw new Error(data?.message || `HTTP ${response.status}: Request failed`);
      }
      return data;
    } catch (err) {
      // If network error / backend offline, log warning and let caller handle
      console.warn(`[Apex ApiClient] Request to ${url} failed:`, err.message);
      throw err;
    }
  }

  // --- Auth APIs ---
  static async loginWithPassword({ identifier, password }) {
    const res = await this.request('/auth/login/password', {
      method: 'POST',
      body: JSON.stringify({ identifier, password })
    });
    if (res.data?.token) {
      this.setToken(res.data.token);
    }
    return res;
  }

  static async registerWithPassword(payload) {
    const res = await this.request('/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    if (res.data?.token) {
      this.setToken(res.data.token);
    }
    return res;
  }

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

  // --- Users Profile APIs ---
  static async getMe() {
    return this.request('/users/me');
  }

  static async updateProfile(payload) {
    return this.request('/users/me', {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
  }

  static async deleteProfile() {
    return this.request('/users/me', {
      method: 'DELETE'
    });
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

  static async getOwnerVenues() {
    return this.request('/owner/venues');
  }

  static async createOwnerVenue(payload) {
    return this.request('/owner/venues', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  static async updateOwnerVenue(venueId, payload) {
    return this.request(`/owner/venues/${encodeURIComponent(venueId)}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
  }

  static async deleteOwnerVenue(venueId) {
    return this.request(`/owner/venues/${encodeURIComponent(venueId)}`, {
      method: 'DELETE'
    });
  }

  static async addFacility(facilityPayload) {
    return this.request('/facilities', {
      method: 'POST',
      body: JSON.stringify(facilityPayload)
    });
  }

  static async updateFacility(courtId, facilityPayload) {
    return this.request(`/facilities/${encodeURIComponent(courtId)}`, {
      method: 'PUT',
      body: JSON.stringify(facilityPayload)
    });
  }

  static async deleteFacility(courtId) {
    return this.request(`/facilities/${encodeURIComponent(courtId)}`, {
      method: 'DELETE'
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

  static async initiateBooking(payload) {
    return this.request('/bookings/initiate', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  static async verifyPayment(bookingId, payload) {
    return this.request(`/bookings/${encodeURIComponent(bookingId)}/verify-payment`, {
      method: 'POST',
      body: JSON.stringify(payload)
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

  static async getBooking(bookingId) {
    return this.request(`/bookings/${encodeURIComponent(bookingId)}`);
  }

  static async updateBooking(bookingId, payload) {
    return this.request(`/bookings/${encodeURIComponent(bookingId)}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
  }

  static async cancelBooking(bookingId, reason = 'Player requested cancellation') {
    return this.request(`/bookings/${encodeURIComponent(bookingId)}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ reason })
    });
  }

  static async deleteBooking(bookingId) {
    return this.request(`/bookings/${encodeURIComponent(bookingId)}`, {
      method: 'DELETE'
    });
  }

  // --- Owner Bookings & Gate ---
  static async getOwnerBookings() {
    return this.request('/owner/bookings');
  }

  static async cancelBookingByOwner(bookingId, reason = 'Owner emergency cancellation') {
    return this.request(`/owner/bookings/${encodeURIComponent(bookingId)}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ reason })
    });
  }

  static async updateOwnerBookingStatus(bookingId, status) {
    return this.request(`/owner/bookings/${encodeURIComponent(bookingId)}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    });
  }

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
