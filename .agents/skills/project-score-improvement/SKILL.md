---
name: project-score-improvement
description: Evaluate the project like a senior reviewer and suggest the highest-impact improvements to increase project quality, grading score, maintainability, security, and production readiness.
---

# Project Score Improvement Skill

Use this skill when the user asks how to improve the project, increase score, make it production-ready, or prepare it for review.

## Evaluation Areas

Score the project across:

1. Architecture
2. Security
3. Backend correctness
4. Mobile correctness
5. Data model
6. Testing
7. Error handling
8. UX completeness
9. DevOps/config
10. Documentation

## Priority System

- P0: blocks demo or causes data/security disaster
- P1: must fix before serious review
- P2: improves quality significantly
- P3: polish

## Output Format

1. Current estimated score
2. Biggest score blockers
3. 1-day improvement plan
4. 3-day improvement plan
5. 1-week improvement plan
6. What not to waste time on
7. Final senior verdict

## Review Style

Be direct. Do not flatter weak code. Explain trade-offs. Prioritize changes that improve review score and real maintainability.
