package com.example.passwordcheckercli.service;

import com.example.passwordcheckercli.model.EvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEvaluatorServiceTest {

    private PasswordEvaluatorService evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new PasswordEvaluatorService();
    }

    // --- Criteria checks ---

    @Test
    void strongPasswordMeetsAllCriteria() {
        EvaluationResult result = evaluator.evaluate("Str0ng!Password2026");

        assertThat(result.criteria().minLength8()).isTrue();
        assertThat(result.criteria().uppercase()).isTrue();
        assertThat(result.criteria().lowercase()).isTrue();
        assertThat(result.criteria().numbers()).isTrue();
        assertThat(result.criteria().specialChars()).isTrue();
        assertThat(result.score()).isGreaterThanOrEqualTo(80);
        assertThat(result.label()).isEqualTo("very strong");
    }

    @Test
    void shortPasswordFailsLengthCriterion() {
        EvaluationResult result = evaluator.evaluate("Ab1!");
        assertThat(result.criteria().minLength8()).isFalse();
        assertThat(result.label()).isEqualTo("weak");
    }

    @Test
    void missingUppercaseDetected() {
        EvaluationResult result = evaluator.evaluate("lowercase1!");
        assertThat(result.criteria().uppercase()).isFalse();
        assertThat(result.suggestions()).contains("Add uppercase letters.");
    }

    @Test
    void missingNumbersDetected() {
        EvaluationResult result = evaluator.evaluate("NoNumbers!ABC");
        assertThat(result.criteria().numbers()).isFalse();
        assertThat(result.suggestions()).contains("Add numbers.");
    }

    @Test
    void missingSpecialCharsDetected() {
        EvaluationResult result = evaluator.evaluate("NoSpecial123ABC");
        assertThat(result.criteria().specialChars()).isFalse();
        assertThat(result.suggestions()).contains("Add special characters like !, @, #, or %.");
    }

    // --- Scoring and labels ---

    @Test
    void scoreIsWithinRange() {
        for (String pw : new String[]{"", "a", "abc123", "Correct!Horse#Battery9Staple"}) {
            int score = evaluator.evaluate(pw).score();
            assertThat(score).isBetween(0, 100);
        }
    }

    @Test
    void labelWeakForTrivialPassword() {
        assertThat(evaluator.evaluate("abc").label()).isEqualTo("weak");
    }

    @Test
    void labelVeryStrongForComplexPassword() {
        assertThat(evaluator.evaluate("X#7kLm!Pq@9Zt2").label()).isEqualTo("very strong");
    }

    // --- Pattern detection ---

    @Test
    void detectsSequentialCharacters() {
        assertThat(evaluator.evaluate("Pass123!XYZ").patterns().sequential()).isTrue();
    }

    @Test
    void detectsRepeatedCharacters() {
        assertThat(evaluator.evaluate("AAAsecure!1").patterns().repeated()).isTrue();
    }

    @Test
    void detectsKeyboardPattern() {
        assertThat(evaluator.evaluate("Qwerty!234A").patterns().keyboard()).isTrue();
    }

    @Test
    void detectsCommonWeakTerm() {
        assertThat(evaluator.evaluate("MyPassword!1").patterns().commonTerm()).isTrue();
    }

    @Test
    void detectsLeetSubstitution() {
        EvaluationResult result = evaluator.evaluate("P@$$w0rd!9A");
        assertThat(result.patterns().commonTerm()).isTrue();
    }

    @Test
    void detectsDictionaryWord() {
        assertThat(evaluator.evaluate("MySunshine!2X").patterns().dictionaryWord()).isTrue();
    }

    @Test
    void detectsBirthDatePattern() {
        assertThat(evaluator.evaluate("Secure!2000AB").patterns().nameOrBirthDate()).isTrue();
    }

    // --- Suggestions ---

    @Test
    void suggestionsAreSpecificAndActionable() {
        EvaluationResult result = evaluator.evaluate("short");

        assertThat(result.suggestions())
                .contains("Add uppercase letters.", "Add numbers.", "Add special characters like !, @, #, or %.");
        assertThat(result.suggestions().get(0)).contains("Add");
    }

    // --- Privacy and performance ---

    @Test
    void evaluationCompletesWithin100ms() {
        long before = System.currentTimeMillis();
        evaluator.evaluate("Complex!Pass1234withManyExtraCharacters");
        long elapsed = System.currentTimeMillis() - before;

        assertThat(elapsed).isLessThan(100);
    }

    @Test
    void nullPasswordHandledGracefully() {
        EvaluationResult result = evaluator.evaluate(null);
        assertThat(result.score()).isEqualTo(0);
        assertThat(result.label()).isEqualTo("weak");
    }

    @Test
    void emptyPasswordHandledGracefully() {
        EvaluationResult result = evaluator.evaluate("");
        assertThat(result.score()).isEqualTo(0);
    }
}
