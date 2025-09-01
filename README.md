# Playwright Stealth Pool

一个提供Playwright连接池管理和反检测功能的Java库。该项目旨在简化Playwright的使用，提供高效的资源管理，并集成反自动化检测功能。

## 🚀 核心特性

- **🔄 连接池管理**: 支持Playwright实例和Browser实例的连接池管理
- **🥷 反检测功能**: 内置JavaScript脚本绕过常见的自动化检测机制  
- **🎨 自定义脚本**: 支持注入自定义初始化脚本，灵活扩展反检测能力
- **🎭 人类行为模拟**: 智能模拟真实用户行为，包括鼠标移动、滚动、悬停等
- **⚙️ 灵活配置**: 丰富的配置选项，支持各种使用场景
- **🛡️ 健壮性**: 完善的错误处理和资源清理机制
- **📊 监控支持**: 提供连接池状态监控和统计信息

## 📦 项目结构

```
cn.xuanyuanli.playwright.stealth/
├── behavior/                # 人类行为模拟
│   ├── BehaviorIntensity.java
│   └── HumanBehaviorSimulator.java
├── config/                  # 配置管理
│   ├── PlaywrightConfig.java
│   └── StealthMode.java
├── manager/                 # 管理器
│   ├── PlaywrightManager.java
│   └── PlaywrightBrowserManager.java  
├── pool/                    # 连接池工厂
│   ├── PlaywrightFactory.java
│   └── PlaywrightBrowserFactory.java
└── stealth/                 # 反检测功能
    └── StealthScriptProvider.java
```

## 🏗️ 架构设计

### 管理器对比

| 特性 | PlaywrightManager | PlaywrightBrowserManager |
|------|------------------|-------------------------|
| 管理对象 | Playwright实例 | Browser实例 |
| 适用场景 | 短时间、一次性操作 | 频繁操作、保持会话 |
| 资源开销 | 较低 | 较高 |
| 启动速度 | 较慢（每次创建Browser） | 较快（复用Browser） |
| 推荐用途 | 批量处理、脚本任务 | Web服务、长时间运行 |

## 📖 使用指南

### 基本用法

```java
// 1. 创建配置
PlaywrightConfig config = new PlaywrightConfig()
    .setHeadless(true)
    .setStealthMode(StealthMode.FULL)
    .setDisableImageRender(true);

// 2. 创建管理器
PlaywrightManager manager = new PlaywrightManager(8);

// 3. 执行页面操作
manager.execute(config, page -> {
    page.navigate("https://example.com");
    page.waitForLoadState();
    
    // 执行人类行为模拟（可选）
    HumanBehaviorSimulator.simulate(page);
    
    System.out.println(page.title());
});

// 4. 记得关闭
manager.close();
```

### 使用Browser管理器

```java
// 创建Browser连接池管理器
PlaywrightBrowserManager browserManager = new PlaywrightBrowserManager(config, 5);

// 并发执行任务
IntStream.range(0, 20).parallel().forEach(i -> {
    browserManager.execute(page -> {
        page.navigate("https://httpbin.org/get");
        page.waitForLoadState();
        
        // 模拟人类行为（可选）
        HumanBehaviorSimulator.quickSimulate(page); // 快速模式，适合批量处理
        
        // 处理页面...
    });
});

browserManager.close();
```

### 自定义脚本增强反检测

```java
// 创建自定义反检测脚本
List<String> customScripts = Arrays.asList(
    // 隐藏webdriver属性
    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});",
    // 模拟chrome对象
    "window.chrome = {runtime: {}, loadTimes: function() {}};",
    // 自定义插件信息
    "Object.defineProperty(navigator, 'plugins', {get: () => [1,2,3]});",
    // 注入自定义标识
    "window.customStealthFlag = 'enhanced';"
);

PlaywrightConfig enhancedConfig = new PlaywrightConfig()
    .setStealthMode(StealthMode.LIGHT)      // 启用内置轻量级反检测
    .setCustomInitScripts(customScripts)    // 添加自定义脚本
    .setHeadless(true);

// 使用增强配置
manager.execute(enhancedConfig, page -> {
    // 内置反检测脚本 + 自定义脚本都会在页面加载前执行
    page.navigate("https://bot-detection-site.com");
    page.waitForLoadState();
    
    // 执行人类行为模拟增强伪装效果
    HumanBehaviorSimulator.simulate(page, BehaviorIntensity.THOROUGH);
    
    // 验证自定义脚本是否生效
    Object customFlag = page.evaluate("window.customStealthFlag");
    System.out.println("Custom script result: " + customFlag); // 输出: enhanced
});
```

