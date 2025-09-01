package cn.xuanyuanli.playwright.stealth.behavior;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

/**
 * BehaviorIntensity枚举测试
 *
 * @author xuanyuanli
 */
@DisplayName("行为强度枚举测试")
class BehaviorIntensityTest {

    @Test
    @DisplayName("应该包含3个预期的强度级别")
    void shouldContainExpectedIntensityLevels() {
        BehaviorIntensity[] intensities = BehaviorIntensity.values();
        
        assertThat(intensities).hasSize(3);
        assertThat(intensities).containsExactly(
            BehaviorIntensity.QUICK,
            BehaviorIntensity.NORMAL,
            BehaviorIntensity.THOROUGH
        );
    }

    @ParameterizedTest
    @EnumSource(BehaviorIntensity.class)
    @DisplayName("每个强度级别应该有有效的配置参数")
    void eachIntensityShouldHaveValidConfiguration(BehaviorIntensity intensity) {
        // 验证基本属性
        assertThat(intensity.getDescription()).isNotNull().isNotEmpty();
        
        // 验证时间配置
        assertThat(intensity.getMinDurationMs()).isPositive();
        assertThat(intensity.getMaxDurationMs()).isGreaterThan(intensity.getMinDurationMs());
    }

    @Test
    @DisplayName("强度级别应该按时间递增")
    void intensityLevelsShouldIncreaseByTime() {
        BehaviorIntensity[] intensities = BehaviorIntensity.values();
        
        for (int i = 1; i < intensities.length; i++) {
            BehaviorIntensity current = intensities[i];
            BehaviorIntensity previous = intensities[i - 1];
            
            // 验证时间范围递增
            assertThat(current.getMinDurationMs()).isGreaterThanOrEqualTo(previous.getMinDurationMs());
            assertThat(current.getMaxDurationMs()).isGreaterThan(previous.getMaxDurationMs());
        }
    }

    @Test
    @DisplayName("QUICK应该被识别为快速强度")
    void quickShouldBeIdentifiedAsQuickIntensity() {
        assertThat(BehaviorIntensity.QUICK.isQuick()).isTrue();
        assertThat(BehaviorIntensity.NORMAL.isQuick()).isFalse();
        assertThat(BehaviorIntensity.THOROUGH.isQuick()).isFalse();
    }

    @Test
    @DisplayName("THOROUGH应该被识别为彻底强度")
    void thoroughShouldBeIdentifiedAsThoroughIntensity() {
        assertThat(BehaviorIntensity.QUICK.isThorough()).isFalse();
        assertThat(BehaviorIntensity.NORMAL.isThorough()).isFalse();
        assertThat(BehaviorIntensity.THOROUGH.isThorough()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(BehaviorIntensity.class)
    @DisplayName("toString方法应该返回有意义的描述")
    void toStringShouldReturnMeaningfulDescription(BehaviorIntensity intensity) {
        String description = intensity.toString();
        
        assertThat(description).isNotNull().isNotEmpty();
        assertThat(description).contains(intensity.name());
        assertThat(description).contains(intensity.getDescription());
        assertThat(description).contains(intensity.getMinDurationMs() + "-" + intensity.getMaxDurationMs() + "ms");
    }

    @Test
    @DisplayName("验证QUICK强度的具体配置")
    void shouldValidateQuickIntensityConfiguration() {
        BehaviorIntensity quick = BehaviorIntensity.QUICK;
        
        assertThat(quick.getDescription()).isEqualTo("快速强度");
        assertThat(quick.getMinDurationMs()).isEqualTo(500);
        assertThat(quick.getMaxDurationMs()).isEqualTo(1000);
    }

    @Test
    @DisplayName("验证NORMAL强度的具体配置")
    void shouldValidateNormalIntensityConfiguration() {
        BehaviorIntensity normal = BehaviorIntensity.NORMAL;
        
        assertThat(normal.getDescription()).isEqualTo("正常强度");
        assertThat(normal.getMinDurationMs()).isEqualTo(1500);
        assertThat(normal.getMaxDurationMs()).isEqualTo(3000);
    }

    @Test
    @DisplayName("验证THOROUGH强度的具体配置")
    void shouldValidateThoroughIntensityConfiguration() {
        BehaviorIntensity thorough = BehaviorIntensity.THOROUGH;
        
        assertThat(thorough.getDescription()).isEqualTo("彻底强度");
        assertThat(thorough.getMinDurationMs()).isEqualTo(3000);
        assertThat(thorough.getMaxDurationMs()).isEqualTo(6000);
    }

    @Test
    @DisplayName("时间范围应该覆盖不同的使用场景")
    void timeRangesShouldCoverDifferentUseCase() {
        // QUICK：适合批量处理，0.5-1秒
        assertThat(BehaviorIntensity.QUICK.getMaxDurationMs()).isLessThanOrEqualTo(1000);
        
        // NORMAL：标准场景，1.5-3秒
        assertThat(BehaviorIntensity.NORMAL.getMinDurationMs()).isGreaterThanOrEqualTo(1500);
        assertThat(BehaviorIntensity.NORMAL.getMaxDurationMs()).isLessThanOrEqualTo(3000);
        
        // THOROUGH：高仿真需求，3-6秒
        assertThat(BehaviorIntensity.THOROUGH.getMinDurationMs()).isGreaterThanOrEqualTo(3000);
        assertThat(BehaviorIntensity.THOROUGH.getMaxDurationMs()).isLessThanOrEqualTo(6000);
    }

    @Test
    @DisplayName("强度级别之间不应该有时间重叠")
    void intensityLevelsShouldNotHaveTimeOverlap() {
        // QUICK的最大值应该小于NORMAL的最小值，或者最多相等
        assertThat(BehaviorIntensity.QUICK.getMaxDurationMs())
            .isLessThanOrEqualTo(BehaviorIntensity.NORMAL.getMinDurationMs());
        
        // NORMAL的最大值应该小于或等于THOROUGH的最小值
        assertThat(BehaviorIntensity.NORMAL.getMaxDurationMs())
            .isLessThanOrEqualTo(BehaviorIntensity.THOROUGH.getMinDurationMs());
    }
}