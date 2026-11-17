/**
 * Project Apex - Offline Resilience & IndexedDB Storage
 * Stores venue catalogs and entry QR passes for instant offline access at turf gates
 */

const DB_NAME = 'apex_offline_vault';
const DB_VERSION = 1;

export class OfflineStorage {
  static async openDB() {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
      request.onupgradeneeded = (event) => {
        const db = event.target.result;
        if (!db.objectStoreNames.contains('passes')) {
          db.createObjectStore('passes', { keyPath: 'bookingId' });
        }
        if (!db.objectStoreNames.contains('cached_venues')) {
          db.createObjectStore('cached_venues', { keyPath: 'venueId' });
        }
      };
    });
  }

  static async savePass(booking) {
    try {
      const db = await this.openDB();
      const tx = db.transaction('passes', 'readwrite');
      tx.objectStore('passes').put(booking);
      return new Promise(resolve => tx.oncomplete = resolve);
    } catch (e) {
      console.warn("IndexedDB fallback to localStorage", e);
      localStorage.setItem(`pass_${booking.bookingId}`, JSON.stringify(booking));
    }
  }

  static async getOfflinePasses() {
    try {
      const db = await this.openDB();
      const tx = db.transaction('passes', 'readonly');
      const store = tx.objectStore('passes');
      const request = store.getAll();
      return new Promise((resolve) => {
        request.onsuccess = () => resolve(request.result || []);
        request.onerror = () => resolve([]);
      });
    } catch (e) {
      return [];
    }
  }

  static async cacheVenues(venues) {
    try {
      const db = await this.openDB();
      const tx = db.transaction('cached_venues', 'readwrite');
      const store = tx.objectStore('cached_venues');
      venues.forEach(v => store.put(v));
      return new Promise(resolve => tx.oncomplete = resolve);
    } catch (e) {
      console.warn("Failed to cache venues", e);
    }
  }
}
