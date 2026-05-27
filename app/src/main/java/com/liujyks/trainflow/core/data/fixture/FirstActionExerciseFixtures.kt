package com.liujyks.trainflow.core.data.fixture

import com.liujyks.trainflow.core.model.ContentSourceMeta
import com.liujyks.trainflow.core.model.ContentStatus
import com.liujyks.trainflow.core.model.EquipmentKind
import com.liujyks.trainflow.core.model.Exercise
import com.liujyks.trainflow.core.model.ExerciseCapabilities
import com.liujyks.trainflow.core.model.ExerciseDifficulty
import com.liujyks.trainflow.core.model.ExerciseInstructionContent
import com.liujyks.trainflow.core.model.ExerciseRecoveryMapping
import com.liujyks.trainflow.core.model.ExerciseRole
import com.liujyks.trainflow.core.model.ExerciseSide
import com.liujyks.trainflow.core.model.ExerciseSubstitution
import com.liujyks.trainflow.core.model.RepTarget

object FirstActionExerciseFixtures {
    private val reviewedSourceMeta = ContentSourceMeta(
        author = "TrainFlow E1.1 content slice",
        reviewer = "TrainFlow content review",
        sourceRefs = listOf("docs/planning/action-content-slice.md"),
        updatedAt = "2026-05-27"
    )

