package cn.xuanyuanli.playwright.stealth.pool;

/**
 * 池化实例被淘汰的原因。
 */
public enum RetireReason {
    MAX_BORROW_COUNT,
    MAX_LIFETIME,
    RESOURCE_PRESSURE,
    VALIDATION_FAILED,
    MANUAL
}
