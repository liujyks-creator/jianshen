# UX 与 Visual Contracts

## When to load

当产品包含用户界面、交互、visual direction、surface/state/component、responsive/platform、accessibility、mock/wireframe，或任何“图表/曲线/趋势”时加载。纯后台能力只有在存在操作界面或人类可读输出时才需要适用部分。

## Goal

证明每项用户需求有真实 journey 到达承载它的 surface，每个 surface 的行为、状态、组件和 accessibility 足够明确；需要视觉判断时让用户看到可比较方向并选择；不把未定义的图表语义留给 Story Writer。

## Inputs

- Accepted PRD/scope ledger、named journeys、current-Epic done state、residual map。
- 用户视觉材料、brand/design system、platform conventions、existing UI/code（brownfield）。
- Accepted parent invariants、data semantics 和 downstream consumer needs。
- 当前 UX candidate、mock/wireframe/import identity 与 shared state。

继承来源时引用而不复制产品要求。未受 UX delta 影响的 accepted technical contract 作为约束，不重新推导或重验。

## Collaboration mechanics

### 1. Journey → surface closure

先从 sources 提取每个 named journey 与用户结果，再建立双向表：

| Journey step / requirement | Surface | 用户如何到达 | 成功/失败出口 | Source |
|---|---|---|---|---|

每项需求必须有承载 surface，每个 surface 必须至少被一条 journey 到达。缺少时向用户提问；不得为了闭表自行补屏幕、导航或 protagonist。

form-factor 与 platform/responsive 边界必须在 IA 关闭前确定。多 surface 时说明状态和数据如何跨 surface 传播，但不在 UX 阶段发明 core owner。

### 2. Surface contract

逐 surface 记录：

- purpose 与入口/出口；
- applicable states：initial/cold-load、empty、content、partial、loading、error、offline、permission-denied、disabled、terminal 等；
- component anatomy 与 behavior；
- interaction、focus/back/navigation、gesture/keyboard；
- microcopy/voice，尤其 error、destructive 与 permission 文案；
- accessibility：semantics、focus order、dynamic type/text scale、TalkBack/screen reader、reduced motion、非颜色编码；
- platform/responsive/small-screen 边界；
- source 与未决项。

只列真正适用的 state，但每个省略要能从产品合同或 platform guarantee 解释。一个组件若在多 surface 使用，保持同名并定义共享行为与差异。

### 3. Visual direction 与 key screens

颜色、层级、密度、motion、chart treatment 或关键布局需要视觉判断时，产出 2–3 个可比较 visual directions/key-screen（使用项目允许的视觉工具或 self-contained mock），每个只改变当前承重维度并注明 trade-off。展示实际 artifact，让用户选择、组合或拒绝；不要用纯文字推荐替用户决定视觉。

记录用户选择的 exact artifact identity、适用边界和被拒方向。Mock 是说明，accepted UX/design spine 在冲突时为准。

Finalize 时逐个 IA surface 分类：

- `mocked`：有 identity-bound visual reference；
- `spine-only`：用户明确接受仅由文字/表格合同实现；
- `BLOCKED`：布局或视觉会改变行为但没有用户选择。

### 4. 图表/曲线/趋势合同

看到任何 chart/curve/trend 需求，first action 是建立以下 closure table，而不是创建 Story：

| 维度 | 必须关闭的问题 |
|---|---|
| X/Y axes | 各自变量、方向、domain、zero/baseline 与标尺 |
| Series | 每条线/区域/marker 的语义、优先级与组合方式 |
| Units | 值和时间单位、换算、精度、locale |
| Domain/ticks/legend | 范围、clamp、tick、label、legend、最新值 |
| Raw vs aggregation | 原始样本还是聚合；窗口、统计量、bucket alignment |
| Sampling/downsampling | 采样率、降采样规则、长 session 预算与保真边界 |
| Smoothing | 是否平滑；算法/窗口、延迟、原始值可追溯性；不平滑也要明确 |
| Gap/excluded/unknown | 缺口、用户排除、设备不可用、未知时长如何不同表达；禁止插值伪造 |
| States | empty、zero-sample、partial、error、abandoned、insufficient/no-zone |
| Interaction | pan/zoom/scrub/tooltip/selection、默认 viewport、恢复 |
| Small screen | label 密度、触控目标、横竖屏、长 session |
| Accessibility | TalkBack 摘要与逐点策略、非颜色编码、contrast、focus order |

每项记录 `DECIDED / OPEN / N/A`、source/decision owner 与 downstream effect。任何适用项 `OPEN` 都是进入 Epic/Story/CE 的 `phaseBlocker`；不得写 `known omissions=0` 或让 Writer 临场选择。

数据合同若已定义 sample/gap/coverage 等 semantics，UX 只能选择呈现方式并检查兼容；不得重算、改写或重复验证未受 delta 影响的技术真源。若呈现选择反而要求新 schema/owner/data responsibility，停止 UX local decision，升级到 Architecture/Product。

### 5. Advanced Elicitation checkpoint

surface map、visual direction 或 chart contract 形成后，按风险做一次 boundary sweep、stakeholder/accessibility lens 或 map-is-not-territory。用真实 journey 走 happy、empty、partial、error 和 long-session；展示发现和建议，用户接受后才更新 spine/mock。

## State and output

沿用项目既有 UX artifacts（如 DESIGN/EXPERIENCE 等价物），至少包含或引用：journey-surface map、IA、surface/state/component/interaction/microcopy/accessibility contracts、platform/responsive、visual decisions、chart closure（适用时）、mock coverage 与共同状态。

每个 mock/wireframe/import 有 identity、用途和冲突规则；不创建第二套模板。

## Blockers

- requirement 没有 surface，或 surface 没有 journey；
- 会改变行为的 state/component/interaction/accessibility 仍未决定；
- 视觉判断会改变关键行为但用户未看过/选择方向；
- chart closure 有任何适用 `OPEN`；
- UX 选择需要改变 product scope、core owner/schema/data responsibility；
- source/authority 不足。

## Completion / readiness checks

- 所有 current requirements 与 named journeys 双向覆盖 surface；
- 每个 surface 的适用 states、components、interaction、microcopy、accessibility、platform/responsive 已关闭；
- visual choice 来自用户对可比较 artifact 的接受，不是 Planner 静默推荐；
- chart/curve/trend 的全部适用维度为 `DECIDED` 或有理由的 `N/A`；
- mock coverage 对每个 surface 为 mocked 或用户接受的 spine-only，无未声明 orphan；
- accepted technical truth 被继承且只做冲突检查；
- 无 UX phase blocker，`firstUnfinishedAction` 指向 Architecture 或 Epic/Story 的首个具体动作。
