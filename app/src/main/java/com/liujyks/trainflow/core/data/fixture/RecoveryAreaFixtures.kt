package com.liujyks.trainflow.core.data.fixture

import com.liujyks.trainflow.core.model.RecoveryArea
import com.liujyks.trainflow.core.model.RecoveryBodyRegion

object RecoveryAreaFixtures {
    val areas: List<RecoveryArea> = listOf(
        RecoveryArea(
            id = "lower-body-release",
            name = "下肢放松",
            bodyRegion = RecoveryBodyRegion.LOWER,
            summary = "围绕股四头肌、臀部和小腿做温和拉伸或轻量放松，帮助训练后从下肢发力状态回到平稳状态。",
            cautionText = defaultCaution
        ),
        RecoveryArea(
            id = "posterior-chain-release",
            name = "臀腿后侧放松",
            bodyRegion = RecoveryBodyRegion.BACK,
            summary = "关注臀部、腘绳肌和髋后侧，优先用缓慢呼吸配合可控幅度，避免为了拉伸感强行压低身体。",
            cautionText = defaultCaution
        ),
        RecoveryArea(
            id = "chest-shoulder-release",
            name = "胸肩前侧放松",
            bodyRegion = RecoveryBodyRegion.FRONT,
            summary = "围绕胸部、肩前侧和上臂后侧做轻柔打开，让上肢推类训练后的紧张感逐步下降。",
            cautionText = defaultCaution
        ),
        RecoveryArea(
            id = "upper-back-release",
            name = "上背放松",
            bodyRegion = RecoveryBodyRegion.UPPER,
            summary = "关注上背、背阔肌和肩胛周围，用轻量活动和自然呼吸恢复肩背活动感。",
            cautionText = defaultCaution
        ),
        RecoveryArea(
            id = "core-breathing-reset",
            name = "核心呼吸重置",
            bodyRegion = RecoveryBodyRegion.FULL,
            summary = "通过平稳呼吸和低强度核心重置，让训练后的腹部紧张和整体节奏慢下来。",
            cautionText = defaultCaution
        )
    )

    private const val defaultCaution = "仅作为训练后放松方向，不做康复治疗或医疗诊断；如有明显疼痛或不适，应停止并寻求专业帮助。"
}
