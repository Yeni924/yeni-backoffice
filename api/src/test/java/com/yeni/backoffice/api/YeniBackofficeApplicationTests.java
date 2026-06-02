package com.yeni.backoffice.api;

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
		MvcResult loginResult = mockMvc.perform(post("/api/admin/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginId\":\"test\",\"password\":\"1234\"}"))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(get("/admin/navigation").session(session))
				.andExpect(status().isOk());
	}

}

