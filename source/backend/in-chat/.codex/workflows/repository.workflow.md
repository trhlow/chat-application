# Documentation Workflow

This project uses two governance folders:

- `skill/`: capability profiles, implementation playbooks, domain know-how
- `rules/`: mandatory engineering standards and guardrails

## When adding a new document

1. Pick the right folder:
   - Use `skill/` for "how to do" documents.
   - Use `rules/` for "must/must not" constraints.
2. Use lowercase kebab-case file names.
   - Example: `message-status-playbook.md`
3. Start each file with one H1 title and a short purpose section.
4. Keep statements explicit and testable.
5. Update `README.md` governance links when adding important new docs.
6. If a new rule impacts behavior, add or update tests in the same PR.

## Suggested naming

- `skill/<domain>-skill.md`
- `rules/<scope>-rules.md`

## Change review checklist

- Is this doc in the correct folder?
- Is content non-duplicative with existing docs?
- Are terms consistent with code (`room`, `message`, `presence`, `status`)?
- If rule changed, are tests and API behavior aligned?
