package com.codesense.prototype.workshop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class CodeRunner {

    public record Result(String stdout, String stderr, int exitCode, boolean timedOut, String tool) {
    }

    private CodeRunner() {
    }

    public static Result run(String language, String source, String stdin) {
        String lang = LangConverter.normalize(language);
        if ("python".equals(lang)) {
            return new Result("", "Run Python in the browser.", 1, false, "pyodide");
        }
        if ("c".equals(lang)) {
            return new Result("", "Run C in the browser.", 1, false, "jscpp");
        }
        if (looksUnsafe(source)) {
            return new Result("", "That script uses a tool this sandbox does not allow.", 1, false, "sandbox");
        }
        try {
            Path dir = Files.createTempDirectory("codesense-run-");
            try {
                if ("java".equals(lang)) {
                    return runJava(dir, source, stdin);
                }
                return runC(dir, source, stdin);
            } finally {
                deleteTree(dir);
            }
        } catch (Exception error) {
            return new Result("", error.getMessage() == null ? "Could not run." : error.getMessage(), 1, false, "host");
        }
    }

    private static Result runJava(Path dir, String source, String stdin) throws Exception {
        Path file = dir.resolve("Lab.java");
        Files.writeString(file, source, StandardCharsets.UTF_8);
        Result compiled = exec(List.of("javac", file.getFileName().toString()), dir, "", 15, "javac");
        if (compiled.exitCode() != 0) {
            return compiled;
        }
        return exec(List.of("java", "-cp", ".", "Lab"), dir, stdin, 8, "java");
    }

    private static Result runC(Path dir, String source, String stdin) throws Exception {
        Path file = dir.resolve("lab.c");
        Files.writeString(file, source, StandardCharsets.UTF_8);
        String compiler = firstOnPath("gcc", "clang", "tcc");
        if (compiler == null) {
            return new Result("", "No C compiler found (gcc, clang, or tcc). You can still read the code and predict each step.",
                    1, false, "missing");
        }
        Path binary = dir.resolve(isWindows() ? "lab.exe" : "lab");
        Result compiled = exec(List.of(compiler, file.getFileName().toString(), "-o", binary.getFileName().toString()),
                dir, "", 15, compiler);
        if (compiled.exitCode() != 0) {
            return compiled;
        }
        return exec(List.of(binary.toAbsolutePath().toString()), dir, stdin, 8, compiler);
    }

    private static Result exec(List<String> command, Path dir, String stdin, int seconds, String tool)
            throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(dir.toFile());
        builder.redirectErrorStream(false);
        Process process = builder.start();
        if (stdin != null && !stdin.isEmpty()) {
            process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
            if (!stdin.endsWith("\n")) {
                process.getOutputStream().write('\n');
            }
        }
        process.getOutputStream().close();
        boolean finished = process.waitFor(seconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new Result("", "Stopped: this looked like it would run forever.", 124, true, tool);
        }
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Result(out, err, process.exitValue(), false, tool);
    }

    private static boolean looksUnsafe(String source) {
        String blob = source == null ? "" : source.toLowerCase();
        return blob.contains("runtime.getruntime")
                || blob.contains("processbuilder")
                || blob.contains("files.")
                || blob.contains("#include <windows")
                || blob.contains("system(")
                || blob.contains("/bin/")
                || blob.contains("cmd.exe");
    }

    private static String firstOnPath(String... names) {
        for (String name : names) {
            try {
                ProcessBuilder builder = isWindows()
                        ? new ProcessBuilder("where", name)
                        : new ProcessBuilder("which", name);
                builder.redirectErrorStream(true);
                Process process = builder.start();
                boolean finished = process.waitFor(3, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    return name;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static void deleteTree(Path dir) {
        try (var paths = Files.walk(dir)) {
            List<Path> all = new ArrayList<>();
            paths.forEach(all::add);
            for (int i = all.size() - 1; i >= 0; i--) {
                Files.deleteIfExists(all.get(i));
            }
        } catch (IOException ignored) {
            // temp cleanup
        }
    }
}
