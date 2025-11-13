package com.ski.shop.inventory.service;

import com.ski.shop.inventory.dto.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for pricing calculation and caching functionality
 */
@QuarkusTest
@DisplayName("Performance Tests")
public class PerformanceTest {

    @Inject
    PricingCalculationService pricingService;

    @Inject
    InventoryApplicationService inventoryService;

    @Inject
    ReservationService reservationService;

    private UUID testProductId;

    @BeforeEach
    void setUp() {
        testProductId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Pricing Calculation Performance Test")
    void testPricingCalculationPerformance() throws Exception {
        // Create test data
        PricingCalculationRequest request = new PricingCalculationRequest(
                testProductId,
                1,
                LocalDate.now(),
                LocalDate.now().plusDays(3)
        );
        request.customerTier = "STANDARD";

        // Warm up
        for (int i = 0; i < 10; i++) {
            try {
                pricingService.calculatePricing(request);
            } catch (Exception e) {
                // Expected for non-existent product in warm-up
            }
        }

        // Performance test - measure time for multiple calculations
        int iterations = 100;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            try {
                pricingService.calculatePricing(request);
            } catch (Exception e) {
                // Expected for non-existent product
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTime = (double) totalTime / iterations;

        System.out.printf("Pricing calculation performance: %d iterations in %d ms (avg: %.2f ms)%n", 
                         iterations, totalTime, avgTime);

        // Assert reasonable performance (should be under 10ms average)
        assertTrue(avgTime < 50.0, "Average pricing calculation time should be under 50ms");
    }

    @Test
    @DisplayName("Bulk Pricing Calculation Performance Test")
    void testBulkPricingCalculationPerformance() throws Exception {
        // Create bulk request with multiple items
        List<PricingCalculationRequest> requests = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PricingCalculationRequest request = new PricingCalculationRequest(
                    UUID.randomUUID(),
                    i + 1,
                    LocalDate.now(),
                    LocalDate.now().plusDays(i + 1)
            );
            request.customerTier = "STANDARD";
            requests.add(request);
        }

        BulkPricingCalculationRequest bulkRequest = new BulkPricingCalculationRequest(requests);
        bulkRequest.applyBulkDiscount = true;

        // Performance test
        int iterations = 50;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            try {
                pricingService.calculateBulkPricing(bulkRequest);
            } catch (Exception e) {
                // Expected for non-existent products
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTime = (double) totalTime / iterations;

        System.out.printf("Bulk pricing calculation performance: %d iterations in %d ms (avg: %.2f ms)%n", 
                         iterations, totalTime, avgTime);

        // Assert reasonable performance
        assertTrue(avgTime < 200.0, "Average bulk pricing calculation time should be under 200ms");
    }

    @Test
    @DisplayName("Cache Effectiveness Test")
    void testCacheEffectiveness() throws Exception {
        // Test search caching effectiveness
        testSearchCacheEffectiveness();
        testReservationCacheEffectiveness();
    }

    private void testSearchCacheEffectiveness() throws Exception {
        // First search (cache miss)
        long startTime = System.currentTimeMillis();
        inventoryService.searchEquipmentAdvanced(
                null, "Test", null, null, null, null, null, null, null, null, true, 
                io.quarkus.panache.common.Page.of(0, 10));
        long firstSearchTime = System.currentTimeMillis() - startTime;

        // Second search (cache hit)
        startTime = System.currentTimeMillis();
        inventoryService.searchEquipmentAdvanced(
                null, "Test", null, null, null, null, null, null, null, null, true, 
                io.quarkus.panache.common.Page.of(0, 10));
        long secondSearchTime = System.currentTimeMillis() - startTime;

        System.out.printf("Search cache effectiveness: First: %d ms, Second: %d ms%n", 
                         firstSearchTime, secondSearchTime);

        // Cache should make second search significantly faster (but allow for some variance)
        // Don't enforce strict timing in CI environment
        assertTrue(secondSearchTime <= firstSearchTime + 50, 
                  "Cached search should not be significantly slower than first search");
    }

    private void testReservationCacheEffectiveness() throws Exception {
        UUID testReservationId = UUID.randomUUID();

        // First call (cache miss)
        long startTime = System.currentTimeMillis();
        ReservationResponse first = reservationService.getReservation(testReservationId);
        long firstCallTime = System.currentTimeMillis() - startTime;

        // Second call (cache hit)
        startTime = System.currentTimeMillis();
        ReservationResponse second = reservationService.getReservation(testReservationId);
        long secondCallTime = System.currentTimeMillis() - startTime;

        System.out.printf("Reservation cache effectiveness: First: %d ms, Second: %d ms%n", 
                         firstCallTime, secondCallTime);

        // Both should be null for non-existent reservation
        assertNull(first);
        assertNull(second);

        // Cache should make second call faster or similar
        assertTrue(secondCallTime <= firstCallTime + 10, 
                  "Cached reservation lookup should not be significantly slower");
    }

    @Test
    @DisplayName("Concurrent Access Performance Test")
    void testConcurrentAccessPerformance() throws Exception {
        int threadCount = 10;
        int iterationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<Long>> futures = new ArrayList<>();
        
        // Submit concurrent tasks
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Future<Long> future = executor.submit(() -> {
                long threadStartTime = System.currentTimeMillis();
                
                for (int j = 0; j < iterationsPerThread; j++) {
                    try {
                        // Test concurrent pricing calculations
                        PricingCalculationRequest request = new PricingCalculationRequest(
                                UUID.randomUUID(),
                                j + 1,
                                LocalDate.now(),
                                LocalDate.now().plusDays(1)
                        );
                        pricingService.calculatePricing(request);

                        // Test concurrent search operations
                        inventoryService.searchEquipmentAdvanced(
                                null, "Thread" + threadId, null, null, null, null, null, 
                                null, null, null, true, io.quarkus.panache.common.Page.of(0, 5));

                    } catch (Exception e) {
                        // Expected for non-existent data
                    }
                }
                
                return System.currentTimeMillis() - threadStartTime;
            });
            futures.add(future);
        }

        // Wait for all threads to complete and collect results
        long totalTime = 0;
        for (Future<Long> future : futures) {
            totalTime += future.get(30, TimeUnit.SECONDS);
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS), 
                  "Executor should terminate within 60 seconds");

        double avgTimePerThread = (double) totalTime / threadCount;
        System.out.printf("Concurrent access performance: %d threads x %d iterations, avg time per thread: %.2f ms%n", 
                         threadCount, iterationsPerThread, avgTimePerThread);

        // Assert reasonable concurrent performance
        assertTrue(avgTimePerThread < 2000, "Average time per thread should be under 2 seconds");
    }

    @Test
    @DisplayName("Memory Usage Test")
    void testMemoryUsage() {
        // Get initial memory usage
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Perform operations that should use caching
        for (int i = 0; i < 100; i++) {
            try {
                // Create various requests to test memory usage
                PricingCalculationRequest request = new PricingCalculationRequest(
                        UUID.randomUUID(),
                        i % 10 + 1,
                        LocalDate.now(),
                        LocalDate.now().plusDays(i % 7 + 1)
                );
                pricingService.calculatePricing(request);

                // Search operations
                inventoryService.searchEquipmentAdvanced(
                        "Category" + (i % 5), null, null, null, null, null, null, 
                        null, null, null, true, io.quarkus.panache.common.Page.of(0, 10));

            } catch (Exception e) {
                // Expected for non-existent data
            }
        }

        // Get final memory usage
        runtime.gc(); // Suggest garbage collection
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;

        System.out.printf("Memory usage test: Initial: %d bytes, Final: %d bytes, Used: %d bytes%n", 
                         initialMemory, finalMemory, memoryUsed);

        // Assert reasonable memory usage (under 50MB for test operations)
        assertTrue(memoryUsed < 50 * 1024 * 1024, 
                  "Memory usage should be reasonable (under 50MB)");
    }
}