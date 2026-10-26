package com.codesense.prototype.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CodeSenseProperties properties;

    public WebConfig(CodeSenseProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = new ArrayList<>();
        origins.add("http://127.0.0.1:*");
        origins.add("http://localhost:*");
        String extra = properties.getFrontendOrigin();
        if (extra != null && !extra.isBlank() && !"*".equals(extra.trim())) {
            origins.add(extra.trim());
        }
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(false);
    }
}
