package cn.xuanyuanli.playwright.stealth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * StealthMode枚举单元测试
 *
 * <p>测试StealthMode枚举的各项功能，包括：</p>
 * <ul>
 *   <li>枚举值验证</li>
 *   <li>状态检查方法</li>
 *   <li>性能级别验证</li>
 *   <li>描述信息验证</li>
 * </ul>
 *
 * @author xuanyuanli
 */
@DisplayName("StealthMode 反检测模式枚举测试")
class StealthModeTest {

    @Nested
    @DisplayName("枚举值验证测试")
    class EnumValueTests {

        @Test
        @DisplayName("应该包含所有预期的枚举值")
        void shouldContainAllExpectedValues() {
            StealthMode[] values = StealthMode.values();
            
            assertThat(values).hasSize(3);
            assertThat(values).containsExactlyInAnyOrder(
                    StealthMode.DISABLED,
                    StealthMode.LIGHT,
                    StealthMode.FULL
            );
        }

        @Test
        @DisplayName("valueOf方法应该正确工作")
        void shouldWorkWithValueOf() {
            assertThat(StealthMode.valueOf("DISABLED")).isEqualTo(StealthMode.DISABLED);
            assertThat(StealthMode.valueOf("LIGHT")).isEqualTo(StealthMode.LIGHT);
            assertThat(StealthMode.valueOf("FULL")).isEqualTo(StealthMode.FULL);
        }

