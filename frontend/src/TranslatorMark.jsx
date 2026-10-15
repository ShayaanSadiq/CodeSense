import { useMemo, useState } from 'react'

const GLOSSARY = [
  {
    word: 'what you are given',
    construct: 'INPUT',
    hint: 'The numbers or text you start with.',
    aliases: [
      'you are given some numbers',
      'you are given some text',
      'you are given two measurements',
      'you are given two numbers',
      'you are given something',
    ],
    unused: 'This card does not match a sentence in the question, so you do not need it.',
  },
  {
    word: 'remember the first one',
    construct: 'SET',
    hint: 'Save one number now so you can compare later ones to it.',
    aliases: ['remember the first number', 'remember a count of zero'],
    unused:
      'You would not use this card. The question does not ask you to remember a first number. You already have the values, so you just calculate once.',
  },
  {
    word: 'look at each one',
    construct: 'FOR',
    hint: 'Go through them one by one.',
    aliases: ['look at each number', 'look at each letter'],
    unused:
      'You would not use this card. The question does not go through a list. You already have the values, so you just calculate once.',
  },
  {
    word: 'do it in one step',
    construct: 'COMPUTE',
    hint: 'You already have everything. Just calculate once.',
    aliases: ['do it in one step'],
    unused:
      'You would not use this card. The question looks at each item one by one. One-step calculate is for problems like area, where you already have everything.',
  },
  {
    word: 'only if this one is bigger',
    construct: 'IF',
    hint: 'Change your answer only when a new number is bigger.',
    aliases: [
      'keep it only if this one is bigger',
      'keep it only if this one is smaller',
      'add one only if the letter is a vowel',
    ],
    unused:
      'You would not use this card. There is no check for each item here. You already have the values, so you just calculate once.',
  },
  {
    word: 'stop at the last one',
    construct: 'UNTIL',
    hint: 'You are done when there are no more numbers.',
    aliases: ['stop at the last number', 'stop at the last letter'],
    unused:
      'You would not use this card. There is no list to finish. You already have the values, so you just calculate once.',
  },
  {
    word: 'show the answer',
    construct: 'OUTPUT',
    hint: 'Last step. Give back the result you kept.',
    aliases: ['show the answer'],
    unused: 'This card does not match a sentence in the question, so you do not need it.',
  },
]

