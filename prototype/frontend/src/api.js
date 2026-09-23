async function read(res) {
  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const body = await res.json()
      if (body.error) message = body.error
    } catch {
      /* keep default */
    }
    throw new Error(message)
  }
  return res.json()
}

export function fetchHealth() {
  return fetch('/api/health').then((res) => read(res))
}

export function saveLlm(body) {
  return fetch('/api/llm', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then((res) => read(res))
}

export function getWorkshop(id) {
  return fetch(`/api/workshop/${id}`).then((res) => read(res))
}

export function startWorkshop(question) {
  return fetch('/api/workshop', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question }),
  }).then((res) => read(res))
}

export function answerWorkshop(id, answer, dontKnow = false) {
  return fetch(`/api/workshop/${id}/answer`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ answer, dontKnow }),
  }).then((res) => read(res))
}

export function confirmWorkshop(id, lines) {
  return fetch(`/api/workshop/${id}/confirm`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ lines }),
  }).then((res) => read(res))
}

export function arrangeWorkshop(id, lines) {
  return fetch(`/api/workshop/${id}/arrange`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ lines }),
  }).then((res) => read(res))
}
