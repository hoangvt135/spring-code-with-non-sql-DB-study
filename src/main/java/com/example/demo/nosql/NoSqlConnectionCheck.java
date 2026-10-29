package com.example.demo.nosql;

import java.time.Duration;
import java.time.LocalDateTime;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class NoSqlConnectionCheck implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(NoSqlConnectionCheck.class);
    private final MongoTemplate mongoTemplate;
    private final StringRedisTemplate redisTemplate;

    public NoSqlConnectionCheck(MongoTemplate mongoTemplate, StringRedisTemplate redisTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) {
        checkMongo();
        checkRedis();
    }

    private void checkMongo() {
        try {
            Document doc = new Document("_id", "last-startup").append("startedAt", LocalDateTime.now().toString());
            mongoTemplate.save(doc, "startup_log");
            Document saved = mongoTemplate.findById("last-startup", Document.class, "startup_log");
            log.info("[NoSQL] MongoDB OK, database = {}, startup_log = {}", mongoTemplate.getDb().getName(), saved.toJson());
        } catch (Exception e) {
            log.error("[NoSQL] không kết nối được mongoDB: {}", e.getMessage());
        }
    }

    private void checkRedis() {
        try {
            redisTemplate.opsForValue().set("demo:last-startup", LocalDateTime.now().toString());
            String value = redisTemplate.opsForValue().get("demo:last-startup");
            log.info("[NoSQL] Redis OK, demo:last-startup = {}", value);
        } catch (Exception e) {
            log.error("[NoSQL] không kết nối được Redis: {}", e.getMessage());
        }
    }

}
