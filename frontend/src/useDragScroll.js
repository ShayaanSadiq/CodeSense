import { useEffect } from 'react'

const EDGE = 92
const MIN_SPEED = 8
const MAX_SPEED = 28

function canScrollY(node) {
  if (!node || node === document.body || node === document.documentElement) {
    return false
  }
  const style = window.getComputedStyle(node)
  if (!/(auto|scroll|overlay)/.test(style.overflowY)) return false
  return node.scrollHeight > node.clientHeight + 1
}

function collectScrollers(start) {
  const found = []
  const seen = new Set()

  function add(node) {
    if (!node || seen.has(node)) return
    seen.add(node)
    if (canScrollY(node)) found.push(node)
  }

  let node = start
  while (node && node !== document.documentElement) {
    add(node)
    node = node.parentElement
  }

  if (start?.closest) {
    add(start.closest('.path-workspace'))
    add(start.closest('.flow-workspace'))
    add(start.closest('.parsons'))
    add(start.closest('main'))
  }

  const main = document.querySelector('main.main, main')
  add(main)
  return found
}

function realPoint(event) {
  return Boolean(event.clientX || event.clientY || event.screenX || event.screenY)
}

function edgeDelta(y, top, bottom) {
  const fromTop = y - top
  const fromBottom = bottom - y
  if (fromTop < EDGE && fromTop <= fromBottom) {
    const t = Math.max(0, Math.min(1, (EDGE - fromTop) / EDGE))
    return -(MIN_SPEED + t * t * (MAX_SPEED - MIN_SPEED))
  }
  if (fromBottom < EDGE) {
    const t = Math.max(0, Math.min(1, (EDGE - fromBottom) / EDGE))
    return MIN_SPEED + t * t * (MAX_SPEED - MIN_SPEED)
  }
  return 0
}

function applyScroll(dy, workspace) {
  if (!dy) return
  const targets = collectScrollers(workspace)
  if (targets.length === 0) {
    const root = document.scrollingElement
    if (root && root.scrollHeight > root.clientHeight + 1) {
      root.scrollTop += dy
    } else {
      window.scrollBy(0, dy)
    }
    return
  }
  for (const node of targets) {
    const prev = node.scrollTop
    const max = node.scrollHeight - node.clientHeight
    node.scrollTop = Math.max(0, Math.min(max, prev + dy))
    if (Math.abs(node.scrollTop - prev) >= 0.5) return
  }
}

function deltaForPointer(y, workspace) {
  let dy = edgeDelta(y, 0, window.innerHeight)
  for (const node of collectScrollers(workspace)) {
    const rect = node.getBoundingClientRect()
    const next = edgeDelta(y, rect.top, rect.bottom)
    if (Math.abs(next) > Math.abs(dy)) dy = next
  }
  return dy
}

function fromWorkspace(event, workspace) {
  if (workspace) return workspace.contains(event.target)
  return Boolean(
    event.target?.closest?.('.path-workspace, .flow-workspace, .parsons'),
  )
}

export function useDragScroll(workspaceRef) {
  useEffect(() => {
    let dragging = false
    let pointerY = 0
    let tickId = 0
    let lastScroll = 0

    function workspace() {
      return workspaceRef?.current || null
    }

    function stopTick() {
      if (tickId) {
        window.cancelAnimationFrame(tickId)
        tickId = 0
      }
    }

    function scrollToPointer() {
      lastScroll = performance.now()
      applyScroll(deltaForPointer(pointerY, workspace()), workspace())
    }

    function tick() {
      if (!dragging) {
        tickId = 0
        return
      }
      if (performance.now() - lastScroll > 12) scrollToPointer()
      tickId = window.requestAnimationFrame(tick)
    }

    function begin(y) {
      pointerY = y
      dragging = true
      if (!tickId) tickId = window.requestAnimationFrame(tick)
    }

    function end() {
      dragging = false
      stopTick()
    }

    function onDragStart(event) {
      if (!fromWorkspace(event, workspace())) return
      begin(event.clientY)
    }

    function onDragEnter(event) {
      if (!dragging) return
      event.preventDefault()
    }

    function onDragOver(event) {
      if (!dragging) return
      event.preventDefault()
      if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
      if (realPoint(event)) pointerY = event.clientY
      scrollToPointer()
    }

    function onDrag(event) {
      if (!dragging) return
      if (realPoint(event)) pointerY = event.clientY
    }

    function onWheel(event) {
      if (!dragging) return
      event.preventDefault()
      if (realPoint(event)) pointerY = event.clientY
      applyScroll(event.deltaY, workspace())
    }

    function onTouchMove(event) {
      if (!dragging) return
      const y = event.touches?.[0]?.clientY
      if (typeof y !== 'number') return
      pointerY = y
      applyScroll(deltaForPointer(pointerY, workspace()), workspace())
    }

    document.addEventListener('dragstart', onDragStart, true)
    document.addEventListener('dragenter', onDragEnter, true)
    document.addEventListener('dragover', onDragOver, true)
    document.addEventListener('drag', onDrag, true)
    document.addEventListener('dragend', end, true)
    document.addEventListener('drop', end, true)
    document.addEventListener('wheel', onWheel, { capture: true, passive: false })
    document.addEventListener('touchmove', onTouchMove, { capture: true, passive: true })

    return () => {
      end()
      document.removeEventListener('dragstart', onDragStart, true)
      document.removeEventListener('dragenter', onDragEnter, true)
      document.removeEventListener('dragover', onDragOver, true)
      document.removeEventListener('drag', onDrag, true)
      document.removeEventListener('dragend', end, true)
      document.removeEventListener('drop', end, true)
      document.removeEventListener('wheel', onWheel, true)
      document.removeEventListener('touchmove', onTouchMove, true)
    }
  }, [workspaceRef])
}
