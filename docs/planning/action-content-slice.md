# TrainFlow E1.1 首批动作内容切片

**状态:** E1.1 accepted content slice
**日期:** 2026-05-27
**范围:** 为 E1.2 动作 fixture、动作库筛选、动作详情和计划编辑提供稳定输入。本文档不实现导入流程、训练引擎、repository、UI 闭环、通知、真实心率、语音或跟练闭环。

## 1. 切片目标

首批动作以计时训练默认入口为优先，同时覆盖力量训练最小闭环。数量控制为 11 个，覆盖：

- 徒手计时动作。
- 哑铃力量动作。
- 杠铃力量动作。
- 单侧动作。
- 热身动作。
- 拉伸动作。
- 可替代动作。
- 可跟练动作。

首批内容不是课程库，也不要求完整视频体系。媒体字段首版可为空，但每个动作必须先具备足够用于列表、筛选、详情、训练中短提示、计划编辑、基础恢复映射和替代动作的文本内容。

## 2. 字段标准

### 2.1 与 `Exercise` contract 对齐的必填字段

每个进入 E1.2 fixture 的动作必须具备以下字段：

| 字段 | E1.1 要求 |
|---|---|
| `id` | 稳定英文 kebab-case，例如 `bodyweight-squat`。 |
| `name` | 简洁中文名称。 |
| `aliases` | 可选，用于常见别名；没有别名可省略。 |
| `category` | 内容分类，建议首批使用 `warmup`、`bodyweight`、`strength`、`stretch`。 |
| `primaryMuscleIds` | 至少 1 个稳定部位 ID。 |
| `secondaryMuscleIds` | 可选，但复合动作建议补充。 |
| `equipment` | 使用既有 `EquipmentKind`：`bodyweight`、`dumbbell`、`barbell`、`mat` 等。 |
| `difficulty` | `beginner`、`intermediate` 或 `advanced`。首批避免高级动作。 |
| `roles` | 使用 `warmup`、`main`、`stretch`、`recovery`。 |
| `capabilities` | 明确计时、次数、重量、跟练、热身、拉伸、循环、单侧能力。 |
| `instructions.shortCue` | 训练执行页短提示，必须短、可扫读。 |
| `instructions.steps` | 2 到 5 条设置与执行步骤。 |
| `instructions.keyPoints` | 2 到 5 条发力/姿态要点。 |
| `instructions.commonMistakes` | 至少 2 条常见错误。 |
| `instructions.breathingCues` | 建议提供；如不适用可省略。 |
| `instructions.cautions` | 必须提供，使用非医疗、非诊断表达。 |
| `recovery` | 建议提供训练部位到恢复区域的基础映射。 |
| `substitutions` | 有可替代动作时提供，至少覆盖器械 fallback 或难度 fallback。 |
| `contentStatus` | E1.2 内部 fixture 可用 `reviewed`；正式发布前再进入 `published`。 |

### 2.2 E1.1 内容准备字段

以下字段用于内容切片和后续 fixture 导入准备，不要求改变 `Exercise` contract：

| 字段 | 用途 |
|---|---|
| training type support | 标明 `timed`、`strength` 或 `both`，用于理解能力标签组合。 |
| onboarding suitability | 标明是否适合新用户默认推荐。 |
| timed default suggestion | 适用于计时训练时，给出默认动作时长和休息建议。 |
| strength default suggestion | 适用于力量训练时，给出组数、次数、休息和重量输入策略。 |
| review notes | 内容审核时关注的动作边界、风险表达和媒体后补要求。 |

如后续发现这些字段应进入正式数据模型，应先记录为未决事项并更新 `docs/planning/decision-log.md`，不要在 E1.2 静默扩展契约。

## 3. 首批动作总览

