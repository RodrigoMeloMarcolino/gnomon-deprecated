---
name: gnomon-sdd-continuity
description: Resume or implement Gnomon work while synchronizing PRD, ADRs, tasks, checkpoint and repository reality. Use before Gnomon development, /auto-pilot, /sync-spec, or continuation from a handoff.
---

# Gnomon SDD continuity

1. Read `AGENTS.md`, `docs/prd.md`, `docs/adr/README.md`, `docs/tasks/README.md`, the active task and `docs/tasks/implementation-checkpoint.md`.
2. Inspect `git status --short` and relevant code before editing; state meaningful divergence and preserve correct work.
3. In assisted work, make the smallest safe slice, add tests, run applicable gates and update task/checkpoint notes. A new architectural decision requires an ADR.
4. Use only repository-versioned skills for Gnomon work.
