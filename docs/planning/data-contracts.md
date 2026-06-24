---
workflowType: data-contracts
projectName: TrainFlow
documentLanguage: zh-Hans
status: draft
inputDocuments:
  - docs/planning/prd.md
  - docs/planning/ux-design.md
stepsCompleted:
  - product-data-requirements
  - initial-contract-draft
---

# TrainFlow 数据与接口标准草案

**文档状态:** 首版草案  
**日期:** 2026-05-21  
**用途:** 为前端原型、动作库导入、训练执行引擎和后续 Android 架构提供共同数据边界。

## 1. 设计目标

本草案先解决 5 个问题：

1. 动作库如何定义，才能逐个导入动作内容而不返工。
2. 计时训练、力量训练和跟练雏形如何共享计划模型，又保留各自参数。
3. 计划数据与实际训练记录如何分离。
4. UI 按钮、未来语音交互、声音、震动和动画如何共用训练命令与训练事件。
5. 实时心率、课程媒体、音乐节拍和 AI 分析如何预留接口，而不把首版复杂度提前塞进核心模型。

## 2. 总体决策

### 2.1 核心概念

首版建议固定以下概念：

| 概念 | 含义 |
|---|---|
| `Exercise` | 动作库中的标准动作 |
| `WorkoutPlan` | 用户保存的训练计划 |
| `PlanBlock` | 计划中的阶段或结构块 |
| `WorkoutSession` | 一次真实训练执行实例 |
| `SessionStep` | 训练执行引擎当前推进的步骤 |
| `WorkoutCommand` | 用户或未来语音发出的训练控制命令 |
| `WorkoutEvent` | 训练执行产生的状态事件 |
| `HeartRateState` | UI 消费的抽象实时心率状态 |

### 2.2 首版建模原则

1. **动作库标准动作不等于计划动作。**  
   `Exercise` 提供能力、教学内容和映射；计划中保存的是对动作的引用与用户参数。

2. **计划不直接保存实际训练结果。**  
   计划描述目标；`WorkoutSession` 保存真实执行数据。

3. **计时训练以步骤推进为主。**  
   动作时间、休息时间、轮次和临近提醒是核心。

4. **力量训练以动作和组推进为主。**  
   每组目标、开始本组、完成本组、实际重量次数和休息记录是核心。

5. **跟练首版复用计时执行能力。**  
   跟练内容在首版可引用计时计划结构，并增加媒体与提示扩展位。

6. **训练引擎只认命令和事件。**  
   UI 按钮、未来语音和其他输入方式都发 `WorkoutCommand`；声音、动画、震动、日志和未来语音输出都消费 `WorkoutEvent`。

## 3. 标识与枚举约定

### 3.1 ID 建议

- 标准动作用稳定字符串 ID，例如 `barbell-bench-press`。
- 用户计划、训练记录、组记录使用系统生成 ID。
- 未来本地与云同步都应保留 `createdAt` 与 `updatedAt`。

### 3.2 基础枚举

```ts
type WorkoutMode = "timed" | "strength" | "follow_along";

type PlanBlockKind =
  | "warmup"
  | "timed_circuit"
  | "strength_exercise"
  | "rest"
  | "stretch"
  | "cooldown";

type ExerciseDifficulty = "beginner" | "intermediate" | "advanced";

type ExerciseRole = "warmup" | "main" | "stretch" | "recovery";

type ExerciseSide = "both" | "left" | "right" | "alternating";

type TimedStageType = "warmup" | "work" | "rest" | "cooldown" | "custom";

type EquipmentKind =
  | "bodyweight"
  | "dumbbell"
  | "barbell"
  | "machine"
  | "cable"
  | "band"
  | "kettlebell"
  | "mat"
  | "other";
```

## 4. 动作库标准

### 4.1 动作库最小标准

每个动作都应至少可用于：

- 动作列表与筛选。
- 动作详情。
- 计划编辑。
- 训练执行中的短提示。
- 后续恢复建议与动作替代。

### 4.2 动作能力

```ts
interface ExerciseCapabilities {
  supportsTimedTraining: boolean;
  supportsReps: boolean;
  supportsWeight: boolean;
  supportsFollowAlong: boolean;
  supportsWarmupRole: boolean;
  supportsStretchRole: boolean;
  supportsCircuitRole: boolean;
  isUnilateral: boolean;
}
```

### 4.3 教学内容

```ts
interface ExerciseInstructionContent {
  shortCue: string;
  steps: string[];
  keyPoints: string[];
  commonMistakes: string[];
  breathingCues?: string[];
  cautions?: string[];
}
```

### 4.4 媒体与内容扩展

```ts
type MediaKind = "image" | "video" | "animation" | "audio";

interface MediaAssetRef {
  id: string;
  kind: MediaKind;
  uri: string;
  altText?: string;
  posterUri?: string;
  durationMs?: number;
  locale?: string;
  role?: "thumbnail" | "demo" | "instruction" | "coach" | "recovery";
}
```

媒体字段首版允许为空，但结构必须保留，避免未来导入动作图片、短视频和跟练媒体时重构动作模型。

### 4.5 恢复与替代映射

```ts
interface ExerciseRecoveryMapping {
  trainedMuscleIds: string[];
  recommendedRecoveryAreaIds: string[];
  recoveryContentIds?: string[];
}

interface ExerciseSubstitution {
  exerciseId: string;
  reasonTags?: string[];
  equipmentFallback?: boolean;
}
```

