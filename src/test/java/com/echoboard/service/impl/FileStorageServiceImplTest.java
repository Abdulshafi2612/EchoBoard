package com.echoboard.service.impl;

import com.echoboard.enums.FileType;
import com.echoboard.exception.AppException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static com.echoboard.exception.ErrorCode.VALIDATION_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceImplTest {

    private final FileStorageServiceImpl fileStorageService = new FileStorageServiceImpl();

    @AfterEach
    void tearDown() throws IOException {
        Path uploadsPath = Path.of("uploads");

        if (!Files.exists(uploadsPath)) {
            return;
        }

        try (var paths = Files.walk(uploadsPath)) {
            paths
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    });
        }
    }

    @Test
    void storeImageFile_whenPngImageIsValid_shouldStoreFileAndReturnNormalizedPath() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "fake-image-content".getBytes()
        );

        String storedPath = fileStorageService.storeImageFile(file, "sessions/1/logo");

        assertNotNull(storedPath);
        assertTrue(storedPath.startsWith("uploads/sessions/1/logo/"));
        assertTrue(storedPath.endsWith(".png"));
        assertTrue(Files.exists(Path.of(storedPath)));
    }

    @Test
    void storeImageFile_whenJpgImageIsValid_shouldStoreFileAndReturnNormalizedPath() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "fake-image-content".getBytes()
        );

        String storedPath = fileStorageService.storeImageFile(file, "sessions/1/logo");

        assertNotNull(storedPath);
        assertTrue(storedPath.startsWith("uploads/sessions/1/logo/"));
        assertTrue(storedPath.endsWith(".jpg"));
        assertTrue(Files.exists(Path.of(storedPath)));
    }

    @Test
    void storeAttachmentFile_whenPdfAttachmentIsValid_shouldStoreFileAndReturnNormalizedPath() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "question.pdf",
                "application/pdf",
                "fake-pdf-content".getBytes()
        );

        String storedPath = fileStorageService.storeAttachmentFile(file, "sessions/1/questions/10");

        assertNotNull(storedPath);
        assertTrue(storedPath.startsWith("uploads/sessions/1/questions/10/"));
        assertTrue(storedPath.endsWith(".pdf"));
        assertTrue(Files.exists(Path.of(storedPath)));
    }

    @Test
    void storeAttachmentFile_whenDocxAttachmentIsValid_shouldStoreFileAndReturnNormalizedPath() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "question.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "fake-docx-content".getBytes()
        );

        String storedPath = fileStorageService.storeAttachmentFile(file, "sessions/1/questions/10");

        assertNotNull(storedPath);
        assertTrue(storedPath.startsWith("uploads/sessions/1/questions/10/"));
        assertTrue(storedPath.endsWith(".docx"));
        assertTrue(Files.exists(Path.of(storedPath)));
    }

    @Test
    void storeImageFile_whenFileIsNull_shouldThrowValidationException() {
        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageService.storeImageFile(null, "sessions/1/logo")
        );

        assertEquals(VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("File is required", exception.getMessage());
    }

    @Test
    void storeImageFile_whenFileIsEmpty_shouldThrowValidationException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[0]
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageService.storeImageFile(file, "sessions/1/logo")
        );

        assertEquals(VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("File is required", exception.getMessage());
    }

    @Test
    void storeImageFile_whenFileIsTooLarge_shouldThrowValidationException() {
        byte[] largeContent = new byte[(2 * 1024 * 1024) + 1];

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                largeContent
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageService.storeImageFile(file, "sessions/1/logo")
        );

        assertEquals(VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("File is too large", exception.getMessage());
    }

    @Test
    void storeImageFile_whenFileNameHasNoExtension_shouldThrowValidationException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar",
                "image/png",
                "fake-image-content".getBytes()
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageService.storeImageFile(file, "sessions/1/logo")
        );

        assertEquals(VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("File extension is required", exception.getMessage());
    }

    @Test
    void storeImageFile_whenFileNameIsNull_shouldThrowValidationException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                null,
                "image/png",
                "fake-image-content".getBytes()
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageService.storeImageFile(file, "sessions/1/logo")
        );

        assertEquals(VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("File extension is required", exception.getMessage());
    }

    @Test
    void storeImageFile_whenExtensionIsUnsupportedForImages_shouldThrowValidationException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "fake-pdf-content".getBytes()
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageService.storeImageFile(file, "sessions/1/logo")
        );

        assertEquals(VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Unsupported file type", exception.getMessage());
    }

    @Test
    void storeAttachmentFile_whenExtensionIsUnsupportedForAttachments_shouldThrowValidationException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "archive.zip",
                "application/zip",
                "fake-zip-content".getBytes()
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageService.storeAttachmentFile(file, "sessions/1/questions/10")
        );

        assertEquals(VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Unsupported file type", exception.getMessage());
    }

    @Test
    void getFileType_whenFileHasUppercaseExtension_shouldReturnFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.PNG",
                "image/png",
                "fake-image-content".getBytes()
        );

        FileType fileType = fileStorageService.getFileType(file);

        assertEquals(FileType.PNG, fileType);
    }

    @Test
    void getFileType_whenFileHasJpegExtension_shouldReturnJpegFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpeg",
                "image/jpeg",
                "fake-image-content".getBytes()
        );

        FileType fileType = fileStorageService.getFileType(file);

        assertEquals(FileType.JPEG, fileType);
    }

    @Test
    void getFileType_whenFileIsEmpty_shouldThrowValidationException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[0]
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageService.getFileType(file)
        );

        assertEquals(VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("File is required", exception.getMessage());
    }
}