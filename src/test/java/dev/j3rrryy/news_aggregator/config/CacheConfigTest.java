package dev.j3rrryy.news_aggregator.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

public class CacheConfigTest {

    @Test
    void cacheManagerBeanShouldBeCreatedInContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.registerBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class));
        context.register(CacheConfig.class);
        context.refresh();

        RedisCacheManager cacheManager = context.getBean(RedisCacheManager.class);
        assertThat(cacheManager).isNotNull();
        context.close();
    }

}
