package com.sambath.admincafe.upload;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final S3Client s3;
    private final SupabaseS3Properties props;

    public FileStorageService(S3Client s3, SupabaseS3Properties props) {
        this.s3 = s3;
        this.props = props;
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("file exceeds max size of " + MAX_BYTES + " bytes");
        }

        String ext = resolveExtension(file);
        String key = UUID.randomUUID() + ext;

        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(props.getS3().getBucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read upload stream", e);
        }

        return publicUrl(key);
    }

    public boolean delete(String filename) {
        if (filename == null || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return false;
        }
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(props.getS3().getBucket())
                    .key(filename)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            return false;
        }
    }

    private String publicUrl(String key) {
        return props.getUrl() + "/storage/v1/object/public/" + props.getS3().getBucket() + "/" + key;
    }

    private static String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null) {
            return ".bin";
        }
        int dot = original.lastIndexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException("file has no extension");
        }
        String ext = original.substring(dot).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTS.contains(ext)) {
            throw new IllegalArgumentException("unsupported file type: " + ext);
        }
        return ext;
    }
}
