package com.projectapex.controller;

import com.projectapex.dto.request.CreateRazorpayOrderRequest;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.dto.response.RazorpayOrderResponse;
import com.projectapex.model.FacilityRecord;
import com.projectapex.model.VenueRecord;
import com.projectapex.service.FirebaseService;
import com.projectapex.service.PricingService;
import com.projectapex.service.RazorpayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final RazorpayService razorpayService;
    private final PricingService pricingService;
    private final FirebaseService firebaseService;

    public PaymentController(RazorpayService razorpayService,
                             PricingService pricingService,
                             FirebaseService firebaseService) {
        this.razorpayService = razorpayService;
        this.pricingService = pricingService;
        this.firebaseService = firebaseService;
    }

    /**
     * Initializes a verified order via the Razorpay Orders API
     */
    @PostMapping("/razorpay/create-order")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createRazorpayOrder(
            @Valid @RequestBody CreateRazorpayOrderRequest req) {
        FacilityRecord facility = firebaseService.getCourts().get(req.getCourtId());
        PricingService.PricingResult pricing = pricingService.calculatePrice(facility, req.getSlot(), req.getPromoCode());
        int finalAmount = pricing.total > 0 ? pricing.total : req.getAmountInRupees();

        String receiptId = "rcpt_" + req.getDate().replace("-", "") + "_" + (1000 + (int)(Math.random() * 9000));
        String orderId = razorpayService.createOrder(finalAmount, receiptId);

        VenueRecord venue = facility != null ? firebaseService.getVenues().get(facility.getVenueId()) : null;

        RazorpayOrderResponse res = new RazorpayOrderResponse(
                orderId,
                razorpayService.getKeyId(),
                (long) finalAmount * 100, // in paise
                "INR"
        );
        res.setVenueName(venue != null ? venue.getName() : "Apex Sports Arena");
        res.setCourtName(facility != null ? facility.getName() : "Main Pitch");
        res.setDate(req.getDate());
        res.setSlot(req.getSlot());

        return ResponseEntity.ok(ApiResponse.ok("Razorpay Order created successfully", res));
    }
}
