package com.college.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * CORS + registration of the login check.
 * app.cors.allowed-origins (env CORS_ORIGINS) = comma-separated list of front-end origins allowed to call the API.
 * Locally the React dev server proxies /api, so CORS is not even needed there.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor interceptor;

    public WebConfig(AuthInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/**");
    }

    /** Runs first, so even 410 / 429 answers of SecurityFilter carry the CORS headers. */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(
            @Value("${app.cors.allowed-origins:http://localhost:5173,https://*.app.github.dev}") String origins) {
        List<String> list = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(list);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setExposedHeaders(List.of("Content-Disposition", "Retry-After"));
        cfg.setAllowCredentials(false);           // bearer token in a header, no cookies
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
