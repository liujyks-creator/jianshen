import { useEffect, useMemo, useState } from 'react'
import {
  Activity,
  ArrowRight,
  BarChart3,
  Bell,
  BookOpen,
  ChevronRight,
  CirclePlay,
  Dumbbell,
  HeartPulse,
  House,
  LibraryBig,
  Pause,
  Play,
  Plus,
  RefreshCw,
  SkipForward,
  Sparkles,
  TimerReset,
  Volume2,
  Watch,
} from 'lucide-react'
import './App.css'
import {
  exercises,
  firstStrengthBlock,
  followAlongPlan,
  heartRateStates,
  recoveryAreas,
  strengthPlan,
  strengthRecovery,
  strengthSession,
  timedCircuit,
  timedPlan,
  timedSession,
} from './data/fixtures'
import type {
  Exercise,
  HeartRateState,
  RepTarget,
  StrengthSetPlan,
  WorkoutMode,
} from './data/contracts'

type View =
  | 'home'
  | 'timed-editor'
  | 'timed-session'
  | 'strength-editor'
  | 'strength-session'
  | 'follow'
  | 'library'
  | 'summary'

type StrengthPhase = 'prepare' | 'active' | 'confirm' | 'rest'

const modeCopy: Record<WorkoutMode, string> = {
  timed: '计时训练',
  strength: '力量训练',
  follow_along: '跟练雏形',
}

function formatClock(seconds: number) {
  const safeSeconds = Math.max(0, seconds)
  const minutes = Math.floor(safeSeconds / 60)
  const remaining = safeSeconds % 60
  return `${minutes}:${remaining.toString().padStart(2, '0')}`
}

function findExercise(exerciseId?: string) {
  return exercises.find((exercise) => exercise.id === exerciseId)
}

function formatRepTarget(target?: RepTarget) {
  if (!target) {
    return '按动作目标'
  }

  return target.kind === 'fixed' ? `${target.reps} 次` : `${target.minReps}-${target.maxReps} 次`
}

function formatWeight(value?: { value: number; unit: string }) {
  return value ? `${value.value} ${value.unit}` : '自重'
}

function getPlanStrengthSet(set: StrengthSetPlan) {
  return {
    weight: set.targetWeight ?? firstStrengthBlock?.target?.weight,
    repTarget: set.repTarget ?? firstStrengthBlock?.target?.repTarget,
    restAfterSec: set.restAfterSec ?? firstStrengthBlock?.target?.restAfterSetSec ?? 75,
  }
}

function HeartRateBadge({
  state,
  onNext,
}: {
  state: HeartRateState
  onNext: () => void
}) {
  const available = state.availability === 'available'
  const warning = state.warningLevel === 'attention' || state.warningLevel === 'high'

  return (
    <button className={`heart-rate ${available ? 'live' : ''} ${warning ? 'attention' : ''}`} onClick={onNext}>
      <HeartPulse aria-hidden="true" />
      <span>
        <strong>{state.bpm ? `${state.bpm} bpm` : '-- bpm'}</strong>
        <small>{state.message ?? state.availability}</small>
      </span>
    </button>
  )
}