### 4.6 标准动作接口

```ts
interface Exercise {
  id: string;
  name: string;
  aliases?: string[];
  category: string;
  primaryMuscleIds: string[];
  secondaryMuscleIds?: string[];
  equipment: EquipmentKind[];
  difficulty: ExerciseDifficulty;
  roles: ExerciseRole[];
  capabilities: ExerciseCapabilities;
  instructions: ExerciseInstructionContent;
  media?: MediaAssetRef[];
  recovery?: ExerciseRecoveryMapping;
  substitutions?: ExerciseSubstitution[];
  contentStatus: "draft" | "reviewed" | "published";
  sourceMeta?: ContentSourceMeta;
  extensions?: Record<string, unknown>;
}

interface ContentSourceMeta {
  author?: string;
  reviewer?: string;
  sourceRefs?: string[];
  updatedAt?: string;
}
```

### 4.7 动作库导入约束

首版动作导入建议遵守：

- 必填字段缺失时不进入可选动作列表。
- 未审阅动作可以服务内部原型，但正式发布时应区分内容状态。
- 动作短提示必须存在，训练执行页不能依赖长文案截断。
- 动作媒体可后补，不阻塞先定义接口。

## 5. 训练计划模型

### 5.1 计划公共字段

```ts
interface WorkoutPlan {
  id: string;
  mode: WorkoutMode;
  title: string;
  description?: string;
  blocks: PlanBlock[];
  reminder?: PlanReminder;
  preferences?: PlanPreferences;
  followAlong?: FollowAlongPlanMeta;
  createdAt: string;
  updatedAt: string;
}

interface PlanReminder {
  enabled: boolean;
  scheduleAt?: string;
  repeatRule?: string;
}

interface PlanPreferences {
  cueSettings?: CueSettings;
  heartRateDisplay?: HeartRateDisplayPreference;
}

interface HeartRateDisplayPreference {
  enabled: boolean;
  showDisconnectedPlaceholder: boolean;
}
```

### 5.2 计划块

```ts
type PlanBlock =
  | WarmupBlock
  | TimedCircuitBlock
  | StrengthExerciseBlock
  | RestBlock
  | StretchBlock
  | CooldownBlock;

interface PlanBlockBase {
  id: string;
  kind: PlanBlockKind;
  title?: string;
  order: number;
}
```

### 5.3 热身、拉伸和休息块

```ts
interface WarmupBlock extends PlanBlockBase {
  kind: "warmup";
  durationSec?: number;
  items?: TimedExerciseItem[];
}

interface StretchBlock extends PlanBlockBase {
  kind: "stretch";
  durationSec?: number;
  items?: TimedExerciseItem[];
}

interface CooldownBlock extends PlanBlockBase {
  kind: "cooldown";
  durationSec?: number;
  items?: TimedExerciseItem[];
}

interface RestBlock extends PlanBlockBase {
  kind: "rest";
  durationSec: number;
  label?: string;
}
```

## 6. 计时训练计划

> E10.2 迁移备注：Android 生产实现已把计时训练改为纯间歇计时器体验。`TimedCircuitBlock` 继续作为最小兼容容器表达轮数、轮间休息和阶段序列；`TimedExerciseItem` 现在可以表示纯阶段，`exerciseId` 变为可选。计时训练不再要求动作库动作，也不进入动作选择、动作详情或动作推荐。跟练雏形仍可继续用 `exerciseId` 引用动作库动作，力量训练不受影响。

### 6.1 计时训练结构

计时训练主结构建议使用 `TimedCircuitBlock`：

```ts
interface TimedCircuitBlock extends PlanBlockBase {
  kind: "timed_circuit";
  rounds: number;
  restBetweenRoundsSec?: number;
  items: TimedExerciseItem[];
}

interface TimedExerciseItem {
  id: string;
  exerciseId?: string;
  labelOverride?: string;
  side?: ExerciseSide;
  stageType?: TimedStageType;
  iconKey?: string;
  colorHex?: string;
  workDurationSec: number;
  restAfterSec?: number;
  cueSettings?: CueSettings;
  autoAdvance?: boolean;
}
```

### 6.1.1 阶段颜色色板

E10.17 约定：阶段颜色由集中 `StageColorPreset` 色板提供，UI 不应在各处散落 hex。`TimedExerciseItem.colorHex` 仍是计划结构中的保存字段；色板 metadata 服务编辑器、可访问性和执行页高对比，不改变训练引擎、命令、事件或 session record 语义。

```ts
interface StageColorPreset {
  id: string;
  name: string;
  hex: string;
  tone: string;
  recommendedUse: string;
  textColor: string;
  isHighAttention: boolean;
  isRecommended: boolean;
}
```

E10.17 规则：

- 推荐色保持 5-8 个，服务快速创建。
- 更多颜色覆盖 20 个 Material-like 色值，服务自定义。
- `textColor` 用于深色执行页中阶段色填充中心圆时的圆内文字 / 图标对比。
- 选中态不能只靠颜色表达，必须有外圈 / 描边、对勾和 TalkBack 文案。
- 非法 `colorHex` 读回或映射时回退到当前阶段类型默认安全色，不应影响计划持久化或执行页。

