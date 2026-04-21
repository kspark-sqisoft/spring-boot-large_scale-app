package com.board.api.features.file.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.board.api.common.config.FileStorageProperties;
import com.board.api.common.exception.ApiException;
import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.features.file.domain.StoredFile;
import com.board.api.features.file.infrastructure.persistence.StoredFileRepository;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    /** Content-Type과 일치하는 최소 매직 바이트 (서비스의 시그니처 검증 통과용) */
    private static byte[] minimalJpegPayload(int size) {
        byte[] b = new byte[Math.max(size, 3)];
        b[0] = (byte) 0xFF;
        b[1] = (byte) 0xD8;
        b[2] = (byte) 0xFF;
        return b;
    }

    private static byte[] minimalPngPayload(int size) {
        byte[] sig = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
        byte[] b = new byte[Math.max(size, sig.length)];
        System.arraycopy(sig, 0, b, 0, sig.length);
        return b;
    }

    private static byte[] minimalGifPayload(int size) {
        byte[] b = new byte[Math.max(size, 4)];
        b[0] = 'G';
        b[1] = 'I';
        b[2] = 'F';
        b[3] = '8';
        return b;
    }

    private static byte[] minimalWebpPayload(int size) {
        int n = Math.max(size, 12);
        byte[] b = new byte[n];
        b[0] = 'R';
        b[1] = 'I';
        b[2] = 'F';
        b[3] = 'F';
        b[8] = 'W';
        b[9] = 'E';
        b[10] = 'B';
        b[11] = 'P';
        return b;
    }

    @TempDir Path tempDir;

    @Mock StoredFileRepository storedFileRepository;
    @Mock SnowflakeIdGenerator  idGenerator;

    FileStorageService service;

    @BeforeEach
    void setUp() throws Exception {
        FileStorageProperties props = new FileStorageProperties();
        props.setDir(tempDir.toString());
        service = new FileStorageService(props, storedFileRepository, idGenerator);
        service.ensureRootExists();
    }

    @Test
    void storeImage_saves_file_and_returns_metadata() {
        when(idGenerator.nextId()).thenReturn(1000L);
        when(storedFileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", minimalJpegPayload(100));

        StoredFile result = service.storeImage(42L, file);

        assertThat(result.getOwnerUserId()).isEqualTo(42L);
        assertThat(result.getContentType()).isEqualTo("image/jpeg");
        assertThat(result.getOriginalName()).isEqualTo("photo.jpg");
        assertThat(result.getSizeBytes()).isEqualTo(100L);
        verify(storedFileRepository).save(any(StoredFile.class));
    }

    @Test
    void storeImage_rejects_empty_file() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.storeImage(42L, empty))
                .isInstanceOf(ApiException.class);
        verify(storedFileRepository, never()).save(any());
    }

    @Test
    void storeImage_rejects_file_exceeding_5mb() {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", big);

        assertThatThrownBy(() -> service.storeImage(42L, file))
                .isInstanceOf(ApiException.class);
        verify(storedFileRepository, never()).save(any());
    }

    @Test
    void storeImage_rejects_disallowed_content_type() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.js", "application/javascript", new byte[100]);

        assertThatThrownBy(() -> service.storeImage(42L, file))
                .isInstanceOf(ApiException.class);
        verify(storedFileRepository, never()).save(any());
    }

    @Test
    void storeImage_accepts_all_allowed_image_types() {
        when(idGenerator.nextId()).thenReturn(1L, 2L, 3L, 4L);
        when(storedFileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        for (String type : new String[]{"image/jpeg", "image/png", "image/gif", "image/webp"}) {
            byte[] payload = switch (type) {
                case "image/jpeg" -> minimalJpegPayload(10);
                case "image/png" -> minimalPngPayload(10);
                case "image/gif" -> minimalGifPayload(10);
                case "image/webp" -> minimalWebpPayload(12);
                default -> throw new IllegalStateException(type);
            };
            MockMultipartFile file = new MockMultipartFile("file", "img", type, payload);
            StoredFile result = service.storeImage(42L, file);
            assertThat(result.getContentType()).isEqualTo(type);
        }
    }

    @Test
    void storeImage_sanitizes_filename_with_special_chars() {
        when(idGenerator.nextId()).thenReturn(2000L);
        when(storedFileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "my file (1).jpg", "image/png", minimalPngPayload(10));

        StoredFile result = service.storeImage(42L, file);

        // 공백과 괄호는 _ 로 치환되어야 함
        assertThat(result.getOriginalName()).doesNotContain(" ", "(", ")");
    }

    @Test
    void resolveAbsolutePath_returns_path_under_root() {
        when(idGenerator.nextId()).thenReturn(3000L);
        when(storedFileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", minimalPngPayload(10));
        StoredFile stored = service.storeImage(42L, file);

        Path resolved = service.resolveAbsolutePath(stored);

        assertThat(resolved).startsWith(tempDir);
    }
}