function App() {
  const [view, setView] = useState<View>('home')
  const [selectedExerciseId, setSelectedExerciseId] = useState(exercises[0].id)
  const [heartIndex, setHeartIndex] = useState(0)
  const [timedItemIndex, setTimedItemIndex] = useState(0)
  const [timedRound, setTimedRound] = useState(1)
  const [timedPhase, setTimedPhase] = useState<'work' | 'rest'>('work')
  const [timedPaused, setTimedPaused] = useState(false)
  const [timedSeconds, setTimedSeconds] = useState(timedCircuit?.items[0]?.workDurationSec ?? 12)
  const [strengthSetIndex, setStrengthSetIndex] = useState(1)
  const [strengthPhase, setStrengthPhase] = useState<StrengthPhase>('prepare')
  const [strengthSeconds, setStrengthSeconds] = useState(0)
  const [actualWeight, setActualWeight] = useState(firstStrengthBlock?.target?.weight?.value ?? 60)
  const [actualReps, setActualReps] = useState(10)
  const [selectedEffort, setSelectedEffort] = useState('刚好')

  const heartRate = heartRateStates[heartIndex]
  const timedItems = useMemo(() => timedCircuit?.items ?? [], [])
  const timedItem = timedItems[timedItemIndex]
  const timedExercise = findExercise(timedItem?.exerciseId)
  const nextTimedItem = timedItems[(timedItemIndex + 1) % Math.max(timedItems.length, 1)]
  const nextTimedExercise = findExercise(nextTimedItem?.exerciseId)
  const strengthExercise = findExercise(firstStrengthBlock?.exerciseId)
  const strengthSets = firstStrengthBlock?.sets ?? []
  const currentStrengthSet = strengthSets[strengthSetIndex]
  const currentStrengthTarget = currentStrengthSet ? getPlanStrengthSet(currentStrengthSet) : undefined
  const selectedExercise = findExercise(selectedExerciseId) ?? exercises[0]

  const strengthVolume = useMemo(
    () =>
      strengthSession.strengthSetRecords?.reduce(
        (total, record) => total + (record.actualWeight?.value ?? 0) * (record.actualReps ?? 0),
        0,
      ) ?? 0,
    [],
  )

  useEffect(() => {
    if (view !== 'timed-session' || timedPaused || !timedItem) {
      return
    }

    const timer = window.setInterval(() => {
      setTimedSeconds((seconds) => {
        if (seconds > 1) {
          return seconds - 1
        }

        if (timedPhase === 'work') {
          setTimedPhase('rest')
          return timedItem.restAfterSec ?? 8
        }

        setTimedPhase('work')
        const nextIndex = timedItemIndex + 1
        if (nextIndex >= timedItems.length) {
          setTimedItemIndex(0)
          setTimedRound((round) => (round >= (timedCircuit?.rounds ?? 1) ? 1 : round + 1))
          return timedItems[0]?.workDurationSec ?? 12
        }

        setTimedItemIndex(nextIndex)
        return timedItems[nextIndex]?.workDurationSec ?? 12
      })
    }, 1000)

    return () => window.clearInterval(timer)
  }, [timedItem, timedItemIndex, timedItems, timedPaused, timedPhase, view])

  useEffect(() => {
    if (view !== 'strength-session' || strengthPhase !== 'active') {
      return
    }

    const timer = window.setInterval(() => {
      setStrengthSeconds((seconds) => seconds + 1)
    }, 1000)

    return () => window.clearInterval(timer)
  }, [strengthPhase, view])

  useEffect(() => {
    if (view !== 'strength-session' || strengthPhase !== 'rest') {
      return
    }

    const timer = window.setInterval(() => {
      setStrengthSeconds((seconds) => {
        if (seconds > 1) {
          return seconds - 1
        }

        setStrengthPhase('prepare')
        setStrengthSetIndex((index) => (index + 1 >= strengthSets.length ? 1 : index + 1))
        return 0
      })
    }, 1000)

    return () => window.clearInterval(timer)
  }, [strengthPhase, strengthSets.length, view])

  function cycleHeartState() {
    setHeartIndex((index) => (index + 1) % heartRateStates.length)
  }

  function openTimedSession() {
    setTimedItemIndex(0)
    setTimedRound(1)
    setTimedPhase('work')
    setTimedPaused(false)
    setTimedSeconds(timedItems[0]?.workDurationSec ?? 12)
    setView('timed-session')
  }

  function startStrengthSet() {
    setStrengthSeconds(0)
    setStrengthPhase('active')
  }

  function completeStrengthSet() {
    setActualWeight(currentStrengthTarget?.weight?.value ?? 0)
    setActualReps(
      currentStrengthTarget?.repTarget?.kind === 'fixed'
        ? currentStrengthTarget.repTarget.reps
        : currentStrengthTarget?.repTarget?.maxReps ?? 10,
    )
    setStrengthPhase('confirm')
  }

  function saveStrengthSet() {
    setStrengthSeconds(currentStrengthTarget?.restAfterSec ?? 75)
    setStrengthPhase('rest')
  }

  function extendStrengthRest(seconds: number) {
    setStrengthSeconds((current) => current + seconds)
  }

  return (
    <main className="prototype-shell">
      <aside className="side-rail">
        <div className="brand">
          <span>TF</span>
          <div>
            <strong>TrainFlow</strong>
            <small>交互原型</small>
          </div>
        </div>

        <nav aria-label="Prototype pages">
          <RailButton icon={<House />} active={view === 'home'} label="训练首页" onClick={() => setView('home')} />
          <RailButton icon={<TimerReset />} active={view === 'timed-editor'} label="计时计划" onClick={() => setView('timed-editor')} />
          <RailButton icon={<Dumbbell />} active={view === 'strength-editor'} label="力量计划" onClick={() => setView('strength-editor')} />
          <RailButton icon={<LibraryBig />} active={view === 'library'} label="动作库" onClick={() => setView('library')} />
          <RailButton icon={<BarChart3 />} active={view === 'summary'} label="训练总结" onClick={() => setView('summary')} />
        </nav>

        <section className="rail-note">
          <Sparkles aria-hidden="true" />
          <p>当前原型先验证页面流、提醒状态、单组记录和心率展示位。</p>
        </section>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">首版验证</p>
            <h1>{viewTitle(view)}</h1>
          </div>
          <HeartRateBadge state={heartRate} onNext={cycleHeartState} />
        </header>

        {view === 'home' && (
          <HomeView
            onTimed={() => setView('timed-editor')}
            onStrength={() => setView('strength-editor')}
            onFollow={() => setView('follow')}
          />
        )}

        {view === 'timed-editor' && <TimedEditor onStart={openTimedSession} />}

        {view === 'timed-session' && (
          <TimedSession
            exercise={timedExercise}
            nextExercise={nextTimedExercise}
            phase={timedPhase}
            paused={timedPaused}
            round={timedRound}
            seconds={timedSeconds}
            heartRate={heartRate}
            onPause={() => setTimedPaused((paused) => !paused)}
            onSkip={() => setTimedSeconds(1)}
            onOpenLibrary={() => setView('library')}
          />
        )}

        {view === 'strength-editor' && <StrengthEditor onStart={() => setView('strength-session')} />}

        {view === 'strength-session' && (
          <StrengthSession
            exercise={strengthExercise}
            planSet={currentStrengthSet}
            target={currentStrengthTarget}
            phase={strengthPhase}
            seconds={strengthSeconds}
            actualWeight={actualWeight}
            actualReps={actualReps}
            effort={selectedEffort}
            heartRate={heartRate}
            onStart={startStrengthSet}
            onComplete={completeStrengthSet}
            onSave={saveStrengthSet}
            onWeight={setActualWeight}
            onReps={setActualReps}
            onEffort={setSelectedEffort}
            onExtendRest={extendStrengthRest}
          />
        )}

        {view === 'follow' && <FollowAlongView onStart={openTimedSession} />}

        {view === 'library' && (
          <LibraryView
            selectedExercise={selectedExercise}
            onSelect={setSelectedExerciseId}
          />
        )}

        {view === 'summary' && <SummaryView strengthVolume={strengthVolume} />}
      </section>
    </main>
  )
}

