package dev.j3rrryy.news_aggregator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI defineOpenAPI() {
        Info info = new Info()
                .title("News Aggregator")
                .version("v0.1.0")
                .description("API for news collection and analytics");
        return new OpenAPI().info(info);
    }

}
