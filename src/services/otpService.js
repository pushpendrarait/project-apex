/**
 * Project Apex - Dual Mobile & Email OTP Authentication Service
 * Zero Cloud Functions Architecture: Secure client-side OTP dispatch, lease TTL, and verification
 * Enforces dual verification: Mobile SMS OTP + Email Verification Code
 */

import { mockDb } from './firebaseConfig.js';
import { UserService } from './userService.js';

const OTP_TTL_MS = 5 * 60 * 1000; // 5 minutes TTL
const OTP_SESSION_KEY = 'apex_active_otp_session';

export class OtpService {
  /**
   * Generates a 6-digit numeric OTP code
   */
  static generateOtpCode() {
    return String(Math.floor(100000 + Math.random() * 900000));
  }

  /**
   * Formats Indian mobile number to E.164-compatible (+91 XXXXX XXXXX)
   */
  static normalizePhone(phone) {
    if (!phone) return '';
    const digitsOnly = phone.replace(/[^0-9]/g, '');
    if (digitsOnly.length === 10) {
      return `+91 ${digitsOnly.slice(0, 5)} ${digitsOnly.slice(5)}`;
    }
    if (digitsOnly.length === 12 && digitsOnly.startsWith('91')) {
      return `+91 ${digitsOnly.slice(2, 7)} ${digitsOnly.slice(7)}`;
    }
    return phone.trim();
  }

