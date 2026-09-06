# E17 Remainder Epic & Story Plan

<!-- E18-E22-PLAN-LANDING:BEGIN -->
## E18–E22 当前入口与本文件保留边界（2026-09-06）

当前详细入口：[E18–E22 正式计划](e18-onward-epic-story-plan.md#current-status)；唯一 Story selector 为其 F.37，source identities 与原 F9 PASS 的证据限界见该计划页首。正式来源基线为 `d2c9ac48027177389092d56c208c64447a3c6a93`，旧正文不以漂移工作区替代。

本文件旧 E17/CS 编号、Story分解、实施顺序、root/DAG、待审与下一步文字，在当前派发意义上由 E18–E22 正式计划 B–D/F.1/F.37 及 F.26 的24节点/42边替代。E17胶囊封口，CS-03/CS-04A/CS-05/CS-04B与胶囊资产保留；04C仍HELD，旧CS-06不恢复。

本计划文档 candidate 尚未通过独立 Review 并成为同步 main 的祖先时，tracked landing 为 pending；满足这些条件后 landed，E18-S01 进入 F8 完成步骤；这不自动解锁代码 Writer 或把未来 Story 标 done。

下方原正文保留。§3–§9的schema、数学、typed/NULL/union/order、original analysis、owner及证据合同按新计划明确继承范围继续消费；export v2 root/时间/选择、系统授权与24h清理等只按新计划D/F.23/F.27的窄替代，不能因旧编号退出就删除技术保证，也不能把本文件旧lease/容量或等待Review文字升级为当前操作。
<!-- E18-E22-PLAN-LANDING:END -->


本文件是 E17 remainder 的唯一 tracked canonical 详细计划。详细合同来自 exact V11，除本文件新增的 canonical 状态说明、原候选标题降级和历史 ledger 标注外，不重新规划、不改写 AC，也不改变 Story、owner、schema、DAG 或产品范围。

## 当前 canonical 身份与门禁

- Canonical contract source：`INLINE-E17-REMAINDER-EPIC-STORY-PLAN-V11`，`SHA-256=6A92D46A835B637DDFBB9DEC09A661D72736768C07FD16866F88AAF62EAB8736`。
- Closure-bounded re-Planning Review：`E17-CLEAN-SHEET-V11-CA1-CLOSURE-RE-PLANNING-REVIEW-ATTEMPT-5`，`SHA-256=92C11E019EFEBA016C9E3DFCC0FECCADD2B902A8FD785A9048D850A9CAD8570B`，`terminal=PASS`，`SPEC=PASS`，`QUALITY=PASS`，`EVIDENCE=PASS`，`findings=0`。
- Closure-bounded scoped Consistency re-Audit：`E17-CLEAN-SHEET-V11-CA1-SCOPED-CONSISTENCY-RE-AUDIT-ATTEMPT-2`，`SHA-256=39FB55004A24A331BAB078BF02D546CDC749836DCCDF7830B5F58E25DF7C8541`，`terminal=PASS`，`CONSISTENCY=PASS`，`findings=0`。
- Accepted base：`c2b569cd11953de95c8c6146c519fa6299c9557c`。
- `planningCurrentNode=tracked_planning_sync`。
- 当前状态：`TRACKED_PLANNING_SYNC_CANDIDATE / NOT_IMPLEMENTATION_READY`。
- 当前唯一未完成动作：本 tracked planning sync candidate 必须先通过一次只审查本 docs-sync delta 的 fresh 独立 Review，并被合并、推送且成为同步 `main` / `origin/main` 的 ancestor。只有这些条件全部满足，tracked planning sync 才完成，主管理才可从 roots=`CS-01/CS-03` 中选择一个 exact root Story 准备 Dev 提示词。
- 本计划已通过全部 planning Review / Consistency Audit 门禁，但尚未实现任何 CS-01 至 CS-12 Story，也没有产生 production、runtime、Gradle、APK、AVD、device、human 或 performance evidence。

下方保留 V11 的完整实质合同。V11 顶部候选 metadata 与 §14 ledger 记录的是 Review / Audit 完成前的阶段快照；它们现在是 `non-operative / historical`，不得覆盖上述当前 canonical 状态，也不得重新触发 Planning Review 或 Consistency Audit。

**Accepted source title:** E17 Remainder Epic & Story Plan V11 — CA1-MF-1 Bounded Audit Repair

Artifact ID：`INLINE-E17-REMAINDER-EPIC-STORY-PLAN-V11`
Approved proposal basis：`E17-CLEAN-SHEET-V8-SCOPED-CORRECT-COURSE-2-PROPOSAL-R2` (`SHA-256=FCA443E25E65E3CD833E50DA418C01639F15079B6C2A77D4E2026769A9F85028`) + `R2 Amendment 1`
Current candidate basis：`INLINE-E17-REMAINDER-EPIC-STORY-PLAN-V10` (`SHA-256=3BA471C9C61F33E461288DB8CBF148AD280EF98CD96A0FEF09F9AD07BD8C1486`)
Closure Review context：`E17-CLEAN-SHEET-V10-CLOSURE-RE-PLANNING-REVIEW-ATTEMPT-4` (`SHA-256=815B2E34DFB8B49E25472CC95E8BF3E272974FA635847EF5E1A7FBD0C52EFC82`，`PASS`)
Audit Repair basis：`E17-CLEAN-SHEET-V10-SCOPED-CONSISTENCY-AUDIT-ATTEMPT-1` (`SHA-256=AC578ED347BEA0CC40B34A8E5CA334F49C8D7726D675E09A44F0C9220D274878`，唯一approved finding=`CA1-MF-1`)
状态：`POST_AUDIT_REPAIR_V11 / PENDING_CLOSURE_BOUNDED_FRESH_RE_PLANNING_REVIEW / NOT_IMPLEMENTATION_READY`
Role：`fresh Planning Repair Planner`
Intent：`bounded Audit Repair`
Attempt：`1`
Mode：`Fast path / MANUAL_RELAY`
Classification：`BOUNDED_REPAIR`

本V11只修复Consistency Audit Attempt 1完整且唯一finding `CA1-MF-1`：保持既有unique-owner合同，把误列于CS-03的legacy reader与analysis status/reason semantic AC/evidence归还CS-09与CS-05，并保持CS-12为cross-layer复核。它不重做E17全量规划、Planning Review、Consistency Audit、Discovery、Product Brief、PRD、UX、Architecture、Create Epics and Stories、E17 setup、Product/UX/math/chart/E18或implementation。

V10继续是只读Repair base；Attempt 4已对V10取得closure-bounded Planning Review PASS，随后Consistency Audit Attempt 1仅返回`CA1-MF-1`。本V11不继承既有Review或Audit verdict为自身PASS；下一门禁只能是针对该Repair的closure-bounded fresh re-Planning Review，不得从Audit Repair直接启动下一次Audit。

## 1. Trigger、成功条件与已接受决定

Trigger：

- V9 SHA-256：`0D37A7E5D42BB7DDF95A43FF3E559DFFC6666FF984301B46AEB218D365196531`
- Attempt 3 Review SHA-256：`FC5953FEAF14E3FD1677BBCF3CC52996DC1D45E1AADA0DAB998B83FF60A16FF1`
- Review终态：`CHANGES_REQUESTED / SPEC=FAIL / QUALITY=FAIL / EVIDENCE=FAIL`
- Findings：`A3-MF-1`至`A3-MF-5`共5 must-fix、0 should-fix，无blocker
- Classification：全部minimum correction可在既有owner、schema/data responsibility与12-Story边界内闭合，因此为`BOUNDED_REPAIR`；不触发structural escalation或Scoped Correct Course。

已经接受并保持的承重决定：

- `CC-D03-B`
- `P-BALANCED-V2`
- `U-A`：CS-10唯一export capability；CS-11唯一S4–S7及export UI
- `R-A`：Application只provision repository；CS-04首次受保护repository API惰性启动single-flight reconciliation
- 12 Stories及其ownership；Attempt 3已证明实际consumer dependency要求在既有图增加`CS-06 -> CS-09`，因此V10 material graph为28 edges
- 五表方向与原子`Migration(4,5)`
- Product、UX、analysis math、chart、E18 residual及其他未受影响accepted state

成功条件：

1. `A3-MF-1`至`A3-MF-5`及同根因adjacent omissions形成单Writer/Reviewer可机械判定的closed contract。
2. SQLite不能表达的约束均绑定唯一validator、DAO guarded row-count及evidence owner。
3. 合同传播到CS-03/04/05/06/07/08/09/10/11/12的具体AC与evidence。
4. 12-Story ownership不变；显式记录既有CS-09 AC已经要求的`CS-06 -> CS-09`，机械结果为28 edges、longest path 8。
5. 用户已以`明确批准完整proposal（R2 + R2 Amendment 1）`整体批准；V9据该批准写入，本V10只按Attempt 3完整finding batch作bounded Repair。
6. 写入后仅进入closure-bounded fresh re-Planning Review；不自动进入Consistency Audit或implementation。

## 2. 六项 Finding old→new

| Finding | Old | V9 accepted new（V10继续保留） |
|---|---|---|
| MF-1 | 新session列、五表、recording lifecycle及version组合不闭合；legacy矩阵遗漏v4 `active/paused` | 冻结完整DDL、header/session/recording矩阵、legacy `active/paused`只读及reconciliation no-op分类、全部v1组合与唯一guard owner |
| MF-2 | 八个storage JSON和export v1缺嵌套类型、NULL、排序、cross-field与正交分支 | 冻结八个closed JSON；删除不存在的per-phase zone总和要求；冻结完整export类型语法、所有数组、`WeightValue`、`RepTarget`、phase display、SessionStepRecord字段及legacy/no-HR正交矩阵 |
| MF-3 | phase payload仍有“同上”“其余字段”等概括；paused矛盾；true rest正duration作用域不清 | 为每个variant逐exact key冻结R/N/M或literal；paused只保留`variant="paused"`；补齐legacy literals；同一positive-duration predicate贯穿CS-06/09/11 |
| MF-4 | lease manifest不是closed state machine，crash window和provider identity验证不完整 | 冻结三状态逐字段矩阵、单向转换、rename/manifest顺序、全部crash-window恢复、URI/manifest/file同identity验证及corrupt cleanup |
| MF-5 | R1只保留摘要，遗漏已接受测量环境、identity、timing/PSS/profile及CS-12重跑条件 | 完整内联`P-BALANCED-V2`，包括AVD/JDK/Gradle/APK/install/runtime、三个instrumentation class、计时/PSS及禁止修改边界 |
| SF-1 | ordinary readiness存在第二token | V9 closure继续保留；V10只以§14当前token作为ordinary readiness authority |

### 2.1 Attempt 3 bounded Repair closure

Repair固定输入为V9 `SHA-256=0D37A7E5D42BB7DDF95A43FF3E559DFFC6666FF984301B46AEB218D365196531`与不可拆分Attempt 3 Review `SHA-256=FC5953FEAF14E3FD1677BBCF3CC52996DC1D45E1AADA0DAB998B83FF60A16FF1`。accepted main为`c2b569cd11953de95c8c6146c519fa6299c9557c`。

| Attempt 3 finding | V10 minimum causally complete closure | Direct ripple |
|---|---|---|
| `A3-MF-1` | 在§4冻结canonical running/terminal header、phase/acquisition连续覆盖、recording terminal tuple、raw input cut、snapshot binding、唯一validator调用点、DAO expected tuple/rowCount与逐项illegal fixtures | §4、§10、CS-03/04/05/09/10/12 |
| `A3-MF-2` | 在§4.4冻结显式plan snapshot storage v1；既有无版本payload只能进入`legacy_unversioned`，禁止合成v1或调用`fallbackMode`；export按真实storage identity输出1或NULL | §4.3/4.4、§7.2、§10、CS-03/09/10/12 |
| `A3-MF-3` | 在§5.8与§7.5冻结analysis v1 pure status priority以及session/phase每个reason的required-iff/forbidden、scope与duration source | §5.8、§7.4/7.5、§10、CS-03/05/09/10/11/12 |
| `A3-MF-4` | 在focus v1使用phase identity已有的`compositionBlockId + roundIndex0`唯一定位composition block-local round；legacy相邻分支使用已有`blockId + roundIndex0`，不生成新round identity | §6.6、CS-06/09/11/12 |
| `A3-MF-5` | 保留CS-06 predicate owner，显式增加`CS-06 -> CS-09`；DAG更新为28 edges、longest path 8 | §6.5、CS-06/09、§12 |

没有新用户承重决定；Product、UX、analysis math、12 Stories、`U-A`、`R-A`、`CC-D03-B`、`P-BALANCED-V2`与其他unaffected合同保持关闭。

### 2.2 Consistency Audit Attempt 1 bounded Repair closure

Repair固定输入为V10 `SHA-256=3BA471C9C61F33E461288DB8CBF148AD280EF98CD96A0FEF09F9AD07BD8C1486`、Attempt 4 `SHA-256=815B2E34DFB8B49E25472CC95E8BF3E272974FA635847EF5E1A7FBD0C52EFC82`与不可拆分Audit Attempt 1 `SHA-256=AC578ED347BEA0CC40B34A8E5CA334F49C8D7726D675E09A44F0C9220D274878`。accepted main仍为`c2b569cd11953de95c8c6146c519fa6299c9557c`。

| Audit finding | V11 minimum causally complete closure | Preserved boundary |
|---|---|---|
| `CA1-MF-1` | CS-03只保留canonical plan snapshot storage v1 writer/strict validator及structural schema；legacy strict reader与全部legacy negative evidence唯一归CS-09；`StatusProjectionV1`、status priority和quality reason semantic validator/negative evidence唯一归CS-05；CS-12只做cross-layer复核 | CS-10/11消费合同、12 Story、owner/interface/schema、DAG、`CC-D03-B`、`P-BALANCED-V2`及全部unaffected内容不变 |

Bounded adjacent-omission scan只命中§10 owner表、CS-03/05/09/12 AC/Evidence、§12 owner/impact文字与§14 readiness handoff；没有同根因遗漏要求新增owner、interface、schema、Story或edge。

## 3. MF-1：完整 Room v5 DDL

### 3.1 `workout_sessions`七个新增列

```sql
ALTER TABLE workout_sessions
ADD COLUMN timeline_version INTEGER DEFAULT NULL
CHECK (timeline_version IS NULL OR timeline_version = 1);

ALTER TABLE workout_sessions
ADD COLUMN last_durable_offset_ms INTEGER DEFAULT NULL
CHECK (last_durable_offset_ms IS NULL OR last_durable_offset_ms >= 0);

ALTER TABLE workout_sessions
ADD COLUMN last_mutation_sequence INTEGER DEFAULT NULL
CHECK (last_mutation_sequence IS NULL OR last_mutation_sequence >= 0);

ALTER TABLE workout_sessions
ADD COLUMN trusted_end_offset_ms INTEGER DEFAULT NULL
CHECK (trusted_end_offset_ms IS NULL OR trusted_end_offset_ms >= 0);

ALTER TABLE workout_sessions
ADD COLUMN terminal_reason TEXT DEFAULT NULL
CHECK (
  terminal_reason IS NULL OR terminal_reason IN (
    'completed',
    'user_abandoned',
    'owner_cleared',
    'process_interrupted'
  )
);

ALTER TABLE workout_sessions
ADD COLUMN display_metadata_contract_version INTEGER DEFAULT NULL
CHECK (
  display_metadata_contract_version IS NULL
  OR display_metadata_contract_version = 1
);

ALTER TABLE workout_sessions
ADD COLUMN session_display_metadata_json TEXT DEFAULT NULL;
```

`Migration(4,5)`不得回填或推测canonical事实。所有v4旧行七列均保持NULL，包括旧`ready/active/paused/completed/abandoned`。

### 3.2 `workout_phase_intervals`

```sql
CREATE TABLE workout_phase_intervals (
  id TEXT NOT NULL PRIMARY KEY,
  session_id TEXT NOT NULL,
  sequence INTEGER NOT NULL CHECK (sequence >= 0),
  start_offset_ms INTEGER NOT NULL CHECK (start_offset_ms >= 0),
  end_offset_ms INTEGER CHECK (end_offset_ms IS NULL OR end_offset_ms >= 0),
  start_mutation_sequence INTEGER NOT NULL
    CHECK (start_mutation_sequence >= 0),
  end_mutation_sequence INTEGER
    CHECK (end_mutation_sequence IS NULL OR end_mutation_sequence >= 0),
  open_marker INTEGER
    CHECK (open_marker IS NULL OR open_marker = 1),
  phase_kind TEXT NOT NULL CHECK (
    phase_kind IN (
      'timed_work',
      'timed_rest',
      'strength_prepare_set',
      'strength_active_set',
      'strength_confirm_set',
      'strength_rest',
      'follow_along_action',
      'follow_along_rest',
      'paused'
    )
  ),
  phase_identity_json TEXT NOT NULL,

  FOREIGN KEY (session_id)
    REFERENCES workout_sessions(id)
    ON DELETE CASCADE,

  UNIQUE (session_id, sequence),
  UNIQUE (session_id, open_marker),

  CHECK (
    (
      open_marker = 1
      AND end_offset_ms IS NULL
      AND end_mutation_sequence IS NULL
    )
    OR
    (
      open_marker IS NULL
      AND end_offset_ms IS NOT NULL
      AND end_mutation_sequence IS NOT NULL
    )
  ),

  CHECK (
    end_offset_ms IS NULL
    OR end_offset_ms > start_offset_ms
    OR (
      end_offset_ms = start_offset_ms
      AND end_mutation_sequence > start_mutation_sequence
    )
  )
);

CREATE INDEX index_workout_phase_intervals_session_start
ON workout_phase_intervals(session_id, start_offset_ms);
```

Zero-duration mutation合法，但同offset的end mutation sequence必须严格大于start。

### 3.3 `heart_rate_recordings`

```sql
CREATE TABLE heart_rate_recordings (
  recording_id TEXT NOT NULL PRIMARY KEY,
  session_id TEXT NOT NULL UNIQUE,

  status TEXT NOT NULL
    CHECK (status IN ('active', 'terminal')),

  started_offset_ms INTEGER NOT NULL
    CHECK (started_offset_ms >= 0),
  started_mutation_sequence INTEGER NOT NULL
    CHECK (started_mutation_sequence >= 0),

  ended_offset_ms INTEGER
    CHECK (ended_offset_ms IS NULL OR ended_offset_ms >= 0),
  ended_mutation_sequence INTEGER
    CHECK (ended_mutation_sequence IS NULL OR ended_mutation_sequence >= 0),

  source_contract_version INTEGER NOT NULL
    CHECK (source_contract_version = 1),
  source_kind TEXT NOT NULL
    CHECK (source_kind = 'ble_hrs'),
  acquisition_contract_version INTEGER NOT NULL
    CHECK (acquisition_contract_version = 1),
  parameter_snapshot_version INTEGER NOT NULL
    CHECK (parameter_snapshot_version = 1),

  age INTEGER
    CHECK (age IS NULL OR age BETWEEN 1 AND 130),
  personal_max_bpm INTEGER
    CHECK (personal_max_bpm IS NULL OR personal_max_bpm BETWEEN 30 AND 260),
  effective_max_bpm INTEGER
    CHECK (effective_max_bpm IS NULL OR effective_max_bpm BETWEEN 30 AND 260),
  effective_max_source TEXT
    CHECK (
      effective_max_source IS NULL
      OR effective_max_source IN ('personal_max', 'age_220_minus_age')
    ),
  alert_threshold_bpm INTEGER
    CHECK (
      alert_threshold_bpm IS NULL
      OR alert_threshold_bpm BETWEEN 30 AND 260
    ),

  zone_snapshot_json TEXT,
  original_analysis_version INTEGER
    CHECK (
      original_analysis_version IS NULL
      OR original_analysis_version = 1
    ),

  FOREIGN KEY (session_id)
    REFERENCES workout_sessions(id)
    ON DELETE CASCADE,

  CHECK (
    (
      status = 'active'
      AND ended_offset_ms IS NULL
      AND ended_mutation_sequence IS NULL
      AND original_analysis_version IS NULL
    )
    OR
    (
      status = 'terminal'
      AND ended_offset_ms IS NOT NULL
      AND ended_mutation_sequence IS NOT NULL
      AND original_analysis_version = 1
    )
  ),

  CHECK (
    ended_offset_ms IS NULL
    OR ended_offset_ms > started_offset_ms
    OR (
      ended_offset_ms = started_offset_ms
      AND ended_mutation_sequence > started_mutation_sequence
    )
  ),

  CHECK (
    (
      effective_max_bpm IS NULL
      AND effective_max_source IS NULL
      AND zone_snapshot_json IS NULL
      AND age IS NULL
      AND personal_max_bpm IS NULL
    )
    OR
    (
      effective_max_source = 'personal_max'
      AND personal_max_bpm IS NOT NULL
      AND effective_max_bpm = personal_max_bpm
      AND zone_snapshot_json IS NOT NULL
    )
    OR
    (
      effective_max_source = 'age_220_minus_age'
      AND personal_max_bpm IS NULL
      AND age IS NOT NULL
      AND effective_max_bpm = 220 - age
      AND zone_snapshot_json IS NOT NULL
    )
  )
);
```

Recording只允许一次`active -> terminal`。用户关闭或重开心率只改变acquisition intent，不终结recording或创建第二recording。

### 3.4 `heart_rate_acquisition_intervals`

```sql
CREATE TABLE heart_rate_acquisition_intervals (
  id TEXT NOT NULL PRIMARY KEY,
  recording_id TEXT NOT NULL,
  sequence INTEGER NOT NULL CHECK (sequence >= 0),

  start_offset_ms INTEGER NOT NULL CHECK (start_offset_ms >= 0),
  end_offset_ms INTEGER CHECK (end_offset_ms IS NULL OR end_offset_ms >= 0),

  start_mutation_sequence INTEGER NOT NULL
    CHECK (start_mutation_sequence >= 0),
  end_mutation_sequence INTEGER
    CHECK (end_mutation_sequence IS NULL OR end_mutation_sequence >= 0),

  open_marker INTEGER
    CHECK (open_marker IS NULL OR open_marker = 1),

  recording_intent TEXT NOT NULL CHECK (
    recording_intent IN ('expected_recording', 'user_excluded')
  ),

  intent_reason TEXT CHECK (
    intent_reason IS NULL
    OR intent_reason IN (
      'user_turned_off',
      'user_opted_out',
      'user_disconnected_suppress_recovery'
    )
  ),

  device_state TEXT NOT NULL CHECK (
    device_state IN (
      'not_observing',
      'no_source_selected',
      'permission_required',
      'bluetooth_unavailable',
      'searching',
      'connecting',
      'waiting_first_sample',
      'live',
      'stale',
      'reconnecting',
      'disconnected',
      'technical_failure'
    )
  ),

  device_reason TEXT CHECK (
    device_reason IS NULL
    OR device_reason IN (
      'initial_acquisition',
      'automatic_recovery',
      'source_not_selected',
      'source_unavailable',
      'permission_missing',
      'permission_revoked',
      'bluetooth_off',
      'platform_unavailable',
      'first_sample_timeout',
      'sample_stale_timeout',
      'unexpected_disconnect',
      'connection_timeout',
      'measurement_stream_unavailable',
      'platform_failure'
    )
  ),

  FOREIGN KEY (recording_id)
    REFERENCES heart_rate_recordings(recording_id)
    ON DELETE CASCADE,

  UNIQUE (recording_id, sequence),
  UNIQUE (recording_id, open_marker),

  CHECK (
    (
      recording_intent = 'expected_recording'
      AND intent_reason IS NULL
    )
    OR
    (
      recording_intent = 'user_excluded'
      AND intent_reason IN (
        'user_turned_off',
        'user_opted_out',
        'user_disconnected_suppress_recovery'
      )
    )
  ),

  CHECK (
    (
      open_marker = 1
      AND end_offset_ms IS NULL
      AND end_mutation_sequence IS NULL
    )
    OR
    (
      open_marker IS NULL
      AND end_offset_ms IS NOT NULL
      AND end_mutation_sequence IS NOT NULL
    )
  ),

  CHECK (
    end_offset_ms IS NULL
    OR end_offset_ms > start_offset_ms
    OR (
      end_offset_ms = start_offset_ms
      AND end_mutation_sequence > start_mutation_sequence
    )
  )
);

CREATE INDEX index_hr_acquisition_recording_start
ON heart_rate_acquisition_intervals(recording_id, start_offset_ms);
```

非NULL `device_reason`还必须通过12-state/14-reason allowed-pair validator；枚举CHECK不能代替pair验证。

### 3.5 `heart_rate_samples`

```sql
CREATE TABLE heart_rate_samples (
  recording_id TEXT NOT NULL,
  sample_sequence INTEGER NOT NULL CHECK (sample_sequence >= 0),
  offset_ms INTEGER NOT NULL CHECK (offset_ms >= 0),
  mutation_sequence INTEGER NOT NULL CHECK (mutation_sequence >= 0),
  bpm INTEGER NOT NULL CHECK (bpm BETWEEN 1 AND 65535),

  PRIMARY KEY (recording_id, sample_sequence),

  FOREIGN KEY (recording_id)
    REFERENCES heart_rate_recordings(recording_id)
    ON DELETE CASCADE
);

CREATE INDEX index_hr_samples_canonical_order
ON heart_rate_samples(
  recording_id,
  offset_ms,
  mutation_sequence,
  sample_sequence
);
```

`1..65535`是BLE HRS structural value domain，不是医疗阈值。查询必须显式：

```sql
ORDER BY offset_ms, mutation_sequence, sample_sequence
```

### 3.6 `heart_rate_analysis_snapshots`

```sql
CREATE TABLE heart_rate_analysis_snapshots (
  recording_id TEXT NOT NULL,
  analysis_version INTEGER NOT NULL CHECK (analysis_version = 1),

  created_at TEXT NOT NULL,
  input_last_mutation_sequence INTEGER NOT NULL
    CHECK (input_last_mutation_sequence >= 0),

  sample_status TEXT NOT NULL CHECK (
    sample_status IN (
      'no_canonical_samples',
      'canonical_only_excluded',
      'primary_points_available'
    )
  ),

  coverage_status TEXT NOT NULL CHECK (
    coverage_status IN (
      'no_eligible_duration',
      'insufficient',
      'partial',
      'normal'
    )
  ),

  zone_status TEXT NOT NULL CHECK (
    zone_status IN ('available', 'unavailable_no_effective_max')
  ),

  canonical_sample_count INTEGER NOT NULL
    CHECK (canonical_sample_count >= 0),
  primary_point_sample_count INTEGER NOT NULL
    CHECK (
      primary_point_sample_count >= 0
      AND primary_point_sample_count <= canonical_sample_count
    ),

  eligible_duration_ms INTEGER
    CHECK (eligible_duration_ms IS NULL OR eligible_duration_ms >= 0),
  covered_duration_ms INTEGER
    CHECK (covered_duration_ms IS NULL OR covered_duration_ms >= 0),
  coverage_basis_points INTEGER
    CHECK (
      coverage_basis_points IS NULL
      OR coverage_basis_points BETWEEN 0 AND 10000
    ),

  weighted_bpm_ms INTEGER
    CHECK (weighted_bpm_ms IS NULL OR weighted_bpm_ms >= 0),
  observed_avg_bpm INTEGER
    CHECK (observed_avg_bpm IS NULL OR observed_avg_bpm BETWEEN 1 AND 65535),

  observed_max_bpm INTEGER
    CHECK (observed_max_bpm IS NULL OR observed_max_bpm BETWEEN 1 AND 65535),
  highest_offset_ms INTEGER
    CHECK (highest_offset_ms IS NULL OR highest_offset_ms >= 0),
  highest_mutation_sequence INTEGER
    CHECK (
      highest_mutation_sequence IS NULL
      OR highest_mutation_sequence >= 0
    ),
  highest_sample_sequence INTEGER
    CHECK (
      highest_sample_sequence IS NULL
      OR highest_sample_sequence >= 0
    ),

  analysis_config_json TEXT NOT NULL,
  zone_durations_json TEXT,
  phase_aggregates_json TEXT NOT NULL,
  duration_breakdown_json TEXT NOT NULL,
  quality_reasons_json TEXT NOT NULL,

  PRIMARY KEY (recording_id, analysis_version),

  FOREIGN KEY (recording_id)
    REFERENCES heart_rate_recordings(recording_id)
    ON DELETE CASCADE,

  CHECK (
    eligible_duration_ms IS NOT NULL
    AND covered_duration_ms IS NOT NULL
    AND covered_duration_ms <= eligible_duration_ms
  ),

  CHECK (
    (
      eligible_duration_ms = 0
      AND covered_duration_ms = 0
      AND coverage_basis_points IS NULL
      AND coverage_status = 'no_eligible_duration'
    )
    OR
    (
      eligible_duration_ms > 0
      AND coverage_basis_points IS NOT NULL
      AND coverage_status IN ('insufficient', 'partial', 'normal')
    )
  ),

  CHECK (
    (
      covered_duration_ms = 0
      AND weighted_bpm_ms IS NULL
      AND observed_avg_bpm IS NULL
    )
    OR
    (
      covered_duration_ms > 0
      AND weighted_bpm_ms IS NOT NULL
      AND observed_avg_bpm IS NOT NULL
    )
  ),

  CHECK (
    (
      observed_max_bpm IS NULL
      AND highest_offset_ms IS NULL
      AND highest_mutation_sequence IS NULL
      AND highest_sample_sequence IS NULL
    )
    OR
    (
      observed_max_bpm IS NOT NULL
      AND highest_offset_ms IS NOT NULL
      AND highest_mutation_sequence IS NOT NULL
      AND highest_sample_sequence IS NOT NULL
    )
  ),

  CHECK (
    (
      primary_point_sample_count = 0
      AND observed_max_bpm IS NULL
    )
    OR
    (
      primary_point_sample_count > 0
      AND observed_max_bpm IS NOT NULL
    )
  ),

  CHECK (
    (
      sample_status = 'no_canonical_samples'
      AND canonical_sample_count = 0
      AND primary_point_sample_count = 0
    )
    OR
    (
      sample_status = 'canonical_only_excluded'
      AND canonical_sample_count > 0
      AND primary_point_sample_count = 0
    )
    OR
    (
      sample_status = 'primary_points_available'
      AND primary_point_sample_count > 0
    )
  ),

  CHECK (
    (
      zone_status = 'unavailable_no_effective_max'
      AND zone_durations_json IS NULL
    )
    OR
    (
      zone_status = 'available'
      AND (
        eligible_duration_ms = 0
        OR zone_durations_json IS NOT NULL
      )
    )
  )
);
```

5000/7000/8000bp分类、checked multiplication、JSON key set、zone sum、phase aggregate sum、raw-anchor引用和跨表binding由CS-05 validator承担，不放入可能溢出或无法查子表的SQLite CHECK。

## 4. MF-1：session、legacy、recording与version矩阵

### 4.1 Session矩阵

| Row类别 | Existing status | 七个新增列 | 读取语义 | Reconciliation |
|---|---|---|---|---|
| v4 legacy ready | `ready` | 全NULL | `legacy_incomplete_nonterminal` | no-op；不创建timeline |
| v4 legacy running | `active/paused` | 全NULL | 保留原status并返回`legacy_noncanonical_nonterminal`；不是E17 canonical runtime | no-op分类；不写terminal、不造offset、phase、reason或snapshot |
| v4 legacy terminal | `completed/abandoned` | 全NULL | `timelineStatus=legacy_incomplete`；允许legacy terminal history/export | 不参与canonical reconciliation |
| E17 running | `active/paused` | timeline=1、durable tuple与display metadata存在；trusted end/reason NULL | canonical nonterminal | 按R-A扫描并CAS reconcile |
| E17 completed | `completed` | canonical tuple完整 | `terminal_reason=completed` | 已terminal no-op |
| E17 abandoned | `abandoned` | canonical tuple完整 | reason为下列三者之一 | 已terminal no-op |

Canonical abandoned reasons：

```text
user_abandoned
owner_cleared
process_interrupted
```

Legacy `active/paused`规则：

- Migration成功不依赖将其terminal化。
- CS-04 gate可以在完成canonical扫描后进入`SUCCEEDED`，同时返回typed legacy residual集合；该集合不是reconciliation失败。
- 不复活旧engine、notification、FGS、recording或current step。
- CS-09/10对其返回`session_not_terminal / legacy_noncanonical_nonterminal`，不当作corruption，不允许terminal export。
- 不阻止新的canonical StartSession；它也不成为当前runtime owner。
- 只能保留原行或由既有明确用户删除路径删除；本Correct Course不新增自动abandon或数据修复。
- 任一“部分NULL、部分canonical”的七列组合不是legacy，属于`invalid_partial_canonical_header`，为nonretryable/manual-resolution-required。

### 4.2 Recording/session组合

| Session | Recording |
|---|---|
| legacy任意状态 | 不存在E17 recording |
| E17 active/paused且HR从未开启 | 无recording |
| E17 active/paused且HR已开启 | `status=active`，end/binding NULL |
| E17 terminal且无HR identity | 无recording，合法`not_recorded` |
| E17 terminal且有recording | `status=terminal`，end tuple存在，`original_analysis_version=1` |
| E17 terminal + active recording | 非法 |
| E17 running + terminal recording | 非法 |
| terminal recording缺snapshot/binding | 不可观察；transaction rollback/invariant failure |

#### 4.2.1 Canonical header与final tuple

Canonical tuple记为`T=(offset_ms, mutation_sequence)`，按offset优先、mutation sequence次序作lexicographic比较。`last_mutation_sequence`是session内单调递增且不复用的最终输入cut identity；任何phase/acquisition endpoint或sample的mutation sequence都不得大于当前header的`last_mutation_sequence`。

Canonical header只有以下两类合法矩阵：

| Header类别 | `status` | `timeline_version` | `last_durable_offset_ms` | `last_mutation_sequence` | `trusted_end_offset_ms` | `terminal_reason` | display version/json |
|---|---|---:|---:|---:|---:|---|---|
| running | `active/paused` | `1` | Required | Required | NULL | NULL | `1` + valid 5.1 object |
| terminal completed | `completed` | `1` | Required | Required | Required且=`last_durable_offset_ms` | `completed` | `1` + valid 5.1 object |
| terminal abandoned | `abandoned` | `1` | Required | Required | Required且=`last_durable_offset_ms` | `user_abandoned/owner_cleared/process_interrupted`之一 | `1` + valid 5.1 object |

Canonical terminal final tuple唯一为：

```text
finalT = (trusted_end_offset_ms, last_mutation_sequence)
trusted_end_offset_ms = last_durable_offset_ms
```

除上表外，canonical七列的任何NULL、status/reason、version或equality组合都为`invalid_canonical_header_v1`。Legacy仍严格是4.1中“七列全NULL”的分支；不得把partial header降级为legacy。

#### 4.2.2 Phase、recording、acquisition与snapshot同一input cut

`CanonicalSessionGraphV1Validator`冻结下列跨行predicate；这些predicate均在真实Room rows上验证，不能由单表CHECK、内存DTO默认值或export reader补救：

1. **Phase coverage**：同session的phase `sequence`必须是从0开始的连续整数。首行`start_offset_ms=0`；相邻行必须满足`previous.end_offset_ms = next.start_offset_ms`且`previous.end_mutation_sequence = next.start_mutation_sequence`。running header恰有最后一行open、其余全closed；terminal header没有open row，末行`(end_offset_ms,end_mutation_sequence)=finalT`。任一gap、overlap、duplicate sequence、非末尾open或endpoint大于header input cut均非法。
2. **Recording range**：存在recording时，`startT=(started_offset_ms,started_mutation_sequence)`必须满足`(0,0) <= startT <= finalT/current durable T`。running session只能配active recording；terminal session只能配terminal recording。Terminal recording的`(ended_offset_ms,ended_mutation_sequence)`必须exact等于`finalT`。
3. **Acquisition coverage**：存在recording时，acquisition `sequence`从0连续；首行start tuple exact等于recording `startT`；相邻end/start tuple exact相等。Active recording恰有最后一行open；terminal recording没有open row，末行end tuple exact等于`finalT`。三种`user_excluded` reason和`expected_recording`共同形成完整、无gap、无overlap的recording-window partition。
4. **Raw input cut**：每个sample的`(offset_ms,mutation_sequence)`必须处于recording `[startT, finalT]`，且`mutation_sequence <= last_mutation_sequence`；phase/acquisition的所有start/end mutation也必须小于等于同一cut。Canonical sample order仍为`offset_ms -> mutation_sequence -> sample_sequence`，同offset不折叠。
5. **Original snapshot binding**：terminal recording必须有且只有一个同`recording_id`、`analysis_version=1`的snapshot；`snapshot.input_last_mutation_sequence = session.last_mutation_sequence = recording.ended_mutation_sequence`；recording `original_analysis_version=1`必须引用该row。Snapshot计算输入exact为上述cut内全部validated phase、acquisition与sample rows，不得遗漏、加入cut后row或按current state重算。
6. **No-HR terminal**：session不存在recording时，不得存在该session可达的acquisition、sample或analysis snapshot row；该分支只投影`not_recorded`，不合成recording identity。

#### 4.2.3 唯一validator、调用时点与DAO guard

- CS-03唯一实现/拥有纯`CanonicalSessionGraphV1Validator`及其header/recording/partition子predicate；不新增第二repository、writer或export validator。
- CS-04每次ongoing phase/acquisition/sample mutation前验证已加载header与expected open row，写后在同一Room transaction内对受影响partition调用该validator。Close/open DAO必须匹配`session_id/recording_id + expected status + expected last tuple + expected open row id + NULL end fields`，每个update/insert expected `rowCount=1`；0或大于1立即rollback并保留原错误。
- CS-05是terminal transaction owner：先对pre-terminal exact tuple和完整raw cut调用同一validator，再在一个Room transaction中关闭最后phase/acquisition、terminalize recording、写session terminal header、插入analysis v1 snapshot并完成NULL→1 original binding；每个guard匹配pre-terminal status/tuple且`rowCount=1`，snapshot conflict或binding rowCount非1整体rollback；commit前对terminal graph再次验证。
- CS-09/10只读同一validated graph或同一typed failure；不得各自实现宽松cross-row解释。CS-12只复核identity-bound evidence，不修复production rows。

必须逐项存在的illegal fixtures：terminal durable/trusted不等、completed/reason不等、abandoned reason非法、phase first-start/gap/overlap/open/final-end错误、recording start越界、recording/session status不配、recording end不等`finalT`、acquisition first-start/gap/overlap/open/final-end错误、sample在recording范围外或mutation越cut、snapshot input sequence不等final mutation、snapshot/raw recording ID不等、snapshot缺失/重复、original binding缺失/错误、DAO stale expected tuple及每个guard的rowCount 0/2。每个fixture必须证明transaction无partial write。

### 4.3 Version组合

唯一v1组合：

```text
timeline_version=1
display_metadata_contract_version=1
displayMetadataContractVersion=1

phaseIdentityContractVersion=1
legacy_timed_v1/payloadVersion=1/mode=timed
timed_composition_v2/payloadVersion=2/mode=timed
strength_v1/payloadVersion=1/mode=strength
follow_along_v1/payloadVersion=1/mode=follow_along

source_contract_version=1
source_kind=ble_hrs
acquisition_contract_version=1
parameter_snapshot_version=1
zoneSnapshotContractVersion=1 iff effective max exists

analysis_version=1
analysisConfigContractVersion=1
sampleIntervalContractVersion=1
zoneAttributionContractVersion=1
statusProjectionContractVersion=1
durationPartitionContractVersion=1
zoneDurationsContractVersion=1 when zone_durations_json is non-NULL
phaseAggregatesContractVersion=1
durationBreakdownContractVersion=1
qualityReasonsContractVersion=1

planSnapshotStorageContractVersion=1 only when the persisted root carries literal 1
legacy unversioned plan snapshot -> storage contract version is NULL, never synthesized

exportContractVersion=1
displayContractVersion=1
shareLeaseContractVersion=1
```

Unknown version：

- Write：transaction mutation前拒绝。
- Read：typed unsupported failure。
- Reconciliation：`failed_nonretryable / manual_resolution_required`。
- Export：不重算、不选择latest、不fallback current serializer、不输出半份JSON。

### 4.4 Plan snapshot真实storage identity与strict reader

#### Canonical storage v1

E17新建canonical session的`plan_snapshot_json`必须由既有`WorkoutPlanSnapshotStorageJson.toStorageJson`边界升级为显式v1 writer。Root exact keys、顺序与NULL规则为：

```json
{
  "planSnapshotStorageContractVersion": 1,
  "planId": null,
  "title": "训练标题",
  "mode": "timed",
  "blocks": [],
  "preferences": null,
  "followAlong": null
}
```

- 七个root keys全部Required；`planId/preferences/followAlong`可为JSON `null`，不得因Kotlin值为NULL而省略。`title`为string，`mode`为`timed/strength/follow_along`且必须等于`workout_sessions.mode`，`blocks`为array。
- 每个block必须按其persisted `kind`严格消费现有`WorkoutPlanSnapshotStorageJson` writer的对应closed shape；`timed_composition`必须携带`compositionVersion=2`并保留block id、stage groups、targets、rounds、boundary style与compatibility字段；legacy timed、strength、warmup/rest/stretch/cooldown也必须逐元素一对一解析。任何unknown/missing/wrong-type member、unknown kind/version、默认补值、`mapNotNull`丢元素或normalization改变persisted fact均为`invalid_plan_snapshot_storage_v1`。
- `PlanSnapshotStorageV1Validator`由CS-03唯一拥有。Strict parse成功后按v1 canonical writer重序列化必须与persisted UTF-8 JSON原文byte-for-byte相等；因此export可保留原文且不需要consumer-local canonicalization。
- Canonical header只允许storage v1。Root缺version、version不是integer 1、payload corrupt或root mode与session mode不等均在StartSession写前或read时fail closed；不得把`fallbackMode`注入结果标为v1。

#### Existing legacy/unversioned rows

- Fresh `Migration(4,5)`不改写任何既有`plan_snapshot_json`。Root没有`planSnapshotStorageContractVersion`时唯一分类为`legacy_unversioned`，其export字段`planSnapshotStorageContractVersion=NULL`；绝不合成1。
- `LegacyUnversionedPlanSnapshotReader`由CS-09唯一调用：要求root JSON object、明确存在合法`title/mode/blocks`，root `mode`等于session mode，blocks逐元素完整解析；可读取既有writer曾省略的nullable `planId/preferences/followAlong`。它不得调用当前`toPlanSnapshot(fallbackMode=...)`，不得补“未命名训练”、fallback mode、empty blocks、default enum、丢弃unknown block或normalization后伪装成功。
- Legacy strict read失败返回`invalid_legacy_unversioned_plan_snapshot`；显式非1 version返回`unsupported_plan_snapshot_storage_version`。History/resolver显示typed unavailable；terminal export整体失败且不输出半份JSON。原始row始终不回写。
- CS-10只导出CS-03 v1 validator或CS-09 legacy strict reader已经接受的persisted原文string；不得recompute、reserialize domain object或把legacy升级为v1。

## 5. MF-2：八个 closed storage JSON

共同规则：

- Root必须为object。
- Known v1对象的key set必须完全相等。
- Nullable key仍必须存在并写JSON `null`。
- Unknown member、missing key、错误NULL、错误type、非法literal、非整数的count/duration/index均失败。
- Duration/count/index均为非负integer。
- 不保存raw exception、device ID/name/address、URI、文件名或diagnostic fragment。
- Unknown version返回`unsupported_*_version`。
- Known-version corruption返回`invalid_*_contract`。
- 不允许best-effort、consumer-local修复或current serializer fallback。

### 5.1 `session_display_metadata_json`

Root exact keys：

```json
{
  "displayMetadataContractVersion": 1,
  "entries": []
}
```

Entry exact keys：

```json
{
  "entityKind": "exercise",
  "stableId": "non-empty-string",
  "displayNameAtFirstReference": "string",
  "customNameAtFirstReference": null,
  "resolutionSource": "plan_snapshot"
}
```

规则：

- `entityKind`固定`exercise`。
- `resolutionSource`仅`plan_snapshot/runtime_substitution`。
- `customNameAtFirstReference`唯一nullable member。
- `stableId`唯一。
- 数组按首次引用mutation顺序。
- Active只append；既有entry不可变；terminal后整个对象不可变。

### 5.2 `zone_snapshot_json`

```json
{
  "zoneSnapshotContractVersion": 1,
  "unit": "bpm",
  "effectiveMaxBpm": 180,
  "effectiveMaxSource": "personal_max",
  "zones": [
    {
      "zoneId": "below_50",
      "lowerBoundBasisPointsInclusive": null,
      "upperBoundBasisPointsExclusive": 5000
    },
    {
      "zoneId": "from_50_to_60",
      "lowerBoundBasisPointsInclusive": 5000,
      "upperBoundBasisPointsExclusive": 6000
    },
    {
      "zoneId": "from_60_to_70",
      "lowerBoundBasisPointsInclusive": 6000,
      "upperBoundBasisPointsExclusive": 7000
    },
    {
      "zoneId": "from_70_to_80",
      "lowerBoundBasisPointsInclusive": 7000,
      "upperBoundBasisPointsExclusive": 8000
    },
    {
      "zoneId": "from_80_to_90",
      "lowerBoundBasisPointsInclusive": 8000,
      "upperBoundBasisPointsExclusive": 9000
    },
    {
      "zoneId": "at_or_above_90",
      "lowerBoundBasisPointsInclusive": 9000,
      "upperBoundBasisPointsExclusive": null
    }
  ]
}
```

六项顺序、ID、bounds固定。Root effective值必须等于recording typed columns。Alert不是zone。

### 5.3 `phase_identity_json`

Exact root keys：

```json
{
  "phaseIdentityContractVersion": 1,
  "family": "timed_composition_v2",
  "payloadVersion": 2,
  "mode": "timed",
  "phaseKind": "timed_work",
  "orderedStructureSignature": {
    "signatureContractVersion": 1,
    "algorithm": "sha256",
    "digestHexLowercase": "64-lowercase-hex"
  },
  "payload": {}
}
```

Family/payload矩阵见第6节。Signature input和canonical encoder保持V8 accepted contract。

### 5.4 `analysis_config_json`

```json
{
  "analysisConfigContractVersion": 1,
  "sampleValidityCapMs": 2500,
  "sampleIntervalContractVersion": 1,
  "partialLowerBoundBasisPoints": 5000,
  "phaseConclusionBasisPoints": 7000,
  "normalBasisPoints": 8000,
  "coverageThresholdRule": "checked_integer_cross_multiply",
  "coverageBasisPointsRule": "floor_integer_ratio",
  "displayPercentRule": "floor_basis_points_div_100",
  "weightedAverageRule": "checked_integer_time_integral",
  "averageDisplayRule": "positive_integer_half_up",
  "zeroCoveredRule": "null_integral_and_average",
  "observedMaxRule": "eligible_canonical_point_first_tie",
  "zoneAttributionContractVersion": 1,
  "zoneAttributionRule": "checked_cross_multiply_six_zones",
  "statusProjectionContractVersion": 1,
  "durationPartitionContractVersion": 1
}
```

全部Required、无NULL、literal和值不得变化。

### 5.5 `zone_durations_json`

```json
{
  "zoneDurationsContractVersion": 1,
  "below50DurationMs": 0,
  "from50To60DurationMs": 0,
  "from60To70DurationMs": 0,
  "from70To80DurationMs": 0,
  "from80To90DurationMs": 0,
  "atOrAbove90DurationMs": 0
}
```

六项和必须exact等于whole `covered_duration_ms`。Zone unavailable或eligible=0时整列NULL。

### 5.6 `phase_aggregates_json`

Root：

```json
{
  "phaseAggregatesContractVersion": 1,
  "aggregates": []
}
```

Entry exact keys：

```json
{
  "phaseSequence": 0,
  "phaseKind": "timed_work",
  "eligibleDurationMs": 0,
  "coveredDurationMs": 0,
  "coverageBasisPoints": null,
  "coverageStatus": "no_eligible_duration",
  "conclusionEligible": false,
  "weightedBpmMs": null,
  "observedAvgBpm": null,
  "observedMaxBpm": null,
  "highestOffsetMs": null,
  "highestMutationSequence": null,
  "highestSampleSequence": null
}
```

规则：

- 数组按`phaseSequence`升序且唯一。
- 只包含primary-eligible phase；excluded phase不生成伪aggregate。
- `coverageStatus`仅`no_eligible_duration/insufficient/partial/normal`。
- eligible=0时basis NULL、status=`no_eligible_duration`、`conclusionEligible=false`。
- eligible>0时basis为0..10000；`conclusionEligible`精确使用7000bp。
- covered=0时weighted/avg NULL。
- Max四字段全NULL或全存在并引用该phase内同一raw sample。
- 各entry的eligible、covered、weighted integral按phase自身边界计算。
- 所有primary phase的eligible总和、covered总和、weighted integral总和必须分别等于whole snapshot对应值。
- Whole observed max必须等于所有合法phase max中的最大bpm，并以canonical tuple最早者作为first-highest。
- **不包含任何per-phase zone字段；删除“phase zone贡献总和与whole一致”的不可实现要求，也不新增phase-zone schema。**

### 5.7 `duration_breakdown_json`

```json
{
  "durationBreakdownContractVersion": 1,
  "canonicalSessionDurationMs": 0,
  "recordingWindowDurationMs": 0,
  "notRequestedBeforeRecordingStartMs": 0,
  "intentAxis": {
    "expectedRecordingDurationMs": 0,
    "userExcludedDurationMs": 0,
    "userTurnedOffDurationMs": 0,
    "userOptedOutDurationMs": 0,
    "userDisconnectedSuppressRecoveryDurationMs": 0
  },
  "phaseAxis": {
    "primaryEligibleDurationMs": 0,
    "phaseExcludedDurationMs": 0,
    "strengthPrepareExcludedDurationMs": 0,
    "pausedExcludedDurationMs": 0
  },
  "primaryAnalysisPartition": {
    "primaryEligibleDurationMs": 0,
    "eligibleCoveredDurationMs": 0,
    "eligibleUncoveredDurationMs": 0
  },
  "deviceStateDurations": {
    "not_observing": 0,
    "no_source_selected": 0,
    "permission_required": 0,
    "bluetooth_unavailable": 0,
    "searching": 0,
    "connecting": 0,
    "waiting_first_sample": 0,
    "live": 0,
    "stale": 0,
    "reconnecting": 0,
    "disconnected": 0,
    "technical_failure": 0
  },
  "deviceReasonDurations": {
    "initial_acquisition": 0,
    "automatic_recovery": 0,
    "source_not_selected": 0,
    "source_unavailable": 0,
    "permission_missing": 0,
    "permission_revoked": 0,
    "bluetooth_off": 0,
    "platform_unavailable": 0,
    "first_sample_timeout": 0,
    "sample_stale_timeout": 0,
    "unexpected_disconnect": 0,
    "connection_timeout": 0,
    "measurement_stream_unavailable": 0,
    "platform_failure": 0
  },
  "orthogonalityContract": {
    "contractVersion": 1,
    "rule": "primary_partition_is_mutually_exclusive_device_axes_are_independent_do_not_sum"
  }
}
```

Equations：

```text
canonicalSessionDurationMs
= notRequestedBeforeRecordingStartMs
+ userExcludedDurationMs
+ phaseExcludedDurationMs
+ primaryEligibleDurationMs

recordingWindowDurationMs
= canonicalSessionDurationMs
- notRequestedBeforeRecordingStartMs

expectedRecordingDurationMs
+ userExcludedDurationMs
= recordingWindowDurationMs

userExcludedDurationMs
= userTurnedOffDurationMs
+ userOptedOutDurationMs
+ userDisconnectedSuppressRecoveryDurationMs

phaseExcludedDurationMs
= strengthPrepareExcludedDurationMs
+ pausedExcludedDurationMs

primaryEligibleDurationMs
= eligibleCoveredDurationMs
+ eligibleUncoveredDurationMs

sum(deviceStateDurations)
= recordingWindowDurationMs

sum(deviceReasonDurations)
<= recordingWindowDurationMs
```

12 state keys和14 reason keys始终全部存在，未出现写0。

### 5.8 `quality_reasons_json`

```json
{
  "qualityReasonsContractVersion": 1,
  "sessionReasons": [],
  "phaseReasons": []
}
```

Session item：

```json
{
  "reasonCode": "partial_coverage",
  "durationMs": null
}
```

Phase item：

```json
{
  "phaseSequence": 0,
  "reasonCode": "partial_coverage",
  "durationMs": null
}
```

Fixed reason literals：

```text
no_eligible_duration
no_canonical_samples
canonical_only_excluded
eligible_uncovered_present
insufficient_coverage
partial_coverage
unavailable_no_effective_max
not_requested_before_recording_start
strength_prepare_excluded
paused_excluded
user_turned_off_excluded
user_opted_out_excluded
user_disconnected_suppress_recovery_excluded
process_interrupted
```

要求positive `durationMs`：

```text
eligible_uncovered_present
not_requested_before_recording_start
strength_prepare_excluded
paused_excluded
user_turned_off_excluded
user_opted_out_excluded
user_disconnected_suppress_recovery_excluded
```

要求`durationMs=NULL`：

```text
no_eligible_duration
no_canonical_samples
canonical_only_excluded
insufficient_coverage
partial_coverage
unavailable_no_effective_max
process_interrupted
```

`process_interrupted`只允许session scope。Session reasons按固定enum顺序；phase reasons按`phaseSequence -> enum order`；重复pair非法。

#### 5.8.1 Analysis v1 reason presence/forbidden matrix

`quality_reasons_json`不是可选diagnostic集合。对一个valid analysis v1 snapshot，validator必须由typed snapshot、5.7三个axis、raw phase/sample cut与terminal reason纯函数地产生**恰好**下表required items；required condition不成立即forbidden，除此表外不得出现其他item。

| reasonCode | Session item required iff | Phase item required iff | `durationMs` exact source |
|---|---|---|---|
| `no_eligible_duration` | whole `eligible_duration_ms=0` | 该primary-eligible phase aggregate `eligibleDurationMs=0` | NULL |
| `no_canonical_samples` | `sample_status=no_canonical_samples` | 该primary-eligible phase `eligibleDurationMs>0`且其eligible partition内canonical raw sample count=0 | NULL |
| `canonical_only_excluded` | `sample_status=canonical_only_excluded` | forbidden；这是whole sample axis，不从phase局部猜测 | NULL |
| `eligible_uncovered_present` | `intentAxis.expectedRecordingDurationMs>0`且whole `eligibleUncoveredDurationMs>0` | 该aggregate `eligibleDurationMs-coveredDurationMs>0` | 对应whole或phase差值，必须positive |
| `insufficient_coverage` | `coverage_status=insufficient` | 该aggregate `coverageStatus=insufficient` | NULL |
| `partial_coverage` | `coverage_status=partial` | 该aggregate `coverageStatus=partial` | NULL |
| `unavailable_no_effective_max` | `zone_status=unavailable_no_effective_max`且whole `eligible_duration_ms>0` | 同一snapshot zone unavailable且该aggregate `eligibleDurationMs>0` | NULL |
| `not_requested_before_recording_start` | 5.7同名duration `>0` | forbidden；recording开始前没有recording-local phase attribution | `notRequestedBeforeRecordingStartMs` |
| `strength_prepare_excluded` | 5.7同名duration `>0` | 对应`strength_prepare_set` phase与recording window交集duration `>0` | whole轴值或该phase交集duration |
| `paused_excluded` | 5.7同名duration `>0` | 对应`paused` phase与recording window交集duration `>0` | whole轴值或该phase交集duration |
| `user_turned_off_excluded` | 5.7同名intent duration `>0` | 该phase与对应acquisition interval交集duration `>0` | whole轴值或该phase交集duration |
| `user_opted_out_excluded` | 5.7同名intent duration `>0` | 该phase与对应acquisition interval交集duration `>0` | whole轴值或该phase交集duration |
| `user_disconnected_suppress_recovery_excluded` | 5.7同名intent duration `>0` | 该phase与对应acquisition interval交集duration `>0` | whole轴值或该phase交集duration |
| `process_interrupted` | session `terminal_reason=process_interrupted` | forbidden | NULL |

Additional exact rules：

- `no_eligible_duration`可以与sample-axis reason并存；它不删除真实`no_canonical_samples/canonical_only_excluded`事实。Top export status由7.5 priority决定，不由reason数组顺序决定。
- `coverage_status=no_eligible_duration`时`insufficient_coverage/partial_coverage`均forbidden；`insufficient`时只允许前者，`partial`时只允许后者，`normal`时两者均forbidden。
- `zone_status=available`或whole eligible=0时`unavailable_no_effective_max` forbidden；`process_interrupted`之外的terminal reason不得生成该item。
- Phase item的`phaseSequence`必须引用同一snapshot中的真实closed phase；session-only code出现在phase、phase-only attribution没有positive交集、duration与5.7/phase intersection不等、missing required、extra forbidden或重复pair均为`invalid_quality_reasons_v1`。
- CS-05 `AnalysisSnapshotV1Validator`是此矩阵唯一producer/validator；CS-09只做analysis-version dispatch，CS-10按validated对象export，CS-11只消费typed status/reason presentation，不重新推导。

## 6. MF-3：四family逐variant closed matrix

记号：

- `R`：Required、非NULL。
- `N`：key Required，但值可NULL，并受所列cross-field约束。
- `M`：key Required，值必须为JSON `null`。
- `="literal"`：Required且固定literal。
- 所有`Index0`为0-based non-negative integer。

Common envelope永远包含：

```text
phaseIdentityContractVersion
family
payloadVersion
mode
phaseKind
orderedStructureSignature
payload
```

### 6.1 `legacy_timed_v1`

Payload exact keys：

```text
variant
blockId
stepIndex0
legacyBlockKind
legacyStageType
itemId
exerciseId
roundIndex0
```

| variant | phaseKind | blockId | stepIndex0 | legacyBlockKind | legacyStageType | itemId | exerciseId | roundIndex0 |
|---|---|---|---|---|---|---|---|---|
| `boundary_block_work` warmup | `timed_work` | R | R | `warmup` | `warmup` | M | M | M |
| `boundary_block_work` stretch | `timed_work` | R | R | `stretch` | `cooldown` | M | M | M |
| `boundary_block_work` cooldown | `timed_work` | R | R | `cooldown` | `cooldown` | M | M | M |
| `boundary_item_work` | `timed_work` | R | R | `warmup/stretch/cooldown` | `warmup/work/cooldown/custom`，等于snapshot item literal | R | N：等于item.exerciseId或NULL | M |
| `boundary_item_rest` | `timed_rest` | R | R | `warmup/stretch/cooldown` | `rest` | R | M | M |
| `boundary_rest_after_item` | `timed_rest` | R | R | `warmup/stretch/cooldown` | `rest` | R | N：等于被引用item.exerciseId或NULL | M |
| `circuit_item_work` | `timed_work` | R | R | `timed_circuit` | `work/custom` | R | N：等于item.exerciseId或NULL | R |
| `circuit_item_rest` | `timed_rest` | R | R | `timed_circuit` | `rest` | R | M | R |
| `circuit_rest_after_item` | `timed_rest` | R | R | `timed_circuit` | `rest` | R | N：等于被引用item.exerciseId或NULL | R |
| `between_round_rest` | `timed_rest` | R | R | `timed_circuit` | `rest` | M | M | R |
| `standalone_rest` | `timed_rest` | R | R | `rest` | `rest` | M | M | M |
| `paused` | `paused` | M | M | M | M | M | M | M |

每一行的`variant`本身Required且等于表中literal。Paused不是“全部字段M”：`variant="paused"`仍Required；只有其余七个payload字段M。

Legacy mapper不得把不存在于snapshot的exercise补入rest或boundary identity。

### 6.2 `timed_composition_v2`

Payload exact keys：

```text
variant
compositionVersion
compositionBlockId
timelineStageId
timelineStageKind
stageGroupId
targetId
targetKind
roundIndex0
stageGroupIndex0
targetIndex0
stageInstanceIndex0
targetInstanceIndex0
stepIndex0
```

| variant | phaseKind | compositionVersion | compositionBlockId | timelineStageId | timelineStageKind | stageGroupId | targetId | targetKind | roundIndex0 | stageGroupIndex0 | targetIndex0 | stageInstanceIndex0 | targetInstanceIndex0 | stepIndex0 |
|---|---|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| `warmup` | `timed_work` | `2` | R | R synthetic | `warmup` | R=`timelineStageId` | R=`timelineStageId + ":target"` | `warmup` | M | M | `0` | R | R | R |
| `stage_group_action` | `timed_work` | `2` | R | R | `stage_group` | R real | R real | `action` | R | R | R | R | R | R |
| `stage_group_custom` | `timed_work` | `2` | R | R | `stage_group` | R real | R real | `custom` | R | R | R | R | R | R |
| `stage_group_rest` | `timed_rest` | `2` | R | R | `stage_group` | R real | R real | `rest` | R | R | R | R | R | R |
| `between_round_rest` | `timed_rest` | `2` | R | R synthetic | `between_round_rest` | R=`timelineStageId` | R=`timelineStageId + ":target"` | `between_round_rest` | R | M | `0` | R | R | R |
| `cooldown` | `timed_work` | `2` | R | R synthetic | `cooldown` | R=`timelineStageId` | R=`timelineStageId + ":target"` | `cooldown` | M | M | `0` | R | R | R |
| `paused` | `paused` | `2` | M | M | M | M | M | M | M | M | M | M | M | M |

所有real stage group即使`rounds=1`也处于adapter round loop，因此`roundIndex0=0`仍Required。不存在`non-repeated stage-group` variant。

### 6.3 `strength_v1`

Payload exact keys：

```text
variant
blockId
setPlanId
plannedExerciseId
actualExerciseId
exerciseSetIndex0
globalSetIndex0
setKind
substitutedFromExerciseId
```

| variant | phaseKind | blockId | setPlanId | plannedExerciseId | actualExerciseId | exerciseSetIndex0 | globalSetIndex0 | setKind | substitutedFromExerciseId |
|---|---|---|---|---|---|---|---|---|---|
| `prepare_set` | `strength_prepare_set` | R | R | R | R | R | R | R=`warmup/working/drop/backoff` | N |
| `active_set` | `strength_active_set` | R | R | R | R | R | R | R=`warmup/working/drop/backoff` | N |
| `confirm_set` | `strength_confirm_set` | R | R | R | R | R | R | R=`warmup/working/drop/backoff` | N |
| `rest` | `strength_rest` | R | R | R | R | R | R | R=`warmup/working/drop/backoff` | N |
| `paused` | `paused` | M | M | M | M | M | M | M | M |

每一行`variant`均Required。Paused明确保留`variant="paused"`；只有其余八个字段M。

Substitution规则：

```text
substitutedFromExerciseId == NULL
  -> actualExerciseId == plannedExerciseId

substitutedFromExerciseId != NULL
  -> substitutedFromExerciseId == plannedExerciseId
  AND actualExerciseId != plannedExerciseId
```

同set的四个非paused phase除`variant/phaseKind`外共享完整identity。

### 6.4 `follow_along_v1`

Payload exact keys：

```text
variant
blockId
stepIndex0
followAlongStepKind
itemId
exerciseId
roundIndex0
```

| variant | phaseKind | blockId | stepIndex0 | followAlongStepKind | itemId | exerciseId | roundIndex0 |
|---|---|---|---|---|---|---|---|
| `circuit_action` | `follow_along_action` | R | R | `action` | R | R | R |
| `non_circuit_action` | `follow_along_action` | R | R | `action` | R | R | M |
| `circuit_rest_after_action` | `follow_along_rest` | R | R | `rest_after_action` | R | R，等于被引用action | R |
| `non_circuit_rest_after_action` | `follow_along_rest` | R | R | `rest_after_action` | R | R，等于被引用action | M |
| `between_round_rest` | `follow_along_rest` | R | R | `between_round_rest` | M | M | R |
| `block_rest` | `follow_along_rest` | R | R | `block_rest` | M | M | M |
| `boundary` | `follow_along_action` | R | R | `boundary` | M | M | M |
| `paused` | `paused` | M | M | M | M | M | M |

Paused明确保留`variant="paused"`；其余六个字段M。Follow-along不得借用timed family。

### 6.5 True work/rest predicate

Legacy：

```text
legacyTrueWork =
  EXISTS phase where
    variant = circuit_item_work
    AND legacyStageType IN (work, custom)
    AND plannedDurationMs > 0

legacyTrueRest =
  (
    EXISTS phase where
      variant = circuit_item_rest
      AND plannedDurationMs > 0
  )
  OR
  (
    EXISTS phase where
      variant = circuit_rest_after_item
      AND plannedDurationMs > 0
  )
  OR
  (
    EXISTS phase where
      variant = between_round_rest
      AND plannedDurationMs > 0
  )

legacyTimedFocusEligible =
  family/version/identity/signature全部合法
  AND structure完全可解析
  AND legacyTrueWork
  AND legacyTrueRest
```

每个rest分支都独立要求positive duration。Boundary work/rest、warmup、stretch、cooldown及standalone rest不单独使toggle eligible。

Composition：

```text
compositionTrueWork =
  EXISTS phase where
    variant IN (stage_group_action, stage_group_custom)
    AND plannedDurationMs > 0

compositionTrueRest =
  (
    EXISTS phase where
      variant = stage_group_rest
      AND plannedDurationMs > 0
  )
  OR
  (
    EXISTS phase where
      variant = between_round_rest
      AND plannedDurationMs > 0
  )

compositionTimedFocusEligible =
  compositionVersion = 2
  AND adapter expansion成功
  AND all identities/signature可解析
  AND compositionTrueWork
  AND compositionTrueRest
```

Warmup/cooldown即使brownfield adapter映射为work step，也不是真实work target。

CS-06是该domain predicate的唯一production owner；CS-09、CS-11必须调用CS-06产出的同一predicate，不得复制相似逻辑。该真实consumer依赖通过§12显式`CS-06 -> CS-09`与既有`CS-06 -> CS-11`表达。

### 6.6 Focus identity v1

Exact keys：

```text
focusContractVersion
sessionId
family
structureDigestHexLowercase
focusKind
legacyBlockId
compositionBlockId
roundIndex0
phaseSequence
```

| family / focusKind | legacyBlockId | compositionBlockId | roundIndex0 | phaseSequence |
|---|---|---|---|---|
| 任一family / `whole` | NULL | NULL | NULL | NULL |
| `legacy_timed_v1` / `round` | Required，等于phase payload既有`blockId` | NULL | Required | NULL |
| `legacy_timed_v1` / `work/rest` | Required，等于phase payload既有`blockId` | NULL | Required | Required，指向同block-local round真实work/rest phase |
| `timed_composition_v2` / `round` | NULL | Required，等于phase payload既有`compositionBlockId` | Required | NULL |
| `timed_composition_v2` / `work/rest` | NULL | Required，等于phase payload既有`compositionBlockId` | Required | Required，指向同composition-block-local round真实work/rest phase |

Composition round的唯一key固定为：

```text
(sessionId, family=timed_composition_v2, structureDigestHexLowercase,
 compositionBlockId, roundIndex0)
```

`roundIndex0`从`TimedCompositionTimeline`的block-local `roundIndex`转换为0-based；不得把它提升为session-global，也不得生成新的round UUID。Legacy相邻分支同理使用已有`blockId + roundIndex0`，防止一个session含多个legacy circuit block时碰撞。

Cross-field validator：

- `round`要求该block/round tuple至少有一个phase，且所有匹配phase的payload block id与round index完全相等。
- `work/rest`的`phaseSequence`必须在同session唯一存在，family/signature/block id/round index与focus完全相等，并分别满足6.5 true-work/true-rest variant和`plannedDurationMs>0`。
- composition focus不得填`legacyBlockId`，legacy focus不得填`compositionBlockId`；whole不得携带任一block/round/phase字段；任一missing/extra/mismatch/duplicate匹配均为invalid。
- Restore必须重新验证session、family、structure signature、block-local round tuple及phaseSequence。任一失效回退`whole`；不得跨block选择相同round、选择相似identity、读current plan或从HR曲线推断。

## 7. MF-2：Export v1 closed contract

### 7.1 通用类型规则

- 所有object均为closed key set。
- Nullable member必须存在并显式为`null`。
- `string`不得用数字、boolean或object替代。
- ID字段为非空string。
- `*Ms/*Sec/*Count/*Sequence/*Index0/*Index1`为integer；duration/count/index非负。
- Wall timestamp为RFC3339 string；`generatedAt`固定UTC且包含毫秒。
- JSON number不得为NaN或Infinity。
- Object property输出顺序固定供golden/diff使用；消费者不得依赖object文本顺序。
- Unknown export/storage/display/phase version整体fail closed。

完整document：

```json
{
  "trainFlowSessionExport": {
    "exportContractVersion": 1,
    "generatedAt": "2026-08-24T00:00:00.000Z",
    "displayLocale": "zh-CN",
    "displayContractVersion": 1,
    "session": {},
    "execution": {},
    "heartRate": {}
  }
}
```

根对象不得有其他key。

### 7.2 `session`

Exact keys：

```text
sessionId
planId
mode
terminalStatus
terminalReason
startedAt
endedAt
timelineStatus
timelineVersion
trustedEndOffsetMs
canonicalSessionDurationMs
planSnapshotStorageContractVersion
planSnapshotJson
displayMetadata
```

Types/literals：

| Key | Type / NULL |
|---|---|
| `sessionId` | non-empty string |
| `planId` | string或NULL |
| `mode` | `timed/strength/follow_along` |
| `terminalStatus` | `completed/abandoned` |
| `terminalReason` | canonical时按4.1；legacy时NULL |
| `startedAt` / `endedAt` | canonical均为RFC3339 string；legacy按persisted fact为string或NULL，不伪造 |
| `timelineStatus` | `canonical_v1/legacy_incomplete` |
| `timelineVersion` | canonical固定1；legacy NULL |
| `trustedEndOffsetMs` | canonical非负integer；legacy NULL |
| `canonicalSessionDurationMs` | canonical等于`trustedEndOffsetMs`；legacy NULL |
| `planSnapshotStorageContractVersion` | canonical storage v1为integer `1`；`legacy_unversioned`为NULL；不得由session status或current serializer合成 |
| `planSnapshotJson` | 4.4 strict validator/reader接受的immutable Room `plan_snapshot_json`原文string；canonical reserialization不得替换原文 |
| `displayMetadata` | canonical为5.1对象；legacy NULL |

Canonical completed/abandoned必须满足terminal reason矩阵；legacy不得根据status反推reason。

### 7.3 `execution`

Exact keys：

```text
sessionStepRecords
phases
timedRestExtensions
strengthSetRecords
```

#### `sessionStepRecords[]`

Exact keys：

```text
stepId
kind
blockId
itemId
setPlanId
exerciseId
startedAt
endedAt
skipped
actualDurationSec
plannedDurationSec
```

Types：

- `stepId`：non-empty string。
- `kind`：`prepare/timed_work/timed_rest/strength_prepare_set/strength_active_set/strength_confirm_set/strength_rest/stretch/completed`。
- `blockId/itemId/setPlanId/exerciseId`：string或NULL。
- `startedAt`：RFC3339 string。
- `endedAt`：RFC3339 string或NULL。
- `skipped`：boolean。
- `actualDurationSec/plannedDurationSec`：non-negative integer或NULL。

明确选择：现有persisted `blockId/itemId/setPlanId/exerciseId/plannedDurationSec`全部进入export v1，key不得省略。它们作为legacy/brownfield execution hints原样导出，不替代canonical phase truth。

Machine identity/self-contained依据：

- Canonical identity来自`phases[].phaseIdentity`和immutable `planSnapshotJson`。
- Legacy step hints与plan snapshot共同保留已持久化结构。
- Exporter不得根据这些nullable hints伪造缺失phase、timeline或exercise。
- 排序按parsed `startedAt`升序，再按`stepId`升序。

#### `phases[]`

Exact keys：

```text
sequence
startOffsetMs
endOffsetMs
startMutationSequence
endMutationSequence
phaseKind
phaseIdentity
display
```

- Canonical terminal phase全部closed，end字段不得NULL。
- 排序按`sequence`升序且唯一。
- `phaseIdentity`是5.3的parsed closed object。
- `display` exact keys：

```text
displayContractVersion
locale
resolutionStatus
label
```

`displayContractVersion=1`；`locale`必须等于root `displayLocale`。

`resolutionStatus`：

```text
resolved
unresolved_missing_metadata
unresolved_invalid_metadata
unresolved_invalid_identity
unsupported_identity_version
```

规则：

```text
resolutionStatus=resolved
  -> label为non-empty string

resolutionStatus!=resolved
  -> label必须NULL
```

Unresolved分支保留machine identity，不输出伪fallback label，不查询current plan/library。

#### `timedRestExtensions[]`

Exact keys：

```text
recordId
stepId
stepIndex0
roundIndex1
restStageId
restStageTitle
previousStageId
previousStageTitle
addedSec
plannedRestSec
restElapsedBeforeExtensionSec
extensionAtRemainingSec
cumulativeExtraRestSec
eventElapsedSec
```

- `recordId/stepId/restStageTitle`为non-empty string。
- `stepIndex0`为0-based non-negative integer。
- `roundIndex1`为brownfield 1-based positive integer或NULL。
- `restStageId/previousStageId/previousStageTitle`为string或NULL。
- 其余`*Sec`均为non-negative integer；`addedSec>0`，`plannedRestSec>0`。
- `cumulativeExtraRestSec>=addedSec`。
- 排序为`eventElapsedSec -> stepIndex0 -> cumulativeExtraRestSec -> recordId`。

#### `strengthSetRecords[]`

Exact keys：

```text
recordId
exerciseId
sourceSetPlanId
setOrder
setKind
side
plannedWeight
plannedRepTarget
actualWeight
actualReps
activeDurationSec
actualRestAfterSec
effort
substitutedFromExerciseId
```

- `recordId/exerciseId`：non-empty string。
- `sourceSetPlanId`：string或NULL。
- `setOrder`：positive integer。
- `setKind`：`warmup/working/drop/backoff`。
- `side`：`both/left/right/alternating`或NULL。
- `plannedWeight/actualWeight`：`WeightValue`或NULL。
- `plannedRepTarget`：`RepTarget`或NULL。
- `actualReps`：non-negative integer或NULL。
- `activeDurationSec/actualRestAfterSec`：non-negative integer或NULL。
- `effort`：`easy/good/hard/form_breakdown`或NULL。
- `substitutedFromExerciseId`：string或NULL。
- 排序为`setOrder -> recordId`。
- Notes/user feedback仍不进入v1。

`WeightValue` exact union：

```json
{
  "value": 60.0,
  "unit": "kg"
}
```

- Exact keys=`value/unit`。
- `value`为finite non-negative JSON number，且`value >= 0`。
- `unit`仅`kg/lb`。
- 未记录重量仍使用包含字段的NULL；不得用合成的`0`代替缺失值。已持久化的显式`0`重量是合法`WeightValue`，必须无损导出。
- Brownfield production只拒绝`parsedWeight < 0.0`并可构造`WeightValue(value=0.0, unit=...)`；本条是accepted source contract的确定性传播。

`RepTarget` exact union：

```json
{ "kind": "fixed", "reps": 10 }
```

或：

```json
{ "kind": "range", "minReps": 8, "maxReps": 12 }
```

规则：

- `fixed` exact keys=`kind/reps`；`reps`为1..200 integer。
- `range` exact keys=`kind/minReps/maxReps`；二者为1..200 integer且`minReps<=maxReps`。
- 不允许在fixed对象中出现range keys，反之亦然。

### 7.4 `heartRate`

Exact keys：

```text
status
recording
intentAndAcquisition
samples
originalAnalysis
durationAudit
```

Top status仅：

```text
not_recorded
no_eligible_duration
zero_samples
insufficient
partial
recorded_no_zones
recorded
```

#### `recording`

Exact keys：

```text
recordingId
status
startedOffsetMs
startedMutationSequence
endedOffsetMs
endedMutationSequence
sourceContractVersion
sourceKind
acquisitionContractVersion
parameterSnapshotVersion
age
personalMaxBpm
effectiveMaxBpm
effectiveMaxSource
alertThresholdBpm
zoneSnapshot
originalAnalysisVersion
```

Terminal export有recording时：

- `status="terminal"`。
- start/end offset与mutation均为non-negative integer。
- source/acquisition/parameter版本固定1，`sourceKind="ble_hrs"`。
- `age`为1..130 integer或NULL。
- BPM参数为30..260 integer或NULL。
- effective max/source/zone snapshot遵守3.3矩阵。
- `originalAnalysisVersion=1`。
- Active recording、缺end或缺binding均不输出export。

#### `intentAndAcquisition[]`

Exact keys：

```text
sequence
startOffsetMs
startMutationSequence
endOffsetMs
endMutationSequence
recordingIntent
intentReason
deviceState
deviceReason
```

- Terminal export的interval全部closed。
- 按`sequence`升序且唯一。
- Intent、reason、device state/reason使用3.4 literals及allowed-pair矩阵。
- 不输出open marker、raw provider state或diagnostic。
- Recording开始前时长不伪造interval。

#### `samples[]`

Exact keys：

```text
sampleSequence
offsetMs
mutationSequence
bpm
```

排序固定：

```text
offsetMs -> mutationSequence -> sampleSequence
```

`sampleSequence`仍是stable identity；same-offset不折叠。

#### `originalAnalysis`

Exact keys：

```text
analysisVersion
createdAt
inputLastMutationSequence
analysisConfig
sampleStatus
coverageStatus
zoneStatus
status
canonicalSampleCount
primaryPointSampleCount
eligibleDurationMs
coveredDurationMs
coverageBasisPoints
weightedBpmMs
observedAvgBpm
observedMaxBpm
highestOffsetMs
highestMutationSequence
highestSampleSequence
zoneDurations
phaseAggregates
qualityReasons
```

Types/cross-field：

- `analysisVersion=1`。
- `createdAt`为RFC3339 string。
- `analysisConfig/zoneDurations/phaseAggregates/qualityReasons`分别复用5.4/5.5/5.6/5.8 parsed object。
- 三个axis literal与DDL一致。
- `status`必须等于top `heartRate.status`。
- Counts/durations为non-negative integer。
- `coverageBasisPoints=NULL` iff eligible=0；否则0..10000。
- `weightedBpmMs/observedAvgBpm=NULL` iff covered=0。
- Max四字段全NULL或全存在。
- `zoneDurations=NULL`当且仅当`zoneStatus=unavailable_no_effective_max`或eligible=0；available且eligible>0时必须存在。
- `durationAudit`使用5.7 parsed object，且其whole durations等于typed snapshot字段。

### 7.5 Legacy与no-HR正交矩阵

Timeline axis：

| Axis | session timeline fields | execution.phases |
|---|---|---|
| `canonical_v1` | version/end/duration/display metadata存在 | 完整ordered phases |
| `legacy_incomplete` | timeline字段与display metadata为NULL | empty；不伪造canonical phases |

Heart-rate axis：

| Axis | recording | arrays | analysis |
|---|---|---|---|
| `not_recorded` | NULL | acquisition/samples均empty | originalAnalysis/durationAudit均NULL |
| recording present | terminal object | 按事实 | originalAnalysis/durationAudit存在 |

允许组合：

| Timeline | HR | 结果 |
|---|---|---|
| Legacy | no-HR | 合法legacy terminal export，`status=not_recorded` |
| Canonical | no-HR | 合法canonical no-HR export，`status=not_recorded` |
| Canonical | recording | 按zero/no-eligible/insufficient/partial/normal分支 |
| Legacy | recording | 非法，typed invariant failure；不得合并解释为“Legacy/no HR” |

HR recording分支：

Analysis v1的top `heartRate.status`由CS-05冻结的`StatusProjectionV1` pure function按下列完整优先级求值；CS-09/10/11只消费结果，不各自重排：

```text
if recording is absent:
  not_recorded
else if terminal recording/snapshot/original binding invariant is invalid:
  typed invariant failure; no export document
else if eligible_duration_ms == 0:
  no_eligible_duration
else if canonical_sample_count == 0:
  zero_samples
else if coverage_status == insufficient:
  insufficient
else if coverage_status == partial:
  partial
else if coverage_status == normal AND zone_status == unavailable_no_effective_max:
  recorded_no_zones
else if coverage_status == normal AND zone_status == available:
  recorded
else:
  invalid_status_projection_v1; no export document
```

因此`eligible_duration_ms=0 && samples=[]`唯一投影为`no_eligible_duration`，不是`zero_samples`。`canonical_only_excluded`在eligible>0时按coverage axis投影`insufficient/partial/normal`，其原因仍由5.8精确保留。

| Branch | 规则 |
|---|---|
| No eligible | priority 3；`eligible_duration_ms=0`、status=`no_eligible_duration`、basis NULL；即使samples empty也不改为zero |
| Zero sample | priority 4；`eligible_duration_ms>0`且`canonical_sample_count=0`；analysis存在；status=`zero_samples` |
| Insufficient | status=`insufficient` |
| Partial | status=`partial` |
| Normal/no effective max | `zoneDurations=NULL`；status=`recorded_no_zones` |
| Normal/zones | zone object存在；status=`recorded` |
| Recording缺binding | 整份export失败 |
| Unknown/corrupt version | 整份export失败 |

## 8. MF-4：`CC-D03-B` closed lifecycle/provider合同

### 8.1 文件、URI与manifest

文件位于CS-10专用App-private/no-backup目录：

```text
e17-session-export-v1-<opaqueLeaseId>.json.part
e17-session-export-v1-<opaqueLeaseId>.json.ready
e17-session-export-v1-<opaqueLeaseId>.lease.json
```

URI exact identity：

```text
content://com.liujyks.trainflow.e17export/lease/<opaqueLeaseId>
```

`opaqueLeaseId`是不可预测随机ID，不含session/user/device identity。

Manifest exact keys：

```json
{
  "shareLeaseContractVersion": 1,
  "opaqueLeaseId": "opaque-random-id",
  "state": "writing",
  "readyFileName": null,
  "contentUri": null,
  "dispatchBootCount": null,
  "dispatchElapsedRealtimeMs": null,
  "expiresAtElapsedRealtimeMs": null
}
```

### 8.2 Closed state matrix

| State | `readyFileName` | `contentUri` | `dispatchBootCount` | `dispatchElapsedRealtimeMs` | `expiresAtElapsedRealtimeMs` |
|---|---|---|---|---|---|
| `writing` | M | M | M | M | M |
| `ready_undispatched` | R，等于exact `.ready` filename | R，等于exact URI | M | M | M |
| `dispatch_committed` | R | R | R | R | R |

Cross-field：

```text
manifest opaqueLeaseId
= URI path opaqueLeaseId
= ready filename opaqueLeaseId
= manifest filename opaqueLeaseId

expiresAtElapsedRealtimeMs
= checkedAdd(dispatchElapsedRealtimeMs, 600000)
```

`ready_undispatched`拥有URI identity但尚无platform dispatch/grant。Provider仍要求`dispatch_committed`，因此此状态的open fail closed。

### 8.3 唯一状态转换和持久化顺序

唯一前向转换：

```text
writing
-> ready_undispatched
-> dispatch_committed
-> physical cleanup/delete
```

禁止反向、跳转、复用旧lease ID或把cleanup建模为第四个可打开状态。

Creation/write顺序：

1. 生成opaque ID。
2. 通过temp-manifest write/flush/file-sync/atomic replace持久化`writing` manifest。
3. 创建`.part`并stream write。
4. Flush、file-sync、close `.part`。
5. 从`.part`执行closed-schema validation。
6. 在同一目录原子rename `.part -> .ready`。
7. 通过temp-manifest durable replace写`ready_undispatched`，固定`readyFileName/contentUri`。
8. Share前读取`BOOT_COUNT`与`elapsedRealtime`，checked计算expiry。
9. Durable replace为`dispatch_committed`。
10. 仅在步骤9成功后向platform dispatch只读URI grant。
11. 同步dispatch明确失败时revoke并按pre-dispatch failure cleanup；已经可能交给platform但无法证明失败时保守保留至expiry。

Save/CreateDocument不进入`dispatch_committed`；完成或取消后清理`ready_undispatched`。

Cleanup顺序：

```text
best-effort revoke
-> delete .ready/.part
-> delete manifest
-> delete manifest temp residue
```

删除manifest必须晚于删除可读文件，避免遗留无manifest `.ready`。

### 8.4 Crash-window恢复矩阵

| Crash window | Durable/physical结果 | 下一owner行为 |
|---|---|---|
| manifest创建前 | 可能有无manifest `.part` | exact-prefix orphan删除 |
| `writing`已提交、part未创建 | manifest only | 删除manifest |
| part写入中 | `writing + .part` | 删除part和manifest |
| validation后、rename前 | `writing + .part` | 删除 |
| rename后、ready manifest前 | `writing + .ready` | 视为未dispatch orphan，删除 |
| `ready_undispatched`提交后、dispatch前 | ready+manifest，无grant | 删除 |
| dispatch manifest temp写入中 | durable旧状态仍为ready | Binder dispatch尚未调用；按ready删除 |
| `dispatch_committed`提交后、Binder前 | 保守视为可能dispatch | 按lease保留到expiry |
| Binder dispatch中/后 | committed | 按lease |
| revoke后、file delete前 | expired manifest仍在 | provider因expiry fail closed；下一owner继续删 |
| ready删除后、manifest删除前 | manifest但file缺失 | provider fail closed；下一owner删manifest |
| manifest删除后 | 不得残留ready；若异常残留按unmanifested orphan删除 | fail closed并cleanup |

### 8.5 Lease identity、boot与expiry

```text
lease identity =
  (shareLeaseContractVersion=1,
   opaqueLeaseId,
   dispatchBootCount,
   dispatchElapsedRealtimeMs,
   expiresAtElapsedRealtimeMs)
```

Boot identity使用Android `Settings.Global.BOOT_COUNT`。Share前无法取得boot identity时：

```text
share_boot_identity_unavailable
```

Share fail closed；Save不受影响。

Expiry：

```text
currentBootCount != dispatchBootCount
  -> expired

currentBootCount == dispatchBootCount
AND elapsedRealtimeMs >= expiresAtElapsedRealtimeMs
  -> expired

otherwise
  -> unexpired
```

等号即expired。

### 8.6 Lease-aware provider open

每次`openFile()`必须验证：

1. URI scheme/authority/path完全匹配fixed provider URI。
2. URI只含一个validated opaque lease ID。
3. Manifest存在、是closed v1 object且state=`dispatch_committed`。
4. `manifest.contentUri`与请求URI字节等价。
5. Manifest、URI、manifest filename、ready filename的opaque ID全部相同。
6. `readyFileName`没有path separator/traversal且等于exact expected filename。
7. `.ready`位于专用no-backup目录并真实存在。
8. Boot相同。
9. `elapsedRealtimeMs < expiresAtElapsedRealtimeMs`。
10. Open mode只读。

任一失败不返回descriptor、不fallback普通FileProvider。Expired时先拒绝新的open，再best-effort revoke/cleanup。

Expiry前已经取得且仍持有的OS file descriptor：

- App不承诺后续revoke/unlink使该descriptor失效。
- 不宣称recipient完成、失败或停止读取。
- 该平台边界不延长任何新的provider open资格。

### 8.7 Startup/provider cleanup

每process的CS-10 capability内部执行一次serialized cleanup gate，所有新export等待。

| 状态 | 行为 |
|---|---|
| `.part`或`writing` | 未dispatch orphan，删除 |
| `ready_undispatched` | 未dispatch，删除 |
| committed、同boot且未expiry | 保留manifest/ready/lease，不提前revoke |
| committed、同boot且已expiry | 新open禁止；revoke并cleanup |
| committed、boot变化 | expired；revoke并cleanup |
| manifest missing但有exact-prefix residue | provider fail closed；删除residue |
| invalid/corrupt manifest | provider fail closed；按专用目录、exact prefix/suffix和可解析opaque ID清理；绝不开放URI |
| cleanup IO失败 | `share_cleanup_failed`；阻止新export并保留原始cause于protected diagnostic |

Invalid manifest若opaque ID可安全解析，先derive fixed URI并best-effort revoke；若不能解析，删除本地residue即可使后续open因manifest/file缺失失败。不得信任corrupt manifest提供的任意path或URI。

无App进程或其他合法owner在expiry时被唤醒：

- 不新增`SCHEDULE_EXACT_ALARM`。
- 不新增exact-alarm receiver或boot receiver。
- `.ready`可暂留App-private/no-backup。
- 下一次provider open先判expired并拒绝，再cleanup。
- 下一次startup晚于expiry时cleanup。
- 不声称恰好在第`600000ms`完成物理删除。

### 8.8 CS-11 handoff

Share普通return固定只能显示：

```text
已交给系统分享
```

禁止：

- `分享成功`
- chooser/Activity resume/ordinary ActivityResult作为recipient completion
- 以上事件触发提前revoke/delete

Save只在目标stream write/flush/close全部成功后显示App写入完成。外部copy不受local delete控制。

## 9. MF-5：完整 `P-BALANCED-V2`

### 9.1 固定测量环境

```text
AVD=TrainFlow_Pixel_API_36
AVD_HOME=C:\Users\25073\Desktop\jianshen\.local\android-avd
CONFIG=C:\Users\25073\Desktop\jianshen\.local\android-avd\TrainFlow_Pixel_API_36.avd\config.ini
CONFIG_SHA256=8276A65E3A6E0867E25F96B4940ECE639EF5AD1F75E4E41625684955C142375E

API=36
tag=Google APIs
ABI=x86_64
hw.cpu.ncore=4
hw.ramSize=2G
vm.heapSize=228M
hw.gpu.enabled=no
hw.gpu.mode=auto
resolution=1080x2400
density=420dpi
```

Projector测试只测pure read/projection，不产生Compose render、GPU、frame-time或视觉性能claim。

### 9.2 JDK、Gradle与project identity

Fresh PowerShell执行Gradle前必须：

```powershell
. 'C:\Users\25073\Desktop\jianshen\.local\env.ps1'
java -version
```

JDK：

```text
JAVA_HOME=C:\Users\25073\Desktop\jianshen\.local\jdk\jdk-17.0.19+10
OpenJDK Temurin 17.0.19+10
```

禁止依赖未配置的system PATH，禁止安装、升级或替换JDK。

Project/build：

```text
compileSdk=36
targetSdk=36
minSdk=26
AGP=9.2.0
Gradle=9.4.1
variant=debug
applicationId=com.liujyks.trainflow
testApplicationId=com.liujyks.trainflow.test
runner=androidx.test.runner.AndroidJUnitRunner
```

Build命令：

```powershell
.\gradlew.bat --no-daemon --rerun-tasks :app:assembleDebug :app:assembleDebugAndroidTest
```

Artifacts：

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
```

必须记录JDK vendor/version以及Gradle命令exit code；非0即失败。

### 9.3 固定instrumentation class

```text
E17FinalizerPerformanceContractTest
E17ProjectorPerformanceContractTest
E17ExportPerformanceContractTest
```

不得用普通unit test、host JVM benchmark、旧APK、其他class或未绑定candidate的报告替代。

### 9.4 Source、APK与installed identity

每次证据必须满足：

1. Worktree clean。
2. Index empty。
3. `SOURCE_SHA=git rev-parse HEAD`完整SHA。
4. `SOURCE_SHA`等于该Story immutable candidate full SHA。
5. 计算并记录两个APK SHA-256。
6. 只安装这两个已记录hash的APK。
7. 对app与test package分别记录`pm path`。
8. 对两者分别记录package name、versionCode/versionName、sourceDir、targetSdk、debuggable、signing identity；test package还记录runner与target package。
9. 从两个`pm path`分别拉回installed base APK。
10. 拉回app base APK SHA-256必须等于`app-debug.apk` SHA-256。
11. 拉回test base APK SHA-256必须等于`app-debug-androidTest.apk` SHA-256。
12. 记录AVD serial、`ro.boot.qemu.avd_name`、SDK、ABI、build fingerprint、config SHA。
13. 任一identity不一致即blocker；不得继续测量或换artifact。

CS-12必须在最终整合candidate SHA上重新build、安装并重跑全部三项performance contract；不得汇总CS-05/09/10旧candidate结果代替。

### 9.5 测量方法

每项：

- 2次warmup，不计入判定。
- Finalizer/projector各5次measured repetition。
- Export 3次measured repetition。
- 每个measured repetition都必须通过；不使用平均值掩盖单次超限。
- 时间使用`SystemClock.elapsedRealtimeNanos()`单调时钟。
- Measurement window开始前取一次PSS，期间每50ms读取目标App进程`Debug.MemoryInfo.totalPss`，结束边界后立即再取一次。
- 判定整个window中的绝对peak total PSS；不以heap allocation、delta PSS或平均PSS替代。
- 测量期间不得并行Gradle build、第二AVD、其他TrainFlow instrumentation或另一性能合同。

准确计时边界：

| Contract | Start | End |
|---|---|---|
| Finalizer | 调用repository terminal finalization、进入事务前的最后一个单调时钟点 | snapshot insert、original binding、session/recording terminal update及Room transaction commit全部成功后的第一个单调时钟点 |
| Projector | 已验证consistent-read输入完全materialized后、调用pure projector前 | 完整projection对象返回且mandatory/non-mandatory结果集合已构造后的第一个单调时钟点 |
| Export | CS-10开始consistent export read transaction前 | stream write、flush、file-sync、close、closed-schema validation、`.part -> .ready` rename及`ready_undispatched` manifest durable commit全部完成后的第一个单调时钟点 |

Share dispatch、chooser和CreateDocument目标写入不计入export preparation时间。

### 9.6 固定风险profile

共同输入：

```text
canonical session duration=8h=28800000ms
canonical samples=250000
phase intervals=10000
acquisition intervals=10000
same-offset burst<=32
```

Profile必须同时包含：

- 至少一个phase zero-duration mutation：相同offset、严格增加mutation sequence。
- 至少一个acquisition zero-duration mutation，同样保留tuple。
- Boundary-heavy数据：phase/acquisition边界前、边界exact tuple及边界后均有sample事实。
- 至少一个`>2500ms` sample-validity gap。
- 至少一个`>20000ms` projector visual gap。
- Intent expected与三种user-excluded reason。
- Primary eligible、strength prepare excluded、paused excluded。
- 12 device states与所有合法non-NULL reason pair。
- Six-zone覆盖，且有效maximum和zone snapshot合法。
- 同一最高BPM至少出现两次，验证canonical first-highest。
- 至少一个exact 32-sample same-offset burst，以mutation/sample sequence稳定排序。
- Zero/no-eligible/partial等小分支由功能合同覆盖；performance主profile不得通过删除boundary、excluded或quality事实减小负载。

250000 samples约等于8小时平均8.7Hz，高于accepted Band 9约1Hz事实，但不是产品输入硬上限。

### 9.7 时间、PSS与输出门槛

| Owner | Max time | Peak total PSS | Output |
|---|---:|---:|---|
| CS-05 finalizer | `8000ms` | `384MiB` | 五个analysis JSON合计UTF-8 bytes `<=16MiB`；不得丢phase/quality事实 |
| CS-09 projector | `1500ms` | `320MiB` | 全部mandatory anchors + `<=1600` non-mandatory；总点数`<=mandatoryAnchorCount+1600` |
| CS-10 export | `30000ms` | `384MiB` | 完整validated JSON `<=128MiB`；不得截断、降级或整文件驻内存 |

Projector另固定：

```text
plotWidthPx=1600
phaseBands<=10000
mandatoryAnchors<=20000
```

所有phase boundary、gap edge、first-highest和selection anchor必须保留。

超过128MiB表示该冻结风险profile失败，不允许截断，也不建立用户可见训练时长硬上限。

### 9.8 AVD禁止修改与blocker

禁止：

- 创建、clone、wipe或升级AVD。
- 下载或替换system image。
- 修改`config.ini`。
- 改变RAM、CPU、GPU、ABI、API、density、resolution或VM heap。
- 用启动参数覆盖`-memory/-cores/-gpu`或QEMU硬件。
- 换用另一AVD、物理设备或host JVM获得更好数字。
- 把GPU关闭解释为环境缺陷。
- 把projector pure-function结果冒充Compose rendering性能。

AVD缺失、config SHA不匹配、runtime不是API36/x86_64/2GiB/4核、JDK不匹配、指定APK不能build/install或installed hash不匹配时，必须返回environment/identity blocker；不得自动修复环境。

## 10. SQLite不能表达约束的唯一owner

| Constraint | Validator/owner | DAO guard | Evidence |
|---|---|---|---|
| Session legacy/canonical header tuple | CS-03 `CanonicalSessionHeaderV1Validator` | CS-04 expected status+tuple，rowCount=1 | CS-03五状态migration matrix；CS-04 reconciliation |
| Partial seven-column header | CS-03 | 任何mutation前拒绝 | invalid tuple matrix |
| Canonical cross-row graph与final input cut | CS-03 `CanonicalSessionGraphV1Validator`；CS-04 ongoing调用；CS-05 terminal调用 | 4.2.3 exact expected tuple/open row，逐guard rowCount=1，terminal whole transaction | 4.2.3逐项illegal fixtures、rollback、CS-12 cross-layer |
| Canonical plan snapshot storage v1 | CS-03 canonical writer + `PlanSnapshotStorageV1Validator` | StartSession写前strict v1 | root/block closed shape、literal version 1、key/type/mode及persisted UTF-8 byte identity |
| Legacy unversioned plan snapshot | CS-09 `LegacyUnversionedPlanSnapshotReader` | strict read only；原始row不回写 | `legacy_unversioned`、no `fallbackMode`、no default、no element drop、unknown/corrupt/mode mismatch及no-rewrite evidence |
| Phase family/variant/key set | CS-03 `PhaseIdentityV1Validator` | CS-04写前调用 | exhaustive per-variant tests；CS-06/07/08 mapper |
| Display metadata append/immutability | CS-03 validator；CS-04 mutation owner | expected active + insert-if-absent，rowCount=1 | append/race/terminal tests |
| Recording source/parameter/zone | CS-03 `RecordingHeaderV1Validator` | CS-04 guarded create/terminal | complete pair matrix |
| Device state/reason | CS-03 `AcquisitionV1Validator`；CS-04 runtime mapper | close/open expected row，rowCount各=1 | full allowed/illegal pair tests |
| Monotonic tuple | CS-04 repository | expected last tuple，rowCount=1 | concurrency/property tests |
| Interval gap/overlap/open/partition | CS-04写；CS-05 finalize | unique open + guarded closure | transaction/finalizer evidence |
| Snapshot axes/count/duration/anchor/JSON | CS-05 `AnalysisSnapshotV1Validator` | insert conflict=fail | exhaustive/property/rollback |
| Status projection与quality reason presence | CS-05 `StatusProjectionV1` + `AnalysisSnapshotV1Validator` | snapshot insert前exact projection/reason set；冲突rollback | 全priority overlap、每code required/forbidden/session-phase scope |
| Threshold/overflow/whole aggregate sums | CS-05 checked finalizer | wholetransaction rollback | boundary/overflow tests |
| Per-phase zone absence | CS-05 schema validator | 不生成字段 | closed-key negative tests |
| Original binding | CS-05 repository | NULL→1、same recording、rowCount=1 | binding/race tests |
| Unknown/corrupt read | CS-09 version-aware reader | 无fallback query、无`fallbackMode` parser | storage/analysis/phase typed failure evidence |
| Focus block-local identity | CS-06 focus/predicate owner；CS-09 projector consumer | CS-09只接受6.6 exact focus tuple | multi-block same-round、cross-block mismatch、restore fallback |
| Historical display | CS-09 resolver | 无current-plan read | resolved/unresolved matrix |
| Export key/type/order/branch | CS-10 serializer+validator | validation后rename | goldens/round-trip/negative |
| Lease/manifest/provider | CS-10 lease owner | durable forward-only transition | crash/provider/expiry matrix |
| UI handoff/toggle | CS-11 | 只消费typed结果 | AVD/state/accessibility |
| Final source/runtime consistency | CS-12 evidence-only | production delta=0 | final candidate pack |

## 11. Story AC与evidence传播

Ownership和Story数量不变；以下是必须写入对应Story的新增/替换AC与evidence，不是owner摘要。

### CS-03 — Canonical schema and migration foundation

新增/替换AC：

1. 实现本V10保留的完整`Migration(4,5)`和五表DDL；Room version/base变化时写前停止。
2. v4 `ready/active/paused/completed/abandoned` migration fixture全部保留七列NULL。
3. `active/paused + all seven NULL`合法分类为legacy noncanonical；不得被validator误判canonical。
4. 七列部分NULL/部分canonical为invalid partial header。
5. 冻结全部version组合及八个closed JSON。
6. `PhaseIdentityV1Validator`覆盖本V10保留的所有variant和exact key set，包括各family paused。
7. `phase_aggregates_json`禁止per-phase zone member。
8. 提供session/phase/display/recording/acquisition validators，不承担runtime mutation。
9. 实现4.2唯一`CanonicalSessionGraphV1Validator`，覆盖header、phase/recording/acquisition/raw cut/snapshot binding全部predicate；CS-04/05调用同一实现。
10. 将既有plan snapshot writer升级为4.4显式storage v1并提供byte-stable strict validator；只负责canonical root/block closed shape、literal version 1、key/type/mode和persisted UTF-8 byte identity，不读取或解释`legacy_unversioned`。
11. 对八个closed storage JSON只承担version/key/type/NULL/closed-shape等structural schema validation；不实现`StatusProjectionV1`、status priority或quality reason required-iff/forbidden/scope/duration语义。

Evidence：

- Fresh v4、五种legacy status及fresh v5 migration tests。
- `PRAGMA table_info/foreign_key_list/index_list`与DDL identity。
- 全部legal/illegal header tuple。
- JSON structural missing/extra/wrong-type/null/version/closed-key fuzz。
- 四family逐variant exhaustive matrix。
- Explicit negative：strength/follow paused缺variant、paused存在位置字段、phase aggregate加入zone字段均失败。
- 4.2.3全部cross-row illegal fixtures；canonical plan snapshot v1 root/block missing/extra/wrong-type/wrong-version/mode mismatch/canonical-byte mismatch均失败。

### CS-04 — Recorder, guarded writes and reconciliation

新增/替换AC：

1. 首次受保护API经R-A single-flight gate扫描并先分类legacy/canonical/invalid partial。
2. Legacy `active/paused`保留status和七列NULL，返回typed residual；不写terminal、不生成timeline/reason/snapshot。
3. Legacy residual不复活engine/notification/FGS，也不阻止新的canonical StartSession。
4. Canonical nonterminal继续按`(session_id,last_durable_offset_ms,last_mutation_sequence,reconciliation_contract_version=1)` CAS reconcile为`abandoned/process_interrupted`。
5. 所有phase、display metadata、recording、acquisition mutation写前调用CS-03 validator。
6. Guarded close/open、terminal和metadata append必须检查rowCount=1。
7. Invalid partial、unknown version和corrupt JSON为nonretryable/manual-resolution-required。
8. 每次ongoing mutation按4.2.3匹配expected header/open row/input cut并检查每个rowCount=1；写后同transaction复验partition，失败保留原始signal并rollback。

Evidence：

- Legacy active/paused process-relaunch no-mutation DB diff。
- Canonical/legacy混合数据库扫描。
- Canonical reconcile winner/loser/idempotency。
- Partial header failure。
- State/reason完整矩阵。
- Zero-duration mutation、same-offset sequence、concurrent terminal/append。
- Downstream gate result同时包含success与legacy residual而不混为failure。
- Phase/acquisition gap、overlap、non-tail open、stale expected tuple、rowCount 0/2与sample-after-cut逐项证明无partial write。

### CS-05 — Finalizer and bound snapshot

新增/替换AC：

1. 仅消费完整v1 header/phase/acquisition/sample facts。
2. 按4.2.2/4.2.3以同一`finalT`原子关闭phase/acquisition/recording/session，生成5.6–5.8对象并绑定同一input sequence的original version。
3. Phase aggregates只保存本V10保留字段；禁止新增phase-zone字段。
4. Whole/phase eligible、covered及integral exact一致；first-highest由canonical tuple决定。
5. Eight JSON/version/axis/anchor任一不一致整体rollback。
6. 实现并运行`E17FinalizerPerformanceContractTest`，使用完整`P-BALANCED-V2` profile、timing、PSS及output门槛。
7. Performance evidence绑定CS-05 immutable candidate SHA与已安装APK identity。
8. CS-05唯一实现`StatusProjectionV1`并严格执行7.5完整优先级；CS-05唯一拥有`AnalysisSnapshotV1Validator`的5.8 required-iff/forbidden/scope/duration semantic validation，任何missing/extra/wrong scope/duration整体rollback。

Evidence：

- Threshold等号、zero/no eligible、partial/normal、zone unavailable。
- Zero-duration/boundary/gap/excluded/eligible/six-zone/first-highest/same-offset burst。
- Phase sum/integral与whole；明确没有phase-zone test/schema。
- Overflow/rollback/original binding race。
- Durable/trusted/final recording tuple、phase/acquisition terminal coverage、sample cut、snapshot input sequence和binding逐项正负矩阵。
- `eligible=0 + samples=[]`投影`no_eligible_duration`，以及所有status priority overlap和每个reason missing-required、extra-forbidden、wrong scope、wrong duration negative matrix。
- 2 warmups + 5 measured runs，全部`<=8000ms/384MiB/16MiB`。

### CS-06 — Timed integration and focus eligibility

新增/替换AC：

1. Legacy/composition mapper逐行生成第6节exact variant，不使用“其他字段同上”逻辑。
2. Legacy boundary/circuit/rest-after/round/standalone及paused都经过同一validator。
3. Composition删除non-repeated variant；rounds=1仍写`roundIndex0=0`。
4. 使用第6.5唯一true work/rest predicate；每个rest分支都要求positive duration。
5. 只有结构、version、signature、work/rest全部合法时输出focus eligibility。
6. 输出第6.6 stable focus identity；composition使用已有`compositionBlockId + roundIndex0`，legacy使用已有`blockId + roundIndex0`；不创建toggle或UI。
7. Unsupported/corrupt结构fail closed，不从curve推断。

Evidence：

- 每个legacy/composition variant逐字段golden。
- Paused、zero-duration、boundary、rest-after、rounds=1、between-round synthetic IDs。
- Warmup/cooldown不是true work。
- 每个rest分支在duration=0和positive下的predicate tests。
- Focus eligibility与restore identity fixtures交给CS-09/11共同复用。
- 同session两个composition blocks均含`roundIndex0=0`、两个legacy blocks同round、cross-block phaseSequence与wrong-family block key均fail closed。

### CS-07 — Strength integration adjacent propagation

新增/替换AC：

1. 五个strength variants完全遵守6.3。
2. `paused`只允许`variant="paused"`，其余八字段NULL。
3. 四个set phases共享稳定identity。
4. Substitution组合严格验证。

Evidence：

- 每variant exact JSON golden。
- Paused extra-field negative tests。
- No-substitution/substitution illegal combinations。
- Prepare excluded与active/confirm/rest eligible。

### CS-08 — Follow-along integration adjacent propagation

新增/替换AC：

1. 八个follow-along variants完全遵守6.4。
2. `paused`只保留`variant="paused"`。
3. Action/rest-after-action引用真实item/exercise；block/boundary休息按矩阵NULL。
4. 不借timed family。

Evidence：

- 八variant exact JSON golden。
- Circuit/non-circuit round matrix。
- Paused extra-field negative。
- Unsupported future composition/chapter fail typed。

### CS-09 — Version-aware read/resolver/projector

新增/替换AC：

1. Legacy nonterminal读为`legacy_noncanonical_nonterminal`，不当成terminal/corruption。
2. Terminal legacy用`legacy_incomplete`；canonical用`canonical_v1`。
3. Storage/execution/export read按第4.4/7节type、真实plan snapshot version、NULL和stable order提供一致输入；canonical只消费CS-03 strict v1结果，CS-09唯一实现`LegacyUnversionedPlanSnapshotReader`，不得使用`fallbackMode`、default或element drop，unknown/corrupt/mode mismatch返回typed failure且绝不回写原始row。
4. Resolver的unresolved分支必须输出NULL label和typed status。
5. 作为`CS-06 -> CS-09`material consumer，只调用CS-06共享predicate生成whole/round/work/rest projection。
6. 按6.6验证family-specific block id + block-local round + phaseSequence；Restore identity失效回退whole，不跨block、不猜相似phase。
7. 实现`E17ProjectorPerformanceContractTest`及完整`P-BALANCED-V2`。

Evidence：

- Original binding/unknown version/delete race。
- Legacy active/paused、legacy terminal、canonical no-HR正交read。
- Resolver resolved及四类unresolved。
- Timed focus predicate与restore。
- Canonical plan snapshot v1消费；legacy unversioned strict read、explicit unknown version、corrupt/mode mismatch、no `fallbackMode`、no default、no `mapNotNull`/element drop及no rewrite；multi-block same-round projection/restore。
- 2 warmups + 5 measured runs，全部`<=1500ms/320MiB`，全部mandatory且non-mandatory≤1600。

### CS-10 — Unified export capability

新增/替换AC：

1. 实现第7节完整export v1 exact keys/types/literals/NULL/sort/cross-field，包括7.5 `StatusProjectionV1`结果与5.8 exact reasons。
2. SessionStepRecord五个persisted结构字段全部进入export。
3. Legacy no-HR与canonical no-HR分别golden；legacy+recording是invariant failure。
4. Phase display unresolved使用NULL label，不输出伪文案。
5. Storage JSON以4.4 validated persisted原文导出，不重算或容错；canonical输出plan snapshot version `1`，legacy unversioned输出NULL，禁止合成版本。
6. 实现第8节三状态manifest、durable顺序、crash recovery、provider identity验证及corrupt cleanup。
7. 保持`CC-D03-B`：expiry后新open fail closed；无owner时允许private physical residue。
8. 实现`E17ExportPerformanceContractTest`及完整`P-BALANCED-V2`。
9. Delete/export仍按consistent read transaction线性化。

Evidence：

- Export full-schema goldens及extra/missing/type/null/version negative tests。
- `WeightValue(value=0, unit=kg)`与`WeightValue(value=0, unit=lb)`合法golden；显式`0`必须无损导出，negative、NaN、Infinity均非法并fail closed。
- `RepTarget`全部union与invalid bounds保持不变。
- SessionStepRecord全字段presence。
- Legacy/no-HR/zero/no-eligible/partial/no-zone/normal/abandoned分支。
- `eligible=0 + samples=[]`及每个status priority overlap golden；每个quality reason required/forbidden/session-phase/duration negative。
- Plan snapshot canonical v1/legacy NULL version goldens；unknown/corrupt/mode mismatch/fallback/reserialization均整份失败。
- Array stable-order和same-offset samples。
- 三manifest状态逐字段tests。
- 每个crash window、early/late reboot、boot change、expiry equality。
- URI/manifest/filename/opaque mismatch。
- Corrupt/missing manifest及residual cleanup。
- 已持有descriptor边界，不冒充revoke证明。
- 2 warmups + 3 measured runs，全部`<=30000ms/384MiB/128MiB`。

### CS-11 — Shared S4–S7 and export handoff UI

新增/替换AC：

1. 只消费CS-09投影的CS-06同一predicate和6.6 focus identity；不重新实现predicate或block/round matching。
2. Toggle仅在true work+true rest+resolved structure时显示；默认whole。
3. Restore invalid回退whole；不从curve猜。
4. Legacy nonterminal显示typed unavailable/非terminal状态，不伪造zero或canonical recap。
5. Share dispatch后只显示`已交给系统分享`。
6. Chooser/Activity return不触发success、completion、revoke或cleanup。
7. Expired/new-open failure、corrupt cleanup、save success/cancel/failure均消费CS-10 typed result。
8. 不拥有provider、manifest、serializer、resolver或performance threshold。

Evidence：

- Predicate eligible/ineligible、每个zero-duration rest分支。
- Whole→round→work/rest及restore。
- 两个composition/legacy blocks具有相同roundIndex时仍按block id唯一切换与restore；cross-block identity回退whole。
- 7.5全status与5.8 reason presentation消费矩阵，不改变priority或隐藏required reason。
- Legacy active/paused与legacy terminal。
- Share return/process kill/early relaunch/late relaunch/expiry UI。
- Save write/flush/close完成与cancel。
- API36 AVD、TalkBack、Big Type、720×1280及完整state matrix。

### CS-12 — Final integrated acceptance

新增/替换AC：

1. 在最终整合candidate SHA上确认clean worktree、empty index及全部predecessor ancestry。
2. Fresh加载`.local/env.ps1`，验证Temurin JDK identity。
3. 在最终SHA重新build两个debug APK并记录Gradle exit code。
4. 完成两个package的APK SHA、`pm path`、metadata、pulled base APK hash一致性。
5. 不修改固定AVD，验证config和runtime identity。
6. 在最终SHA重跑三个固定performance instrumentation class；不得复用旧Story evidence。
7. 只做cross-layer复核：证明DB、S4–S7、export读取同一terminal graph、CS-03 canonical plan snapshot结果、CS-09 legacy strict reader结果、CS-05 status/reason结果、binding/axes/anchor/focus tuple或同一typed failure；不产生、修复或重实现这些production合同。
8. Process-kill/relaunch覆盖全部manifest状态、expiry equality、boot change及corrupt cleanup。
9. Legacy active/paused migration保持无mutation；legacy/canonical no-HR分别验证。
10. CS-12仍为evidence/governance only，production/test/debug executable delta必须为0。

Evidence：

- 完整identity-bound manifest。
- 三项performance每次raw result、单调time、50ms PSS samples与absolute peak。
- Finalizer 5、projector 5、export 3 measured runs全部通过。
- AVD未修改证明。
- Export branch/golden、provider/open/lease、UI handoff及legacy matrix交叉核对。
- 4.2.3 terminal graph、CS-03 canonical plan snapshot root/block/version/key/type/mode/byte identity、CS-09 legacy no-fallback/default/element-drop/no-rewrite、CS-05 status priority与quality reason semantic negative evidence、6.6 multi-block focus的identity-bound正负证据交叉核对。
- 任一production finding返回对应CS-03..11 Repair/Correct Course；CS-12不修代码。

## 12. Impact map、ownership与DAG

| Finding | Contract surface | Story ripple |
|---|---|---|
| MF-1 | DDL、session/recording lifecycle、legacy matrix、versions | CS-03/04/05/09/10/11/12 |
| MF-2 | 八JSON、export closed schema、branch/type/order | CS-03/05/09/10/11/12 |
| MF-3 | 四family、predicate、focus identity | CS-03/04/05/06/07/08/09/11/12 |
| MF-4 | manifest/provider/cleanup/handoff | CS-10/11/12 |
| MF-5 | source/build/runtime/performance evidence | CS-05/09/10/12 |
| SF-1 | readiness token | V9 ordinary readiness及后续机械消费 |
| A3-MF-1 | canonical header/recording/partition/final input cut | CS-03/04/05/09/10/12 |
| A3-MF-2 | plan snapshot storage v1与legacy unversioned export | CS-03/09/10/12 |
| A3-MF-3 | status priority与quality reason presence | CS-03/05/09/10/11/12 |
| A3-MF-4 | block-local focus identity | CS-06/09/11/12 |
| A3-MF-5 | explicit predicate material dependency | CS-06/09及DAG |
| CA1-MF-1 | canonical/legacy/status-reason owner与AC/evidence归属 | CS-03/05/09/12；CS-10/11消费不变 |

Story ownership保持：

- CS-03：schema/migration、canonical plan snapshot v1 writer/strict validator及structural schema validators
- CS-04：Recorder/write guards/reconciliation
- CS-05：analysis/snapshot/finalizer、`StatusProjectionV1`及quality reason semantic validator
- CS-06/07/08：mode facts和family mapper
- CS-09：version-aware read、`LegacyUnversionedPlanSnapshotReader`、resolver/projector/focus
- CS-10：export/provider/lease
- CS-11：S4–S7/toggle/handoff UI
- CS-12：cross-layer final identity-bound evidence/docs closeout，不产生production合同

Material edges除把已存在于consumer AC的隐藏依赖显式化外保持；新增唯一edge为`CS-06 -> CS-09`：

```text
CS-01 -> CS-02

CS-01, CS-02, CS-05 -> CS-06
CS-01, CS-02, CS-05 -> CS-07
CS-01, CS-02, CS-05 -> CS-08

CS-03 -> CS-04
CS-04 -> CS-05
CS-05 -> CS-09
CS-06 -> CS-09

CS-03, CS-05, CS-06, CS-07, CS-08, CS-09 -> CS-10

CS-06, CS-07, CS-08, CS-09, CS-10 -> CS-11

CS-02, CS-10, CS-11 -> CS-12
```

机械结果更新为：

```text
nodes=12
materialEdges=28
roots=CS-01/CS-03
sink=CS-12
acyclic=true
orphans=0
longestMaterialPath=8
```

Longest path按本artifact既有node-count口径的一个8-node witness为`CS-03 -> CS-04 -> CS-05 -> CS-06 -> CS-09 -> CS-10 -> CS-11 -> CS-12`。节点数、roots、sink、acyclic与orphans不变；只更正V9 hidden forward dependency，不移动predicate owner。

## 13. Preserved state与non-goals

保持不变：

- Product outcome、CURRENT scope、三模式与local-first。
- S1–S7、chart、20s visual gap、first-highest、accessibility及既有microcopy。
- 2.5s sample validity、5000/7000/8000bp、weighted average、six-zone math、三个typed axes。
- E18 residual/future map。
- 五表与原子`Migration(4,5)`。
- `U-A`、`R-A`、`CC-D03-B`、`P-BALANCED-V2`。
- 12-Story ownership；DAG只按A3-MF-5显式增加`CS-06 -> CS-09`，现为28 edges、longest path 8。
- `MANUAL_RELAY`。
- CS-10/11既有consumer合同；CS-12只做cross-layer复核。

不新增：

- Exact alarm权限或receiver。
- 第二repository/exporter/resolver/UI owner。
- 云、账号、AI、医疗、multi-source、bulk export。
- CSV/ZIP。
- Phase-zone新schema。
- 用户可见训练时长硬上限。
- AVD/JDK/SDK安装、升级或配置修改。
- 本Planner中的Gradle build、AVD/device/human evidence。
- Planning Review、Consistency Audit或自动角色派发。

Bounded adjacent-omission scan结果：

- 同producer/owner：CS-03 canonical plan snapshot/structural validators、CS-04 guarded writes、CS-05 finalizer/status/reason semantic producer-validator、CS-06 predicate/focus owner、CS-09 legacy strict reader/direct reader/projector、CS-10 export、CS-11 UI与CS-12 cross-layer evidence均已传播；没有第二owner。
- 同state family/boundary/error：canonical running/terminal、recording/acquisition/snapshot input cut、plan snapshot canonical v1/legacy unversioned/unknown/corrupt、status/reason、legacy/composition focus均已闭合。
- 直接consumer：history/resolver/projector、S4–S7、export JSON、final evidence已覆盖；analytics只消费同一analysis snapshot，不产生额外schema。
- 相邻AC/evidence：CS-03/04/05/06/09/10/11/12已更新；CS-01/02/07/08的既有合同不受五项根因影响。
- Unaffected保持关闭：Product、UX、analysis math、12 Stories、E18 residual、`U-A`、`R-A`、`CC-D03-B`、`P-BALANCED-V2`、MF-4 lease/provider lifecycle、MF-5 performance profile以及其他non-goals均未重开。

## 14. V11 候选阶段的 Readiness、ledger 与下一门禁（non-operative / historical）

> 本节逐字保留 V11 候选阶段在 Attempt 5 Review 与 Attempt 2 Audit 前的状态记录。其 `PENDING_CLOSURE_BOUNDED_FRESH_RE_PLANNING_REVIEW`、`phaseBlockers`、`currentNode` 与 handoff 已被本文件开头的 PASS identities 和 tracked planning sync 当前状态 supersede，不能作为当前门禁或实现许可。

本V11的ordinary readiness为：

```text
READY_FOR_CLOSURE_BOUNDED_FRESH_RE_PLANNING_REVIEW / NOT_IMPLEMENTATION_READY
```

Planning Review PASS本身仍不自动产生implementation readiness。

Ledger：

- `acceptedFacts`：本Repair 6项exact input identity、accepted main `c2b569cd11953de95c8c6146c519fa6299c9557c`、V10 exact candidate、Attempt 4 PASS context及Audit Attempt 1唯一approved finding `CA1-MF-1`。
- `acceptedDecisions`：既有Product/UX/Architecture、`U-A`、`R-A`、`CC-D03-B`、`P-BALANCED-V2`；完整R2与`R2 Amendment 1`已经用户整体批准。
- `assumptions`：None。
- `openQuestions`：None。
- `phaseBlockers`：针对`CA1-MF-1` Repair的closure-bounded fresh re-Planning Review尚未PASS；在其PASS前不得启动下一次Consistency Audit或implementation。
- `deferred`：closure-bounded fresh re-Planning Review PASS后，由主管理按accepted gate决定单独scoped Consistency Audit；全部适用门禁PASS后才可进行tracked docs/Story implementation。
- `rejectedAlternatives`：重做全量规划/Review/Audit、从Audit Repair直接启动下一次Audit、提前把CS-05/09责任塞入CS-03、第二validator/reader/owner、改变CS-10/11 consumer、改变DAG、前序阶段重启、exact alarm、修改AVD、phase-zone扩张。
- `currentNode=post_audit_repair_v11 / PENDING_CLOSURE_BOUNDED_FRESH_RE_PLANNING_REVIEW`
- `firstUnfinishedAction=return to primary management for a closure-bounded fresh re-Planning Review of the CA1-MF-1 Repair`

获批与handoff：

- 用户整体批准原文：`明确批准完整proposal（R2 + R2 Amendment 1）`。
- 获批基线为`R2 SHA-256 FCA443E25E65E3CD833E50DA418C01639F15079B6C2A77D4E2026769A9F85028 + R2 Amendment 1`。
- `R2 Amendment 1`只把`WeightValue.value`闭合为finite non-negative JSON number并增加CS-10 zero/negative/non-finite evidence；`RepTarget`及R2其余内容不变。
- V11 exact path：`C:\Users\25073\Desktop\jianshen\.local\planning\INLINE-E17-REMAINDER-EPIC-STORY-PLAN-V11.md`。
- 针对`CA1-MF-1` Repair的closure-bounded fresh re-Planning Review尚未执行；不得把本V11、既有Attempt 4 PASS、文档hash或机械检查当作V11 Review PASS。
- 下一次Consistency Audit与implementation仍未解锁；不得从本Audit Repair直接启动Audit。
- `MANUAL_RELAY`保持；只交回主管理准备closure-bounded fresh re-Planning Review，不自动派发任何角色。
- 本Repair未执行Android build、Gradle、AVD、device/human evidence、Planning Review、Consistency Audit、Writer、implementation、stage、commit、merge、push、pull、rebase、reset、clean、stash、move或delete。
