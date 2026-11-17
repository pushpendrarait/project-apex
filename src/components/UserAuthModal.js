/**
 * Project Apex - User Profile & Registration Modal
 * Allows switching between profiles or registering a brand new user persisted in Firestore/localStorage
 */

import { UserService } from '../services/userService.js';

export class UserAuthModal {
  static show({ onUserChanged }) {
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
      <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl relative overflow-hidden text-white flex flex-col max-h-[90vh]">
        <!-- Header -->
        <div class="flex items-center justify-between pb-4 border-b border-slate-800">
          <div>
            <div class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold uppercase tracking-wider mb-1">
              👤 Account & Profile Manager
            </div>
            <h3 class="text-xl font-black text-white">Switch or Create User</h3>
            <p class="text-xs text-slate-400">Current session: <strong class="text-emerald-400">${currentUser.displayName}</strong> (${currentUser.role})</p>
          </div>
          <button id="btn-close-auth-modal" class="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition">
            ✕
          </button>
        </div>

        <!-- Navigation Tabs inside Modal -->
        <div class="flex gap-2 p-1.5 bg-slate-950 rounded-2xl border border-slate-800 my-4 text-xs font-bold">
          <button id="tab-switch-user" class="flex-1 py-2 rounded-xl transition bg-emerald-500 text-slate-950 shadow-md">
            Switch Profile (${allUsers.length})
          </button>
          <button id="tab-new-user" class="flex-1 py-2 rounded-xl transition text-slate-400 hover:text-white">
            + Register New User
          </button>
        </div>

        <!-- Tab 1: Switch Profile Section -->
        <div id="section-switch-user" class="overflow-y-auto space-y-2 max-h-64 pr-1 text-xs">
          ${allUsers.map(u => {
            const isActive = u.uid === currentUser.uid;
            const initials = u.displayName.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();

            return `
              <div class="p-3.5 rounded-2xl border transition-all flex items-center justify-between ${isActive ? 'bg-emerald-950/60 border-emerald-500/80 shadow-md' : 'bg-slate-950/60 border-slate-800 hover:border-slate-700'}">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-xl bg-slate-800 border border-slate-700 text-emerald-400 font-bold flex items-center justify-center text-sm">
                    ${initials}
                  </div>
                  <div>
                    <div class="font-bold text-white flex items-center gap-2">
                      <span>${u.displayName}</span>
                      <span class="text-[9px] uppercase px-1.5 py-0.2 rounded font-mono ${u.role === 'venue_owner' ? 'bg-indigo-500/20 text-indigo-300' : 'bg-emerald-500/20 text-emerald-400'}">
                        ${u.role}
                      </span>
                    </div>
                    <div class="text-[11px] text-slate-400">${u.phone} • ${u.city || 'Thane'}</div>
                  </div>
                </div>

                <div>
                  ${isActive ? `
                    <span class="text-xs font-black text-emerald-400 px-3 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/30">Active</span>
                  ` : `
                    <button data-uid="${u.uid}" class="btn-activate-user px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold transition">
                      Select
                    </button>
                  `}
                </div>
              </div>
            `;
          }).join('')}
        </div>

        <!-- Tab 2: Create User Form -->
        <form id="form-create-user" class="hidden overflow-y-auto space-y-3.5 pr-1 text-xs">
          <div>
            <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Full Name *</label>
            <input id="input-new-name" type="text" required placeholder="e.g. Sahil Shinde" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Mobile Number *</label>
              <input id="input-new-phone" type="tel" required placeholder="+91 98765 43210" value="+91 " class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
            </div>

            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">City / Metro *</label>
              <select id="input-new-city" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition">
                <option value="Thane" selected>Thane</option>
                <option value="Mumbai">Mumbai</option>
                <option value="Navi Mumbai">Navi Mumbai</option>
              </select>
            </div>
          </div>

          <div>
            <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Email Address (Optional)</label>
            <input id="input-new-email" type="email" placeholder="player@example.com" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
          </div>

          <div>
            <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1.5">Account Role *</label>
            <div class="grid grid-cols-2 gap-2">
              <label class="flex items-center gap-2 p-3 rounded-xl bg-slate-950 border border-slate-800 hover:border-emerald-500/50 cursor-pointer">
                <input type="radio" name="account-role" value="player" checked class="text-emerald-500 focus:ring-0" />
                <div>
                  <span class="font-bold text-white block">🏏 Sports Player</span>
                  <span class="text-[10px] text-slate-400">+100 Welcome Points</span>
                </div>
              </label>

              <label class="flex items-center gap-2 p-3 rounded-xl bg-slate-950 border border-slate-800 hover:border-indigo-500/50 cursor-pointer">
                <input type="radio" name="account-role" value="venue_owner" class="text-indigo-500 focus:ring-0" />
                <div>
                  <span class="font-bold text-white block">🏢 Venue Partner</span>
                  <span class="text-[10px] text-slate-400">Manage Turfs & Courts</span>
                </div>
              </label>
            </div>
          </div>

          <div id="create-user-feedback" class="text-xs hidden p-3 rounded-xl"></div>

          <div class="pt-3 border-t border-slate-800 flex justify-end gap-3">
            <button type="submit" class="w-full py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black shadow-lg shadow-emerald-500/20 transition active:scale-95">
              + Save User & Sign In
            </button>
          </div>
        </form>
      </div>
    `;

    const close = () => modal.remove();
    modal.querySelector('#btn-close-auth-modal').addEventListener('click', close);

    // Tab switching
    const tabSwitch = modal.querySelector('#tab-switch-user');
    const tabNew = modal.querySelector('#tab-new-user');
    const secSwitch = modal.querySelector('#section-switch-user');
    const formNew = modal.querySelector('#form-create-user');

    tabSwitch.addEventListener('click', () => {
      tabSwitch.className = 'flex-1 py-2 rounded-xl transition bg-emerald-500 text-slate-950 shadow-md font-bold';
      tabNew.className = 'flex-1 py-2 rounded-xl transition text-slate-400 hover:text-white font-bold';
      secSwitch.classList.remove('hidden');
      formNew.classList.add('hidden');
    });

    tabNew.addEventListener('click', () => {
      tabNew.className = 'flex-1 py-2 rounded-xl transition bg-emerald-500 text-slate-950 shadow-md font-bold';
      tabSwitch.className = 'flex-1 py-2 rounded-xl transition text-slate-400 hover:text-white font-bold';
      formNew.classList.remove('hidden');
      secSwitch.classList.add('hidden');
    });

    // Activate existing user
    modal.querySelectorAll('.btn-activate-user').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const uid = e.currentTarget.getAttribute('data-uid');
        const user = UserService.setCurrentUser(uid);
        close();
        if (onUserChanged) onUserChanged(user);
      });
    });

    // Create user submission
    formNew.addEventListener('submit', (e) => {
      e.preventDefault();

      const displayName = modal.querySelector('#input-new-name').value;
      const phone = modal.querySelector('#input-new-phone').value;
      const city = modal.querySelector('#input-new-city').value;
      const email = modal.querySelector('#input-new-email').value;
      const role = modal.querySelector('input[name="account-role"]:checked').value;
      const feedback = modal.querySelector('#create-user-feedback');

      try {
        const newUser = UserService.createUser({
          displayName,
          phone,
          email,
          city,
          role
        });

        feedback.className = 'text-xs p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 block';
        feedback.textContent = `✓ Account for "${newUser.displayName}" created & persisted! Logged in as ${newUser.role}.`;

        setTimeout(() => {
          close();
          if (onUserChanged) onUserChanged(newUser);
        }, 800);
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `Error: ${err.message}`;
      }
    });
  }
}
