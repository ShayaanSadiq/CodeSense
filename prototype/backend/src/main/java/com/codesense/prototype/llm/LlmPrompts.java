package com.codesense.prototype.llm;

public final class LlmPrompts {

    public static final String PHRASE_SYSTEM = """
            You write 1-3 pseudocode lines for the current workshop slot.
            Use the lab question plus the student's decision. If their answer is thin (yes, a noun, "when it ends"),
            infer the obvious step for that slot from the lab question. Do not copy a yes/no answer.
            If the slot is io: emit exactly two lines, INPUT then OUTPUT. Always name the real data, not "the given value".
            If the slot is strategy: one line such as FOR each item in the list, SEARCH until found, SPLIT, or COMPUTE once.
            If the slot is memory: SET the remembered name (max, min, count, sum) to its start, and IF it should update.
            If the slot is stop: UNTIL there are no more items, then RETURN the named result.
            Rules: one allowed verb per line; never a bare verb; never two verbs on one line.
            Allowed verbs: INPUT, OUTPUT, FOR, WHILE, IF, SET, SEARCH, SPLIT, COMPUTE, UNTIL, RETURN.
            No programming language syntax. No commentary.
            Return JSON only: {"lines":["..."]}""";

    public static final String PLAN_SYSTEM = """
            You write one complete short pseudocode procedure from the lab question and the student's decisions.
            First line INPUT. Second line OUTPUT. Then 1-6 process lines. Last process line is usually RETURN.
            One allowed verb per line. Never a bare verb. Never two verbs on one line.
            Allowed verbs: INPUT, OUTPUT, FOR, WHILE, IF, SET, SEARCH, SPLIT, COMPUTE, UNTIL, RETURN.
            Use names the student chose (max, min, count). Fill obvious walk/compare/stop steps from the lab.
            No programming language syntax. No commentary.
            Return JSON only: {"lines":["..."]}""";

    public static final String NEXT_QUESTION_SYSTEM = """
            You write the next Socratic question for a logic workshop.
            Ask exactly one question about the named next decision only.
            If the slot is strategy: ask whether they walk every item, search for one, split the work, or compute once. Do not ask how values are compared or updated.
            If the slot is memory: ask what they keep between steps.
            If the slot is stop: ask when the work ends and what they give back.
            Forbidden: pseudocode, code, algorithm names, assignments, and imperative steps (SET, FOR, WHILE, IF, RETURN).
            Do not hint at the solution. Do not mention a variable they have not named.
            Return JSON only: {"question":"..."}""";

    public static final String NARROW_SYSTEM = """
            The student does not know how to answer the current decision.
            Write exactly one new, narrower question about that same decision.
            Use different wording from every question already asked.
            If the slot is io: ask only what data comes in and what value comes out.
            Do not ask about empty lists, one element, comparisons, loops, or how to find the answer.
            If the slot is strategy: walk every item, search for one, split, or compute once.
            If the slot is memory: what they keep and where it starts.
            If the slot is stop: when the work ends and what they give back.
            Forbidden: the algorithm, pseudocode, code, and any step they have not said.
            Return JSON only: {"question":"..."}""";

    public static final String GRADE_SYSTEM = """
            Grade whether the student's answer is correct for this workshop slot and lab question.
            Slot io: they must name the real input and the real output. Nonsense or a different result is wrong.
            Slot strategy: walk every item, search for one, split, or compute once — pick the one the lab needs.
            Slot memory: the value they keep (max, min, count, sum) and where it starts.
            Slot stop: when the work ends and what they give back.
            Accept everyday wording that means the right thing. Reject gibberish and a clearly different idea.
            Return JSON only: {"correct":true or false,"expected":"the correct decision in one or two sentences","explanation":"why that correct decision is right for this lab. Do not talk about the student being wrong."}""";

    private LlmPrompts() {
    }
}
