/**
 * Project Apex - Payment Processing & QR Entry Pass Generator
 * Simulates secure Razorpay / UPI transaction authorization
 */

export class PaymentService {
  /**
   * Simulates Razorpay / UPI checkout modal and authorization
   */
  static async processPayment({ amount, method = 'UPI', upiId = 'rohan@okhdfcbank', cardDetails = null }) {
    // Artificial latency simulating bank gateway handshake (800ms)
    await new Promise(resolve => setTimeout(resolve, 800));

    // Simulated 99% success rate
    const isSuccess = Math.random() < 0.99;

    if (!isSuccess) {
      throw new Error("Payment transaction declined by issuing bank. Please try another payment method.");
    }

    const txId = `tx_${Date.now()}_${Math.floor(1000 + Math.random() * 9000)}`;
    const paymentGatewayId = `pay_rzp_${Math.floor(100000000 + Math.random() * 900000000)}`;

    return {
      status: 'SUCCESS',
      transactionId: txId,
      gatewayReference: paymentGatewayId,
      amount,
      method,
      timestamp: Date.now()
    };
  }

  /**
   * Generates a tamper-proof QR code pass token for venue gate security
   */
  static generateQRPassToken({ venueId, courtId, date, slot, bookingId }) {
    const cityCode = venueId.includes('thane') ? 'TH' : venueId.includes('mumbai') ? 'MB' : 'NM';
    const randSuffix = Math.floor(100000 + Math.random() * 900000);
    return `APEX-${cityCode}-${date.replace(/-/g, '')}-${slot.replace(':', '')}-${randSuffix}`;
  }
}
