package com.liujyks.trainflow.feature.recovery

import com.liujyks.trainflow.core.domain.recovery.BasicRecoveryRecommendation
import com.liujyks.trainflow.core.domain.recovery.NON_MEDICAL_BOUNDARY_TEXT
import com.liujyks.trainflow.core.model.RecoveryArea
import com.liujyks.trainflow.core.model.RecoveryBodyRegion

internal data class RecoveryScreenState(
    val title: String,
    val sessionLabel: String,
    val sourceNote: String,
    val trainedMuscleSummary: String,
    val sourceExerciseSummary: String,
    val areaItems: List<RecoveryAreaUiState>,
    val nonMedicalNotice: String,
    val emptyTitle: String,
    val emptyDescription: String
) {
    val isEmpty: Boolean
        get() = areaItems.isEmpty()
}

internal data class RecoveryAreaUiState(
    val id: String,
    val name: String,
    val bodyRegionLabel: String,
    val summary: String,
    val cautionText: String
)

internal fun BasicRecoveryRecommendation.toRecoveryScreenState(): RecoveryScreenState {
    return RecoveryScreenState(
        title = "恢复建议",
        sessionLabel = "本次训练 · ${sessionId}",
        sourceNote = "当前建议来自本次训练动作的 fixture recovery 映射和内存态 session/summary 数据；不读取 Room session records，不写入 recovery_recommendations 表。",
        trainedMuscleSummary = recommendation.trainedMuscleIds
            .map { muscleId -> muscleId.muscleLabel() }
            .takeIf { labels -> labels.isNotEmpty() }
            ?.joinToString("、", prefix = "主要训练部位：")
            ?: "本次未识别到训练部位",
        sourceExerciseSummary = sourceExerciseNames
            .takeIf { names -> names.isNotEmpty() }
            ?.joinToString("、", prefix = "来源动作：")
            ?: "暂无可用于恢复建议的动作记录",
        areaItems = recoveryAreas.map { area -> area.toUiState() },
        nonMedicalNotice = nonMedicalBoundaryText,
        emptyTitle = "暂无可生成的恢复建议",
        emptyDescription = "本次没有已完成或已确认的可识别训练动作，或动作尚未配置恢复区域映射。"
    )
}

internal fun emptyRecoveryScreenState(): RecoveryScreenState {
    return RecoveryScreenState(
        title = "恢复建议",
        sessionLabel = "暂无训练来源",
        sourceNote = "当前没有可展示的内存态恢复建议；本阶段不读取 Room session records。",
        trainedMuscleSummary = "本次未识别到训练部位",
        sourceExerciseSummary = "暂无可用于恢复建议的动作记录",
        areaItems = emptyList(),
        nonMedicalNotice = NON_MEDICAL_BOUNDARY_TEXT,
        emptyTitle = "暂无可生成的恢复建议",
        emptyDescription = "完成可识别的计时动作或确认力量训练组后，再查看基础恢复建议。"
    )
}

private fun RecoveryArea.toUiState(): RecoveryAreaUiState {
    return RecoveryAreaUiState(
        id = id,
        name = name,
        bodyRegionLabel = bodyRegion.label,
        summary = summary,
        cautionText = cautionText ?: NON_MEDICAL_BOUNDARY_TEXT
    )
}

private val RecoveryBodyRegion.label: String
    get() = when (this) {
        RecoveryBodyRegion.FRONT -> "身体前侧"
        RecoveryBodyRegion.BACK -> "身体后侧"
        RecoveryBodyRegion.UPPER -> "上肢 / 上背"
        RecoveryBodyRegion.LOWER -> "下肢"
        RecoveryBodyRegion.FULL -> "整体重置"
    }

internal fun String.muscleLabel(): String {
    return when (this) {
        "full_body" -> "全身"
        "calves" -> "小腿"
        "quads" -> "股四头肌"
        "glutes" -> "臀部"
        "hamstrings" -> "腘绳肌"
        "hip_flexors" -> "髋屈肌"
        "core" -> "核心"
        "chest" -> "胸部"
        "triceps" -> "肱三头肌"
        "shoulders" -> "肩部"
        "lats" -> "背阔肌"
        "upper_back" -> "上背"
        "biceps" -> "肱二头肌"
        else -> split('_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }
}