| id | 名称 | 支持 | 主要部位 | 器械 | 难度 | 首批用途 |
|---|---|---|---|---|---|---|
| `jumping-jacks` | 开合跳 | timed | full_body、calves | bodyweight | beginner | 热身、计时、跟练 |
| `bodyweight-squat` | 徒手深蹲 | both | quads、glutes | bodyweight | beginner | 计时主动作、力量次数动作 |
| `incline-push-up` | 上斜俯卧撑 | both | chest、triceps | bodyweight | beginner | 新手上肢推、俯卧撑替代 |
| `forearm-plank` | 平板支撑 | timed | core | bodyweight、mat | beginner | 核心计时、跟练 |
| `alternating-reverse-lunge` | 交替后撤弓步 | both | quads、glutes | bodyweight | intermediate | 单侧/交替动作、计时循环 |
| `glute-bridge` | 臀桥 | both | glutes、hamstrings | bodyweight、mat | beginner | 下肢激活、计时或次数 |
| `dumbbell-goblet-squat` | 哑铃杯式深蹲 | strength | quads、glutes | dumbbell | beginner | 哑铃力量下肢 |
| `one-arm-dumbbell-row` | 单臂哑铃划船 | strength | lats、upper_back | dumbbell | intermediate | 单侧力量拉、替代映射 |
| `dumbbell-romanian-deadlift` | 哑铃罗马尼亚硬拉 | strength | hamstrings、glutes | dumbbell | intermediate | 髋铰链力量 |
| `barbell-bench-press` | 杠铃卧推 | strength | chest、triceps | barbell | intermediate | 杠铃力量推、重量记录 |
| `standing-quad-stretch` | 站姿股四头肌拉伸 | timed | quads、hip_flexors | bodyweight | beginner | 拉伸、恢复映射 |

## 4. 动作内容卡片

### 4.1 `jumping-jacks` / 开合跳

- 支持: timed；`supportsTimedTraining=true`、`supportsFollowAlong=true`、`supportsWarmupRole=true`、`supportsCircuitRole=true`。
- 部位: primary `full_body`、`calves`；secondary `shoulders`。
- 器械/难度: `bodyweight`；`beginner`；适合新用户热身默认推荐。
- 角色: `warmup`、`main`。
- 设置指导: 站立，双脚并拢，双臂自然放在身体两侧；预留身侧和头顶空间；膝盖保持微屈。
- 执行提示: 双脚向两侧跳开的同时双臂向上打开；落地轻一点；按稳定节奏回到起始姿势。
- shortCue: `轻落地，手脚同步打开。`
- 要点: 保持躯干直立；脚掌轻触地面；按能持续完成的节奏训练。
- 常见错误: 落地过重；耸肩甩手；膝盖内扣；节奏过快导致动作变形。
- 安全注意: 如跳跃让膝踝不适，可改为左右点步开合；不做心率阈值或医疗判断。
- 计时默认: 30 秒动作，15 秒休息，热身 1 到 2 轮。
- 力量默认: 不作为重量力量动作；如按次数记录，可在后续扩展为 `supportsReps` 但首批不启用。
- 替代: `bodyweight-squat` 可作为低冲击热身替代，reason `low_impact`。
- 恢复映射: trained `full_body`、`calves`；recovery `lower-body-release`。

### 4.2 `bodyweight-squat` / 徒手深蹲

- 支持: both；`supportsTimedTraining=true`、`supportsReps=true`、`supportsWeight=false`、`supportsFollowAlong=true`、`supportsCircuitRole=true`。
- 部位: primary `quads`、`glutes`；secondary `core`、`hamstrings`。
- 器械/难度: `bodyweight`；`beginner`；适合新用户默认推荐。
- 角色: `main`、`warmup`。
- 设置指导: 双脚约与肩同宽，脚尖自然外展；胸口打开，视线看向前下方；双手可前伸帮助平衡。
- 执行提示: 臀部向后向下坐，膝盖跟随脚尖方向；下蹲到可控深度后站起。
- shortCue: `膝盖跟脚尖，臀部向后坐。`
- 要点: 脚跟稳定贴地；躯干保持可控；站起时臀腿一起发力。
- 常见错误: 膝盖明显内扣；脚跟抬起；为了追求深度而弓背；起身时先抬臀。
- 安全注意: 只做到能稳定控制的深度；如膝盖不适，缩小幅度或换臀桥。
- 计时默认: 40 秒动作，20 秒休息，2 到 3 轮。
- 力量默认: 3 组，固定 10 次或 `8-12` 次区间，组间休息 60 秒，不填写重量。
- 替代: `glute-bridge` 可作为膝盖压力较低的下肢替代。
- 恢复映射: trained `quads`、`glutes`；recovery `lower-body-release`。