function viewTitle(view: View) {
  switch (view) {
    case 'timed-editor':
      return '计时训练快速创建'
    case 'timed-session':
      return '计时训练执行'
    case 'strength-editor':
      return '力量训练计划'
    case 'strength-session':
      return '力量训练执行'
    case 'follow':
      return '跟练训练雏形'
    case 'library':
      return '动作库'
    case 'summary':
      return '训练总结'
    default:
      return '训练首页'
  }
}

function RailButton({
  icon,
  active,
  label,
  onClick,
}: {
  icon: React.ReactNode
  active: boolean
  label: string
  onClick: () => void
}) {
  return (
    <button className={`rail-button ${active ? 'active' : ''}`} onClick={onClick}>
      {icon}
      <span>{label}</span>
    </button>
  )
}

function HomeView({
  onTimed,
  onStrength,
  onFollow,
}: {
  onTimed: () => void
  onStrength: () => void
  onFollow: () => void
}) {
  return (
    <div className="dashboard-grid">
      <section className="panel start-panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">默认入口</p>
            <h2>先把训练跑起来</h2>
          </div>
          <span className="status-pill">计时训练推荐</span>
        </div>
        <p>先用动作时长、休息提醒和轮次推进开始训练，力量计划仍在同层可见。</p>
        <div className="mode-actions">
          <button className="primary-action" onClick={onTimed}>
            <CirclePlay />
            开始计时训练
          </button>
          <button className="secondary-action" onClick={onStrength}>
            <Dumbbell />
            力量训练
          </button>
          <button className="secondary-action" onClick={onFollow}>
            <BookOpen />
            跟练雏形
          </button>
        </div>
      </section>

      <section className="panel compact-panel">
        <div className="section-heading">
          <h2>今日计划</h2>
          <Bell />
        </div>
        <PlanStrip title={timedPlan.title} meta="18:30 提醒 · 动作与休息都在最后 5 秒提示" mode="timed" />
        <PlanStrip title={strengthPlan.title} meta="19:15 提醒 · 默认手动开始本组" mode="strength" />
      </section>

      <section className="panel wide-panel">
        <div className="section-heading">
          <h2>继续训练</h2>
          <span>训练闭环先跑通</span>
        </div>
        <div className="continue-grid">
          <QuickMetric title="动作库接口" value={`${exercises.length} 个 fixture`} detail="覆盖热身、拉伸、力量、跟练" />
          <QuickMetric title="最近力量容量" value="1,320 kg" detail="保留组耗时与实际休息" />
          <QuickMetric title="心率展示位" value="5 种状态" detail="设备未接也能完整训练" />
        </div>
      </section>
    </div>
  )
}

