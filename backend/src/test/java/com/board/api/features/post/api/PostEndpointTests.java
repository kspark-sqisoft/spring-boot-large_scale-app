package com.board.api.features.post.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.security.AppUserDetails;
import com.board.api.features.auth.domain.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostEndpointTests {

	private static AppUserDetails writerPrincipal() {
		return new AppUserDetails(9_999_001L, "mvc-writer@test.local", "hash", UserRole.USER);
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void crud_flow() throws Exception {
		mockMvc.perform(get("/api/v1/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));

		MvcResult created = mockMvc.perform(post("/api/v1/posts")
						.with(user(writerPrincipal()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"첫 글","content":"본문입니다."}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("첫 글"))
				.andExpect(jsonPath("$.id").exists())
				.andReturn();

		String json = created.getResponse().getContentAsString();
		long id = Long.parseLong(JsonPath.read(json, "$.id").toString());

		mockMvc.perform(get("/api/v1/posts/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("본문입니다."));

		mockMvc.perform(put("/api/v1/posts/" + id)
						.with(user(writerPrincipal()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"수정 제목","content":"수정 본문"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("수정 제목"));

		mockMvc.perform(delete("/api/v1/posts/" + id).with(user(writerPrincipal())))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/posts/" + id))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithAnonymousUser
	void create_requires_authentication() throws Exception {
		mockMvc.perform(post("/api/v1/posts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"x\",\"content\":\"y\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void list_after_create_has_one_element() throws Exception {
		mockMvc.perform(post("/api/v1/posts")
				.with(user(writerPrincipal()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"목록용\",\"content\":\"c\"}"));

		mockMvc.perform(get("/api/v1/posts?page=0&size=10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].title").value("목록용"));
	}
}
