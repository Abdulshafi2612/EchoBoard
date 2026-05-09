package com.echoboard.service;

import com.echoboard.enums.FileType;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeImageFile(MultipartFile file, String folderName);

    String storeAttachmentFile(MultipartFile file, String folderName);

    FileType getFileType(MultipartFile file);
}
