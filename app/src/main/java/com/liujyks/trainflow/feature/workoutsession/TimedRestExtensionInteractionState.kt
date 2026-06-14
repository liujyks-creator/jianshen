package com.liujyks.trainflow.feature.workoutsession

import com.liujyks.trainflow.core.engine.TimedSessionStepKind
import com.liujyks.trainflow.core.engine.TimedWorkoutEngineState
import com.liujyks.trainflow.core.model.SessionStatus

internal const val TimedRestExtensionSeconds = 15
internal const val TimedRestExtensionConfirmWindowMillis = 2_000L
internal const val TimedRestExtensionSuccessFeedbackMillis = 800L
internal const val TimedRestExtensionLimitPerStep = 4
internal const val TimedRestExtensionLimitSecPerStep =
    TimedRestExtensionSeconds * TimedRestExtensionLimitPerStep

internal data class TimedRestExtensionInteractionState(
    val pendingStepId: String? = null,
    val pendingStartedAtMillis: Long? = null,
    val successStepId: String? = null,
    val successStartedAtMillis: Long? = null
)

internal data class TimedRestExtensionClickResult(
    val state: TimedRestExtensionInteractionState,
    val shouldDispatchExtendRest: Boolean
)

internal data class TimedRestExtensionControlUiState(
    val buttonLabel: String = "+15秒",
    val buttonEnabled: Boolean = false,
    val helperText: String? = null,
    val extensionCount: Int = 0,
    val cumulativeExtraRestSec: Int = 0,
    val hitExtensionLimit: Boolean = false
)

internal fun TimedRestExtensionInteractionState.onRestExtensionClick(
    engineState: TimedWorkoutEngineState,
    nowMillis: Long
): TimedRestExtensionClickResult {
    val context = engineState.currentRestExtensionContext()
        ?: return TimedRestExtensionClickResult(
            state = reset(),
            shouldDispatchExtendRest = false
        )
    if (context.hitExtensionLimit) {
        return TimedRestExtensionClickResult(
            state = reset(),
            shouldDispatchExtendRest = false
        )
    }

    val normalized = normalizedFor(context.stepId, nowMillis)
    val pendingMatchesCurrentStep = normalized.pendingStepId == context.stepId
    val pendingStartedAt = normalized.pendingStartedAtMillis
    val isConfirmedInWindow = pendingMatchesCurrentStep &&
        pendingStartedAt != null &&
        nowMillis - pendingStartedAt <= TimedRestExtensionConfirmWindowMillis

    return if (isConfirmedInWindow) {
        TimedRestExtensionClickResult(
            state = TimedRestExtensionInteractionState(
                successStepId = context.stepId,
                successStartedAtMillis = nowMillis
            ),
            shouldDispatchExtendRest = true
        )
    } else {
        TimedRestExtensionClickResult(
            state = TimedRestExtensionInteractionState(
                pendingStepId = context.stepId,
                pendingStartedAtMillis = nowMillis
            ),
            shouldDispatchExtendRest = false
        )
    }
}

internal fun TimedRestExtensionInteractionState.toRestExtensionControlUiState(
    engineState: TimedWorkoutEngineState,
    nowMillis: Long
): TimedRestExtensionControlUiState {
    val context = engineState.currentRestExtensionContext()
        ?: return TimedRestExtensionControlUiState()
    val normalized = normalizedFor(context.stepId, nowMillis)

    if (context.hitExtensionLimit) {
        return TimedRestExtensionControlUiState(
            buttonLabel = "+15秒",
            buttonEnabled = false,
            helperText = "已额外休息 1 分钟，需要更久可以暂停训练",
            extensionCount = context.extensionCount,
            cumulativeExtraRestSec = context.cumulativeExtraRestSec,
            hitExtensionLimit = true
        )
    }

    val successVisible = normalized.successStepId == context.stepId &&
        normalized.successStartedAtMillis != null &&
        nowMillis - normalized.successStartedAtMillis < TimedRestExtensionSuccessFeedbackMillis
    val pendingVisible = normalized.pendingStepId == context.stepId &&
        normalized.pendingStartedAtMillis != null &&
        nowMillis - normalized.pendingStartedAtMillis <= TimedRestExtensionConfirmWindowMillis

    return when {
        successVisible -> TimedRestExtensionControlUiState(
            buttonLabel = "已加 15秒",
            buttonEnabled = false,
            extensionCount = context.extensionCount,
            cumulativeExtraRestSec = context.cumulativeExtraRestSec
        )
        pendingVisible -> TimedRestExtensionControlUiState(
            buttonLabel = "确认 +15秒",
            buttonEnabled = true,
            extensionCount = context.extensionCount,
            cumulativeExtraRestSec = context.cumulativeExtraRestSec
        )
        else -> TimedRestExtensionControlUiState(
            buttonLabel = "+15秒",
            buttonEnabled = true,
            extensionCount = context.extensionCount,
            cumulativeExtraRestSec = context.cumulativeExtraRestSec
        )
    }
}

internal fun TimedRestExtensionInteractionState.clearForCurrentEngineStep(
    engineState: TimedWorkoutEngineState,
    nowMillis: Long
): TimedRestExtensionInteractionState {
    val context = engineState.currentRestExtensionContext() ?: return reset()
    return normalizedFor(context.stepId, nowMillis)
}

private data class TimedRestExtensionStepContext(
    val stepId: String,
    val extensionCount: Int,
    val cumulativeExtraRestSec: Int
) {
    val hitExtensionLimit: Boolean
        get() = extensionCount >= TimedRestExtensionLimitPerStep ||
            cumulativeExtraRestSec >= TimedRestExtensionLimitSecPerStep
}

private fun TimedWorkoutEngineState.currentRestExtensionContext(): TimedRestExtensionStepContext? {
    val step = currentStep ?: return null
    if (status != SessionStatus.ACTIVE || step.kind != TimedSessionStepKind.REST) return null

    val currentStepExtensions = restExtensionHistory.filter { extension ->
        extension.stepId == step.id
    }
    return TimedRestExtensionStepContext(
        stepId = step.id,
        extensionCount = currentStepExtensions.size,
        cumulativeExtraRestSec = currentStepExtensions.sumOf { extension -> extension.addedSec }
    )
}

private fun TimedRestExtensionInteractionState.normalizedFor(
    currentStepId: String,
    nowMillis: Long
): TimedRestExtensionInteractionState {
    val keepPending = pendingStepId == currentStepId &&
        pendingStartedAtMillis != null &&
        nowMillis - pendingStartedAtMillis <= TimedRestExtensionConfirmWindowMillis
    val keepSuccess = successStepId == currentStepId &&
        successStartedAtMillis != null &&
        nowMillis - successStartedAtMillis < TimedRestExtensionSuccessFeedbackMillis

    return copy(
        pendingStepId = if (keepPending) pendingStepId else null,
        pendingStartedAtMillis = if (keepPending) pendingStartedAtMillis else null,
        successStepId = if (keepSuccess) successStepId else null,
        successStartedAtMillis = if (keepSuccess) successStartedAtMillis else null
    )
}

private fun TimedRestExtensionInteractionState.reset(): TimedRestExtensionInteractionState {
    return TimedRestExtensionInteractionState()
}
