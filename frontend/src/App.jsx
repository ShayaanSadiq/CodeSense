import { useEffect, useRef, useState } from 'react'
import {
  answerWorkshop,
  arrangeWorkshop,
  confirmWorkshop,
  convertCode,
  fetchMe,
  fetchRoles,
  login,
  logout,
  predictPython,
  getWorkshop,
  rephraseQuestion,
  setToken,
  signup,
  startWorkshop,
  translateWorkshop,
} from './api.js'
import { FlowchartJoin } from './FlowchartJoin.jsx'
import { PythonLab } from './PythonLab.jsx'
import { PythonRunner } from './PythonRunner.jsx'
import { ParsonsPath } from './ParsonsPath.jsx'
import { TranslatorMark } from './TranslatorMark.jsx'
import './App.css'

const SESSION_KEY = 'codesense-workshop-id'
const METHOD_KEY = 'codesense-method'
const THEME_KEY = 'codesense-theme'

const SAMPLES = [
  {
    label: 'Largest in a list',
    text: 'Find the largest number in a list.',
  },
  {
    label: 'Count vowels',
    text: 'Count the vowels in a string.',
  },
  {
    label: 'Rectangle area',
    text: 'Compute the area of a rectangle given length and width.',
  },
]

const STAGE_LABEL = {
  1: 'Core logic',
  2: 'Choose an approach',
  3: 'Starting value',
  4: 'Decision',
  5: 'Iteration',
  6: 'Build the plan',
  7: 'Check your reasoning',
}

const METHODS = [
  { id: 'socratic', label: 'Socratic model' },
  { id: 'translator', label: 'Pseudocode translator' },
]

const LANGS = [
  { id: 'python', label: 'Python' },
  { id: 'java', label: 'Java' },
  { id: 'c', label: 'C' },
]

function constraintBullets(brief) {
  if (Array.isArray(brief?.constraintItems) && brief.constraintItems.length > 0) {
    return brief.constraintItems
  }
  if (Array.isArray(brief?.constraints)) {
    return brief.constraints
  }
  if (typeof brief?.constraints === 'string' && brief.constraints.trim()) {
    return brief.constraints.split(/(?<=\.)\s+/).filter(Boolean)
  }
  return []
}

function testRows(brief) {
  if (Array.isArray(brief?.tests) && brief.tests.length > 0) {
    return brief.tests.map((row) => ({
      input: row.input || '',
      expected: row.expected || '',
      status: row.status || 'Pass',
    }))
  }
  const pass = (brief?.passedTests || []).map((row) => splitCase(row, 'Pass'))
  const fail = (brief?.failedTests || []).map((row) => splitCase(row, 'Fail'))
  return [...pass, ...fail]
}

function splitCase(row, status) {
  const parts = String(row).split(/\s*(?:→|->)\s*/)
  if (parts.length >= 2) {
    return { input: parts[0], expected: parts.slice(1).join(' → '), status }
  }
  return { input: String(row), expected: '', status }
}

function ChipLine({ text }) {
  const parts = String(text).split(/(\[[^\]]+\]|(?<![A-Za-z])-?\d+(?:\.\d+)?(?![A-Za-z]))/)
  return (
    <>
      {parts.map((part, index) =>
        /^(\[[^\]]+\]|-?\d+(?:\.\d+)?)$/.test(part) ? (
          <code key={`${part}-${index}`} className="lab-chip">
            {part}
          </code>
        ) : (
          <span key={`${part}-${index}`}>{part}</span>
        ),
      )}
    </>
  )
}

function cardUnusedMap(raw) {
  if (!raw || typeof raw !== 'object') {
    return {}
  }
  if (Array.isArray(raw)) {
    const out = {}
    for (const item of raw) {
      const card = String(item?.card || '').toUpperCase()
      const why = item?.why || item?.explanation
      if (card && why) {
        out[card] = why
      }
    }
    return out
  }
  return raw
}

