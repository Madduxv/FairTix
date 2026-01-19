# Guide to Contributing

## Branching Rules
 - No direct commits to main
 - All changes go through pull requests

## Branch Naming
```
feature/<short-description>
bugfix/<short-description>
chore/<short-description>
```
Examples:
```
feature/add-venue-endpoints
bugfix/postgres-connection
chore/update-readme
```

## Merge Requirements
 - PR must have at least 1 approval
 - CI must pass
 - No unresolved merge conflicts

## PR expectations
Each PR should:
 - Be focused on one thing
 - Explain **what** and **why**
 - Reference an issue (if possible)

Example:
```
Closes #67
```

## PR Title Format
```
\[backend\] Add event creation endpoint
\[frontend\] Implement seat-map UI
\[infrastructure\] Configure Redis caching
```

## Environment Variables and Secrets
 - Never commit .env
 - Use .env.example for reference
 - **No secrets in source code, commit messages, or PR descriptions please**

If a secret is accidentally committed:
 1. Rotate it immediately
 2. Remove it from git history (if needed)
 3. Notify the team

## Resolving Disagreements
 - Prefer the simpler solution
 - Prefer consistency over novelty
 - Rock, paper, scissors