E10.2 约定：

- 纯计时阶段使用 `labelOverride` 作为阶段名称。
- `stageType` 表达热身、工作、休息、放松或自定义；阶段图标和颜色只表达阶段状态，不表达动作教学。
- `stageType: "rest"` 的 item 在执行引擎中展开为休息步骤，而不是动作库动作。
- 热身和放松可以继续用 `WarmupBlock` / `CooldownBlock` 表达不参与循环的阶段；工作、休息和自定义阶段放入 `TimedCircuitBlock.items` 后按 `rounds` 重复。
- E10.2 已实现阶段拖拽排序；只有阶段行右侧明确的拖动手柄触发长按拖拽，名称输入、时间输入、类型选择、颜色入口、复制 / 删除和行空白区域不触发排序。
- 上移 / 下移仍保留为备用和无障碍排序路径。
- 热身固定在编辑列表开头，放松固定在末尾，中间的工作、休息和自定义阶段可排序；执行结构应与编辑页显示顺序保持一致。
- E10.2 仅补齐计时阶段颜色入口；计划主题色 / 整体配色编辑保留为后续 polish，不代表力量或跟练动作选择页已实现。

### 6.2 计时提醒设置

```ts
interface CueSettings {
  actionEnding?: CountdownCue;
  restEnding?: CountdownCue;
}

interface CountdownCue {
  enabled: boolean;
  thresholdSec: number;
  soundEnabled: boolean;
  vibrationEnabled: boolean;
  emphasisAnimationEnabled: boolean;
  voiceCueEnabled?: boolean;
}
```

首版默认建议：

```ts
const defaultCountdownCue: CountdownCue = {
  enabled: true,
  thresholdSec: 5,
  soundEnabled: true,
  vibrationEnabled: true,
  emphasisAnimationEnabled: true,
  voiceCueEnabled: false
};
```

### 6.3 计时训练示例

```ts
const timedPlanExample: WorkoutPlan = {
  id: "plan-timed-001",
  mode: "timed",
  title: "全身循环 20 分钟",
  blocks: [
    {
      id: "warmup-1",
      kind: "warmup",
      order: 1,
      durationSec: 300
    },
    {
      id: "circuit-1",
      kind: "timed_circuit",
      order: 2,
      rounds: 3,
      restBetweenRoundsSec: 90,
      items: [
        {
          id: "item-1",
          exerciseId: "jumping-jack",
          workDurationSec: 45,
          restAfterSec: 15,
          autoAdvance: true
        }
      ]
    }
  ],
  createdAt: "2026-05-21T00:00:00Z",
  updatedAt: "2026-05-21T00:00:00Z"
};
```

## 7. 力量训练计划

### 7.1 力量动作块

```ts
interface StrengthExerciseBlock extends PlanBlockBase {
  kind: "strength_exercise";
  exerciseId: string;
  target?: StrengthExerciseTarget;
  sets: StrengthSetPlan[];
  substitutions?: string[];
  setTimerMode?: "manual_start" | "auto_after_rest";
}

interface StrengthExerciseTarget {
  weight?: WeightValue;
  repTarget?: RepTarget;
  restAfterSetSec?: number;
}

interface StrengthSetPlan {
  id: string;
  order: number;
  kind: "warmup" | "working" | "drop" | "backoff";
  side?: ExerciseSide;
  targetWeight?: WeightValue;
  repTarget?: RepTarget;
  restAfterSec?: number;
}

interface WeightValue {
  value: number;
  unit: "kg" | "lb";
}

type RepTarget =
  | { kind: "fixed"; reps: number }
  | { kind: "range"; minReps: number; maxReps: number };
```

### 7.2 力量训练默认值

首版默认建议：

- `repTarget` 默认 `{ kind: "range", minReps: 8, maxReps: 12 }`
- `setTimerMode` 默认 `"manual_start"`
- 动作级目标可作为多个组的共享默认值。
- 用户展开逐组设置后，`StrengthSetPlan` 覆盖动作级默认值。

### 7.3 力量训练示例

```ts
const strengthBlockExample: StrengthExerciseBlock = {
  id: "strength-bench-press",
  kind: "strength_exercise",
  order: 2,
  exerciseId: "barbell-bench-press",
  setTimerMode: "manual_start",
  target: {
    weight: { value: 60, unit: "kg" },
    repTarget: { kind: "range", minReps: 8, maxReps: 12 },
    restAfterSetSec: 90
  },
  sets: [
    {
      id: "bench-warmup-1",
      order: 1,
      kind: "warmup",
      targetWeight: { value: 20, unit: "kg" },
      repTarget: { kind: "fixed", reps: 12 },
      restAfterSec: 60
    },
    {
      id: "bench-working-1",
      order: 2,
      kind: "working"
    }
  ],
  substitutions: ["dumbbell-bench-press", "machine-chest-press"]
};
```

## 8. 跟练雏形计划元数据

跟练首版不单独发明完全不同的训练计划引擎。  
建议使用 `WorkoutPlan`，并通过跟练元数据和媒体提示增加展示能力。

```ts
interface FollowAlongPlanMeta {
  preset: boolean;
  coverMediaId?: string;
  coachMediaIds?: string[];
  chapterIds?: string[];
  timelineCueIds?: string[];
  musicTrackIds?: string[];
  aiAnalysisProfileId?: string;
}
```

