# Security policy

## Supported versions

Only the latest commit on `main` is supported. There are no maintained release branches
or backports.

## Reporting a vulnerability

Please report security issues **privately** through GitHub:

1. Open the repository's **Security** tab.
2. Choose **Report a vulnerability** (GitHub private vulnerability reporting).

If private reporting is unavailable, contact the maintainer
[@Giancarmine](https://github.com/Giancarmine) through GitHub instead of opening a public
issue.

Include:

- the affected commit or deployment;
- reproduction steps or a proof of concept;
- the impact you expect;
- any suggested fix.

**Do not include real database passwords, encryption keys, connection strings with
credentials, or personal data in the report.** Use placeholders such as
`jdbc:postgresql://<host>:5432/<db>` and `<password>`.

## What is in scope

- Leakage or reuse of database credentials, encryption keys, or deployment secrets.
- Unauthorized access to the recipe API.
- Injection, unsafe deserialization, or denial-of-service bugs reachable through an
  endpoint.
- Dependency vulnerabilities that affect the built artifact.
- Data loss or failed deletes caused by the ingredient cascade.
- Identifier handling in `PUT /recipe/{uuid}`.

Out of scope: missing features already listed in
[`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md), the retired historical Heroku database
host, and vulnerabilities that require a compromised host.

## Credential handling

`src/main/resources/application.yml` does not contain database credentials. Supply the
datasource at runtime through `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` (or a local `.env` file, which is ignored by Git).

The historical Heroku host, username, and Jasypt-encrypted password were removed from the
current tree. They remain in Git history and in forks, so treat them as compromised:

1. Do not reuse the old host, username, or password anywhere.
2. If any of them is still active, rotate it immediately.
3. Audit database access logs for unexpected activity.
4. Do not rewrite Git history unless the maintainer explicitly asks. Rotation matters
   more than history cleanup.

## If a secret is exposed

1. Revoke or rotate it at the source (database user, encryption key, platform secret).
2. Update every deployment that uses it.
3. Confirm the service still starts and can reach the database.
4. Check logs for the old value and remove any cached or generated artifacts that
   contain it.

## Response

This is a volunteer project with no guaranteed SLA. Reports are acknowledged as soon as
possible, and fixes follow the normal pull-request workflow.
