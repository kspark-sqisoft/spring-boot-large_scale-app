package com.board.api.features.file.api;

/** 정적 파일 제공 경로·업로드 엔드포인트 접두사 */
public final class FileApiPaths {

	// FileDownloadController — GET으로 이미지 스트리밍
	public static final String FILES = "/api/v1/files";
	// UploadController — POST multipart 업로드
	public static final String UPLOADS = "/api/v1/uploads";

	private FileApiPaths() {
	}
}
