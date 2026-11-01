# Quality Guardian — Demo Project

This is a **synthetic demo project** bundled with the Quality Guardian API.
It exists solely to demonstrate the scanner's capabilities in a safe,
controlled environment.

## What this project demonstrates

- `config.properties` contains a synthetic, non-functional API key entry.
  The scanner will detect it as a potential `SECRET_ASSIGNMENT` and report
  the file path and line number without storing or returning the key value.

- There is no `pom.xml` and no Spring Boot configuration. The scanner will
  produce two `REQUIRED_FILE` warnings. These are expected — this project
  is not a Java application.

## Security note

All API keys and credentials in this project are intentionally fake.
They have no access to any real service or account.
