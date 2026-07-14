# Governance

This project is governed by cloud-itonami's open occupation pattern (ADR-2607011000, CLAUDE.md Actors section). The Traffic Governor is the independent safety layer gating all planning proposals — the public operator console displays all proposals, verdicts, and audit trails live.

## Decision Process

- **Advisor proposals** (`traffic.advisor`) are deterministic (mock) or LLM-derived (`llm-advisor`), with explicit confidence levels.
- **Governor checks** are pure functions: unregistered sites, non-propose effects, and binding-authority attempts are **hard blocks** (`:hold`, no write). Safety-critical proposals and low confidence **always escalate** to human sign-off.
- **Human approval** is the **only gate** for escalated proposals. The graph checkpoints before the interrupt, and resumption (`actor/approve!`) commits the record.
- **Audit ledger** is append-only: every proposal, verdict, and disposition is recorded, regardless of outcome.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).
