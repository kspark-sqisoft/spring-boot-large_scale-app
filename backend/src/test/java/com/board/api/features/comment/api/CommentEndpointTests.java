package com.board.api.features.comment.api;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentEndpointTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void list_requires_existing_post() throws Exception {
		mockMvc.perform(get("/api/v1/posts/9000000000000000000/comments"))
				.andExpect(status().isNotFound());
	}

	@Test
	void comment_crud_and_depth_and_permissions() throws Exception {
		String accessA = register("ca-" + System.nanoTime() + "@t.local", "password123");
		long postId = createPost(accessA, "댓글 테스트 글", "본문");

		mockMvc.perform(get("/api/v1/posts/" + postId + "/comments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.comments").isArray())
				.andExpect(jsonPath("$.comments.length()").value(0));

		mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"루트\"}"))
				.andExpect(status().isUnauthorized());

		MvcResult rootRes = mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
						.header("Authorization", "Bearer " + accessA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"루트\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.depth").value(0))
				.andExpect(jsonPath("$.parentCommentId").doesNotExist())
				.andReturn();
		String rootId = JsonPath.read(rootRes.getResponse().getContentAsString(), "$.id");

		String accessB = register("cb-" + System.nanoTime() + "@t.local", "password123");
		MvcResult replyRes = mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
						.header("Authorization", "Bearer " + accessB)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"답글\",\"parentCommentId\":\"" + rootId + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.depth").value(1))
				.andExpect(jsonPath("$.parentCommentId").value(rootId))
				.andReturn();
		String replyId = JsonPath.read(replyRes.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
						.header("Authorization", "Bearer " + accessA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"금지\",\"parentCommentId\":\"" + replyId + "\"}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/posts/" + postId + "/comments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.comments.length()").value(2));

		mockMvc.perform(put("/api/v1/posts/" + postId + "/comments/" + rootId)
						.header("Authorization", "Bearer " + accessB)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"남의 글\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/v1/posts/" + postId + "/comments/" + rootId)
						.header("Authorization", "Bearer " + accessA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"수정됨\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("수정됨"));

		mockMvc.perform(delete("/api/v1/posts/" + postId + "/comments/" + rootId)
						.header("Authorization", "Bearer " + accessB))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/api/v1/posts/" + postId + "/comments/" + rootId)
						.header("Authorization", "Bearer " + accessA))
				.andExpect(status().isNoContent());
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
