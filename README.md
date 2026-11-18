# chaike-ai

Single-merchant ecommerce administration backend.

## Stack

- Java 21 (Eclipse Temurin)
- Spring Boot 3.5.7
- Gradle (Groovy DSL)
- MySQL 8.4
- MyBatis-Plus

## Configuration

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for the local MySQL 8.4 instance. Defaults target `localhost:3306/shop_admin` and user `root` with an empty password.

Build with Gradle: `gradle bootRun` (Gradle Wrapper will be added when Gradle is available in the environment).