### 8.1 跟练扩展原则

- 计时训练计划可在满足媒体与提示条件时切换为跟练视图。
- 首版可先用少量预置跟练计划。
- 教练视频、课程章节、自动语音、音乐节拍与 AI 纠错只保留元数据与扩展位。

## 9. 训练执行会话

### 9.1 计划与会话分离

执行训练时应生成 `WorkoutSession`：

```ts
interface WorkoutSession {
  id: string;
  planId?: string;
  mode: WorkoutMode;
  planSnapshot: WorkoutPlanSnapshot;
  status: SessionStatus;
  startedAt?: string;
  endedAt?: string;
  totalElapsedSec?: number;
  effectiveElapsedSec?: number;
  pausedElapsedSec?: number;
  currentStep?: SessionStep;
  stepHistory: SessionStepRecord[];
  timedRestExtensionRecords?: TimedRestExtensionRecord[];
  strengthSetRecords?: StrengthSetRecord[];
  userFeedback?: SessionFeedback;
}

type SessionStatus =
  | "ready"
  | "active"
  | "paused"
  | "completed"
  | "abandoned";

interface WorkoutPlanSnapshot {
  planId?: string;
  title: string;
  mode: WorkoutMode;
  blocks: PlanBlock[];
  preferences?: PlanPreferences;
  followAlong?: FollowAlongPlanMeta;
}

interface SessionFeedback {
  overallEffort?: "easy" | "good" | "hard";
  discomfortNotes?: string;
  notes?: string;
}
```

计划快照很重要：用户后来改计划，不应污染历史训练记录。

E10.4 约定：

- `WorkoutSession.planId` 继续作为查询字段保存计划 ID；`WorkoutPlanSnapshot.planId` 也可保存同一计划 ID，方便历史记录只看快照时仍能识别来源。
- 本地 Room 的 `plan_snapshot_json` 必须保存 MVP 所需的完整计划快照，而不是只保存标题和模式。至少保留 title、mode、blocks、计时阶段/轮次/休息结构、力量动作/计划组/目标/休息结构、preferences/cueSettings、heartRateDisplay 和 followAlong 元数据；读回后 `planSnapshot.blocks` 不应为空，除非原计划本来为空。
- `totalElapsedSec` 表示本次训练从 `startedAt` 到 `endedAt` 的 wall-clock 总耗时，包含准备、确认、休息、正式组和暂停。
- `effectiveElapsedSec` 表示训练执行的有效推进时间，不包含暂停。计时训练使用引擎 active elapsed；力量训练当前使用引擎 `sessionElapsedSec`，包含正式组与休息推进，不把 prepare / confirm 停留时间计入 effective。
- `pausedElapsedSec` 单独保存暂停累计时间；计时训练来自 `TimedWorkoutEngineState.pausedElapsedSec`，力量训练暂停时不推进组耗时或休息倒计时。
- 计时、力量和基础跟练在 completed / abandoned 终态都可以写入本地真实 `WorkoutSession`；终态写入使用一次性 guard 和异常吞并边界，避免重组重复插入或 Room 异常直接 crash UI。
- E10.4 仍只是本地 Room MVP 记录闭环；统计图表、趋势分析、删除清理、心率数据、云同步、账号体系和后台可靠计时仍留给后续 story。

E10.14 约定：

- 计时训练中的 `extend_rest` / `+15秒` 表示延长当前休息阶段，不插入新的休息阶段，也不修改原 `WorkoutPlan` 或 plan snapshot 中的计划休息秒数。
- 生产 UI 应在 route / UI state 层提供轻量防误触：第一次点击只进入 `确认 +15秒` 待确认态，2 秒内第二次点击才真正派发 `WorkoutCommand.ExtendRest`；超时自动恢复，不记录、不加时。
- 确认成功后可短暂显示 `已加 15秒`；每个 rest step 最多确认成功 4 次，即最多额外休息 60 秒。达到上限后禁用该休息阶段的 `+15秒`，提示“已额外休息 1 分钟，需要更久可以暂停训练”，但不自动暂停。
- 额外休息是用户主动增加恢复时间，和暂停不同：暂停冻结训练流程并计入 `pausedElapsedSec`；额外休息继续处于 active 训练流程内，计入本次 `totalElapsedSec` 和有效推进时间，但通过 `timedRestExtensionRecords` 单独记录。
- completed 与 abandoned 终态都应保存已发生的额外休息记录；ready/start gate 尚未真正开始时不能产生额外休息记录。
- E10.14 只提供真实记录输入，不实现 E12 统计图表、趋势分析、真实心率、motion timing rules、Stage color picker、声音播放、notification action 或后台可靠计时。

E10.18 约定：

- 编辑已保存计划时，编辑器应从当前 `WorkoutPlan` 回填标题、描述、blocks、计时阶段 / 轮次 / 休息 / cue settings、力量动作 / 目标 / 计划组 / 休息等计划结构。
- 保存编辑结果时继续使用同一个 `WorkoutPlan.id` 并保留原 `createdAt`，只更新当前计划结构、目标和 `updatedAt`；复制计划仍生成新的计划 ID。
- 编辑计划不得回写任何既有 `WorkoutSession.planSnapshot`。历史记录和后续统计应按训练发生时保存的 snapshot 展示 / 分析，不能用编辑后的当前计划反推旧 session。
- 跟练计划在完整编排能力完成前不暴露假的完整编辑入口。
- E10.18 不引入计划版本历史、撤销 / 重做、云同步冲突解决、完整跟练编排、统计图表、声音播放、真实心率设备、foreground service、exact alarm、notification action 或 reset production command。

