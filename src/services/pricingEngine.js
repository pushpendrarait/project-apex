/**
 * Project Apex - Dynamic Pricing & Promotional Discount Engine
 * Evaluates time-band elasticity, odd-hour stimulation, and promo validation
 */

export const ODD_HOUR_BANDS = [
  { start: "06:00", end: "08:00", name: "Early Bird", discountPct: 20 },
  { start: "11:00", end: "16:00", name: "Afternoon Grind", discountPct: 35 },
  { start: "23:00", end: "02:00", name: "Night Owl Turf", discountPct: 25 }
];

export const PEAK_HOUR_BANDS = [
  { start: "19:00", end: "23:00", name: "Prime Time", multiplier: 1.2 }
];

export const PROMO_CODES = {
  "THANE50": {
    code: "THANE50",
    type: "FLAT",
    value: 50,
    minSpend: 500,
    cityRestriction: "Thane",
    description: "Flat ₹50 OFF for sports bookings in Thane"
  },
  "APEX20": {
    code: "APEX20",
    type: "PERCENTAGE",
    value: 20,
    maxDiscount: 200,
    minSpend: 600,
    description: "20% OFF (Up to ₹200) for all sports enthusiasts"
  },
  "ODDHOUR35": {
    code: "ODDHOUR35",
    type: "PERCENTAGE",
    value: 10,
    requiresOddHour: true,
    description: "Extra 10% bonus discount on already reduced odd-hour slots"
  }
};

/**
 * Checks if a given time slot (HH:MM) falls into odd-hour bands
 */
export function evaluateSlotTimeBand(slotTime) {
  const [slotHour] = slotTime.split(':').map(Number);
  
  // Early Bird 06:00 - 08:00
  if (slotHour >= 6 && slotHour < 8) {
    return { isOddHour: true, bandName: "Early Bird", discountPct: 20, isPeak: false };
  }
  // Afternoon Grind 11:00 - 16:00
  if (slotHour >= 11 && slotHour < 16) {
    return { isOddHour: true, bandName: "Afternoon Grind (Off-Peak)", discountPct: 35, isPeak: false };
  }
  // Night Owl 23:00 - 02:00
  if (slotHour >= 23 || slotHour < 2) {
    return { isOddHour: true, bandName: "Night Owl Turf", discountPct: 25, isPeak: false };
  }
  // Prime Time 19:00 - 23:00
  if (slotHour >= 19 && slotHour < 23) {
    return { isOddHour: false, bandName: "Prime Time Peak", discountPct: 0, isPeak: true, multiplier: 1.2 };
  }

  // Standard regular hours
  return { isOddHour: false, bandName: "Standard Hours", discountPct: 0, isPeak: false, multiplier: 1.0 };
}

/**
 * Calculate dynamic price breakdown for a court booking
 */
export function calculateSlotPrice({ court, slotTime, promoCode = null, city = "Thane" }) {
  const baseRate = court.baseHourlyRate || 1000;
  const band = evaluateSlotTimeBand(slotTime);

  let slotBasePrice = baseRate;
  let oddHourDiscountAmount = 0;

  if (band.isPeak) {
    slotBasePrice = Math.round(baseRate * (band.multiplier || 1.2));
  } else if (band.isOddHour) {
    oddHourDiscountAmount = Math.round(baseRate * (band.discountPct / 100));
  }

  const subtotal = slotBasePrice - oddHourDiscountAmount;

  // Evaluate promo code
  let promoDiscountAmount = 0;
  let promoError = null;

  if (promoCode) {
    const promo = PROMO_CODES[promoCode.trim().toUpperCase()];
    if (!promo) {
      promoError = "Invalid coupon code";
    } else if (promo.cityRestriction && promo.cityRestriction.toLowerCase() !== city.toLowerCase()) {
      promoError = `Coupon valid only in ${promo.cityRestriction}`;
    } else if (promo.minSpend && subtotal < promo.minSpend) {
      promoError = `Minimum spend of ₹${promo.minSpend} required`;
    } else if (promo.requiresOddHour && !band.isOddHour) {
      promoError = "Coupon valid exclusively on Odd-Hour discounted slots";
    } else {
      if (promo.type === "FLAT") {
        promoDiscountAmount = promo.value;
      } else if (promo.type === "PERCENTAGE") {
        const calculated = Math.round(subtotal * (promo.value / 100));
        promoDiscountAmount = promo.maxDiscount ? Math.min(calculated, promo.maxDiscount) : calculated;
      }
    }
  }

  const convenienceFee = 20; // Nominal platform processing fee
  const finalTotal = Math.max(0, subtotal - promoDiscountAmount + convenienceFee);

  return {
    baseRate,
    bandName: band.bandName,
    isOddHour: band.isOddHour,
    isPeak: band.isPeak,
    discountPct: band.discountPct || 0,
    oddHourDiscountAmount,
    subtotal,
    promoCode: promoDiscountAmount > 0 ? promoCode.toUpperCase() : null,
    promoDiscountAmount,
    promoError,
    convenienceFee,
    total: finalTotal,
    savingsTotal: oddHourDiscountAmount + promoDiscountAmount
  };
}