### 4.3 `incline-push-up` / 上斜俯卧撑

- 支持: both；`supportsTimedTraining=true`、`supportsReps=true`、`supportsWeight=false`、`supportsFollowAlong=true`、`supportsCircuitRole=true`。
- 部位: primary `chest`、`triceps`；secondary `shoulders`、`core`。
- 器械/难度: `bodyweight`；`beginner`；适合新用户上肢推默认推荐。
- 角色: `main`。
- 设置指导: 双手撑在稳固高台或长凳上，手掌略宽于肩；身体从头到脚保持一条线。
- 执行提示: 弯肘让胸口靠近支撑面，再推回起始位置；全程收紧腹部。
- shortCue: `身体成一直线，胸口靠近支撑面。`
- 要点: 肘部约向身体斜后方打开；肩胛保持稳定；动作幅度以可控为先。
- 常见错误: 塌腰；耸肩；只点头不屈肘；支撑物不稳。
- 安全注意: 支撑物必须稳固；腕部不适时调整手掌角度或缩短幅度。
- 计时默认: 30 秒动作，20 秒休息。
- 力量默认: 3 组，`8-12` 次，组间休息 75 秒。
- 替代: `forearm-plank` 作为核心稳定替代；后续可加入标准俯卧撑作为进阶替代。
- 恢复映射: trained `chest`、`triceps`；recovery `chest-shoulder-release`。

### 4.4 `forearm-plank` / 平板支撑

- 支持: timed；`supportsTimedTraining=true`、`supportsReps=false`、`supportsWeight=false`、`supportsFollowAlong=true`、`supportsCircuitRole=true`。
- 部位: primary `core`；secondary `shoulders`、`glutes`。
- 器械/难度: `bodyweight`、`mat`；`beginner`。
- 角色: `main`。
- 设置指导: 前臂撑地，肘在肩下方；双脚向后伸直，身体从头到脚保持一条线。
- 执行提示: 轻轻收紧腹部和臀部，稳定呼吸，保持身体不塌腰不拱背。
- shortCue: `肘在肩下，腹臀收紧。`
- 要点: 颈部自然延伸；肩膀远离耳朵；用稳定呼吸维持动作。
- 常见错误: 臀部过高；腰部下塌；憋气；肩膀前顶。
- 安全注意: 腰背不适时缩短时长或改为跪姿支撑；不把坚持时间作为疼痛忍耐目标。
- 计时默认: 20 到 30 秒动作，20 秒休息。
- 力量默认: 不作为次数/重量动作。
- 替代: `glute-bridge` 可作为低压力核心/臀部控制替代。
- 恢复映射: trained `core`；recovery `core-breathing-reset`。

### 4.5 `alternating-reverse-lunge` / 交替后撤弓步

- 支持: both；`supportsTimedTraining=true`、`supportsReps=true`、`supportsWeight=false`、`supportsFollowAlong=true`、`supportsCircuitRole=true`、`isUnilateral=true`。
- 部位: primary `quads`、`glutes`；secondary `hamstrings`、`core`。
- 器械/难度: `bodyweight`；`intermediate`。
- 角色: `main`。
- 设置指导: 站立，双脚与髋同宽；保持躯干直立，双手可扶髋或放在胸前。
- 执行提示: 一脚向后撤，前脚稳定发力站回；左右交替进行。
- shortCue: `前脚踩稳，向后撤一步再站回。`
- 要点: 前膝跟随脚尖；后撤距离以躯干稳定为准；左右节奏一致。
- 常见错误: 前膝内扣；后脚落点太窄导致摇晃；身体前倾过多；用后脚蹬地抢动作。
- 安全注意: 平衡不足时先扶墙或改为徒手深蹲；不追求膝盖触地。
- 计时默认: 40 秒动作，20 秒休息。
- 力量默认: 3 组，每侧 8 到 10 次，组间休息 75 秒，不填写重量。
- 替代: `bodyweight-squat` 作为非单侧替代；`glute-bridge` 作为低冲击替代。
- 恢复映射: trained `quads`、`glutes`；recovery `lower-body-release`。

