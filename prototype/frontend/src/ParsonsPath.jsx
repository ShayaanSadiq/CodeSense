import { useEffect, useMemo, useState } from 'react'

const WIDTH = 640
const HEIGHT = 260

function stations(count) {
  if (count < 1) return []
  return Array.from({ length: count }, (_, index) => {
    const t = count === 1 ? 0.5 : index / (count - 1)
    const x = 48 + t * 544
    const y = 92 + Math.sin(t * Math.PI * 1.15) * 46
    return { x, y }
  })
}

function pathD(points) {
  if (points.length === 0) return ''
  if (points.length === 1) return `M ${points[0].x} ${points[0].y}`
  let d = `M ${points[0].x} ${points[0].y}`
  for (let i = 1; i < points.length; i += 1) {
    const prev = points[i - 1]
    const cur = points[i]
    const cpx = (prev.x + cur.x) / 2
    d += ` Q ${cpx} ${prev.y + (i % 2 === 0 ? -18 : 18)} ${cur.x} ${cur.y}`
  }
  return d
}

export function ParsonsPath({ lines, onChange }) {
  const [tray, setTray] = useState(lines)
  const [path, setPath] = useState([])
  const [dragFrom, setDragFrom] = useState(null)

  const pieceKey = [...lines.map((line) => line.id)].sort().join(',')
  useEffect(() => {
    setTray(lines.map((line) => ({ ...line, indent: 0 })))
    setPath([])
  }, [pieceKey])

  const points = useMemo(() => stations(Math.max(lines.length, 1)), [lines.length])
  const curve = useMemo(() => pathD(points), [points])

  function emit(nextPath) {
    if (nextPath.length === lines.length) {
      onChange(nextPath)
    }
  }

  function placeOn(slot, item, indent) {
    const nextPath = path.slice()
    const nextTray = tray.filter((line) => line.id !== item.id)
    const existing = nextPath[slot]
    if (existing) {
      nextTray.push(existing)
    }
    nextPath[slot] = { ...item, indent }
    const compact = []
    const holes = []
    nextPath.forEach((line, index) => {
      if (line) compact.push(line)
      else holes.push(index)
    })
    while (compact.length < lines.length) {
      compact.push(null)
    }
    setTray(nextTray)
    setPath(compact)
    emit(compact.filter(Boolean))
    setDragFrom(null)
  }

  function returnToTray(index) {
    const item = path[index]
    if (!item) return
    const nextPath = path.slice()
    nextPath[index] = null
    setPath(nextPath)
    setTray([...tray, { ...item, indent: 0 }])
    setDragFrom(null)
  }

  function setIndent(index, indent) {
    const item = path[index]
    if (!item) return
    const nextPath = path.slice()
    nextPath[index] = { ...item, indent: Math.max(0, Math.min(2, indent)) }
    setPath(nextPath)
    emit(nextPath.filter(Boolean))
  }

  function payload(event) {
    try {
      return JSON.parse(event.dataTransfer.getData('application/json'))
    } catch {
      return null
    }
  }

  return (
    <div className="path-workspace">
      <div className="path-tray" aria-label="Unused blocks">
        {tray.length === 0 ? (
          <p className="path-tray-empty">All blocks are on the path.</p>
        ) : (
          tray.map((line) => (
            <button
              key={line.id}
              type="button"
              className="path-block"
              draggable
              onDragStart={(event) => {
                event.dataTransfer.setData(
                  'application/json',
                  JSON.stringify({ source: 'tray', id: line.id }),
                )
                event.dataTransfer.effectAllowed = 'move'
                setDragFrom({ source: 'tray', id: line.id })
              }}
            >
              <code>{line.text}</code>
            </button>
          ))
        )}
      </div>
      <div className="path-canvas" aria-label="Pseudocode path">
        <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} className="path-svg">
          <path d={curve} className="path-curve" fill="none" />
          {points.map((point, index) => (
            <g key={`node-${index}`}>
              <line
                x1={point.x}
                y1={point.y}
                x2={point.x}
                y2={point.y + 52}
                className="path-hang"
              />
              <circle cx={point.x} cy={point.y} r="5" className="path-tick" />
              {!path[index] && (
                <text x={point.x} y={point.y - 10} className="path-x">
                  ×
                </text>
              )}
            </g>
          ))}
        </svg>
        {points.map((point, index) => {
          const item = path[index]
          const left = `${(point.x / WIDTH) * 100}%`
          const top = item?.indent
            ? `${((point.y + 48) / HEIGHT) * 100}%`
            : `${(point.y / HEIGHT) * 100}%`
          return (
            <div
              key={`slot-${index}`}
              className={
                item
                  ? 'path-station filled'
                  : dragFrom
                    ? 'path-station open hot'
                    : 'path-station open'
              }
              style={{ left, top }}
              onDragOver={(event) => {
                event.preventDefault()
                event.dataTransfer.dropEffect = 'move'
              }}
              onDrop={(event) => {
                event.preventDefault()
                const data = payload(event)
                const fromTray = tray.find((line) => line.id === data?.id)
                const fromPath = path.find((line) => line?.id === data?.id)
                const itemToPlace = fromTray || fromPath
                if (!itemToPlace) return
                if (fromPath) {
                  const currentIndex = path.findIndex((line) => line?.id === itemToPlace.id)
                  if (currentIndex >= 0) {
                    const cleared = path.slice()
                    cleared[currentIndex] = null
                    setPath(cleared)
                  }
                }
                placeOn(index, itemToPlace, itemToPlace.indent || 0)
              }}
            >
              {item ? (
                <div
                  className="path-block on-path"
                  draggable
                  onDragStart={(event) => {
                    event.dataTransfer.setData(
                      'application/json',
                      JSON.stringify({ source: 'path', id: item.id }),
                    )
                    setDragFrom({ source: 'path', id: item.id })
                  }}
                >
                  <code>{item.text}</code>
                  <div className="path-indent" role="group" aria-label="Indent">
                    {[0, 1].map((step) => (
                      <button
                        key={step}
                        type="button"
                        className={
                          item.indent === step
                            ? 'parsons-step selected'
                            : 'parsons-step'
                        }
                        aria-label={step === 0 ? 'On the path' : 'Hang under the path'}
                        onClick={() => setIndent(index, step)}
                      />
                    ))}
                  </div>
                  <button
                    type="button"
                    className="path-return"
                    aria-label="Return block to tray"
                    onClick={() => returnToTray(index)}
                  >
                    ×
                  </button>
                </div>
              ) : (
                <span className="path-slot-label">{index + 1}</span>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
