package com.example.demo.stats;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

import com.example.demo.common.AppConfig;

@Service
public class ProductStatsService {
    private static final Logger log = LoggerFactory.getLogger(ProductStatsService.class);
    private static final String VIEWS_KEY = "demo:product:views";
    private static final String RECENT_KEY_PREFIX = "demo:recent:";
    private final StringRedisTemplate redisTemplate;

    private final Duration recentTtl;
    private final int recentLimit;
    private final int topLimit;

    public ProductStatsService(StringRedisTemplate redisTemplate, AppConfig config) {
        this.redisTemplate = redisTemplate;
        this.recentTtl = config.getDuration("app.stats.recent-ttl");
        this.recentLimit = config.getInt("app.stats.recent-limit");
        this.topLimit = config.getInt("app.stats.top-limit");
        log.info("[Stats] recent-ttl={}, recent-limit={}, top-limit={}", recentTtl, recentLimit, topLimit);
    }

    // Ghi nhận 1 lượt xem, trả về tổng lượt xem MỚI của sản phẩm
    public long recordView(long productId, String username) {
        String id = String.valueOf(productId);
        try {
            Double views = redisTemplate.opsForZSet().incrementScore(VIEWS_KEY, id, 1);
            String recentKey = RECENT_KEY_PREFIX + username;
            redisTemplate.opsForList().remove(recentKey, 0, id);
            redisTemplate.opsForList().leftPush(recentKey, id);
            redisTemplate.opsForList().trim(recentKey, 0, recentLimit - 1);
            redisTemplate.expire(recentKey, recentTtl);

            return views.longValue();
        } catch (DataAccessException e) {
            log.warn("[Stats] Redis lỗi, không ghi được lượt xem sản phẩm {}: {}", productId, e.getMessage());
            return 0;
        }
    }

    // Lượt xem của MỌI sản phẩm, lấy bằng 1 lệnh duy nhất: id → số lượt xem
    public Map<Long, Long> allViews() {
        Map<Long, Long> result = new HashMap<>();
        try {
            Set<TypedTuple<String>> tuples = redisTemplate.opsForZSet().rangeWithScores(VIEWS_KEY, 0, -1);
            for (TypedTuple<String> t : tuples) {
                result.put(Long.valueOf(t.getValue()), t.getScore().longValue());
            }
        } catch (DataAccessException e) {
            log.warn("[Stats] Redis lỗi, không đọc được lượt xem: {}", e.getMessage());
        }
        return  result;
    }

    // Id của top-limit sản phẩm xem nhiều nhất, nhiều nhất đứng đầu
    public List<Long> topProductIds() {
        List<Long> ids = new ArrayList<>();
        try {
            Set<String> members = redisTemplate.opsForZSet().reverseRange(VIEWS_KEY, 0, topLimit - 1);
            for (String m : members) {
                ids.add(Long.valueOf(m));
            }
        } catch (DataAccessException e) {
            log.warn("[Stats] Redis lỗi, không đọc được top sản phẩm: {}", e.getMessage());
        }
        return ids;
    }

    // Id các sản phẩm user vừa xem, mới nhất đứng đầu
    public List<Long> recentProductIds(String username) {
        List<Long> ids = new ArrayList<>();
        try {
            List<String> members = redisTemplate.opsForList().range(RECENT_KEY_PREFIX + username, 0, -1);
            for (String m : members) {
                ids.add(Long.valueOf(m));
            }
        } catch (DataAccessException e) {
            log.warn("[Stats] Redis lỗi, không đọc được sản phẩm vừa xem của {}: {}", username, e.getMessage());
        }
        return ids;
    }

    // Xoá sản phẩm khỏi bảng xếp hạng (gọi khi xoá sản phẩm trong MySQL)
    public void removeProduct(long productId) {
        try {
            redisTemplate.opsForZSet().remove(VIEWS_KEY, String.valueOf(productId));
        } catch (DataAccessException e) {
            log.warn("[Stats] Redis lỗi, không xoá được lượt xem sản phẩm {}: {}", productId, e.getMessage());
        }
    }
}