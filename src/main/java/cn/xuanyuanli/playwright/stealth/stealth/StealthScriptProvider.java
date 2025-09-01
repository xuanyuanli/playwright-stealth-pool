package cn.xuanyuanli.playwright.stealth.stealth;

/**
 * 反检测脚本提供器
 * 
 * <p>提供用于绕过各种自动化检测的JavaScript脚本。这些脚本会在页面初始化时注入，
 * 用于修改浏览器的指纹特征，使其更难被识别为自动化程序。</p>
 * 
 * <p><strong>注意：</strong>使用这些脚本时请遵守目标网站的服务条款和相关法律法规</p>
 *
 * @author xuanyuanli
 */
public class StealthScriptProvider {

    /**
     * 获取完整的反检测脚本
     * 
     * <p>该脚本包含了多种反检测机制：</p>
     * <ul>
     *   <li>修改navigator.webdriver属性</li>
     *   <li>模拟navigator.languages</li>
     *   <li>模拟navigator.plugins和mimeTypes</li>
     *   <li>模拟硬件信息（platform, hardwareConcurrency, deviceMemory）</li>
     *   <li>修复WebGL指纹检测</li>
     *   <li>修复AudioContext检测</li>
     * </ul>
     *
     * @return JavaScript反检测脚本
     */
    public static String getStealthScript() {
        return """
                // ==================== Navigator WebDriver 修复 ====================
                // 移除自动化标识，防止通过navigator.webdriver检测
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined,
                    configurable: true
                });
                
                // ==================== Navigator Languages 模拟 ====================
                // 模拟真实的语言偏好
                Object.defineProperty(navigator, 'languages', {
                    get: () => ['zh-CN', 'zh', 'en-US', 'en'],
                    configurable: true
                });
                
                // ==================== Navigator Plugins 模拟 ====================
                // 模拟常见的浏览器插件，避免空插件列表被检测
                const mockPlugins = [
                    {
                        name: 'Chrome PDF Viewer',
                        filename: 'internal-pdf-viewer',
                        description: 'Portable Document Format',
                        length: 1
                    },
                    {
                        name: 'Native Client',
                        filename: 'internal-nacl-plugin',
                        description: 'Native Client',
                        length: 2
                    },
                    {
                        name: 'Chromium PDF Viewer',
                        filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai',
                        description: 'Portable Document Format',
                        length: 1
                    }
                ];
                
                const mockMimeTypes = [
                    {
                        type: 'application/pdf',
                        suffixes: 'pdf',
                        description: 'Portable Document Format',
                        enabledPlugin: mockPlugins[0]
                    },
                    {
                        type: 'application/x-nacl',
                        suffixes: '',
                        description: 'Native Client Executable',
                        enabledPlugin: mockPlugins[1]
                    },
                    {
                        type: 'application/x-pnacl',
                        suffixes: '',
                        description: 'Portable Native Client Executable',
                        enabledPlugin: mockPlugins[1]
                    }
                ];
                
                Object.defineProperty(navigator, 'plugins', {
                    get: () => {
                        const plugins = [...mockPlugins];
                        plugins.length = mockPlugins.length;
                        plugins.item = function(index) { return this[index] || null; };
                        plugins.namedItem = function(name) {
                            return this.find(plugin => plugin.name === name) || null;
                        };
                        return plugins;
                    },
                    configurable: true
                });
                
                Object.defineProperty(navigator, 'mimeTypes', {
                    get: () => {
                        const mimeTypes = [...mockMimeTypes];
                        mimeTypes.length = mockMimeTypes.length;
                        mimeTypes.item = function(index) { return this[index] || null; };
                        mimeTypes.namedItem = function(name) {
                            return this.find(mimeType => mimeType.type === name) || null;
                        };
                        return mimeTypes;
                    },
                    configurable: true
                });
                
                // ==================== Navigator 硬件信息模拟 ====================
                // 模拟真实的硬件平台信息
                Object.defineProperty(navigator, 'platform', {
                    get: () => 'Win32',
                    configurable: true
                });
                
                // 模拟合理的CPU核心数
                Object.defineProperty(navigator, 'hardwareConcurrency', {
                    get: () => 8,
                    configurable: true
                });
                
                // 模拟设备内存信息（GB）
                Object.defineProperty(navigator, 'deviceMemory', {
                    get: () => 8,
                    configurable: true
                });
                
                // 模拟其他Navigator属性
                Object.defineProperty(navigator, 'appName', {
                    get: () => 'Netscape',
                    configurable: true
                });
                
                Object.defineProperty(navigator, 'product', {
                    get: () => 'Gecko',
                    configurable: true
                });
                
                Object.defineProperty(navigator, 'productSub', {
                    get: () => '20030107',
                    configurable: true
                });
                
                // ==================== WebGL 指纹修复 ====================
                // 修复WebGL渲染器和厂商信息，避免被识别为虚拟环境
                if (typeof WebGLRenderingContext !== 'undefined') {
                    const getParameter = WebGLRenderingContext.prototype.getParameter;
                    WebGLRenderingContext.prototype.getParameter = function(parameter) {
                        // UNMASKED_VENDOR_WEBGL
                        if (parameter === 37445) {
                            return 'Intel Inc.';
                        }
                        // UNMASKED_RENDERER_WEBGL
                        if (parameter === 37446) {
                            return 'Intel(R) UHD Graphics 630';
                        }
                        return getParameter.call(this, parameter);
                    };
                }
                
                // WebGL2 支持
                if (typeof WebGL2RenderingContext !== 'undefined') {
                    const getParameter2 = WebGL2RenderingContext.prototype.getParameter;
                    WebGL2RenderingContext.prototype.getParameter = function(parameter) {
                        if (parameter === 37445) {
                            return 'Intel Inc.';
                        }
                        if (parameter === 37446) {
                            return 'Intel(R) UHD Graphics 630';
                        }
                        return getParameter2.call(this, parameter);
                    };
                }
                
                // ==================== AudioContext 指纹修复 ====================
                // 修复AudioContext的指纹检测
                const OriginalAudioContext = window.AudioContext || window.webkitAudioContext;
                if (OriginalAudioContext) {
                    const AudioContextProxy = new Proxy(OriginalAudioContext, {
                        construct(target, args) {
                            const context = new target(...args);
                
                            // 修改baseLatency属性
                            if (context.baseLatency !== undefined) {
                                Object.defineProperty(context, 'baseLatency', {
                                    get: () => 0.00512,
                                    configurable: true
                                });
                            }
                
                            return context;
                        }
                    });
                
                    window.AudioContext = AudioContextProxy;
                    if (window.webkitAudioContext) {
                        window.webkitAudioContext = AudioContextProxy;
                    }
                }
                
                // ==================== Permissions API 修复 ====================
                // 修复权限API查询结果
                if (navigator.permissions && navigator.permissions.query) {
                    const originalQuery = navigator.permissions.query;
                    navigator.permissions.query = function(parameters) {
                        return originalQuery(parameters).then(result => {
                            // 对于通知权限，返回granted状态
                            if (parameters.name === 'notifications') {
                                Object.defineProperty(result, 'state', {
                                    get: () => 'granted'
                                });
                            }
                            return result;
                        });
                    };
                }
                
                // ==================== Chrome Runtime 修复 ====================
                // 移除chrome.runtime等扩展API，避免被检测
                if (typeof chrome !== 'undefined' && chrome.runtime) {
                    delete chrome.runtime;
                }
                
                // ==================== 控制台调试检测对抗 ====================
                // 防止通过控制台调试检测
                let devtools = { open: false, orientation: null };
                setInterval(() => {
                    if (window.outerHeight - window.innerHeight > 200 ||
                        window.outerWidth - window.innerWidth > 200) {
                        devtools.open = true;
                        devtools.orientation = window.outerHeight - window.innerHeight > 200 ? 'vertical' : 'horizontal';
                    } else {
                        devtools.open = false;
                        devtools.orientation = null;
                    }
                }, 500);
                """;
    }

    /**
     * 获取轻量级反检测脚本
     * 
     * <p>仅包含最基础的反检测功能，性能开销更小：</p>
     * <ul>
     *   <li>修改navigator.webdriver</li>
     *   <li>基础的navigator属性模拟</li>
     * </ul>
     *
     * @return 轻量级JavaScript反检测脚本
     */
    public static String getLightStealthScript() {
        return """
                // 移除自动化标识
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined,
                    configurable: true
                });
                
                // 基础语言设置
                Object.defineProperty(navigator, 'languages', {
                    get: () => ['zh-CN', 'zh', 'en'],
                    configurable: true
                });
                
                // 基础平台信息
                Object.defineProperty(navigator, 'platform', {
                    get: () => 'Win32',
                    configurable: true
                });
                """;
    }
}