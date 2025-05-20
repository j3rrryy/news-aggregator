package dev.j3rrryy.news_aggregator.parser.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ParserExecutorConfig {

    @Bean
    public ExecutorService ioExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public ExecutorService cpuExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                cores, cores * 2, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

}
