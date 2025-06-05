package dev.j3rrryy.news_aggregator.parser.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SchedulerConfigTest {

    @Test
    void taskSchedulerBeanIsConfiguredCorrectly() {
        try (var context = new AnnotationConfigApplicationContext(SchedulerConfig.class)) {
            TaskScheduler taskScheduler = context.getBean(TaskScheduler.class);

            assertThat(taskScheduler).isInstanceOf(ThreadPoolTaskScheduler.class);
            ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) taskScheduler;

            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
        }
    }

}
