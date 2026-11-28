package com.projectapex.controller;

import com.projectapex.dto.request.CreateDiscountRequest;
import com.projectapex.dto.response.ApiResponse;
import com.projectapex.model.DiscountRecord;
import com.projectapex.service.FirebaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/owner")
@CrossOrigin(origins = "*")
public class OwnerDiscountController {

    private final FirebaseService firebaseService;

    public OwnerDiscountController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @PostMapping("/discounts")
    public ResponseEntity<ApiResponse<DiscountRecord>> createDiscount(
            @Valid @RequestBody CreateDiscountRequest req,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        String discountId = "disc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        DiscountRecord d = new DiscountRecord(
                discountId,
                ownerId,
                req.getName(),
                req.getType(),
                req.getCode(),
                req.getValue(),
                req.getValueType()
        );
        d.setMaxDiscountPaise(req.getMaxDiscountPaise());
        d.setMinOrderPaise(req.getMinOrderPaise());
        d.setMinSlots(req.getMinSlots());
        d.setDaysOfWeek(req.getDaysOfWeek());
        d.setTimeWindowStart(req.getTimeWindowStart());
        d.setTimeWindowEnd(req.getTimeWindowEnd());
        d.setApplicableVenueIds(req.getApplicableVenueIds());
        d.setApplicableResourceIds(req.getApplicableResourceIds());
        d.setApplicableSports(req.getApplicableSports());
        d.setValidFrom(req.getValidFrom());
        d.setValidUntil(req.getValidUntil());
        d.setMaxTotalUses(req.getMaxTotalUses());
        d.setMaxUsesPerUser(req.getMaxUsesPerUser());
        d.setStackableWithAutoDiscount(req.isStackableWithAutoDiscount());
        d.setActive(true);

        firebaseService.getDiscounts().put(discountId, d);
        return ResponseEntity.ok(ApiResponse.ok("Discount created", d));
    }

    @GetMapping("/discounts")
    public ResponseEntity<ApiResponse<List<DiscountRecord>>> getOwnerDiscounts(Authentication auth) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        List<DiscountRecord> list = firebaseService.getDiscounts().values().stream()
                .filter(d -> ownerId.equals(d.getOwnerId()) || "admin".equalsIgnoreCase(ownerId))
                .sorted(Comparator.comparing(DiscountRecord::getCreatedAt).reversed())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Discounts retrieved", list));
    }

    @PutMapping("/discounts/{discountId}")
    public ResponseEntity<ApiResponse<DiscountRecord>> updateDiscount(
            @PathVariable String discountId,
            @RequestBody CreateDiscountRequest req,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        DiscountRecord d = firebaseService.getDiscounts().get(discountId);
        if (d == null) {
            throw new IllegalArgumentException("Discount not found: " + discountId);
        }
        if (!d.getOwnerId().equals(ownerId) && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized");
        }

        if (req.getName() != null) d.setName(req.getName());
        if (req.getValue() > 0) d.setValue(req.getValue());
        if (req.getMaxDiscountPaise() > 0) d.setMaxDiscountPaise(req.getMaxDiscountPaise());
        if (req.getMinOrderPaise() > 0) d.setMinOrderPaise(req.getMinOrderPaise());
        firebaseService.persistDiscount(d);

        return ResponseEntity.ok(ApiResponse.ok("Discount updated", d));
    }

    @PatchMapping("/discounts/{discountId}/toggle")
    public ResponseEntity<ApiResponse<DiscountRecord>> toggleDiscount(
            @PathVariable String discountId,
            Authentication auth
    ) {
        String ownerId = auth != null ? auth.getName() : "demo_owner";
        DiscountRecord d = firebaseService.getDiscounts().get(discountId);
        if (d == null) {
            throw new IllegalArgumentException("Discount not found: " + discountId);
        }
        if (!d.getOwnerId().equals(ownerId) && !"admin".equalsIgnoreCase(ownerId)) {
            throw new IllegalStateException("Unauthorized");
        }

        d.setActive(!d.isActive());
        firebaseService.persistDiscount(d);
        return ResponseEntity.ok(ApiResponse.ok("Discount status updated", d));
    }
}
