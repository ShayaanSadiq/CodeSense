import { useEffect, useRef, useState } from 'react'
import { useDragScroll } from './useDragScroll.js'

const FALLBACK_ROLES = {
  INPUT: {
    title: 'Start with what you are given',
    why: 'You cannot do any work until you know the numbers or text. That is why this is always first.',
  },
  SET: {
    title: 'Remember the first one',
    why: 'Save the first number now. Later numbers will be compared to it, so this comes right after what you are given.',
  },
  FOR: {
    title: 'Look at each one',
    why: 'Now go through every number, one by one. This comes after you know the list and have remembered the first number.',
  },
  COMPUTE: {
    title: 'Do it in one step',
    why: 'You do not need to go through a list. Just calculate once from the values you already have.',
  },
  IF: {
    title: 'Check each number',
    why: 'This check happens for every number, not after you finish. Put it under the step that looks at each one.',
  },
  UPDATE: {
    title: 'Change it only when the check is yes',
    why: 'This is not a second starting value. It belongs under the check, after you look at each number.',
  },
  UNTIL: {
    title: 'Stop at the last one',
    why: 'You stop when there are no more numbers. This comes after the check.',
  },
  OUTPUT: {
    title: 'Show the answer',
    why: 'Last step. Give back the result you kept. The person only sees it after all the work. This is always last.',
  },
}
FALLBACK_ROLES.RETURN = FALLBACK_ROLES.OUTPUT

