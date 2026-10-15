import { useEffect, useRef, useState } from 'react'
import { runWorkshop } from './api.js'
import { runC } from './cRuntime.js'
import { clearStop, loadPyodideRuntime, requestStop } from './pythonRuntime.js'

function fileName(language) {
  if (language === 'java') return 'Lab.java'
  if (language === 'c') return 'lab.c'
  return 'main.py'
}

function langLabel(language) {
  if (language === 'java') return 'Java'
  if (language === 'c') return 'C'
  return 'Python'
}

export function PythonRunner({
  title,
  initialCode,
  language = 'python',
  sessionId,
  onBack,
}) {
  const [code, setCode] = useState(initialCode || '')
  const [stdin, setStdin] = useState('')
  const [lines, setLines] = useState([])
  const [status, setStatus] = useState('idle')
  const [prompt, setPrompt] = useState('')
  const runningRef = useRef(false)
  const timerRef = useRef(null)
  const inputResolve = useRef(null)
  const label = langLabel(language)

  useEffect(() => {
    setCode(initialCode || '')
  }, [initialCode])

  useEffect(
    () => () => {
      if (timerRef.current) clearTimeout(timerRef.current)
    },
    [],
  )

  function write(kind, text) {
    if (text == null || text === '') return
    setLines((current) => [...current, { kind, text: String(text) }])
  }

  function waitForInput(message) {
    setPrompt(message || 'The script asked for input.')
    setStatus('input')
    return new Promise((resolve) => {
      inputResolve.current = resolve
    })
  }

  function submitInput(event) {
    event.preventDefault()
    if (!inputResolve.current) return
    const value = stdin
    write('in', value || '(empty)')
    setStdin('')
    setPrompt('')
    const resolve = inputResolve.current
    inputResolve.current = null
    setStatus('running')
    resolve(`${value}\n`)
  }

  async function runPython() {
    const pyodide = await loadPyodideRuntime()
    setStatus('running')
    pyodide.setStdout({ batched: (text) => write('out', text) })
    pyodide.setStderr({ batched: (text) => write('err', text) })
    pyodide.setStdin({
      stdin: () => waitForInput('The script asked for input.'),
    })
    const hung = new Promise((_, reject) => {
      timerRef.current = setTimeout(() => {
        requestStop()
        reject(new Error('Stopped: this looked like it would run forever.'))
      }, 5000)
    })
    await Promise.race([pyodide.runPythonAsync(code), hung])
  }

  async function runHost() {
    setStatus('running')
    const result = await runWorkshop(sessionId, stdin, code)
    if (result.stdout) write('out', result.stdout)
    if (result.stderr) write('err', result.stderr)
    if (!result.stdout && !result.stderr) {
      write('out', result.exitCode === 0 ? '(no output)' : 'The program did not print anything.')
    }
  }

  async function runCInBrowser() {
    setStatus('running')
    const hung = new Promise((_, reject) => {
      timerRef.current = setTimeout(() => {
        reject(new Error('Stopped: this looked like it would run forever.'))
      }, 5000)
    })
    await Promise.race([
      Promise.resolve().then(() => {
        runC(code, stdin, (text) => write('out', text))
      }),
      hung,
    ])
  }

  async function run() {
    if (runningRef.current) return
    runningRef.current = true
    setStatus('loading')
    setLines([])
    setPrompt('')
    clearStop()
    try {
      if (language === 'python') {
        await runPython()
      } else if (language === 'c') {
        await runCInBrowser()
      } else {
        await runHost()
      }
      setStatus('idle')
    } catch (err) {
      const message = String(err?.message || err)
      if (message.includes('KeyboardInterrupt') || message.includes('interrupted')) {
        write('err', 'Stopped.')
      } else {
        write('err', message)
      }
      setStatus('idle')
    } finally {
      if (timerRef.current) {
        clearTimeout(timerRef.current)
        timerRef.current = null
      }
      runningRef.current = false
      inputResolve.current = null
      setPrompt('')
      clearStop()
    }
  }

  function stop() {
    requestStop()
    if (timerRef.current) {
      clearTimeout(timerRef.current)
      timerRef.current = null
    }
    runningRef.current = false
    if (inputResolve.current) {
      inputResolve.current('\n')
      inputResolve.current = null
    }
    setStatus('idle')
    write('err', 'Stopped.')
  }

  const busy = status === 'loading' || status === 'running' || status === 'input'

  return (
    <section className="ide-page">
      <header className="ide-head">
        <div>
          <p className="socratic-title">Q) {title}</p>
          <p className="muted same-idea">
            {label} · edit, run, and read the output here
          </p>
        </div>
        <button type="button" className="ghost" onClick={onBack}>
          Back to the plan
        </button>
      </header>
      <div className="ide-work">
        <div className="ide-pane editor">
          <div className="ide-toolbar">
            <span className="ide-file">{fileName(language)}</span>
            <div className="ide-toolbar-actions">
              <button type="button" className="ide-stop" disabled={!busy} onClick={stop}>
                Stop
              </button>
              <button type="button" className="ide-run" disabled={busy} onClick={run}>
                {status === 'loading' ? 'Loading…' : 'Run'}
              </button>
            </div>
          </div>
          <textarea
            className="ide-code"
            value={code}
            onChange={(event) => setCode(event.target.value)}
            spellCheck={false}
            aria-label={`${label} editor`}
          />
        </div>
        <div className="ide-pane output">
          <div className="ide-toolbar">
            <span className="ide-file">Output</span>
            <span className="ide-state">
              {status === 'loading'
                ? `Loading ${label}…`
                : status === 'running'
                  ? 'Running'
                  : status === 'input'
                    ? 'Waiting for input'
                    : 'Ready'}
            </span>
          </div>
          <pre className="ide-console">
            {lines.length === 0 ? (
              <span className="ide-empty">
                Run the script to see printed text and errors here.
              </span>
            ) : (
              lines.map((line, index) => (
                <span key={`${line.kind}-${index}`} className={`ide-line ${line.kind}`}>
                  {line.kind === 'in' ? `> ${line.text}` : line.text}
                  {'\n'}
                </span>
              ))
            )}
          </pre>
          {status === 'input' && (
            <form className="ide-input" onSubmit={submitInput}>
              <label>
                <span>{prompt}</span>
                <input
                  value={stdin}
                  onChange={(event) => setStdin(event.target.value)}
                  autoFocus
                />
              </label>
              <button type="submit" className="ide-run">
                Submit
              </button>
            </form>
          )}
          {language === 'java' && (
            <label className="ide-input field" style={{ margin: '0.6rem 0.75rem 0.75rem' }}>
              <span>Typed input for the next run</span>
              <input
                value={stdin}
                onChange={(event) => setStdin(event.target.value)}
                placeholder="Leave blank if the program needs none"
                disabled={busy}
              />
            </label>
          )}
        </div>
      </div>
    </section>
  )
}
