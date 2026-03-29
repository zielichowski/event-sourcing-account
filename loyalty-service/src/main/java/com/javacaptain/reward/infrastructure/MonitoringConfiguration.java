package com.javacaptain.reward.infrastructure;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MonitoringConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags(){
        return registry -> registry.config().commonTags("application", "loyalty-service");
    }
}