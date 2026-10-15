package com.codesense.prototype.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WorkshopConstants {

    public static final int MAX_ANSWER_TRIES = 3;

    public static final String REFUSAL_MESSAGE =
            "CodeSense builds one procedure. Paste the part that reads input and produces one result.";

    public static final String HARMFUL_REFUSAL =
            "CodeSense will not help with that request.";

    public static final Map<Slot, String> FIXED_QUESTIONS = Map.of(
            Slot.INPUT, "Name what data the program is given.",
            Slot.STRATEGY, "Which approach do you use: look at every item, search for one, split the problem, or compute once?",
            Slot.MEMORY, "What do you remember between steps?",
            Slot.STOP, "When do you stop walking the input?",
            Slot.OUTPUT, "What one result should the program give back?");

    public static final Map<Slot, String> NARROW_QUESTIONS = Map.of(
            Slot.INPUT, "Name the value given to the program.",
            Slot.STRATEGY,
            "Which of these is the work: walk through every item, stop when you find one, divide the work, or just compute once?",
            Slot.MEMORY, "What number or word do you keep as you go, and where does it start?",
            Slot.STOP, "After the last item, is the walk finished?",
            Slot.OUTPUT, "Name the one value it should print when it finishes.");

    public static final Map<Slot, List<String>> NARROW_LADDERS = Map.of(
            Slot.INPUT,
            List.of(
                    NARROW_QUESTIONS.get(Slot.INPUT),
                    "What data comes in?",
                    "Say the input in a few words."),
            Slot.STRATEGY,
            List.of(
                    NARROW_QUESTIONS.get(Slot.STRATEGY),
                    "Which do you do: look at every item, or only until you find one?",
                    "Which is this: a walk through everything, a search, a split, or one calculation?"),
            Slot.MEMORY,
            List.of(
                    NARROW_QUESTIONS.get(Slot.MEMORY),
                    "What name would you give the value you keep as you go?",
                    "Where does that remembered value start?"),
            Slot.STOP,
            List.of(
                    NARROW_QUESTIONS.get(Slot.STOP),
                    "When are there no more items to look at?",
                    "When is the walk finished?"),
            Slot.OUTPUT,
            List.of(
                    NARROW_QUESTIONS.get(Slot.OUTPUT),
                    "What single value should it show when it finishes?",
                    "Say the output in a few words."));

    public static final List<String> MEMORY_FOLLOW_UPS = List.of(
            "Where does that first remembered value come from?",
            "When do you change that remembered value?");

    public static final Set<String> ALLOWED_VERBS = Set.of(
            "INPUT", "OUTPUT", "FOR", "WHILE", "IF", "SET", "SEARCH", "SPLIT", "COMPUTE", "UNTIL", "RETURN");

    private WorkshopConstants() {
    }
}
