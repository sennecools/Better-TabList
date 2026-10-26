package com.sennecools.tablist.metrics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ServerMetricsResolverTest {

    @AfterEach
    void clearSample() {
        ServerMetricsResolver.reset();
    }

    @Test
    void vanillaDoesNotInventRollingAveragesOrPercentiles() {
        String result = ServerMetricsResolver.replacePlaceholders(
                "#TPS #CTPS #MSPT #CMSPT #TPS_1M #CTPS_5M #MSPT_P95 #CMSPT_P95",
                () -> ServerMetricsResolver.vanillaMetrics(40.0, Double.NaN));
        assertEquals("20.0 &a20.0 40.0 &a40.0 N/A N/A N/A N/A", result);
    }

    @Test
    void samplesOncePerUpdateAndResetsForNextUpdate() {
        Object server = new Object();
        AtomicInteger polls = new AtomicInteger();
        Supplier<ServerMetrics> supplier = () -> ServerMetricsResolver.sample(server,
                () -> ServerMetricsResolver.vanillaMetrics(polls.incrementAndGet() == 1 ? 100.0 : 50.0, Double.NaN));
        assertEquals("&c10.0 &c100.0", ServerMetricsResolver.replacePlaceholders("#CTPS #CMSPT", supplier));
        assertEquals("100.0", ServerMetricsResolver.replacePlaceholders("#MSPT", supplier));
        assertEquals(1, polls.get());
        ServerMetricsResolver.reset();
        assertEquals("&a20.0 &e50.0", ServerMetricsResolver.replacePlaceholders("#CTPS #CMSPT", supplier));
        assertEquals(2, polls.get());
    }

    @Test
    void leavesTemplatesWithoutMetricsUntouched() {
        assertEquals("{name} #AFK", ServerMetricsResolver.replacePlaceholders("{name} #AFK", () -> {
            throw new AssertionError("Templates without metrics must not poll the provider");
        }));
    }

    @Test
    void numbersAreLocaleIndependent() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("42.5", ServerMetricsResolver.replacePlaceholders("#MSPT",
                    () -> ServerMetricsResolver.vanillaMetrics(42.5, Double.NaN)));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void optionalSparkApiCanBeAbsentAtRuntime() {
        assertNull(getClass().getClassLoader().getResource("me/lucko/spark/api/SparkProvider.class"));
        assertNull(ServerMetricsResolver.pollSpark());
    }
}
