package com.kafe.config;

import com.kafe.model.Cafe;
import com.kafe.model.SurplusDeal;
import com.kafe.model.User;
import com.kafe.repository.CafeRepository;
import com.kafe.repository.SurplusDealRepository;
import com.kafe.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final CafeRepository cafeRepository;
    private final SurplusDealRepository surplusDealRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(CafeRepository cafeRepository, SurplusDealRepository surplusDealRepository, 
                          UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.cafeRepository = cafeRepository;
        this.surplusDealRepository = surplusDealRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (cafeRepository.count() > 0) {
            System.out.println("Database already contains seeded data. Skipping...");
            return;
        }

        System.out.println("Seeding database with Phnom Penh Cafes and Surplus Deals...");

        // Seed default Customer and Merchant Users
        if (userRepository.count() == 0) {
            User defaultCustomer = new User(
                "Test Customer",
                "012345678",
                passwordEncoder.encode("password"),
                "CUSTOMER"
            );
            User defaultMerchant = new User(
                "Shop Merchant",
                "099888777",
                passwordEncoder.encode("password"),
                "MERCHANT"
            );
            userRepository.saveAll(Arrays.asList(defaultCustomer, defaultMerchant));
        }

        // Seed Cafes
        Cafe c1 = new Cafe("Brown Coffee Roastery (BKK1)", 11.5512, 104.9215, "21:00", 
            "https://images.unsplash.com/photo-1554118811-1e0d58224f24?q=80&w=400");
        Cafe c2 = new Cafe("Tube Coffee (Boeung Keng Kang)", 11.5535, 104.9240, "20:30", 
            "https://images.unsplash.com/photo-1507133750040-4a8f57021571?q=80&w=400");
        Cafe c3 = new Cafe("Temple Coffee & Bakery (TTP)", 11.5385, 104.9145, "22:00", 
            "https://images.unsplash.com/photo-1509440159596-0249088772ff?q=80&w=400");
        Cafe c4 = new Cafe("Slay Coffee (Street 271)", 11.5420, 104.9090, "20:00", 
            "https://images.unsplash.com/photo-1517701604599-bb29b565090c?q=80&w=400");
        Cafe c5 = new Cafe("Boss Coffee & Catering (TTP)", 11.5395, 104.9160, "21:30", 
            "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?q=80&w=400");
        Cafe c6 = new Cafe("CoffeePOP 24H (BKK1)", 11.5495, 104.9195, "23:59", 
            "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?q=80&w=400");

        cafeRepository.saveAll(Arrays.asList(c1, c2, c3, c4, c5, c6));

        // Seed Surplus Deals
        Random r = new Random();
        List<Cafe> cafes = cafeRepository.findAll();

        String[] titles = {
            "Mystery Pastry Bag",
            "Assorted Donut Box",
            "EOD Baker's Basket",
            "Surplus Croissant Bundle",
            "Gourmet Bread Pack",
            "Muffin & Scone Trio",
            "Late Night Sweet Treat",
            "Artisan Coffee & Danish Combo"
        };

        String[] dealImages = {
            "https://images.unsplash.com/photo-1555507036-ab1f4038808a?q=80&w=400", // Pastries
            "https://images.unsplash.com/photo-1551024601-bec78aea704b?q=80&w=400", // Donuts
            "https://images.unsplash.com/photo-1509440159596-0249088772ff?q=80&w=400", // Bread basket
            "https://images.unsplash.com/photo-1549931319-a545dcf3bc73?q=80&w=400", // Croissants
            "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?q=80&w=400", // Bakeries
            "https://images.unsplash.com/photo-1607958996333-41aef7caefaa?q=80&w=400"  // Cookies
        };

        for (int i = 0; i < 15; i++) {
            Cafe cafe = cafes.get(r.nextInt(cafes.size()));
            String title = titles[r.nextInt(titles.length)];
            double original = 4.00 + (r.nextDouble() * 8.00); // $4.00 to $12.00
            double discount = Math.round((original * 0.3) * 100.0) / 100.0; // 70% off
            int qty = 1 + r.nextInt(5); // 1 to 5 available
            String dealImage = dealImages[r.nextInt(dealImages.length)];
            
            // Format original and discount prices nicely
            original = Math.round(original * 100.0) / 100.0;

            String pickupStart = "19:30";
            String pickupEnd = "21:00";
            if (cafe.getClosingTime().equals("23:59")) {
                pickupStart = "22:00";
                pickupEnd = "23:30";
            } else if (cafe.getClosingTime().compareTo("21:00") <= 0) {
                pickupStart = "18:30";
                pickupEnd = "20:00";
            }

            SurplusDeal deal = new SurplusDeal(cafe, title, original, discount, qty, pickupStart, pickupEnd, dealImage);
            surplusDealRepository.save(deal);
        }

        System.out.println("Successfully seeded database.");
    }
}
