---
name: gnomon-quality-gates
description: Validate and harden Gnomon Java changes using its Maven, ArchUnit, integration and security gates. Use when running or fixing formatting, tests, architecture, coverage, SpotBugs, dependency checks, CI or schema validation.
---

# Gnomon quality gates

Read task 09, `docs/ci.md`, ADRs 0002/0016/0017 and `ArchitectureTest` as applicable. Use Java 21; the host is Java 17, so use the Maven Temurin 21 container when needed.

Minimum assisted-task gate: `spotless:check` and `test`. Also run `verify -Pintegration` for persistence, transactions, security or scheduling. Hardened commands are `verify -Pquality`, `verify -Pintegration`, `verify -Pall-tests,quality`, and `verify -Psecurity`.

Treat failed gates as defects or environment blockers and report the exact command/result.
