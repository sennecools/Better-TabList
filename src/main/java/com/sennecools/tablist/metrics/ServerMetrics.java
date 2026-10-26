package com.sennecools.tablist.metrics;

public record ServerMetrics(double tps, double tps1m, double tps5m, double mspt, double mspt95p, double cpu) {
}
