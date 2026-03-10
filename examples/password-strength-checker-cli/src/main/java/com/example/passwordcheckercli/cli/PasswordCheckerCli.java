package com.example.passwordcheckercli.cli;

import com.example.passwordcheckercli.model.EvaluationResult;
import com.example.passwordcheckercli.model.EvaluationResult.Criteria;
import com.example.passwordcheckercli.model.EvaluationResult.Patterns;
import com.example.passwordcheckercli.service.PasswordEvaluatorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive CLI runner for the password strength checker.
 *
 * Security note: passwords typed in interactive mode are read via Console.readPassword()
 * so they are never echoed to stdout. In pipe/non-tty mode, Scanner is used transparently.
 * Passwords are held only as local char[] / String variables for the duration of a single
 * evaluation and are never logged or written to any persistent location.
 */
@Component
public class PasswordCheckerCli implements CommandLineRunner {

    // ANSI colour codes
    private static final String RESET  = "\033[0m";
    private static final String BOLD   = "\033[1m";
    private static final String RED    = "\033[31m";
    private static final String YELLOW = "\033[33m";
    private static final String GREEN  = "\033[32m";
    private static final String CYAN   = "\033[36m";
    private static final String BLUE   = "\033[34m";
    private static final String DIM    = "\033[2m";

    private final PasswordEvaluatorService evaluator;

    public PasswordCheckerCli(PasswordEvaluatorService evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public void run(String... args) {
        printBanner();

        if (args.length > 0 && "--compare".equals(args[0])) {
            runCompareMode();
        } else {
            runInteractiveMode();
        }
    }

    // -------------------------------------------------------------------------
    // Modes
    // -------------------------------------------------------------------------

    private void runInteractiveMode() {
        print(CYAN + "Type a password to check its strength. Enter 'q' to quit, '--compare' to switch to comparison mode." + RESET);
        print("");

        while (true) {
            String input = promptPassword("> Password: ");

            if (input == null || "q".equalsIgnoreCase(input.trim())) {
                print(DIM + "Goodbye." + RESET);
                break;
            }

            if ("--compare".equalsIgnoreCase(input.trim())) {
                runCompareMode();
                return;
            }

            if (input.isBlank()) {
                continue;
            }

            EvaluationResult result = evaluator.evaluate(input);
            printResult(result);
            print("");
        }
    }

    private void runCompareMode() {
        print(CYAN + BOLD + "\n--- Compare Mode ---" + RESET);
        print(DIM + "Enter passwords one at a time. Enter a blank line when finished." + RESET);
        print("");

        List<String> passwords = new ArrayList<>();
        int index = 1;

        while (true) {
            String input = promptPassword(String.format("  Option %d (blank to finish): ", index));
            if (input == null || input.isBlank()) break;
            passwords.add(input);
            index++;
        }

        if (passwords.isEmpty()) {
            print(YELLOW + "No passwords entered." + RESET);
            return;
        }

        printComparisonTable(passwords);

        // Discard all password data immediately after rendering
        passwords.replaceAll(p -> "");
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private void printResult(EvaluationResult result) {
        print(BOLD + "\n  Strength : " + labelColor(result.label()) + result.label().toUpperCase() + RESET);
        print(BOLD + "  Score    : " + scoreBar(result.score()) + "  " + result.score() + "/100" + RESET);
        print("");

        printCriteria(result.criteria());

        if (!result.warnings().isEmpty()) {
            print(YELLOW + BOLD + "\n  Warnings:" + RESET);
            result.warnings().forEach(w -> print(YELLOW + "    ! " + w + RESET));
        }

        if (!result.suggestions().isEmpty()) {
            print(CYAN + BOLD + "\n  Suggestions:" + RESET);
            result.suggestions().forEach(s -> print(CYAN + "    → " + s + RESET));
        }

        if (result.warnings().isEmpty() && result.suggestions().isEmpty()) {
            print(GREEN + "  Your password looks great!" + RESET);
        }
    }

    private void printCriteria(Criteria c) {
        print(BOLD + "  Criteria:" + RESET);
        printCriterion(c.minLength8(),  "At least 8 characters");
        printCriterion(c.uppercase(),   "Uppercase letters");
        printCriterion(c.lowercase(),   "Lowercase letters");
        printCriterion(c.numbers(),     "Numbers");
        printCriterion(c.specialChars(),"Special characters");
    }

    private void printCriterion(boolean met, String label) {
        String mark  = met ? GREEN + "  [✓]" : RED + "  [✗]";
        print(mark + " " + label + RESET);
    }

    private void printComparisonTable(List<String> passwords) {
        // Header
        print("");
        print(BOLD + String.format("  %-6s %-8s %-14s %s", "#", "Score", "Strength", "Top Suggestion") + RESET);
        print(DIM + "  " + "─".repeat(70) + RESET);

        String best = null;
        int bestScore = -1;

        List<EvaluationResult> results = new ArrayList<>();
        for (String pw : passwords) {
            EvaluationResult r = evaluator.evaluate(pw);
            results.add(r);
            if (r.score() > bestScore) {
                bestScore = r.score();
                best = pw;
            }
        }

        for (int i = 0; i < results.size(); i++) {
            EvaluationResult r = results.get(i);
            String suggestion = r.suggestions().isEmpty() ? "None" : r.suggestions().get(0);
            boolean isBest = passwords.get(i).equals(best);
            String prefix = isBest ? GREEN + BOLD : "";
            String suffix = isBest ? "  ← strongest" + RESET : RESET;
            if (suggestion.length() > 40) suggestion = suggestion.substring(0, 37) + "...";
            print(prefix + String.format("  %-6d %-8d %-14s %s", i + 1, r.score(), r.label(), suggestion) + suffix);
        }

        print(DIM + "  " + "─".repeat(70) + RESET);
    }

    private String scoreBar(int score) {
        int filled = score / 5;
        String color = labelColor(score < 30 ? "weak" : score < 55 ? "moderate" : score < 80 ? "strong" : "very strong");
        return color + "█".repeat(filled) + DIM + "░".repeat(20 - filled) + RESET;
    }

    private String labelColor(String label) {
        return switch (label) {
            case "weak"        -> RED;
            case "moderate"    -> YELLOW;
            case "strong"      -> GREEN;
            default            -> BLUE;
        };
    }

    private void printBanner() {
        print(BOLD + CYAN);
        print("  ╔═══════════════════════════════════╗");
        print("  ║   Password Strength Checker CLI   ║");
        print("  ╚═══════════════════════════════════╝" + RESET);
        print(DIM + "  All evaluation runs locally. Nothing is stored or transmitted." + RESET);
        print("");
    }

    // -------------------------------------------------------------------------
    // Input helpers
    // -------------------------------------------------------------------------

    /**
     * Reads a password without echoing if a real TTY console is available,
     * otherwise falls back to plain Scanner. Passwords are returned as String
     * and are never logged anywhere in this class.
     */
    private String promptPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword(prompt);
            if (chars == null) return null;
            String value = new String(chars);
            java.util.Arrays.fill(chars, '\0'); // zero out char array immediately
            return value;
        }

        // Fallback for pipes / IDE terminals
        System.out.print(prompt);
        Scanner scanner = new Scanner(System.in);
        if (!scanner.hasNextLine()) return null;
        return scanner.nextLine();
    }

    private void print(String text) {
        System.out.println(text);
    }
}