function TimedEditor({ onStart }: { onStart: () => void }) {
  return (
    <div className="editor-layout">
      <section className="panel editor-main">
        <div className="section-heading">
          <div>
            <p className="eyebrow">快速创建</p>
            <h2>{timedPlan.title}</h2>
          </div>
          <button className="ghost-action">
            <Plus />
            加动作
          </button>
        </div>

        <BlockRow label="热身" value="1:30" meta="进入主循环前准备" tone="warm" />
        {timedCircuit?.items.map((item, index) => {
          const exercise = findExercise(item.exerciseId)
          return (
            <BlockRow
              key={item.id}
              label={`${index + 1}. ${exercise?.name ?? item.exerciseId}`}
              value={`${item.workDurationSec}s / 休息 ${item.restAfterSec ?? 0}s`}
              meta={exercise?.instructions.shortCue ?? '动作短提示'}
              tone="timed"
            />
          )
        })}
        <BlockRow label="循环" value={`${timedCircuit?.rounds ?? 0} 轮`} meta="轮间休息 18 秒" tone="neutral" />
      </section>

      <aside className="panel editor-side">
        <h2>提醒设置</h2>
        <SettingRow label="动作快结束" value="最后 5 秒" />
        <SettingRow label="休息快结束" value="最后 5 秒" />
        <SettingRow label="声音 / 震动" value="开启" />
        <SettingRow label="倒计时强化动画" value="开启" />
        <SettingRow label="语音读秒" value="接口预留" />
        <button className="primary-action full" onClick={onStart}>
          <Play />
          开始这个计划
        </button>
      </aside>
    </div>
  )
}