### 高级配置

```java
PlaywrightConfig config = new PlaywrightConfig()
    // 浏览器设置
    .setHeadless(false)                    // 显示浏览器界面
    .setStartMaximized(true)               // 窗口最大化
    .setSlowMo(100.0)                      // 操作延迟100ms
    
    // 性能优化
    .setDisableGpu(true)                   // 禁用GPU加速
    .setDisableImageRender(true)           // 禁用图片渲染
    
    // 反检测配置
    .setStealthMode(StealthMode.FULL)      // 完整反检测模式
    .setDisableAutomationControlled(true)  // 隐藏自动化标识
    
    // 自定义脚本（可选）
    .setCustomInitScripts(customScripts)   // 注入自定义脚本
    
    // 网络配置
    .setProxy(new Proxy("http://proxy:8080"));
```

### 自定义浏览器上下文

```java
manager.execute(config, context -> {
    // 设置地理位置
    context.setGeolocation(new Geolocation(39.9042, 116.4074));
    
    // 授予权限
    context.grantPermissions(Arrays.asList("geolocation", "notifications"));
    
    // 设置额外头信息
    context.setExtraHTTPHeaders(Map.of("Custom-Header", "value"));
    
}, page -> {
    page.navigate("https://example.com");
    page.waitForLoadState();
    
    // 根据需要选择行为模拟强度
    HumanBehaviorSimulator.simulate(page, BehaviorIntensity.NORMAL);
    
    // 页面操作...
});
```

## 🥷 反检测功能

该库提供三层反检测机制：**内置反检测** + **自定义脚本扩展** + **人类行为模拟**

### 内置JavaScript指纹修复
- ✅ 隐藏 `navigator.webdriver` 属性
- ✅ 模拟真实的 `navigator.plugins` 和 `mimeTypes`
- ✅ 修复 WebGL 指纹信息
- ✅ 模拟硬件信息（CPU核心数、内存等）
- ✅ 修复 AudioContext 指纹

### 自定义脚本扩展
- ✅ 支持注入多个自定义JavaScript脚本
- ✅ 脚本按顺序执行，可覆盖或增强内置功能
- ✅ 支持复杂脚本逻辑和异步操作
- ✅ 脚本执行失败不影响页面正常加载
- ✅ 与内置反检测脚本完全兼容

### 人类行为模拟
- ✅ **精确时间控制**：基于目标时间范围执行，而非固定延迟
- ✅ **安全操作**：鼠标移动、滚动、悬停、轻微拖拽（不改变页面状态）
- ✅ **智能延迟**：模拟真实用户的不规律行为模式
- ✅ **强度级别**：`QUICK`（0.5-1.5秒）、`NORMAL`（1.5-3秒）、`THOROUGH`（3-6秒）

### 浏览器启动参数
- ✅ `--disable-blink-features=AutomationControlled`
- ✅ `--disable-gpu`（可配置）
- ✅ 自定义 User-Agent

### 使用示例

```java
// 禁用反检测（性能最佳）
PlaywrightConfig disabledConfig = new PlaywrightConfig()
    .setStealthMode(StealthMode.DISABLED)
    .setDisableAutomationControlled(false);

// 轻量级反检测（平衡性能和隐蔽性）
PlaywrightConfig lightConfig = new PlaywrightConfig()
    .setStealthMode(StealthMode.LIGHT)      // 基础反检测
    .setDisableAutomationControlled(true);   // 配合启动参数

// 完整反检测（最强隐蔽性）  
PlaywrightConfig fullConfig = new PlaywrightConfig()
    .setStealthMode(StealthMode.FULL)       // 全面反检测
    .setDisableAutomationControlled(true);

// 自定义脚本增强（最高灵活性）
List<String> enhancedScripts = Arrays.asList(
    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});",
    "window.chrome = {runtime: {}, app: {isInstalled: false}};",
    "Object.defineProperty(navigator, 'permissions', {get: () => ({query: () => Promise.resolve({state: 'granted'})})});"
);
PlaywrightConfig customConfig = new PlaywrightConfig()
    .setStealthMode(StealthMode.LIGHT)      // 基础内置反检测
    .setCustomInitScripts(enhancedScripts)  // 自定义增强脚本
    .setDisableAutomationControlled(true);

// 使用示例
manager.execute(customConfig, page -> {
    page.navigate("https://example.com");
    page.waitForLoadState();
    
    // 三层防护：内置脚本 + 自定义脚本 + 人类行为模拟
    HumanBehaviorSimulator.simulate(page, BehaviorIntensity.THOROUGH);
    
    // 页面操作...
});
```