export function TranslatorMark({
  question,
  voiced,
  needed = [],
  unused = {},
  lines = [],
  onAssemble,
  pending,
}) {
  const entries = GLOSSARY
  const [marks, setMarks] = useState([])
  const [flashKey, setFlashKey] = useState('')
  const [wrongKey, setWrongKey] = useState('')
  const [feedback, setFeedback] = useState('')
  const [feedbackOk, setFeedbackOk] = useState(false)
  const prompt = voiced || question
  const spans = useMemo(
    () => sentenceSpans(prompt, entries, lines),
    [prompt, entries, lines],
  )
  const required = useMemo(() => {
    if (needed.length > 0) {
      return entries.filter((entry) => needed.includes(entry.construct))
    }
    return entries.filter((entry) =>
      spans.some((span) => span.entry && normalize(span.entry.word) === normalize(entry.word)),
    )
  }, [entries, spans, needed])

  const ready =
    required.length > 0 &&
    required.every((entry) => marks.some((mark) => normalize(mark.word) === normalize(entry.word))) &&
    marks.every((mark) =>
      required.some((entry) => normalize(entry.word) === normalize(mark.word)),
    )

  function toggle(entry) {
    const key = normalize(entry.word)
    if (marks.some((mark) => normalize(mark.word) === key)) {
      setWrongKey('')
      setFlashKey('')
      setFeedback('')
      setFeedbackOk(false)
      setMarks((current) => current.filter((mark) => normalize(mark.word) !== key))
      return
    }
    const belongs = required.some((item) => normalize(item.word) === key)
    if (belongs) {
      setWrongKey('')
      setFlashKey(key)
      setFeedback(entry.hint)
      setFeedbackOk(true)
      setMarks((current) => [...current, { word: entry.word, construct: entry.construct }])
      return
    }
    setWrongKey(key)
    setFlashKey('')
    setFeedback(unused[entry.construct] || entry.unused)
    setFeedbackOk(false)
  }

  function isMarked(entry) {
    if (!entry) return false
    return marks.some((mark) => normalize(mark.word) === normalize(entry.word))
  }

  function assemble() {
    if (!ready) {
      setFeedback('Match every sentence in the question to a card before you build the path.')
      return
    }
    onAssemble(marks)
  }

  return (
    <section className="translator">
      <p className="parsons-label">Match the simple phrases</p>
      <p className="parsons-hint">
        The sentences use different words on purpose. Match each one to the
        card that means the same thing. A card that does not belong will tell
        you why.
      </p>
      <p className="translator-question">
        <span className="q-prefix">Q) </span>
        {spans.map((span, index) => {
          if (!span.entry) {
            return <span key={`${span.text}-${index}`}>{span.text}</span>
          }
          const marked = isMarked(span.entry)
          const flashing = marked && flashKey === normalize(span.entry.word)
          return (
            <span key={`${span.text}-${index}`}>
              <button
                type="button"
                className={
                  flashing
                    ? 'trigger marked flash'
                    : marked
                      ? 'trigger marked'
                      : 'trigger'
                }
                onClick={() => toggle(span.entry)}
              >
                {span.text.replace(/\s+$/, '')}
              </button>{' '}
            </span>
          )
        })}
      </p>
      {feedback && (
        <p className={feedbackOk ? 'banner unused-why ok' : 'banner unused-why'}>
          {feedback}
        </p>
      )}
      <ul className="glossary-grid">
        {entries.map((entry) => {
          const marked = isMarked(entry)
          const needed = required.some((item) => normalize(item.word) === normalize(entry.word))
          return (
            <li key={entry.word}>
              <button
                type="button"
                className={
                  wrongKey === normalize(entry.word)
                    ? 'glossary-card wrong'
                    : marked
                      ? 'glossary-card marked'
                      : needed
                        ? 'glossary-card'
                        : 'glossary-card extra'
                }
                onClick={() => toggle(entry)}
              >
                <span className="glossary-construct">{entry.construct}</span>
                <strong className="glossary-phrase">{entry.word}</strong>
                <span className="glossary-hint">{entry.hint}</span>
              </button>
            </li>
          )
        })}
      </ul>
      <button
        type="button"
        className="primary"
        disabled={pending || !ready}
        onClick={assemble}
      >
        {pending ? 'Assembling…' : 'Assemble the plan'}
      </button>
    </section>
  )
}

function sentenceSpans(text, entries, lines) {
  if (Array.isArray(lines) && lines.length > 0) {
    return lines.map((line) => {
      const card = String(line?.card || '').toUpperCase()
      const raw = String(line?.text || '').trim()
      return {
        text: raw ? `${raw} ` : '',
        entry: entries.find((entry) => entry.construct === card) || null,
      }
    })
  }
  const source = text || ''
  const chunks = source.match(/[^.!?]+[.!?]+\s*|[^.!?]+$/g) || (source ? [source] : [])
  return chunks.map((chunk) => ({
    text: chunk,
    entry: bestEntry(chunk, entries),
  }))
}

function bestEntry(sentence, entries) {
  let best = null
  let bestLen = 0
  const lower = sentence.toLowerCase()
  for (const entry of entries) {
    for (const needle of [entry.word, ...(entry.aliases || [])]) {
      if (!needle) continue
      if (lower.includes(needle.toLowerCase()) && needle.length > bestLen) {
        best = entry
        bestLen = needle.length
      }
    }
  }
  return best
}

function normalize(token) {
  return (token || '').toLowerCase().replace(/[^a-z0-9]+/g, '')
}