function TimedSession({
  exercise,
  nextExercise,
  phase,
  paused,
  round,
  seconds,
  heartRate,
  onPause,
  onSkip,
  onOpenLibrary,
}: {
  exercise?: Exercise
  nextExercise?: Exercise
  phase: 'work' | 'rest'
  paused: boolean
  round: number
  seconds: number
  heartRate: HeartRateState
  onPause: () => void
  onSkip: () => void
  onOpenLibrary: () => void
}) {
  const ending = seconds <= 5
  const resting = phase === 'rest'

  return (
    <section className={`session-stage ${resting ? 'resting' : ''}`}>
      <div className="session-copy">
        <span className="status-pill">{resting ? '休息' : '动作'} · 第 {round} 轮</span>
        <h2>{resting ? '准备下一步' : exercise?.name}</h2>
        <p>{resting ? `下一动作: ${nextExercise?.name ?? '循环开始'}` : exercise?.instructions.shortCue}</p>
      </div>

      <div className={`countdown ${ending ? 'ending' : ''}`}>
        <small>{ending ? '临近结束提醒' : '当前倒计时'}</small>
        <strong>{formatClock(seconds)}</strong>
      </div>

      <div className="session-status-row">
        <HeartChip state={heartRate} />
        <span>
          <Volume2 />
          动作与休息都提示
        </span>
        <button onClick={onOpenLibrary}>
          <BookOpen />
          动作要点
        </button>
      </div>

      <div className="session-controls">
        <button className="secondary-action" onClick={onPause}>
          {paused ? <Play /> : <Pause />}
          {paused ? '继续' : '暂停'}
        </button>
        <button className="primary-action" onClick={onSkip}>
          <SkipForward />
          {resting ? '跳过休息' : '跳到提醒'}
        </button>
      </div>
    </section>
  )
}

function StrengthEditor({ onStart }: { onStart: () => void }) {
  return (
    <div className="editor-layout">
      <section className="panel editor-main">
        <div className="section-heading">
          <div>
            <p className="eyebrow">并联能力</p>
            <h2>{strengthPlan.title}</h2>
          </div>
          <span className="status-pill">默认 8-12 次</span>
        </div>

        {strengthPlan.blocks
          .filter((block) => block.kind === 'strength_exercise')
          .map((block) => {
            const exercise = findExercise(block.exerciseId)
            return (
              <article className="strength-card" key={block.id}>
                <div>
                  <h3>{exercise?.name}</h3>
                  <p>{exercise?.instructions.shortCue}</p>
                </div>
                <div className="set-plan">
                  <span>{formatWeight(block.target?.weight)}</span>
                  <span>{formatRepTarget(block.target?.repTarget)}</span>
                  <span>{block.sets.length} 组</span>
                  <span>{block.setTimerMode === 'manual_start' ? '手动开始本组' : '自动计时'}</span>
                </div>
              </article>
            )
          })}
      </section>

      <aside className="panel editor-side">
        <h2>高级组设定</h2>
        <SettingRow label="动作内热身组" value="已启用" />
        <SettingRow label="逐组重量次数" value="可展开" />
        <SettingRow label="替代动作" value="卧推 2 个" />
        <SettingRow label="每组耗时" value="记录" />
        <button className="primary-action full" onClick={onStart}>
          <Dumbbell />
          开始力量训练
        </button>
      </aside>
    </div>
  )
}

