---
name: bugfix-with-tests
description: Fix a specific bug safely by identifying root cause, applying minimal patch, and adding or recommending regression tests.
---

# Bugfix With Tests Skill

Use this skill when the user asks to fix a bug, error, failing behavior, or regression.

## Workflow

1. Read the error message carefully.
2. Locate the smallest affected area.
3. Identify root cause.
4. Make the minimal safe fix.
5. Add a regression test if practical.
6. Provide verification commands.

## Rules

- Do not rewrite unrelated code.
- Do not introduce new architecture during a bugfix.
- Do not suppress errors without fixing the cause.
- Do not claim a fix is complete without verification steps.
- If tests cannot be run, say so clearly.

## Output Format

1. Root cause
2. Fix applied or suggested
3. Files affected
4. Regression test
5. Commands to run
6. Remaining risk
