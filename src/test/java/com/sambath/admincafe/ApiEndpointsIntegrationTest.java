package com.sambath.admincafe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ApiEndpointsIntegrationTest {

    @Autowired MockMvc mvc;
    final ObjectMapper json = new ObjectMapper();

    @Test
    void fullFlow() throws Exception {
        // Unauthorized without token
        mvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());

        // Bad login
        mvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@brewmaster.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password."));

        // Good login
        MvcResult loginRes = mvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@brewmaster.com\",\"password\":\"brew1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value("u_1"))
                .andExpect(jsonPath("$.user.role").value("Owner"))
                .andReturn();
        String token = json.readTree(loginRes.getResponse().getContentAsString()).get("token").asText();
        String bearer = "Bearer " + token;
        assertThat(token).isNotBlank();

        // /me
        mvc.perform(get("/api/me").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("admin@brewmaster.com"));

        // Category create / update / dup conflict
        MvcResult catRes = mvc.perform(post("/api/categories").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Brew\",\"image\":\"https://x/i.jpg\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String catId = json.readTree(catRes.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(put("/api/categories/" + catId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Cold Brew\",\"image\":\"https://x/cb.jpg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cold Brew"));

        mvc.perform(post("/api/categories").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Espresso\",\"image\":null}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/categories/" + catId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Espresso\",\"image\":null}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Category name already exists."));

        // Place order
        MvcResult orderRes = mvc.perform(post("/api/orders").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "tableNumber": "Table 7",
                          "customerName": "Dara",
                          "isTakeout": false,
                          "items": [
                            {"productName":"Latte","quantity":1,"size":"L","notes":[],"priceOrder":4.50}
                          ]
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();
        String orderId = json.readTree(orderRes.getResponse().getContentAsString()).get("id").asText();

        // PATCH order — recompute totals
        mvc.perform(patch("/api/orders/" + orderId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "kitchenNote": "Sugar 50%",
                          "items": [
                            {"productName":"Latte","quantity":2,"size":"L","notes":["Sugar 50%"],"priceOrder":9.00}
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kitchenNote").value("Sugar 50%"))
                .andExpect(jsonPath("$.subtotal").value(9.00))
                .andExpect(jsonPath("$.total").value(9.00));

        // Mark Completed → creates a transaction
        MvcResult statusRes = mvc.perform(patch("/api/orders/" + orderId + "/status")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"Completed\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode statusJson = json.readTree(statusRes.getResponse().getContentAsString());
        String txId = statusJson.get("transaction").get("id").asText();
        assertThat(txId).startsWith("BW-");

        // PATCH on Completed order → 409
        mvc.perform(patch("/api/orders/" + orderId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kitchenNote\":\"too late\"}"))
                .andExpect(status().isConflict());

        // Refund the transaction
        mvc.perform(post("/api/transactions/" + txId + "/refund").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Wrong item served\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.status").value("REFUNDED"))
                .andExpect(jsonPath("$.transaction.amount").value(-9.00))
                .andExpect(jsonPath("$.order.id").value(orderId));

        // Second refund → 409
        mvc.perform(post("/api/transactions/" + txId + "/refund").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Transaction already refunded."));

        // DELETE — place another order then delete
        MvcResult o2 = mvc.perform(post("/api/orders").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "isTakeout": true,
                          "items": [{"productName":"Mocha","quantity":1,"notes":[],"priceOrder":5.00}]
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();
        String o2Id = json.readTree(o2.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(delete("/api/orders/" + o2Id).header("Authorization", bearer))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/orders/" + o2Id).header("Authorization", bearer))
                .andExpect(status().isNotFound());

        // Guest order — no token required
        MvcResult guestRes = mvc.perform(post("/api/guest/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "isTakeout": true,
                          "customerName": "Kiosk",
                          "items": [{"productName":"Drip","quantity":1,"notes":[],"priceOrder":3.00}]
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();
        String guestOrderId = json.readTree(guestRes.getResponse().getContentAsString()).get("id").asText();

        // Guest order must NOT appear in admin's main list
        MvcResult listRes = mvc.perform(get("/api/orders").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode ordersList = json.readTree(listRes.getResponse().getContentAsString());
        for (JsonNode n : ordersList) {
            assertThat(n.get("id").asText()).isNotEqualTo(guestOrderId);
        }

        // Admin CAN see guest orders via /api/orders/guest
        MvcResult guestListRes = mvc.perform(get("/api/orders/guest").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode guestList = json.readTree(guestListRes.getResponse().getContentAsString());
        boolean found = false;
        for (JsonNode n : guestList) {
            if (n.get("id").asText().equals(guestOrderId)) found = true;
        }
        assertThat(found).isTrue();

        // /api/orders/guest still requires a token
        mvc.perform(get("/api/orders/guest")).andExpect(status().isUnauthorized());

        // Logout — token revoked
        mvc.perform(post("/api/logout").header("Authorization", bearer))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/me").header("Authorization", bearer))
                .andExpect(status().isUnauthorized());

        // Guest endpoint still works after logout
        mvc.perform(post("/api/guest/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "isTakeout": true,
                          "items": [{"productName":"Tea","quantity":1,"notes":[],"priceOrder":2.00}]
                        }
                        """))
                .andExpect(status().isCreated());
    }
}
