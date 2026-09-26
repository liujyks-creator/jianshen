package com.liujyks.trainflow.feature.followalong

internal data class FollowAlongScreenState(
    val title: String = "跟练",
    val summary: String = "在其他设备播放视频或直播，点击开始记录本次训练。",
    val startLabel: String = "开始跟练",
    val canStartFollowAlong: Boolean = true
)

internal fun buildDefaultFollowAlongScreenState(): FollowAlongScreenState = FollowAlongScreenState()
