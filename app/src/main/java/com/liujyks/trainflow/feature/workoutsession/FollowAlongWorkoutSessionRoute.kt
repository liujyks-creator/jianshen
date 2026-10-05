package com.liujyks.trainflow.feature.workoutsession

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liujyks.trainflow.core.data.PlanSnapshotStorageV1Validator
import com.liujyks.trainflow.core.data.PreparedPlanSnapshotStorageV1Result
import com.liujyks.trainflow.core.data.RecorderPhaseInput
import com.liujyks.trainflow.core.data.RecorderTerminalInput
import com.liujyks.trainflow.core.data.RecorderTerminalKind
import com.liujyks.trainflow.core.data.RecorderTerminalSubmission
import com.liujyks.trainflow.core.data.WorkoutSessionHistoricalResult
import com.liujyks.trainflow.core.data.WorkoutSessionRepository
import com.liujyks.trainflow.core.data.WorkoutSessionTimelineRecorder
import com.liujyks.trainflow.core.data.resolveWorkoutSessionHistorical
import com.liujyks.trainflow.feature.history.WorkoutSessionHeartRateCard
import com.liujyks.trainflow.feature.history.WorkoutSessionHeartRateCardUiState
import com.liujyks.trainflow.feature.history.buildWorkoutSessionHeartRateCardUiState
import com.liujyks.trainflow.core.data.toStorageJson
import com.liujyks.trainflow.core.database.entity.HeartRateRecordingEntity
import com.liujyks.trainflow.core.database.entity.WorkoutSessionEntity
import com.liujyks.trainflow.core.health.HeartRateRuntimeOwner
import com.liujyks.trainflow.core.model.SessionStatus
import com.liujyks.trainflow.core.model.WorkoutMode
import com.liujyks.trainflow.core.notifications.ActiveWorkoutNotificationController
import com.liujyks.trainflow.core.notifications.ActiveWorkoutNotificationProducer
import com.liujyks.trainflow.feature.settings.HeartRateSettingsUiState
import com.liujyks.trainflow.ui.designsystem.currentCardCorner
import com.liujyks.trainflow.ui.designsystem.currentPageHorizontalPadding
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin
import com.liujyks.trainflow.ui.theme.TrainFlowAccent
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral100
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral200
import com.liujyks.trainflow.ui.theme.TrainFlowTheme
import com.liujyks.trainflow.ui.theme.isBigType
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
internal fun FollowAlongWorkoutSessionRoute(
    activeWorkoutNotifications: ActiveWorkoutNotificationController,
    workoutSessionRepository: WorkoutSessionRepository,
    heartRateRuntimeOwner: HeartRateRuntimeOwner,
    heartRateSettings: HeartRateSettingsUiState,
    onBackToFollowAlong: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionId = remember { "follow-along-${System.currentTimeMillis()}" }
    val scope = rememberCoroutineScope()
    val snapshotJson = remember(sessionId) { freeFollowAlongSnapshot().toStorageJson() }
    val snapshot = remember(sessionId) {
        (PlanSnapshotStorageV1Validator.prepare(snapshotJson, WorkoutMode.FOLLOW_ALONG) as
            PreparedPlanSnapshotStorageV1Result.Valid).prepared
    }
    val initialPhase = remember(sessionId) { freeFollowAlongPhase(snapshot) }
    val metadataJson = remember(sessionId) {
        JSONObject().put("displayMetadataContractVersion", 1).put("entries", JSONArray()).toString()
    }
    var startedAt by remember(sessionId) { mutableStateOf<Instant?>(null) }
    var startClock by remember(sessionId) { mutableStateOf<Long?>(null) }
    var recorder by remember(sessionId) { mutableStateOf<WorkoutSessionTimelineRecorder?>(null) }
    var notificationProducer by remember(sessionId) { mutableStateOf<ActiveWorkoutNotificationProducer?>(null) }
    var notificationVersion by remember(sessionId) { mutableStateOf(0L) }
    var active by remember(sessionId) { mutableStateOf(false) }
    var stopping by remember(sessionId) { mutableStateOf(false) }
    var elapsedSec by remember(sessionId) { mutableStateOf(0) }
    var saved by remember(sessionId) { mutableStateOf(false) }
    var persistedSummary by remember(sessionId) { mutableStateOf<FollowAlongPersistedSummary?>(null) }
    var initializationFailure by remember(sessionId) { mutableStateOf<Throwable?>(null) }
    var saveFailure by remember(sessionId) { mutableStateOf<Throwable?>(null) }
    var recapReadFailure by remember(sessionId) { mutableStateOf<Throwable?>(null) }
    var recapSource by remember(sessionId) { mutableStateOf<WorkoutSessionHistoricalResult?>(null) }
    var endConfirmation by remember(sessionId) { mutableStateOf(WorkoutEndConfirmationUiState()) }
    fun submitNotification(status: SessionStatus) {
        notificationProducer?.let { producer ->
            notificationVersion += 1
            activeWorkoutNotifications.update(
                producer,
                notificationVersion,
                followAlongActiveWorkoutNotificationState(
                    sessionId = sessionId,
                    status = status,
                    uiState = buildFollowAlongWorkoutSessionUiState(
                        elapsedSec, active, stopping, saved, persistedSummary,
                        saveFailure, recapReadFailure, initializationFailure
                    )
                )
            )
        }
    }

    LaunchedEffect(sessionId) {
        notificationProducer = activeWorkoutNotifications.beginSession(sessionId)
        val actualStart = Instant.now()
        startedAt = actualStart
        val startTime = actualStart.atZone(ZoneId.systemDefault())
        val maxBpm = heartRateSettings.personalMaxHeartRateBpm ?: heartRateSettings.ageYears?.let { 220 - it }
        val maxSource = when {
            heartRateSettings.personalMaxHeartRateBpm != null -> "personal_max"
            heartRateSettings.ageYears != null -> "age_220_minus_age"
            else -> null
        }
        val zoneSnapshot = maxBpm?.let {
            val bounds = listOf(null to 5000, 5000 to 6000, 6000 to 7000,
                7000 to 8000, 8000 to 9000, 9000 to null)
            val names = listOf("below_50", "from_50_to_60", "from_60_to_70",
                "from_70_to_80", "from_80_to_90", "at_or_above_90")
            JSONObject().put("zoneSnapshotContractVersion", 1).put("unit", "bpm")
                .put("effectiveMaxBpm", it).put("effectiveMaxSource", maxSource)
                .put("zones", JSONArray(bounds.mapIndexed { index, (lower, upper) ->
                    JSONObject().put("zoneId", names[index])
                        .put("lowerBoundBasisPointsInclusive", lower ?: JSONObject.NULL)
                        .put("upperBoundBasisPointsExclusive", upper ?: JSONObject.NULL)
                })).toString()
        }
        try {
            val acquired = WorkoutSessionTimelineRecorder.admitAndBind(
                repository = workoutSessionRepository,
                runtime = heartRateRuntimeOwner,
                scope = scope,
                entryId = sessionId,
                session = WorkoutSessionEntity(
                    id = sessionId, planId = null, mode = "follow_along", status = "active",
                    planSnapshotJson = snapshotJson, startedAt = actualStart.toString(),
                    timelineVersion = 1, lastDurableOffsetMs = 0, lastMutationSequence = 0,
                    displayMetadataContractVersion = 1, sessionDisplayMetadataJson = metadataJson,
                    startLocalDate = startTime.toLocalDate().toString(), startZoneId = startTime.zone.id,
                    startUtcOffsetSeconds = startTime.offset.totalSeconds.toLong(),
                    timeMetadataSourceContractVersion = 1
                ),
                initialPhase = initialPhase,
                initialRecording = if (heartRateSettings.enabled) HeartRateRecordingEntity(
                    recordingId = "$sessionId:heart-rate", sessionId = sessionId, status = "active",
                    startedOffsetMs = 0, startedMutationSequence = 0,
                    endedOffsetMs = null, endedMutationSequence = null,
                    sourceContractVersion = 1, sourceKind = "ble_hrs", acquisitionContractVersion = 1,
                    parameterSnapshotVersion = 1, age = heartRateSettings.ageYears,
                    personalMaxBpm = heartRateSettings.personalMaxHeartRateBpm, effectiveMaxBpm = maxBpm,
                    effectiveMaxSource = maxSource, alertThresholdBpm = heartRateSettings.alertThresholdBpm,
                    zoneSnapshotJson = zoneSnapshot
                ) else null
            )
            recorder = acquired
            acquired.freezeStart().await()
            startClock = SystemClock.elapsedRealtime()
            active = true
            submitNotification(SessionStatus.ACTIVE)
        } catch (cause: CancellationException) {
            notificationProducer?.let(activeWorkoutNotifications::release)
            throw cause
        } catch (cause: Throwable) {
            notificationProducer?.let(activeWorkoutNotifications::release)
            initializationFailure = cause
        }
    }

    LaunchedEffect(active, stopping) {
        val clock = startClock
        if (active && !stopping && clock != null) {
            while (true) {
                delay(1000)
                elapsedSec = ((SystemClock.elapsedRealtime() - clock) / 1000).toInt()
                submitNotification(SessionStatus.ACTIVE)
            }
        }
    }

    suspend fun readSavedRecap() {
        recapSource = null
        recapReadFailure = null
        persistedSummary = null
        try {
            val historical = resolveWorkoutSessionHistorical(
                workoutSessionRepository.readSessionStrict(sessionId), "zh-CN")
            recapSource = historical
            val resolved = historical as? WorkoutSessionHistoricalResult.Resolved
                ?: error("Saved follow-along session $sessionId cannot be resolved: $historical")
            persistedSummary = resolved.toPersistedFreeFollowAlongSummary()
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            recapReadFailure = cause
        }
    }

    fun stopConfirmedSession() {
        val cut = SystemClock.elapsedRealtime()
        val end = Instant.now()
        val duration = freeFollowAlongElapsedSeconds(requireNotNull(startedAt), end)
        elapsedSec = duration
        active = false
        stopping = true
        submitNotification(SessionStatus.COMPLETED)
        val submission = requireNotNull(recorder).freezeTerminal(RecorderTerminalInput(
            elapsedRealtimeMs = cut,
            kind = RecorderTerminalKind.COMPLETED,
            endedAt = end.toString(),
            totalElapsedSec = duration,
            effectiveElapsedSec = duration,
            pausedElapsedSec = 0,
            sessionDisplayMetadataJson = metadataJson,
            stepRecords = emptyList(),
            restExtensions = emptyList(),
            strengthSets = emptyList(),
            snapshotCreatedAt = end.toString()
        ))
        when (submission) {
            is RecorderTerminalSubmission.Closed -> saveFailure = requireNotNull(submission.originalCause)
            is RecorderTerminalSubmission.Accepted -> {
                scope.launch {
                    try {
                        submission.operation.saved.await()
                        saved = true
                        readSavedRecap()
                    } catch (cause: CancellationException) {
                        throw cause
                    } catch (cause: Throwable) {
                        saveFailure = cause
                    }
                }
                scope.launch {
                    try {
                        submission.operation.released.await()
                    } catch (cause: CancellationException) {
                        throw cause
                    } catch (cause: Throwable) {
                        android.util.Log.e("FollowAlongSession", "Recorder release failed", cause)
                    }
                }
            }
        }
    }

    val uiState = buildFollowAlongWorkoutSessionUiState(
        elapsedSec, active, stopping, saved, persistedSummary,
        saveFailure, recapReadFailure, initializationFailure
    )
    BackHandler {
        when {
            uiState.canReturn -> onBackToFollowAlong()
            uiState.canStop -> endConfirmation = endConfirmation.request(true)
        }
    }
    DisposableEffect(recorder) {
        val currentRecorder = recorder
        onDispose { currentRecorder?.clear() }
    }
    DisposableEffect(activeWorkoutNotifications, sessionId) {
        onDispose { notificationProducer?.let(activeWorkoutNotifications::release) }
    }

    FollowAlongWorkoutSessionScreen(
        uiState = uiState,
        showEndConfirmation = endConfirmation.visible,
        onRequestEnd = { endConfirmation = endConfirmation.request(uiState.canStop) },
        onCancelEnd = { endConfirmation = endConfirmation.cancel() },
        onConfirmEnd = {
            val result = endConfirmation.confirm(uiState.canStop)
            endConfirmation = result.nextState
            if (result.command != null) stopConfirmedSession()
        },
        onBackToFollowAlong = onBackToFollowAlong,
        heartRateCard = buildWorkoutSessionHeartRateCardUiState(recapSource, recapReadFailure),
        onRetryHeartRate = { scope.launch { readSavedRecap() } },
        modifier = modifier
    )
}

