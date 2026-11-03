package com.ecommerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** End-to-end test of the main user journey, running against an in-memory H2 database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class EcommerceFlowTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    @Test
    void customerCanBrowseAddToCartCheckoutAndCancel() throws Exception {
        // --- register + login ---
        String email = "buyer-" + UUID.randomUUID() + "@test.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test Buyer", "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("USER"));
        String userToken = login(email, PASSWORD);
        String adminToken = login("admin@ecommerce.com", "Admin@123");

        // --- admin creates a category and a product ---
        JsonNode category = body(mvc.perform(post("/api/admin/categories").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Cat-" + UUID.randomUUID(), "description", "test"))))
                .andExpect(status().isCreated()));
        JsonNode product = body(mvc.perform(post("/api/admin/products").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Test Gadget", "description", "demo", "price", 250.50,
                        "stockQuantity", 10, "categoryId", category.get("id").asLong()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").isNotEmpty()));
        long productId = product.get("id").asLong();

        // --- a normal user must NOT be able to create products ---
        mvc.perform(post("/api/admin/products").header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Hack", "price", 1, "stockQuantity", 1, "categoryId", 1))))
                .andExpect(status().isForbidden());

        // --- public search finds it (no token needed) ---
        mvc.perform(get("/api/products").param("keyword", "test gadget").param("sortBy", "price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Test Gadget"));

        // --- cart: add twice (quantity merges), over-ordering is rejected ---
        addToCart(userToken, productId, 2).andExpect(status().isOk());
        addToCart(userToken, productId, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalAmount").value(751.50));
        addToCart(userToken, productId, 50).andExpect(status().isBadRequest());

        // --- checkout with UPI: order is PAID, stock drops from 10 to 7, cart empties ---
        JsonNode order = body(mvc.perform(post("/api/orders").header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("shippingAddress", "12 MG Road, Bengaluru", "paymentMethod", "UPI"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payment.status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalAmount").value(751.50)));
        long orderId = order.get("id").asLong();

        mvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.stockQuantity").value(7));
        mvc.perform(get("/api/cart").header("Authorization", bearer(userToken)))
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/orders").header("Authorization", bearer(userToken)))
                .andExpect(jsonPath("$.totalElements").value(1));

        // --- another customer cannot see this order ---
        String otherEmail = "other-" + UUID.randomUUID() + "@test.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Other", "email", otherEmail, "password", PASSWORD))));
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(login(otherEmail, PASSWORD))))
                .andExpect(status().isNotFound());

        // --- cancel: stock is restored and payment refunded ---
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.payment.status").value("REFUNDED"));
        mvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.stockQuantity").value(10));
    }

    @Test
    void failedPaymentRollsBackStockAndKeepsCart() throws Exception {
        String adminToken = login("admin@ecommerce.com", "Admin@123");
        String email = "rich-" + UUID.randomUUID() + "@test.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Rich Buyer", "email", email, "password", PASSWORD))));
        String token = login(email, PASSWORD);

        JsonNode category = body(mvc.perform(post("/api/admin/categories").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Lux-" + UUID.randomUUID())))));
        JsonNode product = body(mvc.perform(post("/api/admin/products").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Diamond Watch", "price", 2000000, "stockQuantity", 5,
                        "categoryId", category.get("id").asLong())))));
        long productId = product.get("id").asLong();

        addToCart(token, productId, 1).andExpect(status().isOk());

        // price is above the mock gateway's test limit => payment declined => HTTP 402
        mvc.perform(post("/api/orders").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("shippingAddress", "Somewhere", "paymentMethod", "CARD"))))
                .andExpect(status().isPaymentRequired());

        // nothing was half-saved: stock unchanged, cart still has the item, no order exists
        mvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.stockQuantity").value(5));
        mvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get("/api/orders").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cart").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidInputReturnsFieldLevelErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "A", "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nobody@test.com", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@test.com";
        String payload = json(Map.of("name", "Dup User", "email", email, "password", PASSWORD));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void passwordsAreStoredHashed(@Autowired com.ecommerce.repository.UserRepository users) throws Exception {
        String email = "hash-" + UUID.randomUUID() + "@test.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Hash User", "email", email, "password", PASSWORD))));
        String stored = users.findByEmail(email).orElseThrow().getPassword();
        assertEquals(true, stored.startsWith("$2"), "password must be a BCrypt hash");
        assertEquals(false, stored.contains(PASSWORD));
    }

    // ---------- helpers ----------

    private String login(String email, String password) throws Exception {
        JsonNode node = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()));
        return node.get("token").asText();
    }

    private ResultActions addToCart(String token, long productId, int quantity) throws Exception {
        return mvc.perform(post("/api/cart/items").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("productId", productId, "quantity", quantity))));
    }

    private JsonNode body(ResultActions actions) throws Exception {
        return mapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
