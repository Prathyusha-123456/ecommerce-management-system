package com.ecommerce.config;

import com.ecommerce.entity.*;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** On first start: creates the admin account and a small demo catalogue so the app is usable immediately. */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository, CartRepository cartRepository,
                           CategoryRepository categoryRepository, ProductRepository productRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.name}") String adminName,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminName = adminName;
        this.adminEmail = adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedCatalogue();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        User admin = new User();
        admin.setName(adminName);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin = userRepository.save(admin);

        Cart cart = new Cart();
        cart.setUser(admin);
        cartRepository.save(cart);
        log.info("Created admin account: {}", adminEmail);
    }

    private void seedCatalogue() {
        if (categoryRepository.count() > 0) {
            return;
        }
        Category electronics = category("Electronics", "Gadgets and accessories");
        Category books = category("Books", "Programming and technology books");
        Category fashion = category("Fashion", "Clothing and footwear");
        Category home = category("Home & Kitchen", "Everyday home essentials");

        product("Wireless Bluetooth Earbuds", "Noise-isolating earbuds with 24-hour battery case", "1999.00", 50, electronics);
        product("Smartphone 128GB", "6.5-inch AMOLED display, 50MP camera", "18999.00", 25, electronics);
        product("Mechanical Keyboard", "Hot-swappable switches with RGB backlight", "3499.00", 40, electronics);
        product("27-inch Monitor", "QHD IPS panel, 75Hz refresh rate", "14999.00", 15, electronics);
        product("Clean Code", "A handbook of agile software craftsmanship", "899.00", 60, books);
        product("Effective Java", "Best practices for the Java platform", "1099.00", 45, books);
        product("Spring in Action", "Build applications with the Spring framework", "1299.00", 35, books);
        product("Cotton Casual Shirt", "Breathable slim-fit cotton shirt", "799.00", 80, fashion);
        product("Running Shoes", "Lightweight cushioned shoes for daily runs", "2499.00", 55, fashion);
        product("Denim Jacket", "Classic regular-fit denim jacket", "2199.00", 30, fashion);
        product("Non-stick Cookware Set", "3-piece induction-friendly set", "2799.00", 20, home);
        product("Electric Kettle 1.5L", "Auto shut-off stainless steel kettle", "1199.00", 70, home);
        product("Stainless Steel Bottle", "Insulated 750ml bottle, keeps drinks cold for 24h", "499.00", 100, home);
        log.info("Seeded demo categories and products");
    }

    private Category category(String name, String description) {
        Category c = new Category();
        c.setName(name);
        c.setDescription(description);
        return categoryRepository.save(c);
    }

    private void product(String name, String description, String price, int stock, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setPrice(new BigDecimal(price));
        p.setStockQuantity(stock);
        p.setCategory(category);
        productRepository.save(p);
    }
}
