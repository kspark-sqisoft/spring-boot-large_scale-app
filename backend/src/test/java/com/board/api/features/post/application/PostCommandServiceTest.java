package com.board.api.features.post.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.board.api.common.exception.ApiException;
import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.features.auth.domain.UserRole;
import com.board.api.features.file.domain.StoredFile;
import com.board.api.features.file.infrastructure.persistence.StoredFileRepository;
import com.board.api.features.post.domain.Post;
import com.board.api.features.post.domain.PostImage;
import com.board.api.features.post.infrastructure.persistence.PostImageRepository;
import com.board.api.features.post.infrastructure.persistence.PostRepository;

@ExtendWith(MockitoExtension.class)
class PostCommandServiceTest {

	private static final long OWNER = 42L;

	@Mock
	private PostRepository postRepository;
	@Mock
	private PostImageRepository postImageRepository;
	@Mock
	private StoredFileRepository storedFileRepository;
	@Mock
	private SnowflakeIdGenerator idGenerator;

	@InjectMocks
	private PostCommandService postCommandService;

	private Post existing;

	@BeforeEach
	void setUp() {
		existing = Post.create(200L, "old", "body");
	}

	@Test
	void create_assigns_snowflake_id_and_saves_without_images() {
		ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
		when(idGenerator.nextId()).thenReturn(55L);
		when(postRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

		Post created = postCommandService.create(OWNER, "제목", "본문", List.of());

		assertThat(created.getId()).isEqualTo(55L);
		assertThat(captor.getValue().getAuthorUserId()).isEqualTo(OWNER);
		verify(postRepository).save(any(Post.class));
		verify(postImageRepository, never()).save(any(PostImage.class));
	}

	@Test
	void create_with_images_validates_owner() {
		when(storedFileRepository.findByIdAndOwnerUserId(10L, OWNER)).thenReturn(Optional.of(storedFile(10L, OWNER)));
		when(idGenerator.nextId()).thenReturn(55L, 9001L);
		when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
		when(postImageRepository.save(any(PostImage.class))).thenAnswer(inv -> inv.getArgument(0));

		postCommandService.create(OWNER, "t", "c", List.of(10L));

		verify(storedFileRepository).findByIdAndOwnerUserId(10L, OWNER);
	}

	@Test
	void create_rejects_foreign_file() {
		when(storedFileRepository.findByIdAndOwnerUserId(10L, OWNER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> postCommandService.create(OWNER, "t", "c", List.of(10L)))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void update_with_null_images_does_not_touch_post_images() {
		when(postRepository.findById(200L)).thenReturn(Optional.of(existing));
		when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

		Post updated = postCommandService.update(200L, OWNER, UserRole.USER, "new title", "new body", null);

		assertThat(updated.getTitle()).isEqualTo("new title");
		verify(postImageRepository, never()).deleteByPostId(anyLong());
		verify(postImageRepository, never()).save(any(PostImage.class));
	}

	@Test
	void update_with_empty_image_list_clears() {
		when(postRepository.findById(200L)).thenReturn(Optional.of(existing));
		when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

		postCommandService.update(200L, OWNER, UserRole.USER, "a", "b", List.of());

		verify(postImageRepository).deleteByPostId(200L);
		verify(postImageRepository, never()).save(any(PostImage.class));
	}

	@Test
	void update_missing_post_throws_not_found() {
		when(postRepository.findById(999L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> postCommandService.update(999L, OWNER, UserRole.USER, "a", "b", null))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void update_forbidden_when_not_author() {
		Post otherAuthor = Post.create(200L, 999L, "x", "y");
		when(postRepository.findById(200L)).thenReturn(Optional.of(otherAuthor));
		assertThatThrownBy(() -> postCommandService.update(200L, OWNER, UserRole.USER, "a", "b", null))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void delete_missing_post_throws() {
		when(postRepository.findById(1L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> postCommandService.delete(1L, OWNER, UserRole.USER))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void delete_existing_removes() {
		when(postRepository.findById(200L)).thenReturn(Optional.of(existing));
		postCommandService.delete(200L, OWNER, UserRole.USER);
		verify(postRepository).deleteById(200L);
	}

	@Test
	void delete_forbidden_when_not_author() {
		Post otherAuthor = Post.create(200L, 999L, "x", "y");
		when(postRepository.findById(200L)).thenReturn(Optional.of(otherAuthor));
		assertThatThrownBy(() -> postCommandService.delete(200L, OWNER, UserRole.USER))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void admin_can_delete_others_post() {
		Post otherAuthor = Post.create(200L, 999L, "x", "y");
		when(postRepository.findById(200L)).thenReturn(Optional.of(otherAuthor));
		postCommandService.delete(200L, OWNER, UserRole.ADMIN);
		verify(postRepository).deleteById(200L);
	}

	private static StoredFile storedFile(long id, long owner) {
		return new StoredFile(id, owner, "image/png", "x.png", 1, "p", java.time.Instant.now());
	}
}
