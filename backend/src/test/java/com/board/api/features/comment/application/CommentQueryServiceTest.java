package com.board.api.features.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.board.api.common.exception.ApiException;
import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.domain.UserRole;
import com.board.api.features.auth.infrastructure.persistence.UserRepository;
import com.board.api.features.comment.api.dto.CommentListResponse;
import com.board.api.features.comment.domain.Comment;
import com.board.api.features.comment.infrastructure.persistence.CommentRepository;
import com.board.api.features.post.infrastructure.persistence.PostRepository;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceTest {

    private static final long POST_ID   = 100L;
    private static final long AUTHOR_ID = 42L;

    @Mock PostRepository    postRepository;
    @Mock CommentRepository commentRepository;
    @Mock UserRepository    userRepository;

    @InjectMocks CommentQueryService service;

    @Test
    void listForPost_fails_when_post_not_found() {
        when(postRepository.existsById(POST_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.listForPost(POST_ID))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void listForPost_returns_empty_list_when_no_comments() {
        when(postRepository.existsById(POST_ID)).thenReturn(true);
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(POST_ID)).thenReturn(List.of());

        CommentListResponse response = service.listForPost(POST_ID);

        assertThat(response.comments()).isEmpty();
    }

    @Test
    void listForPost_returns_comments_with_author_info() {
        Comment root  = Comment.createRoot(1L, POST_ID, AUTHOR_ID, "루트");
        Comment reply = Comment.createReply(2L, POST_ID, 1L, AUTHOR_ID, "답글");
        User author   = user(AUTHOR_ID, "작성자닉");

        when(postRepository.existsById(POST_ID)).thenReturn(true);
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(POST_ID)).thenReturn(List.of(root, reply));
        when(userRepository.findAllById(Set.of(AUTHOR_ID))).thenReturn(List.of(author));

        CommentListResponse response = service.listForPost(POST_ID);

        assertThat(response.comments()).hasSize(2);
        assertThat(response.comments().get(0).depth()).isEqualTo(0);
        assertThat(response.comments().get(1).depth()).isEqualTo(1);
        assertThat(response.comments().get(0).author().displayName()).isEqualTo("작성자닉");
    }

    @Test
    void listForPost_uses_email_prefix_when_display_name_is_null() {
        Comment root = Comment.createRoot(1L, POST_ID, AUTHOR_ID, "루트");
        User author  = user(AUTHOR_ID, null); // 닉네임 없음

        when(postRepository.existsById(POST_ID)).thenReturn(true);
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(POST_ID)).thenReturn(List.of(root));
        when(userRepository.findAllById(Set.of(AUTHOR_ID))).thenReturn(List.of(author));

        CommentListResponse response = service.listForPost(POST_ID);

        // 이메일 "user42@test.com" → 표시명 "user42"
        assertThat(response.comments().get(0).author().displayName()).isEqualTo("user" + AUTHOR_ID);
    }

    @Test
    void listForPost_uses_placeholder_when_author_row_missing() {
        Comment root = Comment.createRoot(1L, POST_ID, AUTHOR_ID, "루트");

        when(postRepository.existsById(POST_ID)).thenReturn(true);
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(POST_ID)).thenReturn(List.of(root));
        // 사용자 row가 DB에 없는 상황 (데이터 불일치)
        when(userRepository.findAllById(Set.of(AUTHOR_ID))).thenReturn(List.of());

        CommentListResponse response = service.listForPost(POST_ID);

        assertThat(response.comments().get(0).author().displayName()).isEqualTo("(알 수 없음)");
    }

    @Test
    void listForPost_batches_author_lookup_to_prevent_n_plus_1() {
        long authorB = 77L;
        Comment c1 = Comment.createRoot(1L, POST_ID, AUTHOR_ID, "A 댓글");
        Comment c2 = Comment.createRoot(2L, POST_ID, authorB,   "B 댓글");

        when(postRepository.existsById(POST_ID)).thenReturn(true);
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(POST_ID)).thenReturn(List.of(c1, c2));
        when(userRepository.findAllById(Set.of(AUTHOR_ID, authorB)))
                .thenReturn(List.of(user(AUTHOR_ID, "A닉"), user(authorB, "B닉")));

        CommentListResponse response = service.listForPost(POST_ID);

        assertThat(response.comments()).hasSize(2);
        // findAllById 1회 호출로 N+1 방지 검증
        verify(userRepository, times(1)).findAllById(Set.of(AUTHOR_ID, authorB));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static User user(long id, String displayName) {
        return new User(id, "user" + id + "@test.com", "hash", UserRole.USER,
                Instant.now(), Instant.now(), displayName, null);
    }
}