    val entries: List<ActionExerciseFixture> = listOf(
        ActionExerciseFixture(
            exercise = Exercise(
                id = "jumping-jacks",
                name = "开合跳",
                aliases = listOf("Jumping Jacks"),
                category = "warmup",
                primaryMuscleIds = listOf("full_body", "calves"),
                secondaryMuscleIds = listOf("shoulders"),
                equipment = listOf(EquipmentKind.BODYWEIGHT),
                difficulty = ExerciseDifficulty.BEGINNER,
                roles = listOf(ExerciseRole.WARMUP, ExerciseRole.MAIN),
                capabilities = capabilities(
                    timed = true,
                    followAlong = true,
                    warmup = true,
                    circuit = true
                ),
                instructions = ExerciseInstructionContent(
                    shortCue = "轻落地，手脚同步打开。",
                    steps = listOf(
                        "站立，双脚并拢，双臂自然放在身体两侧。",
                        "双脚向两侧跳开的同时双臂向上打开。",
                        "轻落地后按稳定节奏回到起始姿势。"
                    ),
                    keyPoints = listOf(
                        "保持躯干直立。",
                        "脚掌轻触地面。",
                        "按能持续完成的节奏训练。"
                    ),
                    commonMistakes = listOf(
                        "落地过重。",
                        "耸肩甩手。",
                        "膝盖内扣。",
                        "节奏过快导致动作变形。"
                    ),
                    breathingCues = listOf("保持自然呼吸，不要为了速度憋气。"),
                    cautions = listOf("如跳跃让膝踝不适，可改为左右点步开合。")
                ),
                recovery = ExerciseRecoveryMapping(
                    trainedMuscleIds = listOf("full_body", "calves"),
                    recommendedRecoveryAreaIds = listOf("lower-body-release")
                ),
                substitutions = listOf(
                    ExerciseSubstitution(
                        exerciseId = "bodyweight-squat",
                        reasonTags = listOf("low_impact")
                    )
                ),
                contentStatus = ContentStatus.REVIEWED,
                sourceMeta = reviewedSourceMeta
            ),
            trainingTypeSupport = TrainingTypeSupport.TIMED,
            onboardingSuitable = true,
            timedDefault = TimedDefaultSuggestion(workDurationSec = 30, restAfterSec = 15, minRounds = 1, maxRounds = 2),
            reviewNotes = listOf("低冲击替代只作为普通动作替换，不基于心率或医疗判断。")
        ),
        ActionExerciseFixture(
            exercise = Exercise(
                id = "bodyweight-squat",
                name = "徒手深蹲",
                aliases = listOf("Air Squat"),
                category = "bodyweight",
                primaryMuscleIds = listOf("quads", "glutes"),
                secondaryMuscleIds = listOf("core", "hamstrings"),
                equipment = listOf(EquipmentKind.BODYWEIGHT),
                difficulty = ExerciseDifficulty.BEGINNER,
                roles = listOf(ExerciseRole.MAIN, ExerciseRole.WARMUP),
                capabilities = capabilities(
                    timed = true,
                    reps = true,
                    followAlong = true,
                    circuit = true
                ),
                instructions = ExerciseInstructionContent(
                    shortCue = "膝盖跟脚尖，臀部向后坐。",
                    steps = listOf(
                        "双脚约与肩同宽，脚尖自然外展。",
                        "胸口打开，视线看向前下方。",
                        "臀部向后向下坐，下蹲到可控深度后站起。"
                    ),
                    keyPoints = listOf(
                        "脚跟稳定贴地。",
                        "躯干保持可控。",
                        "站起时臀腿一起发力。"
                    ),
                    commonMistakes = listOf(
                        "膝盖明显内扣。",
                        "脚跟抬起。",
                        "为了追求深度而弓背。",
                        "起身时先抬臀。"
                    ),
                    breathingCues = listOf("下蹲吸气，站起呼气。"),
                    cautions = listOf("只做到能稳定控制的深度；如膝盖不适，缩小幅度或换臀桥。")
                ),
                recovery = ExerciseRecoveryMapping(
                    trainedMuscleIds = listOf("quads", "glutes"),
                    recommendedRecoveryAreaIds = listOf("lower-body-release")
                ),
                substitutions = listOf(
                    ExerciseSubstitution(
                        exerciseId = "glute-bridge",
                        reasonTags = listOf("lower_knee_load")
                    )
                ),
                contentStatus = ContentStatus.REVIEWED,
                sourceMeta = reviewedSourceMeta
            ),
            trainingTypeSupport = TrainingTypeSupport.BOTH,
            onboardingSuitable = true,
            timedDefault = TimedDefaultSuggestion(workDurationSec = 40, restAfterSec = 20, minRounds = 2, maxRounds = 3),
            strengthDefault = StrengthDefaultSuggestion(
                sets = 3,
                repTarget = RepTarget.Range(),
                restAfterSetSec = 60,
                weightStrategy = WeightInputStrategy.NONE
            )
        ),
        ActionExerciseFixture(
            exercise = Exercise(
                id = "incline-push-up",
                name = "上斜俯卧撑",
                aliases = listOf("Incline Push-up"),
                category = "bodyweight",
                primaryMuscleIds = listOf("chest", "triceps"),
                secondaryMuscleIds = listOf("shoulders", "core"),
                equipment = listOf(EquipmentKind.BODYWEIGHT),
                difficulty = ExerciseDifficulty.BEGINNER,
                roles = listOf(ExerciseRole.MAIN),
                capabilities = capabilities(
                    timed = true,
                    reps = true,
                    followAlong = true,
                    circuit = true
                ),
                instructions = ExerciseInstructionContent(
                    shortCue = "身体成一直线，胸口靠近支撑面。",
                    steps = listOf(
                        "双手撑在稳固高台或长凳上，手掌略宽于肩。",
                        "身体从头到脚保持一条线。",
                        "弯肘让胸口靠近支撑面，再推回起始位置。"
                    ),
                    keyPoints = listOf(
                        "肘部约向身体斜后方打开。",
                        "肩胛保持稳定。",
                        "动作幅度以可控为先。"
                    ),
                    commonMistakes = listOf(
                        "塌腰。",
                        "耸肩。",
                        "只点头不屈肘。",
                        "支撑物不稳。"
                    ),
                    breathingCues = listOf("下放吸气，推起呼气。"),
                    cautions = listOf("支撑物必须稳固；腕部不适时调整手掌角度或缩短幅度。")
                ),
                recovery = ExerciseRecoveryMapping(
                    trainedMuscleIds = listOf("chest", "triceps"),
                    recommendedRecoveryAreaIds = listOf("chest-shoulder-release")
                ),
                substitutions = listOf(
                    ExerciseSubstitution(
                        exerciseId = "forearm-plank",
                        reasonTags = listOf("core_stability")
                    )
                ),
                contentStatus = ContentStatus.REVIEWED,
                sourceMeta = reviewedSourceMeta
            ),
            trainingTypeSupport = TrainingTypeSupport.BOTH,
            onboardingSuitable = true,
            timedDefault = TimedDefaultSuggestion(workDurationSec = 30, restAfterSec = 20),
            strengthDefault = StrengthDefaultSuggestion(
                sets = 3,
                repTarget = RepTarget.Range(),
                restAfterSetSec = 75,
                weightStrategy = WeightInputStrategy.NONE
            )
        ),
        ActionExerciseFixture(
            exercise = Exercise(
                id = "forearm-plank",
                name = "平板支撑",
                aliases = listOf("Plank"),
                category = "bodyweight",
                primaryMuscleIds = listOf("core"),
                secondaryMuscleIds = listOf("shoulders", "glutes"),
                equipment = listOf(EquipmentKind.BODYWEIGHT, EquipmentKind.MAT),
                difficulty = ExerciseDifficulty.BEGINNER,
                roles = listOf(ExerciseRole.MAIN),
                capabilities = capabilities(
                    timed = true,
                    followAlong = true,
                    circuit = true
                ),
                instructions = ExerciseInstructionContent(
                    shortCue = "肘在肩下，腹臀收紧。",
                    steps = listOf(
                        "前臂撑地，肘在肩下方。",
                        "双脚向后伸直，身体从头到脚保持一条线。",
                        "轻轻收紧腹部和臀部，稳定呼吸。"
                    ),
                    keyPoints = listOf(
                        "颈部自然延伸。",
                        "肩膀远离耳朵。",
                        "用稳定呼吸维持动作。"
                    ),
                    commonMistakes = listOf(
                        "臀部过高。",
                        "腰部下塌。",
                        "憋气。",
                        "肩膀前顶。"
                    ),
                    breathingCues = listOf("保持平稳呼吸，避免憋气硬撑。"),
                    cautions = listOf("腰背不适时缩短时长或改为跪姿支撑。")
                ),
                recovery = ExerciseRecoveryMapping(
                    trainedMuscleIds = listOf("core"),
                    recommendedRecoveryAreaIds = listOf("core-breathing-reset")
                ),
                substitutions = listOf(
                    ExerciseSubstitution(
                        exerciseId = "glute-bridge",
                        reasonTags = listOf("lower_pressure")
                    )
                ),
                contentStatus = ContentStatus.REVIEWED,
                sourceMeta = reviewedSourceMeta
            ),
            trainingTypeSupport = TrainingTypeSupport.TIMED,
            onboardingSuitable = true,
            timedDefault = TimedDefaultSuggestion(workDurationSec = 25, restAfterSec = 20),
            reviewNotes = listOf("不把坚持时间表述为疼痛忍耐目标。")
        ),
        ActionExerciseFixture(
            exercise = Exercise(
                id = "alternating-reverse-lunge",
                name = "交替后撤弓步",
                aliases = listOf("Alternating Reverse Lunge"),
                category = "bodyweight",
                primaryMuscleIds = listOf("quads", "glutes"),
                secondaryMuscleIds = listOf("hamstrings", "core"),
                equipment = listOf(EquipmentKind.BODYWEIGHT),
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                roles = listOf(ExerciseRole.MAIN),
                capabilities = capabilities(
                    timed = true,
                    reps = true,
                    followAlong = true,
                    circuit = true,
                    unilateral = true
                ),
                instructions = ExerciseInstructionContent(
                    shortCue = "前脚踩稳，向后撤一步再站回。",
                    steps = listOf(
                        "站立，双脚与髋同宽。",
                        "一脚向后撤，保持躯干直立。",
                        "前脚稳定发力站回，左右交替进行。"
                    ),
                    keyPoints = listOf(
                        "前膝跟随脚尖。",
                        "后撤距离以躯干稳定为准。",
                        "左右节奏一致。"
                    ),
                    commonMistakes = listOf(
                        "前膝内扣。",
                        "后脚落点太窄导致摇晃。",
                        "身体前倾过多。",
                        "用后脚蹬地抢动作。"
                    ),
                    breathingCues = listOf("下撤吸气，站回呼气。"),
                    cautions = listOf("平衡不足时先扶墙或改为徒手深蹲；不追求膝盖触地。")
                ),
                recovery = ExerciseRecoveryMapping(
                    trainedMuscleIds = listOf("quads", "glutes"),
                    recommendedRecoveryAreaIds = listOf("lower-body-release")
                ),
                substitutions = listOf(
                    ExerciseSubstitution(exerciseId = "bodyweight-squat", reasonTags = listOf("bilateral")),
                    ExerciseSubstitution(exerciseId = "glute-bridge", reasonTags = listOf("low_impact"))
                ),
                contentStatus = ContentStatus.REVIEWED,
                sourceMeta = reviewedSourceMeta
            ),
            trainingTypeSupport = TrainingTypeSupport.BOTH,
            onboardingSuitable = false,
            timedDefault = TimedDefaultSuggestion(workDurationSec = 40, restAfterSec = 20, side = ExerciseSide.ALTERNATING),
            strengthDefault = StrengthDefaultSuggestion(
                sets = 3,
                repTarget = RepTarget.Range(minReps = 8, maxReps = 10),
                restAfterSetSec = 75,
                weightStrategy = WeightInputStrategy.NONE,
                perSide = true
            )
        ),
        ActionExerciseFixture(
            exercise = Exercise(
                id = "glute-bridge",
                name = "臀桥",
                aliases = listOf("Glute Bridge"),
                category = "bodyweight",
                primaryMuscleIds = listOf("glutes"),
                secondaryMuscleIds = listOf("hamstrings", "core"),
                equipment = listOf(EquipmentKind.BODYWEIGHT, EquipmentKind.MAT),
                difficulty = ExerciseDifficulty.BEGINNER,
                roles = listOf(ExerciseRole.WARMUP, ExerciseRole.MAIN),
                capabilities = capabilities(
                    timed = true,
                    reps = true,
                    followAlong = true,
                    warmup = true,
                    circuit = true
                ),
                instructions = ExerciseInstructionContent(
                    shortCue = "臀部发力抬髋，控制下放。",
                    steps = listOf(
                        "仰卧屈膝，双脚踩地约与髋同宽。",
                        "脚跟靠近臀部但保持舒适距离。",
                        "收紧臀部把髋部抬起，再控制下放。"
                    ),
                    keyPoints = listOf(
                        "顶部停顿一瞬。",
                        "肋骨不过度外翻。",
                        "脚跟稳定踩地。"
                    ),
                    commonMistakes = listOf(
                        "用腰顶起。",
                        "脚离身体太远。",
                        "顶部过度挺腰。",
                        "下放失控。"
                    ),
                    breathingCues = listOf("抬髋呼气，下放吸气。"),
                    cautions = listOf("腰部不适时减小幅度；不要把动作做成腰部反复挤压。")
                ),
                recovery = ExerciseRecoveryMapping(
                    trainedMuscleIds = listOf("glutes", "hamstrings"),
                    recommendedRecoveryAreaIds = listOf("posterior-chain-release")
                ),
                substitutions = listOf(
                    ExerciseSubstitution(exerciseId = "bodyweight-squat", reasonTags = listOf("standing_lower_body")),
                    ExerciseSubstitution(exerciseId = "forearm-plank", reasonTags = listOf("mat_core"))
                ),
                contentStatus = ContentStatus.REVIEWED,
                sourceMeta = reviewedSourceMeta
            ),
            trainingTypeSupport = TrainingTypeSupport.BOTH,
            onboardingSuitable = true,
            timedDefault = TimedDefaultSuggestion(workDurationSec = 35, restAfterSec = 15),
            strengthDefault = StrengthDefaultSuggestion(
                sets = 3,
                repTarget = RepTarget.Range(minReps = 10, maxReps = 15),
                restAfterSetSec = 60,
                weightStrategy = WeightInputStrategy.NONE
            )
        ),
        ActionExerciseFixture(
            exercise = Exercise(
                id = "dumbbell-goblet-squat",
                name = "哑铃杯式深蹲",
                aliases = listOf("Goblet Squat"),
                category = "strength",
                primaryMuscleIds = listOf("quads", "glutes"),
                secondaryMuscleIds = listOf("core", "upper_back"),
                equipment = listOf(EquipmentKind.DUMBBELL),
                difficulty = ExerciseDifficulty.BEGINNER,
                roles = listOf(ExerciseRole.MAIN),
                capabilities = capabilities(reps = true, weight = true),
                instructions = ExerciseInstructionContent(
                    shortCue = "哑铃贴近胸前，稳定下蹲站起。",
                    steps = listOf(
                        "双手托住哑铃一端或贴近胸前。",
                        "双脚约与肩同宽，核心收紧。",
                        "保持哑铃靠近身体，下蹲后稳定推地站起。"
                    ),
                    keyPoints = listOf(
                        "核心收紧。",
                        "膝盖跟脚尖。",
                        "哑铃不要远离身体。"
                    ),
                    commonMistakes = listOf(
                        "哑铃前坠。",
                        "下蹲时弓背。",
                        "脚跟抬起。",
                        "起身时膝盖内扣。"
                    ),
                    breathingCues = listOf("下蹲吸气，站起呼气。"),
                    cautions = listOf("选择可稳定控制的重量；拿放哑铃时先站稳。")
                ),
                recovery = ExerciseRecoveryMapping(
                    trainedMuscleIds = listOf("quads", "glutes"),
                    recommendedRecoveryAreaIds = listOf("lower-body-release")
                ),
                substitutions = listOf(
                    ExerciseSubstitution(
                        exerciseId = "bodyweight-squat",
                        reasonTags = listOf("no_equipment"),
                        equipmentFallback = true
                    )
                ),
                contentStatus = ContentStatus.REVIEWED,
                sourceMeta = reviewedSourceMeta
            ),
            trainingTypeSupport = TrainingTypeSupport.STRENGTH,
            onboardingSuitable = true,
            strengthDefault = StrengthDefaultSuggestion(
                sets = 3,
                repTarget = RepTarget.Range(),
                restAfterSetSec = 90,
                weightStrategy = WeightInputStrategy.USER_ENTERED
            ),
            reviewNotes = listOf("首批不推荐作为计时默认动作，避免暗示负重计时循环。")
        ),
        ActionExerciseFixture(
            exercise = Exercise(
                id = "one-arm-dumbbell-row",
                name = "单臂哑铃划船",
                aliases = listOf("One-arm Dumbbell Row"),
                category = "strength",
                primaryMuscleIds = listOf("lats", "upper_back"),
                secondaryMuscleIds = listOf("biceps", "core"),
                equipment = listOf(EquipmentKind.DUMBBELL),
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                roles = listOf(ExerciseRole.MAIN),
                capabilities = capabilities(reps = true, weight = true, unilateral = true),
                instructions = ExerciseInstructionContent(
                    shortCue = "背平，手肘向后拉。",
                    steps = listOf(
                        "一手扶稳定支撑面，另一手握哑铃。",
                        "背部保持平直，髋部稳定。",
                        "手肘向身体后侧拉，哑铃靠近躯干，再控制下放。"
                    ),
                    keyPoints = listOf(
                        "肩胛先稳定再拉。",
                        "哑铃路线贴近身体。",
                        "左右重量与次数分别记录。"
                    ),
                    commonMistakes = listOf(
                        "转体借力。",
                        "耸肩。",
                        "手腕拉得比手肘高。",
                        "下放过快。"
                    ),
                    breathingCues = listOf("上拉呼气，下放吸气。"),
                    cautions = listOf("支撑面必须稳固；腰背无法稳定时减重或换更高支撑。")
                ),
                recovery = ExerciseRecoveryMapping(
                    trainedMuscleIds = listOf("lats", "upper_back"),
                    recommendedRecoveryAreaIds = listOf("upper-back-release")
                ),
                contentStatus = ContentStatus.REVIEWED,
                sourceMeta = reviewedSourceMeta
            ),
            trainingTypeSupport = TrainingTypeSupport.STRENGTH,
            onboardingSuitable = false,
            strengthDefault = StrengthDefaultSuggestion(
                sets = 3,
                repTarget = RepTarget.Range(),
                restAfterSetSec = 90,
                weightStrategy = WeightInputStrategy.USER_ENTERED,
                perSide = true
            ),
            reviewNotes = listOf("首批暂无无器械 fallback，后续内容扩展可补弹力带或器械划船。")
        ),
        ActionExerciseFixture(
            exercise = Exercise(
                id = "dumbbell-romanian-deadlift",
                name = "哑铃罗马尼亚硬拉",
                aliases = listOf("Dumbbell Romanian Deadlift"),
                category = "strength",
                primaryMuscleIds = listOf("hamstrings", "glutes"),
                secondaryMuscleIds = listOf("upper_back", "core"),
                equipment = listOf(EquipmentKind.DUMBBELL),
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                roles = listOf(ExerciseRole.MAIN),
                capabilities = capabilities(reps = true, weight = true),
                instructions = ExerciseInstructionContent(
                    shortCue = "髋向后折，哑铃贴腿走。",
                    steps = listOf(
                        "双手各持一只哑铃站立，膝盖微屈。",
                        "哑铃靠近大腿前侧，背部保持中立。",
                        "髋部向后折叠，哑铃沿腿前侧下放，再用臀腿发力站回。"
                    ),
                    keyPoints = listOf(
                        "背部保持中立。",
                        "动作来自髋部而不是弯腰。",
                        "下放深度以腿后侧拉伸和背部稳定为准。"
                    ),
                    commonMistakes = listOf(
                        "弯腰够地。",
                        "哑铃远离身体。",
                        "膝盖完全锁死。",
                        "站起时过度后仰。"
                    ),
                    breathingCues = listOf("下放吸气，站回呼气。"),
                    cautions = listOf("初次使用从轻重量开始；腰背不适时停止本动作并选择低负担替代。")
                ),
                recovery = ExerciseRecoveryMapping(
                    trainedMuscleIds = listOf("hamstrings", "glutes"),
                    recommendedRecoveryAreaIds = listOf("posterior-chain-release")
                ),
                substitutions = listOf(
                    ExerciseSubstitution(
                        exerciseId = "glute-bridge",
                        reasonTags = listOf("lower_load"),
                        equipmentFallback = true
                    )
                ),
                contentStatus = ContentStatus.REVIEWED,
                sourceMeta = reviewedSourceMeta
            ),
            trainingTypeSupport = TrainingTypeSupport.STRENGTH,
            onboardingSuitable = false,
            strengthDefault = StrengthDefaultSuggestion(
                sets = 3,
                repTarget = RepTarget.Range(),
                restAfterSetSec = 90,
                weightStrategy = WeightInputStrategy.USER_ENTERED
            )
        ),
        ActionExerciseFixture(
            exercise = Exercise(
                id = "barbell-bench-press",
                name = "杠铃卧推",
                aliases = listOf("Bench Press"),
                category = "strength",
                primaryMuscleIds = listOf("chest", "triceps"),
                secondaryMuscleIds = listOf("shoulders"),
                equipment = listOf(EquipmentKind.BARBELL),
                difficulty = ExerciseDifficulty.INTERMEDIATE,
                roles = listOf(ExerciseRole.MAIN),
                capabilities = capabilities(reps = true, weight = true),
                instructions = ExerciseInstructionContent(
                    shortCue = "脚踩稳，肩胛稳，控制下放再推起。",
                    steps = listOf(
                        "仰卧在卧推凳上，双脚稳定踩地。",
                        "握距略宽于肩，杠铃位于眼睛上方附近。",
                        "取杠后控制下降到胸前可控位置，再向上推回。"
                    ),
                    keyPoints = listOf(
                        "手腕保持稳定。",
                        "杠铃路径可控。",
                        "正式组前可设置热身组。"
                    ),
                    commonMistakes = listOf(
                        "弹胸借力。",
                        "肩膀前顶。",
                        "握距过宽导致肩部不适。",
                        "无人保护时挑战极限重量。"
                    ),
                    breathingCues = listOf("下放吸气，推起呼气。"),
                    cautions = listOf("重量训练应使用安全架或保护者；首版只记录训练目标和结果。")
                ),
                recovery = ExerciseRecoveryMapping(
                    trainedMuscleIds = listOf("chest", "triceps"),
                    recommendedRecoveryAreaIds = listOf("chest-shoulder-release")
                ),
                substitutions = listOf(
                    ExerciseSubstitution(
                        exerciseId = "incline-push-up",
                        reasonTags = listOf("no_equipment", "lower_load"),
                        equipmentFallback = true
                    )
                ),
                contentStatus = ContentStatus.REVIEWED,
                sourceMeta = reviewedSourceMeta
            ),
            trainingTypeSupport = TrainingTypeSupport.STRENGTH,
            onboardingSuitable = false,
            strengthDefault = StrengthDefaultSuggestion(
                sets = 3,
                repTarget = RepTarget.Range(),
                restAfterSetSec = 120,
                weightStrategy = WeightInputStrategy.USER_ENTERED,
                warmupSets = 1
            ),
            reviewNotes = listOf("不提供最大重量判断；正式组安全边界留给训练记录和用户确认。")
        ),
        ActionExerciseFixture(
            exercise = Exercise(
                id = "standing-quad-stretch",
                name = "站姿股四头肌拉伸",
                aliases = listOf("Standing Quad Stretch"),
                category = "stretch",
                primaryMuscleIds = listOf("quads", "hip_flexors"),
                equipment = listOf(EquipmentKind.BODYWEIGHT),
                difficulty = ExerciseDifficulty.BEGINNER,
                roles = listOf(ExerciseRole.STRETCH, ExerciseRole.RECOVERY),
                capabilities = capabilities(
                    timed = true,
                    followAlong = true,
                    stretch = true,
                    unilateral = true
                ),
                instructions = ExerciseInstructionContent(
                    shortCue = "扶稳，双膝靠近，温和拉伸大腿前侧。",
                    steps = listOf(
                        "站立，可扶墙保持平衡。",
                        "一侧膝盖弯曲，手扶同侧脚背或脚踝。",
                        "双膝尽量靠近，骨盆保持稳定，左右分别完成。"
                    ),
                    keyPoints = listOf(
                        "不要强拉脚踝。",
                        "身体保持直立。",
                        "拉伸保持可自然呼吸。"
                    ),
                    commonMistakes = listOf(
                        "腰部前顶。",
                        "膝盖向外打开过多。",
                        "用力拉到疼痛。",
                        "站立不稳仍强行保持。"
                    ),
                    breathingCues = listOf("保持自然呼吸，不要屏气拉伸。"),
                    cautions = listOf("拉伸只做到温和紧张，不追求疼痛；平衡不足时改为侧卧拉伸。")
                ),
                recovery = ExerciseRecoveryMapping(
                    trainedMuscleIds = listOf("quads", "hip_flexors"),
                    recommendedRecoveryAreaIds = listOf("lower-body-release")
                ),
                contentStatus = ContentStatus.REVIEWED,
                sourceMeta = reviewedSourceMeta
            ),
            trainingTypeSupport = TrainingTypeSupport.TIMED,
            onboardingSuitable = true,
            timedDefault = TimedDefaultSuggestion(
                workDurationSec = 30,
                restAfterSec = 5,
                side = ExerciseSide.LEFT
            ),
            reviewNotes = listOf("平衡 fallback 后续补充，不在首批 11 个动作内静默造新动作。")
        )
    )