### 🎭 人类行为模拟详细说明

#### 行为强度对比

| 强度 | 执行时间     | 适用场景 | 特点 |
|------|----------|----------|------|
| **QUICK** | 0.5-1.5秒 | 批量处理、快速验证 | 基础鼠标移动、简单滚动 |
| **NORMAL** | 1.5-3秒   | 标准场景、一般网站 | 平衡的行为组合、自然延迟 |
| **THOROUGH** | 3-6秒     | 强检测网站、高仿真需求 | 复杂行为序列、深度模拟 |

#### 使用示例

```java
// 基本使用（推荐）
page.navigate("https://example.com");
page.waitForLoadState();
HumanBehaviorSimulator.simulate(page); // 默认 NORMAL 强度

// 快速模拟（批量处理）
HumanBehaviorSimulator.quickSimulate(page); // 等同于 QUICK 强度

// 指定强度级别
HumanBehaviorSimulator.simulate(page, BehaviorIntensity.THOROUGH);

// 完整的防护方案
manager.execute(config, page -> {
    page.navigate("https://target-site.com");
    page.waitForLoadState(); // 等待页面完全加载
    
    // 执行人类行为模拟（在页面操作前）
    HumanBehaviorSimulator.simulate(page, BehaviorIntensity.NORMAL);
    
    // 正常的页面操作
    String title = page.title();
    page.locator("#content").textContent();
});
```

#### 安全保证

人类行为模拟器采用以下安全措施：

- ❌ **不执行危险操作**：无点击、按键、表单提交等可能改变页面状态的操作
- ✅ **只读行为**：仅执行鼠标移动、滚动、悬停等安全操作
- ✅ **异常处理**：所有操作都有容错机制，不会中断主流程
- ✅ **精确控制**：时间驱动的执行模式，确保在预期时间内完成

### StealthMode 详细说明

| 模式 | 性能开销 | 检测覆盖 | 主要功能 | 适用场景 |
|------|----------|----------|----------|----------|
| **DISABLED** | 无 | 无 | 不注入任何脚本 | 内部测试、性能敏感 |
| **LIGHT** | 极低 | 基础检测 | `navigator.webdriver`<br/>`navigator.languages`<br/>`navigator.platform` | 一般网站、批量任务 |
| **FULL** | 中等 | 全面检测 | 所有LIGHT功能 +<br/>插件模拟、WebGL修复<br/>AudioContext修复等 | 强检测网站、生产环境 |
| **CUSTOM** | 可控 | 自定义 | 内置功能 + 自定义脚本<br/>完全可定制的反检测策略 | 特殊需求、高级用户 |
```

## 📊 监控和调试

### 连接池状态监控

```java
// 获取连接池状态
System.out.println(manager.getPoolStatus());
// 输出: Pool Status - Active: 2, Idle: 3, Total: 5/8

// 获取详细统计（仅BrowserManager支持）
System.out.println(browserManager.getPoolStatistics());
// 输出: Browser Pool Statistics - Created: 10, Borrowed: 25, Returned: 23...
```

### 预热和清理

```java
// 预热连接池
browserManager.warmUpPool(3);

