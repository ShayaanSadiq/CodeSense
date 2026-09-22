export type Slot = 'io' | 'strategy' | 'memory' | 'stop'
export type Phase = 'ask' | 'confirm' | 'arrange' | 'refused' | 'done'
export type LlmMode = 'openai' | 'ollama' | 'gemini' | 'groq' | 'rules'
export type LlmProvider = 'ollama' | 'openai' | 'gemini' | 'groq'

export interface PseudoLine {
  id: string
  text: string
  indent: number
}

export interface Session {
  id: string
  labQuestion: string
  phase: Phase
  slot: Slot
  currentQuestion: string
  proposedLines: PseudoLine[]
  puzzleLines?: PseudoLine[]
  hasRepetition: boolean | null
  usedFallbackQuestion: boolean
  refusal: string | null
  llmMode: LlmMode
  wrongAttempts?: number
  maxAttempts?: number
  verdict?: 'ok' | 'wrong' | 'revealed' | null
  feedback?: string | null
  explanation?: string | null
  correctAnswer?: string | null
}

export interface Health {
  ok: boolean
  scope: string
  llm: LlmMode
  mode: LlmMode
  provider: LlmProvider | 'rules'
  model: string
  ready: boolean
  error: string | null
  hasKey: boolean
  ollamaUp: boolean
  models: string[]
}
