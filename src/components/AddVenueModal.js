/**
 * Project Apex - Add Venue & Sports Facility Modal
 * Interactive onboarding for Venue Partners to list new turfs and courts
 */

import { VenueService } from '../services/venueService.js';
import { UserService } from '../services/userService.js';

export class AddVenueModal {
  static show({ onVenueAdded }) {
    let modal = document.getElementById('apex-add-venue-modal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'apex-add-venue-modal';
      modal.className = 'fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-fade-in';
      document.body.appendChild(modal);
    }

    const currentUser = UserService.getCurrentUser();

    modal.innerHTML = `
      <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-2xl w-full p-6 sm:p-8 shadow-2xl relative overflow-hidden text-white flex flex-col max-h-[90vh]">
        <!-- Modal Header -->
        <div class="flex items-center justify-between pb-4 border-b border-slate-800">
          <div>
            <div class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-[10px] font-bold uppercase tracking-wider mb-1">
              🏢 Partner Onboarding
            </div>
            <h3 class="text-2xl font-black text-white">List a New Sports Venue</h3>
            <p class="text-xs text-slate-400">Add turf or court facilities in Thane, Mumbai & Navi Mumbai</p>
          </div>
          <button id="btn-close-venue-modal" class="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition">
            ✕
          </button>
        </div>

        <!-- Scrollable Form Body -->
        <form id="form-add-venue" class="overflow-y-auto py-5 space-y-4 pr-1 text-xs">
          
          <!-- Venue Name & City -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Venue / Facility Name *</label>
              <input id="input-venue-name" type="text" required placeholder="e.g. Apex Thunder Turf Ghodbunder" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
            </div>

            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">City / Region *</label>
              <select id="input-venue-city" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition">
                <option value="Thane" selected>Thane (Ghodbunder, Majiwada, Vartak Nagar)</option>
                <option value="Mumbai">Mumbai (Bandra, Powai, Andheri, BKC)</option>
                <option value="Navi Mumbai">Navi Mumbai (Vashi, Nerul, Kharghar)</option>
              </select>
            </div>
          </div>

          <!-- Area & Address -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Area / Locality *</label>
              <input id="input-venue-area" type="text" required placeholder="e.g. Hiranandani Estate, Ghodbunder Rd" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
            </div>

            <div>
              <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Base Starting Price (₹ / Hour) *</label>
              <input id="input-venue-price" type="number" required min="300" step="50" value="1200" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white font-mono focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
            </div>
          </div>

          <div>
            <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Full Physical Address</label>
            <input id="input-venue-address" type="text" placeholder="Plot No, Near Landmark, Street Address, Pincode" class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
          </div>

          <!-- Sports Selection (Checkboxes) -->
          <div>
            <label class="block font-bold uppercase tracking-wider text-slate-300 mb-2">Sports Supported (Pick at least one) *</label>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-2 text-xs">
              <label class="flex items-center gap-2 p-2.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-emerald-500/50 cursor-pointer">
                <input type="checkbox" name="sport-option" value="cricket" checked class="rounded text-emerald-500 focus:ring-0" />
                <span>🏏 Box Cricket</span>
              </label>
              <label class="flex items-center gap-2 p-2.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-emerald-500/50 cursor-pointer">
                <input type="checkbox" name="sport-option" value="football" checked class="rounded text-emerald-500 focus:ring-0" />
                <span>⚽ Football Turf</span>
              </label>
              <label class="flex items-center gap-2 p-2.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-emerald-500/50 cursor-pointer">
                <input type="checkbox" name="sport-option" value="badminton" class="rounded text-emerald-500 focus:ring-0" />
                <span>🏸 Badminton Court</span>
              </label>
              <label class="flex items-center gap-2 p-2.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-emerald-500/50 cursor-pointer">
                <input type="checkbox" name="sport-option" value="pickleball" class="rounded text-emerald-500 focus:ring-0" />
                <span>🏓 Pickleball Court</span>
              </label>
              <label class="flex items-center gap-2 p-2.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-emerald-500/50 cursor-pointer">
                <input type="checkbox" name="sport-option" value="squash" class="rounded text-emerald-500 focus:ring-0" />
                <span>🎾 Squash</span>
              </label>
              <label class="flex items-center gap-2 p-2.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-emerald-500/50 cursor-pointer">
                <input type="checkbox" name="sport-option" value="tennis" class="rounded text-emerald-500 focus:ring-0" />
                <span>🎾 Lawn Tennis</span>
              </label>
            </div>
          </div>

          <!-- Amenities Selection -->
          <div>
            <label class="block font-bold uppercase tracking-wider text-slate-300 mb-2">Amenities Provided</label>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-2 text-xs">
              <label class="flex items-center gap-2 p-2 rounded-xl bg-slate-950 border border-slate-800 cursor-pointer">
                <input type="checkbox" name="amenity-option" value="FIFA-grade Turf" checked class="rounded text-emerald-500 focus:ring-0" />
                <span>Pro Grade Turf</span>
              </label>
              <label class="flex items-center gap-2 p-2 rounded-xl bg-slate-950 border border-slate-800 cursor-pointer">
                <input type="checkbox" name="amenity-option" value="LED Floodlights" checked class="rounded text-emerald-500 focus:ring-0" />
                <span>LED Floodlights</span>
              </label>
              <label class="flex items-center gap-2 p-2 rounded-xl bg-slate-950 border border-slate-800 cursor-pointer">
                <input type="checkbox" name="amenity-option" value="Changing Rooms" checked class="rounded text-emerald-500 focus:ring-0" />
                <span>Changing Rooms</span>
              </label>
              <label class="flex items-center gap-2 p-2 rounded-xl bg-slate-950 border border-slate-800 cursor-pointer">
                <input type="checkbox" name="amenity-option" value="Air-conditioned Dugout" checked class="rounded text-emerald-500 focus:ring-0" />
                <span>AC Dugout</span>
              </label>
              <label class="flex items-center gap-2 p-2 rounded-xl bg-slate-950 border border-slate-800 cursor-pointer">
                <input type="checkbox" name="amenity-option" value="Valet Parking" class="rounded text-emerald-500 focus:ring-0" />
                <span>Free Parking</span>
              </label>
              <label class="flex items-center gap-2 p-2 rounded-xl bg-slate-950 border border-slate-800 cursor-pointer">
                <input type="checkbox" name="amenity-option" value="Cafeteria" class="rounded text-emerald-500 focus:ring-0" />
                <span>Cafeteria</span>
              </label>
            </div>
          </div>

          <!-- Cover Image URL -->
          <div>
            <label class="block font-bold uppercase tracking-wider text-slate-300 mb-1">Cover Photo URL (Optional)</label>
            <input id="input-venue-image" type="url" placeholder="https://images.unsplash.com/photo-..." class="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-4 py-2.5 text-xs text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none transition" />
            <p class="text-[10px] text-slate-500 mt-1">Leave blank to use verified high-definition sports facility photography automatically.</p>
          </div>

          <div id="add-venue-feedback" class="text-xs hidden p-3 rounded-xl"></div>

          <!-- Submit Button -->
          <div class="pt-4 border-t border-slate-800 flex justify-end gap-3">
            <button type="button" id="btn-cancel-add-venue" class="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold transition">
              Cancel
            </button>
            <button type="submit" id="btn-submit-venue" class="px-6 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black shadow-lg shadow-emerald-500/20 transition active:scale-95 flex items-center gap-2">
              <span>+ Create & Persist Venue</span>
            </button>
          </div>
        </form>
      </div>
    `;

    const close = () => modal.remove();
    modal.querySelector('#btn-close-venue-modal').addEventListener('click', close);
    modal.querySelector('#btn-cancel-add-venue').addEventListener('click', close);

    const form = modal.querySelector('#form-add-venue');
    const feedback = modal.querySelector('#add-venue-feedback');

    form.addEventListener('submit', (e) => {
      e.preventDefault();

      const name = modal.querySelector('#input-venue-name').value;
      const city = modal.querySelector('#input-venue-city').value;
      const area = modal.querySelector('#input-venue-area').value;
      const address = modal.querySelector('#input-venue-address').value;
      const startingPrice = modal.querySelector('#input-venue-price').value;
      const customImage = modal.querySelector('#input-venue-image').value.trim();

      const sports = Array.from(modal.querySelectorAll('input[name="sport-option"]:checked')).map(el => el.value);
      const amenities = Array.from(modal.querySelectorAll('input[name="amenity-option"]:checked')).map(el => el.value);

      if (sports.length === 0) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = 'Please select at least one sport category for this facility.';
        return;
      }

      try {
        const images = customImage ? [customImage] : [];
        const newVenue = VenueService.createVenue({
          name,
          city,
          area,
          address,
          sports,
          amenities,
          startingPrice: Number(startingPrice) || 1000,
          images,
          ownerId: currentUser.uid
        });

        feedback.className = 'text-xs p-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 block';
        feedback.textContent = `✓ Venue "${newVenue.name}" created and persisted successfully! Added to ${newVenue.city} directory.`;

        setTimeout(() => {
          close();
          if (onVenueAdded) onVenueAdded(newVenue);
        }, 800);
      } catch (err) {
        feedback.className = 'text-xs p-3 rounded-xl bg-red-950/80 border border-red-500/50 text-red-300 block';
        feedback.textContent = `Error: ${err.message}`;
      }
    });
  }
}