// 清理空闲连接
browserManager.evictIdleBrowsers();
```

## ⚠️ 注意事项

### 反检测脚本使用声明

本库提供的反检测功能仅供以下合法用途：
- ✅ 自动化测试和质量保证
- ✅ 网站性能监控
- ✅ 合规的数据采集
- ✅ 学习和研究目的

**请务必**：
- 🔴 遵守目标网站的服务条款和robots.txt
- 🔴 遵守相关法律法规
- 🔴 不用于恶意目的或侵犯他人权益
- 🔴 控制请求频率，避免对目标服务器造成负担

### 性能建议

1. **选择合适的管理器**：
   - 短时间任务使用 `PlaywrightManager`
   - 长时间运行使用 `PlaywrightBrowserManager`

2. **连接池大小设置**：
   - 考虑CPU核心数和内存限制
   - 建议值：核心数 × 2 到核心数 × 4

3. **配置优化**：
   - 启用 `disableImageRender` 提高页面加载速度
   - 启用 `disableGpu` 在服务器环境中提高稳定性

4. **自定义脚本使用建议**：
   - 与内置反检测配合使用，避免重复功能
   - 测试脚本稳定性，确保不影响页面正常功能
   - 根据目标网站特点定制脚本内容
   - 定期更新脚本以应对检测机制的变化

5. **人类行为模拟建议**：
   - 在页面完全加载后（`page.waitForLoadState()`）再执行行为模拟
   - 根据场景选择合适的强度：批量处理用QUICK，一般场景用NORMAL，强检测网站用THOROUGH
   - 行为模拟与反检测脚本配合使用效果最佳
   - 避免在时间敏感的操作中使用高强度模拟

### 常见问题

**Q: 为什么Browser创建很慢？**
A: 首次创建需要初始化浏览器引擎。使用 `warmUpPool()` 预热连接池。

**Q: 反检测脚本不起作用？**  
A: 确保 `stealthMode` 不是 `DISABLED`，根据网站检测强度选择 `LIGHT` 或 `FULL` 模式。

**Q: 如何调试自定义脚本？**
A: 在脚本中添加 `console.log()` 语句，通过浏览器控制台查看输出。或使用 `page.evaluate()` 验证脚本效果。

**Q: 自定义脚本执行顺序？**
A: 内置反检测脚本先执行，然后按列表顺序执行自定义脚本。后执行的脚本可以覆盖前面的设置。

**Q: 内存使用过高？**
A: 适当减小连接池大小，及时调用 `close()` 释放资源。

**Q: 人类行为模拟会影响页面内容吗？**
A: 不会。行为模拟只执行安全的只读操作（鼠标移动、滚动、悬停），不会点击或修改页面内容。

**Q: 如何选择合适的行为模拟强度？**
A: QUICK用于批量处理，NORMAL用于一般场景，THOROUGH用于强检测网站。建议从NORMAL开始测试。

**Q: 行为模拟什么时候执行？**
A: 在`page.navigate()`和`page.waitForLoadState()`之后，正式操作页面之前执行。

## 🧪 测试配置

### 测试分类和环境变量控制

项目支持多种测试运行模式，通过环境变量灵活控制：

#### 测试分类
- **单元测试**：基础功能测试，无外部依赖
- **集成测试**：需要网络连接，测试真实场景
- **E2E测试**：端到端测试，完整工作流程验证  
- **性能测试**：性能基准测试，资源使用评估

#### 环境变量控制

| 环境变量 | 测试默认状态 | 控制逻辑 | 示例 |
|---------|-------------|----------|------|
| `DISABLE_INTEGRATION_TESTS` | 启用 | 需设置 `true` 才禁用 | `DISABLE_INTEGRATION_TESTS=true` |
| `ENABLE_PERFORMANCE_TESTS` | 禁用 | 需设置 `true` 才启用 | `ENABLE_PERFORMANCE_TESTS=true` |
| `DISABLE_E2E_TESTS` | 启用 | 需设置 `true` 才禁用 | `DISABLE_E2E_TESTS=true` |

**注意**：Maven Surefire 配置默认排除 `performance` 和 `slow` 分组的测试。

### 测试配置架构

#### 统一配置原则
- **单一职责**：Maven Surefire 负责JVM级别配置，JUnit Platform 负责测试执行配置
- **避免冲突**：移除重复的并行执行配置，统一在 JUnit Platform 管理
- **配置简化**：只维护一套配置文件，减少维护负担

#### `junit-platform.properties` - 统一配置文件
JUnit Platform 官方配置文件，统一管理所有测试行为：
- ✅ 并行执行：`junit.jupiter.execution.parallel.enabled=true`
- ✅ 超时配置：使用正确的属性名（`junit.jupiter.execution.timeout.*`）
- ✅ 生命周期：`per_class`（每个类共享实例，提高性能）
- ✅ 动态线程分配：根据CPU核心数自动调整

### 推荐运行方式

#### 日常开发 - 快速单元测试
```bash
# Maven 默认配置（推荐日常开发）
mvn test
# 默认排除：performance、slow 分组
# 默认包含：单元测试、集成测试、E2E测试（非 performance/slow）
# 运行时间：约1-2分钟