### 4.6 `glute-bridge` / 臀桥

- 支持: both；`supportsTimedTraining=true`、`supportsReps=true`、`supportsWeight=false`、`supportsFollowAlong=true`、`supportsWarmupRole=true`、`supportsCircuitRole=true`。
- 部位: primary `glutes`；secondary `hamstrings`、`core`。
- 器械/难度: `bodyweight`、`mat`；`beginner`；适合新用户下肢激活。
- 角色: `warmup`、`main`。
- 设置指导: 仰卧屈膝，双脚踩地约与髋同宽；脚跟靠近臀部但保持舒适距离。
- 执行提示: 收紧臀部把髋部抬起，到肩、髋、膝接近一条线，再控制下放。
- shortCue: `臀部发力抬髋，控制下放。`
- 要点: 顶部停顿一瞬；肋骨不过度外翻；脚跟稳定踩地。
- 常见错误: 用腰顶起；脚离身体太远；顶部过度挺腰；下放失控。
- 安全注意: 腰部不适时减小幅度；不要把动作做成腰部反复挤压。
- 计时默认: 35 秒动作，15 秒休息。
- 力量默认: 3 组，`10-15` 次，组间休息 60 秒。
- 替代: `bodyweight-squat` 作为站姿下肢替代；`forearm-plank` 作为垫上核心替代。
- 恢复映射: trained `glutes`、`hamstrings`；recovery `posterior-chain-release`。

### 4.7 `dumbbell-goblet-squat` / 哑铃杯式深蹲

- 支持: strength；`supportsTimedTraining=false`、`supportsReps=true`、`supportsWeight=true`、`supportsFollowAlong=false`。
- 部位: primary `quads`、`glutes`；secondary `core`、`upper_back`。
- 器械/难度: `dumbbell`；`beginner`。
- 角色: `main`。
- 设置指导: 双手托住哑铃一端或贴近胸前，双脚约与肩同宽。
- 执行提示: 保持哑铃靠近身体，下蹲后用脚掌稳定推地站起。
- shortCue: `哑铃贴近胸前，稳定下蹲站起。`
- 要点: 核心收紧；膝盖跟脚尖；哑铃不要远离身体。
- 常见错误: 哑铃前坠；下蹲时弓背；脚跟抬起；起身时膝盖内扣。
- 安全注意: 选择可稳定控制的重量；拿放哑铃时先站稳。
- 计时默认: 不作为计时默认动作；后续可作为计时循环进阶但首批不推荐。
- 力量默认: 3 组，`8-12` 次，组间休息 90 秒，计划重量由用户填写。
- 替代: `bodyweight-squat` 为无器械 fallback，equipmentFallback `true`。
- 恢复映射: trained `quads`、`glutes`；recovery `lower-body-release`。

### 4.8 `one-arm-dumbbell-row` / 单臂哑铃划船

- 支持: strength；`supportsTimedTraining=false`、`supportsReps=true`、`supportsWeight=true`、`supportsFollowAlong=false`、`isUnilateral=true`。
- 部位: primary `lats`、`upper_back`；secondary `biceps`、`core`。
- 器械/难度: `dumbbell`；`intermediate`。
- 角色: `main`。
- 设置指导: 一手扶稳定支撑面，另一手握哑铃；背部保持平直，髋部稳定。
- 执行提示: 手肘向身体后侧拉，哑铃靠近躯干，再控制下放；左右分别完成。
- shortCue: `背平，手肘向后拉。`
- 要点: 肩胛先稳定再拉；哑铃路线贴近身体；左右重量与次数分别记录。
- 常见错误: 转体借力；耸肩；手腕拉得比手肘高；下放过快。
- 安全注意: 支撑面必须稳固；腰背无法稳定时减重或换更高支撑。
- 计时默认: 不作为计时默认动作。
- 力量默认: 3 组，每侧 `8-12` 次，组间休息 75 到 90 秒，计划重量由用户填写。
- 替代: 后续可加入器械划船；首批无器械 fallback 暂记为未决内容扩展。
- 恢复映射: trained `lats`、`upper_back`；recovery `upper-back-release`。

