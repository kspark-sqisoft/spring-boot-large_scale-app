package com.board.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityBootstrapProperties {

	private boolean bootstrapAdmin = false;

	private String initialAdminEmail = "admin@board.local";

	private String initialAdminPassword = "ChangeMe_Admin_1";

	public boolean isBootstrapAdmin() {
		return bootstrapAdmin;
	}

	public void setBootstrapAdmin(boolean bootstrapAdmin) {
		this.bootstrapAdmin = bootstrapAdmin;
	}

	public String getInitialAdminEmail() {
		return initialAdminEmail;
	}

	public void setInitialAdminEmail(String initialAdminEmail) {
		this.initialAdminEmail = initialAdminEmail;
	}

	public String getInitialAdminPassword() {
		return initialAdminPassword;
	}

	public void setInitialAdminPassword(String initialAdminPassword) {
		this.initialAdminPassword = initialAdminPassword;
	}
}
