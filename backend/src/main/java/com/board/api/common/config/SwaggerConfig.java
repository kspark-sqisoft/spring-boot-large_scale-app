package com.board.api.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI 3) 문서 설정.
 * 브라우저에서 /swagger-ui.html 을 열면 API 목록과 직접 테스트 UI를 볼 수 있습니다.
 */
// @Configuration: 이 클래스 안의 @Bean들이 애플리케이션 컨텍스트에 등록됨
@Configuration
public class SwaggerConfig {

    // OpenAPI Bean 하나로 Swagger UI가 읽을 문서 메타데이터 전체를 구성
    @Bean
    public OpenAPI openAPI() {
        // Swagger UI에서 JWT를 입력해 인증 테스트할 때 쓸 보안 스킴 이름
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                // API 기본 정보 (Swagger UI 상단에 표시)
                .info(new Info()
                        .title("Board API")
                        .description("Spring Boot 대규모 게시판 API")
                        .version("v1"))

                // 모든 API에 bearerAuth 인증 요구 (자물쇠 아이콘 표시)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))

                // bearerAuth 스킴 정의: HTTP Authorization 헤더에 Bearer JWT를 넣는 방식
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)  // HTTP 인증 방식
                                .scheme("bearer")                // "Bearer" 접두사
                                .bearerFormat("JWT")));          // 토큰 형식 힌트
    }
}
