package com.sambath.admincafe.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class UploadStaticConfig implements WebMvcConfigurer {

    private final FileStorageService storage;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storage.getRoot().toUri().toString();
        registry.addResourceHandler(storage.getPublicPath() + "/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
