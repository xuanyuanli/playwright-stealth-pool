package cn.xuanyuanli.playwright.stealth.config;

import lombok.Getter;

/**
 * 反检测模式枚举
 * 
 * <p>定义了不同级别的反检测功能，用户可以根据性能需求和检测强度选择合适的模式。</p>
 * 
 * <h3>模式对比：</h3>
 * <table border="1">
 *   <tr>
 *     <th>模式</th>
 *     <th>性能开销</th>
 *     <th>检测覆盖</th>
 *     <th>适用场景</th>
 *   </tr>
 *   <tr>
 *     <td>DISABLED</td>
 *     <td>无</td>
 *     <td>无</td>
 *     <td>内部测试、可信环境</td>
 *   </tr>
 *   <tr>
 *     <td>LIGHT</td>
 *     <td>极低</td>
 *     <td>基础检测</td>
 *     <td>性能敏感、批量任务</td>
 *   </tr>
 *   <tr>
 *     <td>FULL</td>
 *     <td>中等</td>
 *     <td>全面检测</td>
 *     <td>强检测网站、生产环境</td>
 *   </tr>
 * </table>
 *
 * @author xuanyuanli
 */
@Getter
public enum StealthMode {
    
    /**
     * 禁用反检测
     * 
     * <p>不注入任何反检测脚本，性能最佳但容易被检测到自动化特征。</p>
     * 
     * <p><strong>适用场景：</strong></p>
     * <ul>
     *   <li>内部系统测试</li>
     *   <li>不需要隐藏自动化特征的场景</li>
     *   <li>对性能要求极高的批量处理</li>
     * </ul>
     */
    DISABLED("禁用反检测", 0),
    
    /**
     * 轻量级反检测
     * 
     * <p>仅包含最基础的反检测功能，执行开销极低。主要修复：</p>
     * <ul>
     *   <li>隐藏 navigator.webdriver 属性</li>
     *   <li>设置基础的 navigator.languages</li>
     *   <li>设置基础的 navigator.platform</li>
     * </ul>
     * 
     * <p><strong>适用场景：</strong></p>
     * <ul>
     *   <li>性能敏感的应用</li>
     *   <li>检测机制较简单的网站</li>
     *   <li>大规模并发处理</li>
     * </ul>
     */
    LIGHT("轻量级反检测", 1),
    
    /**
     * 完整反检测
     * 
     * <p>包含全面的反检测功能，能够绕过大多数常见的自动化检测。主要功能：</p>
     * <ul>
     *   <li>Navigator 属性全面伪装</li>
     *   <li>Plugin 和 MimeType 模拟</li>
     *   <li>WebGL 指纹修复</li>
     *   <li>AudioContext 指纹修复</li>
     *   <li>权限 API 修复</li>
     *   <li>Chrome 扩展 API 清理</li>
     *   <li>开发者工具检测对抗</li>
     * </ul>
     * 
     * <p><strong>适用场景：</strong></p>
     * <ul>
     *   <li>具有强检测机制的网站</li>
     *   <li>生产环境的自动化任务</li>
     *   <li>需要高隐蔽性的数据采集</li>
     * </ul>
     */
    FULL("完整反检测", 2);
    
    /**
     * 模式描述
     */
    private final String description;
    
    /**
     * 性能开销级别（0-2，数字越大开销越大）
     */
    private final int performanceLevel;
    
    StealthMode(String description, int performanceLevel) {
        this.description = description;
        this.performanceLevel = performanceLevel;
    }

    /**
     * 检查是否启用了反检测功能
     *
     * @return true表示启用了反检测（LIGHT或FULL），false表示禁用（DISABLED）
     */
    public boolean isEnabled() {
        return this != DISABLED;
    }
    
    /**
     * 检查是否为轻量级模式
     *
     * @return true表示轻量级模式
     */
    public boolean isLight() {
        return this == LIGHT;
    }
    
    /**
     * 检查是否为完整模式
     *
     * @return true表示完整模式
     */
    public boolean isFull() {
        return this == FULL;
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s, 性能开销: %d)", 
                           name(), description, performanceLevel);
    }
}