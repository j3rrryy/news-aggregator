package dev.j3rrryy.news_aggregator.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI defineOpenAPI() {
        Info info = new Info()
                .title("News Aggregator")
                .version("v1.0.0")
                .description("API for news collection and analytics");
        Components components = new Components()
                .addResponses("ValidationFailed", new ApiResponse()
                        .description("Validation failed")
                        .content(new Content().addMediaType(
                                        "application/json",
                                        new MediaType().schema(new Schema<>().example(new HashMap<>()))
                                )
                        )
                );
        return new OpenAPI().info(info).components(components);
    }

}