export function ParsonsPath({ lines, guide = [], roles = [], notes = [], onChange, showErrors = false }) {
  const [tray, setTray] = useState(lines)
  const [path, setPath] = useState([])
  const [dragFrom, setDragFrom] = useState(null)
  const [picked, setPicked] = useState(null)
  const workspaceRef = useRef(null)
  useDragScroll(workspaceRef)

  const pieceKey = [...lines.map((line) => line.id)].sort().join(',')
  useEffect(() => {
    setTray(lines.map((line) => ({ ...line, indent: 0 })))
    setPath([])
    setPicked(null)
  }, [pieceKey])

  const roleByVerb = {}
  for (const role of roles) {
    roleByVerb[role.construct] = role
  }

  function emit(nextPath) {
    onChange(nextPath.filter(Boolean))
  }

  function itemFrom(data) {
    if (!data?.id) return null
    return (
      tray.find((line) => line.id === data.id) ||
      path.find((line) => line?.id === data.id) ||
      null
    )
  }

  function placeOn(slot, item, indent) {
    const nextPath = path.slice()
    while (nextPath.length < lines.length) nextPath.push(null)
    const nextTray = tray.filter((line) => line.id !== item.id)
    const existing = nextPath[slot]
    if (existing && existing.id !== item.id) {
      nextTray.push({ ...existing, indent: 0 })
    }
    const fromPath = path.findIndex((line) => line?.id === item.id)
    if (fromPath >= 0) {
      nextPath[fromPath] = null
    }
    nextPath[slot] = { ...item, indent }
    setTray(nextTray)
    setPath(nextPath)
    emit(nextPath)
    setDragFrom(null)
    setPicked(null)
  }

  function returnToTray(index) {
    const item = path[index]
    if (!item) return
    const nextPath = path.slice()
    nextPath[index] = null
    setPath(nextPath)
    setTray([...tray, { ...item, indent: 0 }])
    emit(nextPath)
    setDragFrom(null)
    setPicked(null)
  }

  function setIndent(index, indent) {
    const item = path[index]
    if (!item) return
    const nextPath = path.slice()
    nextPath[index] = { ...item, indent: Math.max(0, Math.min(2, indent)) }
    setPath(nextPath)
    emit(nextPath)
  }

  function payload(event) {
    try {
      return JSON.parse(event.dataTransfer.getData('application/json'))
    } catch {
      return null
    }
  }

  function startDrag(event, source, id) {
    event.dataTransfer.setData('application/json', JSON.stringify({ source, id }))
    event.dataTransfer.effectAllowed = 'move'
    setDragFrom({ source, id })
  }

  const usedTitles = new Set()

  return (
    <div className="path-workspace full" ref={workspaceRef}>
      <div className="path-tray" aria-label="Unused blocks">
        {tray.length === 0 ? (
          <p className="path-tray-empty">
            All blocks are on the path. Remove one if you need to move it.
          </p>
        ) : (
          tray.map((line) => (
            <button
              key={line.id}
              type="button"
              className={
                picked?.id === line.id ? 'path-block picked' : 'path-block'
              }
              draggable
              onDragStart={(event) => startDrag(event, 'tray', line.id)}
              onClick={() =>
                setPicked((current) =>
                  current?.id === line.id ? null : { source: 'tray', id: line.id },
                )
              }
            >
              <code>{line.text}</code>
            </button>
          ))
        )}
      </div>
      <ol className="path-stack" aria-label="Pseudocode path">
        {lines.map((line, index) => {
          const expected = guide[index] || line
          const verb = verbOf(expected.text)
          const role = uniqueRole(expected.text, index, roleByVerb, usedTitles)
          const item = path[index]
          const hanging = verb === 'IF' && !isLoopUpdate(expected.text)
          const nested = Boolean(item?.indent)
          const misplaced = Boolean(
            showErrors &&
              item &&
              (item.id !== expected.id ||
                (verb === 'IF' && !isLoopUpdate(expected.text)
                ? !item.indent
                : Boolean(item.indent))),
          )
          const note = misplaced
            ? notes.find((entry) => entry.id === item.id)
            : null
          return (
            <li
              key={`station-${expected.id || index}`}
              className={
                misplaced
                  ? nested
                    ? 'path-step is-nested is-wrong'
                    : 'path-step is-wrong'
                  : nested
                    ? 'path-step is-nested'
                    : 'path-step'
              }
            >
              {index > 0 && <span className="path-arrow" aria-hidden="true" />}
              <div className="path-step-head">
                <span className="path-step-num">{index + 1}</span>
                <div>
                  <p className="path-step-title">{role.title}</p>
                  <p className="path-step-why">{role.why}</p>
                </div>
              </div>
              <div
                className={
                  item
                    ? misplaced
                      ? nested
                        ? 'path-drop filled nested wrong'
                        : 'path-drop filled wrong'
                      : nested
                        ? 'path-drop filled nested'
                        : 'path-drop filled'
                    : dragFrom || picked
                      ? 'path-drop open hot'
                      : 'path-drop open'
                }
                onDragOver={(event) => {
                  event.preventDefault()
                  event.dataTransfer.dropEffect = 'move'
                }}
                onDrop={(event) => {
                  event.preventDefault()
                  const next = itemFrom(payload(event))
                  if (!next) return
                  placeOn(
                    index,
                    next,
                    verbOf(next.text) === 'IF' ? 1 : isLoopUpdate(next.text) ? 2 : next.indent || 0,
                  )
                }}
                onClick={() => {
                  if (item || !picked) return
                  const next = itemFrom(picked)
                  if (!next) return
                  placeOn(
                    index,
                    next,
                    verbOf(next.text) === 'IF' ? 1 : isLoopUpdate(next.text) ? 2 : 0,
                  )
                }}
              >
                {item ? (
                  <div
                    className={nested ? 'path-block on-path hung' : 'path-block on-path'}
                    draggable
                    onDragStart={(event) => startDrag(event, 'path', item.id)}
                  >
                    {nested && (
                      <p className="path-nested-label">Sits under the step above</p>
                    )}
                    <code>{item.text}</code>
                    {note?.why && <p className="path-order-why">{note.why}</p>}
                    <div className="path-tools">
                      <div className="path-indent" role="group" aria-label="Indent">
                        <button
                          type="button"
                          className={
                            !item.indent ? 'path-indent-btn selected' : 'path-indent-btn'
                          }
                          onClick={(event) => {
                            event.stopPropagation()
                            setIndent(index, 0)
                          }}
                        >
                          Lined up
                        </button>
                        <button
                          type="button"
                          className={
                            item.indent ? 'path-indent-btn selected' : 'path-indent-btn'
                          }
                          onClick={(event) => {
                            event.stopPropagation()
                            setIndent(index, 1)
                          }}
                        >
                          Sits under
                        </button>
                      </div>
                      <button
                        type="button"
                        className="path-remove"
                        onClick={(event) => {
                          event.stopPropagation()
                          returnToTray(index)
                        }}
                      >
                        Remove
                      </button>
                    </div>
                  </div>
                ) : (
                  <p className="path-drop-hint">
                    {picked
                      ? 'Drop or click here to place the block'
                      : 'Drag a block here, or click one on the tray first'}
                  </p>
                )}
              </div>
            </li>
          )
        })}
      </ol>
    </div>
  )
}

function verbOf(text) {
  return (text || '').trim().split(/\s+/)[0].toUpperCase()
}

function isLoopUpdate(text) {
  return /^SET\s+\S+\s+to\s+(this\b|count \+ 1)/i.test((text || '').trim())
}

function uniqueRole(text, index, roleByVerb, usedTitles) {
  const verb = verbOf(text)
  const picked = isLoopUpdate(text)
    ? FALLBACK_ROLES.UPDATE
    : FALLBACK_ROLES[verb] ||
      roleByVerb[verb] || {
        title: `Step ${index + 1}`,
        why: 'Put the matching block here.',
      }
  if (!usedTitles.has(picked.title)) {
    usedTitles.add(picked.title)
    return picked
  }
  if (picked.title === FALLBACK_ROLES.IF.title || verb === 'SET' || verb === 'IF') {
    const update = FALLBACK_ROLES.UPDATE
    if (!usedTitles.has(update.title)) {
      usedTitles.add(update.title)
      return update
    }
  }
  const title = `Step ${index + 1}`
  usedTitles.add(title)
  return { title, why: picked.why }
}
