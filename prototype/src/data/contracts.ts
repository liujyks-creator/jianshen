export type WorkoutMode = 'timed' | 'strength' | 'follow_along'

export type ExerciseDifficulty = 'beginner' | 'intermediate' | 'advanced'

export type ExerciseRole = 'warmup' | 'main' | 'stretch' | 'recovery'

export type ExerciseSide = 'both' | 'left' | 'right' | 'alternating'

export type TimedStageType = 'warmup' | 'work' | 'rest' | 'cooldown' | 'custom'

export type EquipmentKind =
  | 'bodyweight'
  | 'dumbbell'
  | 'barbell'
  | 'machine'
  | 'cable'
  | 'band'
  | 'kettlebell'
  | 'mat'
  | 'other'

export interface MediaAssetRef {
  id: string
  kind: 'image' | 'video' | 'animation' | 'audio'
  uri: string
  altText?: string
  posterUri?: string
  durationMs?: number
  locale?: string
  role?: 'thumbnail' | 'demo' | 'instruction' | 'coach' | 'recovery'
}

export interface ExerciseCapabilities {
  supportsTimedTraining: boolean
  supportsReps: boolean
  supportsWeight: boolean
  supportsFollowAlong: boolean
  supportsWarmupRole: boolean
  supportsStretchRole: boolean
  supportsCircuitRole: boolean
  isUnilateral: boolean
}

export interface ExerciseInstructionContent {
  shortCue: string
  steps: string[]
  keyPoints: string[]
  commonMistakes: string[]
  breathingCues?: string[]
  cautions?: string[]
}

export interface ExerciseRecoveryMapping {
  trainedMuscleIds: string[]
  recommendedRecoveryAreaIds: string[]
  recoveryContentIds?: string[]
}

export interface ExerciseSubstitution {
  exerciseId: string
  reasonTags?: string[]
  equipmentFallback?: boolean
}

export interface ContentSourceMeta {
  author?: string
  reviewer?: string
  sourceRefs?: string[]
  updatedAt?: string
}

export interface Exercise {
  id: string
  name: string
  aliases?: string[]
  category: string
  primaryMuscleIds: string[]
  secondaryMuscleIds?: string[]
  equipment: EquipmentKind[]
  difficulty: ExerciseDifficulty
  roles: ExerciseRole[]
  capabilities: ExerciseCapabilities
  instructions: ExerciseInstructionContent
  media?: MediaAssetRef[]
  recovery?: ExerciseRecoveryMapping
  substitutions?: ExerciseSubstitution[]
  contentStatus: 'draft' | 'reviewed' | 'published'
  sourceMeta?: ContentSourceMeta
  extensions?: Record<string, unknown>
}

export interface PlanReminder {
  enabled: boolean
  scheduleAt?: string
  repeatRule?: string
}

export interface HeartRateDisplayPreference {
  enabled: boolean
  showDisconnectedPlaceholder: boolean
}

export interface PlanPreferences {
  cueSettings?: CueSettings
  heartRateDisplay?: HeartRateDisplayPreference
}

export interface WorkoutPlan {
  id: string
  mode: WorkoutMode
  title: string
  description?: string
  blocks: PlanBlock[]
  reminder?: PlanReminder
  preferences?: PlanPreferences
  followAlong?: FollowAlongPlanMeta
  createdAt: string
  updatedAt: string
}

export interface PlanBlockBase {
  id: string
  title?: string
  order: number
}

export interface WarmupBlock extends PlanBlockBase {
  kind: 'warmup'
  durationSec?: number
  items?: TimedExerciseItem[]
}

export interface StretchBlock extends PlanBlockBase {
  kind: 'stretch'
  durationSec?: number
  items?: TimedExerciseItem[]
}

export interface CooldownBlock extends PlanBlockBase {
  kind: 'cooldown'
  durationSec?: number
  items?: TimedExerciseItem[]
}

export interface RestBlock extends PlanBlockBase {
  kind: 'rest'
  durationSec: number
  label?: string
}

export interface TimedCircuitBlock extends PlanBlockBase {
  kind: 'timed_circuit'
  rounds: number
  restBetweenRoundsSec?: number
  items: TimedExerciseItem[]
}

export interface TimedExerciseItem {
  id: string
  exerciseId?: string
  labelOverride?: string
  side?: ExerciseSide
  stageType?: TimedStageType
  iconKey?: string
  colorHex?: string
  workDurationSec: number
  restAfterSec?: number
  cueSettings?: CueSettings
  autoAdvance?: boolean
}

export interface StageColorPreset {
  id: string
  name: string
  hex: string
  tone: string
  recommendedUse: string
  textColor: string
  isHighAttention: boolean
  isRecommended: boolean
}

export interface CountdownCue {
  enabled: boolean
  thresholdSec: number
  soundEnabled: boolean
  vibrationEnabled: boolean
  emphasisAnimationEnabled: boolean
  voiceCueEnabled?: boolean
}

export interface CueSettings {
  actionEnding?: CountdownCue
  restEnding?: CountdownCue
}

export interface WeightValue {
  value: number
  unit: 'kg' | 'lb'
}

export type RepTarget =
  | { kind: 'fixed'; reps: number }
  | { kind: 'range'; minReps: number; maxReps: number }

export interface StrengthExerciseTarget {
  weight?: WeightValue
  repTarget?: RepTarget
  restAfterSetSec?: number
}

export interface StrengthSetPlan {
  id: string
  order: number
  kind: 'warmup' | 'working' | 'drop' | 'backoff'
  side?: ExerciseSide
  targetWeight?: WeightValue
  repTarget?: RepTarget
  restAfterSec?: number
}

