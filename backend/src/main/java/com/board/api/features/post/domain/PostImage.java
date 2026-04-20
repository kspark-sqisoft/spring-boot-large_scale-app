package com.board.api.features.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PostImage {

	@Id
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "file_id", nullable = false)
	private Long fileId;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;
}
