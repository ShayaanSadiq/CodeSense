import { useEffect, useRef, useState } from 'react'
import { useDragScroll } from './useDragScroll.js'

const SHAPE = {
  INPUT: 'io',
  OUTPUT: 'io',
  IF: 'diamond',
}

export function FlowchartJoin({ lines, guide = [], notes = [], onChange, showErrors = false }) {
  const [tray, setTray] = useState(lines)
  const [path, setPath] = useState([])
  const [dragFrom, setDragFrom] = useState(null)
  const [picked, setPicked] = useState(null)
  const dragRef = useRef(null)
  const ghostRef = useRef(null)
  const workspaceRef = useRef(null)
  useDragScroll(workspaceRef)

  const pieceKey = [...lines.map((line) => line.id)].sort().join(',')
  useEffect(() => {
    setTray(lines.map((line) => ({ ...line, indent: 0 })))
    setPath([])
    setPicked(null)
  }, [pieceKey])

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
    const raw =
      event.dataTransfer.getData('application/json') ||
      event.dataTransfer.getData('text/plain')
    try {
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  function droppedItem(event) {
    return itemFrom(payload(event) || dragRef.current || dragFrom)
  }

  function allowDrop(event) {
    event.preventDefault()
    event.stopPropagation()
    if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
  }

  function startDrag(event, source, id) {
    const next = { source, id }
    dragRef.current = next
    setDragFrom(next)
    const packed = JSON.stringify(next)
    event.dataTransfer.setData('application/json', packed)
    event.dataTransfer.setData('text/plain', packed)
    event.dataTransfer.effectAllowed = 'move'
    attachDragGhost(event)
  }

  function attachDragGhost(event) {
    clearDragGhost()
    const piece = event.currentTarget
    if (!(piece instanceof HTMLElement)) return
    const rect = piece.getBoundingClientRect()
    const canvas = paintPieceGhost(piece, rect)
    canvas.className = 'flow-drag-ghost'
    document.body.appendChild(canvas)
    ghostRef.current = canvas
    event.dataTransfer.setDragImage(
      canvas,
      event.clientX - rect.left,
      event.clientY - rect.top,
    )
  }

  function clearDragGhost() {
    ghostRef.current?.remove()
    ghostRef.current = null
  }

  function endDrag() {
    dragRef.current = null
    setDragFrom(null)
    window.setTimeout(clearDragGhost, 0)
  }

  return (
    <div className="flow-workspace" ref={workspaceRef}>
      <div className="flow-tray" aria-label="Unused flowchart pieces">
        {tray.length === 0 ? (
          <p className="path-tray-empty">
            Every piece is joined. Remove one if you need to move it.
          </p>
        ) : (
          tray.map((line) => {
            const shape = shapeOf(line.text)
            return (
              <div
                key={line.id}
                role="button"
                tabIndex={0}
                className={
                  picked?.id === line.id
                    ? `flow-piece ${shape} picked`
                    : `flow-piece ${shape}`
                }
                draggable
                onDragStart={(event) => startDrag(event, 'tray', line.id)}
                onDragEnd={endDrag}
                onClick={() =>
                  setPicked((current) =>
                    current?.id === line.id ? null : { source: 'tray', id: line.id },
                  )
                }
              >
                <span className="flow-piece-inner">
                  <code>{line.text}</code>
                </span>
              </div>
            )
          })
        )}
      </div>
      <ol className="flow-stack" aria-label="Flowchart">
        {lines.map((line, index) => {
          const expected = guide[index] || line
          const item = path[index]
          const hanging = item ? isDecision(item.text) : false
          const nested = hanging && Boolean(item?.indent)
          const misplaced = Boolean(
            showErrors &&
              item &&
              (item.id !== expected.id || indentWrong(item, expected)),
          )
          const note = misplaced ? notes.find((entry) => entry.id === item.id) : null
          const placedShape = item ? shapeOf(item.text) : 'slot'
          return (
            <li
              key={`flow-${expected.id || index}`}
              className={misplaced ? 'flow-station is-wrong' : 'flow-station'}
            >
              {index > 0 && <span className="flow-join" aria-hidden="true" />}
              <div className="flow-station-head">
                <p className="flow-station-title">Step {index + 1}</p>
                <p className="flow-station-why">Drop the piece that belongs here.</p>
              </div>
              <div className={item ? `flow-drop is-${placedShape}` : 'flow-drop'}>
                {hanging && nested && <p className="flow-yes">Yes</p>}
                <div
                  className={
                    item
                      ? misplaced
                        ? `flow-socket filled ${placedShape} wrong`
                        : nested
                          ? `flow-socket filled ${placedShape} nested`
                          : `flow-socket filled ${placedShape}`
                      : dragFrom || picked
                        ? 'flow-socket open slot hot'
                        : 'flow-socket open slot'
                  }
                  onDragEnter={allowDrop}
                  onDragOver={allowDrop}
                  onDrop={(event) => {
                    allowDrop(event)
                    const next = droppedItem(event)
                    if (!next) return
                    placeOn(index, next, defaultIndent(next.text) || next.indent || 0)
                    endDrag()
                  }}
                  onClick={() => {
                    if (!picked) return
                    const next = itemFrom(picked)
                    if (!next) return
                    placeOn(index, next, defaultIndent(next.text))
                  }}
                >
                  {item ? (
                    <div
                      className={`flow-piece on-flow ${placedShape}`}
                      draggable
                      onDragStart={(event) => startDrag(event, 'path', item.id)}
                      onDragEnd={endDrag}
                    >
                      <span className="flow-piece-inner">
                        <code>{item.text}</code>
                        {note?.why && <p className="path-order-why">{note.why}</p>}
                      </span>
                    </div>
                  ) : (
                    <p className="flow-socket-hint">
                      {picked || dragFrom
                        ? 'Drop or click here to place the piece'
                        : 'Drop a piece here'}
                    </p>
                  )}
                </div>
                {item && (
                  <div className="path-tools flow-tools">
                    {hanging && (
                      <div className="path-indent" role="group" aria-label="Join">
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
                          Main line
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
                          Yes arm
                        </button>
                      </div>
                    )}
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

function shapeOf(text) {
  return SHAPE[verbOf(text)] || 'box'
}

function isLoopUpdate(text) {
  return /^SET\s+\S+\s+to\s+this\b/i.test((text || '').trim())
}

function isInlineIf(text) {
  const value = (text || '').trim()
  if (verbOf(value) !== 'IF') return false
  return /,\s*SET\b/i.test(value)
}

function isDecision(text) {
  return verbOf(text) === 'IF' && !isInlineIf(text)
}

function defaultIndent(text) {
  if (isDecision(text)) return 1
  if (isLoopUpdate(text)) return 2
  return 0
}

function indentWrong(item, expected) {
  const need = defaultIndent(expected.text)
  if (need > 0) return !item.indent
  return Boolean(item.indent)
}

function paintPieceGhost(el, rect) {
  const dpr = Math.max(1, window.devicePixelRatio || 1)
  const width = Math.max(1, Math.round(rect.width))
  const height = Math.max(1, Math.round(rect.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(width * dpr)
  canvas.height = Math.round(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  const ctx = canvas.getContext('2d')
  ctx.scale(dpr, dpr)
  const inner = el.querySelector('.flow-piece-inner')
  const fill = inner ? getComputedStyle(inner).backgroundColor : getComputedStyle(el).backgroundColor
  const stroke = getComputedStyle(el).getPropertyValue('--green-dark').trim() || '#17b169'
  const color = inner ? getComputedStyle(inner).color : getComputedStyle(el).color
  const font = inner ? getComputedStyle(inner).font : getComputedStyle(el).font
  ctx.beginPath()
  if (el.classList.contains('diamond')) {
    ctx.moveTo(width / 2, 2)
    ctx.lineTo(width - 2, height / 2)
    ctx.lineTo(width / 2, height - 2)
    ctx.lineTo(2, height / 2)
  } else if (el.classList.contains('io')) {
    const cut = width * 0.16
    ctx.moveTo(cut, 2)
    ctx.lineTo(width - 2, 2)
    ctx.lineTo(width - cut, height - 2)
    ctx.lineTo(2, height - 2)
  } else {
    const r = 4
    ctx.moveTo(r, 1.5)
    ctx.arcTo(width - 1.5, 1.5, width - 1.5, height - 1.5, r)
    ctx.arcTo(width - 1.5, height - 1.5, 1.5, height - 1.5, r)
    ctx.arcTo(1.5, height - 1.5, 1.5, 1.5, r)
    ctx.arcTo(1.5, 1.5, width - 1.5, 1.5, r)
  }
  ctx.closePath()
  ctx.fillStyle = fill || '#0c0c0c'
  ctx.fill()
  ctx.lineWidth = 2.5
  ctx.strokeStyle = stroke
  ctx.stroke()
  ctx.fillStyle = color || '#f3f6f4'
  ctx.font = font || '13px "Segoe UI", system-ui, sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  const maxWidth = el.classList.contains('diamond') ? width * 0.52 : width * 0.78
  wrapGhostText(ctx, (el.textContent || '').replace(/\s+/g, ' ').trim(), width / 2, height / 2, maxWidth, 16)
  return canvas
}

function wrapGhostText(ctx, text, x, y, maxWidth, lineHeight) {
  const words = text.split(' ')
  const lines = []
  let current = ''
  for (const word of words) {
    const next = current ? `${current} ${word}` : word
    if (current && ctx.measureText(next).width > maxWidth) {
      lines.push(current)
      current = word
    } else {
      current = next
    }
  }
  if (current) lines.push(current)
  const start = y - ((lines.length - 1) * lineHeight) / 2
  lines.forEach((line, index) => {
    ctx.fillText(line, x, start + index * lineHeight, maxWidth)
  })
}
