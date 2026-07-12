package com.pr1tcha.riftborne.codex.data.entry;

public record CodexArticleSection(String heading, String body) {
    public CodexArticleSection {
        heading = heading == null ? "" : heading;
        body = body == null ? "" : body;
    }
}
