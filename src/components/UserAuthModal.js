/**
 * Project Apex - Authentication & Registration Modal
 * BookMyShow & Snabbit Look & Feel
 * Dual Password & Cryptographic Dual OTP Authentication Flow
 * Fully Integrated with Java Spring Boot Middleware (Port 8080)
 */

import { UserService } from '../services/userService.js';
import { OtpService } from '../services/otpService.js';
import { ApiClient } from '../services/apiClient.js';

export class UserAuthModal {
  static async show({ initialTab = 'signin', onUserChanged, onNavigate }) {
    let modal = document.getElementById('apex-user-auth-modal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'apex-user-auth-modal';
      modal.className = 'fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-fade-in';
      document.body.appendChild(modal);
    }

    const currentUser = UserService.getCurrentUser();

    // Check backend health
    let backendStatus = await ApiClient.checkHealth();

    // State for login and registration flows
    let currentMode = ['owner', 'player', 'register', 'signup'].includes(initialTab) ? 'signup' : 'signin';
    let signinSubMode = 'password'; // 'password' | 'otp'
    let signupRole = initialTab === 'owner' ? 'owner' : 'player';
    let pendingRegistration = null;
    let activeOtpSession = null;
    let loginOtpSession = null;
    let otpCountdownInterval = null;
    let remainingOtpSeconds = 300;

    modal.innerHTML = `
      <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-xl w-full p-6 sm:p-8 shadow-2xl relative overflow-hidden text-white flex flex-col max-h-[92vh]">
        
        <!-- Modal Header -->
        <div class="flex items-center justify-between pb-3 border-b border-slate-800">
          <div>
            <div class="flex items-center gap-2 mb-1">
              <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold uppercase tracking-wider">
                ⚡ Project Apex Identity & Access
              </span>
              <!-- Live Backend Health Indicator -->
              <span id="auth-backend-pill" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-mono border ${backendStatus.isOnline ? 'bg-emerald-950/80 text-emerald-300 border-emerald-500/40' : 'bg-amber-950/80 text-amber-300 border-amber-500/40'}">
                <span class="w-1.5 h-1.5 rounded-full ${backendStatus.isOnline ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'}"></span>
                <span>${backendStatus.isOnline ? 'Java Backend: Connected' : 'Cloud Firestore Mode'}</span>
              </span>
            </div>
            <h3 class="text-2xl font-black text-white" id="modal-heading">
              ${currentMode === 'signin' ? 'Account Sign In' : 'Create Account'}
            </h3>
            <p class="text-xs text-slate-400" id="modal-subheading">
              ${currentMode === 'signin' ? 'Sign in with your Password or instant Mobile/Email OTP' : 'Register with verified Mobile (+91), Email & Password'}
            </p>
          </div>
          <button id="btn-close-auth-modal" class="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition" aria-label="Close">
            ✕
          </button>
        </div>

        <!-- Primary Mode Switcher (Login vs Sign Up) -->
        <div id="auth-mode-container" class="flex gap-2 p-1.5 bg-slate-950 rounded-2xl border border-slate-800 my-3 text-xs font-bold">
          <button id="mode-btn-signin" class="flex-1 py-2.5 rounded-xl transition ${currentMode === 'signin' ? 'bg-emerald-500 text-slate-950 shadow-md font-black' : 'text-slate-400 hover:text-white'}">
            🔑 Sign In
          </button>
          <button id="mode-btn-signup" class="flex-1 py-2.5 rounded-xl transition ${currentMode === 'signup' ? 'bg-emerald-500 text-slate-950 shadow-md font-black' : 'text-slate-400 hover:text-white'}">
            ✨ Create Account
          </button>
        </div>

        <!-- Global Feedback Message Box -->
        <div id="auth-feedback" class="text-xs hidden p-3 rounded-xl mb-3"></div>

        <!-- ============================================== -->
        <!-- 1. SIGN IN SECTION                             -->
        <!-- ============================================== -->
        <div id="section-login" class="${currentMode === 'signin' ? '' : 'hidden'} overflow-y-auto space-y-4 pr-1 text-xs">
          
          ${currentUser ? `
            <!-- Currently Logged In Banner -->
            <div class="p-3.5 rounded-2xl bg-slate-950/80 border border-emerald-500/30 flex items-center justify-between gap-3">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 font-bold flex items-center justify-center text-sm">
                  ${currentUser.displayName.slice(0, 2).toUpperCase()}
                </div>
                <div>
                  <div class="font-bold text-white text-sm flex items-center gap-1.5">
                    <span>${currentUser.displayName}</span>
                    <span class="text-[9px] uppercase px-1.5 py-0.2 rounded font-mono ${ApiClient.isOwnerRole(currentUser.role) ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/40' : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40'}">
                      ${ApiClient.isOwnerRole(currentUser.role) ? 'Facility Owner' : 'Player'}
                    </span>
                  </div>
                  <div class="text-[11px] text-slate-400">${currentUser.phone || ''} • ${currentUser.email || ''}</div>
                </div>
              </div>
              <button type="button" id="btn-signout-active" class="px-3 py-1.5 rounded-xl bg-red-950/60 hover:bg-red-900/60 text-red-300 border border-red-800/60 text-xs font-bold transition">
                🚪 Sign Out
              </button>
            </div>
          ` : ''}

          <!-- Sign-in Sub-Method Selector: Password vs OTP -->
          <div class="flex gap-2 p-1 bg-slate-950 rounded-xl border border-slate-800 text-xs font-bold">
            <button type="button" id="btn-sub-password" class="flex-1 py-1.5 rounded-lg transition ${signinSubMode === 'password' ? 'bg-slate-800 text-white font-bold' : 'text-slate-400 hover:text-white'}">
              🔐 Password Login
            </button>
            <button type="button" id="btn-sub-otp" class="flex-1 py-1.5 rounded-lg transition ${signinSubMode === 'otp' ? 'bg-slate-800 text-white font-bold' : 'text-slate-400 hover:text-white'}">
              📱 Instant OTP Login
            </button>
          </div>

          <!-- A. PASSWORD LOGIN FORM -->
          <form id="form-password-login" class="${signinSubMode === 'password' ? '' : 'hidden'} bg-slate-950/80 p-5 rounded-2xl border border-slate-800 space-y-3.5 shadow-lg">
            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">
                Mobile Number or Email *
              </label>
              <input id="input-pw-login-identifier" type="text" required placeholder="e.g. +91 98201 12345 or rohan.deshmukh@apex.dev" class="w-full bg-slate-900 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition font-mono" />
            </div>

            <div>
              <div class="flex items-center justify-between mb-1">
                <label class="block font-bold uppercase tracking-wider text-slate-300">
                  Password *
                </label>
                <span class="text-[10px] text-slate-400">Demo: password123</span>
              </div>
              <div class="relative">
                <input id="input-pw-login-password" type="password" required placeholder="Enter your password" class="w-full bg-slate-900 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
                <button type="button" id="btn-toggle-login-pw" class="absolute right-3 top-2.5 text-xs text-slate-400 hover:text-white" title="Show/Hide Password">👁️</button>
              </div>
            </div>

            <button type="submit" id="btn-submit-password-login" class="w-full py-3 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-400 hover:from-emerald-400 hover:to-teal-300 text-slate-950 font-black text-xs shadow-md shadow-emerald-500/20 transition active:scale-95 flex items-center justify-center gap-1.5">
              <span>Sign In to Account →</span>
            </button>
          </form>

          <!-- B. OTP LOGIN FORM -->
          <div id="box-otp-login" class="${signinSubMode === 'otp' ? '' : 'hidden'} bg-slate-950/80 p-5 rounded-2xl border border-slate-800 space-y-3.5 shadow-lg">
            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">
                Mobile Number or Email *
              </label>
              <p class="text-[11px] text-slate-400 mb-2">We will send a 6-digit one-time password (OTP) to your phone or email.</p>
              <div class="flex flex-col sm:flex-row gap-2">
                <input id="input-login-identifier" type="text" placeholder="e.g. +91 98201 12345 or user@example.com" class="flex-1 bg-slate-900 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition font-mono" />
                <button id="btn-request-login-otp" class="px-5 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold transition whitespace-nowrap shadow-md shadow-emerald-500/20">
                  Send OTP →
                </button>
              </div>
            </div>

            <!-- OTP Input Box (Revealed after OTP dispatch) -->
            <div id="container-login-otp-verify" class="hidden space-y-3 pt-3 border-t border-slate-800">
              <div class="p-3 rounded-xl bg-emerald-950/70 border border-emerald-500/40 text-emerald-300 flex items-center justify-between gap-2 shadow-inner">
                <div class="space-y-0.5">
                  <span class="text-[9px] uppercase font-bold text-slate-400 block font-sans">📱 SIMULATED OTP DISPATCH</span>
                  <span id="login-otp-preview-text" class="text-xs text-white font-mono">OTP Code generated</span>
                </div>
                <button type="button" id="btn-autofill-login-otp" class="px-2.5 py-1 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 text-[10px] font-bold border border-emerald-500/40 transition">
                  ⚡ Auto-Fill OTP
                </button>
              </div>

              <div>
                <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Enter 6-Digit Login OTP *</label>
                <div class="flex gap-2">
                  <input id="input-login-otp-code" type="text" maxlength="6" placeholder="• • • • • •" class="flex-1 bg-slate-900 border border-emerald-500/50 rounded-xl px-4 py-2.5 text-center text-lg font-mono tracking-widest font-bold text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none shadow-sm" />
                  <button id="btn-verify-login-otp" class="px-6 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-400 hover:from-emerald-400 hover:to-teal-300 text-slate-950 font-black transition shadow-lg shadow-emerald-500/20">
                    Verify & Sign In
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Switch to Sign Up link -->
          <div class="text-center pt-2">
            <span class="text-slate-400">Don't have an account?</span>
            <button type="button" id="btn-switch-to-signup" class="text-emerald-400 hover:text-emerald-300 font-bold ml-1 underline transition">
              Create an Account (Sign Up)
            </button>
          </div>
        </div>

        <!-- ============================================== -->
        <!-- 2. CREATE ACCOUNT SECTION                      -->
        <!-- ============================================== -->
        <div id="section-signup" class="${currentMode === 'signup' ? '' : 'hidden'} overflow-y-auto space-y-4 pr-1 text-xs">
          
          <!-- Role Selector (Player vs Facility Owner) -->
          <div class="flex gap-2 p-1 bg-slate-950 rounded-xl border border-slate-800 text-xs font-bold">
            <button type="button" id="role-btn-player" class="flex-1 py-2 rounded-lg transition ${signupRole === 'player' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 font-bold' : 'text-slate-400 hover:text-white'}">
              🏏 Player Account (+100 Pts)
            </button>
            <button type="button" id="role-btn-owner" class="flex-1 py-2 rounded-lg transition ${signupRole === 'owner' ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/40 font-bold' : 'text-slate-400 hover:text-white'}">
              🏢 Facility Owner Account
            </button>
          </div>

          <!-- A. REGISTER AS PLAYER FORM -->
          <form id="form-register-player" class="${signupRole === 'player' ? '' : 'hidden'} space-y-3.5">
            <div class="bg-gradient-to-r from-emerald-950/60 to-slate-950 p-3 rounded-2xl border border-emerald-500/30 flex items-center gap-2.5 text-emerald-300 text-[11px]">
              <span class="text-xl">🎁</span>
              <div>
                <strong class="text-white block font-bold">Player Welcome Package</strong>
                <span>Instant <strong>100 Turf Loyalty Points</strong> (₹100 value) + Personal Referral Code</span>
              </div>
            </div>

            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Full Name *</label>
              <input id="input-player-name" type="text" required placeholder="e.g. Hardik Pandya" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Mobile Number (SMS OTP) *</label>
                <input id="input-player-phone" type="tel" required placeholder="+91 98765 43210" value="+91 " class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
              </div>

              <div>
                <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">City / Region *</label>
                <select id="input-player-city" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition">
                  <option value="Thane" selected>Thane (Ghodbunder, Majiwada)</option>
                  <option value="Mumbai">Mumbai (Bandra, Powai, BKC)</option>
                  <option value="Navi Mumbai">Navi Mumbai (Vashi, Nerul)</option>
                </select>
              </div>
            </div>

            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Email Address (Email OTP) *</label>
              <input id="input-player-email" type="email" required placeholder="e.g. hardik.pandya@example.com" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
            </div>

            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Create Password *</label>
              <div class="relative">
                <input id="input-player-password" type="password" required minlength="6" placeholder="Min. 6 characters" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
                <button type="button" class="btn-toggle-reg-pw absolute right-3 top-2.5 text-xs text-slate-400 hover:text-white" title="Show/Hide">👁️</button>
              </div>
            </div>

            <div class="pt-2">
              <button type="submit" class="w-full py-3.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-400 hover:from-emerald-400 hover:to-teal-300 text-slate-950 font-black text-xs shadow-lg shadow-emerald-500/20 transition active:scale-95 flex items-center justify-center gap-1.5">
                <span>Continue to Dual OTP Verification →</span>
              </button>
            </div>
          </form>

          <!-- B. REGISTER AS FACILITY OWNER FORM -->
          <form id="form-register-owner" class="${signupRole === 'owner' ? '' : 'hidden'} space-y-3.5">
            <div class="bg-gradient-to-r from-indigo-950/60 to-slate-950 p-3 rounded-2xl border border-indigo-500/30 flex items-center gap-2.5 text-indigo-300 text-[11px]">
              <span class="text-xl">🏢</span>
              <div>
                <strong class="text-white block font-bold">Facility Partner Onboarding</strong>
                <span>List your sports venue, eliminate double-bookings & monetize idle off-peak hours</span>
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Owner / Manager Full Name *</label>
                <input id="input-owner-name" type="text" required placeholder="e.g. Mahesh Patil" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
              </div>

              <div>
                <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Mobile Number (SMS OTP) *</label>
                <input id="input-owner-phone" type="tel" required placeholder="+91 98765 43210" value="+91 " class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2 text-xs text-white font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Email Address *</label>
                <input id="input-owner-email" type="email" required placeholder="e.g. mahesh@sportsarena.com" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
              </div>

              <div>
                <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Create Password *</label>
                <div class="relative">
                  <input id="input-owner-password" type="password" required minlength="6" placeholder="Min. 6 characters" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
                  <button type="button" class="btn-toggle-reg-pw absolute right-3 top-2 text-xs text-slate-400 hover:text-white" title="Show/Hide">👁️</button>
                </div>
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Facility / Turf Name *</label>
                <input id="input-facility-name" type="text" required placeholder="e.g. Apex Thunder Turf Ghodbunder" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
              </div>

              <div>
                <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">City / Region *</label>
                <select id="input-facility-city" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition">
                  <option value="Thane" selected>Thane</option>
                  <option value="Mumbai">Mumbai</option>
                  <option value="Navi Mumbai">Navi Mumbai</option>
                </select>
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Area / Locality *</label>
                <input id="input-facility-area" type="text" required placeholder="e.g. Ghodbunder Road, Majiwada" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
              </div>

              <div>
                <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Base Hourly Rate (₹ / hr) *</label>
                <input id="input-facility-price" type="number" required min="300" step="50" value="1200" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2 text-xs text-white font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
              </div>
            </div>

            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1.5">Sports Available at Facility *</label>
              <div class="grid grid-cols-3 gap-2 text-xs">
                <label class="flex items-center gap-1.5 p-2 rounded-xl bg-slate-950 border border-slate-800 cursor-pointer">
                  <input type="checkbox" name="owner-sport" value="cricket" checked class="rounded text-emerald-500 focus:ring-0" />
                  <span>🏏 Cricket</span>
                </label>
                <label class="flex items-center gap-1.5 p-2 rounded-xl bg-slate-950 border border-slate-800 cursor-pointer">
                  <input type="checkbox" name="owner-sport" value="football" checked class="rounded text-emerald-500 focus:ring-0" />
                  <span>⚽ Football</span>
                </label>
                <label class="flex items-center gap-1.5 p-2 rounded-xl bg-slate-950 border border-slate-800 cursor-pointer">
                  <input type="checkbox" name="owner-sport" value="badminton" class="rounded text-emerald-500 focus:ring-0" />
                  <span>🏸 Badminton</span>
                </label>
                <label class="flex items-center gap-1.5 p-2 rounded-xl bg-slate-950 border border-slate-800 cursor-pointer">
                  <input type="checkbox" name="owner-sport" value="pickleball" class="rounded text-emerald-500 focus:ring-0" />
                  <span>🏓 Pickleball</span>
                </label>
                <label class="flex items-center gap-1.5 p-2 rounded-xl bg-slate-950 border border-slate-800 cursor-pointer">
                  <input type="checkbox" name="owner-sport" value="squash" class="rounded text-emerald-500 focus:ring-0" />
                  <span>🎾 Squash</span>
                </label>
                <label class="flex items-center gap-1.5 p-2 rounded-xl bg-slate-950 border border-slate-800 cursor-pointer">
                  <input type="checkbox" name="owner-sport" value="tennis" class="rounded text-emerald-500 focus:ring-0" />
                  <span>🎾 Tennis</span>
                </label>
              </div>
            </div>

            <div class="pt-2">
              <button type="submit" class="w-full py-3.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-xs shadow-lg shadow-indigo-600/20 transition active:scale-95 flex items-center justify-center gap-1.5">
                <span>Continue to Dual OTP Verification →</span>
              </button>
            </div>
          </form>

          <!-- Switch to Sign In link -->
          <div class="text-center pt-2">
            <span class="text-slate-400">Already registered?</span>
            <button type="button" id="btn-switch-to-signin" class="text-emerald-400 hover:text-emerald-300 font-bold ml-1 underline transition">
              Sign In to Your Account
            </button>
          </div>
        </div>

        <!-- ============================================== -->
        <!-- 3. DUAL OTP VERIFICATION STEP (MOBILE + EMAIL) -->
        <!-- ============================================== -->
        <div id="section-otp-verify" class="hidden overflow-y-auto space-y-4 pr-1 text-xs">
          <div class="bg-gradient-to-r from-emerald-950/80 via-slate-950 to-indigo-950/80 p-4 rounded-2xl border border-emerald-500/40 space-y-2">
            <div class="flex items-center justify-between">
              <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-bold text-[10px] uppercase tracking-wider">
                🔐 Dual OTP Authentication
              </span>
              <span id="otp-timer-badge" class="font-mono text-amber-400 font-bold text-xs">05:00</span>
            </div>
            <h4 class="font-black text-white text-sm">Verify Mobile Number & Email Address</h4>
            <p class="text-[11px] text-slate-300">
              Both your mobile number and email must be verified to activate your account in Firestore.
            </p>
          </div>

          <!-- Simulated SMS & Email Dispatch Preview Cards -->
          <div class="space-y-2 font-mono text-[11px]">
            <div class="p-3 rounded-xl bg-slate-950 border border-emerald-500/40 text-emerald-300 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 shadow-inner">
              <div class="space-y-0.5">
                <span class="text-[9px] uppercase font-bold text-slate-400 block font-sans">📱 SIMULATED SMS GATEWAY DISPATCH</span>
                <span id="disp-sms-msg" class="text-xs text-white">SMS Code sent to your phone</span>
              </div>
              <button type="button" id="btn-autofill-sms" class="px-2.5 py-1 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 text-[10px] font-bold border border-emerald-500/40 transition">
                ⚡ Auto-Fill SMS
              </button>
            </div>

            <div class="p-3 rounded-xl bg-slate-950 border border-indigo-500/40 text-indigo-300 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 shadow-inner">
              <div class="space-y-0.5">
                <span class="text-[9px] uppercase font-bold text-slate-400 block font-sans">📧 SIMULATED EMAIL DISPATCH</span>
                <span id="disp-email-msg" class="text-xs text-white">Verification Code sent to your email</span>
              </div>
              <button type="button" id="btn-autofill-email" class="px-2.5 py-1 rounded-lg bg-indigo-500/20 hover:bg-indigo-500/30 text-indigo-300 text-[10px] font-bold border border-indigo-500/40 transition">
                ⚡ Auto-Fill Email
              </button>
            </div>
          </div>

          <!-- Dual OTP Input Form -->
          <form id="form-verify-dual-otp" class="space-y-3.5 pt-2">
            <div>
              <div class="flex items-center justify-between mb-1">
                <label class="block font-bold uppercase tracking-wider text-slate-300">
                  Mobile SMS OTP (6 Digits) *
                </label>
                <span class="text-[10px] text-slate-400" id="otp-phone-label"></span>
              </div>
              <input id="input-verify-phone-otp" type="text" maxlength="6" required placeholder="• • • • • •" class="w-full bg-slate-950 border border-emerald-500/50 rounded-xl px-4 py-2.5 text-center text-base font-mono tracking-widest font-bold text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none shadow-sm" />
            </div>

            <div>
              <div class="flex items-center justify-between mb-1">
                <label class="block font-bold uppercase tracking-wider text-slate-300">
                  Email Verification Code (6 Digits) *
                </label>
                <span class="text-[10px] text-slate-400" id="otp-email-label"></span>
              </div>
              <input id="input-verify-email-otp" type="text" maxlength="6" required placeholder="• • • • • •" class="w-full bg-slate-950 border border-indigo-500/50 rounded-xl px-4 py-2.5 text-center text-base font-mono tracking-widest font-bold text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none shadow-sm" />
            </div>

            <div class="flex items-center justify-between pt-1 text-xs">
              <button type="button" id="btn-otp-back" class="text-slate-400 hover:text-white transition">
                ← Edit Phone / Email
              </button>
              <button type="button" id="btn-otp-resend" class="text-amber-400 hover:text-amber-300 font-bold transition">
                Resend OTPs
              </button>
            </div>

            <div class="pt-2">
              <button type="submit" id="btn-submit-verify-otp" class="w-full py-3.5 rounded-xl bg-gradient-to-r from-emerald-500 to-indigo-600 hover:opacity-95 text-white font-black text-xs shadow-lg transition active:scale-95 flex items-center justify-center gap-2">
                <span>✓ Verify Both OTPs & Activate Account</span>
              </button>
            </div>
          </form>
        </div>

      </div>
    `;

    const close = () => {
      if (otpCountdownInterval) clearInterval(otpCountdownInterval);
      modal.remove();
    };

    modal.querySelector('#btn-close-auth-modal').addEventListener('click', close);

    // DOM Elements
    const modeContainer = modal.querySelector('#auth-mode-container');
    const modeBtnSignin = modal.querySelector('#mode-btn-signin');
    const modeBtnSignup = modal.querySelector('#mode-btn-signup');

    const headingEl = modal.querySelector('#modal-heading');
    const subheadingEl = modal.querySelector('#modal-subheading');

    const secLogin = modal.querySelector('#section-login');
    const secSignup = modal.querySelector('#section-signup');
    const secOtpVerify = modal.querySelector('#section-otp-verify');
    const feedback = modal.querySelector('#auth-feedback');

    const roleBtnPlayer = modal.querySelector('#role-btn-player');
    const roleBtnOwner = modal.querySelector('#role-btn-owner');
    const formPlayer = modal.querySelector('#form-register-player');
    const formOwner = modal.querySelector('#form-register-owner');

    const btnSubPassword = modal.querySelector('#btn-sub-password');
    const btnSubOtp = modal.querySelector('#btn-sub-otp');
    const formPasswordLogin = modal.querySelector('#form-password-login');
    const boxOtpLogin = modal.querySelector('#box-otp-login');

    // Switch between Password and OTP Login sub-tabs
    const setSigninSubMode = (subMode) => {
      signinSubMode = subMode;
      feedback.classList.add('hidden');
      if (subMode === 'password') {
        btnSubPassword.className = 'flex-1 py-1.5 rounded-lg transition bg-slate-800 text-white font-bold';
        btnSubOtp.className = 'flex-1 py-1.5 rounded-lg transition text-slate-400 hover:text-white';
        formPasswordLogin.classList.remove('hidden');
        boxOtpLogin.classList.add('hidden');
      } else {
        btnSubOtp.className = 'flex-1 py-1.5 rounded-lg transition bg-slate-800 text-white font-bold';
        btnSubPassword.className = 'flex-1 py-1.5 rounded-lg transition text-slate-400 hover:text-white';
        boxOtpLogin.classList.remove('hidden');
        formPasswordLogin.classList.add('hidden');
      }
    };

    btnSubPassword.addEventListener('click', () => setSigninSubMode('password'));
    btnSubOtp.addEventListener('click', () => setSigninSubMode('otp'));

    // Toggle password visibility
    const setupPwToggle = (btnId, inputId) => {
      const btn = modal.querySelector(btnId);
      const input = modal.querySelector(inputId);
      if (btn && input) {
        btn.addEventListener('click', () => {
          input.type = input.type === 'password' ? 'text' : 'password';
        });
      }
    };
    setupPwToggle('#btn-toggle-login-pw', '#input-pw-login-password');
    modal.querySelectorAll('.btn-toggle-reg-pw').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const inp = e.currentTarget.parentElement.querySelector('input');
        if (inp) inp.type = inp.type === 'password' ? 'text' : 'password';
      });
    });

    // Switch between Login and Sign Up
    const setMode = (mode) => {
      currentMode = mode;
      feedback.classList.add('hidden');
      modeContainer.classList.remove('hidden');
      secOtpVerify.classList.add('hidden');

      if (mode === 'signin') {
        modeBtnSignin.className = 'flex-1 py-2.5 rounded-xl transition bg-emerald-500 text-slate-950 shadow-md font-black';
        modeBtnSignup.className = 'flex-1 py-2.5 rounded-xl transition text-slate-400 hover:text-white';
        headingEl.textContent = 'Account Sign In';
        subheadingEl.textContent = 'Sign in with your Password or instant Mobile/Email OTP';
        secLogin.classList.remove('hidden');
        secSignup.classList.add('hidden');
      } else {
        modeBtnSignup.className = 'flex-1 py-2.5 rounded-xl transition bg-emerald-500 text-slate-950 shadow-md font-black';
        modeBtnSignin.className = 'flex-1 py-2.5 rounded-xl transition text-slate-400 hover:text-white';
        headingEl.textContent = 'Create Account';
        subheadingEl.textContent = 'Register with verified Mobile (+91), Email & Password';
        secSignup.classList.remove('hidden');
        secLogin.classList.add('hidden');
      }
    };

    // Switch between Player and Owner registration inside Sign Up
    const setRole = (role) => {
      signupRole = role;
      feedback.classList.add('hidden');
      if (role === 'player') {
        roleBtnPlayer.className = 'flex-1 py-2 rounded-lg transition bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 font-bold';
        roleBtnOwner.className = 'flex-1 py-2 rounded-lg transition text-slate-400 hover:text-white';
        formPlayer.classList.remove('hidden');
        formOwner.classList.add('hidden');
      } else {
        roleBtnOwner.className = 'flex-1 py-2 rounded-lg transition bg-indigo-500/20 text-indigo-300 border border-indigo-500/40 font-bold';
        roleBtnPlayer.className = 'flex-1 py-2 rounded-lg transition text-slate-400 hover:text-white';
        formOwner.classList.remove('hidden');
        formPlayer.classList.add('hidden');
      }
    };

    modeBtnSignin.addEventListener('click', () => setMode('signin'));
    modeBtnSignup.addEventListener('click', () => setMode('signup'));

    const btnSwitchToSignup = modal.querySelector('#btn-switch-to-signup');
    if (btnSwitchToSignup) btnSwitchToSignup.addEventListener('click', () => setMode('signup'));

    const btnSwitchToSignin = modal.querySelector('#btn-switch-to-signin');
    if (btnSwitchToSignin) btnSwitchToSignin.addEventListener('click', () => setMode('signin'));

    roleBtnPlayer.addEventListener('click', () => setRole('player'));
    roleBtnOwner.addEventListener('click', () => setRole('owner'));

    // Sign out button
    const btnSignoutActive = modal.querySelector('#btn-signout-active');
    if (btnSignoutActive) {
      btnSignoutActive.addEventListener('click', () => {
        UserService.logout();
        ApiClient.setToken(null);
        feedback.className = 'text-xs p-3 rounded-xl bg-slate-800 text-slate-300 block';
        feedback.textContent = '✓ Signed out successfully. You are now in Guest mode.';
        setTimeout(() => {
          close();
          if (onUserChanged) onUserChanged(null);
        }, 500);
      });
    }

    // -------------------------------------------------------------
    // HANDLE PASSWORD SIGN IN (Sends to Java Spring Boot Middleware)
    // -------------------------------------------------------------
    formPasswordLogin.addEventListener('submit', async (e) => {
      e.preventDefault();
      const identifier = modal.querySelector('#input-pw-login-identifier').value.trim();
      const password = modal.querySelector('#input-pw-login-password').value;
      const submitBtn = modal.querySelector('#btn-submit-password-login');

      submitBtn.disabled = true;
      submitBtn.innerHTML = `<span class="animate-spin mr-2">⏳</span> Verifying Credentials...`;

      try {
        const result = await UserService.loginWithPassword({ identifier, password });
        feedback.className = 'text-xs p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 block';
        feedback.textContent = result.source === 'backend'
          ? `✓ Authenticated via Java Spring Boot Middleware! Welcome back, ${result.user.displayName}.`
          : `✓ Signed in successfully! Welcome back, ${result.user.displayName}.`;

        setTimeout(() => {
          close();
          if (onUserChanged) onUserChanged(result.user);
          if (ApiClient.isOwnerRole(result.user.role) && onNavigate) {
            onNavigate('partner');
          } else if (onNavigate) {
            onNavigate('bookings');
          }
        }, 700);
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `❌ ${err.message}`;
        submitBtn.disabled = false;
        submitBtn.innerHTML = `Sign In to Account →`;
      }
    });

    // 5-min timer for OTP verification
    const startOtpTimer = () => {
      if (otpCountdownInterval) clearInterval(otpCountdownInterval);
      remainingOtpSeconds = 300;
      const timerEl = modal.querySelector('#otp-timer-badge');

      otpCountdownInterval = setInterval(() => {
        remainingOtpSeconds = Math.max(0, remainingOtpSeconds - 1);
        const mins = String(Math.floor(remainingOtpSeconds / 60)).padStart(2, '0');
        const secs = String(remainingOtpSeconds % 60).padStart(2, '0');
        if (timerEl) timerEl.textContent = `${mins}:${secs}`;

        if (remainingOtpSeconds <= 0) {
          clearInterval(otpCountdownInterval);
          feedback.className = 'text-xs p-3 rounded-xl bg-amber-950/80 border border-amber-500/50 text-amber-300 block';
          feedback.textContent = '⚠️ OTP codes have expired. Please click "Resend OTPs" to generate fresh codes.';
        }
      }, 1000);
    };

    // Open Dual OTP Verification View
    const openOtpVerificationScreen = (otpResult, registrationPayload) => {
      pendingRegistration = registrationPayload;
      activeOtpSession = otpResult;

      feedback.classList.add('hidden');
      modeContainer.classList.add('hidden');
      secLogin.classList.add('hidden');
      secSignup.classList.add('hidden');
      secOtpVerify.classList.remove('hidden');

      headingEl.textContent = 'Verify Credentials';
      subheadingEl.textContent = 'Enter the codes sent to your mobile phone and email';

      modal.querySelector('#disp-sms-msg').textContent = `Code: ${otpResult.phoneOtp} (Sent to ${otpResult.phone})`;
      modal.querySelector('#disp-email-msg').textContent = `Code: ${otpResult.emailOtp} (Sent to ${otpResult.email})`;
      modal.querySelector('#otp-phone-label').textContent = `Sent to ${otpResult.phone}`;
      modal.querySelector('#otp-email-label').textContent = `Sent to ${otpResult.email}`;

      const phoneInput = modal.querySelector('#input-verify-phone-otp');
      const emailInput = modal.querySelector('#input-verify-email-otp');
      phoneInput.value = '';
      emailInput.value = '';
      phoneInput.focus();

      startOtpTimer();
    };

    // Auto-fill buttons in registration OTP view
    modal.querySelector('#btn-autofill-sms').addEventListener('click', () => {
      if (activeOtpSession) {
        modal.querySelector('#input-verify-phone-otp').value = activeOtpSession.phoneOtp;
      }
    });

    modal.querySelector('#btn-autofill-email').addEventListener('click', () => {
      if (activeOtpSession) {
        modal.querySelector('#input-verify-email-otp').value = activeOtpSession.emailOtp;
      }
    });

    // Back button in OTP view
    modal.querySelector('#btn-otp-back').addEventListener('click', () => {
      setMode('signup');
      if (pendingRegistration) {
        setRole(pendingRegistration.type === 'owner' ? 'owner' : 'player');
      }
    });

    // Resend OTP button
    modal.querySelector('#btn-otp-resend').addEventListener('click', async () => {
      if (!pendingRegistration) return;
      try {
        const freshOtp = await OtpService.sendRegistrationOtp({
          phone: pendingRegistration.phone,
          email: pendingRegistration.email,
          displayName: pendingRegistration.displayName
        });
        openOtpVerificationScreen(freshOtp, pendingRegistration);
        feedback.className = 'text-xs p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 block';
        feedback.textContent = '✓ Fresh 6-digit OTP codes dispatched via SMS and Email.';
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `⚠️ ${err.message}`;
      }
    });

    // -------------------------------------------------------------
    // HANDLE DUAL OTP VERIFICATION SUBMISSION & REGISTRATION
    // -------------------------------------------------------------
    modal.querySelector('#form-verify-dual-otp').addEventListener('submit', async (e) => {
      e.preventDefault();
      const phoneOtp = modal.querySelector('#input-verify-phone-otp').value.trim();
      const emailOtp = modal.querySelector('#input-verify-email-otp').value.trim();
      const submitBtn = modal.querySelector('#btn-submit-verify-otp');

      if (!phoneOtp || !emailOtp) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = 'Please enter both the 6-digit Mobile SMS OTP and the 6-digit Email Verification Code.';
        return;
      }

      submitBtn.disabled = true;
      submitBtn.innerHTML = `<span class="animate-spin mr-2">⏳</span> Registering in Database...`;

      try {
        // 1. Verify OTPs
        const verResult = OtpService.verifyRegistrationOtp({
          sessionId: activeOtpSession.sessionId,
          phoneOtp,
          emailOtp
        });

        // 2. Register via Java Spring Boot backend & persist to Firestore
        let registeredUser = null;
        if (pendingRegistration.type === 'player') {
          const regRes = await UserService.registerWithPassword({
            displayName: pendingRegistration.displayName,
            phone: pendingRegistration.phone,
            city: pendingRegistration.city,
            email: pendingRegistration.email,
            password: pendingRegistration.password,
            role: 'player'
          });
          registeredUser = regRes.user;

          feedback.className = 'text-xs p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 block';
          feedback.textContent = `✓ Account & Password Activated! Welcome ${registeredUser.displayName}! Awarded 100 bonus loyalty credits.`;

          setTimeout(() => {
            close();
            if (onUserChanged) onUserChanged(registeredUser);
            if (onNavigate) onNavigate('bookings');
          }, 900);

        } else if (pendingRegistration.type === 'owner') {
          const regRes = await UserService.registerWithPassword({
            displayName: pendingRegistration.displayName,
            phone: pendingRegistration.phone,
            email: pendingRegistration.email,
            password: pendingRegistration.password,
            city: pendingRegistration.city,
            role: 'venue_owner',
            facilityName: pendingRegistration.facilityName,
            area: pendingRegistration.area,
            sports: pendingRegistration.sports,
            startingPrice: pendingRegistration.startingPrice
          });
          registeredUser = regRes.user;

          feedback.className = 'text-xs p-3 rounded-xl bg-indigo-950/80 border border-indigo-500/50 text-indigo-300 block';
          feedback.textContent = `✓ Partner Account Activated! Facility "${pendingRegistration.facilityName}" provisioned in ${pendingRegistration.city}.`;

          setTimeout(() => {
            close();
            if (onUserChanged) onUserChanged(registeredUser);
            if (onNavigate) onNavigate('partner');
          }, 900);
        }

      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `❌ ${err.message}`;
        submitBtn.disabled = false;
        submitBtn.innerHTML = `✓ Verify Both OTPs & Activate Account`;
      }
    });

    // -------------------------------------------------------------
    // HANDLE REGISTER PLAYER INITIAL FORM
    // -------------------------------------------------------------
    formPlayer.addEventListener('submit', async (e) => {
      e.preventDefault();
      const displayName = modal.querySelector('#input-player-name').value.trim();
      const phone = modal.querySelector('#input-player-phone').value.trim();
      const city = modal.querySelector('#input-player-city').value;
      const email = modal.querySelector('#input-player-email').value.trim();
      const password = modal.querySelector('#input-player-password').value;

      if (!password || password.length < 6) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = 'Password must be at least 6 characters.';
        return;
      }

      try {
        const otpResult = await OtpService.sendRegistrationOtp({
          phone,
          email,
          displayName
        });

        openOtpVerificationScreen(otpResult, {
          type: 'player',
          displayName,
          phone: otpResult.phone,
          city,
          email: otpResult.email,
          password
        });
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `⚠️ ${err.message}`;
      }
    });

    // -------------------------------------------------------------
    // HANDLE REGISTER OWNER INITIAL FORM
    // -------------------------------------------------------------
    formOwner.addEventListener('submit', async (e) => {
      e.preventDefault();
      const displayName = modal.querySelector('#input-owner-name').value.trim();
      const phone = modal.querySelector('#input-owner-phone').value.trim();
      const email = modal.querySelector('#input-owner-email').value.trim();
      const password = modal.querySelector('#input-owner-password').value;
      const city = modal.querySelector('#input-facility-city').value;
      const facilityName = modal.querySelector('#input-facility-name').value.trim();
      const area = modal.querySelector('#input-facility-area').value.trim();
      const startingPrice = modal.querySelector('#input-facility-price').value;

      const sports = Array.from(modal.querySelectorAll('input[name="owner-sport"]:checked')).map(el => el.value);

      if (!password || password.length < 6) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = 'Password must be at least 6 characters.';
        return;
      }

      if (sports.length === 0) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = 'Please select at least one sport available at your facility.';
        return;
      }

      try {
        const otpResult = await OtpService.sendRegistrationOtp({
          phone,
          email,
          displayName
        });

        openOtpVerificationScreen(otpResult, {
          type: 'owner',
          displayName,
          phone: otpResult.phone,
          email: otpResult.email,
          password,
          city,
          facilityName,
          area,
          sports,
          startingPrice: Number(startingPrice) || 1200
        });
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `⚠️ ${err.message}`;
      }
    });

    // -------------------------------------------------------------
    // HANDLE LOGIN: REQUEST OTP
    // -------------------------------------------------------------
    modal.querySelector('#btn-request-login-otp').addEventListener('click', async () => {
      const idInput = modal.querySelector('#input-login-identifier');
      const val = idInput.value.trim();
      if (!val) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = 'Please enter your registered mobile number or email.';
        return;
      }

      try {
        loginOtpSession = await OtpService.sendLoginOtp(val);
        const verifyBox = modal.querySelector('#container-login-otp-verify');
        verifyBox.classList.remove('hidden');

        modal.querySelector('#login-otp-preview-text').textContent = `Login OTP is ${loginOtpSession.otp} (Sent to ${loginOtpSession.target})`;
        modal.querySelector('#input-login-otp-code').value = loginOtpSession.otp;
        feedback.className = 'text-xs p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 block';
        feedback.textContent = `✓ 6-digit login OTP sent to ${loginOtpSession.target}`;
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `⚠️ ${err.message}`;
      }
    });

    // Auto-fill login OTP
    modal.querySelector('#btn-autofill-login-otp').addEventListener('click', () => {
      if (loginOtpSession) {
        modal.querySelector('#input-login-otp-code').value = loginOtpSession.otp;
      }
    });

    // -------------------------------------------------------------
    // HANDLE LOGIN: VERIFY OTP
    // -------------------------------------------------------------
    modal.querySelector('#btn-verify-login-otp').addEventListener('click', async () => {
      if (!loginOtpSession) return;
      const otpCode = modal.querySelector('#input-login-otp-code').value.trim();

      try {
        // Try Java backend OTP verification first
        try {
          await ApiClient.verifyLoginOtp({
            sessionId: loginOtpSession.sessionId,
            otp: otpCode
          });
        } catch (e) {
          console.warn('[UserAuthModal] Java backend OTP verify fallback:', e.message);
        }

        const res = OtpService.verifyLoginOtp({
          sessionId: loginOtpSession.sessionId,
          otp: otpCode
        });

        feedback.className = 'text-xs p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 block';
        feedback.textContent = `✓ Welcome back, ${res.user.displayName}! Signed in successfully.`;

        setTimeout(() => {
          close();
          if (onUserChanged) onUserChanged(res.user);
          if (ApiClient.isOwnerRole(res.user.role) && onNavigate) {
            onNavigate('partner');
          } else if (onNavigate) {
            onNavigate('bookings');
          }
        }, 700);
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `❌ ${err.message}`;
      }
    });
  }
}
