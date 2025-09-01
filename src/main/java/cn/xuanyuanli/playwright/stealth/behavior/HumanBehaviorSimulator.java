package cn.xuanyuanli.playwright.stealth.behavior;

import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 人类行为模拟器
 * 
 * <p>在页面加载完成后模拟真实用户的安全行为模式，通过随机的鼠标移动、滚动、悬停等操作，
 * 帮助绕过基于用户行为分析的反爬虫检测机制。</p>
 * 
 * <p><strong>核心功能：</strong></p>
 * <ul>
 *   <li>精确时间控制的行为模拟</li>
 *   <li>完全随机的动作序列</li>
 *   <li>安全的操作（不改变页面状态）</li>
 *   <li>自然的行为延迟分布</li>
 * </ul>
 * 
 * <p><strong>支持的安全动作：</strong></p>
 * <ul>
 *   <li>随机鼠标移动和轨迹</li>
 *   <li>多种滚动模式</li>
 *   <li>元素悬停（仅限只读元素）</li>
 *   <li>轻微鼠标拖拽</li>
 * </ul>
 * 
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * // 页面导航后调用
 * page.navigate("https://example.com");
 * page.waitForLoadState();
 * 
 * // 基本使用 - 正常强度模拟
 * HumanBehaviorSimulator.simulate(page);
 * 
 * // 指定强度级别
 * HumanBehaviorSimulator.simulate(page, BehaviorIntensity.THOROUGH);
 * 
 * // 快速模拟（用于批量处理）
 * HumanBehaviorSimulator.quickSimulate(page);
 * }</pre>
 * 
 * <p><strong>安全保证：</strong></p>
 * <ul>
 *   <li>不执行点击、按键等可能改变页面的操作</li>
 *   <li>所有操作都有异常处理，不会中断流程</li>
 *   <li>精确的时间控制，确保在预期时间内完成</li>
 * </ul>
 *
 * @author xuanyuanli
 */
@Slf4j
public class HumanBehaviorSimulator {

    /**
     * 安全动作枚举 - 仅包含不会改变页面状态的操作
     */
    private enum SafeAction {
        MOUSE_MOVE,      // 随机鼠标移动
        SCROLL_DOWN,     // 向下滚动  
        SCROLL_UP,       // 向上滚动
        SCROLL_TO,       // 滚动到指定位置
        HOVER_ELEMENT,   // 悬停元素（只读）
        MOUSE_TRACK,     // 鼠标轨迹移动
    }
    
    /**
     * 执行人类行为模拟（使用正常强度）
     * 
     * @param page Playwright页面对象
     */
    public static void simulate(Page page) {
        simulate(page, BehaviorIntensity.NORMAL);
    }
    
    /**
     * 执行快速人类行为模拟（用于批量处理）
     * 
     * @param page Playwright页面对象
     */
    public static void quickSimulate(Page page) {
        simulate(page, BehaviorIntensity.QUICK);
    }
    
    /**
     * 执行指定强度的人类行为模拟
     * 
     * @param page      Playwright页面对象
     * @param intensity 模拟强度级别
     */
    public static void simulate(Page page, BehaviorIntensity intensity) {
        if (page == null) {
            log.warn("Page is null, skipping behavior simulation");
            return;
        }
        
        if (intensity == null) {
            log.warn("BehaviorIntensity is null, using default NORMAL intensity");
            intensity = BehaviorIntensity.NORMAL;
        }
        
        log.debug("Starting behavior simulation with intensity: {}", intensity);
        
        long startTime = System.currentTimeMillis();
        long targetDuration = ThreadLocalRandom.current().nextLong(
            intensity.getMinDurationMs(), intensity.getMaxDurationMs() + 1);
        long endTime = startTime + targetDuration;
        
        try {
            // 时间驱动的随机行为执行
            executeTimeDrivenBehavior(page, endTime, intensity);
            
            long actualDuration = System.currentTimeMillis() - startTime;
            log.debug("Behavior simulation completed in {}ms (target: {}ms) with intensity: {}", 
                     actualDuration, targetDuration, intensity);
            
        } catch (Exception e) {
            log.error("Failed to execute behavior simulation with intensity: " + intensity, e);
        }
    }
    
