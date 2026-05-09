package com.echoboard.service.impl;

import com.echoboard.exception.AppException;
import com.echoboard.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.echoboard.exception.ErrorCode.VALIDATION_ERROR;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final String ROOT_PATH = "uploads/";
    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;


    @Override
    public String storeFile(MultipartFile file, String folderName) {
        if (file == null || file.isEmpty()) {
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "File is required"
            );
        }

        if(file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "File is too large"
            );
        }

        Path uploadDirectory = Path.of(ROOT_PATH, folderName);

        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException exception) {
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "Failed to create upload directory"
            );
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename
                    .substring(originalFilename.lastIndexOf("."))
                    .toLowerCase();
        }

        if (!extension.equals(".png") && !extension.equals(".jpg") && !extension.equals(".jpeg")) {
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "Only PNG and JPG images are allowed"
            );
        }
        String safeFileName = UUID.randomUUID() + extension;

        Path targetPath = uploadDirectory.resolve(safeFileName);

        try {
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "Failed to store file"
            );
        }
        return targetPath.toString().replace("\\", "/");
    }
}
