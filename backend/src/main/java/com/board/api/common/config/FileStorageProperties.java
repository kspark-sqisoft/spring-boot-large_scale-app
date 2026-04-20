package com.board.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "app.upload")
@Getter
@Setter
public class FileStorageProperties {

	/**
	 * 이미지 등 바이너리 저장 루트(절대 또는 상대 경로).
	 */
	private String dir = "./data/uploads";
}
