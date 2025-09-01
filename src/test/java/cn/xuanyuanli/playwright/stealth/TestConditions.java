package cn.xuanyuanli.playwright.stealth;

/**
 * 测试条件工具类
 * 
 * <p>提供基于环境变量和系统属性的测试条件检查</p>
 * 
 * @author xuanyuanli
 */
public class TestConditions {
    
    /**
     * 检查是否启用集成测试
     * 
     * <p>集成测试默认启用，只有明确禁用时才禁用：</p>
     * <ul>
     * <li>环境变量：DISABLE_INTEGRATION_TESTS=true</li>
     * <li>系统属性：-Ddisable.integration.tests=true</li>
     * </ul>
     * 
     * @return true如果应该启用集成测试，false否则
     */
    public static boolean isIntegrationTestsEnabled() {
        // 只有明确禁用才禁用，简化逻辑
        String disableEnv = System.getenv("DISABLE_INTEGRATION_TESTS");
        String disableProp = System.getProperty("disable.integration.tests");
        
        return !"true".equalsIgnoreCase(disableEnv) && !"true".equalsIgnoreCase(disableProp);
    }
    
    /**
     * 检查是否启用性能测试
     * 
     * <p>性能测试默认禁用，可通过以下方式启用：</p>
     * <ul>
     * <li>环境变量：ENABLE_PERFORMANCE_TESTS=true</li>
     * <li>系统属性：-Denable.performance.tests=true</li>
     * </ul>
     * 
     * @return true如果应该启用性能测试，false否则
     */
    public static boolean isPerformanceTestsEnabled() {
        String enableEnv = System.getenv("ENABLE_PERFORMANCE_TESTS");
        String enableProp = System.getProperty("enable.performance.tests");
        
        return "true".equalsIgnoreCase(enableEnv) || "true".equalsIgnoreCase(enableProp);
    }
    
    /**
     * 检查是否启用E2E测试
     * 
     * <p>E2E测试默认启用，可通过以下方式禁用：</p>
     * <ul>
     * <li>环境变量：DISABLE_E2E_TESTS=true</li>
     * <li>系统属性：-Ddisable.e2e.tests=true</li>
     * </ul>
     * 
     * @return true如果应该启用E2E测试，false否则
     */
    public static boolean isE2ETestsEnabled() {
        // 首先检查集成测试是否启用
        if (!isIntegrationTestsEnabled()) {
            return false;
        }
        
        String disableEnv = System.getenv("DISABLE_E2E_TESTS");
        String disableProp = System.getProperty("disable.e2e.tests");
        
        return !"true".equalsIgnoreCase(disableEnv) && !"true".equalsIgnoreCase(disableProp);
    }
    
}