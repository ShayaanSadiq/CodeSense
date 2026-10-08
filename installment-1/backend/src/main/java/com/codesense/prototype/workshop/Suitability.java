package com.codesense.prototype.workshop;

import com.codesense.prototype.model.WorkshopConstants;
import java.util.List;
import java.util.regex.Pattern;

public final class Suitability {

    private record Rule(Pattern pattern, String reason) {
    }

    private static final List<Rule> HARMFUL = List.of(
            new Rule(
                    Pattern.compile(
                            "\\b(bomb|explosive|weapon|firearm|gunpowder|malware|ransomware|keylogger|phishing|bioweapon|poison)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "harmful"));

    private static final List<Rule> REFUSE = List.of(
            new Rule(Pattern.compile("\\b(sql|select\\s+.+\\s+from|inner join|left join|create table|primary key)\\b", Pattern.CASE_INSENSITIVE), "sql"),
            new Rule(Pattern.compile("\\b(html|css|\\bjsx\\b|webpage|web page|website|react component|dom)\\b", Pattern.CASE_INSENSITIVE), "webpage"),
            new Rule(Pattern.compile("\\b(class diagram|uml|design a class|write a class|create a class|inheritance hierarchy|object[- ]oriented design)\\b", Pattern.CASE_INSENSITIVE), "design"),
            new Rule(Pattern.compile("\\b(debug this|find the bug|fix the (?:error|bug)|what is wrong with|which line (?:is|has) (?:the )?bug)\\b", Pattern.CASE_INSENSITIVE), "debug"),
            new Rule(Pattern.compile("\\b(define|what is the difference|explain the difference|differentiate between|explain the concept|write a short note|discuss the advantages)\\b", Pattern.CASE_INSENSITIVE), "theory"),
            new Rule(Pattern.compile("\\b(android app|graphical user interface|\\bgui\\b|swing|javafx)\\b", Pattern.CASE_INSENSITIVE), "gui"),
            new Rule(Pattern.compile("\\b(write two programs|following programs|part\\s*a\\b[\\s\\S]{0,200}part\\s*b\\b)", Pattern.CASE_INSENSITIVE), "multi-procedure"));

    private static final Pattern CONSOLE_PROCEDURE = Pattern.compile(
            "\\b(find|count|compute|calculate|determine|check|reverse|sum|add|multiply|factorial|fibonacci|palindrome|convert|search|largest|smallest|maximum|minimum|average|gcd|prime|vowel|area|volume|sort|given|list|array|string|number|integer)\\b",
            Pattern.CASE_INSENSITIVE);

    private Suitability() {
    }

    public static boolean ok(String question) {
        return refusal(question) == null;
    }

    public static String refusal(String question) {
        String q = question == null ? "" : question.trim();
        if (q.length() < 8) {
            return WorkshopConstants.REFUSAL_MESSAGE;
        }
        for (Rule rule : HARMFUL) {
            if (rule.pattern.matcher(q).find()) {
                return WorkshopConstants.HARMFUL_REFUSAL;
            }
        }
        for (Rule rule : REFUSE) {
            if (rule.pattern.matcher(q).find()) {
                return WorkshopConstants.REFUSAL_MESSAGE;
            }
        }
        if (!CONSOLE_PROCEDURE.matcher(q).find()) {
            return WorkshopConstants.REFUSAL_MESSAGE;
        }
        return null;
    }
}
