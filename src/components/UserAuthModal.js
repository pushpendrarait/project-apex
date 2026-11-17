/**
 * Project Apex - Authentication & Registration Modal
 * Multi-role onboarding with Mandatory Dual Mobile & Email OTP Verification
 */

import { UserService } from '../services/userService.js';
import { OtpService } from '../services/otpService.js';

export class UserAuthModal {
  static show({ initialTab = 'signin', onUserChanged, onNavigate }) {
    let modal = document.getElementById('apex-user-auth-modal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'apex-user-auth-modal';
      modal.className = 'fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-fade-in';
      document.body.appendChild(modal);
    }

    const currentUser = UserService.getCurrentUser();
    const allUsers = UserService.getAllUsers();

    // State for OTP registration flow
    let pendingRegistration = null;
    let activeOtpSession = null;
    let otpCountdownInterval = null;
    let remainingOtpSeconds = 300;

    modal.innerHTML = `
      <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-xl w-full p-6 sm:p-8 shadow-2xl relative overflow-hidden text-white flex flex-col max-h-[92vh]">
        
        <!-- Header -->
        <div class="flex items-center justify-between pb-4 border-b border-slate-800">
          <div>
            <div class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold uppercase tracking-wider mb-1">
              ⚡ Project Apex Identity & Access
            </div>
            <h3 class="text-2xl font-black text-white" id="modal-heading">Account Access</h3>
            <p class="text-xs text-slate-400">Sign in or register with verified Mobile (+91) & Email OTP</p>
          </div>
          <button id="btn-close-auth-modal" class="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition">
            ✕
          </button>
        </div>

        <!-- Role / Mode Tabs -->
        <div id="auth-tabs-container" class="flex gap-1.5 p-1.5 bg-slate-950 rounded-2xl border border-slate-800 my-4 text-xs font-bold">
          <button id="tab-btn-signin" class="flex-1 py-2 rounded-xl transition ${initialTab === 'signin' ? 'bg-emerald-500 text-slate-950 shadow-md font-black' : 'text-slate-400 hover:text-white'}">
            🔑 Sign In
          </button>
          <button id="tab-btn-player" class="flex-1 py-2 rounded-xl transition ${initialTab === 'player' ? 'bg-emerald-500 text-slate-950 shadow-md font-black' : 'text-slate-400 hover:text-white'}">
            🏏 New Player
          </button>
          <button id="tab-btn-owner" class="flex-1 py-2 rounded-xl transition ${initialTab === 'owner' ? 'bg-indigo-600 text-white shadow-md font-black' : 'text-slate-400 hover:text-white'}">
            🏢 Facility Owner
          </button>
        </div>

        <!-- Global Feedback Message Box -->
        <div id="auth-feedback" class="text-xs hidden p-3 rounded-xl mb-3"></div>

        <!-- 1. SIGN IN SECTION -->
        <div id="section-signin" class="${initialTab === 'signin' ? '' : 'hidden'} overflow-y-auto space-y-4 pr-1 text-xs">
          <!-- Passwordless Login Option -->
          <div class="bg-slate-950/60 p-4 rounded-2xl border border-slate-800 space-y-3">
            <div class="flex items-center justify-between">
              <span class="font-bold text-white text-xs flex items-center gap-1.5">
                <span>📱 Passwordless OTP Sign In</span>
              </span>
              <span class="text-[10px] text-emerald-400 font-mono">Instant SMS/Email</span>
            </div>
            <div class="flex flex-col sm:flex-row gap-2">
              <input id="input-login-otp-id" type="text" placeholder="Enter Mobile (+91) or Email" class="flex-1 bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
              <button id="btn-send-login-otp" class="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold transition">
                Send OTP
              </button>
            </div>

            <!-- OTP Input Box for Login -->
            <div id="container-login-otp-verify" class="hidden space-y-2 pt-2 border-t border-slate-800">
              <div id="login-otp-preview" class="text-[11px] p-2.5 rounded-xl bg-indigo-950/70 border border-indigo-500/30 text-indigo-300 font-mono"></div>
              <div class="flex gap-2">
                <input id="input-login-otp-code" type="text" maxlength="6" placeholder="Enter 6-digit OTP" class="flex-1 bg-slate-900 border border-slate-700 rounded-xl px-4 py-2 text-center text-sm font-mono tracking-widest text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none" />
                <button id="btn-confirm-login-otp" class="px-5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold transition">
                  Verify & Sign In
                </button>
              </div>
            </div>
          </div>

          <!-- Standard Identifier Sign In Form -->
          <form id="form-signin" class="space-y-3">
            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-400 mb-1">Or Quick Sign In by Name / Account ID</label>
              <input id="input-login-id" type="text" placeholder="e.g. Rohan or demo_player" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
            </div>

            <button type="submit" class="w-full py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-xs shadow-lg shadow-emerald-500/20 transition active:scale-95">
              Sign In →
            </button>
          </form>

          <!-- 1-Click Fast Profile Switcher -->
          <div class="pt-3 border-t border-slate-800 space-y-2">
            <div class="flex items-center justify-between">
              <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">Fast Profile Switcher:</span>
              <button type="button" id="btn-signout-guest" class="text-[10px] text-slate-400 hover:text-rose-400 transition underline">
                Sign Out (Guest Mode)
              </button>
            </div>
            <div class="space-y-1.5 max-h-40 overflow-y-auto pr-1">
              ${allUsers.map(u => {
                const isActive = currentUser && u.uid === currentUser.uid;
                const isOwner = u.role === 'venue_owner';

                return `
                  <div class="p-2 rounded-xl border transition-all flex items-center justify-between ${isActive ? 'bg-emerald-950/60 border-emerald-500/80 shadow-md' : 'bg-slate-950/60 border-slate-800 hover:border-slate-700'}">
                    <div class="flex items-center gap-2">
                      <div class="w-7 h-7 rounded-lg bg-slate-800 border border-slate-700 text-emerald-400 font-bold flex items-center justify-center text-[10px]">
                        ${u.displayName.slice(0, 2).toUpperCase()}
                      </div>
                      <div>
                        <div class="font-bold text-white flex items-center gap-1.5">
                          <span class="text-xs">${u.displayName}</span>
                          <span class="text-[8px] uppercase px-1.5 py-0.2 rounded font-mono ${isOwner ? 'bg-indigo-500/20 text-indigo-300' : 'bg-emerald-500/20 text-emerald-400'}">
                            ${isOwner ? 'Owner' : 'Player'}
                          </span>
                        </div>
                        <div class="text-[10px] text-slate-400">${u.phone}</div>
                      </div>
                    </div>

                    <div>
                      ${isActive ? `
                        <span class="text-[10px] font-bold text-emerald-400 px-2 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/30">Active</span>
                      ` : `
                        <button data-uid="${u.uid}" class="btn-fast-login px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs transition">
                          Switch
                        </button>
                      `}
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          </div>
        </div>

        <!-- 2. REGISTER AS PLAYER SECTION -->
        <form id="form-register-player" class="${initialTab === 'player' ? '' : 'hidden'} overflow-y-auto space-y-3.5 pr-1 text-xs">
          <!-- Perks Banner -->
          <div class="bg-gradient-to-r from-emerald-950/60 to-slate-950 p-3 rounded-2xl border border-emerald-500/30 flex items-center gap-2.5 text-emerald-300 text-[11px]">
            <span class="text-xl">🎁</span>
            <div>
              <strong class="text-white block font-bold">Player Welcome Package</strong>
              <span>Instant <strong>100 Turf Loyalty Points</strong> (₹100 value) + Personal Referral Code</span>
            </div>
          </div>

          <div>
            <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Full Name *</label>
            <input id="input-player-name" type="text" required placeholder="e.g. Sahil Shinde" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
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
                <option value="Mumbai">Mumbai (Bandra, Powai, Andheri)</option>
                <option value="Navi Mumbai">Navi Mumbai (Vashi, Nerul)</option>
              </select>
            </div>
          </div>

          <div>
            <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Email Address (Email OTP) *</label>
            <input id="input-player-email" type="email" required placeholder="e.g. sahil.shinde@example.com" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
            <span class="text-[10px] text-slate-500 mt-0.5 block">A 6-digit verification code will be sent to this email</span>
          </div>

          <div class="pt-2">
            <button type="submit" class="w-full py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-xs shadow-lg shadow-emerald-500/20 transition active:scale-95 flex items-center justify-center gap-1.5">
              <span>Send Mobile & Email Verification OTPs →</span>
            </button>
          </div>
        </form>

        <!-- 3. REGISTER AS FACILITY OWNER SECTION -->
        <form id="form-register-owner" class="${initialTab === 'owner' ? '' : 'hidden'} overflow-y-auto space-y-3.5 pr-1 text-xs">
          <!-- Perks Banner -->
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
              <input id="input-owner-phone" type="tel" required placeholder="+91 98200 11223" value="+91 " class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2 text-xs text-white font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
            </div>
          </div>

          <div>
            <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Business Email Address (Email OTP) *</label>
            <input id="input-owner-email" type="email" required placeholder="e.g. contact@patilsports.com" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
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

          <!-- Sports selection -->
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
            <button type="submit" class="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-xs shadow-lg shadow-indigo-600/20 transition active:scale-95 flex items-center justify-center gap-1.5">
              <span>Send Partner Verification OTPs →</span>
            </button>
          </div>
        </form>

        <!-- 4. DUAL OTP VERIFICATION STEP (MOBILE + EMAIL) -->
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
              Enter the 6-digit codes sent to your phone and email to confirm your registration.
            </p>
          </div>

          <!-- Simulated SMS & Email Dispatch Preview Cards -->
          <div class="space-y-2 font-mono text-[11px]">
            <!-- SMS Card -->
            <div class="p-3 rounded-xl bg-slate-950 border border-emerald-500/40 text-emerald-300 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 shadow-inner">
              <div class="space-y-0.5">
                <span class="text-[9px] uppercase font-bold text-slate-400 block font-sans">📱 SIMULATED SMS GATEWAY DISPATCH</span>
                <span id="disp-sms-msg" class="text-xs text-white">SMS Code sent to your phone</span>
              </div>
              <button type="button" id="btn-autofill-sms" class="px-2.5 py-1 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 text-[10px] font-bold border border-emerald-500/40 transition">
                ⚡ Auto-Fill SMS OTP
              </button>
            </div>

            <!-- Email Card -->
            <div class="p-3 rounded-xl bg-slate-950 border border-indigo-500/40 text-indigo-300 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 shadow-inner">
              <div class="space-y-0.5">
                <span class="text-[9px] uppercase font-bold text-slate-400 block font-sans">📧 SIMULATED EMAIL DISPATCH</span>
                <span id="disp-email-msg" class="text-xs text-white">Email Code sent to your inbox</span>
              </div>
              <button type="button" id="btn-autofill-email" class="px-2.5 py-1 rounded-lg bg-indigo-500/20 hover:bg-indigo-500/30 text-indigo-300 text-[10px] font-bold border border-indigo-500/40 transition">
                ⚡ Auto-Fill Email Code
              </button>
            </div>
          </div>

          <!-- Dual OTP Input Form -->
          <form id="form-verify-dual-otp" class="space-y-4 pt-1">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <!-- Mobile OTP Input -->
              <div class="space-y-1">
                <label class="block font-bold uppercase tracking-wider text-emerald-400 text-[11px]">
                  1. Mobile SMS OTP (6 Digits) *
                </label>
                <input id="input-verify-phone-otp" type="text" maxlength="6" required placeholder="e.g. 123456" class="w-full bg-slate-950 border border-emerald-500/50 focus:border-emerald-400 rounded-xl px-4 py-3 text-center text-lg font-mono font-bold tracking-widest text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition shadow-sm" />
                <span id="otp-phone-label" class="text-[10px] text-slate-400 block font-mono">Sent to Mobile</span>
              </div>

              <!-- Email OTP Input -->
              <div class="space-y-1">
                <label class="block font-bold uppercase tracking-wider text-indigo-400 text-[11px]">
                  2. Email Verification Code *
                </label>
                <input id="input-verify-email-otp" type="text" maxlength="6" required placeholder="e.g. 654321" class="w-full bg-slate-950 border border-indigo-500/50 focus:border-indigo-400 rounded-xl px-4 py-3 text-center text-lg font-mono font-bold tracking-widest text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none transition shadow-sm" />
                <span id="otp-email-label" class="text-[10px] text-slate-400 block font-mono">Sent to Email</span>
              </div>
            </div>

            <div class="flex items-center justify-between text-xs pt-1">
              <button type="button" id="btn-otp-back" class="text-slate-400 hover:text-white transition">
                ← Back to Details
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

    // Elements
    const tabsContainer = modal.querySelector('#auth-tabs-container');
    const tabSignin = modal.querySelector('#tab-btn-signin');
    const tabPlayer = modal.querySelector('#tab-btn-player');
    const tabOwner = modal.querySelector('#tab-btn-owner');

    const secSignin = modal.querySelector('#section-signin');
    const formPlayer = modal.querySelector('#form-register-player');
    const formOwner = modal.querySelector('#form-register-owner');
    const secOtpVerify = modal.querySelector('#section-otp-verify');
    const feedback = modal.querySelector('#auth-feedback');

    const setTab = (target) => {
      feedback.classList.add('hidden');
      tabsContainer.classList.remove('hidden');
      [tabSignin, tabPlayer, tabOwner].forEach(t => {
        t.className = 'flex-1 py-2 rounded-xl transition text-slate-400 hover:text-white';
      });
      [secSignin, formPlayer, formOwner, secOtpVerify].forEach(s => s.classList.add('hidden'));

      if (target === 'signin') {
        tabSignin.className = 'flex-1 py-2 rounded-xl transition bg-emerald-500 text-slate-950 shadow-md font-black';
        secSignin.classList.remove('hidden');
      } else if (target === 'player') {
        tabPlayer.className = 'flex-1 py-2 rounded-xl transition bg-emerald-500 text-slate-950 shadow-md font-black';
        formPlayer.classList.remove('hidden');
      } else if (target === 'owner') {
        tabOwner.className = 'flex-1 py-2 rounded-xl transition bg-indigo-600 text-white shadow-md font-black';
        formOwner.classList.remove('hidden');
      }
    };

    tabSignin.addEventListener('click', () => setTab('signin'));
    tabPlayer.addEventListener('click', () => setTab('player'));
    tabOwner.addEventListener('click', () => setTab('owner'));

    // Start 5-min timer for OTP verification
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
      tabsContainer.classList.add('hidden');
      [secSignin, formPlayer, formOwner].forEach(s => s.classList.add('hidden'));
      secOtpVerify.classList.remove('hidden');

      // Populate preview cards
      modal.querySelector('#disp-sms-msg').textContent = `Code: ${otpResult.phoneOtp} (Sent to ${otpResult.phone})`;
      modal.querySelector('#disp-email-msg').textContent = `Code: ${otpResult.emailOtp} (Sent to ${otpResult.email})`;
      modal.querySelector('#otp-phone-label').textContent = `Sent to ${otpResult.phone}`;
      modal.querySelector('#otp-email-label').textContent = `Sent to ${otpResult.email}`;

      // Reset inputs
      const phoneInput = modal.querySelector('#input-verify-phone-otp');
      const emailInput = modal.querySelector('#input-verify-email-otp');
      phoneInput.value = '';
      emailInput.value = '';
      phoneInput.focus();

      startOtpTimer();
    };

