/**
 * Project Apex - Legal Framework Modal
 * Indian Jurisdiction Compliance: IT Act 2000, DPDP Act 2023 & Sports Injury Waivers
 */

export class LegalModal {
  static show(type = 'tos') {
    let modal = document.getElementById('apex-legal-modal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'apex-legal-modal';
      modal.className = 'fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-fade-in';
      document.body.appendChild(modal);
    }

    const content = this.getContent(type);

    modal.innerHTML = `
      <div class="bg-slate-900 border border-slate-800 rounded-3xl max-w-2xl w-full p-6 sm:p-8 shadow-2xl relative overflow-hidden text-white flex flex-col max-h-[85vh]">
        <!-- Header -->
        <div class="flex items-center justify-between pb-4 border-b border-slate-800">
          <div>
            <div class="text-[10px] font-bold uppercase tracking-wider text-emerald-400">Legal & Regulatory Compliance</div>
            <h3 class="text-xl font-black text-white">${content.title}</h3>
          </div>
          <button id="btn-close-legal" class="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300">
            ✕
          </button>
        </div>

        <!-- Scrollable Body -->
        <div class="overflow-y-auto py-4 space-y-4 text-xs text-slate-300 leading-relaxed pr-2">
          ${content.body}
        </div>

        <!-- Footer -->
        <div class="pt-4 border-t border-slate-800 flex justify-end">
          <button id="btn-agree-legal" class="px-6 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs transition">
            Close & Acknowledge
          </button>
        </div>
      </div>
    `;

    const close = () => modal.remove();
    modal.querySelector('#btn-close-legal').addEventListener('click', close);
    modal.querySelector('#btn-agree-legal').addEventListener('click', close);
  }

  static getContent(type) {
    switch (type) {
      case 'injury':
        return {
          title: "Sports Injury & Property Damage Liability Waiver",
          body: `
            <div class="bg-amber-950/40 border border-amber-500/30 p-3 rounded-xl text-amber-200 text-[11px] mb-3">
              <strong>MANDATORY ACKNOWLEDGMENT:</strong> Participating in turf sports involves inherent physical risks.
            </div>
            <p><strong>1. Voluntary Assumption of Inherent Risk:</strong> By reserving or entering any venue listed on Project Apex (including box cricket turfs, football pitches, badminton, pickleball, and squash courts), the participant expressly recognizes that athletic activity entails known and unanticipated risks of physical injury (including muscle sprains, ligament tears, fractures, head impacts, cardiac events, or heat exhaustion).</p>
            <p><strong>2. Intermediary Status & Exemption:</strong> Project Apex operates solely as an electronic booking platform and neutral intermediary under Section 79 of the Information Technology Act 2000. Project Apex and its developers/operators assume zero liability for injuries sustained at partner facilities.</p>
            <p><strong>3. Facility Property & Equipment Damage:</strong> Players and booking organizers are jointly liable for any intentional or negligent destruction of venue assets (floodlights, boundary nets, pitch turf, glass panels, or rental gear).</p>
            <p><strong>4. Weather & Force Majeure:</strong> In case of sudden monsoon downpours or municipal power outages, venue managers reserve the right to reschedule play or issue platform credits.</p>
          `
        };
      case 'privacy':
        return {
          title: "Player Privacy Policy (DPDP Act 2023 Compliant)",
          body: `
            <p><strong>1. Data Collection & Purpose:</strong> Project Apex collects user name, mobile number, email, and location solely for authenticating slot bookings, issuing scannable QR gate passes, and processing transaction receipts.</p>
            <p><strong>2. Firebase Spark Security:</strong> User credentials and records are stored within Google Cloud Firestore and protected by strict Firestore Security Rules. Cloud Functions are completely excluded, ensuring no unauthorized server-side compute leaks.</p>
            <p><strong>3. Non-Disclosure:</strong> We never sell player telemetry or contact numbers to third-party marketing brokers. Venue operators receive only the player name and booking token necessary to facilitate entry.</p>
            <p><strong>4. Data Subject Rights:</strong> Under India's Digital Personal Data Protection Act 2023, players may request account deletion or data portability at any time.</p>
          `
        };
      default:
        return {
          title: "Terms of Service & Platform Intermediary Agreement",
          body: `
            <p><strong>1. Acceptance of Terms:</strong> By accessing Project Apex, you agree to comply with Indian cyber laws, local Maharashtra sports venue safety rules, and platform reservation standards.</p>
            <p><strong>2. 5-Minute Concurrency Lock Lease:</strong> When initiating checkout, you hold a temporary 5-minute atomic lock. Failure to complete payment within this window will automatically release the slot back to the public pool.</p>
            <p><strong>3. 3-Tier Cancellation Policy:</strong> Cancellations made >24 hours prior receive 100% refund (minus ₹50 payment gateway fee); cancellations 6-24 hours prior receive 50% refund; cancellations <6 hours are non-refundable.</p>
            <p><strong>4. Jurisdiction:</strong> Any legal disputes or claims arising out of bookings within Mumbai or Thane shall be subject to the exclusive jurisdiction of the competent courts in Thane / Mumbai, Maharashtra.</p>
          `
        };
    }
  }
}
