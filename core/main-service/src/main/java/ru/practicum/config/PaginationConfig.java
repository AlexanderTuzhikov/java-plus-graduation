package ru.practicum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PaginationConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer paginationCustomizer() {
        return pageableResolver -> {
            pageableResolver.setOneIndexedParameters(true);
            pageableResolver.setMaxPageSize(100);
            pageableResolver.setFallbackPageable(org.springframework.data.domain.PageRequest.of(0, 10));
        };
    }
}