export interface StrengthExerciseBlock extends PlanBlockBase {
  kind: 'strength_exercise'
  exerciseId: string
  target?: StrengthExerciseTarget
  sets: StrengthSetPlan[]
  substitutions?: string[]
  setTimerMode?: 'manual_start' | 'auto_after_rest'
}

export type PlanBlock =
  | WarmupBlock
  | StretchBlock
  | CooldownBlock
  | RestBlock
  | TimedCircuitBlock
  | StrengthExerciseBlock

export interface FollowAlongPlanMeta {
  preset: boolean
  coverMediaId?: string
  coachMediaIds?: string[]
  chapterIds?: string[]
  timelineCueIds?: string[]
  musicTrackIds?: string[]
  aiAnalysisProfileId?: string
}

export type SessionStatus = 'ready' | 'active' | 'paused' | 'completed' | 'abandoned'

export type SessionStepKind =
  | 'prepare'
  | 'timed_work'
  | 'timed_rest'
  | 'strength_prepare_set'
  | 'strength_active_set'
  | 'strength_confirm_set'
  | 'strength_rest'
  | 'stretch'
  | 'completed'

export interface SessionStep {
  id: string
  kind: SessionStepKind
  blockId?: string
  itemId?: string
  setPlanId?: string
  exerciseId?: string
  startedAt?: string
  remainingSec?: number
  plannedDurationSec?: number
}

export interface SessionStepRecord {
  stepId: string
  kind: SessionStepKind
  startedAt: string
  endedAt?: string
  skipped?: boolean
  actualDurationSec?: number
}

export interface TimedRestExtensionRecord {
  id: string
  stepId: string
  stepIndex: number
  roundIndex?: number
  restStageId?: string
  restStageTitle: string
  previousStageId?: string
  previousStageTitle?: string
  addedSec: number
  plannedRestSec: number
  restElapsedBeforeExtensionSec: number
  extensionAtRemainingSec: number
  cumulativeExtraRestSec: number
  eventElapsedSec: number
}

export type SetEffort = 'easy' | 'good' | 'hard' | 'form_breakdown'

export interface StrengthSetRecord {
  id: string
  exerciseId: string
  sourceSetPlanId?: string
  setOrder: number
  setKind: 'warmup' | 'working' | 'drop' | 'backoff'
  side?: ExerciseSide
  plannedWeight?: WeightValue
  plannedRepTarget?: RepTarget
  actualWeight?: WeightValue
  actualReps?: number
  activeDurationSec?: number
  actualRestAfterSec?: number
  effort?: SetEffort
  substitutedFromExerciseId?: string
  notes?: string
}

export interface SessionFeedback {
  overallEffort?: 'easy' | 'good' | 'hard'
  discomfortNotes?: string
  notes?: string
}

export interface WorkoutPlanSnapshot {
  planId?: string
  title: string
  mode: WorkoutMode
  blocks: PlanBlock[]
  preferences?: PlanPreferences
  followAlong?: FollowAlongPlanMeta
}

export interface WorkoutSession {
  id: string
  planId?: string
  mode: WorkoutMode
  planSnapshot: WorkoutPlanSnapshot
  status: SessionStatus
  startedAt?: string
  endedAt?: string
  totalElapsedSec?: number
  effectiveElapsedSec?: number
  pausedElapsedSec?: number
  currentStep?: SessionStep
  stepHistory: SessionStepRecord[]
  timedRestExtensionRecords?: TimedRestExtensionRecord[]
  strengthSetRecords?: StrengthSetRecord[]
  userFeedback?: SessionFeedback
}

export type HeartRateAvailability =
  | 'disabled'
  | 'not_connected'
  | 'connecting'
  | 'available'
  | 'stale'
  | 'error'

export interface HeartRateState {
  availability: HeartRateAvailability
  bpm?: number
  measuredAt?: string
  sourceId?: string
  warningLevel?: 'none' | 'attention' | 'high'
  message?: string
}

export type WorkoutCommand =
  | { type: 'start_session' }
  | { type: 'pause_session' }
  | { type: 'resume_session' }
  | { type: 'skip_step' }
  | { type: 'extend_rest'; seconds: number }
  | { type: 'start_strength_set'; setPlanId?: string }
  | { type: 'complete_strength_set'; activeDurationSec?: number }
  | { type: 'confirm_strength_set'; actualWeight?: WeightValue; actualReps?: number; effort?: SetEffort }
  | { type: 'replace_exercise'; fromExerciseId: string; toExerciseId: string }
  | { type: 'end_session'; reason?: string }

export type WorkoutEvent =
  | { type: 'session_started'; sessionId: string }
  | { type: 'session_paused'; sessionId: string }
  | { type: 'session_resumed'; sessionId: string }
  | { type: 'timed_work_started'; exerciseId?: string; stepId: string }
  | { type: 'timed_work_ending'; stepId: string; remainingSec: number }
  | { type: 'rest_started'; stepId: string; durationSec: number }
  | { type: 'rest_ending'; stepId: string; remainingSec: number }
  | { type: 'strength_set_ready'; setPlanId?: string; exerciseId: string }
  | { type: 'strength_set_started'; setPlanId?: string; exerciseId: string }
  | { type: 'strength_set_completed'; setRecordId: string }
  | { type: 'next_exercise_ready'; exerciseId: string }
  | { type: 'session_completed'; sessionId: string }

export interface RecoveryArea {
  id: string
  name: string
  bodyRegion: 'front' | 'back' | 'upper' | 'lower' | 'full'
  summary: string
  media?: MediaAssetRef[]
  cautionText?: string
}

export interface RecoveryRecommendation {
  sessionId: string
  trainedMuscleIds: string[]
  areaIds: string[]
  contentIds?: string[]
}
