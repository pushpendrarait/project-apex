package com.projectapex.scheduler;

import com.projectapex.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingHoldExpiryScheduler {
    private static final Logger log = LoggerFactory.getLogger(BookingHoldExpiryScheduler.class);

    private final BookingService bookingService;

    public BookingHoldExpiryScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Periodically releases expired 10-minute slot holds
     */
    @Scheduled(fixedDelay = 60000)
    public void cleanupExpiredHolds() {
        int released = bookingService.cleanupExpiredHolds();
        if (released > 0) {
            log.info("BookingHoldExpiryScheduler: Cleaned up {} expired booking holds and released slot locks", released);
        }
    }
}
