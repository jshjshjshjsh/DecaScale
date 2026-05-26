package com.jsh.decascale.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public DefaultRedisScript<Long> stockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                "local stock = redis.call('get', KEYS[1]) " +
                        "if not stock or tonumber(stock) <= 0 then " +
                        "   return 0 " + // 재고 없음 (입구컷)
                        "end " +
                        "redis.call('decr', KEYS[1]) " +
                        "return 1 " // 재고 차감 성공
        );
        return script;
    }
}