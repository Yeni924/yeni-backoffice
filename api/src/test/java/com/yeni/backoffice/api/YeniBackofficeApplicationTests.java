package com.yeni.backoffice.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:yeni-backoffice-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.h2.console.enabled=false"
})
@AutoConfigureMockMvc
class YeniBackofficeApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void contextLoads() {
	}

	@Test
	void dashboardPageLoads() throws Exception {
		mockMvc.perform(get("/dashboard"))
				.andExpect(status().isOk());
	}

	@Test
	void anonymousAdminNavigationRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/admin/navigation"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/login"));
	}

	@Test
	void adminLoginAndNavigationPageLoads() throws Exception {
		MockHttpSession session = loginAdmin();

		mockMvc.perform(get("/admin/navigation").session(session))
				.andExpect(status().isOk());
	}

	@Test
	void paymentBridgeApproveCancelAndQueryFlow() throws Exception {
		MvcResult approveResult = mockMvc.perform(post("/api/payment-bridge/payments/approve")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "pgProvider": "MOCK",
								  "orderNo": "ORDER-TEST-001",
								  "amount": 12000,
								  "currency": "KRW",
								  "buyerName": "Portfolio Buyer",
								  "productName": "Payment Bridge Mock Item",
								  "idempotencyKey": "APPROVE-ORDER-TEST-001",
								  "channelType": "WEB",
								  "storeCode": "PORTFOLIO",
								  "paymentMethod": "CARD"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode approve = objectMapper.readTree(approveResult.getResponse().getContentAsString());
		Long paymentId = approve.get("paymentId").asLong();

		mockMvc.perform(post("/api/payment-bridge/payments/{paymentId}/cancel", paymentId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "pgProvider": "MOCK",
								  "cancelAmount": 3000,
								  "cancelReason": "partial cancel mock",
								  "idempotencyKey": "CANCEL-TEST-001"
								}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/payment-bridge/payments/{paymentId}/retry-query", paymentId))
				.andExpect(status().isOk());
	}

	private MockHttpSession loginAdmin() throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/admin/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginId\":\"test\",\"password\":\"1234\"}"))
				.andExpect(status().isOk())
				.andReturn();

		return (MockHttpSession) loginResult.getRequest().getSession(false);
	}
}
