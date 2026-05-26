package com.liujyks.trainflow.core.model

data class RecoveryArea(
    val id: String,
    val name: String,
    val bodyRegion: RecoveryBodyRegion,
    val summary: String,
    val media: List<MediaAssetRef> = emptyList(),
    val cautionText: String? = null
)

enum class RecoveryBodyRegion(val contractValue: String) {
    FRONT("front"),
    BACK("back"),
    UPPER("upper"),
    LOWER("lower"),
    FULL("full")
}

data class RecoveryRecommendation(
    val sessionId: String,
    val trainedMuscleIds: List<String>,
    val areaIds: List<String>,
    val contentIds: List<String> = emptyList()
)
