---
name: security-audit
description: Audit authentication, authorization, token issuing, secrets, privacy, logging, and user-owned data access in backend and mobile code.
---

# Security Audit Skill

Use this skill when the task mentions auth, token, login, user data, privacy, API key, endpoint exposure, storage, or security review.

## Audit Checklist

1. Authentication required?
2. Authorization enforced?
3. Can user A access user B data?
4. Can a client-side secret be abused?
5. Are tokens issued safely?
6. Are secrets committed?
7. Are tokens logged?
8. Is sensitive data over-returned?
9. Is mobile storage safe?
10. Is release config safe?

## Severity

- P0: remote compromise, token theft, cross-user data access, destructive unauthenticated operation
- P1: serious bypass, weak token issuing, private data exposure
- P2: missing validation, unsafe logs, weak storage
- P3: cleanup or hardening

## Required Output

For every issue:

- Severity
- File/path
- Problem
- Exploit scenario
- Fix
- Test to prove fix

## Rule

Do not downplay security issues just because this is a personal app. Personal apps still contain real private data.
