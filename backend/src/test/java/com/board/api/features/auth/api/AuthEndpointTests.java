package com.board.api.features.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
class AuthEndpointTests {

	private static final Pattern REFRESH_COOKIE = Pattern.compile("board_rt=([^;]+)");

	@Autowired
	private MockMvc mockMvc;

	@Test
	void register_login_refresh_me_logout_flow() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user-auth-test@example.com","password":"password123"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.user.email").value("user-auth-test@example.com"))
				.andExpect(jsonPath("$.user.role").value("USER"));

		MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user-auth-test@example.com","password":"password123"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andReturn();

		String access = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
		String refreshRaw = extractRefreshCookie(login.getResponse().getHeader("Set-Cookie"));

		mockMvc.perform(get("/api/v1/users/me")
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("user-auth-test@example.com"))
				.andExpect(jsonPath("$.role").value("USER"));

		MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
						.cookie(new jakarta.servlet.http.Cookie(AuthCookie.REFRESH_COOKIE_NAME, refreshRaw)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andReturn();

		String newRefresh = extractRefreshCookie(refreshed.getResponse().getHeader("Set-Cookie"));
		mockMvc.perform(post("/api/v1/auth/logout")
						.cookie(new jakarta.servlet.http.Cookie(AuthCookie.REFRESH_COOKIE_NAME, newRefresh)))
				.andExpect(status().isNoContent());
	}

	@Test
	void register_duplicate_email_conflict() throws Exception {
		String body = """
				{"email":"dup@example.com","password":"password123"}
				""";
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
	}

	@Test
	void login_invalid_credentials() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"nobody@example.com","password":"wrongpass1"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	private static String extractRefreshCookie(String setCookieHeader) {
		if (setCookieHeader == null) {
			throw new AssertionError("Expected Set-Cookie header");
		}
		Matcher m = REFRESH_COOKIE.matcher(setCookieHeader);
		if (!m.find()) {
			throw new AssertionError("board_rt cookie not in: " + setCookieHeader);
		}
		return m.group(1);
	}
}