function StrengthSession({
  exercise,
  planSet,
  target,
  phase,
  seconds,
  actualWeight,
  actualReps,
  effort,
  heartRate,
  onStart,
  onComplete,
  onSave,
  onWeight,
  onReps,
  onEffort,
  onExtendRest,
}: {
  exercise?: Exercise
  planSet?: StrengthSetPlan
  target?: ReturnType<typeof getPlanStrengthSet>
  phase: StrengthPhase
  seconds: number
  actualWeight: number
  actualReps: number
  effort: string
  heartRate: HeartRateState
  onStart: () => void
  onComplete: () => void
  onSave: () => void
  onWeight: (value: number) => void
  onReps: (value: number) => void
  onEffort: (value: string) => void
  onExtendRest: (seconds: number) => void
}) {
  if (!planSet || !target) {
    return <section className="panel empty-state">还没有可执行的力量组。</section>
  }

  return (
    <div className="strength-session">
      <section className="panel set-stage">
        <div className="section-heading">
          <div>
            <p className="eyebrow">{planSet.kind === 'warmup' ? '热身组' : '正式组'} · 第 {planSet.order} 组</p>
            <h2>{exercise?.name}</h2>
          </div>
          <HeartChip state={heartRate} />
        </div>

        <div className="target-grid">
          <QuickMetric title="计划重量" value={formatWeight(target.weight)} detail="完成后默认带入实际记录" />
          <QuickMetric title="计划次数" value={formatRepTarget(target.repTarget)} detail={planSet.side ? `侧别: ${planSet.side}` : '动作级默认可覆盖'} />
          <QuickMetric title={phase === 'rest' ? '休息剩余' : '本组耗时'} value={formatClock(seconds)} detail="每组时间留作趋势数据" />
        </div>

        {phase === 'prepare' && (
          <div className="set-actions">
            <button className="primary-action" onClick={onStart}>
              <Play />
              开始本组
            </button>
            <button className="secondary-action">
              <RefreshCw />
              换动作
            </button>
          </div>
        )}

        {phase === 'active' && (
          <div className="set-actions">
            <button className="primary-action" onClick={onComplete}>
              <Dumbbell />
              完成本组
            </button>
            <button className="secondary-action">
              <Pause />
              暂停
            </button>
          </div>
        )}

        {phase === 'rest' && (
          <div className={`rest-clock ${seconds <= 5 ? 'ending' : ''}`}>
            <strong>{formatClock(seconds)}</strong>
            <p>休息快结束时提醒准备下一组。</p>
            <div className="set-actions">
              <button className="secondary-action" onClick={() => onExtendRest(15)}>+15 秒</button>
              <button className="secondary-action" onClick={() => onExtendRest(30)}>+30 秒</button>
              <button className="primary-action" onClick={onStart}>提前开始</button>
            </div>
          </div>
        )}
      </section>

      {phase === 'confirm' && (
        <aside className="panel confirm-sheet">
          <p className="eyebrow">完成本组</p>
          <h2>计划值先带入，再改实际结果</h2>
          <label>
            实际重量
            <input type="number" value={actualWeight} onChange={(event) => onWeight(Number(event.target.value))} />
          </label>
          <label>
            实际次数
            <input type="number" value={actualReps} onChange={(event) => onReps(Number(event.target.value))} />
          </label>
          <div className="effort-picker" aria-label="Training effort">
            {['轻松', '刚好', '很吃力', '动作变形'].map((item) => (
              <button className={effort === item ? 'active' : ''} key={item} onClick={() => onEffort(item)}>
                {item}
              </button>
            ))}
          </div>
          <button className="primary-action full" onClick={onSave}>
            <TimerReset />
            保存并开始休息
          </button>
        </aside>
      )}
    </div>
  )
}

function FollowAlongView({ onStart }: { onStart: () => void }) {
  const current = findExercise('goblet-squat')
  const next = findExercise('plank')

  return (
    <div className="follow-layout">
      <section className="panel follow-media">
        <span className="status-pill">媒体位预留</span>
        <Activity aria-hidden="true" />
        <p>动作演示、教练视频和节拍扩展以后接入。</p>
      </section>
      <section className="panel follow-copy">
        <p className="eyebrow">{followAlongPlan.title}</p>
        <h2>{current?.name}</h2>
        <p>{current?.instructions.shortCue}</p>
        <BlockRow label="下一动作" value={next?.name ?? ''} meta={next?.instructions.shortCue ?? ''} tone="follow" />
        <div className="set-actions">
          <button className="primary-action" onClick={onStart}>
            <CirclePlay />
            用计时流程启动
          </button>
          <button className="secondary-action">
            <Watch />
            心率位已预留
          </button>
        </div>
      </section>
    </div>
  )
}

