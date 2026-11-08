## Summary

<!-- What changed and why? Keep it short. -->

## Related issue

<!-- Link an issue, or write "None". -->

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Documentation
- [ ] Refactor
- [ ] Build or dependency change
- [ ] Breaking API change

## Validation

<!-- List the commands you actually ran. Do not claim checks you did not run. -->

```bash
./mvnw -B -DskipTests package
./mvnw test
git diff --check
```

## API and data impact

- [ ] No API change
- [ ] API change documented in `docs/API.md`
- [ ] Database/schema change (describe the migration plan)
- [ ] Architecture or behavior change documented in `docs/ARCHITECTURE.md`
- [ ] Known issue added or updated in `docs/KNOWN_ISSUES.md`

## Checklist

- [ ] The change is focused; unrelated files were not reformatted.
- [ ] No secrets, credentials, or real `.env` files were added.
- [ ] Tests were added or updated where practical.
- [ ] `AGENTS.md` and the AI context files still match the repository.
- [ ] I did not force-push, rewrite history, or delete branches.