function shuffleParsons(lines) {
  const next = lines.map((line) => ({ ...line, indent: 0 }))
  for (let i = next.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1))
    const left = next[i]
    const right = next[j]
    if (!left || !right) continue
    next[i] = right
    next[j] = left
  }
  if (
    next.length > 1 &&
    next.every((line, index) => line.id === lines[index]?.id)
  ) {
    const first = next[0]
    const second = next[1]
    if (first && second) {
      next[0] = second
      next[1] = first
    }
  }
  return next
}

export default function App() {
  const [page, setPage] = useState('learn')
  const [theme, setTheme] = useState(
    () => (localStorage.getItem(THEME_KEY) === 'dark' ? 'dark' : 'light'),
  )
  const [method, setMethod] = useState(
    () => localStorage.getItem(METHOD_KEY) || 'socratic',
  )
  const [draft, setDraft] = useState('')
  const [session, setSession] = useState(null)
  const [answer, setAnswer] = useState('')
  const [picked, setPicked] = useState('')
  const [arranged, setArranged] = useState([])
  const [pathOrder, setPathOrder] = useState([])
  const [error, setError] = useState(null)
  const [pending, setPending] = useState(false)
  const [lifting, setLifting] = useState(false)
  const [homeGone, setHomeGone] = useState(false)
  const [translateDraft, setTranslateDraft] = useState('')
  const [voicedQuestion, setVoicedQuestion] = useState('')
  const [neededCards, setNeededCards] = useState([])
  const [unusedCards, setUnusedCards] = useState({})
  const [voiceLines, setVoiceLines] = useState([])
  const [roles, setRoles] = useState([])
  const [account, setAccount] = useState(null)
  const [authName, setAuthName] = useState('')
  const [authUser, setAuthUser] = useState('')
  const [authPass, setAuthPass] = useState('')
  const [writingLang, setWritingLang] = useState('')
  const [modelOpen, setModelOpen] = useState(false)
  const modelPickRef = useRef(null)
  const alreadySolvedCurrent = Boolean(
    session?.currentQuestion &&
      (session.turns || []).some(
        (turn) =>
          turn.passed &&
          String(turn.question || '').trim() === String(session.currentQuestion || '').trim(),
      ),
  )
  const askingWhy = Boolean(session?.whyFollowUp) || alreadySolvedCurrent
  const liveQuestion = session?.currentQuestion
  const showMcq =
    session?.askKind === 'mcq' &&
    (session?.choices || []).length > 0 &&
    !askingWhy

  useEffect(() => {
    const saved = localStorage.getItem(SESSION_KEY)
    if (!saved) return
    getWorkshop(saved)
      .then(setSession)
      .catch(() => localStorage.removeItem(SESSION_KEY))
  }, [])

  useEffect(() => {
    const token = localStorage.getItem('codesense-token')
    if (!token) return
    fetchMe()
      .then((next) => {
        setAccount(next)
        setToken(next.token)
      })
      .catch(() => {
        setToken('')
        setAccount(null)
      })
  }, [])

  useEffect(() => {
    if (page === 'dashboard') {
      setPage('learn')
    }
    if (page === 'profile' && !account) {
      setPage('login')
    }
  }, [page, account])

  useEffect(() => {
    fetchRoles().then(setRoles).catch(() => setRoles([]))
  }, [])

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  useEffect(() => {
    if (!modelOpen) return undefined
    function onPointer(event) {
      if (!modelPickRef.current?.contains(event.target)) {
        setModelOpen(false)
      }
    }
    function onKey(event) {
      if (event.key === 'Escape') setModelOpen(false)
    }
    document.addEventListener('mousedown', onPointer)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onPointer)
      document.removeEventListener('keydown', onKey)
    }
  }, [modelOpen])

  useEffect(() => {
    if (session?.phase === 'confirm' && session.proposedLines?.length) {
      confirmWorkshop(session.id, session.proposedLines)
        .then(remember)
        .catch((err) =>
          setError(err instanceof Error ? err.message : 'Something went wrong.'),
        )
      return
    }
    if (session?.phase === 'arrange') {
      const incoming =
        session.puzzleLines && session.puzzleLines.length > 0
          ? session.puzzleLines
          : shuffleParsons(session.confirmedLines)
      setArranged((prev) => {
        const samePieces =
          prev.length === incoming.length &&
          [...prev.map((line) => line.id)].sort().join() ===
            [...incoming.map((line) => line.id)].sort().join()
        return samePieces ? prev : incoming
      })
      setPathOrder([])
    }
  }, [session])

  function chooseMethod(next) {
    setMethod(next)
    localStorage.setItem(METHOD_KEY, next)
    setError(null)
  }

  async function run(work) {
    setPending(true)
    setError(null)
    try {
      return await work()
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Something went wrong.'
      if (message === 'Session not found') {
        localStorage.removeItem(SESSION_KEY)
      }
      setError(message)
      return undefined
    } finally {
      setPending(false)
    }
  }

  async function begin(question) {
    setError(null)
    setPending(true)
    setLifting(true)
    await new Promise((resolve) =>
      requestAnimationFrame(() => requestAnimationFrame(resolve)),
    )
    try {
      if (method === 'translator') {
        const voiced = await rephraseQuestion(question).catch(() => null)
        await wait(560)
        setTranslateDraft(question)
        setVoicedQuestion(voiced?.text || question)
        setNeededCards(Array.isArray(voiced?.needed) ? voiced.needed : [])
        setUnusedCards(cardUnusedMap(voiced?.unused))
        setVoiceLines(Array.isArray(voiced?.lines) ? voiced.lines : [])
        setSession(null)
        setPage('learn')
        setHomeGone(true)
        return
      }
      const [next] = await Promise.all([
        startWorkshop(question, 'path'),
        wait(560),
      ])
      if (!next) {
        return
      }
      remember(next)
      setAnswer('')
      setTranslateDraft('')
      setPage('learn')
      setHomeGone(true)
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Something went wrong.'
      if (message === 'Session not found') {
        localStorage.removeItem(SESSION_KEY)
      }
      setError(message)
    } finally {
      setPending(false)
      setLifting(false)
    }
  }

  function wait(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms))
  }

  async function assemblePlan(marks) {
    const next = await run(() =>
      translateWorkshop(translateDraft || draft, marks, 'flow'),
    )
    if (!next) return
    remember(next)
    setTranslateDraft('')
    setHomeGone(true)
  }

  async function submitAnswer(dontKnow = false) {
    if (!session) return
    const next = await run(() =>
      answerWorkshop(session.id, dontKnow ? "I don't know" : answer, dontKnow),
    )
    if (!next) return
    remember(next)
    if (next.verdict !== 'wrong' && next.verdict !== 'hint') {
      setAnswer('')
      setPicked('')
    }
  }

  async function writeCode(input, language = 'python') {
    if (!session) return
    setWritingLang(language)
    try {
      const next = await run(() => convertCode(session.id, input, language))
      if (!next) return
      remember(next)
    } finally {
      setWritingLang('')
    }
  }

  async function submitAuth(event) {
    event.preventDefault()
    const next = await run(() =>
      page === 'signup'
        ? signup(authName, authUser, authPass)
        : login(authUser, authPass),
    )
    if (!next) return
    setToken(next.token)
    setAccount(next)
    setAuthPass('')
    setPage('learn')
  }

  async function signOut() {
    await run(() => logout())
    setToken('')
    setAccount(null)
    setAuthPass('')
    setPage('login')
  }

  async function submitPredict(answer, dontKnow = false) {
    if (!session) return
    const next = await run(() => predictPython(session.id, answer, dontKnow))
    if (!next) return
    remember(next)
  }

  function updatePathOrder(next) {
    setPathOrder(next)
    setSession((current) =>
      current?.verdict === 'wrong'
        ? { ...current, verdict: null, feedback: null, arrangeNotes: [] }
        : current,
    )
  }

  async function finishArrange() {
    if (!session) return
    const next = await run(() => arrangeWorkshop(session.id, pathOrder))
    if (!next) return
    remember(next)
  }

  function remember(next) {
    localStorage.setItem(SESSION_KEY, next.id)
    setSession(next)
  }

  function reset() {
    localStorage.removeItem(SESSION_KEY)
    setSession(null)
    setDraft('')
    setAnswer('')
    setPicked('')
    setArranged([])
    setPathOrder([])
    setError(null)
    setLifting(false)
    setHomeGone(false)
    setTranslateDraft('')
    setVoicedQuestion('')
    setNeededCards([])
    setUnusedCards({})
    setVoiceLines([])
    setPage('learn')
  }

  function openPythonRunner() {
    setPage('python-run')
    setError(null)
  }

  function go(nextPage) {
    setPage(nextPage === 'dashboard' ? 'learn' : nextPage)
    setError(null)
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <button
          type="button"
          className="brand"
          onClick={reset}
          aria-label="CodeSense home"
        >
          <img src="/logo.png?v=2" alt="CodeSense" />
        </button>
        <nav className="nav">
          <button
            type="button"
            className={
              page === 'learn' || page === 'python-run' ? 'nav-item active' : 'nav-item'
            }
            onClick={() => go('learn')}
          >
            Learning Space
          </button>
        </nav>
        <button
          type="button"
          className="theme-toggle"
          onClick={() => setTheme((current) => (current === 'dark' ? 'light' : 'dark'))}
          aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
        >
          {theme === 'dark' ? 'Light theme' : 'Dark theme'}
        </button>
        <button
          type="button"
          className={
            page === 'profile' || page === 'login' || page === 'signup'
              ? 'nav-item profile active'
              : 'nav-item profile'
          }
          onClick={() => go(account ? 'profile' : 'login')}
        >
          {account?.name || 'Sign in'}
        </button>
      </aside>

      <main className="main">
        {error && <p className="banner">{error}</p>}

        {page === 'profile' && account && (
          <ProfileView account={account} pending={pending} onLogout={signOut} />
        )}

        {page === 'login' && !account && (
          <SignInPage
            user={authUser}
            pass={authPass}
            pending={pending}
            onUser={setAuthUser}
            onPass={setAuthPass}
            onSubmit={submitAuth}
            onCreateAccount={() => go('signup')}
          />
        )}

        {page === 'signup' && !account && (
          <SignUpPage
            name={authName}
            user={authUser}
            pass={authPass}
            pending={pending}
            onName={setAuthName}
            onUser={setAuthUser}
            onPass={setAuthPass}
            onSubmit={submitAuth}
            onSignIn={() => go('login')}
          />
        )}

        {page === 'learn' && !session && !homeGone && (
          <section className={lifting ? 'learn-home lifting' : 'learn-home'}>
            <p className="learn-kicker">Learning space</p>
            <p className="tagline">Don&apos;t memorize, understand it</p>
            <div className="samples" role="group" aria-label="Sample questions">
              {SAMPLES.map((sample) => (
                <button
                  key={sample.label}
                  type="button"
                  className={draft === sample.text ? 'sample selected' : 'sample'}
                  onClick={() => setDraft(sample.text)}
                >
                  {sample.label}
                </button>
              ))}
            </div>
            <form
              className="query-bar"
              onSubmit={(event) => {
                event.preventDefault()
                if (!pending && draft.trim().length >= 8) begin(draft)
              }}
            >
              <div
                className={modelOpen ? 'model-pick open' : 'model-pick'}
                ref={modelPickRef}
              >
                <button
                  type="button"
                  className="model-pick-btn"
                  aria-label="Select model"
                  aria-haspopup="listbox"
                  aria-expanded={modelOpen}
                  onClick={() => setModelOpen((open) => !open)}
                >
                  <span>
                    {METHODS.find((item) => item.id === method)?.label ||
                      'Socratic model'}
                  </span>
                  <span className="model-pick-caret" aria-hidden="true">
                    ▾
                  </span>
                </button>
                {modelOpen ? (
                  <ul className="model-menu" role="listbox" aria-label="Select model">
                    {METHODS.map((item) => (
                      <li key={item.id} role="presentation">
                        <button
                          type="button"
                          className="model-option"
                          role="option"
                          aria-selected={item.id === method}
                          onClick={() => {
                            chooseMethod(item.id)
                            setModelOpen(false)
                          }}
                        >
                          {item.label}
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : null}
              </div>
              <input
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder="Hi, what's your query today?"
                aria-label="Lab question"
                readOnly={lifting}
              />
              {lifting && <p className="flying-title">Q) {draft}</p>}
              <button
                type="submit"
                className="go"
                disabled={pending || draft.trim().length < 8}
                aria-label={pending ? 'Checking' : 'Start'}
              >
                {pending ? '…' : '➤'}
              </button>
            </form>
          </section>
        )}

        {page === 'learn' && session?.phase === 'refused' && (
          <section className="card refuse">
            <p className="refuse-line">{session.refusal}</p>
            <p className="muted">You pasted: “{session.labQuestion}”</p>
            <button type="button" className="primary" onClick={reset}>
              Paste a different part
            </button>
          </section>
        )}

        {page === 'learn' && !session && translateDraft && (
          <section className="translator-page in">
            <header className="socratic-head">
              <div>
                {voicedQuestion && voicedQuestion !== translateDraft && (
                  <p className="same-idea">
                    Same idea as: “{translateDraft}”
                  </p>
                )}
              </div>
              <button type="button" className="ghost" onClick={reset}>
                New question
              </button>
            </header>
            <TranslatorMark
              question={translateDraft}
              voiced={voicedQuestion}
              needed={neededCards}
              unused={unusedCards}
              lines={voiceLines}
              pending={pending}
              onAssemble={assemblePlan}
            />
          </section>
        )}

        {page === 'learn' &&
          session &&
          session.phase === 'arrange' && (
            <section className="translator-page in">
              <header className="socratic-head">
                <div>
                  <p className="socratic-title">
                    Q) {voicedQuestion || session.labQuestion}
                  </p>
                  {voicedQuestion && voicedQuestion !== session.labQuestion && (
                    <p className="same-idea">
                      Same idea as: “{session.labQuestion}”
                    </p>
                  )}
                </div>
                <button type="button" className="ghost" onClick={reset}>
                  New question
                </button>
              </header>
              <p className="parsons-label">
                {session.puzzleKind === 'flow'
                  ? 'Join the flowchart pieces'
                  : 'Build the path'}
              </p>
              <p className="parsons-hint">
                {session.puzzleKind === 'flow'
                  ? 'Drag each piece onto a step. Empty steps all look the same — you decide which piece belongs where.'
                  : 'Each step says why a block belongs there. Put the matching block on that step. If a check happens for every number, move it in under that step. Tap Remove if you need to try again.'}
              </p>
              {session.verdict === 'wrong' && (
                <div className="banner plan-wrong">
                  <p className="plan-wrong-title">The plan you built is wrong.</p>
                  {session.feedback && <p className="plan-wrong-why">{session.feedback}</p>}
                </div>
              )}
              {session.puzzleKind === 'flow' ? (
                <FlowchartJoin
                  lines={arranged}
                  guide={session.confirmedLines}
                  notes={session.arrangeNotes || []}
                  showErrors={session.verdict === 'wrong'}
                  onChange={updatePathOrder}
                />
              ) : (
                <ParsonsPath
                  lines={arranged}
                  guide={session.confirmedLines}
                  roles={roles}
                  notes={session.arrangeNotes || []}
                  showErrors={session.verdict === 'wrong'}
                  onChange={updatePathOrder}
                />
              )}
              <div className="actions">
                <button
                  type="button"
                  className="primary"
                  disabled={
                    pending || pathOrder.length !== arranged.length || arranged.length === 0
                  }
                  onClick={finishArrange}
                >
                  {pending ? 'Checking…' : 'Check this order'}
                </button>
              </div>
            </section>
          )}

        {page === 'learn' &&
          session &&
          session.phase !== 'refused' &&
          session.phase !== 'done' &&
          session.phase !== 'python' &&
          session.phase !== 'arrange' && (
            <section className="socratic in">
              <header className="socratic-head">
                <p className="socratic-title">Q) {session.labQuestion}</p>
                <button type="button" className="ghost" onClick={reset}>
                  New question
                </button>
              </header>
              <div className="thread">
                {session.labBrief && (
                  <article className="lab-brief">
                    <h3>Problem analysis</h3>
                    <p className="lab-row">
                      <strong>Problem:</strong> {session.labQuestion}
                    </p>
                    {session.labBrief.input && (
                      <p className="lab-row">
                        <strong>Input:</strong> <ChipLine text={session.labBrief.input} />
                      </p>
                    )}
                    {session.labBrief.output && (
                      <p className="lab-row">
                        <strong>Expected output:</strong>{' '}
                        <code className="lab-chip">{session.labBrief.output}</code>
                      </p>
                    )}
                    {constraintBullets(session.labBrief).length > 0 && (
                      <>
                        <p className="lab-row">
                          <strong>Constraints:</strong>
                        </p>
                        <ul className="lab-constraints">
                          {constraintBullets(session.labBrief).map((item) => (
                            <li key={item}>{item}</li>
                          ))}
                        </ul>
                      </>
                    )}
                    {testRows(session.labBrief).length > 0 && (
                      <>
                        <p className="lab-row">
                          <strong>Test cases:</strong>
                        </p>
                        <div className="lab-tests">
                          <div className="lab-test-row head">
                            <div>Input</div>
                            <div className="lab-test-meta">
                              <div>Expected output</div>
                              <div>Status</div>
                            </div>
                          </div>
                          {testRows(session.labBrief).map((row, index) => (
                            <div className="lab-test-row" key={`${row.input}-${index}`}>
                              <div>
                                <code className="lab-chip">{row.input}</code>
                              </div>
                              <div className="lab-test-meta">
                                <div>
                                  {row.expected ? (
                                    <code className="lab-chip">{row.expected}</code>
                                  ) : null}
                                </div>
                                <div
                                  className={
                                    String(row.status).toLowerCase() === 'fail'
                                      ? 'lab-status fail'
                                      : 'lab-status pass'
                                  }
                                >
                                  {row.status || 'Pass'}
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      </>
                    )}
                  </article>
                )}
                {(session.confirmedLines || []).length > 0 && session.phase !== 'arrange' && (
                  <article className="turn plan-so-far" aria-label="Your plan so far">
                    <p className="socratic-ask-label">Your plan so far</p>
                    <ol className="plan-list draft-list">
                      {session.confirmedLines.map((line) => (
                        <li key={line.id} style={{ paddingLeft: `${(line.indent || 0) * 24}px` }}>
                          <code>{line.text}</code>
                        </li>
                      ))}
                    </ol>
                  </article>
                )}
                {(session?.turns || []).filter((turn) => turn.passed).map((turn, index) => (
                  <article key={`${turn.question}-${index}`} className="turn">
                    <p className="turn-q">
                      {index + 1}. {turn.question}
                    </p>
                    <div className="turn-ok">
                      <p className="turn-reveal-label">Correct</p>
                      <p className="turn-a">{turn.answer}</p>
                    </div>
                  </article>
                ))}
                {(session.phase === 'ask' || session.phase === 'confirm') && (
                  <article className="turn current">
                    <div className="socratic-ask">
                      <p className="socratic-ask-label">Question</p>
                      <p className="turn-q">{liveQuestion}</p>
                    </div>
                    {session.helpText && (
                      <aside className="socratic-hint" aria-label="Hint">
                        <p className="socratic-hint-label">Hint</p>
                        {session.helpLevel >= 2 && (
                          <p className="socratic-hint-kind">
                            {session.helpLevel === 2
                              ? 'A smaller way to think about this question'
                              : session.helpLevel === 3
                                ? 'A concrete example'
                                : session.helpLevel >= 4
                                  ? 'The idea, in everyday words'
                                  : ''}
                          </p>
                        )}
                        <p className="socratic-hint-body">{session.helpText}</p>
                      </aside>
                    )}
                    {session.verdict === 'wrong' && session.feedback && (
                      <p className="banner">{session.feedback}</p>
                    )}
                    <form
                      className="answer-form"
                      onSubmit={(event) => {
                        event.preventDefault()
                        const ready = showMcq
                          ? Boolean(picked)
                          : answer.trim().length >= (session.helpLevel >= 2 ? 1 : 4)
                        if (pending || !ready) return
                        submitAnswer(false)
                      }}
                      onKeyDown={(event) => {
                        if (event.key !== 'Enter' || event.nativeEvent.isComposing) return
                        const inTextarea = event.target instanceof HTMLTextAreaElement
                        if (inTextarea && event.shiftKey) return
                        if (!inTextarea && event.target.closest('button')) return
                        event.preventDefault()
                        event.currentTarget.requestSubmit()
                      }}
                    >
                      {showMcq ? (
                        <div className="mcq" role="radiogroup" aria-label="Choices">
                          {session.choices.map((choice) => (
                            <label
                              key={choice.letter}
                              className={
                                picked === choice.letter ? 'mcq-choice selected' : 'mcq-choice'
                              }
                            >
                              <input
                                type="radio"
                                name="socratic-mcq"
                                value={choice.letter}
                                checked={picked === choice.letter}
                                onChange={() => {
                                  setPicked(choice.letter)
                                  setAnswer(`${choice.letter}. ${choice.text}`)
                                }}
                              />
                              <span>
                                <strong>{choice.letter}.</strong> {choice.text}
                              </span>
                            </label>
                          ))}
                        </div>
                      ) : (
                        <label className="field">
                          <span>{STAGE_LABEL[session.socraticStage] || STAGE_LABEL[1]}</span>
                          <textarea
                            value={answer}
                            onChange={(event) => setAnswer(event.target.value)}
                            rows={3}
                            placeholder="Say the decision in your own words."
                          />
                        </label>
                      )}
                      <div className="actions">
                        <button
                          type="submit"
                          className="primary"
                          disabled={
                            pending ||
                            (showMcq
                              ? !picked
                              : answer.trim().length < (session.helpLevel >= 2 ? 1 : 4))
                          }
                        >
                          {pending ? 'Checking…' : 'Check this answer'}
                        </button>
                        <button
                          type="button"
                          className="ghost"
                          disabled={pending}
                          onClick={() => submitAnswer(true)}
                        >
                          I don&apos;t know
                        </button>
                      </div>
                    </form>
                  </article>
                )}
              </div>
            </section>
          )}

        {page === 'learn' && session?.phase === 'done' && (
          <section className="card finished">
            <p className="slot">Plan ready</p>
            <h2>
              {session.puzzleKind === 'flow'
                ? 'This is the flowchart you joined.'
                : 'This is the plan you built.'}
            </h2>
            <p className="muted quote">“{session.labQuestion}”</p>
            {session.explanation && (
              <p className="turn-reveal-why">{session.explanation}</p>
            )}
            <ol className="plan-list finished-list">
              {session.confirmedLines.map((line) => (
                <li key={line.id} style={{ paddingLeft: `${line.indent * 24}px` }}>
                  <code>{line.text}</code>
                </li>
              ))}
            </ol>
            <div className="lang-pick" role="group" aria-label="Write as">
              {LANGS.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className="lang-btn"
                  disabled={pending}
                  onClick={() => writeCode(undefined, item.id)}
                >
                  {pending && writingLang === item.id
                    ? 'Writing…'
                    : `Write this as ${item.label}`}
                </button>
              ))}
            </div>
            <div className="actions">
              <button type="button" className="ghost" onClick={reset}>
                Build another plan
              </button>
            </div>
          </section>
        )}

        {page === 'learn' && session?.phase === 'python' && (
          <section className="translator-page in">
            <header className="socratic-head">
              <div>
                <p className="socratic-title">Q) {session.labQuestion}</p>
              </div>
              <button type="button" className="ghost" onClick={reset}>
                New question
              </button>
            </header>
            <h2 className="plan-heading">Generated Pseudocode</h2>
            <ol className="plan-list finished-list">
              {session.confirmedLines.map((line) => (
                <li key={line.id} style={{ paddingLeft: `${line.indent * 24}px` }}>
                  <code>{line.text}</code>
                </li>
              ))}
            </ol>
            <PythonLab
              session={session}
              pending={pending}
              onConvert={writeCode}
              onPredict={submitPredict}
              onOpenRunner={openPythonRunner}
            />
          </section>
        )}

        {page === 'python-run' && session && (
          <PythonRunner
            title={session.labQuestion}
            initialCode={session.pythonCode}
            language={session.codeLanguage || 'python'}
            sessionId={session.id}
            onBack={() => go('learn')}
          />
        )}
      </main>
    </div>
  )
}

