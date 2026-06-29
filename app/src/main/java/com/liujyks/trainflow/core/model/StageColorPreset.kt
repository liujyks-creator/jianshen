package com.liujyks.trainflow.core.model

data class StageColorPreset(
    val id: String,
    val name: String,
    val hex: String,
    val tone: String,
    val recommendedUse: String,
    val textColor: String,
    val isHighAttention: Boolean,
    val isRecommended: Boolean
) {
    val accessibilityLabel: String
        get() {
            val attention = if (isHighAttention) "，高注意色" else ""
            return "$name，适合$recommendedUse$attention"
        }
}

val StageColorPresets: List<StageColorPreset> = listOf(
    StageColorPreset("warmup_teal", "蓝绿色", "#2FBF8F", "teal", "热身 / 恢复", "#FFFFFF", false, true),
    StageColorPreset("work_coral", "珊瑚橙", "#F26B4F", "coral", "工作 / 主训练", "#FFFFFF", true, true),
    StageColorPreset("rest_blue", "清爽蓝", "#65A9FF", "blue", "休息 / 缓冲", "#111820", false, true),
    StageColorPreset("cooldown_blue_grey", "蓝灰色", "#607D8B", "blue_grey", "放松 / 冷静", "#FFFFFF", false, true),
    StageColorPreset("custom_purple", "紫色", "#9C27B0", "purple", "自定义 / 个性阶段", "#FFFFFF", false, true),
    StageColorPreset("energy_orange", "橙色", "#FF9800", "orange", "运动能量", "#111820", true, true),
    StageColorPreset("red", "红色", "#F44336", "red", "工作 / 强提醒", "#FFFFFF", true, false),
    StageColorPreset("pink", "粉色", "#E91E63", "pink", "自定义 / 高醒目", "#FFFFFF", true, false),
    StageColorPreset("purple", "紫色", "#9C27B0", "purple", "自定义", "#FFFFFF", false, false),
    StageColorPreset("deep_purple", "深紫", "#673AB7", "deep_purple", "自定义 / 深色强调", "#FFFFFF", false, false),
    StageColorPreset("cyan", "青色", "#00BCD4", "cyan", "休息 / 呼吸", "#111820", false, false),
    StageColorPreset("light_blue", "亮蓝", "#03A9F4", "light_blue", "休息 / 信息", "#111820", false, false),
    StageColorPreset("blue", "蓝色", "#2196F3", "blue", "信息 / 稳定", "#FFFFFF", false, false),
    StageColorPreset("indigo", "靛蓝", "#3F51B5", "indigo", "冷静 / 深色强调", "#FFFFFF", false, false),
    StageColorPreset("teal", "蓝绿", "#009688", "teal", "恢复 / 放松", "#FFFFFF", false, false),
    StageColorPreset("green", "绿色", "#4CAF50", "green", "热身 / 正向", "#111820", false, false),
    StageColorPreset("light_green", "浅绿", "#8BC34A", "light_green", "恢复 / 轻强度", "#111820", false, false),
    StageColorPreset("lime", "柠黄绿", "#CDDC39", "lime", "提醒 / 高可见", "#111820", true, false),
    StageColorPreset("brown", "棕色", "#795548", "brown", "力量 / 低频自定义", "#FFFFFF", false, false),
    StageColorPreset("deep_orange", "深橙", "#FF5722", "deep_orange", "爆发 / 强动作", "#FFFFFF", true, false),
    StageColorPreset("material_orange", "橙色", "#FF9800", "orange", "运动能量", "#111820", true, false),
    StageColorPreset("amber", "琥珀", "#FFC107", "amber", "提醒 / 明亮", "#111820", true, false),
    StageColorPreset("grey", "灰色", "#9E9E9E", "grey", "默认 / 中性", "#111820", false, false),
    StageColorPreset("dark_grey", "深灰", "#757575", "grey", "低优先级", "#FFFFFF", false, false),
    StageColorPreset("blue_grey", "蓝灰", "#607D8B", "blue_grey", "中性 / 冷静", "#FFFFFF", false, false),
    StageColorPreset("dark_blue_grey", "深蓝灰", "#455A64", "blue_grey", "深色中性", "#FFFFFF", false, false)
)

val RecommendedStageColorPresets: List<StageColorPreset> =
    StageColorPresets.filter { preset -> preset.isRecommended }

val MoreStageColorPresets: List<StageColorPreset> =
    StageColorPresets.filterNot { preset -> preset.isRecommended }

fun stageColorPresetFor(hex: String?): StageColorPreset? {
    val normalized = hex?.normalizeStageColorHexOrNull() ?: return null
    return StageColorPresets.firstOrNull { preset -> preset.hex.equals(normalized, ignoreCase = true) }
}

fun isValidStageColorHex(hex: String?): Boolean {
    return hex.normalizeStageColorHexOrNull() != null
}

fun normalizeStageColorHex(
    hex: String?,
    fallbackStageType: TimedStageType = TimedStageType.CUSTOM
): String {
    return hex.normalizeStageColorHexOrNull() ?: fallbackStageType.defaultColorHex
}

fun stageTextColorHexFor(
    hex: String?,
    fallbackStageType: TimedStageType = TimedStageType.CUSTOM
): String {
    val normalized = normalizeStageColorHex(hex, fallbackStageType)
    return stageColorPresetFor(normalized)?.textColor ?: "#FFFFFF"
}

internal fun String?.normalizeStageColorHexOrNull(): String? {
    val value = this?.trim() ?: return null
    return value
        .takeIf { candidate -> StageColorHexRegex.matches(candidate) }
        ?.uppercase()
}

private val StageColorHexRegex = Regex("#[0-9A-Fa-f]{6}")
