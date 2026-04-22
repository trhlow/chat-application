# Mandatory Skill Routing

All non-trivial work in this repository must route through the installed local skills in `.codex/skills`.

Minimum requirements:
- Start with `chat-backend-review` for initial repo understanding unless the task is already tightly scoped and the matching skill is obvious.
- Use `chat-test-and-regression` for every build, test, verification, or bugfix task.
- Add the matching domain skill before editing auth, websocket, MongoDB, storage, API contract, or full feature-slice code.
- Do not run build or verification commands before selecting and reading the relevant skill files.
