/**
 * Project Apex - Admin Console & Spark Quota Telemetry
 */

import { mockDb } from '../services/firebaseConfig.js';
import { ScenarioRunnerModal } from './ScenarioRunnerModal.js';

export class AdminConsole {
  constructor({ onBack }) {
    this.onBack = onBack;
  }

  render(container) {
    // Calculate current usage telemetry
    const totalVenues = Object.keys(mockDb.data.venues).length;
    const totalCourts = Object.keys(mockDb.data.courts).length;
    const totalBookings = Object.keys(mockDb.data.bookings).length;
    const totalLocks = Object.keys(mockDb.data.concurrency_locks).length;
    const totalAuditLogs = mockDb.data.audit_logs.length;

    // Simulation metrics for Spark Free Tier read/write tracking
    const estimatedDailyReads = 14 + (totalBookings * 2); // 1 per court daily + lookup
    const estimatedDailyWrites = totalBookings * 4 + totalLocks;
    const maxReads = 50000;
    const maxWrites = 20000;

    const readUsagePct = ((estimatedDailyReads / maxReads) * 100).toFixed(2);
    const writeUsagePct = ((estimatedDailyWrites / maxWrites) * 100).toFixed(2);

    container.innerHTML = `
      <div class="space-y-6">
        <!-- Header -->
        <div class="bg-gradient-to-r from-slate-900 via-slate-900 to-rose-950 p-6 rounded-3xl border border-slate-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <div class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/30 text-xs font-bold uppercase tracking-wider mb-2">
              ⚙️ Governance & Architecture Monitor
            </div>
            <h2 class="text-2xl font-black text-white">Project Apex System Telemetry</h2>
            <p class="text-xs text-slate-400">Firebase Spark Free Tier Audit & Multi-Agent Matrix</p>
          </div>

          <div class="flex items-center gap-2">
            <button id="btn-admin-run-scenarios" class="px-4 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-black text-xs shadow-lg shadow-amber-500/20 transition flex items-center gap-1.5">
              <span>⚡ Run Live Workflow Scenarios</span>
            </button>
            <button id="btn-back-admin" class="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs transition">
              ← Switch to Player View
            </button>
          </div>
        </div>

        <!-- Spark Quota Budgets -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <!-- Document Reads -->
          <div class="bg-slate-900 border border-slate-800 p-5 rounded-2xl space-y-3">
            <div class="flex justify-between items-center">
              <span class="text-xs font-bold uppercase text-slate-400">Firestore Daily Reads</span>
              <span class="text-xs font-mono font-bold text-emerald-400">${readUsagePct}% Used</span>
            </div>
            <div class="w-full bg-slate-800 rounded-full h-2.5 overflow-hidden">
              <div class="bg-emerald-500 h-full rounded-full transition-all" style="width: ${Math.max(readUsagePct, 1)}%"></div>
            </div>
            <div class="flex justify-between text-xs text-slate-400 font-mono">
              <span>${estimatedDailyReads} reads</span>
              <span>50,000 max/day</span>
            </div>
            <p class="text-[11px] text-emerald-300">✓ 1-read-per-court aggregated daily doc protocol active</p>
          </div>

          <!-- Document Writes -->
          <div class="bg-slate-900 border border-slate-800 p-5 rounded-2xl space-y-3">
            <div class="flex justify-between items-center">
              <span class="text-xs font-bold uppercase text-slate-400">Firestore Daily Writes</span>
              <span class="text-xs font-mono font-bold text-emerald-400">${writeUsagePct}% Used</span>
            </div>
            <div class="w-full bg-slate-800 rounded-full h-2.5 overflow-hidden">
              <div class="bg-emerald-500 h-full rounded-full transition-all" style="width: ${Math.max(writeUsagePct, 1)}%"></div>
            </div>
            <div class="flex justify-between text-xs text-slate-400 font-mono">
              <span>${estimatedDailyWrites} writes</span>
              <span>20,000 max/day</span>
            </div>
            <p class="text-[11px] text-emerald-300">✓ Atomic multi-doc commit active</p>
          </div>

          <!-- Cloud Functions Status -->
          <div class="bg-slate-900 border border-slate-800 p-5 rounded-2xl space-y-3">
            <div class="flex justify-between items-center">
              <span class="text-xs font-bold uppercase text-slate-400">Cloud Functions</span>
              <span class="text-xs font-bold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400">0 DEPLOYED</span>
            </div>
            <div class="text-2xl font-black text-white font-mono">ZERO CF</div>
            <div class="text-xs text-slate-400">100% Client-side atomic transaction compliance</div>
            <p class="text-[11px] text-emerald-300">✓ Firebase Spark Free Tier $0.00 guarantee</p>
          </div>
        </div>

        <!-- 8 Collections Architecture Health -->
        <div class="bg-slate-900 border border-slate-800 p-6 rounded-2xl space-y-4">
          <h3 class="text-base font-black text-white">8-Collection Core Architecture Status</h3>
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
            <div class="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <span class="text-slate-400 block font-mono">/users</span>
              <span class="font-bold text-white">${Object.keys(mockDb.data.users).length} docs</span>
            </div>
            <div class="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <span class="text-slate-400 block font-mono">/venues</span>
              <span class="font-bold text-white">${totalVenues} docs</span>
            </div>
            <div class="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <span class="text-slate-400 block font-mono">/courts</span>
              <span class="font-bold text-white">${totalCourts} docs</span>
            </div>
            <div class="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <span class="text-slate-400 block font-mono">/availability</span>
              <span class="font-bold text-white">${Object.keys(mockDb.data.availability).length} daily docs</span>
            </div>
            <div class="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <span class="text-slate-400 block font-mono">/concurrency_locks</span>
              <span class="font-bold text-white">${totalLocks} active</span>
            </div>
            <div class="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <span class="text-slate-400 block font-mono">/bookings</span>
              <span class="font-bold text-white">${totalBookings} docs</span>
            </div>
            <div class="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <span class="text-slate-400 block font-mono">/transactions</span>
              <span class="font-bold text-white">${Object.keys(mockDb.data.transactions).length} docs</span>
            </div>
            <div class="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <span class="text-slate-400 block font-mono">/audit_logs</span>
              <span class="font-bold text-white">${totalAuditLogs} entries</span>
            </div>
          </div>
        </div>

        <!-- Multi-Agent Roster Table -->
        <div class="bg-slate-900 border border-slate-800 p-6 rounded-2xl space-y-4">
          <h3 class="text-base font-black text-white">Project Apex Multi-Agent Roster</h3>
          <div class="overflow-x-auto">
            <table class="w-full text-left text-xs text-slate-300">
              <thead class="bg-slate-950 text-slate-400 uppercase font-mono border-b border-slate-800">
                <tr>
                  <th class="p-3">Agent</th>
                  <th class="p-3">Role</th>
                  <th class="p-3">Spec Kitty Mission</th>
                  <th class="p-3">Status</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-800">
                <tr>
                  <td class="p-3 font-bold text-white">apex-pm</td>
                  <td class="p-3">Project Manager & Spec Kitty Controller</td>
                  <td class="p-3 font-mono text-emerald-400">WP01: Governance</td>
                  <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold">Active</span></td>
                </tr>
                <tr>
                  <td class="p-3 font-bold text-white">apex-ba</td>
                  <td class="p-3">Business Analyst</td>
                  <td class="p-3 font-mono text-emerald-400">WP02: PRD & Use Cases</td>
                  <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold">Active</span></td>
                </tr>
                <tr>
                  <td class="p-3 font-bold text-white">apex-database</td>
                  <td class="p-3">Database Expert</td>
                  <td class="p-3 font-mono text-emerald-400">WP03: 8 Collections Schema</td>
                  <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold">Active</span></td>
                </tr>
                <tr>
                  <td class="p-3 font-bold text-white">apex-backend</td>
                  <td class="p-3">Backend Engineer</td>
                  <td class="p-3 font-mono text-emerald-400">WP04: Atomic Locks</td>
                  <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold">Active</span></td>
                </tr>
                <tr>
                  <td class="p-3 font-bold text-white">apex-frontend</td>
                  <td class="p-3">Frontend / UI Expert</td>
                  <td class="p-3 font-mono text-emerald-400">WP05: UI & Slot Grid</td>
                  <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold">Active</span></td>
                </tr>
                <tr>
                  <td class="p-3 font-bold text-white">apex-mobile</td>
                  <td class="p-3">Mobile App Expert</td>
                  <td class="p-3 font-mono text-emerald-400">WP06: PWA & Offline Cache</td>
                  <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold">Active</span></td>
                </tr>
                <tr>
                  <td class="p-3 font-bold text-white">apex-marketing</td>
                  <td class="p-3">Marketing Expert</td>
                  <td class="p-3 font-mono text-emerald-400">WP07: Growth & Odd Hours</td>
                  <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold">Active</span></td>
                </tr>
                <tr>
                  <td class="p-3 font-bold text-white">apex-legal</td>
                  <td class="p-3">Legal Compliance Expert</td>
                  <td class="p-3 font-mono text-emerald-400">WP08: ToS & Waivers</td>
                  <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold">Active</span></td>
                </tr>
                <tr>
                  <td class="p-3 font-bold text-white">apex-deployment</td>
                  <td class="p-3">Deployment Engineer</td>
                  <td class="p-3 font-mono text-emerald-400">WP09: Firebase Spark Tier</td>
                  <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold">Active</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `;

    container.querySelector('#btn-back-admin').addEventListener('click', () => {
      if (this.onBack) this.onBack();
    });

    const btnScenarios = container.querySelector('#btn-admin-run-scenarios');
    if (btnScenarios) {
      btnScenarios.addEventListener('click', () => {
        ScenarioRunnerModal.show({
          onStateUpdated: () => {
            this.render(container);
          }
        });
      });
    }
  }
}
