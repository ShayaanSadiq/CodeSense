import { useMemo, useState } from 'react'

const FALLBACK = [
  { word: 'list', construct: 'INPUT', hint: 'Incoming collection' },
  { word: 'string', construct: 'INPUT', hint: 'Incoming text' },
  { word: 'length', construct: 'INPUT', hint: 'A measurement that comes in' },
  { word: 'width', construct: 'INPUT', hint: 'A measurement that comes in' },
  { word: 'each', construct: 'FOR', hint: 'Walk every item' },
  { word: 'every', construct: 'FOR', hint: 'Walk every item' },
  { word: 'once', construct: 'COMPUTE', hint: 'No loop' },
  { word: 'multiply', construct: 'COMPUTE', hint: 'One calculation' },
  { word: 'largest', construct: 'OUTPUT', hint: 'The result that comes out' },
  { word: 'smallest', construct: 'OUTPUT', hint: 'The result that comes out' },
  { word: 'area', construct: 'OUTPUT', hint: 'The result that comes out' },
  { word: 'vowels', construct: 'OUTPUT', hint: 'The result that comes out' },
]

export function TranslatorMark({ question, glossary, onAssemble, pending }) {
  const entries = glossary.length > 0 ? glossary : FALLBACK
  const [marks, setMarks] = useState([])

  const tokens = useMemo(() => tokenize(question), [question])

  function toggle(word) {
    const key = word.toLowerCase()
    const found = entries.find((entry) => entry.word === key)
    if (!found) return
    setMarks((current) => {
      if (current.some((mark) => mark.word === found.word)) {
        return current.filter((mark) => mark.word !== found.word)
      }
      return [...current, { word: found.word, construct: found.construct }]
    })
  }

  function setConstruct(word, construct) {
    setMarks((current) =>
      current.map((mark) => (mark.word === word ? { ...mark, construct } : mark)),
    )
  }

  return (
    <section className="translator">
      <p className="parsons-label">Mark trigger words</p>
      <p className="parsons-hint">
        Click a glossary word in the question. Then check the construct it maps
        to. Assemble builds the plan; the path puzzle comes next.
      </p>
      <p className="translator-question">
        {tokens.map((token, index) => {
          const key = token.toLowerCase()
          const known = entries.some((entry) => entry.word === key)
          const marked = marks.some((mark) => mark.word === key)
          if (!known) {
            return <span key={`${token}-${index}`}>{token}</span>
          }
          return (
            <button
              key={`${token}-${index}`}
              type="button"
              className={marked ? 'trigger marked' : 'trigger'}
              onClick={() => toggle(token)}
            >
              {token}
            </button>
          )
        })}
      </p>
      <ul className="glossary">
        {entries.map((entry) => (
          <li key={entry.word}>
            <strong>{entry.word}</strong>
            <span>{entry.hint}</span>
            <code>{entry.construct}</code>
          </li>
        ))}
      </ul>
      {marks.length > 0 && (
        <ul className="marks">
          {marks.map((mark) => (
            <li key={mark.word}>
              <span>{mark.word}</span>
              <select
                value={mark.construct}
                onChange={(event) => setConstruct(mark.word, event.target.value)}
                aria-label={`Construct for ${mark.word}`}
              >
                {['INPUT', 'SET', 'FOR', 'COMPUTE', 'IF', 'UNTIL', 'RETURN', 'OUTPUT'].map(
                  (construct) => (
                    <option key={construct} value={construct}>
                      {construct}
                    </option>
                  ),
                )}
              </select>
            </li>
          ))}
        </ul>
      )}
      <button
        type="button"
        className="primary"
        disabled={pending}
        onClick={() => onAssemble(marks)}
      >
        {pending ? 'Assembling…' : 'Assemble the plan'}
      </button>
    </section>
  )
}

function tokenize(text) {
  return (text || '').split(/(\s+)/).filter((part) => part.length > 0)
}
