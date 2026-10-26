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

const TOKEN_KEY = 'codesense-token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

function headers(json = false) {
  const out = {}
  if (json) out['Content-Type'] = 'application/json'
  const token = getToken()
  if (token) out.Authorization = `Bearer ${token}`
  return out
}

export function fetchHealth() {
  return fetch('/api/health').then((res) => read(res))
}

export function saveLlm(body) {
  return fetch('/api/llm', {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(body),
  }).then((res) => read(res))
}

export function getWorkshop(id) {
  return fetch(`/api/workshop/${id}`, { headers: headers() }).then((res) => read(res))
}

export function fetchGlossary() {
  return fetch('/api/workshop/glossary').then((res) => read(res))
}

export function rephraseQuestion(question) {
  return fetch(
    `/api/workshop/rephrase?question=${encodeURIComponent(question)}`,
  ).then((res) => read(res))
}

export function fetchRoles() {
  return fetch('/api/workshop/roles').then((res) => read(res))
}

export function translateWorkshop(question, marks, kind = 'path') {
  return fetch('/api/workshop/translate', {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify({ question, marks, kind }),
  }).then((res) => read(res))
}

export function startWorkshop(question, kind = 'path') {
  return fetch('/api/workshop', {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify({ question, kind }),
  }).then((res) => read(res))
}

export function answerWorkshop(id, answer, dontKnow = false) {
  return fetch(`/api/workshop/${id}/answer`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify({ answer, dontKnow }),
  }).then((res) => read(res))
}

export function confirmWorkshop(id, lines) {
  return fetch(`/api/workshop/${id}/confirm`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify({ lines }),
  }).then((res) => read(res))
}

export function arrangeWorkshop(id, lines) {
  return fetch(`/api/workshop/${id}/arrange`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify({ lines }),
  }).then((res) => read(res))
}

export function convertPython(id, input) {
  return convertCode(id, input, 'python')
}

export function convertCode(id, input, language = 'python') {
  return fetch(`/api/workshop/${id}/code`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify({ input, language }),
  }).then((res) => read(res))
}

export function predictPython(id, answer, dontKnow = false) {
  return fetch(`/api/workshop/${id}/predict`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify({ answer, dontKnow }),
  }).then((res) => read(res))
}

export function runWorkshop(id, stdin = '', source = '') {
  return fetch(`/api/workshop/${id}/run`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify({ stdin, source }),
  }).then((res) => read(res))
}

export function signup(name, username, password) {
  return fetch('/api/auth/signup', {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify({ name, username, password }),
  }).then((res) => read(res))
}

export function login(username, password) {
  return fetch('/api/auth/login', {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify({ username, password }),
  }).then((res) => read(res))
}

export function fetchMe() {
  return fetch('/api/auth/me', { headers: headers() }).then((res) => read(res))
}

export function logout() {
  return fetch('/api/auth/logout', {
    method: 'POST',
    headers: headers(),
  }).then((res) => read(res))
}
