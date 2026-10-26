package com.sennecools.tablist.metrics;

import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow.CpuUsage;
import me.lucko.spark.api.statistic.StatisticWindow.MillisPerTick;
import me.lucko.spark.api.statistic.StatisticWindow.TicksPerSecond;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;

final class SparkMetricsProvider {

    private SparkMetricsProvider() {
    }

    static ServerMetrics poll() {
        try {
            Spark spark = SparkProvider.get();
            if (spark.tps() == null || spark.mspt() == null) return null;

            DoubleAverageInfo mspt = spark.mspt().poll(MillisPerTick.MINUTES_1);
            if (mspt == null) return null;
            double tps1m = spark.tps().poll(TicksPerSecond.MINUTES_1);
            return new ServerMetrics(
                    tps1m, tps1m,
                    spark.tps().poll(TicksPerSecond.MINUTES_5),
                    mspt.mean(), mspt.percentile95th(),
                    spark.cpuProcess() == null ? Double.NaN : spark.cpuProcess().poll(CpuUsage.MINUTES_1) * 100.0
            );
        } catch (IllegalStateException | LinkageError exception) {
            return null;
        }
    }
}
