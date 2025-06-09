package com.igot.cb.util.redis.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RedisConfigTest {

    @InjectMocks
    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
        ReflectionTestUtils.setField(redisConfig, "redisTimeout", 60000L);
    }

    @Test
    void redisConnectionFactory() {
        RedisConnectionFactory factory = redisConfig.redisConnectionFactory();

        assertNotNull(factory);
        assertTrue(factory instanceof LettuceConnectionFactory);

        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        lettuceFactory.afterPropertiesSet(); // Ensures internal state is initialized

        RedisStandaloneConfiguration config = lettuceFactory.getStandaloneConfiguration();
        assertEquals("localhost", config.getHostName());
        assertEquals(6379, config.getPort());
        assertEquals(0, config.getDatabase());

        Optional<Duration> commandTimeout = Optional.of(lettuceFactory.getClientConfiguration().getCommandTimeout());
        assertTrue(commandTimeout.isPresent());
        assertEquals(Duration.ofMillis(60000), commandTimeout.get());
    }

    @Test
    void redisConnectionFactoryWithDefaults() {
        ReflectionTestUtils.setField(redisConfig, "redisTimeout", 0L); // simulate edge case

        RedisConnectionFactory factory = redisConfig.redisConnectionFactory();

        assertNotNull(factory);
    }

    @Test
    void buildPoolConfig() {
        // Act
        GenericObjectPoolConfig<?> poolConfig = ReflectionTestUtils.invokeMethod(redisConfig, "buildPoolConfig");

        // Assert
        assertNotNull(poolConfig);
        assertEquals(3000, poolConfig.getMaxTotal());
        assertEquals(128, poolConfig.getMaxIdle());
        assertEquals(100, poolConfig.getMinIdle());
        // Use getMaxWaitDuration() instead of getMaxWait()
        assertEquals(Duration.ofMillis(5000), poolConfig.getMaxWaitDuration());
    }


    @Test
    void redisTemplate() {
        // Arrange
        RedisConnectionFactory mockFactory = new LettuceConnectionFactory();

        // Act
        RedisTemplate<String, String> template = redisConfig.redisTemplate(mockFactory);

        // Assert
        assertNotNull(template);
        assertEquals(mockFactory, template.getConnectionFactory());
    }
}