package com.codesense.prototype.workshop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LangConverter {

    private LangConverter() {
    }

    public static String normalize(String language) {
        String lang = language == null ? "python" : language.trim().toLowerCase(Locale.ROOT);
        if (lang.startsWith("jav")) {
            return "java";
        }
        if (lang.equals("c") || lang.startsWith("c++") || lang.equals("clang")) {
            return "c";
        }
        return "python";
    }

    public static String displayName(String language) {
        return switch (normalize(language)) {
            case "java" -> "Java";
            case "c" -> "C";
            default -> "Python";
        };
    }

    public static PythonConverter.Program localize(PythonConverter.Program program, String language) {
        String lang = normalize(language);
        if (program == null || "python".equals(lang)) {
            return program;
        }
        String source = "java".equals(lang) ? toJava(program) : toC(program);
        return new PythonConverter.Program(
                source, program.sampleInput(), program.expectedOutput(), program.steps());
    }

    private static String toJava(PythonConverter.Program program) {
        String src = program.source() == null ? "" : program.source();
        List<Integer> numbers = ints(program.sampleInput());
        String text = firstWord(program.sampleInput());
        if (src.contains("max_value")) {
            return javaArray(numbers, "maxValue", ">", "maxValue");
        }
        if (src.contains("min_value")) {
            return javaArray(numbers, "minValue", "<", "minValue");
        }
        if (src.contains("aeiou")) {
            return javaVowels(text.isBlank() ? "hello" : text);
        }
        if (src.contains("side * side")) {
            int side = numbers.isEmpty() ? 5 : numbers.get(0);
            return """
                    public class Lab {
                        public static void main(String[] args) {
                            // What you are given
                            int side = %d;

                            // One calculation
                            int area = side * side;

                            // Show the answer
                            System.out.println(area);
                        }
                    }
                    """.formatted(side);
        }
        if (src.contains("length") && src.contains("width")) {
            int a = numbers.isEmpty() ? 4 : numbers.get(0);
            int b = numbers.size() > 1 ? numbers.get(1) : 6;
            return """
                    public class Lab {
                        public static void main(String[] args) {
                            // What you are given
                            int length = %d;
                            int width = %d;

                            // One calculation
                            int area = length * width;

                            // Show the answer
                            System.out.println(area);
                        }
                    }
                    """.formatted(a, b);
        }
        if (src.contains("% 2")) {
            return javaCountEven(numbers);
        }
        if (src.contains(" * ") && numbers.size() >= 2) {
            return """
                    public class Lab {
                        public static void main(String[] args) {
                            // What you are given
                            int first = %d;
                            int second = %d;

                            // One calculation
                            int product = first * second;

                            // Show the answer
                            System.out.println(product);
                        }
                    }
                    """.formatted(numbers.get(0), numbers.get(1));
        }
        return javaArray(numbers.isEmpty() ? List.of(3, 8, 2) : numbers, "maxValue", ">", "maxValue");
    }

    private static String toC(PythonConverter.Program program) {
        String src = program.source() == null ? "" : program.source();
        List<Integer> numbers = ints(program.sampleInput());
        String text = firstWord(program.sampleInput());
        if (src.contains("max_value")) {
            return cArray(numbers, "max_value", ">");
        }
        if (src.contains("min_value")) {
            return cArray(numbers, "min_value", "<");
        }
        if (src.contains("aeiou")) {
            return cVowels(text.isBlank() ? "hello" : text);
        }
        if (src.contains("side * side")) {
            int side = numbers.isEmpty() ? 5 : numbers.get(0);
            return """
                    #include <stdio.h>
                    int main(void) {
                        // What you are given
                        int side = %d;

                        // One calculation
                        int area = side * side;

                        // Show the answer
                        printf("%%d\\n", area);
                        return 0;
                    }
                    """.formatted(side);
        }
        if (src.contains("length") && src.contains("width")) {
            int a = numbers.isEmpty() ? 4 : numbers.get(0);
            int b = numbers.size() > 1 ? numbers.get(1) : 6;
            return """
                    #include <stdio.h>
                    int main(void) {
                        // What you are given
                        int length = %d;
                        int width = %d;

                        // One calculation
                        int area = length * width;

                        // Show the answer
                        printf("%%d\\n", area);
                        return 0;
                    }
                    """.formatted(a, b);
        }
        if (src.contains("% 2")) {
            return cCountEven(numbers);
        }
        if (src.contains(" * ") && numbers.size() >= 2) {
            return """
                    #include <stdio.h>
                    int main(void) {
                        // What you are given
                        int first = %d;
                        int second = %d;

                        // One calculation
                        int product = first * second;

                        // Show the answer
                        printf("%%d\\n", product);
                        return 0;
                    }
                    """.formatted(numbers.get(0), numbers.get(1));
        }
        return cArray(numbers.isEmpty() ? List.of(3, 8, 2) : numbers, "max_value", ">");
    }

    private static String javaArray(List<Integer> numbers, String name, String cmp, String print) {
        String arr = numbers.toString().replace("[", "{").replace("]", "}");
        return """
                public class Lab {
                    public static void main(String[] args) {
                        // What you are given
                        int[] numbers = %s;

                        // Remember
                        int %s = numbers[0];

                        // Look at each
                        for (int number : numbers) {
                            // Yes or no
                            if (number %s %s) {
                                %s = number;
                            }
                        }

                        // Show the answer
                        System.out.println(%s);
                    }
                }
                """.formatted(arr, name, cmp, name, name, print);
    }

    private static String javaCountEven(List<Integer> numbers) {
        String arr = numbers.toString().replace("[", "{").replace("]", "}");
        return """
                public class Lab {
                    public static void main(String[] args) {
                        // What you are given
                        int[] numbers = %s;

                        // Remember
                        int count = 0;

                        // Look at each
                        for (int number : numbers) {
                            // Yes or no
                            if (number %% 2 == 0) {
                                count = count + 1;
                            }
                        }

                        // Show the answer
                        System.out.println(count);
                    }
                }
                """.formatted(arr);
    }

    private static String javaVowels(String text) {
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"");
        return """
                public class Lab {
                    public static void main(String[] args) {
                        // What you are given
                        String text = "%s";

                        // Remember
                        int count = 0;

                        // Look at each
                        for (int i = 0; i < text.length(); i++) {
                            char letter = text.charAt(i);
                            // Yes or no
                            if ("aeiouAEIOU".indexOf(letter) >= 0) {
                                count = count + 1;
                            }
                        }

                        // Show the answer
                        System.out.println(count);
                    }
                }
                """.formatted(escaped);
    }

    private static String cArray(List<Integer> numbers, String name, String cmp) {
        String arr = numbers.toString().replace("[", "{").replace("]", "}");
        return """
                #include <stdio.h>
                int main(void) {
                    // What you are given
                    int numbers[] = %s;
                    int n = %d;

                    // Remember
                    int %s = numbers[0];

                    // Look at each
                    for (int i = 0; i < n; i++) {
                        // Yes or no
                        if (numbers[i] %s %s) {
                            %s = numbers[i];
                        }
                    }

                    // Show the answer
                    printf("%%d\\n", %s);
                    return 0;
                }
                """.formatted(arr, numbers.size(), name, cmp, name, name, name);
    }

    private static String cCountEven(List<Integer> numbers) {
        String arr = numbers.toString().replace("[", "{").replace("]", "}");
        return """
                #include <stdio.h>
                int main(void) {
                    // What you are given
                    int numbers[] = %s;
                    int n = %d;

                    // Remember
                    int count = 0;

                    // Look at each
                    for (int i = 0; i < n; i++) {
                        // Yes or no
                        if (numbers[i] %% 2 == 0) {
                            count = count + 1;
                        }
                    }

                    // Show the answer
                    printf("%%d\\n", count);
                    return 0;
                }
                """.formatted(arr, numbers.size());
    }

    private static String cVowels(String text) {
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"");
        return """
                #include <stdio.h>
                int main(void) {
                    // What you are given
                    char text[] = "%s";

                    // Remember
                    int count = 0;

                    // Look at each
                    for (int i = 0; text[i] != 0; i++) {
                        char letter = text[i];
                        // Yes or no
                        if (letter == 'a' || letter == 'e' || letter == 'i' || letter == 'o' || letter == 'u'
                                || letter == 'A' || letter == 'E' || letter == 'I' || letter == 'O' || letter == 'U') {
                            count = count + 1;
                        }
                    }

                    // Show the answer
                    printf("%%d\\n", count);
                    return 0;
                }
                """.formatted(escaped);
    }

    private static List<Integer> ints(String raw) {
        List<Integer> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        Matcher matcher = Pattern.compile("-?\\d+").matcher(raw);
        while (matcher.find()) {
            out.add(Integer.parseInt(matcher.group()));
        }
        return out;
    }

    private static String firstWord(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") || trimmed.matches(".*\\d.*") && trimmed.contains(",")) {
            return trimmed;
        }
        return trimmed.replace("\"", "");
    }
}
