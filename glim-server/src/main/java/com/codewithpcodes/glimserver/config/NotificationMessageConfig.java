package com.codewithpcodes.glimserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class NotificationMessageConfig {

    @Bean
    public ResourceBundleMessageSource notificationMessages() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages/notifications");
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }
}