E12.1 基础统计口径：

- 基础统计从本地真实 `WorkoutSession` list 推导，不把 preview / fixture / 内存态示例记录混入生产统计；无真实记录时不显示假统计。
- `totalElapsedSec` 继续表示 wall-clock 总用时；`effectiveElapsedSec` 表示有效推进时间；`pausedElapsedSec` 表示暂停累计时间，三者在统计 UI 中分开展示。
- planned rest 来自每条历史 session 保存的 `WorkoutSession.planSnapshot`，用于表达当时计划目标；计划后来编辑不回写历史 planned rest。
- actual rest 来自真实执行记录：计时训练使用 `SessionStepRecord.kind == "timed_rest"` 的实际时长，力量训练使用 `StrengthSetRecord.actualRestAfterSec`。
- extra rest 仅来自计时训练 `timedRestExtensionRecords.sum(addedSec)`，表示用户确认的 `+15秒` 额外休息；它不同于暂停，不增加 `pausedElapsedSec`。
- completed 与 abandoned 必须分开计数；abandoned session 仍可参与总用时、有效时间、暂停时间、actual rest 和 extra rest 总量统计。
- mode breakdown 只统计 `timed` / `strength` / `follow_along` 的基础数量，不在 E12.1 比较不同计划、不同阶段、不同轮次或不同动作的强弱趋势。
- E12.1 不定义图表趋势、平均心率趋势、真实设备数据、云同步、账号体系、历史记录清理或医疗判断。

E12.2b 力量同类 set 趋势口径：

- 只消费真实持久化 `WorkoutSession` list 中的 `StrengthSetRecord`，不使用 preview / fixture / 内存示例记录。
- 同类 set 比较必须限定同一 `StrengthSetRecord.exerciseId`，不得把不同动作的 set 自动合并成同一趋势。
- planned values 只能从每条历史 `WorkoutSession.planSnapshot` 的力量 block 中查找，不能用编辑后的当前 `WorkoutPlan` 反推旧 session。
- planned lookup 优先使用 `sourceSetPlanId`，且必须限定在对应 `exerciseId` 的 `StrengthExerciseBlock` 内匹配。
- 只有 `sourceSetPlanId == null` 时，才允许 fallback 到同一 `exerciseId` block 内的 `setOrder + setKind`。
- 如果 `sourceSetPlanId != null` 但找不到同一 `exerciseId` 的 matching set，应标记数据不足，不得再 fallback 到 `setOrder + setKind`。
- 替换动作的 planned values 只能查 `substitutedFromExerciseId` 对应原动作 block；非替换动作的 planned values 只能查 record 的 `exerciseId` 对应 block。
- 不得把原动作 block 和替换后动作 block 拼接为候选集合，也不得让替换动作自动并入原动作趋势；替换记录必须在趋势 UI 标注来源。
- 趋势只展示 planned / actual weight、planned / actual reps、set kind、set order、actual rest 和 active duration 等可回顾字段；不判断强弱，不推荐加重量，不输出康复、医疗或训练中断结论。

E12 / E11.3 后续心率趋势边界：

- E11.3 后首版不显示、不录入、不统计心率，也不规划平均心率趋势。
- 如果未来重新进入健康设备阶段，心率趋势只能消费已经保存到训练记录或分析数据源中的明确来源心率数据，不能直接把执行页瞬时 `HeartRateState` 当作历史趋势事实。
- 当前没有设备心率或手动心率 UI；历史页和趋势页不显示未获取心率占位，不绘制假平均心率趋势。
- 所有心率趋势必须标注来源边界，不得做医疗判断、危险告警、训练中断依据或康复结论。

### 9.2 执行步骤

```ts
type SessionStepKind =
  | "prepare"
  | "timed_work"
  | "timed_rest"
  | "strength_prepare_set"
  | "strength_active_set"
  | "strength_confirm_set"
  | "strength_rest"
  | "stretch"
  | "completed";

interface SessionStep {
  id: string;
  kind: SessionStepKind;
  blockId?: string;
  itemId?: string;
  setPlanId?: string;
  exerciseId?: string;
  startedAt?: string;
  remainingSec?: number;
  plannedDurationSec?: number;
}

interface SessionStepRecord {
  stepId: string;
  kind: SessionStepKind;
  startedAt: string;
  endedAt?: string;
  skipped?: boolean;
  actualDurationSec?: number;
}
```

### 9.3 计时休息延长记录

```ts
interface TimedRestExtensionRecord {
  id: string;
  stepId: string;
  stepIndex: number;
  roundIndex?: number;
  restStageId?: string;
  restStageTitle: string;
  previousStageId?: string;
  previousStageTitle?: string;
  addedSec: number;
  plannedRestSec: number;
  restElapsedBeforeExtensionSec: number;
  extensionAtRemainingSec: number;
  cumulativeExtraRestSec: number;
  eventElapsedSec: number;
}
```

字段含义：

