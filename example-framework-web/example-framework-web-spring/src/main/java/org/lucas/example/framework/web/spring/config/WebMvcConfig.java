package org.lucas.example.framework.web.spring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置业务线程池
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        ThreadPoolTaskExecutor threadPool = new ThreadPoolTaskExecutor();
        threadPool.setCorePoolSize(8);
        threadPool.setMaxPoolSize(64);
        threadPool.setQueueCapacity(64);
        threadPool.setThreadNamePrefix("default-");
        threadPool.initialize();
        configurer.setTaskExecutor(threadPool);
    }
}