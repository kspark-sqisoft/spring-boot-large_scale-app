package com.board.api.features.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 도메인 엔티티.
 * JPA가 이 클래스를 DB의 "users" 테이블과 매핑합니다.
 */
// @Entity: JPA 엔티티 선언 → 이 클래스의 인스턴스를 DB에 저장/조회 가능
// @Table: 매핑할 테이블명 지정
// @Getter: Lombok이 모든 필드의 getter 자동 생성
// @NoArgsConstructor(PROTECTED): JPA가 내부적으로 기본 생성자를 필요로 하지만 외부에서 직접 new User() 못 하게 막음
// @AllArgsConstructor: 모든 필드를 인자로 받는 생성자 생성 (create() 정적 팩토리에서 사용)
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

	// @Id: 기본키(PK) 컬럼. Snowflake ID를 애플리케이션에서 직접 생성해 넣음 (@GeneratedValue 미사용)
	@Id
	private Long id;

	// unique=true: DB에 중복 이메일 저장 불가
	@Column(nullable = false, unique = true, length = 255)
	private String email;

	// DB 컬럼명을 password_hash로 지정 (Java 관례: camelCase, DB 관례: snake_case)
	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash; // BCrypt 해시값. 평문 비밀번호는 절대 저장하지 않음

	// @Enumerated(STRING): enum을 숫자(0,1)가 아닌 문자열("USER","ADMIN")로 DB에 저장
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private UserRole role;

	// updatable=false: INSERT 시에만 값 설정, 이후 UPDATE에서 변경 불가
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt; // 계정 생성 시각

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt; // 마지막 수정 시각

	@Column(name = "display_name", length = 100)
	private String displayName; // 닉네임 (없으면 이메일 @ 앞부분을 사용)

	@Column(name = "avatar_file_id")
	private Long avatarFileId; // 프로필 이미지 파일 ID (StoredFile 참조)

	/**
	 * 새 사용자 생성 정적 팩토리.
	 * new User()를 직접 쓰는 대신 이 메서드를 사용해 의미를 명확히 하고,
	 * createdAt/updatedAt을 자동으로 현재 시각으로 설정합니다.
	 */
	public static User create(Long id, String email, String passwordHash, UserRole role) {
		Instant now = Instant.now();
		return new User(id, email, passwordHash, role, now, now, null, null);
	}

	// 비밀번호 변경 시 사용 (평문이 아닌 BCrypt 해시를 넣어야 함)
	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	// 수정 시각을 현재 시각으로 갱신 (save 전에 호출)
	public void touchUpdatedAt() {
		this.updatedAt = Instant.now();
	}

	// 닉네임 변경 (null 허용 → null이면 이메일 기반 기본 닉네임 사용)
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	// 프로필 이미지 변경 (null 허용 → null이면 기본 이미지)
	public void setAvatarFileId(Long avatarFileId) {
		this.avatarFileId = avatarFileId;
	}
}
