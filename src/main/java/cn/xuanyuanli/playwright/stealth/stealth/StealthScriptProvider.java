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
                // 关键: 必须从Navigator.prototype上删除webdriver属性描述符
                // 这样 Object.getOwnPropertyDescriptor(Navigator.prototype, 'webdriver') 才会返回 undefined
                const navigatorProto = Navigator.prototype;
                if (Object.getOwnPropertyDescriptor(navigatorProto, 'webdriver')) {
                    Object.defineProperty(navigatorProto, 'webdriver', {
                        get: () => undefined,
                        configurable: true
                    });
                    // 删除属性描述符，使其完全不存在
                    delete navigatorProto.webdriver;
                }
                // 同时处理navigator实例上的属性
                if (Object.getOwnPropertyDescriptor(navigator, 'webdriver')) {
                    delete navigator.webdriver;
                }
                
                // ==================== Navigator Languages 模拟 ====================
                // 模拟真实的语言偏好
                Object.defineProperty(navigator, 'languages', {
                    get: () => ['zh-CN', 'zh', 'en-US', 'en'],
                    configurable: true
                });
                
                // ==================== Navigator Plugins 模拟 ====================
                // 模拟常见的浏览器插件，避免空插件列表被检测
                // 关键: 必须正确模拟 Plugin, MimeType, PluginArray, MimeTypeArray 的原型链
                // 使检测 navigator.plugins instanceof PluginArray 返回 true

                // 创建 MimeType 对象的辅助函数
                const makeMimeType = (data, enabledPlugin) => {
                    const mimeType = Object.create(MimeType.prototype);
                    Object.defineProperties(mimeType, {
                        type: { value: data.type, enumerable: true },
                        suffixes: { value: data.suffixes, enumerable: true },
                        description: { value: data.description, enumerable: true },
                        enabledPlugin: { value: enabledPlugin, enumerable: true }
                    });
                    return mimeType;
                };

                // 创建 Plugin 对象的辅助函数
                const makePlugin = (data) => {
                    const plugin = Object.create(Plugin.prototype);
                    Object.defineProperties(plugin, {
                        name: { value: data.name, enumerable: true },
                        filename: { value: data.filename, enumerable: true },
                        description: { value: data.description, enumerable: true },
                        length: { value: data.mimeTypes.length, enumerable: true }
                    });
                    // 添加 MimeType 索引
                    data.mimeTypes.forEach((mt, i) => {
                        const mimeType = makeMimeType(mt, plugin);
                        Object.defineProperty(plugin, i, { value: mimeType, enumerable: false });
                        Object.defineProperty(plugin, mt.type, { value: mimeType, enumerable: false });
                    });
                    // 添加 item 和 namedItem 方法
                    plugin.item = function(index) { return this[index] || null; };
                    plugin.namedItem = function(name) { return this[name] || null; };
                    return plugin;
                };

                // 插件数据定义
                const pluginsData = [
                    {
                        name: 'PDF Viewer',
                        filename: 'internal-pdf-viewer',
                        description: 'Portable Document Format',
                        mimeTypes: [
                            { type: 'application/pdf', suffixes: 'pdf', description: 'Portable Document Format' },
                            { type: 'text/pdf', suffixes: 'pdf', description: 'Portable Document Format' }
                        ]
                    },
                    {
                        name: 'Chrome PDF Viewer',
                        filename: 'internal-pdf-viewer',
                        description: 'Portable Document Format',
                        mimeTypes: [
                            { type: 'application/pdf', suffixes: 'pdf', description: 'Portable Document Format' },
                            { type: 'text/pdf', suffixes: 'pdf', description: 'Portable Document Format' }
                        ]
                    },
                    {
                        name: 'Chromium PDF Viewer',
                        filename: 'internal-pdf-viewer',
                        description: 'Portable Document Format',
                        mimeTypes: [
                            { type: 'application/pdf', suffixes: 'pdf', description: 'Portable Document Format' },
                            { type: 'text/pdf', suffixes: 'pdf', description: 'Portable Document Format' }
                        ]
                    },
                    {
                        name: 'Microsoft Edge PDF Viewer',
                        filename: 'internal-pdf-viewer',
                        description: 'Portable Document Format',
                        mimeTypes: [
                            { type: 'application/pdf', suffixes: 'pdf', description: 'Portable Document Format' },
                            { type: 'text/pdf', suffixes: 'pdf', description: 'Portable Document Format' }
                        ]
                    },
                    {
                        name: 'WebKit built-in PDF',
                        filename: 'internal-pdf-viewer',
                        description: 'Portable Document Format',
                        mimeTypes: [
                            { type: 'application/pdf', suffixes: 'pdf', description: 'Portable Document Format' },
                            { type: 'text/pdf', suffixes: 'pdf', description: 'Portable Document Format' }
                        ]
                    }
                ];

                // 创建 PluginArray
                const pluginArray = Object.create(PluginArray.prototype);
                const plugins = pluginsData.map(makePlugin);
                plugins.forEach((plugin, i) => {
                    Object.defineProperty(pluginArray, i, { value: plugin, enumerable: true });
                    Object.defineProperty(pluginArray, plugin.name, { value: plugin, enumerable: false });
                });
                Object.defineProperty(pluginArray, 'length', { value: plugins.length, enumerable: true });
                pluginArray.item = function(index) { return this[index] || null; };
                pluginArray.namedItem = function(name) { return this[name] || null; };
                pluginArray.refresh = function() {};

                // 创建 MimeTypeArray
                const mimeTypeArray = Object.create(MimeTypeArray.prototype);
                const allMimeTypes = [];
                plugins.forEach(plugin => {
                    for (let i = 0; i < plugin.length; i++) {
                        const mt = plugin[i];
                        if (!allMimeTypes.find(m => m.type === mt.type)) {
                            allMimeTypes.push(mt);
                        }
                    }
                });
                allMimeTypes.forEach((mt, i) => {
                    Object.defineProperty(mimeTypeArray, i, { value: mt, enumerable: true });
                    Object.defineProperty(mimeTypeArray, mt.type, { value: mt, enumerable: false });
                });
                Object.defineProperty(mimeTypeArray, 'length', { value: allMimeTypes.length, enumerable: true });
                mimeTypeArray.item = function(index) { return this[index] || null; };
                mimeTypeArray.namedItem = function(name) { return this[name] || null; };

                // 覆盖 navigator.plugins
                Object.defineProperty(navigator, 'plugins', {
                    get: () => pluginArray,
                    configurable: true
                });

                // 覆盖 navigator.mimeTypes
                Object.defineProperty(navigator, 'mimeTypes', {
                    get: () => mimeTypeArray,
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
                // 修复权限API查询结果,使其返回正确的Permissions对象
                const originalQuery = navigator.permissions?.query?.bind(navigator.permissions);
                if (originalQuery) {
                    navigator.permissions.query = (parameters) => (
                        originalQuery(parameters).catch(() => ({
                            state: 'prompt',
                            onchange: null
                        }))
                    );
                }
                
                // ==================== Chrome Runtime 修复 ====================
                // 移除chrome.runtime等扩展API，避免被检测
                if (typeof chrome !== 'undefined' && chrome.runtime) {
                    delete chrome.runtime;
                }
                
                // ==================== 控制台调试检测对抗 ====================
                // 防止通过控制台调试检测 - 使用低频率检测避免CPU占用
                (function() {
                    let devtools = { open: false, orientation: null };
                    let checkCount = 0;
                    const maxChecks = 10; // 最多检测10次后停止
                
                    const intervalId = setInterval(() => {
                        checkCount++;
                        if (checkCount >= maxChecks) {
                            clearInterval(intervalId); // 停止检测，避免长期占用CPU
                            return;
                        }
                
                        if (window.outerHeight - window.innerHeight > 200 ||
                            window.outerWidth - window.innerWidth > 200) {
                            devtools.open = true;
                            devtools.orientation = window.outerHeight - window.innerHeight > 200 ? 'vertical' : 'horizontal';
                        } else {
                            devtools.open = false;
                            devtools.orientation = null;
                        }
                    }, 5000); // 从500ms改为5000ms，降低检测频率
                })();
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
                // 移除自动化标识 - 必须从Navigator.prototype上删除
                const navigatorProto = Navigator.prototype;
                if (Object.getOwnPropertyDescriptor(navigatorProto, 'webdriver')) {
                    Object.defineProperty(navigatorProto, 'webdriver', {
                        get: () => undefined,
                        configurable: true
                    });
                    delete navigatorProto.webdriver;
                }
                if (Object.getOwnPropertyDescriptor(navigator, 'webdriver')) {
                    delete navigator.webdriver;
                }

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