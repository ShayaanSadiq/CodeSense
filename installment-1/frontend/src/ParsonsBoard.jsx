import { useState } from 'react'

export function ParsonsBoard({ lines, onChange }) {
  const [focus, setFocus] = useState(0)
  const [dragFrom, setDragFrom] = useState(null)
  const [overSlot, setOverSlot] = useState(null)

  function move(from, to) {
    if (to < 0 || to >= lines.length || from === to) return
    const next = lines.slice()
    const [item] = next.splice(from, 1)
    if (!item) return
    next.splice(to, 0, item)
    onChange(next)
    setFocus(to)
  }

  function dropOnSlot(slot) {
    if (dragFrom == null) return
    let to = slot
    if (dragFrom < slot) to = slot - 1
    move(dragFrom, to)
    setDragFrom(null)
    setOverSlot(null)
  }

  function setIndent(index, indent) {
    const nextIndent = Math.max(0, Math.min(2, indent))
    onChange(
      lines.map((line, i) => (i === index ? { ...line, indent: nextIndent } : line)),
    )
  }

  function onKey(event, index) {
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      move(index, index - 1)
    } else if (event.key === 'ArrowDown') {
      event.preventDefault()
      move(index, index + 1)
    } else if (event.key === 'Tab') {
      event.preventDefault()
      const line = lines[index]
      if (!line) return
      setIndent(index, line.indent + (event.shiftKey ? -1 : 1))
    } else if (event.key === 'Home') {
      event.preventDefault()
      move(index, 0)
    } else if (event.key === 'End') {
      event.preventDefault()
      move(index, lines.length - 1)
    }
  }

  return (
    <div
      className="parsons"
      role="listbox"
      aria-label="Parsons puzzle. Arrow keys move a line. Tab indents, Shift+Tab outdents."
    >
      <DropSlot
        active={overSlot === 0}
        onDragOver={() => setOverSlot(0)}
        onDrop={() => dropOnSlot(0)}
      />
      {lines.map((line, index) => (
        <div key={line.id}>
          <div
            className={
              focus === index
                ? 'parsons-row focused'
                : 'parsons-row'
            }
            role="option"
            aria-selected={focus === index}
            tabIndex={0}
            draggable
            onFocus={() => setFocus(index)}
            onKeyDown={(event) => onKey(event, index)}
            onDragStart={(event) => {
              event.dataTransfer.setData('text/plain', String(index))
              event.dataTransfer.effectAllowed = 'move'
              setDragFrom(index)
            }}
            onDragEnd={() => {
              setDragFrom(null)
              setOverSlot(null)
            }}
          >
            <span className="parsons-grip" aria-hidden="true">
              ::
            </span>
            <div className="parsons-steps" role="group" aria-label="Indent">
              {[0, 1, 2].map((step) => (
                <button
                  key={step}
                  type="button"
                  className={
                    line.indent === step
                      ? 'parsons-step selected'
                      : 'parsons-step'
                  }
                  aria-label={step === 0 ? 'No indent' : `Indent ${step}`}
                  aria-pressed={line.indent === step}
                  onClick={() => setIndent(index, step)}
                />
              ))}
            </div>
            <code
              className="parsons-text"
              style={{ paddingLeft: `${line.indent * 1.4}rem` }}
            >
              {line.text}
            </code>
          </div>
          <DropSlot
            active={overSlot === index + 1}
            onDragOver={() => setOverSlot(index + 1)}
            onDrop={() => dropOnSlot(index + 1)}
          />
        </div>
      ))}
    </div>
  )
}

function DropSlot({ active, onDragOver, onDrop }) {
  return (
    <div
      className={active ? 'parsons-slot active' : 'parsons-slot'}
      onDragOver={(event) => {
        event.preventDefault()
        event.dataTransfer.dropEffect = 'move'
        onDragOver()
      }}
      onDragLeave={() => {}}
      onDrop={(event) => {
        event.preventDefault()
        onDrop()
      }}
    />
  )
}
