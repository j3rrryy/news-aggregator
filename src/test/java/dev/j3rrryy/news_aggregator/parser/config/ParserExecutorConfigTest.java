package dev.j3rrryy.news_aggregator.parser.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ParserExecutorConfigTest {

    @Test
    void beansShouldBeCreatedInContext() {
        try (var context = new AnnotationConfigApplicationContext(ParserExecutorConfig.class)) {
            ExecutorService ioExecutor = context.getBean("ioExecutor", ExecutorService.class);
            ExecutorService cpuExecutor = context.getBean("cpuExecutor", ExecutorService.class);

            assertThat(ioExecutor).isNotNull();
            assertThat(cpuExecutor).isNotNull();
        }
    }

}
