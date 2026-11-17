/**
 * Project Apex - Authentication & Registration Modal
 * Multi-role onboarding: Sign In, Register as Player, or Register as Facility Owner
 */

import { UserService } from '../services/userService.js';

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

    modal.innerHTML = `
      <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-xl w-full p-6 sm:p-8 shadow-2xl relative overflow-hidden text-white flex flex-col max-h-[92vh]">
        
        <!-- Header -->
        <div class="flex items-center justify-between pb-4 border-b border-slate-800">
          <div>
            <div class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold uppercase tracking-wider mb-1">
              ⚡ Project Apex Identity & Access
            </div>
            <h3 class="text-2xl font-black text-white" id="modal-heading">Account Access</h3>
            <p class="text-xs text-slate-400">Sign in or register as a player or sports facility owner</p>
          </div>
          <button id="btn-close-auth-modal" class="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition">
            ✕
          </button>
        </div>

        <!-- Role / Mode Tabs -->
        <div class="flex gap-1.5 p-1.5 bg-slate-950 rounded-2xl border border-slate-800 my-4 text-xs font-bold">
          <button id="tab-btn-signin" class="flex-1 py-2 rounded-xl transition ${initialTab === 'signin' ? 'bg-emerald-500 text-slate-950 shadow-md font-black' : 'text-slate-400 hover:text-white'}">
            🔑 Sign In
          </button>
          <button id="tab-btn-player" class="flex-1 py-2 rounded-xl transition ${initialTab === 'player' ? 'bg-emerald-500 text-slate-950 shadow-md font-black' : 'text-slate-400 hover:text-white'}">
            🏏 New Player
          </button>
          <button id="tab-btn-owner" class="flex-1 py-2 rounded-xl transition ${initialTab === 'owner' ? 'bg-emerald-500 text-slate-950 shadow-md font-black' : 'text-slate-400 hover:text-white'}">
            🏢 Facility Owner
          </button>
        </div>

        <!-- Global Feedback Message Box -->
        <div id="auth-feedback" class="text-xs hidden p-3 rounded-xl mb-3"></div>

        <!-- 1. SIGN IN SECTION -->
        <div id="section-signin" class="${initialTab === 'signin' ? '' : 'hidden'} overflow-y-auto space-y-4 pr-1 text-xs">
          <form id="form-signin" class="space-y-3">
            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Mobile Number, Email or Name</label>
              <input id="input-login-id" type="text" placeholder="e.g. +91 98201 12345 or rohan" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
            </div>

            <button type="submit" class="w-full py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-xs shadow-lg shadow-emerald-500/20 transition active:scale-95">
              Sign In →
            </button>
          </form>

          <!-- 1-Click Fast Profile Switcher -->
          <div class="pt-3 border-t border-slate-800 space-y-2">
            <div class="flex items-center justify-between">
              <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">Switch Profiles:</span>
              <button type="button" id="btn-signout-guest" class="text-[10px] text-slate-400 hover:text-rose-400 transition underline">
                Sign Out (Guest Mode)
              </button>
            </div>
            <div class="space-y-1.5 max-h-44 overflow-y-auto pr-1">
              ${allUsers.map(u => {
                const isActive = currentUser && u.uid === currentUser.uid;
                const isOwner = u.role === 'venue_owner';

                return `
                  <div class="p-2.5 rounded-xl border transition-all flex items-center justify-between ${isActive ? 'bg-emerald-950/60 border-emerald-500/80 shadow-md' : 'bg-slate-950/60 border-slate-800 hover:border-slate-700'}">
                    <div class="flex items-center gap-2.5">
                      <div class="w-8 h-8 rounded-lg bg-slate-800 border border-slate-700 text-emerald-400 font-bold flex items-center justify-center text-xs">
                        ${u.displayName.slice(0, 2).toUpperCase()}
                      </div>
                      <div>
                        <div class="font-bold text-white flex items-center gap-1.5">
                          <span>${u.displayName}</span>
                          <span class="text-[9px] uppercase px-1.5 py-0.2 rounded font-mono ${isOwner ? 'bg-indigo-500/20 text-indigo-300' : 'bg-emerald-500/20 text-emerald-400'}">
                            ${isOwner ? 'Facility Owner' : 'Player'}
                          </span>
                        </div>
                        <div class="text-[10px] text-slate-400">${u.phone} • ${u.city || 'Thane'}</div>
                      </div>
                    </div>

                    <div>
                      ${isActive ? `
                        <span class="text-[11px] font-bold text-emerald-400 px-2 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/30">Active</span>
                      ` : `
                        <button data-uid="${u.uid}" class="btn-fast-login px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-white font-bold transition">
                          Sign In
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
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Mobile Number (for Passes) *</label>
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
            <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Email Address (Optional)</label>
            <input id="input-player-email" type="email" placeholder="player@example.com" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
          </div>

          <div class="pt-2">
            <button type="submit" class="w-full py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black text-xs shadow-lg shadow-emerald-500/20 transition active:scale-95">
              🏏 Register Player Account & Claim 100 Pts →
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
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Contact Phone Number *</label>
              <input id="input-owner-phone" type="tel" required placeholder="+91 98200 11223" value="+91 " class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2 text-xs text-white font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
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
            <button type="submit" class="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-black text-xs shadow-lg shadow-indigo-600/20 transition active:scale-95">
              🏢 Register Facility & Open Partner Portal →
            </button>
          </div>
        </form>

      </div>
    `;

    const close = () => modal.remove();
    modal.querySelector('#btn-close-auth-modal').addEventListener('click', close);

    // Tab Navigation Logic
    const tabSignin = modal.querySelector('#tab-btn-signin');
    const tabPlayer = modal.querySelector('#tab-btn-player');
    const tabOwner = modal.querySelector('#tab-btn-owner');

    const secSignin = modal.querySelector('#section-signin');
    const formPlayer = modal.querySelector('#form-register-player');
    const formOwner = modal.querySelector('#form-register-owner');
    const feedback = modal.querySelector('#auth-feedback');

    const setTab = (target) => {
      feedback.classList.add('hidden');
      [tabSignin, tabPlayer, tabOwner].forEach(t => {
        t.className = 'flex-1 py-2 rounded-xl transition text-slate-400 hover:text-white';
      });
      [secSignin, formPlayer, formOwner].forEach(s => s.classList.add('hidden'));

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

    // Handle Sign In Form
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

    // Handle Register Player Form
    formPlayer.addEventListener('submit', (e) => {
      e.preventDefault();
      const displayName = modal.querySelector('#input-player-name').value;
      const phone = modal.querySelector('#input-player-phone').value;
      const city = modal.querySelector('#input-player-city').value;
      const email = modal.querySelector('#input-player-email').value;

      try {
        const newUser = UserService.createUser({
          displayName,
          phone,
          city,
          email,
          role: 'player'
        });

        feedback.className = 'text-xs p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 block';
        feedback.textContent = `✓ Welcome ${newUser.displayName}! Player account created with 100 bonus loyalty credits.`;

        setTimeout(() => {
          close();
          if (onUserChanged) onUserChanged(newUser);
        }, 800);
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `⚠️ ${err.message}`;
      }
    });

    // Handle Register Facility Owner Form
    formOwner.addEventListener('submit', (e) => {
      e.preventDefault();
      const displayName = modal.querySelector('#input-owner-name').value;
      const phone = modal.querySelector('#input-owner-phone').value;
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
        const result = UserService.registerFacilityOwner({
          displayName,
          phone,
          city,
          facilityName,
          area,
          sports,
          startingPrice: Number(startingPrice) || 1200
        });

        feedback.className = 'text-xs p-3 rounded-xl bg-indigo-950/80 border border-indigo-500/50 text-indigo-300 block';
        feedback.textContent = `✓ Partner registered! Facility "${result.venue.name}" created and published in ${result.venue.city}.`;

        setTimeout(() => {
          close();
          if (onUserChanged) onUserChanged(result.user);
          if (onNavigate) onNavigate('partner');
        }, 900);
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `⚠️ ${err.message}`;
      }
    });
  }
}
