package cn.xuanyuanli.playwright.stealth.performance;

import java.util.Map;

/**
 * 性能基线管理工具
 * 
 * <p>提供性能测试的基准值和统计验证方法，避免硬编码阈值带来的不稳定性</p>
 *
 * @author xuanyuanli
 */
public class PerformanceBaseline {
    
    /**
     * 性能指标数据结构
     */
    public static class PerformanceMetric {
        public final double mean;      // 平均值 (ms)
        public final double stdDev;    // 标准差 (ms)
        public final double maxAllowed; // 最大允许值 (ms)
        
        public PerformanceMetric(double mean, double stdDev, double maxAllowed) {
            this.mean = mean;
            this.stdDev = stdDev;
            this.maxAllowed = maxAllowed;
        }
        
        public PerformanceMetric(double mean, double stdDev) {
            this(mean, stdDev, mean + 3 * stdDev); // 默认使用3σ规则
        }
    }
    
    /**
     * 性能基线数据 - 基于历史测试数据统计得出
     */
    private static final Map<String, PerformanceMetric> BASELINES = Map.of(
        // Stealth模式性能基线
        "stealth.disabled.avg", new PerformanceMetric(2500, 400, 4000),
        "stealth.light.avg", new PerformanceMetric(2800, 500, 4500), 
        "stealth.full.avg", new PerformanceMetric(3200, 600, 5000),
        
        // 连接池操作基线
        "pool.creation.avg", new PerformanceMetric(8000, 2000, 15000),
        "pool.concurrent.avg", new PerformanceMetric(1500, 300, 3000),
        
        // 内存使用基线 (MB)
        "memory.increase.max", new PerformanceMetric(300, 100, 500),
        "memory.cleanup.max", new PerformanceMetric(50, 30, 150)
    );
    
    /**
     * 验证性能指标是否在基线范围内
     *
     * @param metricName 性能指标名称
     * @param actualValue 实际测量值
     * @return 是否在可接受范围内
     */
    public static boolean isWithinBaseline(String metricName, double actualValue) {
        PerformanceMetric baseline = BASELINES.get(metricName);
        if (baseline == null) {
            return true; // 未定义基线的指标默认通过
        }
        
        return actualValue <= baseline.maxAllowed;
    }
    
    /**
     * 验证性能指标是否在统计正常范围内 (2σ)
     *
     * @param metricName 性能指标名称  
     * @param actualValue 实际测量值
     * @return 是否在统计正常范围内
     */
    public static boolean isStatisticallyNormal(String metricName, double actualValue) {
        PerformanceMetric baseline = BASELINES.get(metricName);
        if (baseline == null) {
            return true;
        }
        
        double lowerBound = baseline.mean - 2 * baseline.stdDev;
        double upperBound = baseline.mean + 2 * baseline.stdDev;
        
        return actualValue >= Math.max(0, lowerBound) && actualValue <= upperBound;
    }
    
    /**
     * 获取基线信息
     *
     * @param metricName 性能指标名称
     * @return 基线信息，如果不存在返回null
     */
    public static PerformanceMetric getBaseline(String metricName) {
        return BASELINES.get(metricName);
    }
    
    /**
     * 格式化性能报告
     *
     * @param metricName 性能指标名称
     * @param actualValue 实际测量值
     * @return 格式化的报告字符串
     */
    public static String formatReport(String metricName, double actualValue) {
        PerformanceMetric baseline = BASELINES.get(metricName);
        if (baseline == null) {
            return String.format("%s: %.1f ms (无基线)", metricName, actualValue);
        }
        
        boolean withinBaseline = actualValue <= baseline.maxAllowed;
        boolean statNormal = isStatisticallyNormal(metricName, actualValue);
        
        return String.format("%s: %.1f ms (基线: %.1f±%.1f ms, 最大: %.1f ms) %s%s",
                metricName, actualValue,
                baseline.mean, baseline.stdDev, baseline.maxAllowed,
                withinBaseline ? "✓" : "✗",
                statNormal ? " [正常]" : " [异常]");
    }
}