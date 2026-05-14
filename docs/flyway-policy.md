# Flyway policy

## Current decision

Production uses Flyway with an existing Supabase PostgreSQL database. The repository currently contains migrations `V1` through `V12` and `V14`; no `V13` file exists in the current tree, and no deleted `V13*` migration was found in Git history.

Decision: keep the existing migration numbers and do not rename applied migrations.

Runtime configuration:

```yaml
spring.flyway.out-of-order: false
spring.flyway.ignore-migration-patterns: "*:missing"
```

## Rationale

- Renaming `V14__google_oauth.sql` to `V13__google_oauth.sql` would be unsafe if any environment, including Supabase production, has already recorded `V14` in `flyway_schema_history`.
- Adding new functional work as `V13` after `V14` would require out-of-order migrations and makes production history harder to reason about.
- The missing pattern is only for tolerating historical missing migrations during validation. It is not permission to delete or rewrite migration files.

## Rules for future migrations

- Never modify or rename a migration that may already have run in Supabase or any shared environment.
- New migrations must use the next version after the highest committed version. With the current tree, the next migration must be `V15__<description>.sql`.
- Do not create another version gap intentionally.
- If a migration is accidentally removed after deployment, restore the file exactly or document the exception here before release.
- Keep `SPRING_FLYWAY_OUT_OF_ORDER=false` in production.

## Verification performed

Commands used before this decision:

```powershell
git -C AURA-AI-BACKEND log --all --diff-filter=D --summary | Select-String -Pattern 'V13' -CaseSensitive:$false
git -C AURA-AI-BACKEND log --all --oneline -- 'src/main/resources/db/migration/V13*'
Get-ChildItem AURA-AI-BACKEND\src\main\resources\db\migration -Name | Sort-Object
```

Result: no `V13*` migration exists or was found as deleted in Git history.
