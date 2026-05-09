package com.echoboard.service.impl;

import com.echoboard.enums.FileType;
import com.echoboard.exception.AppException;
import com.echoboard.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static com.echoboard.exception.ErrorCode.VALIDATION_ERROR;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final String ROOT_PATH = "uploads";
    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;

    private static final Set<FileType> ALLOWED_IMAGE_TYPES = Set.of(
            FileType.PNG,
            FileType.JPG,
            FileType.JPEG
    );

    private static final Set<FileType> ALLOWED_ATTACHMENT_TYPES = Set.of(
            FileType.PNG,
            FileType.JPG,
            FileType.JPEG,
            FileType.PDF,
            FileType.DOC,
            FileType.DOCX
    );

    @Override
    public String storeImageFile(MultipartFile file, String folderName) {
        return storeFile(file, folderName, ALLOWED_IMAGE_TYPES);
    }

    @Override
    public String storeAttachmentFile(MultipartFile file, String folderName) {
        return storeFile(file, folderName, ALLOWED_ATTACHMENT_TYPES);
    }

    @Override
    public FileType getFileType(MultipartFile file) {
        validateFileExists(file);
        return extractFileType(file.getOriginalFilename());
    }

    private String storeFile(
            MultipartFile file,
            String folderName,
            Set<FileType> allowedFileTypes
    ) {
        validateFileExists(file);
        validateFileSize(file);

        FileType fileType = extractFileType(file.getOriginalFilename());
        validateFileType(fileType, allowedFileTypes);

        Path uploadDirectory = Path.of(ROOT_PATH, folderName);
        createUploadDirectory(uploadDirectory);

        String safeFileName = UUID.randomUUID() + "." + fileType.name().toLowerCase();
        Path targetPath = uploadDirectory.resolve(safeFileName);

        storeFileOnDisk(file, targetPath);

        return targetPath.toString().replace("\\", "/");
    }

    private void validateFileExists(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "File is required"
            );
        }
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "File is too large"
            );
        }
    }

    private void validateFileType(FileType fileType, Set<FileType> allowedFileTypes) {
        if (!allowedFileTypes.contains(fileType)) {
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "Unsupported file type"
            );
        }
    }

    private void createUploadDirectory(Path uploadDirectory) {
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException exception) {
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "Failed to create upload directory"
            );
        }
    }

    private void storeFileOnDisk(MultipartFile file, Path targetPath) {
        try {
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "Failed to store file"
            );
        }
    }

    private FileType extractFileType(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "File extension is required"
            );
        }

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf(".") + 1)
                .toUpperCase();

        try {
            return FileType.valueOf(extension);
        } catch (IllegalArgumentException exception) {
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "Unsupported file type"
            );
        }
    }
}