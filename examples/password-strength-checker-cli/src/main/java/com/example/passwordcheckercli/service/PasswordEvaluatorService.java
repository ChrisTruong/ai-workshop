package com.example.passwordcheckercli.service;

import com.example.passwordcheckercli.model.EvaluationResult;
import com.example.passwordcheckercli.model.EvaluationResult.Criteria;
import com.example.passwordcheckercli.model.EvaluationResult.Patterns;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PasswordEvaluatorService {

    private static final List<String> KEYBOARD_PATTERNS = List.of(
            "qwerty", "asdf", "zxcv", "qazwsx", "1q2w3e"
    );

    private static final Set<String> COMMON_WEAK_TERMS = Set.of(
            "password", "123456", "qwerty", "admin", "welcome", "letmein",
            "iloveyou", "monkey", "dragon", "football", "baseball", "abc123", "changeme"
    );

    private static final Set<String> COMMON_WORDS = Set.of(
            "hello", "summer", "winter", "secret", "login", "master", "sunshine",
            "princess", "freedom", "trustnoone", "flower", "apple", "orange"
    );

    private static final Map<Character, Character> SUBSTITUTION_MAP = Map.of(
            '@', 'a', '$', 's', '1', 'i', '3', 'e',
            '4', 'a', '5', 's', '7', 't', '0', 'o', '!', 'i'
    );

    private static final Pattern SEQUENTIAL_ALPHA =
            Pattern.compile("[a-z]{3,}|[A-Z]{3,}");
    private static final Pattern REPEATED_CHARS =
            Pattern.compile("(.)\\1{2,}");
    private static final Pattern YEAR_PATTERN =
            Pattern.compile("(19\\d{2}|20\\d{2})");
    private static final Pattern BIRTH_DATE_PATTERN =
            Pattern.compile("(?:0[1-9]|[12][0-9]|3[01])(?:0[1-9]|1[0-2])(?:19\\d{2}|20\\d{2})");
    private static final Pattern COMMON_NAMES =
            Pattern.compile("\\b(?:john|maria|anna|mohamed|david|james|robert)\\b",
                    Pattern.CASE_INSENSITIVE);

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";

    public EvaluationResult evaluate(String password) {
        if (password == null) {
            password = "";
        }

        int length = password.codePointCount(0, password.length());
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars()
                .anyMatch(cp -> !Character.isLetterOrDigit(cp) && !Character.isWhitespace(cp));

        Criteria criteria = new Criteria(length >= 8, hasUpper, hasLower, hasDigit, hasSpecial);
        Patterns patterns = checkPatterns(password);

        int score = computeScore(length, criteria, patterns);
        String label = getLabel(score);

        List<String> warnings = buildWarnings(patterns);
        List<String> suggestions = buildSuggestions(length, criteria, patterns);

        return new EvaluationResult(score, label, criteria, patterns, warnings, suggestions);
    }

    // -------------------------------------------------------------------------
    // Internal helpers – nothing here logs or stores any password value
    // -------------------------------------------------------------------------

    private Patterns checkPatterns(String password) {
        String lower = password.toLowerCase();
        String normalized = normalizeLeet(lower);

        boolean sequential = containsSequential(lower);
        boolean repeated = REPEATED_CHARS.matcher(password).find();
        boolean keyboard = KEYBOARD_PATTERNS.stream().anyMatch(lower::contains);
        boolean commonTerm = COMMON_WEAK_TERMS.stream()
                .anyMatch(t -> lower.contains(t) || normalized.contains(t));
        boolean dictionary = COMMON_WORDS.stream()
                .anyMatch(w -> lower.contains(w) || normalized.contains(w));
        boolean personalInfo = COMMON_NAMES.matcher(normalized).find()
                || YEAR_PATTERN.matcher(password).find()
                || BIRTH_DATE_PATTERN.matcher(password).find();

        return new Patterns(sequential, repeated, keyboard, commonTerm, dictionary, personalInfo);
    }

    private String normalizeLeet(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            sb.append(SUBSTITUTION_MAP.getOrDefault(c, c));
        }
        return sb.toString();
    }

    private boolean containsSequential(String lower) {
        for (int i = 0; i <= lower.length() - 3; i++) {
            String chunk = lower.substring(i, i + 3);
            String reversed = new StringBuilder(chunk).reverse().toString();
            if (ALPHABET.contains(chunk) || ALPHABET.contains(reversed)) return true;
            if (DIGITS.contains(chunk) || DIGITS.contains(reversed)) return true;
        }
        return false;
    }

    private int computeScore(int length, Criteria c, Patterns p) {
        int score = 0;
        if (length >= 8) score += 10;
        score += Math.min(20, Math.max(0, (length - 8) * 2));
        if (c.uppercase()) score += 12;
        if (c.lowercase()) score += 12;
        if (c.numbers()) score += 12;
        if (c.specialChars()) score += 12;
        if (length >= 14) score += 10;

        if (p.sequential()) score -= 12;
        if (p.repeated()) score -= 10;
        if (p.keyboard()) score -= 10;
        if (p.commonTerm()) score -= 22;
        if (p.dictionaryWord()) score -= 8;
        if (p.nameOrBirthDate()) score -= 8;

        return Math.min(100, Math.max(0, score));
    }

    private String getLabel(int score) {
        if (score < 30) return "weak";
        if (score < 55) return "moderate";
        if (score < 80) return "strong";
        return "very strong";
    }

    private List<String> buildWarnings(Patterns p) {
        List<String> w = new ArrayList<>();
        if (p.sequential()) w.add("Contains sequential characters (e.g. abc, 123).");
        if (p.repeated()) w.add("Contains repeated characters (e.g. aaa, 111).");
        if (p.keyboard()) w.add("Contains keyboard patterns (e.g. qwerty, asdf).");
        if (p.commonTerm()) w.add("Contains commonly used weak password words or substitutions.");
        if (p.dictionaryWord()) w.add("Contains dictionary words that are easier to guess.");
        if (p.nameOrBirthDate()) w.add("Contains names or birth date-like patterns.");
        return w;
    }

    private List<String> buildSuggestions(int length, Criteria c, Patterns p) {
        List<String> s = new ArrayList<>();
        if (length < 12) s.add("Add " + (12 - length) + " more characters to reach at least 12.");
        if (!c.uppercase()) s.add("Add uppercase letters.");
        if (!c.lowercase()) s.add("Add lowercase letters.");
        if (!c.numbers()) s.add("Add numbers.");
        if (!c.specialChars()) s.add("Add special characters like !, @, #, or %.");
        if (p.commonTerm() || p.dictionaryWord()) s.add("Avoid common words; use random unrelated terms.");
        if (p.sequential() || p.repeated() || p.keyboard()) s.add("Avoid obvious sequences, repeats, and keyboard walks.");
        if (p.nameOrBirthDate()) s.add("Remove personal names and date-like patterns.");
        return s;
    }
}
