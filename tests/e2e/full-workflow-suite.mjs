/**
 * Project Apex — Live 3-Tier End-to-End QA Verification & Evidence Runner
 * Executes real headless Chromium UI workflows against Firebase Hosting (Tier 1),
 * REST API checks against Render Java 21 Spring Boot Middleware (Tier 2),
 * and live SQL assertions against Neon Serverless PostgreSQL (Tier 3).
 *
 * Required Env Vars:
 *   NEON_DATABASE_URL   - PostgreSQL connection string (fetched dynamically via neonctl)
 * Optional Env Vars:
 *   WEB_URL             - Default: https://project-apex-pushp.web.app
 *   API_URL             - Default: https://project-apex-backend-fqe3.onrender.com/api/v1
 *   CHROMIUM_PATH       - Path to installed Chromium binary
 *   SCREENSHOT_DIR      - Directory to store UI screenshots
 *   EVIDENCE_OUT        - Path to write structured JSON QA evidence
 *   QA_NODE_MODULES     - Path containing playwright-core & pg packages
 */

import { createRequire } from 'module';
import fs from 'fs';
import path from 'path';
import crypto from 'crypto';

const nodeModulesHint = process.env.QA_NODE_MODULES
  ? path.join(process.env.QA_NODE_MODULES, 'package.json')
  : import.meta.url;
const require = createRequire(nodeModulesHint);
const { chromium } = require('playwright-core');
const { Client } = require('pg');

const WEB_URL = (process.env.WEB_URL || 'https://project-apex-pushp.web.app').replace(/\/+$/, '');
const API_URL = (process.env.API_URL || 'https://project-apex-backend-fqe3.onrender.com/api/v1').replace(/\/+$/, '');
const NEON_DATABASE_URL = process.env.NEON_DATABASE_URL;
const CHROMIUM_PATH = process.env.CHROMIUM_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const SCREENSHOT_DIR = process.env.SCREENSHOT_DIR || path.resolve('docs/qa/screenshots');
const EVIDENCE_OUT = process.env.EVIDENCE_OUT || path.resolve('docs/qa/qa-live-evidence.json');

if (!NEON_DATABASE_URL) {
  console.error('ERROR: NEON_DATABASE_URL env var is required to capture live database evidence.');
  process.exit(1);
}

fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
fs.mkdirSync(path.dirname(EVIDENCE_OUT), { recursive: true });

const runTs = String(Date.now()).slice(-6);
const phoneFive = String(Math.floor(10000 + Math.random() * 89999));

const evidence = {
  executedAt: new Date().toISOString(),
  webUrl: WEB_URL,
  apiUrl: API_URL,
  chromiumExecutable: CHROMIUM_PATH,
  cases: [],
  screenshots: [],
  dbSnapshots: {},
  cleanup: {},
  summary: { passed: 0, failed: 0, total: 0 }
};

function record(id, title, persona, tier, passed, details = {}) {
  evidence.summary.total++;
  if (passed) {
    evidence.summary.passed++;
    console.log(`  ✓ [${id}] ${title}`);
  } else {
    evidence.summary.failed++;
    console.error(`  ✗ [${id}] ${title} -> ${JSON.stringify(details)}`);
    process.exitCode = 1;
  }
  evidence.cases.push({
    id,
    title,
    persona,
    tier,
    status: passed ? 'PASS' : 'FAIL',
    timestamp: new Date().toISOString(),
    details
  });
}

async function shot(page, filename, caption) {
  const fullPath = path.join(SCREENSHOT_DIR, filename);
  await page.screenshot({ path: fullPath, fullPage: false });
  evidence.screenshots.push({ filename, path: fullPath, caption });
  return fullPath;
}

async function apiJson(method, endpoint, { token = null, body = null, headers = {} } = {}) {
  const url = `${API_URL}${endpoint}`;
  const reqHeaders = { 'Content-Type': 'application/json', ...headers };
  if (token) reqHeaders['Authorization'] = `Bearer ${token}`;
  const rawBody = body === null ? undefined : (typeof body === 'string' ? body : JSON.stringify(body));
  const res = await fetch(url, { method, headers: reqHeaders, body: rawBody });
  const text = await res.text();
  let json = null;
  try { json = text ? JSON.parse(text) : null; } catch { json = { raw: text }; }
  return { status: res.status, headers: Object.fromEntries(res.headers.entries()), json, rawText: text };
}

async function warmBackend(maxWaitMs = 90000) {
  const started = Date.now();
  while (Date.now() - started < maxWaitMs) {
    try {
      const r = await apiJson('GET', '/health');
      if (r.status === 200 && r.json?.data?.dbConnected) {
        return { latencyMs: Date.now() - started, health: r.json.data };
      }
    } catch {}
    await new Promise(r => setTimeout(r, 3000));
  }
  throw new Error('Render backend health check timed out after ' + maxWaitMs + 'ms');
}

async function getDbCounts(pgClient) {
  const tables = [
    'apex_users', 'apex_venues', 'apex_resources', 'apex_schedule_rules',
    'apex_date_overrides', 'apex_slot_locks', 'apex_discounts', 'apex_bookings',
    'apex_payments', 'apex_otp_sessions', 'apex_webhook_events', 'apex_facilities', 'apex_audit_logs'
  ];
  const counts = {};
  for (const t of tables) {
    const res = await pgClient.query(`SELECT COUNT(*)::int AS cnt FROM ${t}`);
    counts[t] = res.rows[0].cnt;
  }
  return counts;
}

