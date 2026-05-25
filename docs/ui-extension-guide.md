---
workflowType: ui-extension-guide
projectName: TrainFlow
documentLanguage: zh-Hans
status: draft
date: 2026-05-26
inputDocuments:
  - DESIGN.md
  - docs/architecture.md
  - docs/roadmap-backlog.md
  - docs/planning/data-contracts.md
stepsCompleted:
  - customization-boundaries
  - ui-shell-contract
  - contribution-rules
---

# TrainFlow 开源 UI 定制指南

**目标:** 允许开源社区制作不同风格的 App 主界面、主题、页面组合和按钮位置，同时保护训练核心逻辑、数据契约和安全边界。

## 1. 定制愿景

TrainFlow 的核心是训练计划执行引擎，不是某一种固定 UI。  
官方版本提供一套优雅、克制、专业的默认设计；开源社区可以在不破坏训练语义的前提下创建不同 UI shell。

社区可以探索：

- 更轻量的居家训练首页。
- 更偏力量训练的健身房首页。
- 暗色优先主题。
- 大字版无障碍训练界面。
- 不同按钮位置、底部导航结构或 dashboard 组合。

但所有 UI 变体都必须尊重同一套训练数据、命令、事件和权限边界。

## 2. 可自由定制的内容

| 层级 | 可以定制 | 示例 |
|---|---|---|
| Theme tokens | 颜色、字体、圆角、间距、动效节奏 | 暗色主题、暖色主题、大字主题 |
| UI shell | 首页布局、导航结构、dashboard 信息排序 | 力量训练优先首页、计时训练优先首页 |
| Feature composition | 页面入口组合和默认 tab | 首页显示最近计划或今日训练 |
| Component skin | 按钮样式、卡片样式、chip 样式 | 更扁平、更硬朗、更柔和 |
| Training layout | 训练执行页信息排布 | 主按钮底部、倒计时居中、心率隐藏 |
| Content presentation | 动作详情、恢复建议、历史趋势展示 | 列表、图表、分段控件 |

## 3. 不能破坏的核心边界

社区 UI 不得：

1. 直接绕过训练执行引擎写入 `WorkoutSession`。
2. 改变 `WorkoutCommand` 的语义。
3. 改变 `WorkoutEvent` 的语义。
4. 删除力量训练单组确认流程中的实际记录确认。
5. 把计划值和实际值混为一个字段。
6. 把心率展示描述为医疗诊断或危险状态判断。
7. 在未实现的情况下展示自动语音教练、AI 纠错或完整课程平台入口。
8. 跳过通知、健康数据或设备权限说明。
9. 让训练执行页无法一眼看到当前动作、剩余时间或当前组目标。

## 4. 推荐 Android UI 边界

Android 工程建议保留以下 UI 层边界：

```text
ui:designsystem
ui:shell-official
ui:theme
feature:home
feature:plans
feature:exercise-library
feature:workout-session
feature:history
feature:recovery
```

### 4.1 `ui:designsystem`

- 官方 token、组件、图标使用规则。
- 读取或映射 `DESIGN.md` 中的语义 token。
- 提供 `TrainFlowButton`、`TrainingPrimaryButton`、`PlanCard`、`ExerciseChip`、`SessionTimer` 等基础组件。

### 4.2 `ui:shell-official`

- 官方首页布局。
- 官方底部导航。
- 官方页面组合。
- 可被社区 fork 或替换。

### 4.3 `ui:theme`

- 主题 token 映射。
- 明暗主题策略。
- 社区主题可新增在独立 package/module 中。

### 4.4 Feature UI

Feature 页面可以调整布局，但必须通过 ViewModel 和 use case 与核心层交互。

## 5. UI Shell 合同

一个可接受的 UI shell 必须提供：

1. 训练首页入口。
2. 计划列表入口。
3. 动作库入口。
4. 训练记录入口。
5. 启动计时训练路径。
6. 启动力量训练路径。
7. 活跃训练恢复入口。
8. 设置或偏好入口。

UI shell 可以改变入口排序，但不能让计时训练、力量训练或活跃训练恢复不可发现。

## 6. 训练执行页合同

任何训练执行页变体都必须展示：

| 信息 | 计时训练 | 力量训练 |
|---|---|---|
| 当前动作 | 必须 | 必须 |
| 主时间 | 动作/休息倒计时 | 本组耗时或休息倒计时 |
| 当前阶段 | 动作/休息/暂停 | 准备/进行/确认/休息 |
| 下一步 | 下一动作或下一轮 | 下一组或下一动作 |
| 主控制 | 暂停/继续/跳过/结束 | 开始本组/完成本组/确认/休息控制 |
| 心率位 | 可显示或隐藏 | 可显示或隐藏 |

如果隐藏心率位，必须保留后续可接入的 UI 状态边界。

## 7. 主题贡献要求

社区主题应包含：

1. 主题名称。
2. 设计意图。
3. 颜色 token 映射。
4. 字体和字号策略。
5. 训练执行页截图或说明。
6. 可读性自检。
7. 是否支持浅色、暗色或系统跟随。

主题必须满足：

- 正文对比度不低于 WCAG AA。
- 训练主倒计时对比度足够高。
- 主要按钮和危险按钮视觉上可区分。
- 语义色不能只靠颜色表达，应配合文字或图标。

## 8. 页面布局贡献要求

社区 layout 应说明：

1. 它面向哪类用户。
2. 改动了哪些入口和排序。
3. 是否影响首版默认入口“计时训练”。
4. 活跃训练恢复入口在哪里。
5. 是否保留力量训练入口同屏可见或同层可达。
6. 是否改变训练执行页主按钮位置。

布局可以创新，但不能牺牲训练中可操作性。

## 9. 推荐贡献路径

1. 先 fork 或新建社区 UI shell。
2. 不改 `core:model`、`core:engine`、`core:domain` 的语义。
3. 用官方 fixture 跑通计时训练和力量训练。
4. 对照 `DESIGN.md` 和本文档自检。
5. 提交主题或 layout 文档、截图和测试说明。

## 10. 审查清单

合并 UI 定制前，应检查：

- 是否仍能创建计时计划。
- 是否仍能创建力量计划。
- 是否能完整执行计时训练。
- 是否能完成力量训练单组确认。
- 是否保留训练总结入口。
- 是否保留恢复建议入口。
- 是否没有医疗化心率表述。
- 是否没有展示未实现能力。
- 是否没有直接写数据库绕过引擎。
- 是否可读性达标。

## 11. 暂不支持的插件能力

MVP 阶段不做运行时插件市场，不做远程主题下载，不做用户在 App 内安装第三方 UI 包。

首版开源定制方式是：

- fork。
- 编译期选择 UI shell 或主题。
- 提交社区主题或 layout PR。

运行时主题市场可以在核心训练闭环稳定后再评估。