### 4.9 `dumbbell-romanian-deadlift` / 哑铃罗马尼亚硬拉

- 支持: strength；`supportsTimedTraining=false`、`supportsReps=true`、`supportsWeight=true`、`supportsFollowAlong=false`。
- 部位: primary `hamstrings`、`glutes`；secondary `upper_back`、`core`。
- 器械/难度: `dumbbell`；`intermediate`。
- 角色: `main`。
- 设置指导: 双手各持一只哑铃，站立，膝盖微屈，哑铃靠近大腿前侧。
- 执行提示: 髋部向后折叠，哑铃沿腿前侧下放到可控位置，再用臀腿发力站回。
- shortCue: `髋向后折，哑铃贴腿走。`
- 要点: 背部保持中立；动作来自髋部而不是弯腰；下放深度以腿后侧拉伸和背部稳定为准。
- 常见错误: 弯腰够地；哑铃远离身体；膝盖完全锁死；站起时过度后仰。
- 安全注意: 初次使用从轻重量开始；腰背不适时停止本动作并选择臀桥等低负担替代。
- 计时默认: 不作为计时默认动作。
- 力量默认: 3 组，`8-12` 次，组间休息 90 秒，计划重量由用户填写。
- 替代: `glute-bridge` 为低负担 fallback，equipmentFallback `true`。
- 恢复映射: trained `hamstrings`、`glutes`；recovery `posterior-chain-release`。

### 4.10 `barbell-bench-press` / 杠铃卧推

- 支持: strength；`supportsTimedTraining=false`、`supportsReps=true`、`supportsWeight=true`、`supportsFollowAlong=false`。
- 部位: primary `chest`、`triceps`；secondary `shoulders`。
- 器械/难度: `barbell`；`intermediate`。
- 角色: `main`。
- 设置指导: 仰卧在卧推凳上，双脚稳定踩地；握距略宽于肩，杠铃位于眼睛上方附近。
- 执行提示: 取杠后控制下降到胸前可控位置，再向上推回；全程保持肩胛稳定。
- shortCue: `脚踩稳，肩胛稳，控制下放再推起。`
- 要点: 手腕保持稳定；杠铃路径可控；正式组前可设置热身组。
- 常见错误: 弹胸借力；肩膀前顶；握距过宽导致肩部不适；无人保护时挑战极限重量。
- 安全注意: 重量训练应使用安全架或保护者；首版只记录训练目标和结果，不提供最大重量判断。
- 计时默认: 不作为计时动作。
- 力量默认: 1 到 2 个热身组后 3 个正式组，正式组 `8-12` 次，组间休息 120 秒，计划重量由用户填写。
- 替代: `incline-push-up` 为无器械/低负荷 fallback，equipmentFallback `true`。
- 恢复映射: trained `chest`、`triceps`；recovery `chest-shoulder-release`。

### 4.11 `standing-quad-stretch` / 站姿股四头肌拉伸

