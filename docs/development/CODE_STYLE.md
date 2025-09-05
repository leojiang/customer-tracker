## Code Style Standards and Configuration

This project standardizes code style across backend and frontend for fast, consistent reviews and CI enforcement.

- Backend (Java, Spring Boot): Google Java Style
- Frontend (Next.js/TypeScript): ESLint (with Next.js recommendations)

### References
- Google Java Style Guide: [link](https://google.github.io/styleguide/javaguide.html)
- Checkstyle Google Checks: [link](https://checkstyle.sourceforge.io/google_style.html)
- ESLint (core): [link](https://eslint.org/)

---

## Backend — Java (Google Java Style)

- Formatter: google-java-format (via Spotless)
- Linter: Checkstyle with Google Checks
- Line length: 100
- Indentation: 2 spaces

### EditorConfig (applies across IDEs)
Create `.editorconfig` at the repository root:

```editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.java]
indent_style = space
indent_size = 2
tab_width = 2
max_line_length = 100
```

### Gradle (Kotlin DSL) — Recommended
Add to `backend/build.gradle.kts`:

```kotlin
plugins {
    java
    id("com.diffplug.spotless") version "6.25.0"
    checkstyle
}

spotless {
    java {
        // Google Java Style formatting
        googleJavaFormat("1.18.1")
        // Clean up imports automatically
        removeUnusedImports()
        target("src/**/*.java")
    }
}

checkstyle {
    toolVersion = "10.12.3"
    // Use the official Google checks file placed in this path
    configFile = rootProject.file("config/checkstyle/google_checks.xml")
    isIgnoreFailures = false
}

// Ensure style checks run with `./gradlew check`
tasks.named("check") {
    dependsOn("spotlessCheck", "checkstyleMain", "checkstyleTest")
}
```

Place the official Google Checkstyle config at `backend/config/checkstyle/google_checks.xml`.
You can download the canonical file from the Checkstyle site ("Google Checks") and save it at that path: [link](https://checkstyle.sourceforge.io/google_style.html).

Common tasks:
- Format: `./gradlew :backend:spotlessApply`
- Verify formatting: `./gradlew :backend:spotlessCheck`
- Lint: `./gradlew :backend:checkstyleMain :backend:checkstyleTest`

### Maven — Alternative
Add to `backend/pom.xml` (if using Maven instead of Gradle):

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.diffplug.spotless</groupId>
      <artifactId>spotless-maven-plugin</artifactId>
      <version>2.43.0</version>
      <configuration>
        <java>
          <googleJavaFormat version="1.18.1" />
          <removeUnusedImports />
        </java>
      </configuration>
      <executions>
        <execution>
          <goals>
            <goal>apply</goal>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
    </plugin>

    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-checkstyle-plugin</artifactId>
      <version>3.3.0</version>
      <configuration>
        <configLocation>config/checkstyle/google_checks.xml</configLocation>
        <encoding>UTF-8</encoding>
        <consoleOutput>true</consoleOutput>
        <failsOnError>true</failsOnError>
      </configuration>
      <executions>
        <execution>
          <phase>verify</phase>
          <goals>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

Common commands:
- Format: `mvn spotless:apply`
- Verify formatting: `mvn spotless:check`
- Lint: `mvn checkstyle:check`

---

## Frontend — ESLint (Next.js + TypeScript)

- Base: ESLint Recommended, TypeScript Recommended, Next.js Core Web Vitals
- Indentation: 2 spaces
- Enforce strict equality, curly braces, and no accidental console/debugger

### Install (dev dependencies)

```bash
npm i -D eslint eslint-config-next @typescript-eslint/eslint-plugin @typescript-eslint/parser
```

### ESLint configuration
Create `frontend/.eslintrc.json`:

```json
{
  "root": true,
  "extends": [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "next/core-web-vitals"
  ],
  "parser": "@typescript-eslint/parser",
  "parserOptions": {
    "ecmaVersion": 2023,
    "sourceType": "module"
  },
  "plugins": ["@typescript-eslint"],
  "rules": {
    "eqeqeq": "error",
    "curly": ["error", "all"],
    "no-console": ["warn", { "allow": ["warn", "error"] }],
    "no-debugger": "error",
    "prefer-const": "error",
    "@typescript-eslint/no-unused-vars": [
      "warn",
      { "argsIgnorePattern": "^_", "varsIgnorePattern": "^_" }
    ]
  },
  "ignorePatterns": [
    "node_modules/",
    ".next/",
    "dist/",
    "build/",
    "coverage/",
    ".turbo/",
    ".vercel/"
  ]
}
```

Optionally create `frontend/.eslintignore` (if you need to add custom ignores beyond the config):

```gitignore
node_modules/
.next/
dist/
build/
coverage/
.turbo/
.vercel/
```

Add scripts to `frontend/package.json`:

```json
{
  "scripts": {
    "lint": "eslint --max-warnings=0 \"src/**/*.{ts,tsx}\"",
    "lint:fix": "npm run lint -- --fix"
  }
}
```

### EditorConfig (JS/TS)
Append to root `.editorconfig` to cover JS/TS files:

```editorconfig
[*.{ts,tsx,js,jsx}]
indent_style = space
indent_size = 2
tab_width = 2
```

### How to run
- Lint: `npm run -w frontend lint`
- Auto-fix: `npm run -w frontend lint:fix`

---

## CI enforcement (optional)
If using GitHub Actions, ensure the workflow runs:
- Backend: `./gradlew spotlessCheck checkstyleMain`
- Frontend: `npm run -w frontend lint`

This keeps style consistent in pull requests.
