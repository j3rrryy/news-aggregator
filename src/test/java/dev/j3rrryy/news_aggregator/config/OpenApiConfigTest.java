package dev.j3rrryy.news_aggregator.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenApiConfigTest {

    @Test
    void defineOpenAPIBeanShouldContainCorrectInfoAndComponents() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(OpenApiConfig.class);
        context.refresh();

        OpenAPI openAPI = context.getBean(OpenAPI.class);
        assertThat(openAPI).isNotNull();

        Info info = openAPI.getInfo();
        assertThat(info).isNotNull();
        assertThat(info.getTitle()).isEqualTo("News Aggregator");
        assertThat(info.getVersion()).isEqualTo("v1.0.0");
        assertThat(info.getDescription()).isEqualTo("API for news collection and analytics");

        Components components = openAPI.getComponents();
        assertThat(components).isNotNull();
        assertThat(components.getResponses()).containsKey("ValidationFailed");

        ApiResponse validationFailed = components.getResponses().get("ValidationFailed");
        assertThat(validationFailed).isNotNull();
        assertThat(validationFailed.getDescription()).isEqualTo("Validation failed");

        assertThat(validationFailed.getContent()).containsKey("application/json");
        MediaType jsonMediaType = validationFailed.getContent().get("application/json");
        assertThat(jsonMediaType).isNotNull();

        Schema<?> schema = jsonMediaType.getSchema();
        assertThat(schema).isNotNull();

        Object example = schema.getExample();
        assertThat(example).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) example)).isEmpty();
        context.close();
    }

}