- `stepId` / `stepIndex` 指向本次执行 timeline 中被延长的当前 rest step。
- `roundIndex`、`restStageId` / `restStageTitle` 和 `previousStageId` / `previousStageTitle` 用于后续分析哪些轮次、阶段或前一个工作 / 自定义阶段后更常需要额外休息。
- `addedSec` 当前由 UI 固定为 15 秒；多次点击会产生多条记录。
- `plannedRestSec` 保留原计划休息时长；额外休息不能伪装成 planned rest。
- `restElapsedBeforeExtensionSec` 与 `extensionAtRemainingSec` 记录点击时机；`cumulativeExtraRestSec` 记录当前 rest step 上累计额外休息。
- `extensionCount` 可由同一 `stepId` 的记录数推导；`hitExtensionLimit` 可由同一 `stepId` 的记录数是否达到 4 或 `cumulativeExtraRestSec` 是否达到 60 推导，不必重复落库。
- `eventElapsedSec` 是从真实启动后训练引擎有效推进时间轴上的发生秒数，供记录排序和 E12 后续分析使用。读取同一 session 的记录时应按 `eventElapsedSec -> stepIndex -> cumulativeExtraRestSec -> id` 排序，避免同秒多次确认时字符串 id 顺序错位。

### 9.4 力量训练组记录

```ts
interface StrengthSetRecord {
  id: string;
  exerciseId: string;
  sourceSetPlanId?: string;
  setOrder: number;
  setKind: "warmup" | "working" | "drop" | "backoff";
  side?: ExerciseSide;
  plannedWeight?: WeightValue;
  plannedRepTarget?: RepTarget;
  actualWeight?: WeightValue;
  actualReps?: number;
  activeDurationSec?: number;
  actualRestAfterSec?: number;
  effort?: SetEffort;
  substitutedFromExerciseId?: string;
  notes?: string;
}

type SetEffort = "easy" | "good" | "hard" | "form_breakdown";
```

E12.2b 计划值匹配规则：

- `sourceSetPlanId` 是 planned values 的首选匹配键；匹配必须同时满足同一历史 `planSnapshot`、同一 `exerciseId` block 和同一 set id。
- `sourceSetPlanId` 缺失时，才可在同一 `exerciseId` block 内使用 `setOrder + setKind` 作为兼容 fallback。
- `sourceSetPlanId` 存在但匹配失败时，说明计划快照或记录缺少可比计划组，趋势应显示数据不足；不得继续用 `setOrder + setKind` 猜测。
- 当 `substitutedFromExerciseId` 存在时，planned values 来源是原动作 `substitutedFromExerciseId` 对应 block，实际表现仍归属 record 的 `exerciseId` 并标注“替换自”。
- 当 `substitutedFromExerciseId` 不存在时，planned values 来源只能是 record 的 `exerciseId` 对应 block。

### 9.5 单组默认回填规则

用户完成力量训练单组时：

1. 实际重量默认取本组计划重量。
2. 若本组未设重量，则取动作级默认重量。
3. 实际次数默认取固定次数；若为次数区间，UI 可优先提供区间内快捷选择。
4. 用户可在确认层修改实际重量、实际次数和感受。

## 10. 训练命令

### 10.1 命令接口

```ts
type WorkoutCommand =
  | { type: "start_session" }
  | { type: "pause_session" }
  | { type: "resume_session" }
  | { type: "skip_step" }
  | { type: "extend_rest"; seconds: number }
  | { type: "start_strength_set"; setPlanId?: string }
  | { type: "complete_strength_set"; draft?: StrengthSetCompletionDraft }
  | { type: "confirm_strength_set"; record: StrengthSetCompletionInput }
  | { type: "replace_exercise"; fromExerciseId: string; toExerciseId: string }
  | { type: "update_actual_weight"; setRecordId: string; weight: WeightValue }
  | { type: "update_actual_reps"; setRecordId: string; reps: number }
  | { type: "end_session"; reason?: string };

interface StrengthSetCompletionDraft {
  activeDurationSec?: number;
}

interface StrengthSetCompletionInput {
  actualWeight?: WeightValue;
  actualReps?: number;
  effort?: SetEffort;
  notes?: string;
}
```

### 10.2 输入来源

命令来源应可标识，便于未来语音与调试：

```ts
type CommandSource = "ui" | "voice" | "system" | "wearable";

interface CommandEnvelope {
  command: WorkoutCommand;
  source: CommandSource;
  issuedAt: string;
}
```

## 11. 训练事件

### 11.1 事件接口

```ts
type WorkoutEvent =
  | { type: "session_started"; sessionId: string }
  | { type: "session_paused"; sessionId: string }
  | { type: "session_resumed"; sessionId: string }
  | { type: "timed_work_started"; exerciseId?: string; stepId: string }
  | { type: "timed_work_ending"; stepId: string; remainingSec: number }
  | { type: "rest_started"; stepId: string; durationSec: number }
  | { type: "rest_ending"; stepId: string; remainingSec: number }
  | { type: "strength_set_ready"; setPlanId?: string; exerciseId: string }
  | { type: "strength_set_started"; setPlanId?: string; exerciseId: string }
  | { type: "strength_set_completed"; setRecordId: string }
  | { type: "next_exercise_ready"; exerciseId: string }
  | { type: "session_completed"; sessionId: string };
```

