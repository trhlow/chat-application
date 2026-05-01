package com.chatrealtime.storage;

import com.chatrealtime.exception.FileStorageException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
public class FileMimeDetector {
    private final Tika tika = new Tika();

    public String detect(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream, file.getOriginalFilename());
        } catch (IOException exception) {
            throw new FileStorageException("Cannot detect file type");
        }
    }
}