function LibraryView({
  selectedExercise,
  onSelect,
}: {
  selectedExercise: Exercise
  onSelect: (exerciseId: string) => void
}) {
  return (
    <div className="library-layout">
      <section className="panel library-list">
        <div className="section-heading">
          <h2>动作 fixture</h2>
          <span>{exercises.length} 个</span>
        </div>
        <div className="exercise-list">
          {exercises.map((exercise) => (
            <button
              className={selectedExercise.id === exercise.id ? 'selected' : ''}
              key={exercise.id}
              onClick={() => onSelect(exercise.id)}
            >
              <strong>{exercise.name}</strong>
              <span>
                {exercise.capabilities.supportsWeight ? '重量' : '计时'}
                {exercise.capabilities.supportsFollowAlong ? ' · 跟练' : ''}
              </span>
            </button>
          ))}
        </div>
      </section>

      <section className="panel exercise-detail">
        <p className="eyebrow">{selectedExercise.category}</p>
        <h2>{selectedExercise.name}</h2>
        <p className="lead">{selectedExercise.instructions.shortCue}</p>
        <div className="tag-row">
          {selectedExercise.equipment.map((equipment) => <span key={equipment}>{equipment}</span>)}
          {selectedExercise.roles.map((role) => <span key={role}>{role}</span>)}
        </div>
        <DetailList title="标准步骤" items={selectedExercise.instructions.steps} />
        <DetailList title="发力要点" items={selectedExercise.instructions.keyPoints} />
        <DetailList title="常见错误" items={selectedExercise.instructions.commonMistakes} />
      </section>
    </div>
  )
}

function SummaryView({ strengthVolume }: { strengthVolume: number }) {
  const recovery = recoveryAreas.filter((area) => strengthRecovery.areaIds.includes(area.id))

  return (
    <div className="summary-grid">
      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">力量总结</p>
            <h2>{strengthSession.planSnapshot.title}</h2>
          </div>
          <span className="status-pill">{strengthSession.userFeedback?.overallEffort}</span>
        </div>
        <div className="continue-grid">
          <QuickMetric title="训练容量" value={`${strengthVolume} kg`} detail="实际重量 x 次数" />
          <QuickMetric title="组耗时" value="36s / 42s" detail="同重量趋势先展示不下结论" />
          <QuickMetric title="实际休息" value="94s / 112s" detail="计划与实际差异保留" />
        </div>
      </section>

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">计时总结</p>
            <h2>{timedSession.planSnapshot.title}</h2>
          </div>
          <span>{timedSession.stepHistory.length} 个步骤记录</span>
        </div>
        <PlanStrip title="完成轮数" meta="动作与休息提醒都在会话里留痕" mode="timed" />
      </section>

      <section className="panel recovery-panel">
        <div className="section-heading">
          <h2>练后恢复</h2>
          <ChevronRight />
        </div>
        {recovery.map((area) => (
          <article key={area.id}>
            <strong>{area.name}</strong>
            <p>{area.summary}</p>
          </article>
        ))}
      </section>
    </div>
  )
}

function DetailList({ title, items }: { title: string; items: string[] }) {
  return (
    <section className="detail-list">
      <h3>{title}</h3>
      <ul>
        {items.map((item) => <li key={item}>{item}</li>)}
      </ul>
    </section>
  )
}

function BlockRow({
  label,
  value,
  meta,
  tone,
}: {
  label: string
  value: string
  meta: string
  tone: 'warm' | 'timed' | 'neutral' | 'follow'
}) {
  return (
    <article className={`block-row ${tone}`}>
      <div>
        <strong>{label}</strong>
        <p>{meta}</p>
      </div>
      <span>{value}</span>
    </article>
  )
}

function SettingRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="setting-row">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function PlanStrip({ title, meta, mode }: { title: string; meta: string; mode: WorkoutMode }) {
  return (
    <article className="plan-strip">
      <span>{modeCopy[mode]}</span>
      <div>
        <strong>{title}</strong>
        <p>{meta}</p>
      </div>
      <ArrowRight />
    </article>
  )
}

function QuickMetric({ title, value, detail }: { title: string; value: string; detail: string }) {
  return (
    <article className="metric">
      <span>{title}</span>
      <strong>{value}</strong>
      <p>{detail}</p>
    </article>
  )
}

function HeartChip({ state }: { state: HeartRateState }) {
  return (
    <span className={`heart-chip ${state.warningLevel === 'attention' ? 'attention' : ''}`}>
      <HeartPulse />
      {state.bpm ? `${state.bpm} bpm` : state.message ?? '-- bpm'}
    </span>
  )
}

export default App
