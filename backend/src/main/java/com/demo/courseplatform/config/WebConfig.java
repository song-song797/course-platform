package com.demo.courseplatform.config;

import com.demo.courseplatform.security.AuthInterceptor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final String[] frontendOrigins;
    private final String uploadDir;

    public WebConfig(AuthInterceptor authInterceptor,
                     @Value("${demo.frontend-origins:http://localhost:5173,http://127.0.0.1:5173}") String frontendOrigins,
                     @Value("${demo.upload-dir:uploads}") String uploadDir) {
        this.authInterceptor = authInterceptor;
        this.frontendOrigins = Arrays.stream(frontendOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toArray(String[]::new);
        this.uploadDir = uploadDir;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/v1/**")
            .excludePathPatterns("/api/v1/auth/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
            .allowedOrigins(frontendOrigins)
            .allowedMethods("*")
            .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(uploadPath.toUri().toString());
    }
}
