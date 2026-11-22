/**
 * Project Apex - User Management & Authentication Service
 * Manages user creation, profile persistence, role assignment, and active sessions
 */

import { mockDb } from './firebaseConfig.js';
import { VenueService } from './venueService.js';

const ACTIVE_USER_KEY = 'apex_active_user_id';

export class UserService {
  static activeUserUid = 'demo_player';

  /**
   * Checks if an authenticated user session exists
   */
  static isAuthenticated() {
    return !!this.getCurrentUser();
  }

  /**
   * Retrieves the currently active logged-in user
   */
  static getCurrentUser() {
    if (this.activeUserUid === null) {
      return null;
    }
    let activeUid = this.activeUserUid;
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem(ACTIVE_USER_KEY);
      if (stored === 'guest') {
        return null;
      }
      if (stored && mockDb.data.users[stored]) {
        activeUid = stored;
      }
    }
    return mockDb.data.users[activeUid] || mockDb.data.users['demo_player'];
  }

  /**
   * Switches the active logged-in user session
   */
  static setCurrentUser(uid) {
    if (uid === null || uid === 'guest') {
      this.activeUserUid = null;
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(ACTIVE_USER_KEY, 'guest');
      }
      return null;
    }

    if (!mockDb.data.users[uid]) {
      throw new Error(`User ID ${uid} does not exist in the database.`);
    }
    this.activeUserUid = uid;
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(ACTIVE_USER_KEY, uid);
    }
    return mockDb.data.users[uid];
  }

  /**
   * Signs in a user by phone, email, or UID
   */
  static login(identifier) {
    if (!identifier || !identifier.trim()) {
      throw new Error("Phone number, email, or User ID is required.");
    }
    const clean = identifier.trim().toLowerCase();

    // Check direct UID
    if (mockDb.data.users[clean]) {
      return this.setCurrentUser(clean);
    }

    // Search by phone or email
    const user = Object.values(mockDb.data.users).find(u => 
      (u.phone && u.phone.replace(/[\s+-]/g, '').includes(clean.replace(/[\s+-]/g, ''))) ||
      (u.email && u.email.toLowerCase() === clean) ||
      (u.displayName && u.displayName.toLowerCase() === clean)
    );

    if (!user) {
      throw new Error(`No registered account found matching "${identifier}". Please check your details or register below.`);
    }

    return this.setCurrentUser(user.uid);
  }

  /**
   * Signs out the current user session
   */
  static logout() {
    this.setCurrentUser('guest');
  }

  /**
   * Retrieves all users registered in the system
   */
  static getAllUsers() {
    return Object.values(mockDb.data.users);
  }

  /**
   * Retrieves user by UID
   */
  static getUserById(uid) {
    return mockDb.data.users[uid] || null;
  }

  /**
   * Creates a new user profile and persists it to the database
   */
  static createUser({
    displayName,
    email,
    phone,
    role = 'player', // 'player' | 'venue_owner' | 'admin'
    city = 'Thane',
    venueId = null,
    isPhoneVerified = true,
    isEmailVerified = true,
    otpSessionId = null
  }) {
    if (!displayName || !displayName.trim()) {
      throw new Error("Display Name is required.");
    }
    if (!phone || !phone.trim()) {
      throw new Error("Phone number is required.");
    }

    const cleanName = displayName.trim();
    const cleanPhone = phone.trim();
    const cleanEmail = (email && email.trim()) || `${cleanName.toLowerCase().replace(/\s+/g, '.')}${Math.floor(10 + Math.random() * 90)}@apex.dev`;

    // Generate unique UID
    const slug = cleanName.toLowerCase().replace(/[^a-z0-9]/g, '_');
    const uid = `usr_${slug}_${Math.floor(1000 + Math.random() * 9000)}`;

    // Generate referral code for players
    const firstWord = cleanName.split(' ')[0].toUpperCase().replace(/[^A-Z]/g, '');
    const referralCode = `${firstWord.slice(0, 5)}${Math.floor(10 + Math.random() * 90)}`;

    const newUser = {
      uid,
      displayName: cleanName,
      email: cleanEmail,
      phone: cleanPhone,
      role,
      city,
      isPhoneVerified: !!isPhoneVerified,
      isEmailVerified: !!isEmailVerified,
      otpSessionId,
      loyaltyPoints: role === 'player' ? 100 : 0, // 100 Welcome Points for players!
      referralCode,
      venueId: role === 'venue_owner' ? venueId : null,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };

    // Persist into database
    mockDb.data.users[uid] = newUser;

    // Append to audit log
    mockDb.data.audit_logs.push({
      logId: `audit_${Date.now()}`,
      action: 'USER_CREATED',
      actorId: uid,
      resourceType: 'user',
      resourceId: uid,
      metadata: { role, city, isPhoneVerified, isEmailVerified },
      timestamp: Date.now()
    });

    mockDb.persist();

    // Auto-login as the newly created user
    this.setCurrentUser(uid);

    return newUser;
  }

  /**
   * Registers a Facility Owner and automatically creates and persists their sports venue
   */
  static registerFacilityOwner({
    displayName,
    phone,
    email,
    city = 'Thane',
    facilityName,
    area,
    address,
    sports = ['cricket', 'football'],
    startingPrice = 1200,
    amenities = ['Floodlights', 'Changing Rooms', 'Air-conditioned Dugout', 'Parking'],
    isPhoneVerified = true,
    isEmailVerified = true,
    otpSessionId = null
  }) {
    if (!facilityName || !facilityName.trim()) {
      throw new Error("Facility / Turf Name is required.");
    }
    if (!area || !area.trim()) {
      throw new Error("Facility Area / Locality is required.");
    }

    // 1. Create the user as venue_owner
    const ownerUser = this.createUser({
      displayName,
      phone,
      email,
      role: 'venue_owner',
      city,
      isPhoneVerified,
      isEmailVerified,
      otpSessionId
    });

    // 2. Provision and persist the new sports venue in the database
    const newVenue = VenueService.createVenue({
      name: facilityName.trim(),
      city,
      area: area.trim(),
      address: address ? address.trim() : `${area.trim()}, ${city}, Maharashtra`,
      sports,
      amenities,
      startingPrice: Number(startingPrice) || 1200,
      ownerId: ownerUser.uid
    });

    // 3. Link venueId back to the owner profile
    ownerUser.venueId = newVenue.venueId;
    mockDb.data.users[ownerUser.uid].venueId = newVenue.venueId;
    mockDb.persist();

    return {
      user: ownerUser,
      venue: newVenue
    };
  }

  /**
   * Updates an existing user's details
   */
  static updateUser(uid, updates) {
    const user = mockDb.data.users[uid];
    if (!user) {
      throw new Error(`User ${uid} not found.`);
    }

    Object.assign(user, updates, { updatedAt: Date.now() });
    mockDb.persist();
    return user;
  }
}
