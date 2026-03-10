package com.example.passwordcheckercli.model;

import java.util.List;

public record EvaluationResult(
        int score,
        String label,
        Criteria criteria,
        Patterns patterns,
        List<String> warnings,
        List<String> suggestions
) {
    public record Criteria(
            boolean minLength8,
            boolean uppercase,
            boolean lowercase,
            boolean numbers,
            boolean specialChars
    ) {}

    public record Patterns(
            boolean sequential,
            boolean repeated,
            boolean keyboard,
            boolean commonTerm,
            boolean dictionaryWord,
            boolean nameOrBirthDate
    ) {}
}
