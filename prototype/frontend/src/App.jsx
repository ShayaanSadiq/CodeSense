import { useEffect, useState } from 'react'
import {
  answerWorkshop,
  arrangeWorkshop,
  confirmWorkshop,
  getWorkshop,
  startWorkshop,
} from './api.js'
import { ParsonsBoard } from './ParsonsBoard.jsx'
import './App.css'

const SESSION_KEY = 'codesense-workshop-id'
const METHOD_KEY = 'codesense-method'

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

const SLOT_LABEL = {
  io: 'Inputs and output',
  strategy: 'Strategy',
  memory: 'Memory',
  stop: 'When to stop',
}

const METHODS = [
  { id: 'socratic', label: 'Socratic model' },
  { id: 'translator', label: 'Pseudocode translator' },
]

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
  const [method, setMethod] = useState(
    () => localStorage.getItem(METHOD_KEY) || 'socratic',
  )
  const [draft, setDraft] = useState('')
  const [session, setSession] = useState(null)
  const [answer, setAnswer] = useState('')
  const [arranged, setArranged] = useState([])
  const [error, setError] = useState(null)
  const [pending, setPending] = useState(false)
  const [lifting, setLifting] = useState(false)
  const [homeGone, setHomeGone] = useState(false)

  useEffect(() => {
    const saved = localStorage.getItem(SESSION_KEY)
    if (!saved) return
    getWorkshop(saved)
      .then(setSession)
      .catch(() => localStorage.removeItem(SESSION_KEY))
  }, [])

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
    if (method === 'translator') {
      setError(
        'The pseudocode translator is not made yet.',
      )
      return
    }
    setLifting(true)
    setError(null)
    const [next] = await Promise.all([
      run(() => startWorkshop(question)),
      new Promise((resolve) => setTimeout(resolve, 560)),
    ])
    if (!next) {
      setLifting(false)
      return
    }
    remember(next)
    setAnswer('')
    setPage('learn')
    setHomeGone(true)
  }

  async function submitAnswer(dontKnow = false) {
    if (!session) return
    const next = await run(() =>
      answerWorkshop(session.id, dontKnow ? "I don't know" : answer, dontKnow),
    )
    if (!next) return
    remember(next)
    if (next.verdict !== 'wrong') {
      setAnswer('')
    }
  }

  async function finishArrange() {
    if (!session) return
    const next = await run(() => arrangeWorkshop(session.id, arranged))
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
    setAnswer('')
    setArranged([])
    setError(null)
    setLifting(false)
    setHomeGone(false)
    setPage('learn')
  }

  function go(nextPage) {
    setPage(nextPage)
    setError(null)
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <button
          type="button"
          className="brand"
          onClick={() => go('learn')}
          aria-label="CodeSense"
        >
          <img src="/logo.png" alt="CodeSense" />
        </button>
        <nav className="nav">
          <button
            type="button"
            className={page === 'dashboard' ? 'nav-item active' : 'nav-item'}
            onClick={() => go('dashboard')}
          >
            Dashboard
          </button>
          <button
            type="button"
            className={page === 'learn' ? 'nav-item active' : 'nav-item'}
            onClick={() => go('learn')}
          >
            Learning Space
          </button>
        </nav>
        <button
          type="button"
          className={page === 'profile' ? 'nav-item profile active' : 'nav-item profile'}
          onClick={() => go('profile')}
        >
          Profile
        </button>
      </aside>

      <main className="main">
        {error && <p className="banner">{error}</p>}

        {page === 'dashboard' && (
          <section className="coming">
            <h2>Dashboard</h2>
            <p>TO be impelemneted</p>
          </section>
        )}

        {page === 'profile' && (
          <section className="coming">
            <h2>Profile</h2>
            <p>To be implemented</p>
          </section>
        )}

        {page === 'learn' && !session && !homeGone && (
          <section className={lifting ? 'learn-home lifting' : 'learn-home'}>
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
              <label className="model-pick">
                <span className="visually-hidden">Select model</span>
                <select
                  value={method}
                  onChange={(event) => chooseMethod(event.target.value)}
                  aria-label="Select model"
                >
                  {METHODS.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </label>
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

        {page === 'learn' &&
          (lifting || session) &&
          session?.phase !== 'refused' &&
          session?.phase !== 'done' && (
            <section className={session ? 'socratic in' : 'socratic arriving'}>
              <header className="socratic-head">
                <p className={homeGone || session ? 'socratic-title' : 'socratic-title wait'}>
                  Q) {session?.labQuestion || draft}
                </p>
                <button type="button" className="ghost" onClick={reset}>
                  New question
                </button>
              </header>
              <div className="socratic-split">
                <div className="thread">
                  {(session?.turns || []).map((turn, index) => (
                    <article key={`${turn.question}-${index}`} className="turn">
                      <p className="turn-q">
                        {index + 1}. {turn.question}
                      </p>
                      {turn.revealed ? (
                        <>
                          <p className="turn-mark wrong">Your answer is wrong</p>
                          <p className="turn-a turn-a-wrong">{turn.answer}</p>
                          <div className="turn-reveal">
                            <p className="turn-reveal-label">Correct decision</p>
                            <p className="turn-reveal-answer">
                              {turn.correctAnswer || turn.explanation || 'The workshop has moved on with the right decision.'}
                            </p>
                            {turn.correctAnswer && turn.explanation && (
                              <p className="turn-reveal-why">{turn.explanation}</p>
                            )}
                          </div>
                        </>
                      ) : (
                        <div className="turn-ok">
                          <p className="turn-reveal-label">Correct</p>
                          <p className="turn-a">{turn.answer}</p>
                        </div>
                      )}
                    </article>
                  ))}
                  {(session.phase === 'ask' || session.phase === 'confirm') && (
                    <article className="turn current">
                      <p className="turn-q">{session.currentQuestion}</p>
                      {session.verdict === 'wrong' && session.feedback && (
                        <p className="banner">{session.feedback}</p>
                      )}
                      <label className="field">
                        <span>{SLOT_LABEL[session.slot]}</span>
                        <textarea
                          value={answer}
                          onChange={(event) => setAnswer(event.target.value)}
                          rows={3}
                          placeholder="Say the decision in your own words."
                        />
                      </label>
                      <div className="actions">
                        <button
                          type="button"
                          className="primary"
                          disabled={pending || answer.trim().length < 4}
                          onClick={() => submitAnswer(false)}
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
                    </article>
                  )}
                  {session.phase === 'arrange' && (
                    <p className="muted">
                      Questions are done. Complete the Parsons puzzle to get the
                      pseudocode.
                    </p>
                  )}
                </div>
                <aside className="parsons-panel">
                  <p className="parsons-label">Parson&apos;s puzzle</p>
                  {session.phase === 'arrange' && (
                    <p className="parsons-hint">
                      Drag a line onto a dashed slot. Click the three boxes to
                      indent (or Tab / Shift+Tab). Arrow keys move the focused
                      line. Indent only the IF under the FOR.
                    </p>
                  )}
                  {session.phase === 'arrange' ? (
                    <>
                      {session.verdict === 'wrong' && session.feedback && (
                        <p className="banner">{session.feedback}</p>
                      )}
                      <ParsonsBoard lines={arranged} onChange={setArranged} />
                      <div className="actions">
                        <button
                          type="button"
                          className="primary"
                          disabled={pending || arranged.length === 0}
                          onClick={finishArrange}
                        >
                          {pending ? 'Checking…' : 'Check this order'}
                        </button>
                      </div>
                    </>
                  ) : (
                    <p className="parsons-wait">
                      The puzzle opens after the last Socratic question.
                    </p>
                  )}
                </aside>
              </div>
            </section>
          )}

        {page === 'learn' && session?.phase === 'done' && (
          <section className="card finished">
            <p className="slot">Plan ready</p>
            <h2>This is the procedure you built.</h2>
            <p className="muted quote">“{session.labQuestion}”</p>
            <ol className="plan-list finished-list">
              {session.confirmedLines.map((line) => (
                <li key={line.id} style={{ paddingLeft: `${line.indent * 24}px` }}>
                  <code>{line.text}</code>
                </li>
              ))}
            </ol>
            <button type="button" className="primary" onClick={reset}>
              Build another plan
            </button>
          </section>
        )}
      </main>
    </div>
  )
}
