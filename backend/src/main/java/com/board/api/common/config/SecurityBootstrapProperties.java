package com.board.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityBootstrapProperties {

	private boolean bootstrapAdmin = false;

	private String initialAdminEmail = "admin@board.local";

	private String initialAdminPassword = "ChangeMe_Admin_1";
}
