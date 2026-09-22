# CodeSense prototype — question to plan

A student pastes one console-procedure question, answers the decisions, and gets pseudocode from the model. Wrong answers are graded (three tries, then the correct decision). The last step is a shuffled Parsons puzzle: restore order and indent.

This prototype does **not** run a program, record a trace, or explain a run.

## Run on your computer

**Needs:** [Java 21+](https://adoptium.net/) and [Node.js 20+](https://nodejs.org/)

```bash
cd prototype/backend
mvn spring-boot:run
```

```bash
cd prototype/frontend
npm install
npm run dev
```

Open **http://127.0.0.1:43124**

| Service | URL |
|---------|-----|
| Workshop | http://127.0.0.1:43124 |
| API | http://127.0.0.1:8090 |

### Docker

```bash
cd prototype
docker compose up --build
```

Same URLs. The UI is served on port 43124.

## What to demo

1. Paste `Find the largest number in a list.`
2. Answer: a list comes in, the biggest number comes out → confirm the lines.
3. Strategy: look at every number.
4. Memory: the biggest so far. If asked where it starts, say the first number.
5. Stop: when the list ends, give back max.
6. The lines appear shuffled. Put them in order, indent the update under the loop, then check.
7. Paste `Write a SQL query to list students in CS101.` — the page refuses it.

`Compute the area of a rectangle given length and width` skips memory and stop after “compute once.”

A wrong decision is marked wrong. Three misses show the correct answer and a short explanation.

**I don't know** asks a narrower question. It does not use up a try and does not fill in the algorithm.

## How the questions are produced

The first question is fixed: *What comes in, and what must come out?*

Later questions are written by the connected model from the lab question and the answers so far. A checker drops any question that already contains a step (`SET`, `FOR`, `IF`, a named algorithm, or an assignment). That turn falls back to the next fixed prompt: strategy, memory, or stop.

A line of pseudocode is written by the model from the lab question and the decision the student just made. The first two lines are always **INPUT** and **OUTPUT**.

## LLM

The API is **Spring Boot** (Java 21). The prototype uses a local [Ollama](https://ollama.com) model when one is running. On this machine that is **qwen3:8b**.

The first local answer can take a minute while the model loads. If Ollama is stopped, the rule engine still runs the demo so the page does not die.

| Variable | Effect |
|----------|--------|
| `OLLAMA_BASE_URL` | Default `http://127.0.0.1:11434` |
| `OLLAMA_MODEL` | Default `qwen3:8b` if that model is installed |
| `GEMINI_API_KEY` | Google Gemini |
| `GROQ_API_KEY` | Groq |
| `OPENAI_API_KEY` | OpenAI-compatible API |
| `OPENAI_BASE_URL` | Default `https://api.openai.com/v1` |

Copy `backend/.env.example` if you want a local file. Keys stay on the server.

## Tests

```bash
cd prototype/backend
mvn test
```

## Out of scope here

Program runner, JSONL trace, playback, narration, accounts, sandbox. Those start at installment 1 and 2.
