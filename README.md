# cloud-itonami-isco-2164

Open Occupation Blueprint for **ISCO-08 2164**: Town and Traffic Planners.

This repository designs a forkable OSS platform for an independent town/traffic planner: a planning-support robot prepares zoning scenarios, traffic analyses, and public consultation logistics under a governor-gated actor, so the practice keeps its own planning records and maintains professional control over final planning decisions (zoning changes and traffic regulation authority remain the planning authority's exclusive responsibility).

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a planning-support robot prepares zoning scenario drafts, traffic analyses, public safety assessments, and public consultation materials under an actor that proposes actions and an independent **Traffic Governor** that gates them. The governor never
dispatches the planner's or planning authority's binding authority; `:high`/`:safety-critical` actions (such as
issuing a binding zoning change, setting traffic regulations, or approving a development) remain the planning
authority's exclusive responsibility and can only be proposed, never automated.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
site location + zoning baseline + traffic data + public input
        |
        v
Traffic Advisor -> Traffic Governor -> scenario draft / traffic analysis, or human sign-off
        |
        v
robot actions (gated) + planning records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive planning data without governor approval and
audit evidence. No proposal can claim to issue a binding zoning change, set traffic regulations,
or approve a development — those remain the human planner's/planning authority's exclusive
professional and legal responsibility.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `2164`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors
section, alongside `cloud-itonami-isco-2161`, `-2411`, `-8160`, `-2166`, `-2641`,
`-2651`, `-2652`, `-2654`, `-1219`, `-1223`, `-1330`, `-1341`, `-1349`,
`-1412`, `-1439`, `-2144` and `-2320`): a real
[`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/traffic/store.kotoba` — `Store` protocol + `MemStore`:
  registered sites, committed planning records, an append-only audit ledger.
- `src/traffic/advisor.kotoba` — `Advisor` protocol; `mock-advisor`
  (deterministic, default) proposes a planning operation from a
  request; `llm-advisor` wraps a `langchain.model/ChatModel` — either
  way the advisor only ever produces a `:propose`-effect proposal,
  never a binding zoning change or traffic regulation, and LLM parse failures always yield
  `confidence 0.0` (forces escalation, never fabricated confidence).
- `src/traffic/operation.kotoba` — the **closed vocabulary**: `supported`
  (what the actor may propose) and `reserved` (what belongs to the human
  planner and the planning authority). This is an allowlist, and it is what
  makes the governor a boundary rather than three examples.
- `src/traffic/facts.kotoba` — well-formedness of the site record, the
  request and the proposal envelope. Provenance is asked of the *record*, not
  of whether the store returned something.
- `src/traffic/governor.kotoba` — `TrafficGovernor/check`: a pure
  function, wired as its own `:govern` node. Hard invariants
  (unregistered or unidentified site, a proposal whose `:effect` isn't
  `:propose`, a reserved operation, an **undeclared** operation, a malformed
  envelope) always route to `:hold`. Escalation invariants (public safety
  flags, safety-critical traffic systems, or low advisor confidence) always
  route to `:request-approval` — an `interrupt-before` node that the graph
  checkpoints and only resumes on explicit human approval
  (`actor/approve!`), matching the README's robotics-premise statement
  that binding planning authority always remains the
  planning authority's sole responsibility.
- `src/traffic/phase.kotoba` — the verdict → phase routing rule, extracted
  from the graph so it can be asserted without building one. `:hard?` is
  checked before `:escalate?`: a proposal that is both must hold, because
  escalating it would put a question to a human that they have no authority
  to answer yes to.
- `src/traffic/ledger.kotoba` — hash-chained append-only entries. Every
  entry carries `:ledger/seq`, `:ledger/prev` and `:ledger/hash`, so a
  dropped or reordered entry is detectable; every commit records whether it
  was approved by `:human` or by `:actor`. (A *truncated* ledger still
  verifies — see the namespace docstring for why that limit is real.)
- `src/traffic/actor.kotoba` — `build-graph`, `run-request!`,
  `approve!`: the `langgraph.graph/state-graph` wiring itself.
- `src/traffic/sim.kotoba` — the governed-scenario harness: a table of
  requests run through the **real graph**, asserting the phase each reaches.

## Running it

```bash
kbb -M:test    # 54 tests / 187 assertions
kbb -M:sim     # the governed-scenario harness
```

`kbb -M:test` runs `run_tests.kotoba`, not `cognitect.test-runner`. The
2026-09-10 rename moved every source and test to `.kotoba`, which
`clojure.tools.namespace` does not resolve — the old runner then found
nothing, ran nothing and exited 0. The suite had been dark since that commit.
The runner exits `2` (not `0`, not `1`) when it cannot answer: no sources, no
test namespaces, a source that will not read, or a run that came in **below
the count published here**. That count is load-bearing; a floor that silently
becomes zero is not a floor.

`kbb -M:sim` exits non-zero when the scenario table demonstrated **no
refusal**. A governed actor's claim is not that it acts — it is that there
exist actions it refuses, so a harness that ran only clean scenarios would
print green while showing nothing.

54 tests / 187 assertions

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