@Composable
private fun FollowAlongWorkoutSessionScreen(
    uiState: FollowAlongWorkoutSessionUiState,
    showEndConfirmation: Boolean,
    onRequestEnd: () -> Unit,
    onCancelEnd: () -> Unit,
    onConfirmEnd: () -> Unit,
    onBackToFollowAlong: () -> Unit,
    heartRateCard: WorkoutSessionHeartRateCardUiState = WorkoutSessionHeartRateCardUiState.Hidden,
    onRetryHeartRate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val skin = LocalTrainFlowSkin.current
    val bottomControlsSpec = trainingExecutionBottomControlsSpec()
    Box(modifier = modifier.fillMaxSize().background(skin.tokens.primary)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = currentPageHorizontalPadding())
                .padding(top = if (skin.isBigType) 14.dp else 22.dp,
                    bottom = if (uiState.isTerminal) 22.dp else bottomControlsSpec.fixedBottomContentReserve),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(uiState.title, style = MaterialTheme.typography.headlineLarge, color = TrainFlowNeutral50)
            Text(uiState.statusLabel, style = MaterialTheme.typography.bodyLarge, color = TrainFlowNeutral200)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(currentCardCorner()),
                colors = CardDefaults.cardColors(containerColor = skin.tokens.secondary),
                border = BorderStroke(1.dp, TrainFlowNeutral100)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("本场时间", style = MaterialTheme.typography.titleMedium, color = TrainFlowNeutral200)
                    Text(uiState.timerText, fontSize = 68.sp, lineHeight = 70.sp,
                        fontWeight = FontWeight.ExtraBold, color = TrainFlowNeutral50)
                }
            }
            if (uiState.isTerminal) {
                WorkoutSessionHeartRateCard(heartRateCard, onRetryHeartRate)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = skin.tokens.secondary)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(uiState.terminalTitle.orEmpty(), style = MaterialTheme.typography.headlineSmall,
                            color = TrainFlowNeutral50)
                        Text(uiState.terminalSummary.orEmpty(), style = MaterialTheme.typography.bodyMedium,
                            color = TrainFlowNeutral200)
                        Button(onClick = onBackToFollowAlong, enabled = uiState.canReturn,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("返回跟练")
                        }
                    }
                }
            }
        }
        if (uiState.canStop) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = skin.tokens.primary,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                        .padding(horizontal = currentPageHorizontalPadding(),
                            vertical = bottomControlsSpec.verticalPadding),
                    verticalArrangement = Arrangement.spacedBy(bottomControlsSpec.rowSpacing)
                ) {
                    Button(
                        onClick = onRequestEnd,
                        modifier = Modifier.fillMaxWidth().heightIn(min = bottomControlsSpec.primaryButtonMinHeight)
                    ) {
                        Text("停止跟练", fontSize = if (skin.isBigType) 20.sp else 14.sp)
                    }
                }
            }
        }
        if (showEndConfirmation) {
            WorkoutEndConfirmationDialog(
                title = "停止本次跟练？",
                text = "确认后完成并保存本场记录；取消则继续计时。",
                onCancel = onCancelEnd,
                onConfirm = onConfirmEnd
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FollowAlongWorkoutSessionPreview() {
    TrainFlowTheme {
        FollowAlongWorkoutSessionScreen(
            uiState = buildFollowAlongWorkoutSessionUiState(elapsedSec = 65, active = true),
            showEndConfirmation = false,
            onRequestEnd = {},
            onCancelEnd = {},
            onConfirmEnd = {},
            onBackToFollowAlong = {}
        )
    }
}
