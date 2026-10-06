# Architecture

ByteForce follows a clean layered architecture:

UI -> Controllers -> Services -> Repositories -> Database

The Swing UI never talks directly to the database.
Business logic stays out of event handlers.
SQL is kept out of UI classes.

## Main goals
- keep the app maintainable
- separate concerns clearly
- support future expansion into student, admin, and assessment features
- use Java 21, Maven, Swing, MySQL, JDBC, HikariCP, Flyway, JUnit 5, Mockito, SLF4J, and Logback
