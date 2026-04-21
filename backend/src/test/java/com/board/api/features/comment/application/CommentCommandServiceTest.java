package com.board.api.features.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.board.api.common.exception.ApiException;
import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.domain.UserRole;
import com.board.api.features.auth.infrastructure.persistence.UserRepository;
import com.board.api.features.comment.api.dto.CommentResponse;
import com.board.api.features.comment.api.dto.CreateCommentRequest;
import com.board.api.features.comment.api.dto.UpdateCommentRequest;
import com.board.api.features.comment.domain.Comment;
import com.board.api.features.comment.infrastructure.persistence.CommentRepository;
import com.board.api.features.post.infrastructure.persistence.PostRepository;

@ExtendWith(MockitoExtension.class)
class CommentCommandServiceTest {

    private static final long POST_ID   = 100L;
    private static final long AUTHOR_ID = 42L;
    private static final long OTHER_ID  = 99L;

    @Mock PostRepository      postRepository;
    @Mock CommentRepository   commentRepository;
    @Mock UserRepository      userRepository;
    @Mock SnowflakeIdGenerator idGenerator;

    @InjectMocks CommentCommandService service;

    // ── create ─────────────────────────────────────────────────────────────

    @Test
    void create_root_comment_saves_with_null_parent() {
        when(postRepository.existsById(POST_ID)).thenReturn(true);
        when(idGenerator.nextId()).thenReturn(1L);
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        when(commentRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID)));

        CommentResponse response = service.create(POST_ID, AUTHOR_ID, new CreateCommentRequest("내용", null));

        assertThat(response.depth()).isEqualTo(0);
        assertThat(response.parentCommentId()).isNull();
        assertThat(captor.getValue().getParentId()).isNull();
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void create_reply_sets_parent_and_depth_1() {
        Comment root = Comment.createRoot(10L, POST_ID, AUTHOR_ID, "루트");
        when(postRepository.existsById(POST_ID)).thenReturn(true);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(root));
        when(idGenerator.nextId()).thenReturn(20L);
        when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID)));

        CommentResponse response = service.create(POST_ID, AUTHOR_ID, new CreateCommentRequest("답글", "10"));

        assertThat(response.depth()).isEqualTo(1);
        assertThat(response.parentCommentId()).isEqualTo("10");
    }

    @Test
    void create_fails_when_post_not_found() {
        when(postRepository.existsById(POST_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(POST_ID, AUTHOR_ID, new CreateCommentRequest("내용", null)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void create_fails_when_parent_not_found() {
        when(postRepository.existsById(POST_ID)).thenReturn(true);
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(POST_ID, AUTHOR_ID, new CreateCommentRequest("내용", "999")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void create_fails_when_parent_belongs_to_different_post() {
        Comment foreign = Comment.createRoot(10L, 999L, AUTHOR_ID, "다른 게시글");
        when(postRepository.existsById(POST_ID)).thenReturn(true);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.create(POST_ID, AUTHOR_ID, new CreateCommentRequest("내용", "10")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void create_fails_when_nesting_depth_exceeds_max() {
        // 대댓글(depth 1)에 다시 답글을 달려는 시도
        Comment reply = Comment.createReply(20L, POST_ID, 10L, AUTHOR_ID, "대댓글");
        when(postRepository.existsById(POST_ID)).thenReturn(true);
        when(commentRepository.findById(20L)).thenReturn(Optional.of(reply));

        assertThatThrownBy(() -> service.create(POST_ID, AUTHOR_ID, new CreateCommentRequest("금지", "20")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void create_fails_when_parent_id_is_invalid_format() {
        when(postRepository.existsById(POST_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(POST_ID, AUTHOR_ID, new CreateCommentRequest("내용", "abc")))
                .isInstanceOf(ApiException.class);
    }

    // ── update ─────────────────────────────────────────────────────────────

    @Test
    void update_changes_content_for_owner() {
        Comment comment = Comment.createRoot(1L, POST_ID, AUTHOR_ID, "원본");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID)));

        CommentResponse response = service.update(POST_ID, 1L, AUTHOR_ID, new UpdateCommentRequest("수정됨"));

        assertThat(response.content()).isEqualTo("수정됨");
    }

    @Test
    void update_fails_when_comment_not_found() {
        when(commentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(POST_ID, 1L, AUTHOR_ID, new UpdateCommentRequest("내용")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void update_fails_when_comment_belongs_to_different_post() {
        Comment comment = Comment.createRoot(1L, 999L, AUTHOR_ID, "다른 게시글");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.update(POST_ID, 1L, AUTHOR_ID, new UpdateCommentRequest("내용")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void update_fails_when_not_author() {
        Comment comment = Comment.createRoot(1L, POST_ID, OTHER_ID, "타인 댓글");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.update(POST_ID, 1L, AUTHOR_ID, new UpdateCommentRequest("내용")))
                .isInstanceOf(ApiException.class);
    }

    // ── delete ─────────────────────────────────────────────────────────────

    @Test
    void delete_removes_comment_for_owner() {
        Comment comment = Comment.createRoot(1L, POST_ID, AUTHOR_ID, "내용");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        service.delete(POST_ID, 1L, AUTHOR_ID);

        verify(commentRepository).delete(comment);
    }

    @Test
    void delete_fails_when_comment_not_found() {
        when(commentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(POST_ID, 1L, AUTHOR_ID))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void delete_fails_when_not_author() {
        Comment comment = Comment.createRoot(1L, POST_ID, OTHER_ID, "타인 댓글");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.delete(POST_ID, 1L, AUTHOR_ID))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void delete_fails_when_comment_belongs_to_different_post() {
        Comment comment = Comment.createRoot(1L, 999L, AUTHOR_ID, "다른 게시글");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.delete(POST_ID, 1L, AUTHOR_ID))
                .isInstanceOf(ApiException.class);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static User user(long id) {
        return new User(id, "user" + id + "@test.com", "hash", UserRole.USER,
                Instant.now(), Instant.now(), null, null);
    }
}