- 支持: timed；`supportsTimedTraining=true`、`supportsReps=false`、`supportsWeight=false`、`supportsFollowAlong=true`、`supportsStretchRole=true`、`isUnilateral=true`。
- 部位: primary `quads`、`hip_flexors`。
- 器械/难度: `bodyweight`；`beginner`。
- 角色: `stretch`、`recovery`。
- 设置指导: 站立，可扶墙保持平衡；一侧膝盖弯曲，手扶同侧脚背或脚踝。
- 执行提示: 双膝尽量靠近，骨盆保持稳定，感受大腿前侧温和拉伸；左右分别完成。
- shortCue: `扶稳，双膝靠近，温和拉伸大腿前侧。`
- 要点: 不要强拉脚踝；身体保持直立；拉伸保持可自然呼吸。
- 常见错误: 腰部前顶；膝盖向外打开过多；用力拉到疼痛；站立不稳仍强行保持。
- 安全注意: 拉伸只做到温和紧张，不追求疼痛；平衡不足时改为侧卧拉伸。
- 计时默认: 每侧 25 到 30 秒，换侧时可无休息或 5 秒过渡。
- 力量默认: 不作为力量动作。
- 替代: 后续可补 `side-lying-quad-stretch` 作为平衡 fallback。
- 恢复映射: trained `quads`、`hip_flexors`；recovery `lower-body-release`。

## 5. 身体部位与恢复区域草案

### 5.1 首批 `primaryMuscleIds` / `secondaryMuscleIds`

首批 fixture 可先使用以下稳定 ID：

- `full_body`
- `calves`
- `quads`
- `glutes`
- `hamstrings`
- `hip_flexors`
- `core`
- `chest`
- `triceps`
- `shoulders`
- `lats`
- `upper_back`
- `biceps`

### 5.2 首批恢复区域 ID

这些 ID 只用于基础恢复映射，不表达医疗诊断：

| recovery area id | 用途 |
|---|---|
| `lower-body-release` | 深蹲、弓步、开合跳、股四头肌拉伸后的下肢放松提示。 |
| `posterior-chain-release` | 臀桥、罗马尼亚硬拉后的臀腿后侧放松提示。 |
| `chest-shoulder-release` | 上斜俯卧撑、卧推后的胸肩前侧放松提示。 |
| `upper-back-release` | 单臂哑铃划船后的背部放松提示。 |
| `core-breathing-reset` | 平板支撑后的呼吸和躯干放松提示。 |

## 6. 内容边界

- 指导内容只描述设置、动作路径、发力要点、常见错误和普通安全注意，不做医疗诊断。
- 不写“适合/不适合某疾病人群”等医学判断；如用户有疼痛或既往损伤，文案只建议降低强度、换动作或咨询专业人士。
- 不根据心率给训练强度或风险判断；心率在首版只保留抽象显示占位。
- 不把跟练扩展成课程平台；首批动作只保证可被计时流程和跟练雏形引用。
- 不要求首批动作具备视频、音频、教练口令或 AI 纠错素材。

## 7. 审核标准

E1.2 fixture 导入前，每个动作必须通过以下审核：

1. Contract 完整性：必填字段齐全，能力标签与训练类型一致。
2. 执行可读性：`shortCue` 能在训练执行页独立成立，不依赖长文案。
3. 新手边界：首批 beginner/intermediate 动作优先，避免高风险高级动作。
4. 训练类型适配：计时动作有默认时长/休息；力量动作有组数、次数、休息和重量输入策略。
5. 单侧表达：单侧或交替动作明确 `isUnilateral` 与左右/交替记录建议。
6. 替代路径：至少高频动作具备器械或难度 fallback。
7. 恢复映射：训练部位能映射到基础恢复区域，且不医疗化。
8. 媒体后补：媒体可为空，但后续图片/视频不得改变动作 ID 和核心语义。
9. 内容状态：未完成审核的动作不得作为正式可选动作发布。

## 8. 未决事项

- `prototype/src/data/contracts.ts` 当前没有 `sourceMeta` 和 `extensions`，而 `docs/planning/data-contracts.md` 中有这两个扩展字段。E1.1 不改变契约；E1.2 如需要内容来源/审核人信息，应先决定以文档契约为准补齐 prototype，还是把这些信息留在 fixture 外部清单。
- `one-arm-dumbbell-row` 的无器械替代动作尚未在首批 11 个动作内覆盖，可在后续内容扩展中补充弹力带划船或器械划船。
- 股四头肌拉伸的平衡 fallback 未进入首批清单，后续可根据动作详情页反馈补充。
