package com.kafe.service;

import com.kafe.model.Cafe;
import com.kafe.model.SurplusDeal;
import com.kafe.model.User;
import com.kafe.repository.CafeRepository;
import com.kafe.repository.SurplusDealRepository;
import com.kafe.repository.UserRepository;
import com.kafe.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class KafeServiceConcurrencyTest {

    @Autowired
    private KafeService kafeService;

    @Autowired
    private CafeRepository cafeRepository;

    @Autowired
    private SurplusDealRepository surplusDealRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    public void testConcurrentReservations() throws InterruptedException, ExecutionException {
        // 1. Prepare database test data
        Cafe testCafe = new Cafe("Test Concurrency Cafe", 11.55, 104.92, "20:00", "http://example.com/image.png");
        testCafe = cafeRepository.save(testCafe);

        SurplusDeal testDeal = new SurplusDeal(testCafe, "Concurrent Cupcakes", 5.0, 1.0, 1, "18:00", "20:00", "http://example.com/deal.png");
        testDeal = surplusDealRepository.save(testDeal);

        final Long dealId = testDeal.getId();
        int threadCount = 10;

        // Clear any old reservations and users from previous aborted runs
        reservationRepository.deleteAll();
        for (int i = 0; i < threadCount; i++) {
            userRepository.findByPhoneNumber("concur_phone_" + i).ifPresent(userRepository::delete);
        }

        // Create 10 distinct users to avoid the "Max 2 active reservations" abuse rule
        List<User> testUsers = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            User u = new User("User " + i, "concur_phone_" + i, "password");
            testUsers.add(userRepository.save(u));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<Boolean>> futures = new ArrayList<>();

        // 2. Submit concurrent tasks using distinct users
        for (int i = 0; i < threadCount; i++) {
            final User user = testUsers.get(i);
            futures.add(executorService.submit(() -> {
                try {
                    startLatch.await(); // wait for general signal
                    kafeService.reserveDeal(dealId, user);
                    successCount.incrementAndGet();
                    return true;
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    return false;
                } finally {
                    endLatch.countDown();
                }
            }));
        }

        // 3. Start all threads simultaneously
        startLatch.countDown();
        endLatch.await(); // wait for all threads to finish

        // 4. Verify results
        assertEquals(1, successCount.get(), "Only exactly 1 reservation should succeed");
        assertEquals(threadCount - 1, failureCount.get(), "All other requests must fail due to zero quantity");

        // 5. Verify database state
        SurplusDeal finalDeal = surplusDealRepository.findById(dealId).orElseThrow();
        assertEquals(0, finalDeal.getQuantityAvailable(), "Quantity available must be exactly 0");

        // Clean up
        reservationRepository.deleteAll();
        surplusDealRepository.delete(finalDeal);
        cafeRepository.delete(testCafe);
        userRepository.deleteAll(testUsers);
        
        executorService.shutdown();
    }
}
