<p align="center">
  <img src="frontend/public/logo.png" alt="CodeSense" width="220" />
</p>

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
2. Input first: a list comes in.
3. Strategy: look at every number.
4. Memory: the biggest so far. If asked where it starts, say the first number.
5. Stop: when the list ends.
6. Output last: the biggest number comes out.
7. The lines appear shuffled. Put INPUT first and OUTPUT last, indent the update under the loop, then check.
8. Paste `Write a SQL query to list students in CS101.` — the page refuses it.

`Compute the area of a rectangle given length and width` skips memory and stop after “compute once,” then still asks for the output.

A wrong decision is marked wrong. Three misses show the correct answer and a short explanation.

**I don't know** asks a narrower question. It does not use up a try and does not fill in the algorithm.

## How the questions are produced

The Socratic method asks input first and output last. The **Pseudocode translator** marks trigger words, maps them to constructs, assembles the plan, then opens the same path-style Parsons puzzle (unused blocks on a tray, snap them onto the flow path).

The first Socratic question is fixed: *Name what data the program is given.* The last Socratic question is the output.

Later questions are written by the connected model from the lab question and the answers so far. A checker drops any question that already contains a step (`SET`, `FOR`, `IF`, a named algorithm, or an assignment). That turn falls back to the next fixed prompt: strategy, memory, stop, or output.

A line of pseudocode is written by the model from the lab question and the decision the student just made. The plan starts with **INPUT** and ends with **OUTPUT**.

## LLM

The API is **Spring Boot** (Java 21). Every model call goes through **Spring AI** `ChatClient`. The default local model is [Ollama](https://ollama.com) **qwen3:8b**.

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

## Later installments

2. Pseudocode translator — glossary, mark trigger words, assemble the plan  
3. Flowchart-piece puzzle (mid-project review)  
4. Python convert / Pyodide run / step predict  
5. Java convert / run / predict plus accounts  
6. C convert / run / predict, dashboard, streaks (final review)

This folder does not run a student program or keep accounts. That starts at installment 4.