        @Test
        @DisplayName("valueOf方法对于无效值应该抛出异常")
        void shouldThrowExceptionForInvalidValueOf() {
            assertThatThrownBy(() -> StealthMode.valueOf("INVALID"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("描述信息测试")
    class DescriptionTests {

        @Test
        @DisplayName("DISABLED模式应该有正确的描述")
        void shouldHaveCorrectDescriptionForDisabled() {
            assertThat(StealthMode.DISABLED.getDescription()).isEqualTo("禁用反检测");
            assertThat(StealthMode.DISABLED.getPerformanceLevel()).isEqualTo(0);
        }

        @Test
        @DisplayName("LIGHT模式应该有正确的描述")
        void shouldHaveCorrectDescriptionForLight() {
            assertThat(StealthMode.LIGHT.getDescription()).isEqualTo("轻量级反检测");
            assertThat(StealthMode.LIGHT.getPerformanceLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("FULL模式应该有正确的描述")
        void shouldHaveCorrectDescriptionForFull() {
            assertThat(StealthMode.FULL.getDescription()).isEqualTo("完整反检测");
            assertThat(StealthMode.FULL.getPerformanceLevel()).isEqualTo(2);
        }

        @ParameterizedTest
        @EnumSource(StealthMode.class)
        @DisplayName("所有模式都应该有非空描述")
        void shouldHaveNonEmptyDescription(StealthMode mode) {
            assertThat(mode.getDescription()).isNotNull();
            assertThat(mode.getDescription()).isNotEmpty();
            assertThat(mode.getDescription()).doesNotContainOnlyWhitespaces();
        }
    }

    @Nested
    @DisplayName("性能级别测试")
    class PerformanceLevelTests {

        @Test
        @DisplayName("性能级别应该按预期顺序递增")
        void shouldHaveIncreasingPerformanceLevels() {
            assertThat(StealthMode.DISABLED.getPerformanceLevel())
                    .isLessThan(StealthMode.LIGHT.getPerformanceLevel());
            
            assertThat(StealthMode.LIGHT.getPerformanceLevel())
                    .isLessThan(StealthMode.FULL.getPerformanceLevel());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2})
        @DisplayName("应该包含所有预期的性能级别")
        void shouldContainAllExpectedPerformanceLevels(int level) {
            boolean found = false;
            for (StealthMode mode : StealthMode.values()) {
                if (mode.getPerformanceLevel() == level) {
                    found = true;
                    break;
                }
            }
            assertThat(found).isTrue();
        }

        @ParameterizedTest
        @EnumSource(StealthMode.class)
        @DisplayName("所有模式的性能级别都应该在有效范围内")
        void shouldHaveValidPerformanceLevel(StealthMode mode) {
            assertThat(mode.getPerformanceLevel()).isBetween(0, 10);
        }
    }

    @Nested
    @DisplayName("状态检查方法测试")
    class StatusCheckMethodTests {

        @Test
        @DisplayName("isEnabled方法应该正确工作")
        void shouldCorrectlyCheckEnabled() {
            assertThat(StealthMode.DISABLED.isEnabled()).isFalse();
            assertThat(StealthMode.LIGHT.isEnabled()).isTrue();
            assertThat(StealthMode.FULL.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("isLight方法应该正确工作")
        void shouldCorrectlyCheckLight() {
            assertThat(StealthMode.DISABLED.isLight()).isFalse();
            assertThat(StealthMode.LIGHT.isLight()).isTrue();
            assertThat(StealthMode.FULL.isLight()).isFalse();
        }

        @Test
        @DisplayName("isFull方法应该正确工作")
        void shouldCorrectlyCheckFull() {
            assertThat(StealthMode.DISABLED.isFull()).isFalse();
            assertThat(StealthMode.LIGHT.isFull()).isFalse();
            assertThat(StealthMode.FULL.isFull()).isTrue();
        }
    }

    @Nested
    @DisplayName("toString方法测试")
    class ToStringTests {

        @Test
        @DisplayName("toString应该包含所有关键信息")
        void shouldContainAllKeyInformation() {
            String disabledStr = StealthMode.DISABLED.toString();
            String lightStr = StealthMode.LIGHT.toString();
            String fullStr = StealthMode.FULL.toString();

            // 验证包含枚举名称
            assertThat(disabledStr).contains("DISABLED");
            assertThat(lightStr).contains("LIGHT");
            assertThat(fullStr).contains("FULL");

            // 验证包含描述信息
            assertThat(disabledStr).contains("禁用反检测");
            assertThat(lightStr).contains("轻量级反检测");
            assertThat(fullStr).contains("完整反检测");

            // 验证包含性能级别
            assertThat(disabledStr).contains("0");
            assertThat(lightStr).contains("1");
            assertThat(fullStr).contains("2");
        }

        @ParameterizedTest
        @EnumSource(StealthMode.class)
        @DisplayName("toString应该返回格式化的字符串")
        void shouldReturnFormattedString(StealthMode mode) {
            String result = mode.toString();
            
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
            // 验证基本格式：枚举名(描述, 性能开销: 数字)
            assertThat(result).matches("\\w+\\([^)]+, 性能开销: \\d+\\)");
        }
    }

    @Nested
    @DisplayName("业务逻辑测试")
    class BusinessLogicTests {

        @Test
        @DisplayName("应该正确区分启用和禁用的模式")
        void shouldCorrectlyDistinguishEnabledDisabled() {
            // 只有DISABLED模式是禁用的
            long disabledCount = java.util.Arrays.stream(StealthMode.values())
                    .filter(mode -> !mode.isEnabled())
                    .count();
            assertThat(disabledCount).isEqualTo(1);

            // 其他模式都是启用的
            long enabledCount = java.util.Arrays.stream(StealthMode.values())
                    .filter(StealthMode::isEnabled)
                    .count();
            assertThat(enabledCount).isEqualTo(2);
        }

        @Test
        @DisplayName("应该有唯一的轻量级模式")
        void shouldHaveUniqueLightMode() {
            long lightCount = java.util.Arrays.stream(StealthMode.values())
                    .filter(StealthMode::isLight)
                    .count();
            assertThat(lightCount).isEqualTo(1);
        }

        @Test
        @DisplayName("应该有唯一的完整模式")
        void shouldHaveUniqueFullMode() {
            long fullCount = java.util.Arrays.stream(StealthMode.values())
                    .filter(StealthMode::isFull)
                    .count();
            assertThat(fullCount).isEqualTo(1);
        }

        @Test
        @DisplayName("模式的互斥性验证")
        void shouldBeMutuallyExclusive() {
            for (StealthMode mode : StealthMode.values()) {
                // 每个模式只能匹配一种状态检查
                int trueCount = 0;
                if (mode.isLight()) trueCount++;
                if (mode.isFull()) trueCount++;
                if (!mode.isEnabled()) trueCount++;
                
                // DISABLED模式: !isEnabled() = true, 其他为false
                // LIGHT模式: isLight() = true, 其他为false  
                // FULL模式: isFull() = true, 其他为false
                assertThat(trueCount).isEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("枚举比较测试")
    class EnumComparisonTests {

        @Test
        @DisplayName("应该支持标准的枚举比较")
        void shouldSupportStandardEnumComparison() {
            assertThat(StealthMode.DISABLED).isEqualTo(StealthMode.DISABLED);
            assertThat(StealthMode.LIGHT).isEqualTo(StealthMode.LIGHT);
            assertThat(StealthMode.FULL).isEqualTo(StealthMode.FULL);

            assertThat(StealthMode.DISABLED).isNotEqualTo(StealthMode.LIGHT);
            assertThat(StealthMode.LIGHT).isNotEqualTo(StealthMode.FULL);
        }

        @Test
        @DisplayName("应该支持ordinal排序")
        void shouldSupportOrdinalOrdering() {
            assertThat(StealthMode.DISABLED.ordinal()).isLessThan(StealthMode.LIGHT.ordinal());
            assertThat(StealthMode.LIGHT.ordinal()).isLessThan(StealthMode.FULL.ordinal());
        }

        @Test
        @DisplayName("name方法应该返回枚举名称")
        void shouldReturnEnumName() {
            assertThat(StealthMode.DISABLED.name()).isEqualTo("DISABLED");
            assertThat(StealthMode.LIGHT.name()).isEqualTo("LIGHT");
            assertThat(StealthMode.FULL.name()).isEqualTo("FULL");
        }
    }

    @Nested
    @DisplayName("实际使用场景测试")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("应该支持根据性能要求选择模式")
        void shouldSupportPerformanceBasedSelection() {
            // 性能敏感场景：选择最低开销
            StealthMode performanceMode = java.util.Arrays.stream(StealthMode.values())
                    .min(java.util.Comparator.comparingInt(StealthMode::getPerformanceLevel))
                    .orElse(null);
            assertThat(performanceMode).isEqualTo(StealthMode.DISABLED);

            // 安全优先场景：选择最高防护
            StealthMode securityMode = java.util.Arrays.stream(StealthMode.values())
                    .max(java.util.Comparator.comparingInt(StealthMode::getPerformanceLevel))
                    .orElse(null);
            assertThat(securityMode).isEqualTo(StealthMode.FULL);
        }

        @Test
        @DisplayName("应该支持基于功能需求的选择")
        void shouldSupportFeatureBasedSelection() {
            // 需要反检测功能的场景
            long stealthEnabledCount = java.util.Arrays.stream(StealthMode.values())
                    .filter(StealthMode::isEnabled)
                    .count();
            assertThat(stealthEnabledCount).isGreaterThan(0);

            // 不需要反检测的测试场景
            boolean hasDisabledMode = java.util.Arrays.stream(StealthMode.values())
                    .anyMatch(mode -> !mode.isEnabled());
            assertThat(hasDisabledMode).isTrue();
        }

        @Test
        @DisplayName("应该支持渐进式配置")
        void shouldSupportProgressiveConfiguration() {
            // 验证性能级别的渐进性
            StealthMode[] modes = {StealthMode.DISABLED, StealthMode.LIGHT, StealthMode.FULL};
            
            for (int i = 1; i < modes.length; i++) {
                assertThat(modes[i].getPerformanceLevel())
                        .isGreaterThan(modes[i-1].getPerformanceLevel());
            }
        }
    }
}