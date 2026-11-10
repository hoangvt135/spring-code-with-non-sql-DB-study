package com.example.demo.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;

// Tự nạp MỌI file src/main/resources/config/*.properties lúc app khởi động.
// Thêm module mới chỉ cần thêm file .properties, không phải khai báo ở đâu cả.
// Không phải @Component: nó chạy TRƯỚC khi Spring tạo bean, nên được đăng ký trong META-INF/spring.factories
public class ModuleConfigLoader implements EnvironmentPostProcessor {
    private static final String PATTERN = "classpath*:config/*.properties";

    private final Log log;

    public ModuleConfigLoader(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(ModuleConfigLoader.class);
    }
    
    @Override 
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            Resource[] files = new PathMatchingResourcePatternResolver().getResources(PATTERN);
            Arrays.sort(files, Comparator.comparing(Resource::getFilename));
            for (Resource file : files) {
                Properties props = PropertiesLoaderUtils.loadProperties(new EncodedResource(file, StandardCharsets.UTF_8));
                environment.getPropertySources().addLast(new PropertiesPropertySource("config/" + file.getFilename() , props));
                log.info("[Config] đã nạp config/" + file.getFilename() + " (" + props.size() + " key)");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được file cấu hình trong config/", e);
        }
    }
}