    // Auto-fill buttons
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
      if (pendingRegistration) {
        setTab(pendingRegistration.type === 'owner' ? 'owner' : 'player');
      } else {
        setTab('player');
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

    // Handle Dual OTP Verification Submission
    modal.querySelector('#form-verify-dual-otp').addEventListener('submit', async (e) => {
      e.preventDefault();
      const phoneOtp = modal.querySelector('#input-verify-phone-otp').value;
      const emailOtp = modal.querySelector('#input-verify-email-otp').value;

      try {
        // 1. Verify OTPs atomically
        const verResult = OtpService.verifyRegistrationOtp({
          sessionId: activeOtpSession.sessionId,
          phoneOtp,
          emailOtp
        });

        // 2. Commit User Registration
        if (pendingRegistration.type === 'player') {
          const newUser = UserService.createUser({
            displayName: pendingRegistration.displayName,
            phone: pendingRegistration.phone,
            city: pendingRegistration.city,
            email: pendingRegistration.email,
            role: 'player',
            isPhoneVerified: true,
            isEmailVerified: true,
            otpSessionId: verResult.sessionId
          });

          feedback.className = 'text-xs p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 block';
          feedback.textContent = `✓ Mobile & Email Verified! Welcome ${newUser.displayName}! Awarded 100 bonus loyalty credits.`;

          setTimeout(() => {
            close();
            if (onUserChanged) onUserChanged(newUser);
          }, 900);

        } else if (pendingRegistration.type === 'owner') {
          const result = UserService.registerFacilityOwner({
            displayName: pendingRegistration.displayName,
            phone: pendingRegistration.phone,
            email: pendingRegistration.email,
            city: pendingRegistration.city,
            facilityName: pendingRegistration.facilityName,
            area: pendingRegistration.area,
            sports: pendingRegistration.sports,
            startingPrice: pendingRegistration.startingPrice,
            isPhoneVerified: true,
            isEmailVerified: true,
            otpSessionId: verResult.sessionId
          });

          feedback.className = 'text-xs p-3 rounded-xl bg-indigo-950/80 border border-indigo-500/50 text-indigo-300 block';
          feedback.textContent = `✓ Partner Verified! Facility "${result.venue.name}" created and published in ${result.venue.city}.`;

          setTimeout(() => {
            close();
            if (onUserChanged) onUserChanged(result.user);
            if (onNavigate) onNavigate('partner');
          }, 900);
        }

      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `❌ ${err.message}`;
      }
    });

    // Handle Register Player Initial Form (dispatches OTP)
    formPlayer.addEventListener('submit', async (e) => {
      e.preventDefault();
      const displayName = modal.querySelector('#input-player-name').value;
      const phone = modal.querySelector('#input-player-phone').value;
      const city = modal.querySelector('#input-player-city').value;
      const email = modal.querySelector('#input-player-email').value;

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
          email: otpResult.email
        });
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `⚠️ ${err.message}`;
      }
    });

    // Handle Register Facility Owner Initial Form (dispatches OTP)
    formOwner.addEventListener('submit', async (e) => {
      e.preventDefault();
      const displayName = modal.querySelector('#input-owner-name').value;
      const phone = modal.querySelector('#input-owner-phone').value;
      const email = modal.querySelector('#input-owner-email').value;
      const city = modal.querySelector('#input-facility-city').value;
      const facilityName = modal.querySelector('#input-facility-name').value;
      const area = modal.querySelector('#input-facility-area').value;
      const startingPrice = modal.querySelector('#input-facility-price').value;

      const sports = Array.from(modal.querySelectorAll('input[name="owner-sport"]:checked')).map(el => el.value);

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

    // Handle Passwordless Login OTP Send
    let loginOtpSession = null;
    modal.querySelector('#btn-send-login-otp').addEventListener('click', async () => {
      const idInput = modal.querySelector('#input-login-otp-id');
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

        modal.querySelector('#login-otp-preview').textContent = `⚡ Simulated SMS/Email: Your Login OTP is ${loginOtpSession.otp}`;
        modal.querySelector('#input-login-otp-code').value = loginOtpSession.otp; // Auto-populate for frictionless dev testing
        feedback.className = 'text-xs p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 block';
        feedback.textContent = `✓ 6-digit login OTP sent to ${loginOtpSession.target}`;
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `⚠️ ${err.message}`;
      }
    });

    // Handle Passwordless Login OTP Verify
    modal.querySelector('#btn-confirm-login-otp').addEventListener('click', () => {
      if (!loginOtpSession) return;
      const otpCode = modal.querySelector('#input-login-otp-code').value;

      try {
        const res = OtpService.verifyLoginOtp({
          sessionId: loginOtpSession.sessionId,
          otp: otpCode
        });

        feedback.className = 'text-xs p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 block';
        feedback.textContent = `✓ Welcome back, ${res.user.displayName}! Signed in successfully.`;

        setTimeout(() => {
          close();
          if (onUserChanged) onUserChanged(res.user);
          if (res.user.role === 'venue_owner' && onNavigate) {
            onNavigate('partner');
          }
        }, 700);
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `❌ ${err.message}`;
      }
    });

    // Handle Standard Sign In Form
    modal.querySelector('#form-signin').addEventListener('submit', (e) => {
      e.preventDefault();
      const loginId = modal.querySelector('#input-login-id').value;

      try {
        const user = UserService.login(loginId);
        feedback.className = 'text-xs p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 block';
        feedback.textContent = `✓ Signed in successfully as ${user.displayName} (${user.role})!`;

        setTimeout(() => {
          close();
          if (onUserChanged) onUserChanged(user);
          if (user.role === 'venue_owner' && onNavigate) {
            onNavigate('partner');
          }
        }, 700);
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `⚠️ ${err.message}`;
      }
    });

    // Handle 1-Click Fast Login
    modal.querySelectorAll('.btn-fast-login').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const uid = e.currentTarget.getAttribute('data-uid');
        const user = UserService.setCurrentUser(uid);
        close();
        if (onUserChanged) onUserChanged(user);
        if (user.role === 'venue_owner' && onNavigate) {
          onNavigate('partner');
        }
      });
    });

    // Handle Signout Guest
    const btnSignout = modal.querySelector('#btn-signout-guest');
    if (btnSignout) {
      btnSignout.addEventListener('click', () => {
        UserService.setCurrentUser('guest');
        feedback.className = 'text-xs p-3 rounded-xl bg-slate-800 text-slate-300 block';
        feedback.textContent = 'Signed out. Operating in Guest mode.';
        setTimeout(() => {
          close();
          if (onUserChanged) onUserChanged(null);
        }, 500);
      });
    }
  }
}
