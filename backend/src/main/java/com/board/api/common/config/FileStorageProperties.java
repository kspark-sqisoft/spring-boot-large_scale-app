package com.board.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * application.yml의 app.upload.* 설정을 바인딩하는 클래스.
 * 업로드된 파일이 실제로 저장될 디스크 경로를 관리합니다.
 */
@ConfigurationProperties(prefix = "app.upload")
@Getter
@Setter
public class FileStorageProperties {

	/**
	 * 파일 저장 루트 디렉토리 (절대 또는 상대 경로).
	 * Docker 환경: 컨테이너 볼륨에 마운트해 영속성 확보.
	 * 환경변수 APP_UPLOAD_DIR로 재설정 가능.
	 */
	private String dir = "./data/uploads";
}
