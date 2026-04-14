package com.board.api.features.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserEndpointTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@WithMockUser(roles = "ADMIN")
	void admin_can_list_users() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users?page=0&size=10"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "USER")
	void user_forbidden_on_admin() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users"))
				.andExpect(status().isForbidden());
	}
}
