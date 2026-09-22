import { useEffect, useState } from 'react'
import {
  answerWorkshop,
  arrangeWorkshop,
  confirmWorkshop,
  getWorkshop,
  startWorkshop,
} from './api.ts'
import { ParsonsBoard } from './ParsonsBoard.tsx'
import type { PseudoLine, Session } from './types.ts'
import './App.css'

const SESSION_KEY = 'codesense-workshop-id'

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
  {
    label: 'SQL — should refuse',
    text: 'Write a SQL query to list students in CS101.',
  },
] as const

const SLOT_LABEL: Record<Session['slot'], string> = {
  io: 'Inputs and output',
  strategy: 'Strategy',
  memory: 'Memory',
  stop: 'When to stop',
}

function shuffleParsons(lines: PseudoLine[]): PseudoLine[] {
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
  const [draft, setDraft] = useState('')
  const [session, setSession] = useState<Session | null>(null)
  const [answer, setAnswer] = useState('')
  const [edits, setEdits] = useState<PseudoLine[]>([])
  const [arranged, setArranged] = useState<PseudoLine[]>([])
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  useEffect(() => {
    const saved = localStorage.getItem(SESSION_KEY)
    if (!saved) return
    getWorkshop(saved)
      .then(setSession)
      .catch(() => localStorage.removeItem(SESSION_KEY))
  }, [])

  useEffect(() => {
    if (session?.phase === 'confirm') {
      setEdits(session.proposedLines)
    }
    if (session?.phase === 'arrange') {
      setArranged(
        session.puzzleLines && session.puzzleLines.length > 0
          ? session.puzzleLines
          : shuffleParsons(session.confirmedLines),
      )
    }
  }, [session])

  async function run<T>(work: () => Promise<T>): Promise<T | undefined> {
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

  async function begin(question: string) {
    const next = await run(() => startWorkshop(question))
    if (!next) return
    remember(next)
    setAnswer('')
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

  async function confirmLines() {
    if (!session) return
    const next = await run(() => confirmWorkshop(session.id, edits))
    if (!next) return
    remember(next)
  }

  async function finishArrange() {
    if (!session) return
    const next = await run(() => arrangeWorkshop(session.id, arranged))
    if (!next) return
    remember(next)
  }

  function remember(next: Session) {
    localStorage.setItem(SESSION_KEY, next.id)
    setSession(next)
  }

  function reset() {
    localStorage.removeItem(SESSION_KEY)
    setSession(null)
    setAnswer('')
    setEdits([])
    setArranged([])
    setError(null)
  }

  return (
    <div className="page">
      <header className="top">
        <div>
          <p className="eyebrow">Question to plan</p>
          <h1>CodeSense</h1>
        </div>
        {session && session.phase !== 'refused' && (
          <button type="button" className="ghost" onClick={reset}>
            New question
          </button>
        )}
      </header>

      {error && <p className="banner">{error}</p>}

      {!session && (
        <section className="hero card">
          <p className="lede">
            Paste one console procedure. You decide each step. The model writes
            the INPUT, OUTPUT, and the rest of the pseudocode from those
            decisions.
          </p>
          <label className="field">
            <span>Lab question</span>
            <textarea
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              rows={5}
              placeholder="Find the largest number in a list."
            />
          </label>
          <div className="chips">
            {SAMPLES.map((sample) => (
              <button
                key={sample.label}
                type="button"
                className="chip"
                onClick={() => setDraft(sample.text)}
              >
                {sample.label}
              </button>
            ))}
          </div>
          <button
            type="button"
            className="primary"
            disabled={pending || draft.trim().length < 8}
            onClick={() => begin(draft)}
          >
            {pending ? 'Checking…' : 'Start the workshop'}
          </button>
        </section>
      )}

      {session?.phase === 'refused' && (
        <section className="card refuse">
          <p className="refuse-line">{session.refusal}</p>
          <p className="muted">You pasted: “{session.labQuestion}”</p>
          <button type="button" className="primary" onClick={reset}>
            Paste a different part
          </button>
        </section>
      )}

      {session && session.phase !== 'refused' && session.phase !== 'done' && (
        <div className={session.phase === 'arrange' ? 'work puzzle' : 'work'}>
          {session.phase !== 'arrange' && (
          <aside className="plan card">
            <h2>The plan so far</h2>
            <p className="muted quote">“{session.labQuestion}”</p>
            {session.confirmedLines.length === 0 ? (
              <p className="muted">Nothing confirmed yet. A line appears only after you decide it.</p>
            ) : (
              <ol className="plan-list">
                {session.confirmedLines.map((line) => (
                  <li key={line.id} style={{ paddingLeft: `${line.indent * 20}px` }}>
                    <code>{line.text}</code>
                  </li>
                ))}
              </ol>
            )}
          </aside>
          )}

          <section className="decision card">
            {session.phase === 'ask' && (
              <>
                <p className="slot">{SLOT_LABEL[session.slot]}</p>
                <h2>{session.currentQuestion}</h2>
                {session.verdict === 'wrong' && session.feedback && (
                  <p className="banner">{session.feedback}</p>
                )}
                <label className="field">
                  <span>Your decision</span>
                  <textarea
                    value={answer}
                    onChange={(event) => setAnswer(event.target.value)}
                    rows={4}
                    placeholder="Say the decision in your own words. Do not paste code."
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
              </>
            )}

            {session.phase === 'confirm' && (
              <>
                <p className="slot">Confirm or rewrite</p>
                <h2>
                  {session.verdict === 'revealed'
                    ? 'Here is the correct answer.'
                    : 'Is this what you decided?'}
                </h2>
                {session.verdict === 'revealed' && (
                  <div className="reveal">
                    {session.feedback && <p className="banner">{session.feedback}</p>}
                    {session.correctAnswer && (
                      <p>
                        <strong>Correct answer: </strong>
                        {session.correctAnswer}
                      </p>
                    )}
                    {session.explanation && (
                      <p className="muted">{session.explanation}</p>
                    )}
                  </div>
                )}
                <p className="muted">
                  {session.verdict === 'revealed'
                    ? 'These lines come from the correct decision. Change any line before it joins the plan.'
                    : 'The model wrote these lines from your decision. Change any line before it joins the plan.'}
                </p>
                <ul className="edit-lines">
                  {edits.map((line, index) => (
                    <li key={line.id}>
                      <input
                        value={line.text}
                        onChange={(event) =>
                          setEdits((current) =>
                            current.map((item, i) =>
                              i === index
                                ? { ...item, text: event.target.value }
                                : item,
                            ),
                          )
                        }
                      />
                    </li>
                  ))}
                </ul>
                <div className="actions">
                  <button
                    type="button"
                    className="primary"
                    disabled={pending || edits.every((line) => !line.text.trim())}
                    onClick={confirmLines}
                  >
                    {pending ? 'Saving…' : 'Add to the plan'}
                  </button>
                </div>
              </>
            )}

            {session.phase === 'arrange' && (
              <>
                <p className="slot">Parsons puzzle</p>
                <h2>Put the shuffled lines in the right order, and indent nested steps.</h2>
                <p className="muted">
                  Drag, or use Up / Down. Indent a line under a FOR, WHILE, or
                  IF. The original order is hidden.
                </p>
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
            )}
          </section>
        </div>
      )}

      {session?.phase === 'done' && (
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
    </div>
  )
}