    val exercises: List<Exercise> = entries.map { it.exercise }

    private fun capabilities(
        timed: Boolean = false,
        reps: Boolean = false,
        weight: Boolean = false,
        followAlong: Boolean = false,
        warmup: Boolean = false,
        stretch: Boolean = false,
        circuit: Boolean = false,
        unilateral: Boolean = false
    ) = ExerciseCapabilities(
        supportsTimedTraining = timed,
        supportsReps = reps,
        supportsWeight = weight,
        supportsFollowAlong = followAlong,
        supportsWarmupRole = warmup,
        supportsStretchRole = stretch,
        supportsCircuitRole = circuit,
        isUnilateral = unilateral
    )
}

data class ActionExerciseFixture(
    val exercise: Exercise,
    val trainingTypeSupport: TrainingTypeSupport,
    val onboardingSuitable: Boolean,
    val timedDefault: TimedDefaultSuggestion? = null,
    val strengthDefault: StrengthDefaultSuggestion? = null,
    val reviewNotes: List<String> = emptyList()
)

enum class TrainingTypeSupport {
    TIMED,
    STRENGTH,
    BOTH
}

data class TimedDefaultSuggestion(
    val workDurationSec: Int,
    val restAfterSec: Int,
    val minRounds: Int = 1,
    val maxRounds: Int = 1,
    val side: ExerciseSide? = null
)

data class StrengthDefaultSuggestion(
    val sets: Int,
    val repTarget: RepTarget,
    val restAfterSetSec: Int,
    val weightStrategy: WeightInputStrategy,
    val perSide: Boolean = false,
    val warmupSets: Int = 0
)

enum class WeightInputStrategy {
    NONE,
    USER_ENTERED
}
