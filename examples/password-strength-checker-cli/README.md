# Password Strength Checker CLI (Spring Boot)

An interactive command-line tool that evaluates password strength locally.
No web server is started — all evaluation runs on the JVM, nothing is stored or transmitted.

## Requirements

- Java 21+
- Maven 3.9+

## Run Interactively

```bash
mvn spring-boot:run
```

Type passwords at the prompt and press Enter to see real-time results.
Enter `q` to quit.

## Compare Mode

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--compare"
```

Or at the interactive prompt, type `--compare`.

## Run the Packaged JAR

```bash
mvn clean package -DskipTests
java -jar target/password-strength-checker-cli-0.0.1-SNAPSHOT.jar
java -jar target/password-strength-checker-cli-0.0.1-SNAPSHOT.jar --compare
```

## Run Tests

```bash
mvn test
```

## Security and Privacy Notes

- All evaluation logic runs purely in Java on the local machine.
- Passwords typed in a real terminal are read via `Console.readPassword()` (no echo).
- No password data is logged, persisted, or transmitted at any point.
