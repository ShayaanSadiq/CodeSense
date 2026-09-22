import type { PseudoLine } from './types.ts'

interface ParsonsBoardProps {
  lines: PseudoLine[]
  onChange: (lines: PseudoLine[]) => void
}

export function ParsonsBoard({ lines, onChange }: ParsonsBoardProps) {
  function move(from: number, to: number) {
    if (to < 0 || to >= lines.length || from === to) return
    const next = lines.slice()
    const [item] = next.splice(from, 1)
    if (!item) return
    next.splice(to, 0, item)
    onChange(next)
  }

  function setIndent(index: number, delta: number) {
    onChange(
      lines.map((line, i) =>
        i === index
          ? { ...line, indent: Math.max(0, Math.min(4, line.indent + delta)) }
          : line,
      ),
    )
  }

  return (
    <ol className="parsons">
      {lines.map((line, index) => (
        <li
          key={line.id}
          className="parsons-row"
          draggable
          onDragStart={(event) => {
            event.dataTransfer.setData('text/plain', String(index))
            event.dataTransfer.effectAllowed = 'move'
          }}
          onDragOver={(event) => {
            event.preventDefault()
            event.dataTransfer.dropEffect = 'move'
          }}
          onDrop={(event) => {
            event.preventDefault()
            const from = Number(event.dataTransfer.getData('text/plain'))
            if (Number.isFinite(from)) move(from, index)
          }}
        >
          <span className="parsons-grip" aria-hidden="true">
            ::
          </span>
          <span className="parsons-indent" style={{ width: `${line.indent * 28}px` }} />
          <code className="parsons-text">{line.text}</code>
          <div className="parsons-tools">
            <button
              type="button"
              className="ghost"
              aria-label="Move up"
              disabled={index === 0}
              onClick={() => move(index, index - 1)}
            >
              Up
            </button>
            <button
              type="button"
              className="ghost"
              aria-label="Move down"
              disabled={index === lines.length - 1}
              onClick={() => move(index, index + 1)}
            >
              Down
            </button>
            <button
              type="button"
              className="ghost"
              aria-label="Outdent"
              disabled={line.indent === 0}
              onClick={() => setIndent(index, -1)}
            >
              〈
            </button>
            <button
              type="button"
              className="ghost"
              aria-label="Indent"
              disabled={line.indent === 4}
              onClick={() => setIndent(index, 1)}
            >
              〉
            </button>
          </div>
        </li>
      ))}
    </ol>
  )
}
