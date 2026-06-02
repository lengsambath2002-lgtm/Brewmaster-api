package com.sambath.admincafe.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final Path root;
    private final String publicPath;

    public FileStorageService(
            @Value("${app.upload.dir:./uploads}") String dir,
            @Value("${app.upload.public-path:/uploads}") String publicPath
    ) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        this.publicPath = publicPath.endsWith("/") ? publicPath.substring(0, publicPath.length() - 1) : publicPath;
    }

    @PostConstruct
    void ensureDir() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload dir: " + root, e);
        }
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("file exceeds max size of " + MAX_BYTES + " bytes");
        }

        String ext = resolveExtension(file);
        String filename = UUID.randomUUID() + ext;
        Path dest = root.resolve(filename).normalize();
        if (!dest.startsWith(root)) {
            throw new IllegalArgumentException("invalid filename");
        }

        try {
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file " + filename, e);
        }

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(publicPath + "/" + filename)
                .toUriString();
    }

    public boolean delete(String filename) {
        if (filename == null || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return false;
        }
        try {
            return Files.deleteIfExists(root.resolve(filename));
        } catch (IOException e) {
            return false;
        }
    }

    Path getRoot() {
        return root;
    }

    String getPublicPath() {
        return publicPath;
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
