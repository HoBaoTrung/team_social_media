package com.codegym.socialmedia.component.upload_file;

import org.springframework.web.multipart.MultipartFile;

public interface ImageUploader {
    String upload(MultipartFile file);
}
