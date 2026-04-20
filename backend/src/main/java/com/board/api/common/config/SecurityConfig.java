package com.board.api.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.board.api.common.security.JwtAuthenticationFilter;
import com.board.api.common.security.RestAccessDeniedHandler;
import com.board.api.common.security.RestAuthenticationEntryPoint;

/**
 * Spring Security 전체 설정.
 * JWT(stateless) + CORS + URL별·메서드별 접근 규칙을 정의합니다.
 * 메서드 단위 추가 검사는 {@code @PreAuthorize}로 컨트롤러에서 합니다.
 */
// @Configuration: Spring 설정 클래스 (내부 @Bean 메서드들을 Spring이 실행해 Bean 등록)
// @EnableWebSecurity: Spring Security 활성화
// @EnableMethodSecurity: @PreAuthorize 등 메서드 단위 보안 어노테이션 활성화
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;    // JWT 검증 필터
	private final RestAuthenticationEntryPoint authenticationEntryPoint; // 401 처리
	private final RestAccessDeniedHandler accessDeniedHandler;         // 403 처리
	private final CorsProperties corsProperties;                        // 허용 오리진 목록

	public SecurityConfig(
			JwtAuthenticationFilter jwtAuthenticationFilter,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler,
			CorsProperties corsProperties) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.authenticationEntryPoint = authenticationEntryPoint;
		this.accessDeniedHandler = accessDeniedHandler;
		this.corsProperties = corsProperties;
	}

	/**
	 * HTTP 보안 필터 체인 구성.
	 * 요청이 들어오면 이 체인을 순서대로 통과하며 인증·인가 처리가 됩니다.
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// REST API + JWT → 세션·CSRF 미사용
				// CSRF: 폼 기반 인증에서 위조 요청을 막는 장치인데 JWT는 쿠키 세션이 없어서 불필요
				.csrf(csrf -> csrf.disable())

				// CORS 설정 적용 (아래 corsConfigurationSource() Bean 참조)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				// 세션을 만들지 않음 (STATELESS) — 매 요청마다 JWT로만 인증
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// 인증/인가 실패 처리기 등록
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(authenticationEntryPoint) // 401: 인증 필요
						.accessDeniedHandler(accessDeniedHandler))           // 403: 권한 없음

				// URL별 접근 규칙 정의
				.authorizeHttpRequests(auth -> auth
						// Swagger UI — 인증 없이 접근 가능
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
						// 헬스 체크 — 공개
						.requestMatchers("/api/v1/health").permitAll()
						// 파일 다운로드(GET) — 공개 (이미지 직접 URL 접근)
						.requestMatchers(HttpMethod.GET, "/api/v1/files/**").permitAll()
						// 회원가입·로그인·토큰 갱신·로그아웃 — 공개
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
						// 게시글 목록·상세 조회(GET) — 공개
						.requestMatchers(HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/**").permitAll()
						// 관리자 전용 API — ROLE_ADMIN만 접근 가능
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
						// 나머지 모든 요청은 로그인 필수
						.anyRequest().authenticated())

				// JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 삽입
				// 즉, 요청이 오면 JWT를 먼저 검증하고 인증 정보를 SecurityContext에 넣음
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * CORS(Cross-Origin Resource Sharing) 설정.
	 * 브라우저가 다른 도메인(예: localhost:5173 → localhost:8080)의 API를 호출할 때 허용 여부 결정.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		// 허용할 오리진 목록 (application.yml의 app.cors.allowed-origins)
		configuration.setAllowedOrigins(corsProperties.originList());
		// 허용할 HTTP 메서드
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		// 허용할 요청 헤더 (* = 모두 허용)
		configuration.setAllowedHeaders(List.of("*"));
		// 쿠키(리프레시 토큰)를 포함한 요청 허용
		configuration.setAllowCredentials(true);
		// 브라우저가 Preflight 결과를 캐시하는 시간(초)
		configuration.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration); // 모든 경로에 적용
		return source;
	}

	/**
	 * 비밀번호 암호화에 BCrypt 알고리즘 사용.
	 * strength=12: 해싱 연산 횟수 (2^12번). 높을수록 안전하지만 느려짐.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	/**
	 * 로그인 시 AuthenticationManager가 이메일·비밀번호를 검증.
	 * AuthService에서 authenticationManager.authenticate()를 호출해 사용.
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
