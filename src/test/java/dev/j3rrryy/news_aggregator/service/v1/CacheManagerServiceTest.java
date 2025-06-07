package dev.j3rrryy.news_aggregator.service.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.mockito.Mockito.*;

public class CacheManagerServiceTest {

    private CacheManager cacheManager;
    private CacheManagerService cacheManagerService;

    @BeforeEach
    void setUp() {
        cacheManager = mock(CacheManager.class);
        cacheManagerService = new CacheManagerService(cacheManager);
    }

    @Test
    void clearAllCaches_shouldClearEachCache() {
        Cache cache1 = mock(Cache.class);
        Cache cache2 = mock(Cache.class);

        List<String> cacheNames = List.of("test:cache1", "test:cache2");

        when(cacheManager.getCacheNames()).thenReturn(cacheNames);
        when(cacheManager.getCache("test:cache1")).thenReturn(cache1);
        when(cacheManager.getCache("test:cache2")).thenReturn(cache2);

        cacheManagerService.clearAllCaches();

        verify(cache1).clear();
        verify(cache2).clear();
    }

    @Test
    void clearAllCaches_shouldSkipNullCaches() {
        when(cacheManager.getCacheNames()).thenReturn(List.of("test:cache3"));
        when(cacheManager.getCache("test:cache3")).thenReturn(null);

        cacheManagerService.clearAllCaches();

        verify(cacheManager).getCache("test:cache3");
    }

}
