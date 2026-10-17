package com.proflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Jwt jwt = new Jwt();
    private Client client = new Client();
    private Upload upload = new Upload();
    private Zoom zoom = new Zoom();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000;
    }

    @Getter
    @Setter
    public static class Client {
        private String url = "http://localhost:5173";
    }

    @Getter
    @Setter
    public static class Upload {
        private String dir = "uploads";
    }

    @Getter
    @Setter
    public static class Zoom {
        private String accountId;
        private String clientId;
        private String clientSecret;
        private String hostEmail;
    }
}