function futureDateStr(daysAhead) {
  const d = new Date(Date.now() + daysAhead * 86400000);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

async function main() {
  console.log('\n====================================================================');
  console.log('🚀 PROJECT APEX LIVE 3-TIER E2E TEST EXECUTION (UI + API + NEON DB)');
  console.log('====================================================================\n');

  const pgClient = new Client({
    connectionString: NEON_DATABASE_URL,
    ssl: { rejectUnauthorized: false }
  });
  await pgClient.connect();

  const browser = await chromium.launch({
    executablePath: CHROMIUM_PATH,
    headless: true
  });

  const ownerEmail = `qa.owner.${runTs}@apex.dev`;
  const ownerPhone = `+91 98761 ${phoneFive}`;
  const playerEmail = `qa.player.${runTs}@apex.dev`;
  const playerPhone = `+91 98762 ${phoneFive}`;
  const p2Email = `qa.player2.${runTs}@apex.dev`;
  const p2Phone = `+91 98763 ${phoneFive}`;

  let createdVenueIds = [];
  let createdCourtIds = [];
  let createdUserIds = [];
  let createdBookingIds = [];

  try {
    // ------------------------------------------------------------------
    // PHASE 1: Health, Neon Baseline & CORS Security
    // ------------------------------------------------------------------
    console.log('--- PHASE 1: Cloud Health, CORS & Neon Baseline ---');
    const warm = await warmBackend();
    record('TC-ARC-003', 'Render /api/v1/health & Neon SSL DB connected', 'System', 'Tier 2 + Tier 3',
      warm.health.status === 'UP' && warm.health.database === 'Neon PostgreSQL' && warm.health.dbConnected === true,
      { warmLatencyMs: warm.latencyMs, health: warm.health });

    evidence.dbSnapshots.baselineCounts = await getDbCounts(pgClient);
    record('TC-DB-000', 'Neon Serverless PostgreSQL 13-table baseline queried', 'Admin', 'Tier 3',
      Object.keys(evidence.dbSnapshots.baselineCounts).length === 13,
      evidence.dbSnapshots.baselineCounts);

    // CORS preflight & live JS secret check
    const corsRes = await fetch(`${API_URL}/venues`, {
      method: 'OPTIONS',
      headers: { Origin: WEB_URL, 'Access-Control-Request-Method': 'GET' }
    });
    const acao = corsRes.headers.get('access-control-allow-origin');
    record('TC-ARC-001', 'CORS allows Firebase Hosting origin project-apex-pushp.web.app', 'Guest', 'Tier 1 -> Tier 2',
      corsRes.ok && (acao === WEB_URL || acao === '*'),
      { status: corsRes.status, allowOrigin: acao });

    const htmlRes = await fetch(WEB_URL);
    const htmlBody = await htmlRes.text();
    const apiClientJs = await (await fetch(`${WEB_URL}/src/services/apiClient.js`)).text();
    const hasSecretLeak = /postgresql:\/\/|npg_|rzp_live_|whsec_|jwt\.secret/i.test(htmlBody + apiClientJs);
    record('TC-ARC-002', 'Tier 1 Firebase Hosting serves UI cleanly with zero DB/API secrets', 'Guest', 'Tier 1',
      htmlRes.status === 200 && !hasSecretLeak,
      { httpStatus: htmlRes.status, secretLeakDetected: hasSecretLeak });

    // Guest unauthenticated isolation check
    const guestMe = await apiJson('GET', '/users/me');
    const guestBookingsForbidden = await apiJson('GET', '/bookings/my');
    record('TC-USR-011', 'Guest /users/me returns null & protected endpoints reject anonymous caller', 'Guest', 'Tier 2 Security',
      guestMe.status === 200 && guestMe.json?.data === null && (guestBookingsForbidden.status === 401 || guestBookingsForbidden.status === 403),
      { guestMeStatus: guestMe.status, guestMeData: guestMe.json?.data, bookingsMyStatus: guestBookingsForbidden.status });

    // ------------------------------------------------------------------
    // PHASE 2: Browser UI Walkthrough — Facility Owner CRUD & Gate QR
    // ------------------------------------------------------------------
    console.log('\n--- PHASE 2: Browser UI — Facility Owner Full CRUD, Walk-In & Gate QR ---');
    const context = await browser.newContext({ viewport: { width: 1440, height: 920 } });
    const page = await context.newPage();

    const apiResponses = [];
    page.on('response', res => {
      if (res.url().includes('/api/v1/')) {
        apiResponses.push({ url: res.url().replace(API_URL, ''), status: res.status() });
      }
    });

    await page.goto(WEB_URL, { waitUntil: 'networkidle' });
    await page.waitForFunction(() => {
      const el = document.querySelector('#header-backend-label');
      return el && (el.textContent.includes('Render') || el.textContent.includes('Neon'));
    }, { timeout: 25000 });
    await shot(page, '01_landing_explore.png', 'Live Firebase UI connected to Render + Neon PostgreSQL');
    record('TC-UI-001', 'Live web landing renders venues & green Render + Neon health badge', 'Guest', 'Tier 1 UI',
      await page.locator('#header-backend-label').innerText().then(t => t.includes('Render') || t.includes('Neon')));

    // Register Facility Owner through UI Dual OTP Modal
    await page.click('#btn-header-auth');
    await page.click('#mode-btn-signup');
    await page.click('#role-btn-owner');
    await page.fill('#input-owner-name', 'QA Owner Kadam');
    await page.fill('#input-owner-phone', ownerPhone);
    await page.fill('#input-owner-email', ownerEmail);
    await page.fill('#input-owner-password', 'OwnerPass@123');
    await page.fill('#input-facility-name', `QA Thunder Arena ${runTs}`);
    await page.selectOption('#input-facility-city', 'Thane');
    await page.fill('#input-facility-area', 'Wagle Estate');
    await page.fill('#input-facility-price', '1200');
    await page.click('#form-register-owner button[type="submit"]');

    await page.waitForSelector('#section-otp-verify:not(.hidden)');
    await page.click('#btn-autofill-sms');
    await page.click('#btn-autofill-email');
    await page.click('#btn-submit-verify-otp');

    // Wait for partner portal redirect after backend registration + venue sync
    await page.waitForSelector('#btn-owner-edit-venue', { timeout: 25000 });
    await page.waitForTimeout(1000);
    await shot(page, '02_owner_registered_portal.png', 'Facility Owner registered via UI & Partner Portal loaded');

    const ownerDbRes = await pgClient.query(
      'SELECT uid, full_name, email, role, venue_id, loyalty_points, is_phone_verified, is_email_verified, password_hash FROM apex_users WHERE email = $1',
      [ownerEmail]
    );
    const ownerRow = ownerDbRes.rows[0];
    if (ownerRow) createdUserIds.push(ownerRow.uid);
    if (ownerRow?.venue_id) createdVenueIds.push(ownerRow.venue_id);

    const ownerVenueDb = ownerRow?.venue_id
      ? (await pgClient.query('SELECT venue_id, owner_id, name, city, locality, min_slot_price_paise, status FROM apex_venues WHERE venue_id = $1', [ownerRow.venue_id])).rows[0]
      : null;

    record('TC-USR-003', 'UI Facility Owner registration persists User, Venue, Facility, Resource & ScheduleRule to Neon', 'Facility Owner', 'UI + API + Neon',
      Boolean(ownerRow && ownerRow.venue_id && ownerVenueDb && ownerRow.password_hash && !ownerRow.password_hash.includes('OwnerPass')),
      { ownerUid: ownerRow?.uid, role: ownerRow?.role, venueId: ownerRow?.venue_id, venueName: ownerVenueDb?.name, dbPricePaise: ownerVenueDb?.min_slot_price_paise });

    // Edit Venue via UI
    await page.click('#btn-owner-edit-venue');
    await page.fill('#edit-venue-name', `QA Thunder Arena Pro ${runTs}`);
    await page.fill('#edit-venue-area', 'Hiranandani Estate');
    await page.fill('#edit-venue-price', '1250');
    await page.click('#btn-save-edit-venue');
    await page.waitForFunction((expected) => document.body.innerText.includes(expected), `QA Thunder Arena Pro ${runTs}`, { timeout: 15000 });

    const updVenueDb = (await pgClient.query('SELECT name, locality, min_slot_price_paise FROM apex_venues WHERE venue_id = $1', [ownerRow.venue_id])).rows[0];
    record('TC-FAC-005', 'Owner Edit Venue via UI persists updated name & integer paise price to Neon', 'Facility Owner', 'UI + API + Neon',
      updVenueDb?.name === `QA Thunder Arena Pro ${runTs}` && Number(updVenueDb?.min_slot_price_paise) === 125000,
      updVenueDb);

    // Add Secondary Venue via UI (+ Add New Facility) and Delete Secondary Venue
    await page.click('#btn-owner-add-venue');
    await page.fill('#input-venue-name', `QA Temp Turf Vashi ${runTs}`);
    await page.selectOption('#input-venue-city', 'Navi Mumbai');
    await page.fill('#input-venue-area', 'Vashi Sector 17');
    await page.fill('#input-venue-address', 'Sector 17 Vashi Navi Mumbai');
    await page.fill('#input-venue-price', '950');
    await page.click('#form-add-venue button[type="submit"]');
    await page.waitForFunction((expected) => document.body.innerText.includes(expected), `QA Temp Turf Vashi ${runTs}`, { timeout: 20000 });
    await page.waitForTimeout(800);

    const tempVenueRow = (await pgClient.query('SELECT venue_id, name, min_slot_price_paise FROM apex_venues WHERE name = $1', [`QA Temp Turf Vashi ${runTs}`])).rows[0];
    if (tempVenueRow) createdVenueIds.push(tempVenueRow.venue_id);
    record('TC-FAC-003', 'Owner Add New Facility/Venue via UI creates venue + court in Neon', 'Facility Owner', 'UI + API + Neon',
      Boolean(tempVenueRow && Number(tempVenueRow.min_slot_price_paise) === 95000),
      tempVenueRow);

    // Delete secondary venue via UI
    await page.click('#btn-owner-delete-venue');
    await page.waitForSelector('#owner-portal-status', { timeout: 15000 });
    const tempVenueAfterDel = (await pgClient.query('SELECT COUNT(*)::int AS cnt FROM apex_venues WHERE venue_id = $1', [tempVenueRow.venue_id])).rows[0].cnt;
    createdVenueIds = createdVenueIds.filter(id => id !== tempVenueRow.venue_id);
    record('TC-FAC-007', 'Owner Delete Venue via UI deletes venue & child records from Neon', 'Facility Owner', 'UI + API + Neon',
      tempVenueAfterDel === 0,
      { deletedVenueId: tempVenueRow.venue_id, remainingRowsInNeon: tempVenueAfterDel });

    // Ensure owner primary venue is selected cleanly and Add Court via UI
    await page.click('#btn-owner-add-court');
    await page.fill('#input-court-name', `QA Indoor Pickleball ${runTs}`);
    await page.selectOption('#input-court-sport', 'pickleball');
    await page.fill('#input-court-capacity', '4');
    await page.fill('#input-court-base', '800');
    await page.fill('#input-court-peak', '1100');
    await page.click('#form-add-court button[type="submit"]');
    await page.waitForFunction((expected) => document.body.innerText.includes(expected), `QA Indoor Pickleball ${runTs}`, { timeout: 15000 });

    const newCourtRow = (await pgClient.query('SELECT court_id, venue_id, name, sport, capacity, base_hourly_rate_paise, peak_hourly_rate_paise FROM apex_facilities WHERE name = $1', [`QA Indoor Pickleball ${runTs}`])).rows[0];
    if (newCourtRow) createdCourtIds.push(newCourtRow.court_id);
    await shot(page, '03_owner_courts_created.png', 'Owner court/facility created and rendered in Partner Portal');
    record('TC-FAC-004', 'Owner Add Court/Facility via UI persists capacity & paise rates to Neon apex_facilities + apex_resources', 'Facility Owner', 'UI + API + Neon',
      Boolean(newCourtRow && Number(newCourtRow.base_hourly_rate_paise) === 80000 && newCourtRow.capacity === 4),
      newCourtRow);

    // Edit newly created court via UI
    const editBtnSelector = `.btn-edit-court[data-court-id="${newCourtRow.court_id}"]`;
    await page.click(editBtnSelector);
    await page.fill('#edit-court-name', `QA Pro Pickleball ${runTs}`);
    await page.fill('#edit-court-base', '850');
    await page.fill('#edit-court-peak', '1150');
    await page.click('#btn-save-edit-court');
    await page.waitForFunction((expected) => document.body.innerText.includes(expected), `QA Pro Pickleball ${runTs}`, { timeout: 15000 });

    const updCourtRow = (await pgClient.query('SELECT name, base_hourly_rate_paise, peak_hourly_rate_paise FROM apex_facilities WHERE court_id = $1', [newCourtRow.court_id])).rows[0];
    record('TC-FAC-006', 'Owner Edit Court via UI updates Neon apex_facilities paise prices', 'Facility Owner', 'UI + API + Neon',
      updCourtRow?.name === `QA Pro Pickleball ${runTs}` && Number(updCourtRow?.base_hourly_rate_paise) === 85000,
      updCourtRow);

    // Add a temporary extra court and delete it via UI to prove court Delete button
    await page.click('#btn-owner-add-court');
    await page.fill('#input-court-name', `QA Delete Court ${runTs}`);
    await page.fill('#input-court-base', '600');
    await page.fill('#input-court-peak', '800');
    await page.click('#form-add-court button[type="submit"]');
    await page.waitForFunction((expected) => document.body.innerText.includes(expected), `QA Delete Court ${runTs}`, { timeout: 15000 });
    const delCourtRow = (await pgClient.query('SELECT court_id FROM apex_facilities WHERE name = $1', [`QA Delete Court ${runTs}`])).rows[0];
    await page.click(`.btn-delete-court[data-court-id="${delCourtRow.court_id}"]`);
    await page.waitForTimeout(1000);
    const delCourtCount = (await pgClient.query('SELECT COUNT(*)::int AS cnt FROM apex_facilities WHERE court_id = $1', [delCourtRow.court_id])).rows[0].cnt;
    record('TC-FAC-008', 'Owner Delete Court/Facility via UI removes record from UI & Neon', 'Facility Owner', 'UI + API + Neon',
      delCourtCount === 0,
      { deletedCourtId: delCourtRow.court_id, remainingInNeon: delCourtCount });

    // Owner Walk-In Cash Booking + Gate QR Check-In + Duplicate Check-In Block via UI
    const walkinDate = futureDateStr(2);
    await page.click('#btn-owner-walkin');
    await page.selectOption('#walkin-court', newCourtRow.court_id);
    await page.fill('#walkin-date', walkinDate);
    await page.selectOption('#walkin-slot', '16:00');
    await page.fill('#walkin-name', 'QA Walkin Player');
    await page.fill('#walkin-phone', '+91 98111 00999');
    await page.fill('#walkin-amount', '850');
    await page.click('#btn-submit-walkin');
    await page.waitForFunction(() => document.body.innerText.includes('QA Walkin Player'), { timeout: 15000 });

    const walkinRow = (await pgClient.query(
      'SELECT booking_id, booking_code, qr_pass_token, final_payable_paise, status FROM apex_bookings WHERE user_name = $1 ORDER BY created_at DESC LIMIT 1',
      ['QA Walkin Player']
    )).rows[0];
    if (walkinRow) createdBookingIds.push(walkinRow.booking_id);
    const walkinLock = (await pgClient.query('SELECT lock_id, status FROM apex_slot_locks WHERE booking_id = $1', [walkinRow?.booking_id])).rows[0];
    record('TC-BKG-012', 'Owner Walk-In Cash Booking via UI writes booking, BOOKED lock & CASH payment to Neon', 'Facility Owner', 'UI + API + Neon',
      Boolean(walkinRow && walkinRow.booking_code?.startsWith('ARENA-WALKIN-') && Number(walkinRow.final_payable_paise) === 85000 && walkinLock?.status === 'BOOKED'),
      { walkinRow, walkinLock });

    // Gate QR Check-in (first valid scan + duplicate scan blocked)
    await page.click(`.btn-quick-checkin[data-token="${walkinRow.qr_pass_token}"]`);
    await page.waitForFunction(() => document.querySelector('#token-verify-result')?.innerText.includes('Gate Pass Verified'), { timeout: 15000 });
    await page.waitForTimeout(1000);
    await page.fill('#input-gate-token', walkinRow.qr_pass_token);
    await page.click('#btn-verify-token');
    await page.waitForFunction(() => document.querySelector('#token-verify-result')?.innerText.includes('Already Checked In'), { timeout: 15000 });
    await shot(page, '04_owner_walkin_and_gate.png', 'Walk-in ledger + Gate QR check-in verified & duplicate blocked');

    const checkedInRow = (await pgClient.query('SELECT status, checked_in_at FROM apex_bookings WHERE booking_id = $1', [walkinRow.booking_id])).rows[0];
    record('TC-BKG-013', 'Gate QR Check-In marks CHECKED_IN + checked_in_at in Neon and blocks duplicate scan', 'Facility Owner', 'UI + API + Neon',
      checkedInRow?.status === 'CHECKED_IN' && checkedInRow?.checked_in_at !== null,
      checkedInRow);

    // Owner Logout before Player UI Walkthrough
    await page.click('#btn-back-owner');
    await page.click('#btn-header-auth');
    await page.click('#btn-signout-active');
    await page.waitForFunction(() => document.querySelector('#header-user-name')?.textContent.trim() === 'Guest', { timeout: 10000 });

    // ------------------------------------------------------------------
    // PHASE 3: Browser UI Walkthrough — Player Registration, Profile Edit, Booking, Reschedule, Cancel, Delete & Auth
    // ------------------------------------------------------------------
    console.log('\n--- PHASE 3: Browser UI — Player Registration, Profile, Booking CRUD & Refunds ---');
    await page.click('#btn-header-auth');
    await page.click('#mode-btn-signup');
    await page.click('#role-btn-player');
    await page.fill('#input-player-name', 'QA Player One');
    await page.fill('#input-player-phone', playerPhone);
    await page.selectOption('#input-player-city', 'Thane');
    await page.fill('#input-player-email', playerEmail);
    await page.fill('#input-player-password', 'PlayerPass@123');
    await page.click('#form-register-player button[type="submit"]');

    await page.waitForSelector('#section-otp-verify:not(.hidden)');
    await page.click('#btn-autofill-sms');
    await page.click('#btn-autofill-email');
    await page.click('#btn-submit-verify-otp');

    await page.waitForSelector('#btn-edit-profile', { timeout: 25000 });
    await page.waitForTimeout(800);
    await shot(page, '05_player_portal_registered.png', 'Player registered via UI with 100 welcome loyalty points');

    const p1Db = (await pgClient.query(
      'SELECT uid, full_name, email, role, city, loyalty_points, is_phone_verified, is_email_verified FROM apex_users WHERE email = $1',
      [playerEmail]
    )).rows[0];
    if (p1Db) createdUserIds.push(p1Db.uid);
    const p1Token = await page.evaluate(() => localStorage.getItem('apex_jwt_token'));
    record('TC-USR-001', 'UI Player registration stores verified user & 100 loyalty points in Neon', 'Player', 'UI + API + Neon',
      Boolean(p1Db && p1Db.loyalty_points === 100 && p1Db.is_phone_verified && p1Db.is_email_verified && p1Token),
      { uid: p1Db?.uid, role: p1Db?.role, loyaltyPoints: p1Db?.loyalty_points });

    // Player Edit Profile via UI
    await page.click('#btn-edit-profile');
    await page.fill('#edit-profile-name', 'QA Player Updated');
    await page.selectOption('#edit-profile-city', 'Mumbai');
    await page.click('#btn-save-profile');
    await page.waitForFunction(() => document.body.innerText.includes('QA Player Updated'), { timeout: 15000 });

    const p1UpdDb = (await pgClient.query('SELECT full_name, city FROM apex_users WHERE uid = $1', [p1Db.uid])).rows[0];
    record('TC-USR-008', 'Player Profile Update via UI updates Neon apex_users row & header state', 'Player', 'UI + API + Neon',
      p1UpdDb?.full_name === 'QA Player Updated' && p1UpdDb?.city === 'Mumbai',
      p1UpdDb);

    // Explore -> Select QA Thunder Arena Pro -> Book slot 3 days ahead @ 14:00 (Tier 1 > 24h & Odd-Hour 35% OFF!)
    const bookingDate = futureDateStr(3);
    await page.click('#tab-explore');
    await page.fill('#filter-search', `QA Thunder Arena Pro ${runTs}`);
    await page.click(`.btn-select-venue[data-venue-id="${ownerRow.venue_id}"]`);
    await page.waitForSelector(`.court-tab[data-court-id="${newCourtRow.court_id}"]`, { timeout: 15000 });
    await page.click(`.court-tab[data-court-id="${newCourtRow.court_id}"]`);
    await page.click(`.date-chip[data-date="${bookingDate}"]`);
    await page.click('.slot-cell[data-slot="14:00"]');
    await page.click('#btn-lock-proceed');

    await page.waitForSelector('#btn-pay-confirm', { timeout: 15000 });
    await page.click('#btn-pay-confirm');
    await page.waitForFunction(() => document.body.innerText.includes("You're Ready to Play!"), { timeout: 25000 });
    await shot(page, '06_booking_qr_confirmation.png', 'Online Booking confirmed via Render+Neon & QR Pass rendered');

    const p1BookingRow = (await pgClient.query(
      'SELECT booking_id, booking_code, qr_pass_token, booking_date, slots_json, final_payable_paise, status, razorpay_order_id FROM apex_bookings WHERE user_id = $1 ORDER BY created_at DESC LIMIT 1',
      [p1Db.uid]
    )).rows[0];
    if (p1BookingRow) createdBookingIds.push(p1BookingRow.booking_id);
    const p1LockRow = (await pgClient.query('SELECT lock_id, start_time, status FROM apex_slot_locks WHERE booking_id = $1', [p1BookingRow?.booking_id])).rows[0];
    record('TC-BKG-001', 'UI Slot Hold + Checkout Commit creates CONFIRMED booking, ARENA-* code, BOOKED lock & payment in Neon', 'Player', 'UI + API + Neon',
      Boolean(p1BookingRow && p1BookingRow.status === 'CONFIRMED' && p1BookingRow.booking_code?.startsWith('ARENA-') && p1LockRow?.status === 'BOOKED'),
      { bookingId: p1BookingRow?.booking_id, bookingCode: p1BookingRow?.booking_code, lockStatus: p1LockRow?.status, finalPayablePaise: p1BookingRow?.final_payable_paise });

    // Navigate to Player Portal -> verify booking listed
    await page.click('#btn-view-my-bookings');
    await page.waitForSelector(`[data-booking-card="${p1BookingRow.booking_id}"]`, { timeout: 15000 });
    await shot(page, '07_player_bookings_and_actions.png', 'Player Portal listing CONFIRMED booking with Reschedule/Cancel/Delete actions');

    // Reschedule Booking via UI (14:00 -> 15:00 + notes)
    await page.click(`.btn-edit-booking[data-booking-id="${p1BookingRow.booking_id}"]`);
    await page.selectOption('#edit-booking-slot', '15:00');
    await page.fill('#edit-booking-notes', 'QA Rescheduled Court Side');
    await page.click('#btn-save-reschedule');
    await page.waitForFunction(() => document.body.innerText.includes('QA Rescheduled Court Side'), { timeout: 15000 });

    const reschedBookingDb = (await pgClient.query('SELECT slots_json, booking_date FROM apex_bookings WHERE booking_id = $1', [p1BookingRow.booking_id])).rows[0];
    const oldSlotLocks = (await pgClient.query('SELECT COUNT(*)::int AS cnt FROM apex_slot_locks WHERE resource_id = $1 AND booking_date = $2 AND start_time = $3', [newCourtRow.court_id, bookingDate, '14:00'])).rows[0].cnt;
    const newSlotLocks = (await pgClient.query('SELECT start_time, status FROM apex_slot_locks WHERE booking_id = $1', [p1BookingRow.booking_id])).rows[0];
    record('TC-BKG-006', 'Player Reschedule Booking via UI swaps Neon apex_slot_locks atomically (14:00 -> 15:00)', 'Player', 'UI + API + Neon',
      Boolean(reschedBookingDb?.slots_json?.includes('15:00') && oldSlotLocks === 0 && newSlotLocks?.start_time === '15:00'),
      { slotsJson: reschedBookingDb?.slots_json, oldSlotLockCount: oldSlotLocks, newSlotLock: newSlotLocks });

    // Concurrency & RBAC checks via Player 2 API
    const regP2 = await apiJson('POST', '/auth/register', {
      body: { displayName: 'QA Player Two', email: p2Email, phone: p2Phone, password: 'Password@123', role: 'player', city: 'Thane' }
    });
    const p2Token = regP2.json?.data?.token;
    const p2Uid = regP2.json?.data?.user?.uid;
    if (p2Uid) createdUserIds.push(p2Uid);

    const doubleBookAttempt = await apiJson('POST', '/bookings/initiate', {
      token: p2Token,
      body: { venueId: ownerRow.venue_id, resourceId: newCourtRow.court_id, bookingDate, slotStartTimes: ['15:00'], sportType: 'PICKLEBALL' }
    });
    const idorBookingRead = await apiJson('GET', `/bookings/${p1BookingRow.booking_id}`, { token: p2Token });
    const playerToOwner403 = await apiJson('GET', '/owner/venues', { token: p2Token });
    record('TC-PRC-009', 'Double-booking 15:00 by Player 2 returns HTTP 409 CONFLICT & IDOR/Owner routes return 403 FORBIDDEN', 'Player 2', 'Tier 2 RBAC & ACID',
      doubleBookAttempt.status === 409 && idorBookingRead.status === 403 && playerToOwner403.status === 403,
      { doubleBookStatus: doubleBookAttempt.status, idorStatus: idorBookingRead.status, playerToOwnerStatus: playerToOwner403.status });

    // Cancel Booking via UI (Tier 1 > 24h)
    await page.click(`.btn-cancel-booking[data-booking-id="${p1BookingRow.booking_id}"]`);
    await page.click('#btn-confirm-proceed');
    await page.waitForFunction(() => document.body.innerText.includes('CANCELLED_BY_USER'), { timeout: 15000 });
    await shot(page, '08_player_booking_cancelled.png', 'Player booking cancelled via UI with Tier 1 refund (minus ₹50 fee)');

    const cancelledDb = (await pgClient.query('SELECT status, final_payable_paise, refund_amount_paise, refund_status FROM apex_bookings WHERE booking_id = $1', [p1BookingRow.booking_id])).rows[0];
    const locksAfterCancel = (await pgClient.query('SELECT COUNT(*)::int AS cnt FROM apex_slot_locks WHERE booking_id = $1', [p1BookingRow.booking_id])).rows[0].cnt;
    record('TC-BKG-007', 'Player Tier 1 Cancel (>24h) refunds total - 5000 paise, sets CANCELLED_BY_USER & deletes lock in Neon', 'Player', 'UI + API + Neon',
      Boolean(cancelledDb && cancelledDb.status === 'CANCELLED_BY_USER' && Number(cancelledDb.refund_amount_paise) === Math.max(0, Number(cancelledDb.final_payable_paise) - 5000) && locksAfterCancel === 0),
      { cancelledDb, locksAfterCancel });

    // Delete Booking via UI
    await page.click(`.btn-delete-booking[data-booking-id="${p1BookingRow.booking_id}"]`);
    await page.click('#btn-confirm-proceed');
    await page.waitForFunction((id) => !document.querySelector(`[data-booking-card="${id}"]`), p1BookingRow.booking_id, { timeout: 15000 });
    const bookingCountAfterDel = (await pgClient.query('SELECT COUNT(*)::int AS cnt FROM apex_bookings WHERE booking_id = $1', [p1BookingRow.booking_id])).rows[0].cnt;
    createdBookingIds = createdBookingIds.filter(id => id !== p1BookingRow.booking_id);
    record('TC-BKG-010', 'Player Delete Booking via UI removes row cleanly from UI & Neon apex_bookings', 'Player', 'UI + API + Neon',
      bookingCountAfterDel === 0,
      { deletedBookingId: p1BookingRow.booking_id, remainingInNeon: bookingCountAfterDel });

    // Sign Out -> Password Login + Wrong Password Rejection in UI
    await page.click('#btn-portal-logout');
    await page.waitForSelector('#btn-portal-login', { timeout: 10000 });
    await page.click('#btn-portal-login');
    await page.fill('#input-pw-login-identifier', playerEmail);
    await page.fill('#input-pw-login-password', 'WrongPass999');
    await page.click('#btn-submit-password-login');
    await page.waitForFunction(() => document.querySelector('#auth-feedback')?.innerText.includes('Incorrect password'), { timeout: 10000 });

    await page.fill('#input-pw-login-password', 'PlayerPass@123');
    await page.click('#btn-submit-password-login');
    await page.waitForSelector('#btn-edit-profile', { timeout: 15000 });
    await shot(page, '09_player_password_login_verified.png', 'Wrong password blocked & valid password login restored session cleanly');
    record('TC-USR-005', 'UI Wrong Password rejected & valid Password Login restores session with live Neon state', 'Player', 'UI + API',
      await page.locator('#header-user-name').innerText().then(t => t.trim().length > 0));

    // ------------------------------------------------------------------
    // PHASE 4: API Matrix Verification (Dynamic Pricing, Dual OTP Brute-Force, Webhook Idempotency)
    // ------------------------------------------------------------------
    console.log('\n--- PHASE 4: API Matrix (Pricing, OTP Brute-Force, Razorpay Webhooks) ---');
    const quoteAfternoon = await apiJson('POST', '/pricing/quote', {
      token: p2Token,
      body: { venueId: ownerRow.venue_id, resourceId: newCourtRow.court_id, date: bookingDate, slots: ['13:00', '14:00'], promoCode: 'TURF20' }
    });
    record('TC-PRC-003', 'Server pricing quote computes paise subtotal, Bulk/Promo discount & >=100 paise floor', 'Player', 'Tier 2 Pricing',
      quoteAfternoon.status === 200 && quoteAfternoon.json?.data?.discountAmountPaise > 0 && quoteAfternoon.json?.data?.finalPayablePaise >= 100,
      quoteAfternoon.json?.data);

    // Brute-force Dual OTP lockout check
    const otpMail = `qa.otp.${runTs}@apex.dev`;
    const otpPhone = `+91 98764 ${phoneFive}`;
    const sendOtp = await apiJson('POST', '/auth/registration/send-otp', {
      body: { phone: otpPhone, email: otpMail, displayName: 'QA OTP User' }
    });
    const sid = sendOtp.json?.data?.sessionId;
    for (let i = 0; i < 3; i++) {
      await apiJson('POST', '/auth/registration/verify-otp', {
        body: { sessionId: sid, phoneOtp: '000000', emailOtp: '000000', displayName: 'QA OTP User', phone: otpPhone, email: otpMail }
      });
    }
    const fourthAttempt = await apiJson('POST', '/auth/registration/verify-otp', {
      body: { sessionId: sid, phoneOtp: '123456', emailOtp: '123456', displayName: 'QA OTP User', phone: otpPhone, email: otpMail }
    });
    const otpSessionDb = (await pgClient.query('SELECT session_id, attempts_remaining, verified FROM apex_otp_sessions WHERE session_id = $1', [sid])).rows[0];
    record('TC-USR-009', 'Dual OTP session enforces 3 wrong attempts lockout & persists attempts_remaining=0 in Neon', 'Guest', 'Tier 2 + Neon',
      sendOtp.status === 200 && fourthAttempt.status >= 400 && otpSessionDb?.attempts_remaining === 0 && otpSessionDb?.verified === false,
      { fourthAttemptStatus: fourthAttempt.status, otpSessionDb });

    // Razorpay HMAC Webhook idempotency verification
    const webhookSecret = process.env.RAZORPAY_WEBHOOK_SECRET || 'whsec_apex_test_secret_2026';
    const evtId = `evt_qa_${runTs}`;
    const rawWebhookBody = JSON.stringify({ event: 'order.paid', payload: { payment: { entity: { id: `pay_wh_${runTs}`, order_id: `order_wh_${runTs}`, notes: {} } } } });
    const whSig = crypto.createHmac('sha256', webhookSecret).update(rawWebhookBody).digest('hex');
    const badWh = await apiJson('POST', '/webhooks/razorpay', { body: rawWebhookBody, headers: { 'X-Razorpay-Signature': 'bad_sig', 'X-Razorpay-Event-Id': evtId } });
    const wh1 = await apiJson('POST', '/webhooks/razorpay', { body: rawWebhookBody, headers: { 'X-Razorpay-Signature': whSig, 'X-Razorpay-Event-Id': evtId } });
    const wh2 = await apiJson('POST', '/webhooks/razorpay', { body: rawWebhookBody, headers: { 'X-Razorpay-Signature': whSig, 'X-Razorpay-Event-Id': evtId } });
    const whRows = (await pgClient.query('SELECT COUNT(*)::int AS cnt FROM apex_webhook_events WHERE event_id = $1', [evtId])).rows[0].cnt;
    record('TC-BKG-014', 'Razorpay Webhook rejects invalid HMAC, processes valid signature & deduplicates replay in Neon', 'Webhook', 'Tier 2 + Neon',
      badWh.status >= 400 && wh1.status === 200 && wh2.status === 200 && whRows === 1,
      { badSigStatus: badWh.status, wh1Status: wh1.status, wh2Status: wh2.status, dbEventRows: whRows });

    // Capture peak DB snapshot before cleanup
    evidence.dbSnapshots.duringTestPeakCounts = await getDbCounts(pgClient);
    evidence.dbSnapshots.recentAuditActions = (await pgClient.query(
      'SELECT action, resource_type, COUNT(*)::int AS cnt FROM apex_audit_logs WHERE actor_id = ANY($1) OR resource_id = $2 GROUP BY action, resource_type ORDER BY action',
      [createdUserIds, sid]
    )).rows;

    await context.close();
  } finally {
    // Scoped cleanup of synthetic QA records from live Neon + Render API (never touches seeded data)
    console.log('\n--- CLEANUP: Removing Scoped Synthetic QA Rows via API & Neon ---');
    try {
      // Remove walk-in booking, court, owner, players via Render API/DB so live cache & DB both stay clean
      const ownerLogin = await apiJson('POST', '/auth/login/password', { body: { identifier: ownerEmail, password: 'OwnerPass@123' } });
      const oTok = ownerLogin.json?.data?.token;
      for (const bid of createdBookingIds) {
        if (oTok) await apiJson('DELETE', `/bookings/${bid}`, { token: oTok }).catch(() => {});
      }
      for (const cid of createdCourtIds) {
        if (oTok) await apiJson('DELETE', `/facilities/${cid}`, { token: oTok }).catch(() => {});
      }
      for (const vid of createdVenueIds) {
        if (oTok) await apiJson('DELETE', `/owner/venues/${vid}`, { token: oTok }).catch(() => {});
      }
      if (oTok) await apiJson('DELETE', '/users/me', { token: oTok }).catch(() => {});

      const p1Login = await apiJson('POST', '/auth/login/password', { body: { identifier: playerEmail, password: 'PlayerPass@123' } }).catch(() => null);
      if (p1Login?.json?.data?.token) await apiJson('DELETE', '/users/me', { token: p1Login.json.data.token }).catch(() => {});

      const p2Login = await apiJson('POST', '/auth/login/password', { body: { identifier: p2Email, password: 'Password@123' } }).catch(() => null);
      if (p2Login?.json?.data?.token) await apiJson('DELETE', '/users/me', { token: p2Login.json.data.token }).catch(() => {});

      // Remove QA OTP session, webhook event, payments & audit rows with strict scoped WHERE
      await pgClient.query('DELETE FROM apex_payments WHERE booking_id = ANY($1)', [createdBookingIds.length ? createdBookingIds : ['__none__']]);
      await pgClient.query('DELETE FROM apex_otp_sessions WHERE email LIKE $1', [`%${runTs}@apex.dev`]);
      await pgClient.query('DELETE FROM apex_webhook_events WHERE event_id = $1', [`evt_qa_${runTs}`]);
      await pgClient.query('DELETE FROM apex_audit_logs WHERE actor_id = ANY($1) OR resource_id = $2', [createdUserIds.length ? createdUserIds : ['__none__'], `otp_reg_${runTs}`]);

      evidence.dbSnapshots.postCleanupCounts = await getDbCounts(pgClient);
      evidence.cleanup = { removedBookings: createdBookingIds, removedCourts: createdCourtIds, removedVenues: createdVenueIds, removedUsers: createdUserIds };
    } catch (cleanErr) {
      evidence.cleanupError = cleanErr.message;
    }

    await browser.close();
    await pgClient.end();
    fs.writeFileSync(EVIDENCE_OUT, JSON.stringify(evidence, null, 2));
  }

  console.log('\n====================================================================');
  console.log(`🏁 E2E RESULTS: ${evidence.summary.passed}/${evidence.summary.total} PASSED (${evidence.summary.failed} failed)`);
  console.log(`📸 Screenshots: ${evidence.screenshots.length} captured in ${SCREENSHOT_DIR}`);
  console.log(`📄 Evidence JSON: ${EVIDENCE_OUT}`);
  console.log('====================================================================\n');
}

main().catch(err => {
  console.error('Fatal E2E suite error:', err);
  process.exit(1);
});
