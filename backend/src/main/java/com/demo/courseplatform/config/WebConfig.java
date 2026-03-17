package com.demo.courseplatform.config;

import com.demo.courseplatform.security.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final String frontendOrigin;

    public WebConfig(AuthInterceptor authInterceptor,
                     @Value("${demo.frontend-origin:http://localhost:5173}") String frontendOrigin) {
        this.authInterceptor = authInterceptor;
        this.frontendOrigin = frontendOrigin;
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
            .allowedOrigins(frontendOrigin)
            .allowedMethods("*")
            .allowedHeaders("*");
    }
}
