package com.board.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * application.yml의 app.security.* 설정을 바인딩하는 클래스.
 * 개발 환경에서 최초 관리자 계정을 자동으로 생성하는 기능을 제어합니다.
 */
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityBootstrapProperties {

	// true면 앱 기동 시 initialAdminEmail 계정이 없으면 자동 생성 (개발 편의용)
	// 환경변수 APP_SECURITY_BOOTSTRAP_ADMIN=true 로 활성화
	private boolean bootstrapAdmin = false;

	// 자동 생성할 관리자 이메일 (기본: admin@board.local)
	private String initialAdminEmail = "admin@board.local";

	// 자동 생성할 관리자 초기 비밀번호 (반드시 변경할 것!)
	private String initialAdminPassword = "ChangeMe_Admin_1";
}
