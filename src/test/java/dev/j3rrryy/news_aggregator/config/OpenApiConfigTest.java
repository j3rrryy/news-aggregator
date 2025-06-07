package dev.j3rrryy.news_aggregator.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;
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

    @Test
    void removeOnlyDefault400ShouldStripNonRef400ButKeepRef400() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(OpenApiConfig.class);
        context.refresh();

        OpenApiCustomizer customizer = context.getBean(OpenApiCustomizer.class);
        assertThat(customizer).isNotNull();

        OpenAPI openApi = new OpenAPI();
        PathItem pathItem = new PathItem();

        Operation getOp = new Operation().operationId("getOp");
        ApiResponses getResponses = new ApiResponses();
        getResponses.addApiResponse("200", new ApiResponse().description("OK"));
        getResponses.addApiResponse("400", new ApiResponse().description("Default 400"));
        getOp.setResponses(getResponses);
        pathItem.get(getOp);

        Operation postOp = new Operation().operationId("postOp");
        ApiResponses postResponses = new ApiResponses();
        postResponses.addApiResponse("200", new ApiResponse().description("OK"));
        postResponses.addApiResponse("400", new ApiResponse().$ref("#/components/responses/ValidationFailed"));
        postOp.setResponses(postResponses);
        pathItem.post(postOp);

        openApi.path("/test", pathItem);
        customizer.customise(openApi);

        ApiResponses afterGet = openApi.getPaths().get("/test").getGet().getResponses();
        assertThat(afterGet.containsKey("200")).isTrue();
        assertThat(afterGet.containsKey("400")).isFalse();

        ApiResponses afterPost = openApi.getPaths().get("/test").getPost().getResponses();
        assertThat(afterPost.containsKey("200")).isTrue();
        assertThat(afterPost.containsKey("400")).isTrue();

        ApiResponse kept = afterPost.get("400");
        assertThat(kept.get$ref()).isEqualTo("#/components/responses/ValidationFailed");
        context.close();
    }

    @Test
    void removeOnlyDefault400ShouldDoNothingWhenPathsAreNull() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(OpenApiConfig.class);
        context.refresh();
        OpenApiCustomizer customizer = context.getBean(OpenApiCustomizer.class);

        OpenAPI openApi = new OpenAPI();
        openApi.setPaths(null);

        customizer.customise(openApi);
        assertThat(openApi.getPaths()).isNull();
        context.close();
    }

    @Test
    void removeOnlyDefault400ShouldNotRemoveOtherStatusCodes() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(OpenApiConfig.class);
        context.refresh();
        OpenApiCustomizer customizer = context.getBean(OpenApiCustomizer.class);

        OpenAPI openApi = new OpenAPI();
        PathItem pathItem = new PathItem();

        Operation op = new Operation().operationId("op");
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", new ApiResponse().description("OK"));
        op.setResponses(responses);
        pathItem.get(op);

        openApi.path("/nop", pathItem);
        customizer.customise(openApi);

        ApiResponses after = openApi.getPaths().get("/nop").getGet().getResponses();
        assertThat(after.containsKey("200")).isTrue();
        assertThat(after.containsKey("400")).isFalse();
        context.close();
    }

}
