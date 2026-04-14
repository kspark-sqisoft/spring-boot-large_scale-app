package com.board.api.features.post.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 JWT 발급 후 게시글 쓰기 API를 검증합니다({@code @WithMockUser} 없이).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostAuthenticatedWriteIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registered_user_bearer_token_can_create_post() throws Exception {
		String email = "writer-int-" + System.nanoTime() + "@example.com";
		MvcResult reg = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"password123"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").exists())
				.andReturn();

		String access = JsonPath.read(reg.getResponse().getContentAsString(), "$.accessToken");

		mockMvc.perform(post("/api/v1/posts")
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"통합 테스트 글","content":"JWT로 작성"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("통합 테스트 글"));
	}
}
