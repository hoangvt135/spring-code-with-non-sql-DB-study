package com.example.demo.common;

import java.time.Duration;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {
    private final Environment env;

    public AppConfig(Environment env) {
        this.env = env;
    }

    public String getString(String key) {
        return env.getRequiredProperty(key);
    }

    public int getInt(String key) {
        return env.getRequiredProperty(key, Integer.class);
    }

    public Duration getDuration(String key) {
        return env.getRequiredProperty(key, Duration.class);
    }
}
