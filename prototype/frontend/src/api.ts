import type { Health, LlmProvider, PseudoLine, Session } from './types.ts'

async function read<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const body = (await res.json()) as { error?: string }
      if (body.error) message = body.error
    } catch {
      /* keep default */
    }
    throw new Error(message)
  }
  return (await res.json()) as T
}

export function fetchHealth(): Promise<Health> {
  return fetch('/api/health').then((res) => read<Health>(res))
}

export function saveLlm(body: {
  provider: LlmProvider
  model: string
  apiKey?: string
  baseUrl?: string
}): Promise<Health> {
  return fetch('/api/llm', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }).then((res) => read<Health>(res))
}

export function getWorkshop(id: string): Promise<Session> {
  return fetch(`/api/workshop/${id}`).then((res) => read<Session>(res))
}

export function startWorkshop(question: string): Promise<Session> {
  return fetch('/api/workshop', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question }),
  }).then((res) => read<Session>(res))
}

export function answerWorkshop(
  id: string,
  answer: string,
  dontKnow = false,
): Promise<Session> {
  return fetch(`/api/workshop/${id}/answer`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ answer, dontKnow }),
  }).then((res) => read<Session>(res))
}

export function confirmWorkshop(
  id: string,
  lines: PseudoLine[],
): Promise<Session> {
  return fetch(`/api/workshop/${id}/confirm`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ lines }),
  }).then((res) => read<Session>(res))
}

export function arrangeWorkshop(
  id: string,
  lines: PseudoLine[],
): Promise<Session> {
  return fetch(`/api/workshop/${id}/arrange`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ lines }),
  }).then((res) => read<Session>(res))
}
