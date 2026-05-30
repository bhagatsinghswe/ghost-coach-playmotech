package com.playmotech.ghostcoach.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Upload upload = new Upload();
    private Gemini gemini = new Gemini();

    @Getter @Setter
    public static class Upload {
        private String dir = "./uploads";
        private long maxSizeBytes = 5_242_880L; // 5 MB
    }

    @Getter @Setter
    public static class Gemini {
        private String apiKey;
        private String baseUrl;
        private String model = "gemini-1.5-flash";
        private int timeoutSeconds = 60;
    }
}
