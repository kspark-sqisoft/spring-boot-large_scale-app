package com.board.api.features.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.board.api.common.exception.ApiException;
import com.board.api.features.comment.infrastructure.persistence.CommentRepository;
import com.board.api.features.post.domain.Post;
import com.board.api.features.post.infrastructure.persistence.PostImageRepository;
import com.board.api.features.post.infrastructure.persistence.PostLikeRepository;
import com.board.api.features.post.infrastructure.persistence.PostRepository;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

	@Mock
	private PostRepository postRepository;
	@Mock
	private PostImageRepository postImageRepository;
	@Mock
	private CommentRepository commentRepository;
	@Mock
	private PostLikeRepository postLikeRepository;
	@Mock
	private PostViewService postViewService;

	@InjectMocks
	private PostQueryService postQueryService;

	@Test
	void getById_returns_post() {
		Post post = Post.create(1L, "t", "c");
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		assertThat(postQueryService.getById(1L)).isSameAs(post);
	}

	@Test
	void getById_missing_throws() {
		when(postRepository.findById(2L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> postQueryService.getById(2L))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void list_clamps_page_size_to_max_100() {
		Pageable expected = PageRequest.of(0, 100);
		when(postRepository.findAllByOrderByCreatedAtDesc(expected))
				.thenReturn(new PageImpl<>(List.of()));

		postQueryService.list(0, 500);

		verify(postRepository).findAllByOrderByCreatedAtDesc(expected);
	}

	@Test
	void list_clamps_negative_size_to_1() {
		Pageable expected = PageRequest.of(0, 1);
		when(postRepository.findAllByOrderByCreatedAtDesc(expected))
				.thenReturn(new PageImpl<>(List.of()));

		postQueryService.list(0, -5);

		verify(postRepository).findAllByOrderByCreatedAtDesc(expected);
	}

	@Test
	void list_clamps_negative_page_to_zero() {
		Pageable expected = PageRequest.of(0, 20);
		when(postRepository.findAllByOrderByCreatedAtDesc(expected))
				.thenReturn(new PageImpl<>(List.of()));

		postQueryService.list(-3, 20);

		verify(postRepository).findAllByOrderByCreatedAtDesc(expected);
	}
}
