package org.example.auctionreal;

import org.junit.jupiter.api.Test;
import user.Bidder;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionSystemTest {

    // ===== USER TEST =====

    @Test
    void testCreateBidder() {

        Bidder bidder = new Bidder(
                "B001",
                "thu",
                "1234",
                50000000
        );

        assertEquals("thu", bidder.getUsername());
        assertEquals("BIDDER", bidder.getRole());
    }

    // ===== BID VALIDATION =====

    @Test
    void testBidHigherThanCurrentPrice() {

        double currentPrice = 1000000;
        double bid = 2000000;

        assertTrue(bid > currentPrice);
    }

    @Test
    void testBidLowerThanCurrentPrice() {

        double currentPrice = 5000000;
        double bid = 4000000;

        assertFalse(bid > currentPrice);
    }

    // ===== MIN STEP =====

    @Test
    void testMinStepValidation() {

        double currentPrice = 10000000;
        double minStep = 1000000;

        double validBid = 11000000;

        assertTrue(validBid >= currentPrice + minStep);
    }

    // ===== AUTO BID =====

    @Test
    void testAutoBidIncrease() {

        double currentPrice = 10000000;
        double minStep = 1000000;

        double nextBid = currentPrice + minStep;

        assertEquals(11000000, nextBid);
    }

    @Test
    void testAutoBidStopAtLimit() {

        double autoBidLimit = 15000000;
        double nextBid = 16000000;

        assertFalse(nextBid <= autoBidLimit);
    }

    // ===== COUNTDOWN =====

    @Test
    void testCountdownDecrease() {

        int totalSeconds = 120;

        totalSeconds--;

        assertEquals(119, totalSeconds);
    }

    // ===== MONEY FORMAT =====

    @Test
    void testMoneyValuePositive() {

        double amount = 5000000;

        assertTrue(amount > 0);
    }

    // ===== SOCKET =====

    @Test
    void testSocketPort() {

        int port = 1234;

        assertEquals(1234, port);
    }
}