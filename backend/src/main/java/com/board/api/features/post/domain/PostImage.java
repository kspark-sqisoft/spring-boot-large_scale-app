package com.board.api.features.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "post_images")
public class PostImage {

	@Id
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "file_id", nullable = false)
	private Long fileId;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected PostImage() {
	}

	public PostImage(Long id, Long postId, Long fileId, int sortOrder) {
		this.id = id;
		this.postId = postId;
		this.fileId = fileId;
		this.sortOrder = sortOrder;
	}

	public Long getId() {
		return id;
	}

	public Long getPostId() {
		return postId;
	}

	public Long getFileId() {
		return fileId;
	}

	public int getSortOrder() {
		return sortOrder;
	}
}
