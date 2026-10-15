package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.model.PseudoLine;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckerTest {

    @Test
    void rejectsAQuestionThatAlreadyContainsTheStep() {
        assertTrue(Checker.questionLeaksStep("Should you SET max to the first number?"));
        assertTrue(Checker.questionLeaksStep("Do you FOR each number in the list?"));
        assertTrue(Checker.questionLeaksStep("Is this a job for binary search?"));
        assertTrue(Checker.questionLeaksStep("Do you write max = arr[0]?"));
    }

    @Test
    void allowsADecisionQuestion() {
        assertFalse(Checker.questionLeaksStep("What do you remember between steps?"));
        assertFalse(Checker.questionLeaksStep("Which do you do: look at every item, or stop when you find one?"));
        assertTrue(Checker.questionIsSafe("What do you remember between steps?"));
        assertTrue(Checker.questionIsSafe(
                "If the current item is better than the value you stored, what should happen?"));
    }

    @Test
    void rejectsAQuestionThatAlreadyStatesTheMethod() {
        assertTrue(Checker.questionStatesTheMethod(
                "What happens if you encounter a number that is larger than the current maximum during the iteration?"));
        assertTrue(Checker.questionStatesTheMethod(
                "What would happen if you compared each number with the current maximum and updated the maximum whenever a larger number is found?"));
        assertTrue(Checker.questionStatesTheMethod(
                "What happens if you kept a running maximum while scanning the list?"));
        assertFalse(Checker.questionIsSafe(
                "What would happen if you compared each number with the current maximum and updated the maximum whenever a larger number is found?"));
        assertFalse(Checker.questionStatesTheMethod("What do you remember between steps?"));
        assertTrue(Checker.questionStatesTheMethod(
                "Can you trace the steps of the algorithm as it processes the array [3, 8, 2, 10, 5] and show how the maximum is updated at each step?"));
        assertFalse(Checker.questionIsSafe(
                "Can you trace the steps of the algorithm as it processes the array [3, 8, 2, 10, 5] and show how the maximum is updated at each step?"));
    }

    @Test
    void rejectsAHintThatWalksTheWholeSample() {
        String dump = "After you see 3, then 8, you keep 8 because it's bigger. Then when you see 2, you still keep 8. When you see 10, you update the maximum to 10. Finally, when you see 5, you keep 10 because it's still the biggest.";
        String question = "The numbers come in as 3, then 8, then 2, then 10, then 5. After 3 then 8 then 2, what number are you holding? Then 10? Then 5?";
        assertTrue(Checker.hintDumpsAnswer(dump, question));
        assertTrue(Checker.hintDumpsAnswer(
                "Keep whichever number is bigger. You saw 3 first, then 8, so you would keep 8.",
                question));
        assertTrue(Checker.hintDumpsAnswer(
                "After you check every number, the number you are holding is the largest because you compared it with all others and kept the biggest one.",
                "When you have checked all the numbers in the list, what is the final number you are holding, and why is it the correct answer?"));
        assertTrue(Checker.hintDumpsAnswer(
                "After looking at the first three numbers, you should be holding the largest number you've seen so far, not the first one.",
                "After the first three numbers, what number are you holding in your head?"));
        assertFalse(Checker.hintDumpsAnswer(
                "Go one value at a time. After the first, what is in your head? After the next, did it beat that? After three values, what is left?",
                question));
        var dumpHelp = new Socratic.Help(4, dump, question, "", "10");
        assertFalse(Socratic.helpLooksSafe(dumpHelp, 4, question));
    }

    @Test
    void rejectsYesNoQuestions() {
        assertTrue(Checker.isYesNoQuestion("Do they check each number in the list one by one to find the biggest one?"));
        assertTrue(Checker.isYesNoQuestion("Is this a walk through everything?"));
        assertFalse(Checker.isYesNoQuestion("Which approach do you use: look at every item, or compute once?"));
        assertFalse(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.STRATEGY,
                "Do they check each number in the list one by one to find the biggest one?"));
    }

    @Test
    void inputQuestionMustStayOnInputOnly() {
        assertTrue(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.INPUT,
                "Name what data the program is given."));
        assertTrue(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.INPUT, "What data does the program receive?"));
        assertFalse(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.INPUT, "What comes in, and what must come out?"));
        assertFalse(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.INPUT,
                "How does the program process the input list to find and output the maximum value?"));
        assertTrue(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.OUTPUT, "What one result should it give back?"));
        assertFalse(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.OUTPUT,
                "What should the function return if the list has one element?"));
        assertTrue(Checker.alreadyAsked("What data comes in?", List.of("What data comes in?")));
        assertFalse(Checker.alreadyAsked("What data comes in?", List.of("Name the value given to the program.")));
        assertTrue(Checker.alreadyAsked(
                "If you see 3 and then 8, which one would you remember, and why that one?",
                List.of("You chose to look one at a time. After you see 3, then 8, which number would you keep in mind, and why that one?")));
        assertTrue(Checker.hintMissesTheQuestion(
                "Imagine you're comparing two numbers, 8 and 2. Which one would you keep track of and why? Think about what makes a number the largest.",
                "When should you stop looking through the rest?"));
        assertFalse(Checker.hintMissesTheQuestion(
                "You have looked at some items, but a later one could still be the best. You are not finished until there are no items left.",
                "When should you stop looking through the rest?"));
        assertTrue(Checker.hintMissesTheQuestion(
                "Imagine you're trying to find the tallest person in a line. You start by looking at the first person. Why would you assume they're the tallest before checking anyone else?",
                "You chose the first item. If the list starts with 3, why is 3 a safer start than 0?"));
        assertTrue(Checker.hintMissesTheQuestion(
                "Think of a high-score board. A new score only stays if it beats the one already there.",
                "You chose the first item. If the list starts with 3, why is 3 a safer start than 0?"));
        assertTrue(Checker.hintMissesTheQuestion(
                "A race is not over at the first fast lap. Someone later could still be quicker.",
                "You chose the first item. If the list starts with 3, why is 3 a safer start than 0?"));
        assertFalse(Checker.hintMissesTheQuestion(
                "Zero might not even be in the list. If the list starts with 3, which starting value actually arrived, and which one did you invent?",
                "You chose the first item. If the list starts with 3, why is 3 a safer start than 0?"));
        assertFalse(Checker.hintMissesTheQuestion(
                "If the first reading is -2 and you had stored 0, -2 looks worse than 0 even though -2 is a real value. Which starting value actually arrived?",
                "You chose the first item. If the list starts with 3, why is 3 a safer start than 0?"));
        assertTrue(Checker.hintCopiesQuestion(
                "You chose the first item. If the list starts with 3, why is 3 a safer start than 0?",
                "You chose the first item. If the list starts with 3, why is 3 a safer start than 0?"));
        assertTrue(Checker.hintLeaksEarlierAsk(
                "You chose to look one at a time. After you see 3, then 8, which number would you keep in mind, and why that one?",
                "You chose the first item. If the list starts with 3, why is 3 a safer start than 0?",
                List.of("You chose to look one at a time. After you see 3, then 8, which number would you keep in mind, and why that one?")));
        assertTrue(Checker.questionGoesBackward(
                "Suppose the input is [3, 8, 2, 10, 5]. How would you identify the answer if you were solving it by hand?",
                4,
                List.of()));
        assertTrue(Checker.questionFitsStage(
                "Before you start comparing, what should you store first? Should it always be zero?", 3));
        assertFalse(Checker.questionFitsLab(
                "If the list starts with 3, why is 3 a safer start than 0?",
                "Count the vowels in a string.",
                Socratic.briefFromRules("Count the vowels in a string.")));
        assertFalse(Checker.questionFitsLab(
                "After 3 then 8 then 2, what number are you holding?",
                "Compute the area of a rectangle given length and width.",
                Socratic.briefFromRules("Compute the area of a rectangle given length and width.")));
        assertFalse(Checker.questionFitsStage(
                "What should you do with the count when you encounter a vowel?", 2));
        assertTrue(Checker.questionFitsStage(
                "Should you compare every letter with every other letter, or look through once?", 2));
        assertEquals(6, Checker.stageOfQuestion(
                "If you were doing this by hand, what would you be given first, and what would you write down at the end?"));
        assertTrue(Checker.questionFitsStage(
                "If you were doing this by hand, what would you be given first, and what would you write down at the end?",
                6));
        assertTrue(Checker.hintMissesTheQuestion(
                "Look at just two values from the sample. After 3, you still have to look at 8. Why isn't checking only the first and last values enough?",
                "If you were doing this by hand, what would you be given first, and what would you write down at the end?"));
        assertFalse(Checker.hintMissesTheQuestion(
                "Someone just handed you the list. What do you need in front of you before you can even start looking? After you pick a largest, what would you tell them?",
                "If you were doing this by hand, what would you be given first, and what would you write down at the end?"));
        assertFalse(Checker.hintMissesTheQuestion(
                "You have already looked at 3 and 8. If there was one more number after that, would you skip checking whether it was larger, or would you look at it too?",
                "You have looked at 3 and 8 but not yet 2. Are you finished, or do you still need to look at 2? Why?"));
    }

    @Test
    void treatsEmptyAndIdkAsDontKnow() {
        assertTrue(Checker.isDontKnow(""));
        assertTrue(Checker.isDontKnow("idk"));
        assertTrue(Checker.isDontKnow("I don't know"));
        assertFalse(Checker.isDontKnow("8"));
        assertFalse(Checker.isDontKnow("no"));
        assertFalse(Checker.isDontKnow("70"));
        assertFalse(Checker.isDontKnow("A list comes in and the largest comes out."));
    }

    @Test
    void flagsAThinMemoryAnswer() {
        assertTrue(Checker.memoryAnswerIsThin("the biggest so far"));
        assertFalse(Checker.memoryAnswerIsThin("the first number"));
    }

    @Test
    void rejectsAListWalkQuestionOnAComputeOnceLab() {
        var area = Socratic.briefFromRules("Compute the area of a rectangle given length and width.");
        var largest = Socratic.briefFromRules("Find the largest number in a list.");
        String leaked = "After 3 then 8 then 2, what number are you holding? Then 10? Then 5?";
        assertFalse(Checker.questionFitsLab(leaked, "Compute the area of a rectangle given length and width.", area));
        assertTrue(Checker.questionFitsLab(leaked, "Find the largest number in a list.", largest));
        assertTrue(Checker.questionFitsLab(
                "The two values are 4 and 6. What number do you get when you multiply them?",
                "Compute the area of a rectangle given length and width.",
                area));
        assertFalse(Checker.briefFitsLab(largest, "Compute the area of a rectangle given length and width."));
        assertTrue(Checker.briefFitsLab(area, "Compute the area of a rectangle given length and width."));
    }

    @Test
    void detectRepetition() {
        assertFalse(Checker.detectRepetition("just compute once",
                List.of(new PseudoLine("1", "COMPUTE the result in one step", 0))));
        assertTrue(Checker.detectRepetition("look at every number",
                List.of(new PseudoLine("1", "FOR each number in the input", 0))));
    }
}