### 11.2 事件消费者

首版和后续可按事件驱动：

| 消费者 | 用途 |
|---|---|
| UI | 页面状态与过渡 |
| Sound | 提示音 |
| Haptics | 震动 |
| Animation | 临近结束强化动画 |
| Analytics | 训练行为分析 |
| Voice Output | 后续语音提示 |
| Wearable Sync | 后续外设同步 |

## 12. 实时心率状态

### 12.1 UI 状态接口

`HeartRateState` 是训练执行页消费的 source-aware UI 抽象状态，不是历史趋势事实来源。

```ts
type HeartRateStateKind =
  | "unavailable"
  | "device_connected_no_reading"
  | "device_reading"
  | "manual_reading"
  | "stale_reading"
  | "permission_unavailable"
  | "provider_unavailable";

type HeartRateSourceKind = "none" | "device" | "manual";

type HeartRateUnavailableReason =
  | "no_source"
  | "disabled_by_user"
  | "not_configured";

interface HeartRateState {
  kind: HeartRateStateKind;
  sourceKind: HeartRateSourceKind;
  bpm?: number;
  measuredAt?: string;
  recordedAt?: string;
  sourceId?: string;
  sourceLabel?: string;
  unavailableReason?: HeartRateUnavailableReason;
  message?: string;
}
```

状态语义：

- `unavailable` + `sourceKind: "none"` + `unavailableReason: "no_source"` 表示没有设备数据、没有手动数据；E11.3 后当前生产 UI 不显示该状态。
- `device_connected_no_reading` 表示设备来源存在或已连接，但当前还没有可展示读数。
- `device_reading` 表示来自设备或后续设备 adapter 的当前读数。
- `manual_reading` 表示未来可能重新设计的手动来源读数，必须标注 `sourceKind: "manual"`，不得伪装成设备数据；E11.3 后首版不提供手动输入。
- `stale_reading` 表示上一条明确来源读数已过期，可携带原来源和上次时间，但 UI 应弱化展示。
- `permission_unavailable` 表示未来 provider adapter 需要的权限不可用；E11.1 不申请真实健康、蓝牙或身体传感器权限。
- `provider_unavailable` 表示 provider 被禁用、当前构建未接入或平台能力不可用。

后续 adapter 路线只作为 E11.2 或独立设备阶段评估：

- Apple Watch / iOS：未来 iOS 第一优先路线是 iOS app + watchOS companion，通过 HealthKit / HKWorkoutSession / HKLiveWorkoutBuilder 读取并转换，不能把 HealthKit model 泄漏到 TrainFlow UI / history / analytics；当前 Android-first 阶段不进入 dev。
- HUAWEI Band 9 / Huawei Health：当前真实验证样本是 HUAWEI Band 9 + 非华为 Android 手机 + 已安装华为运动健康，且华为运动健康可以读取手环数据；这只证明华为运动健康能读设备数据，不证明 TrainFlow 第三方 App 可以实时读取心率。E11.2a 先做 feasibility smoke，不直接承诺生产接入。
- 通用心率设备：标准 BLE Heart Rate Service 仍是 Android-first 最通用的实时路线；但当前设备条件下必须先确认 HUAWEI Band 9 是否暴露 BLE HRS `0x180D`，以及 Heart Rate Measurement characteristic `0x2A37` 是否可 notify，再决定是否进入 BLE adapter spike。
- Huawei Health Kit / Health Service Kit：官方生态存在，但实时心率、地区、账号、设备支持、权限申请、非华为手机兼容性都要验证。若 Band 9 不暴露 BLE HRS，再验证 Huawei Health Kit / Health Service Kit 是否能在非华为 Android + Band 9 + HMS Core 条件下授权读取实时心率。
- Health Connect：更适合历史摘要 / 趋势，例如 post-workout summaries 或 average heart-rate trend，不作为 E11.2a 实时执行页首选。
- 所有未来设备路线都必须统一输出 TrainFlow `HeartRateState`，标注来源；如未来重新引入手动数据，必须是 `sourceKind: "manual"`，不得伪装成设备数据。

E11.2a feasibility smoke 边界：

- 不持久化心率，不绘制平均心率趋势，不把执行页瞬时 `HeartRateState` 当历史事实。
- 如果 BLE HRS 可用，后续优先拆 Android BLE HRS adapter spike。
- 如果 BLE HRS 不可用，再验证 Huawei Health Kit / Health Service Kit 的实时授权读取可行性。
- 如果只能通过华为运动健康查看或同步历史数据，则不作为执行页实时心率来源。
- 设备数据必须经 `HeartRateProvider` adapter 输出统一 `HeartRateState`；来源必须标注 `sourceKind`、`sourceId` / `sourceLabel`。
- 不做医疗判断、危险告警、训练中断依据或康复结论。
- E11.3 已撤销首版手动输入，不倒灌到 E11.2。

### 12.2 首版 UI 约束

