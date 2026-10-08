package com.codesense.prototype.llm;

@FunctionalInterface
public interface LlmCompleter {

    String complete(String system, String user);
}
