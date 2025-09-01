package cn.xuanyuanli.playwright.stealth.behavior;

import lombok.Getter;

/**
 * 人类行为模拟强度枚举
 * 
 * <p>定义了不同级别的人类行为模拟强度，用户可以根据需求选择合适的模拟级别。</p>
 * 
 * <h3>强度对比：</h3>
 * <table border="1">
 *   <tr>
 *     <th>强度</th>
 *     <th>执行时间</th>
 *     <th>适用场景</th>
 *   </tr>
 *   <tr>
 *     <td>QUICK</td>
 *     <td>0.5-1.5秒</td>
 *     <td>快速验证、批量处理</td>
 *   </tr>
 *   <tr>
 *     <td>NORMAL</td>
 *     <td>1.5-3秒</td>
 *     <td>一般网站、标准操作</td>
 *   </tr>
 *   <tr>
 *     <td>THOROUGH</td>
 *     <td>3-6秒</td>
 *     <td>强检测网站、高仿真需求</td>
 *   </tr>
 * </table>
 *
 * @author xuanyuanli
 */
@Getter
public enum BehaviorIntensity {
    
    /**
     * 快速强度
     * 
     * <p>执行快速的人类行为模拟，用于批量处理和快速验证。</p>
     * 
     * <p><strong>适用场景：</strong></p>
     * <ul>
     *   <li>快速功能验证</li>
     *   <li>批量数据处理</li>
     *   <li>内部系统测试</li>
     * </ul>
     */
    QUICK("快速强度", 500, 1500),
    
    /**
     * 正常强度
     * 
     * <p>执行标准的人类行为模拟，适用于大多数场景。</p>
     * 
     * <p><strong>适用场景：</strong></p>
     * <ul>
     *   <li>一般网站访问</li>
     *   <li>标准自动化任务</li>
     *   <li>平衡性能与真实性</li>
     * </ul>
     */
    NORMAL("正常强度", 1500, 3000),
    
    /**
     * 彻底强度
     * 
     * <p>执行彻底的人类行为模拟，提供最高的真实性。</p>
     * 
     * <p><strong>适用场景：</strong></p>
     * <ul>
     *   <li>强检测机制的网站</li>
     *   <li>高价值目标</li>
     *   <li>需要极高仿真度的场景</li>
     * </ul>
     */
    THOROUGH("彻底强度", 3000, 6000);
    
    /**
     * 强度描述
     */
    private final String description;
    
    /**
     * 最小总耗时（毫秒）
     */
    private final int minDurationMs;
    
    /**
     * 最大总耗时（毫秒）
     */
    private final int maxDurationMs;
    
    BehaviorIntensity(String description, int minDurationMs, int maxDurationMs) {
        this.description = description;
        this.minDurationMs = minDurationMs;
        this.maxDurationMs = maxDurationMs;
    }
    
    /**
     * 检查是否为快速强度
     *
     * @return true表示快速强度
     */
    public boolean isQuick() {
        return this == QUICK;
    }
    
    /**
     * 检查是否为彻底强度
     *
     * @return true表示彻底强度
     */
    public boolean isThorough() {
        return this == THOROUGH;
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s, 耗时: %d-%dms)", 
                           name(), description, minDurationMs, maxDurationMs);
    }
}