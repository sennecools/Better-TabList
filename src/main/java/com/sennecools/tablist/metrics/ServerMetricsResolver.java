package com.sennecools.tablist.metrics;

import com.sennecools.tablist.config.TabListConfig;
import com.sennecools.tablist.platform.Services;
import net.minecraft.server.MinecraftServer;

import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.function.Supplier;

/** Performance placeholders inspired by MihailDenisov's LPintegration branch. */
public final class ServerMetricsResolver {

    private static WeakReference<Object> cachedServer = new WeakReference<>(null);
    private static ServerMetrics cachedMetrics;

    private ServerMetricsResolver() {
    }

    public static void reset() {
        cachedServer.clear();
        cachedMetrics = null;
    }

    public static String replacePlaceholders(String text, MinecraftServer server) {
        return replacePlaceholders(text, () -> resolve(server));
    }

    static String replacePlaceholders(String text, Supplier<ServerMetrics> supplier) {
        if (!text.contains("#TPS") && !text.contains("#CTPS")
                && !text.contains("#MSPT") && !text.contains("#CMSPT")
                && !text.contains("#CPU") && !text.contains("#CCPU")) return text;

        ServerMetrics metrics = supplier.get();
        // Longer names must be replaced before their shorter aliases.
        return text
                .replace("#CTPS_1M", coloredTps(metrics.tps1m()))
                .replace("#CTPS_5M", coloredTps(metrics.tps5m()))
                .replace("#CMSPT_P95", coloredMspt(metrics.mspt95p()))
                .replace("#TPS_1M", format(metrics.tps1m()))
                .replace("#TPS_5M", format(metrics.tps5m()))
                .replace("#MSPT_P95", format(metrics.mspt95p()))
                .replace("#CTPS", coloredTps(metrics.tps()))
                .replace("#CMSPT", coloredMspt(metrics.mspt()))
                .replace("#CCPU", colored(metrics.cpu(), metrics.cpu() <= 60.0 ? "&a" : metrics.cpu() <= 85.0 ? "&e" : "&c"))
                .replace("#TPS", format(metrics.tps()))
                .replace("#MSPT", format(metrics.mspt()))
                .replace("#CPU", format(metrics.cpu()));
    }

    private static ServerMetrics resolve(MinecraftServer server) {
        return sample(server, () -> {
            ServerMetrics metrics = null;
            if (TabListConfig.useSparkMetrics && Services.PLATFORM.isModLoaded("spark")) {
                metrics = pollSpark();
            }
            return metrics != null ? metrics
                    : vanillaMetrics(server.getAverageTickTimeNanos() / 1_000_000.0, getProcessCpuLoad());
        });
    }

    static ServerMetrics pollSpark() {
        // Keep the call itself inside the guard: optional API types may fail class verification.
        try {
            return SparkMetricsProvider.poll();
        } catch (IllegalStateException | LinkageError exception) {
            return null;
        }
    }

    static ServerMetrics vanillaMetrics(double mspt, double cpu) {
        double tps = mspt <= 0.0 ? 20.0 : Math.min(1000.0 / mspt, 20.0);
        // Vanilla's average cannot stand in for rolling TPS or a 95th percentile.
        return new ServerMetrics(tps, Double.NaN, Double.NaN, mspt, Double.NaN, cpu);
    }

    static ServerMetrics sample(Object source, Supplier<ServerMetrics> supplier) {
        // All players and both header/footer share one sample per update cycle.
        if (cachedServer.get() != source || cachedMetrics == null) {
            cachedMetrics = supplier.get();
            cachedServer = new WeakReference<>(source);
        }
        return cachedMetrics;
    }

    private static double getProcessCpuLoad() {
        if (ManagementFactory.getOperatingSystemMXBean()
                instanceof com.sun.management.OperatingSystemMXBean operatingSystem) {
            double load = operatingSystem.getProcessCpuLoad();
            return load < 0.0 ? Double.NaN : load * 100.0;
        }
        return Double.NaN;
    }

    private static String coloredTps(double value) {
        return colored(value, value >= 18.0 ? "&a" : value >= 15.0 ? "&e" : "&c");
    }

    private static String coloredMspt(double value) {
        return colored(value, value <= 40.0 ? "&a" : value <= 50.0 ? "&e" : "&c");
    }

    private static String colored(double value, String color) {
        return Double.isFinite(value) ? color + format(value) : "N/A";
    }

    private static String format(double value) {
        return Double.isFinite(value) ? String.format(Locale.ROOT, "%.1f", value) : "N/A";
    }
}
