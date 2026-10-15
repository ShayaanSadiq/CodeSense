import { useEffect, useMemo, useState } from 'react'

const LANGS = [
  { id: 'python', label: 'Python' },
  { id: 'java', label: 'Java' },
  { id: 'c', label: 'C' },
]

export function PythonLab({
  session,
  pending,
  onConvert,
  onPredict,
  onOpenRunner,
}) {
  const [sample, setSample] = useState(session.sampleInput || '')
  const [guess, setGuess] = useState('')
  const [hovered, setHovered] = useState(-1)
  const language = session.codeLanguage || 'python'
  const langLabel = LANGS.find((item) => item.id === language)?.label || 'Python'

  useEffect(() => {
    setSample(session.sampleInput || '')
  }, [session.sampleInput])

  useEffect(() => {
    setGuess('')
  }, [session.predictIndex])

  const steps = session.pythonTrace || []
  const index = session.predictIndex || 0
  const current = index < steps.length ? steps[index] : null
  const donePredicting = steps.length > 0 && index >= steps.length
  const last = index > 0 ? steps[index - 1] : null
  const codeRows = useMemo(
    () => attachWalk(session.pythonCode || '', session.pythonWalk || []),
    [session.pythonCode, session.pythonWalk],
  )

  function submitGuess(dontKnow = false) {
    onPredict(dontKnow ? "I don't know" : guess, dontKnow)
    setGuess('')
  }

  return (
    <section className="python-lab">
      <div className="lang-pick" role="group" aria-label="Language">
        {LANGS.map((item) => (
          <button
            key={item.id}
            type="button"
            className={language === item.id ? 'lang-btn selected' : 'lang-btn'}
            disabled={pending}
            onClick={() => onConvert(sample, item.id)}
          >
            {item.label}
          </button>
        ))}
      </div>
      <p className="parsons-label">Write this as {langLabel}</p>
      <p className="parsons-hint">
        {donePredicting
          ? `You predicted every step. Now you can change the given values and run the ${langLabel} in this browser.`
          : 'Answer what happens at each step first. You can run the script after the last question.'}
      </p>
      <pre className="python-code python-code-live" onMouseLeave={() => setHovered(-1)}>
        {codeRows.map((row, lineIndex) => (
          <div
            key={`py-${lineIndex}`}
            className={
              hovered === lineIndex && row.note
                ? 'python-line open'
                : row.note
                  ? 'python-line'
                  : 'python-line mute'
            }
            tabIndex={row.note ? 0 : undefined}
            onMouseEnter={() => setHovered(row.note ? lineIndex : -1)}
            onFocus={() => row.note && setHovered(lineIndex)}
            onClick={() => setHovered(row.note ? lineIndex : -1)}
          >
            <code>{row.text.length ? row.text : ' '}</code>
            {hovered === lineIndex && row.note && (
              <aside className="python-line-tip">
                <p className="python-line-what">{row.note.heading}</p>
                {row.note.body && <p>{row.note.body}</p>}
                {row.note.needed && <p>{row.note.needed}</p>}
              </aside>
            )}
          </div>
        ))}
      </pre>
      <p className="python-hover-hint">Hover a line of code to see what it is, why it works, and why the script needs it.</p>
      {!donePredicting && (
        <div className="python-predict">
          <div className="predict-head">
            <p className="predict-kicker">Predict the next step</p>
            {steps.length > 0 && (
              <p className="predict-count">
                {Math.min(index + 1, steps.length)} of {steps.length}
              </p>
            )}
          </div>
          {steps.length > 0 && (
            <ol className="predict-track" aria-label="Step progress">
              {steps.map((step, stepIndex) => (
                <li
                  key={step.id}
                  className={
                    stepIndex < index
                      ? step.passed
                        ? 'predict-dot done'
                        : 'predict-dot skip'
                      : stepIndex === index
                        ? 'predict-dot now'
                        : 'predict-dot'
                  }
                >
                  <span className="sr-only">
                    Step {stepIndex + 1}
                    {stepIndex < index
                      ? step.passed
                        ? ', correct'
                        : ', shown'
                      : stepIndex === index
                        ? ', current'
                        : ', upcoming'}
                  </span>
                </li>
              ))}
            </ol>
          )}
          {last && (
            <div className={last.passed ? 'predict-last ok' : 'predict-last skip'}>
              <p className="predict-last-kicker">
                {last.passed ? 'You got the last step' : 'Here is the last step'}
              </p>
              {last.lastGuess ? (
                <p className="predict-last-guess">You said {last.lastGuess}</p>
              ) : null}
              <p className="predict-last-body">{last.reveal}</p>
            </div>
          )}
          {current && (
            <div
              className={
                session.verdict === 'easier'
                  ? 'predict-now easier'
                  : session.verdict === 'wrong'
                    ? 'predict-now miss'
                    : 'predict-now'
              }
            >
              {session.verdict === 'easier' && (
                <p className="predict-easier">Easier question · same step</p>
              )}
              <p className="python-now-q">{current.prompt}</p>
              {session.verdict === 'wrong' && session.feedback && (
                <p className="predict-miss">{session.feedback}</p>
              )}
              <form
                onSubmit={(event) => {
                  event.preventDefault()
                  if (pending || !guess.trim()) return
                  submitGuess(false)
                }}
              >
                <label className="field">
                  <span>Your guess</span>
                  <input
                    value={guess}
                    onChange={(event) => setGuess(event.target.value)}
                    placeholder="A number, a list, or a short phrase."
                  />
                </label>
                <div className="actions">
                  <button
                    type="submit"
                    className="primary"
                    disabled={pending || guess.trim().length < 1}
                  >
                    {pending ? 'Checking…' : 'Check this guess'}
                  </button>
                  <button
                    type="button"
                    className="ghost"
                    disabled={pending}
                    onClick={() => submitGuess(true)}
                  >
                    I don&apos;t know
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      )}
      {donePredicting && (
        <div className="python-run">
          <p className="banner unused-why ok">
            Walkthrough finished. The script should print {session.expectedOutput}.
          </p>
          <form
            onSubmit={(event) => {
              event.preventDefault()
              if (pending) return
              onConvert(sample, language)
            }}
          >
            <label className="field">
              <span>Given values</span>
              <input
                value={sample}
                onChange={(event) => setSample(event.target.value)}
                aria-label="Sample input"
              />
            </label>
            <div className="actions">
              <button type="submit" className="ghost" disabled={pending}>
                {pending ? 'Updating…' : 'Use these values'}
              </button>
              <button
                type="button"
                className="primary"
                disabled={!session.pythonCode}
                onClick={onOpenRunner}
              >
                Run in this browser
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  )
}

function attachWalk(source, walk) {
  const notes = (Array.isArray(walk) ? walk : []).filter((note) => {
    const code = String(note?.code || '').trim()
    if (!code) {
      return true
    }
    return scanLine(code, false).runnable
  })
  const used = new Set()
  let inBlock = false
  return source.split('\n').map((text) => {
    const trimmed = text.trim()
    const scan = scanLine(trimmed, inBlock)
    inBlock = scan.inBlock
    if (!scan.runnable) {
      return { text, note: null }
    }
    let match = notes.findIndex(
      (note, index) =>
        !used.has(index) && note.code && sameCode(note.code, trimmed),
    )
    if (match < 0) {
      match = notes.findIndex(
        (note, index) => !used.has(index) && !note.code && (note.heading || note.body),
      )
    }
    if (match < 0) {
      return { text, note: null }
    }
    used.add(match)
    return { text, note: notes[match] }
  })
}

function scanLine(trimmed, inBlock) {
  if (!trimmed) {
    return { runnable: false, inBlock }
  }
  if (inBlock) {
    return { runnable: false, inBlock: !trimmed.includes('*/') }
  }
  if (trimmed.startsWith('//') || trimmed.startsWith('*/')) {
    return { runnable: false, inBlock: false }
  }
  if (trimmed.startsWith('/*')) {
    return { runnable: false, inBlock: !trimmed.includes('*/') }
  }
  if (trimmed.startsWith('#') && !isPreprocessor(trimmed)) {
    return { runnable: false, inBlock: false }
  }
  return { runnable: true, inBlock: false }
}

function isPreprocessor(trimmed) {
  return /^#\s*(include|define|undef|ifdef|ifndef|if|elif|else|endif|pragma|error|warning)\b/i.test(
    trimmed,
  )
}

function sameCode(left, right) {
  return String(left || '')
    .trim()
    .replace(/\s+/g, ' ') === String(right || '')
    .trim()
    .replace(/\s+/g, ' ')
}