    /**
     * 执行时间驱动的行为模拟
     */
    private static void executeTimeDrivenBehavior(Page page, long endTime, BehaviorIntensity intensity) {
        // 获取页面尺寸，用于后续操作
        PageDimensions dimensions = getPageDimensions(page);
        log.trace("Page dimensions: {}x{}", dimensions.width, dimensions.height);
        
        int actionCount = 0;
        while (System.currentTimeMillis() < endTime) {
            long remainingTime = endTime - System.currentTimeMillis();
            
            // 剩余时间太少，直接结束
            if (remainingTime < 50) {
                break;
            }
            
            // 随机选择一个安全动作
            SafeAction action = getRandomSafeAction();

            // 执行动作
            executeAction(page, action, dimensions);
            actionCount++;
            
            // 计算动作耗时和剩余时间
            long remainingAfterAction = endTime - System.currentTimeMillis();
            
            // 智能延迟控制
            if (remainingAfterAction > 0) {
                long delay = calculateSmartDelay(remainingAfterAction, intensity);
                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.debug("Behavior simulation interrupted");
                        break;
                    }
                }
            }
        }
        
        log.trace("Executed {} actions during behavior simulation", actionCount);
    }
    
    /**
     * 随机选择一个安全动作
     */
    private static SafeAction getRandomSafeAction() {
        SafeAction[] actions = SafeAction.values();
        return actions[ThreadLocalRandom.current().nextInt(actions.length)];
    }
    
    /**
     * 执行指定的安全动作
     */
    private static void executeAction(Page page, SafeAction action, PageDimensions dimensions) {
        try {
            switch (action) {
                case MOUSE_MOVE -> executeRandomMouseMove(page, dimensions);
                case SCROLL_DOWN -> executeScrollDown(page);
                case SCROLL_UP -> executeScrollUp(page);  
                case SCROLL_TO -> executeScrollToRandom(page);
                case HOVER_ELEMENT -> executeSafeHover(page);
                case MOUSE_TRACK -> executeMouseTrack(page, dimensions);
            }
            log.trace("Executed action: {}", action);
        } catch (Exception e) {
            log.trace("Action {} failed: {}", action, e.getMessage());
        }
    }
    
    /**
     * 执行随机鼠标移动
     */
    private static void executeRandomMouseMove(Page page, PageDimensions dimensions) {
        double x = ThreadLocalRandom.current().nextDouble(50, dimensions.width - 50);
        double y = ThreadLocalRandom.current().nextDouble(50, dimensions.height - 50);
        page.mouse().move(x, y);
    }
    
    /**
     * 执行向下滚动
     */
    private static void executeScrollDown(Page page) {
        int distance = ThreadLocalRandom.current().nextInt(100, 600);
        page.evaluate("window.scrollBy(0, " + distance + ")");
    }
    
    /**
     * 执行向上滚动
     */
    private static void executeScrollUp(Page page) {
        int distance = ThreadLocalRandom.current().nextInt(50, 300);
        page.evaluate("window.scrollBy(0, -" + distance + ")");
    }
    
    /**
     * 执行滚动到随机位置
     */
    private static void executeScrollToRandom(Page page) {
        double position = ThreadLocalRandom.current().nextDouble(0.1, 0.9);
        page.evaluate("window.scrollTo(0, document.body.scrollHeight * " + position + ")");
    }
    
    /**
     * 执行安全的元素悬停
     */
    private static void executeSafeHover(Page page) {
        // 只选择安全的、不可交互的元素
        String[] safeSelectors = {"p", "div", "span", "h1", "h2", "h3", "h4", "h5", "h6", "img"};
        String selector = safeSelectors[ThreadLocalRandom.current().nextInt(safeSelectors.length)];
        
        try {
            if (page.locator(selector).first().isVisible()) {
                page.locator(selector).first().hover();
            }
        } catch (Exception e) {
            // 悬停失败不影响页面状态，静默处理
        }
    }
    
    /**
     * 执行鼠标轨迹移动（模拟真实用户浏览轨迹）
     */
    private static void executeMouseTrack(Page page, PageDimensions dimensions) {
        double startX = ThreadLocalRandom.current().nextDouble(100, dimensions.width - 100);
        double startY = ThreadLocalRandom.current().nextDouble(100, dimensions.height - 100);
        
        // 创建自然的鼠标移动轨迹
        int trackPoints = ThreadLocalRandom.current().nextInt(2, 5);
        for (int i = 0; i < trackPoints; i++) {
            double deltaX = ThreadLocalRandom.current().nextDouble(-80, 80);
            double deltaY = ThreadLocalRandom.current().nextDouble(-50, 50);
            
            double newX = Math.max(50, Math.min(dimensions.width - 50, startX + deltaX));
            double newY = Math.max(50, Math.min(dimensions.height - 50, startY + deltaY));
            
            page.mouse().move(newX, newY);
            
            // 轨迹点之间的短暂延迟
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(20, 80));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            
            startX = newX;
            startY = newY;
        }
    }

    /**
     * 计算智能延迟时间
     */
    private static long calculateSmartDelay(long remainingTime, BehaviorIntensity intensity) {
        if (remainingTime <= 50) {
            return 0;
        }
        
        // 根据剩余时间和强度级别计算延迟
        long maxDelay = Math.min(remainingTime / 2, 800L);
        
        if (intensity.isQuick()) {
            // 快速模式：较短延迟
            return ThreadLocalRandom.current().nextLong(20, Math.min(maxDelay, 150L));
        } else if (intensity.isThorough()) {
            // 彻底模式：较长延迟，更真实
            return naturalRandomDelay(Math.max(50, Math.min(maxDelay, 600L)));
        } else {
            // 正常模式：平衡的延迟
            return naturalRandomDelay(Math.max(30, Math.min(maxDelay, 400L)));
        }
    }
    
    /**
     * 自然随机延迟（模拟真实用户的不规律行为）
     */
    private static long naturalRandomDelay(long maxDelay) {
        // 模拟真实人类的延迟分布：大部分短延迟，偶尔长延迟
        double random = ThreadLocalRandom.current().nextDouble();
        
        if (random < 0.6) {
            // 60% 概率：快速操作
            return ThreadLocalRandom.current().nextLong(30, Math.max(31, maxDelay / 3));
        } else if (random < 0.9) {
            // 30% 概率：正常速度
            return ThreadLocalRandom.current().nextLong(maxDelay / 3, Math.max(maxDelay / 3 + 1, maxDelay * 2 / 3));
        } else {
            // 10% 概率：慢速操作（思考时间）
            return ThreadLocalRandom.current().nextLong(maxDelay * 2 / 3, maxDelay);
        }
    }
    
    /**
     * 获取页面尺寸信息
     */
    private static PageDimensions getPageDimensions(Page page) {
        try {
            Object result = page.evaluate("() => ({width: window.innerWidth, height: window.innerHeight})");
            
            if (result instanceof java.util.Map) {
                java.util.Map<String, Object> dimensions = (java.util.Map<String, Object>) result;
                
                int width = ((Number) dimensions.get("width")).intValue();
                int height = ((Number) dimensions.get("height")).intValue();
                
                // 确保最小尺寸，避免操作异常
                return new PageDimensions(Math.max(width, 800), Math.max(height, 600));
            }
        } catch (Exception e) {
            log.debug("Failed to get page dimensions, using defaults", e);
        }
        
        // 返回默认尺寸
        return new PageDimensions(1280, 720);
    }
    
    /**
     * 页面尺寸信息内部类
     */
    private static class PageDimensions {
        final int width;
        final int height;
        
        PageDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}