# 纯单元测试（最快）
mvn test -Ddisable.integration.tests=true
# 运行时间：约30-60秒
# 包含：配置测试、工厂测试、基础功能测试
```

#### 本地完整测试
```bash
# 运行所有测试（排除 performance 和 slow 分组）
mvn test
# 运行时间：约1-2分钟
# 包含：单元测试 + 集成测试 + E2E测试（非 performance/slow）

# 包含 performance 和 slow 测试的完整测试
mvn test -DexcludedGroups=""
# 运行时间：约3-5分钟
# 包含：所有测试，包括性能测试和慢测试
```

#### CI/CD 环境
```bash
# CI环境推荐配置 - 完整测试套件（除 performance/slow）
mvn test
# 包含：单元测试 + 集成测试 + E2E测试

# CI分层运行（推荐）
mvn test -Ddisable.integration.tests=true                    # 第一阶段：快速单元测试
mvn test -Dgroups="integration"                             # 第二阶段：集成测试  
mvn test -Dgroups="e2e"                                    # 第三阶段：E2E测试
mvn test -Dgroups="performance"                           # 第四阶段：性能测试
```

#### 性能测试
```bash
# 只运行性能测试（通过分组）
mvn test -Dgroups="performance"

# 或者通过环境变量启用性能测试
ENABLE_PERFORMANCE_TESTS=true mvn test -Dgroups="performance"

# 或者运行特定的性能测试类
mvn test -Dtest="*PerformanceTest*"

# 包含：连接池性能、内存使用、并发测试
```

#### 特定测试分组
```bash
# 只运行E2E测试
mvn test -Dgroups="e2e"

# 只运行集成测试
mvn test -Dgroups="integration"

# 运行特定测试类
mvn test -Dtest="PlaywrightManagerTest"

# 只排除 slow 测试（保留 performance）
mvn test -DexcludedGroups="slow"
```

### 测试性能优化

### 测试最佳实践

1. **开发阶段**：使用 `mvn test`（默认配置）或 `mvn test -Ddisable.integration.tests=true`（纯单元测试）
2. **提交前**：运行 `mvn test -DexcludedGroups=""`（完整测试套件）
3. **CI/CD环境**：分层运行或 `mvn test`（除 performance/slow）
4. **性能测试**：定期运行 `mvn test -Dgroups="performance"`，监控性能回归

### 测试配置原理说明

#### Maven Surefire 默认配置
- **默认排除**：`performance` 和 `slow` 分组的测试（参见 pom.xml）
- **理由**：这些测试通常运行时间较长，不适合日常快速开发

#### 环境变量逻辑
- **集成测试**：默认启用，需设置 `DISABLE_INTEGRATION_TESTS=true` 才禁用
- **性能测试**：默认禁用，需设置 `ENABLE_PERFORMANCE_TESTS=true` 才启用
- **E2E测试**：默认启用，需设置 `DISABLE_E2E_TESTS=true` 才禁用

#### 灵活控制
- **Maven 参数**：通过 `-Dgroups` 和 `-DexcludedGroups` 精确控制
- **系统属性**：支持 `-Ddisable.integration.tests` 等参数
- **分层执行**：CI/CD 中可分阶段运行，提高反馈效率

## 🔗 相关链接

- [Playwright官方文档](https://playwright.dev/)
- [Apache Commons Pool2](https://commons.apache.org/proper/commons-pool/)
- [JUnit 5 用户指南](https://junit.org/junit5/docs/current/user-guide/)

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request。在贡献代码前，请确保：

1. 遵循现有代码风格
2. 添加适当的测试
3. 更新相关文档
4. 确保所有测试通过

## 📄 许可证

请查看项目根目录的 LICENSE 文件了解许可证详情。
