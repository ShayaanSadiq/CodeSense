# CodeSense

Interactive tutor that turns a lab question into pseudocode the student builds decision by decision, then later runs their Python, Java, or C program and explains that run against the plan.

**This folder’s working software is the prototype:** question to pseudocode only. No program runner, trace, narration, accounts, or sandbox yet.

## Run the prototype

You need [Java 21+](https://adoptium.net/) and [Node.js 20+](https://nodejs.org/) (or [Docker Desktop](https://www.docker.com/products/docker-desktop/)).

```bash
cd prototype/frontend
npm install
```

Then two terminals:

```bash
cd prototype/backend
mvn spring-boot:run
```

```bash
cd prototype/frontend
npm run dev
```

Open **http://127.0.0.1:43124**

Docker instead:

```bash
cd prototype
docker compose up --build
```

Details: [prototype/README.md](prototype/README.md)

## What the prototype does

1. Paste one console-procedure question.
2. Answer four decisions, one at a time: inputs and output, strategy, memory, stop. Memory and stop are skipped when nothing repeats. A wrong answer gets two more tries; the third miss shows the correct decision and why.
3. Confirm or rewrite each pseudocode line. The model writes INPUT, OUTPUT, and the rest of the plan from those decisions.
4. Solve a Parsons puzzle: the lines are shuffled, you restore order and indent, then check.

A theory, class-design, SQL, webpage, debugging, multi-procedure, or off-scope question is refused in one sentence.

## Roadmap

The prototype is its own deliverable. Ten installments follow. **Installment 5** is the mid-project review. **Installment 10** is the final review.

1. App shell — workshop in the real site; a session saves the question and the lines
2. Python runner — paste a console program, dump a JSONL trace
3. Playback — step through line, stack, and values. No model yet
4. Java runner — same trace format from JDI
5. Mid-project review — workshop, then Python and Java playback
6. Narration — 1–3 sentences from the trace facts; reject invented values
7. C runner — `gcc -g` and GDB; pointers as address plus one dereference
8. Plan check — each step cites a pseudocode line; flag a plan line the run never did
9. Why, cache, and a sandbox you can defend
10. Final review — one question all the way through
