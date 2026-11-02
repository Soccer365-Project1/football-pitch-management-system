package com.fpms.config;

import com.fpms.dto.response.PitchResponse;
import com.fpms.dto.response.PitchTypeResponse;
import com.fpms.dto.response.TimeSlotResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CatalogCacheConfigTest {
    @Test
    void configuresExpiryNamespaceAndSerializableNestedDtos() {
        var manager = new CatalogCacheConfig().cacheManager(
                mock(RedisConnectionFactory.class), Duration.ofMinutes(15));
        manager.afterPropertiesSet();
        var decorated = (org.springframework.cache.transaction.TransactionAwareCacheDecorator)
                manager.getCache("pitches");
        var config = ((org.springframework.data.redis.cache.RedisCache)
                decorated.getTargetCache()).getCacheConfiguration();
        assertEquals(Duration.ofMinutes(15), config.getTtlFunction().getTimeToLive("key", "value"));
        assertEquals("fpms:catalog:v1:pitches::", config.getKeyPrefixFor("pitches"));
        assertFalse(config.getAllowCacheNullValues());
        var value = List.of(PitchResponse.builder().id(1L)
                .pitchType(PitchTypeResponse.builder().build()).build());
        var serializer = config.getValueSerializationPair();
        var restored = (List<?>) serializer.read(serializer.write(value));
        assertInstanceOf(PitchResponse.class, restored.getFirst());
        assertNotNull(((PitchResponse) restored.getFirst()).getPitchType());
        var slot = TimeSlotResponse.builder().startTime(LocalTime.of(6, 0)).build();
        assertEquals(slot.getStartTime(), ((TimeSlotResponse)
                serializer.read(serializer.write(slot))).getStartTime());
    }

    @Test
    void separatesMethodNamesAndArguments() throws Exception {
        var generator = new CatalogCacheConfig().catalogKeyGenerator();
        var first = String.class.getMethod("substring", int.class);
        var second = String.class.getMethod("charAt", int.class);
        assertNotEquals(generator.generate("", first, 1), generator.generate("", second, 1));
        assertNotEquals(generator.generate("", first, 1), generator.generate("", first, 2));
    }
}
