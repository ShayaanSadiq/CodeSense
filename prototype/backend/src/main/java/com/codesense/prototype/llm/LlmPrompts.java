package com.codesense.prototype.llm;

public final class LlmPrompts {

    public static final String PHRASE_SYSTEM = """
            You write 1-3 pseudocode lines for the current workshop slot.
            Use the lab question plus the student's decision. If their answer is thin (yes, a noun, "when it ends"),
            infer the obvious step for that slot from the lab question. Do not copy a yes/no answer.
            If the slot is io: emit exactly two lines, INPUT then OUTPUT. Always name the real data, not "the given value".
            If the slot is strategy: one line such as FOR each item in the list, SEARCH until found, SPLIT, or COMPUTE once.
            If the slot is memory: SET a real name from the lab (max, min, count, or sum) to its start, and IF it should update.
            For "largest / biggest": SET max to the first number. Never write "the remembered name" or "the value so far".
            If the slot is stop: UNTIL there are no more items, then RETURN the named result.
            Rules: one allowed verb per line; never a bare verb; never two verbs on one line.
            Allowed verbs: INPUT, OUTPUT, FOR, WHILE, IF, SET, SEARCH, SPLIT, COMPUTE, UNTIL, RETURN.
            No programming language syntax. No commentary.
            Return JSON only: {"lines":["..."]}""";

    public static final String PLAN_SYSTEM = """
            You write one complete short pseudocode procedure from the lab question and the student's decisions.
            First line INPUT. Second line OUTPUT. Then SET the starting value (max / min / count). Then FOR. Then IF. Then UNTIL. Last line RETURN.
            One allowed verb per line. Never a bare verb. Never two verbs on one line.
            Allowed verbs: INPUT, OUTPUT, FOR, WHILE, IF, SET, SEARCH, SPLIT, COMPUTE, UNTIL, RETURN.
            Use a short real name from the lab: max, min, count, or sum — never "the remembered name" or "the value so far".
            For largest: SET max to the first number; IF this number is greater than max, SET max to this number; RETURN max.
            Fill obvious walk/compare/stop steps from the lab.
            No programming language syntax. No commentary.
            Return JSON only: {"lines":["..."]}""";

    public static final String NEXT_QUESTION_SYSTEM = """
            You write the next Socratic question for a first-year logic workshop.
            Ask exactly one open question about the named next decision only.
            The question itself must tell the student what to write down.
            Start with What or Which. Never start with How — How asks for a method, not a decision.
            Never a yes/no question. Never start with Do, Does, Did, Is, Are, Can, Should, or Would.
            The student must name the decision themselves. Do not put the answer in the question and ask them to confirm it.
            If the slot is io: this is ONLY "name the incoming data" and "name the one result".
            Ask both in the same question, in everyday words (given / comes in, and give back / print / comes out).
            Never ask how the program works, how it processes data, how it finds a value, or what steps it takes.
            Bad io question: "How does the program process the input list to find and output the maximum value?"
            Good io question: "What data is given to the program, and what one result should it give back?"
            If the slot is strategy: ask which approach they use, listing walk every item, search for one, split the work, or compute once. Do not ask how values are compared or updated. Do not ask whether they check each item.
            If the slot is memory: ask what they keep between steps.
            If the slot is stop: ask when the work ends and what they give back.
            Forbidden: pseudocode, code, algorithm names, assignments, and imperative steps (SET, FOR, WHILE, IF, RETURN).
            Do not hint at the solution. Do not mention a variable they have not named.
            Return JSON only: {"question":"..."}""";

    public static final String NARROW_SYSTEM = """
            The student does not know how to answer the current decision.
            Write exactly one new, narrower open question about that same decision.
            Use different wording from every question already asked.
            Write for a first-year student. The question itself must tell them what to write down.
            Start with What or Which. Never start with How — How asks for a method, not a decision.
            Never a yes/no question. Never start with Do, Does, Did, Is, Are, Can, Should, or Would.
            If the slot is io: ask them to name the incoming data and name the one result. Both in the same question.
            Never ask how the program processes, finds, or computes anything.
            Bad: "How does the program process the input list to find and output the maximum value?"
            Good: "What data is given to the program, and what one result should it give back?"
            Do not ask about empty lists, one element, comparisons, loops, or how to find the answer.
            If the slot is strategy: which approach — walk every item, search for one, split, or compute once.
            If the slot is memory: what they keep and where it starts.
            If the slot is stop: when the work ends and what they give back.
            Forbidden: the algorithm, pseudocode, code, and any step they have not said.
            Return JSON only: {"question":"..."}""";

    public static final String GRADE_SYSTEM = """
            Grade whether the student's answer is correct for this workshop slot and lab question.
            Accept everyday wording that means the right thing. A sentence that starts with yes or no is still correct if the rest names the right decision.
            Do not mark an answer wrong because it is short, informal, or repeats the idea in the question.
            Slot io: they must name the real incoming data and the real outgoing result. Naming only the input is incomplete. Nonsense or a different result is wrong.
            Slot strategy: walk every item, search for one, split, or compute once — pick the one the lab needs.
            For a lab that must look at the whole input (largest, smallest, count, sum), wording such as check every item, look at each number, or go through the list is correct. Do not require the words walk or scan.
            Slot memory: the value they keep (max, min, count, sum) and where it starts.
            Slot stop: when the work ends and what they give back.
            Reject only gibberish or a clearly different idea (for example sum when the lab wants the largest).
            Return JSON only: {"correct":true or false,"expected":"the correct decision in one or two sentences","explanation":"why that correct decision is right for this lab. Do not talk about the student being wrong."}""";

    private LlmPrompts() {
    }
}
