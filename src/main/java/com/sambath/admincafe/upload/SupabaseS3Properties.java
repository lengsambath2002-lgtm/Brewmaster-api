package com.sambath.admincafe.upload;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "supabase")
public class SupabaseS3Properties {

    private String url;
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class S3 {
        private String endpoint;
        private String region;
        private String accessKey;
        private String secretKey;
        private String bucket;
    }
}