function ProfileView({ account, pending, onLogout }) {
  return (
    <section className="dash">
      <h2>Profile</h2>
      <p className="lab-row">
        <strong>Name:</strong> {account.name}
      </p>
      <p className="lab-row">
        <strong>Username:</strong> {account.username}
      </p>
      {account.created && (
        <p className="lab-row">
          <strong>Joined:</strong> {account.created}
        </p>
      )}
      <div className="actions">
        <button type="button" className="ghost" onClick={onLogout} disabled={pending}>
          Log out
        </button>
      </div>
    </section>
  )
}

function SignInPage({ user, pass, pending, onUser, onPass, onSubmit, onCreateAccount }) {
  return (
    <section className="auth-page">
      <p className="auth-kicker">Account</p>
      <h2>Sign in</h2>
      <form className="auth-card auth-form" onSubmit={onSubmit}>
        <label className="field">
          <span>Username</span>
          <input
            value={user}
            onChange={(event) => onUser(event.target.value)}
            autoComplete="username"
          />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            type="password"
            value={pass}
            onChange={(event) => onPass(event.target.value)}
            autoComplete="current-password"
          />
        </label>
        <div className="actions">
          <button
            type="submit"
            className="primary"
            disabled={pending || user.trim().length < 3 || pass.length < 4}
          >
            {pending ? 'Signing in…' : 'Sign in'}
          </button>
        </div>
      </form>
      <p className="auth-switch">
        New here?{' '}
        <button type="button" className="text-link" onClick={onCreateAccount}>
          Create an account
        </button>
      </p>
    </section>
  )
}

function SignUpPage({
  name,
  user,
  pass,
  pending,
  onName,
  onUser,
  onPass,
  onSubmit,
  onSignIn,
}) {
  return (
    <section className="auth-page">
      <p className="auth-kicker">Account</p>
      <h2>Create an account</h2>
      <form className="auth-card auth-form" onSubmit={onSubmit}>
        <label className="field">
          <span>Name</span>
          <input
            value={name}
            onChange={(event) => onName(event.target.value)}
            autoComplete="name"
          />
        </label>
        <label className="field">
          <span>Username</span>
          <input
            value={user}
            onChange={(event) => onUser(event.target.value)}
            autoComplete="username"
          />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            type="password"
            value={pass}
            onChange={(event) => onPass(event.target.value)}
            autoComplete="new-password"
          />
        </label>
        <div className="actions">
          <button
            type="submit"
            className="primary"
            disabled={pending || user.trim().length < 3 || pass.length < 4}
          >
            {pending ? 'Creating…' : 'Create account'}
          </button>
        </div>
      </form>
      <p className="auth-switch">
        Already have an account?{' '}
        <button type="button" className="text-link" onClick={onSignIn}>
          Sign in
        </button>
      </p>
    </section>
  )
}
