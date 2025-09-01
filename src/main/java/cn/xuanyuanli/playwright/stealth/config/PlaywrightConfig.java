package cn.xuanyuanli.playwright.stealth.config;

import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.options.Proxy;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;
import java.util.Objects;

/**
 * Playwright配置类
 *
 * <p>提供Playwright浏览器启动和行为的各种配置选项，包括：</p>
 * <ul>
 *   <li>浏览器启动参数配置</li>
 *   <li>反检测功能配置</li>
 *   <li>上下文和代理配置</li>
 *   <li>自定义初始化脚本注入</li>
 * </ul>
 *
 * @author xuanyuanli
 */
@Data
@Accessors(chain = true)
public class PlaywrightConfig {

    /**
     * 是否禁用自动化控制标识
     *
     * <p>禁用后会在启动参数中添加 --disable-blink-features=AutomationControlled，
     * 帮助绕过一些基于 navigator.webdriver 的检测</p>
     *
     * <p>默认值：true</p>
     */
    private boolean disableAutomationControlled = true;

    /**
     * 是否禁用GPU加速
     *
     * <p>禁用后会在启动参数中添加 --disable-gpu，
     * 在无头模式下可以提高稳定性并减少资源消耗</p>
     *
     * <p>默认值：true</p>
     */
    private boolean disableGpu = true;

    /**
     * 是否禁用图像渲染
     *
     * <p>禁用后会在启动参数中添加 --blink-settings=imagesEnabled=false，
     * 可以显著提高页面加载速度，适用于不需要处理图片的场景</p>
     *
     * <p>默认值：true</p>
     */
    private boolean disableImageRender = true;

    /**
     * 是否启动时窗口最大化
     *
     * <p>启用后会在启动参数中添加 --start-maximized，
     * 仅在非无头模式下有效</p>
     *
     * <p>默认值：false</p>
     */
    private boolean startMaximized = false;

    /**
     * 是否以无头模式运行
     *
     * <p>无头模式不会显示浏览器界面，适用于服务器环境和自动化任务</p>
     *
     * <p>默认值：true</p>
     */
    private boolean headless = true;

    /**
     * 是否启用Chromium沙箱模式
     *
     * <p>沙箱模式可以提高安全性，但在某些环境下可能导致启动失败</p>
     *
     * <p>默认值：false</p>
     */
    private boolean chromiumSandbox = false;

    /**
     * 反检测模式配置
     *
     * <p>控制反检测功能的启用级别，支持三种模式：</p>
     * <ul>
     *   <li>{@link StealthMode#DISABLED} - 禁用反检测，性能最佳</li>
     *   <li>{@link StealthMode#LIGHT} - 轻量级反检测，基础功能</li>
     *   <li>{@link StealthMode#FULL} - 完整反检测，全面防护</li>
     * </ul>
     *
     * <p>默认值：{@link StealthMode#FULL}</p>
     */
    private StealthMode stealthMode = StealthMode.LIGHT;

    /**
     * 操作延迟时间（毫秒）
     *
     * <p>设置后Playwright会在每个操作之间等待指定的毫秒数，
     * 有助于调试和模拟人类操作行为</p>
     *
     * <p>默认值：null（无延迟）</p>
     */
    private Double slowMo;

    /**
     * 浏览器上下文选项
     *
     * <p>用于配置浏览器上下文的各种选项，如User-Agent、视口大小、权限等</p>
     *
     * <p>默认配置：</p>
     * <ul>
     *   <li>忽略HTTPS错误：true</li>
     *   <li>User-Agent：Chrome 132.0.0.0</li>
     * </ul>
     */
    private NewContextOptions newContextOptions = new NewContextOptions()
            .setIgnoreHTTPSErrors(true)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36");

    /**
     * 代理配置
     *
     * <p>用于配置HTTP/HTTPS/SOCKS代理，支持认证</p>
     *
     * <p>默认值：null（不使用代理）</p>
     */
    private Proxy proxy;

    /**
     * 自定义初始化脚本列表
     *
     * <p>在页面加载之前注入的JavaScript代码，用于自定义页面行为或添加额外的反检测功能。
     * 这些脚本会在内置反检测脚本之后按顺序执行。</p>
     *
     * <p><strong>特性：</strong></p>
     * <ul>
     *   <li>支持多个脚本，按列表顺序依次注入</li>
     *   <li>与内置反检测脚本兼容，可以共同使用</li>
     *   <li>在每个新页面加载时自动执行</li>
     *   <li>支持复杂的JavaScript逻辑和异步操作</li>
     * </ul>
     *
     * <p><strong>使用场景：</strong></p>
     * <ul>
     *   <li>自定义反检测逻辑</li>
     *   <li>修改浏览器指纹信息</li>
     *   <li>注入工具函数或全局变量</li>
     *   <li>增强现有反检测功能</li>
     * </ul>
     *
     * <p><strong>注意事项：</strong></p>
     * <ul>
     *   <li>脚本执行失败不会阻止页面加载</li>
     *   <li>避免使用可能导致页面行为异常的脚本</li>
     *   <li>建议进行充分测试以确保脚本稳定性</li>
     * </ul>
     *
     * <p>默认值：null（不注入自定义脚本）</p>
     *
     * @see StealthMode 内置反检测模式
     */
    private List<String> customInitScripts;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PlaywrightConfig that = (PlaywrightConfig) o;
        return disableAutomationControlled == that.disableAutomationControlled &&
                disableGpu == that.disableGpu &&
                disableImageRender == that.disableImageRender &&
                startMaximized == that.startMaximized &&
                headless == that.headless &&
                chromiumSandbox == that.chromiumSandbox &&
                stealthMode == that.stealthMode &&
                Objects.equals(slowMo, that.slowMo) &&
                Objects.equals(contextOptionsToString(), that.contextOptionsToString()) &&
                Objects.equals(proxyToString(), that.proxyToString()) &&
                Objects.equals(customInitScripts, that.customInitScripts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disableAutomationControlled, disableGpu, disableImageRender,
                startMaximized, headless, chromiumSandbox, stealthMode, slowMo,
                contextOptionsToString(), proxyToString(), customInitScripts);
    }

    /**
     * 将NewContextOptions转换为字符串用于比较
     *
     * <p>由于NewContextOptions没有重写equals方法，我们提取关键属性进行字符串比较</p>
     *
     * @return 包含关键配置信息的字符串
     */
    private String contextOptionsToString() {
        if (newContextOptions == null) {
            return null;
        }
        return ToStringBuilder.reflectionToString(newContextOptions, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    /**
     * 将Proxy转换为字符串用于比较
     *
     * <p>由于Proxy没有重写equals方法，我们提取关键属性进行字符串比较</p>
     *
     * @return 包含代理配置信息的字符串
     */
    private String proxyToString() {
        if (proxy == null) {
            return null;
        }
        return ToStringBuilder.reflectionToString(proxy, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}