package cn.xuanyuanli.playwright.stealth.pool;

/**
 * 池化实例是否应被淘汰的决策结果。
 */
public record RetireDecision(boolean shouldRetire, RetireReason reason, String detail) {

    public static RetireDecision keep() {
        return new RetireDecision(false, null, null);
    }

    public static RetireDecision retire(RetireReason reason, String detail) {
        return new RetireDecision(true, reason, detail);
    }
}
