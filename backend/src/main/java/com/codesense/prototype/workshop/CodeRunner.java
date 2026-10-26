package com.codesense.prototype.workshop;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CodeRunner {

    private static final int MAX_SOURCE = 20_000;
    private static final int MAX_STDIN = 4_000;
    private static final int MAX_OUTPUT = 32_000;
    private static final Pattern INCLUDE = Pattern.compile(
            "^\\s*#\\s*include\\s*[<\"]([^>\"]+)[>\"]",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Set<String> C_HEADERS = Set.of(
            "stdio.h", "stdlib.h", "string.h", "ctype.h", "stdbool.h", "limits.h", "math.h", "stddef.h");

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
        if (source != null && source.length() > MAX_SOURCE) {
            return new Result("", "That script is too long to run here.", 1, false, "sandbox");
        }
        if (looksUnsafe(source)) {
            return new Result("", "That script uses a tool this sandbox does not allow.", 1, false, "sandbox");
        }
        String in = stdin == null ? "" : stdin;
        if (in.length() > MAX_STDIN) {
            in = in.substring(0, MAX_STDIN);
        }
        try {
            Path dir = Files.createTempDirectory("codesense-run-");
            try {
                if ("java".equals(lang)) {
                    return runJava(dir, source, in);
                }
                return runC(dir, source, in);
            } finally {
                deleteTree(dir);
            }
        } catch (Exception error) {
            return new Result("", "Could not run.", 1, false, "host");
        }
    }

    private static Result runJava(Path dir, String source, String stdin) throws Exception {
        Path file = dir.resolve("Lab.java");
        Files.writeString(file, source, StandardCharsets.UTF_8);
        Result compiled = exec(List.of("javac", file.getFileName().toString()), dir, "", 15, "javac");
        if (compiled.exitCode() != 0) {
            return compiled;
        }
        return exec(List.of(
                "java",
                "-Xmx64m",
                "-Xss512k",
                "-Djava.awt.headless=true",
                "-cp",
                ".",
                "Lab"), dir, stdin, 8, "java");
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
        scrubEnvironment(builder);
        Process process = builder.start();
        if (stdin != null && !stdin.isEmpty()) {
            byte[] bytes = stdin.getBytes(StandardCharsets.UTF_8);
            process.getOutputStream().write(bytes, 0, Math.min(bytes.length, MAX_STDIN));
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
        String out = readLimited(process.getInputStream());
        String err = readLimited(process.getErrorStream());
        return new Result(out, err, process.exitValue(), false, tool);
    }

    private static void scrubEnvironment(ProcessBuilder builder) {
        builder.environment().keySet().removeIf(key -> {
            String k = key.toLowerCase(Locale.ROOT);
            return k.contains("key")
                    || k.contains("token")
                    || k.contains("secret")
                    || k.contains("password")
                    || k.contains("mongo")
                    || k.contains("credential")
                    || k.contains("openai")
                    || k.contains("gemini")
                    || k.contains("groq");
        });
    }

    private static String readLimited(InputStream in) throws IOException {
        byte[] buf = in.readNBytes(MAX_OUTPUT + 1);
        int n = Math.min(buf.length, MAX_OUTPUT);
        return new String(buf, 0, n, StandardCharsets.UTF_8);
    }

    private static boolean looksUnsafe(String source) {
        String blob = source == null ? "" : source.toLowerCase(Locale.ROOT);
        if (blob.contains("runtime.getruntime")
                || blob.contains("processbuilder")
                || blob.contains("processhandle")
                || blob.contains("files.")
                || blob.contains("nio.file")
                || blob.contains("fileinputstream")
                || blob.contains("fileoutputstream")
                || blob.contains("randomaccessfile")
                || blob.contains("socket")
                || blob.contains("urlconnection")
                || blob.contains("httpclient")
                || blob.contains("system.getenv")
                || blob.contains("javax.script")
                || blob.contains("class.forname")
                || blob.contains("classloader")
                || blob.contains("#include <windows")
                || blob.contains("system(")
                || blob.contains("/bin/")
                || blob.contains("cmd.exe")) {
            return true;
        }
        Matcher includes = INCLUDE.matcher(source == null ? "" : source);
        while (includes.find()) {
            String header = includes.group(1).replace('\\', '/').toLowerCase(Locale.ROOT);
            if (header.contains("..") || header.contains("/") || !C_HEADERS.contains(header)) {
                return true;
            }
        }
        return false;
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
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
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