- E11.3 后训练执行页、历史页和趋势页不消费 `HeartRateState`，不绑定具体手环 SDK、Health Connect、Wear OS、BLE 或厂商 SDK。
- E11.1 只允许 disabled / mock / source-unavailable provider 抽象，不接真实设备、HealthKit、Huawei Health Kit / Health Service Kit、BLE 或厂商 SDK，不实现或保留手动输入 UI，不持久化心率记录。
- 没有设备数据且没有手动数据时，执行页、历史页或趋势入口也不显示“未获取心率”占位；首版直接隐藏心率能力。
- 手动输入已撤销，不作为训练记录或趋势页前置。
- `warningLevel` 口径废弃，不再通过 `HeartRateState` 表达医疗、危险、强告警或训练中断判断。
- `HeartRateState` 不得直接进入历史趋势事实；它只描述执行页当下可展示的来源、数值和不可用状态。

### 12.3 后续持久化与平均心率趋势边界

当前没有持久化心率记录模型，且 E11.3 后首版不再规划平均心率趋势。若未来重新进入健康设备阶段，必须另行设计 `WorkoutSession.heartRateSummary` 或独立 `heart_rate_samples` / `heart_rate_records`。

```ts
interface WorkoutSessionHeartRateSummary {
  sourceKind: "device" | "manual";
  averageBpm: number;
  measuredAt?: string;
  recordedAt: string;
  sampleCount: number;
  sourceId?: string;
  sourceLabel?: string;
}

interface HeartRateSample {
  sessionId: string;
  sourceKind: "device" | "manual";
  bpm: number;
  measuredAt: string;
  recordedAt: string;
  sampleCount?: number;
  sourceId?: string;
  sourceLabel?: string;
}
```

- 设备数据只能作为未来心率趋势的优先来源；手动数据如重新引入，只能作为明确标注来源的可选补充。
- 未来任何持久化 sample / summary 都必须至少保留 `sourceKind`、`sourceLabel`、`sourceId`、`sampleCount`、`measuredAt` 和 `recordedAt` 边界；缺少这些边界时只能作为不可比较或不可绘制数据处理。
- 无明确 `sourceKind`、无 `bpm` / `averageBpm`、无 `measuredAt` / `recordedAt` 或无 `sampleCount` 边界时，不绘制平均心率趋势。
- 平均心率趋势不得从执行页瞬时 `HeartRateState` 反推，不得绘制假趋势。
- 心率趋势不得输出医疗判断、危险告警、训练中断依据、康复结论或强弱判断。

## 13. 恢复建议模型

首版恢复建议先用肌群与恢复区域映射：

```ts
interface RecoveryArea {
  id: string;
  name: string;
  bodyRegion: "front" | "back" | "upper" | "lower" | "full";
  summary: string;
  media?: MediaAssetRef[];
  cautionText?: string;
}

interface RecoveryRecommendation {
  sessionId: string;
  trainedMuscleIds: string[];
  areaIds: string[];
  contentIds?: string[];
}
```

## 14. 前端原型假数据标准

第一轮前端原型建议准备以下 fixture：

1. 8 到 12 个标准动作。
2. 1 个计时训练计划。
3. 1 个力量训练计划。
4. 1 个基础跟练计划。
5. 1 个完成的计时训练会话。
6. 1 个完成的力量训练会话。
7. 5 种心率状态。
8. 1 组恢复建议数据。

### 14.1 首批动作 fixture 覆盖建议

fixture 不追求内容量，先覆盖模型差异：

- 徒手计时动作。
- 哑铃力量动作。
- 杠铃力量动作。
- 单侧动作。
- 热身动作。
- 拉伸动作。
- 有替代动作的动作。
- 可用于跟练展示的动作。

### 14.2 E1.2 fixture 对齐策略

E1.2 导入首批 11 个动作 fixture 时采用以下约束：

- `Exercise.sourceMeta` 按本文档保留，并在首批 fixture 中指向 `docs/planning/action-content-slice.md`。
- `Exercise.extensions` 保留为空对象/空 map，不把审核备注、默认训练建议或后续内容准备字段塞进核心动作契约。
- `training type support`、`onboarding suitability`、`timed default suggestion`、`strength default suggestion` 和 `review notes` 属于 fixture-only 元数据，服务 E1.2 校验和后续计划编辑默认值输入，不作为正式 `Exercise` 字段。
- `prototype/src/data/contracts.ts` 应与本文 `Exercise` 的 `sourceMeta`/`extensions` 字段保持一致；后续如要继续调整 `WorkoutCommand` 细节，应另起 story 对齐。

## 15. 明确不在核心模型里硬编码的能力

以下能力首版留扩展位，不应让核心训练计划模型过早依赖：

- 教练课程商业体系。
- 课程定价与订阅。
- AI 实时纠错结果格式细节。
- 具体音乐播放器与节拍算法。
- 具体手环厂商字段。
- 具体语音识别提供方。
- 医疗级心率告警规则。

## 16. 后续映射建议

### 16.1 前端原型

- 可直接按本文 TypeScript 风格接口准备 mock 数据。
- UI 原型先消费抽象 `WorkoutPlan`、`WorkoutSession`、`WorkoutCommand`、`WorkoutEvent` 和 `HeartRateState`。

### 16.2 Android 架构

- 将公共模型映射为 Kotlin data classes。
- 将训练命令、训练事件和执行状态机放在业务层。
- 将通知、声音、震动、健康数据和设备适配放在平台层或适配层。

### 16.3 iOS 与跨平台

- 保持计划模型、训练会话模型和事件语义稳定。
- 健康数据、蓝牙、通知和音频播放继续作为平台能力适配。
