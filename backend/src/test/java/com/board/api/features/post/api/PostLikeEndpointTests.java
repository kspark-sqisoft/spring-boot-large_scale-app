package com.board.api.features.post.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostLikeEndpointTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void like_unlike_and_counts_on_post() throws Exception {
		String access = register("like-" + System.nanoTime() + "@t.local", "password123");
		long postId = createPost(access, "좋아요 글", "본문");

		mockMvc.perform(get("/api/v1/posts/" + postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likeCount").value(0))
				.andExpect(jsonPath("$.commentCount").value(0))
				.andExpect(jsonPath("$.likedByMe").value(false))
				.andExpect(jsonPath("$.viewCount").value(0));

		mockMvc.perform(post("/api/v1/posts/" + postId + "/likes")
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likeCount").value(1))
				.andExpect(jsonPath("$.likedByMe").value(true));

		mockMvc.perform(get("/api/v1/posts/" + postId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likeCount").value(1))
				.andExpect(jsonPath("$.likedByMe").value(true))
				.andExpect(jsonPath("$.viewCount").value(0));

		mockMvc.perform(post("/api/v1/posts/" + postId + "/likes")
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likeCount").value(1));

		mockMvc.perform(delete("/api/v1/posts/" + postId + "/likes")
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likeCount").value(0))
				.andExpect(jsonPath("$.likedByMe").value(false));
	}

	private String register(String email, String password) throws Exception {
		MvcResult reg = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(reg.getResponse().getContentAsString(), "$.accessToken");
	}

	private long createPost(String access, String title, String content) throws Exception {
		MvcResult res = mockMvc.perform(post("/api/v1/posts")
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return Long.parseLong(JsonPath.read(res.getResponse().getContentAsString(), "$.id").toString());
	}
}