  /**
   * Validates email format
   */
  static isValidEmail(email) {
    if (!email) return false;
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email.trim());
  }

  /**
   * Validates Indian mobile number
   */
  static isValidPhone(phone) {
    if (!phone) return false;
    const digitsOnly = phone.replace(/[^0-9]/g, '');
    return digitsOnly.length >= 10;
  }

  /**
   * Phase 1: Dispatches Dual OTPs for Registration (Mobile SMS + Email Verification)
   */
  static async sendRegistrationOtp({ phone, email, displayName }) {
    if (!phone || !this.isValidPhone(phone)) {
      throw new Error("A valid 10-digit mobile number (+91) is required for OTP verification.");
    }

    if (!email || !this.isValidEmail(email)) {
      throw new Error("A valid email address is required for email verification OTP.");
    }

    const cleanPhone = this.normalizePhone(phone);
    const cleanEmail = email.trim().toLowerCase();

    // Check if phone or email already registered
    const existingUser = Object.values(mockDb.data.users).find(u => 
      (u.email && u.email.toLowerCase() === cleanEmail) ||
      (u.phone && this.normalizePhone(u.phone) === cleanPhone)
    );

    if (existingUser) {
      throw new Error(`An account is already registered with ${cleanPhone === this.normalizePhone(existingUser.phone) ? 'this mobile number' : 'this email'}. Please sign in instead.`);
    }

    const now = Date.now();
    const sessionId = `otp_reg_${now}_${Math.floor(100 + Math.random() * 900)}`;
    const phoneOtp = this.generateOtpCode();
    const emailOtp = this.generateOtpCode();

    if (!mockDb.data.otp_verifications) {
      mockDb.data.otp_verifications = {};
    }

    const sessionData = {
      sessionId,
      phone: cleanPhone,
      email: cleanEmail,
      displayName: (displayName || '').trim(),
      phoneOtp,
      emailOtp,
      phoneVerified: false,
      emailVerified: false,
      attemptsRemaining: 3,
      createdAt: now,
      expiresAt: now + OTP_TTL_MS,
      purpose: 'REGISTRATION'
    };

    mockDb.data.otp_verifications[sessionId] = sessionData;

    // Log simulated notifications
    const simulatedSms = `[SMS to ${cleanPhone}]: Your Project Apex Mobile OTP is ${phoneOtp} (Valid for 5 mins). Do not share this code.`;
    const simulatedEmail = `[Email to ${cleanEmail}]: Your Project Apex Email Verification Code is ${emailOtp}. Enter this to verify your email.`;

    console.log(`📱 ${simulatedSms}`);
    console.log(`📧 ${simulatedEmail}`);

    // Audit log
    mockDb.data.audit_logs.push({
      logId: `audit_otp_${now}`,
      action: 'OTP_DISPATCHED',
      actorId: 'system_auth',
      resourceType: 'otp_session',
      resourceId: sessionId,
      metadata: { phone: cleanPhone, email: cleanEmail, purpose: 'REGISTRATION' },
      timestamp: now
    });

    mockDb.persist();

    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(OTP_SESSION_KEY, JSON.stringify(sessionData));
    }

    return {
      success: true,
      sessionId,
      phone: cleanPhone,
      email: cleanEmail,
      phoneOtp, // Exposed for preview & fast-testing auto-fill in demo environment
      emailOtp, // Exposed for preview & fast-testing auto-fill in demo environment
      expiresInSeconds: Math.floor(OTP_TTL_MS / 1000),
      simulatedMessages: {
        sms: simulatedSms,
        email: simulatedEmail
      }
    };
  }

  /**
   * Phase 2: Validates Dual Registration OTPs (both Phone and Email must be correct)
   */
  static verifyRegistrationOtp({ sessionId, phoneOtp, emailOtp }) {
    if (!sessionId) {
      throw new Error("Active OTP session ID is missing.");
    }

    if (!mockDb.data.otp_verifications) {
      mockDb.data.otp_verifications = {};
    }

    let session = mockDb.data.otp_verifications[sessionId];
    if (!session && typeof localStorage !== 'undefined') {
      try {
        const stored = JSON.parse(localStorage.getItem(OTP_SESSION_KEY));
        if (stored && stored.sessionId === sessionId) {
          session = stored;
          mockDb.data.otp_verifications[sessionId] = session;
        }
      } catch (e) {
        // ignore parse error
      }
    }

    if (!session) {
      throw new Error("OTP verification session not found or has expired. Please request a new OTP.");
    }

    const now = Date.now();
    if (session.expiresAt < now) {
      throw new Error("OTP has expired (5-minute window passed). Please click 'Resend OTP'.");
    }

    if (session.attemptsRemaining <= 0) {
      throw new Error("Maximum invalid OTP attempts exceeded. Please request a fresh OTP.");
    }

    const cleanInputPhoneOtp = String(phoneOtp || '').trim();
    const cleanInputEmailOtp = String(emailOtp || '').trim();

    const isPhoneMatch = cleanInputPhoneOtp === session.phoneOtp;
    const isEmailMatch = cleanInputEmailOtp === session.emailOtp;

    if (!isPhoneMatch || !isEmailMatch) {
      session.attemptsRemaining -= 1;
      mockDb.persist();

      if (!isPhoneMatch && !isEmailMatch) {
        throw new Error(`Both Mobile OTP and Email Code are incorrect. (${session.attemptsRemaining} attempts left)`);
      } else if (!isPhoneMatch) {
        throw new Error(`Invalid Mobile OTP code. (${session.attemptsRemaining} attempts left)`);
      } else {
        throw new Error(`Invalid Email verification code. (${session.attemptsRemaining} attempts left)`);
      }
    }

    // Both OTPs verified!
    session.phoneVerified = true;
    session.emailVerified = true;
    session.verifiedAt = now;

    mockDb.data.audit_logs.push({
      logId: `audit_otp_ver_${now}`,
      action: 'OTP_VERIFIED_SUCCESS',
      actorId: 'system_auth',
      resourceType: 'otp_session',
      resourceId: sessionId,
      metadata: { phone: session.phone, email: session.email },
      timestamp: now
    });

    mockDb.persist();

    return {
      success: true,
      verified: true,
      sessionId,
      phone: session.phone,
      email: session.email,
      displayName: session.displayName
    };
  }

  /**
   * Passwordless Sign-In via OTP (Mobile or Email)
   */
  static async sendLoginOtp(identifier) {
    if (!identifier || !identifier.trim()) {
      throw new Error("Please enter your registered mobile number or email.");
    }
    const clean = identifier.trim().toLowerCase();

    const user = Object.values(mockDb.data.users).find(u => 
      (u.email && u.email.toLowerCase() === clean) ||
      (u.phone && u.phone.replace(/[\s+-]/g, '').includes(clean.replace(/[\s+-]/g, '')))
    );

    if (!user) {
      throw new Error(`No registered account found matching "${identifier}". Please register as a new user.`);
    }

    const now = Date.now();
    const sessionId = `otp_login_${now}_${Math.floor(100 + Math.random() * 900)}`;
    const otp = this.generateOtpCode();

    if (!mockDb.data.otp_verifications) {
      mockDb.data.otp_verifications = {};
    }

    const sessionData = {
      sessionId,
      userId: user.uid,
      target: user.phone || user.email,
      otp,
      createdAt: now,
      expiresAt: now + OTP_TTL_MS,
      attemptsRemaining: 3,
      purpose: 'LOGIN'
    };

    mockDb.data.otp_verifications[sessionId] = sessionData;
    mockDb.persist();

    const simulatedMsg = `[Apex Security] Your Login OTP is ${otp} for ${user.displayName}. Valid for 5 minutes.`;
    console.log(simulatedMsg);

    return {
      success: true,
      sessionId,
      user,
      otp, // For demo preview
      target: user.phone,
      simulatedMessage: simulatedMsg,
      expiresInSeconds: Math.floor(OTP_TTL_MS / 1000)
    };
  }

  /**
   * Verifies Passwordless Login OTP and starts session
   */
  static verifyLoginOtp({ sessionId, otp }) {
    if (!mockDb.data.otp_verifications || !mockDb.data.otp_verifications[sessionId]) {
      throw new Error("Invalid or expired OTP session.");
    }

    const session = mockDb.data.otp_verifications[sessionId];
    const now = Date.now();

    if (session.expiresAt < now) {
      throw new Error("Login OTP expired. Please request a new code.");
    }

    if (String(otp).trim() !== session.otp) {
      session.attemptsRemaining -= 1;
      mockDb.persist();
      throw new Error(`Incorrect Login OTP. (${session.attemptsRemaining} attempts remaining)`);
    }

    const user = mockDb.data.users[session.userId];
    if (!user) {
      throw new Error("User record not found.");
    }

    UserService.setCurrentUser(user.uid);

    return {
      success: true,
      user
    };
  }
}
