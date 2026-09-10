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
- `src/traffic/governor.kotoba` — `TrafficGovernor/check`: a pure
  function, wired as its own `:govern` node. Hard invariants
  (unregistered site, a proposal whose `:effect` isn't `:propose`,
  any attempt to issue a binding zoning change, set traffic regulations, or approve
  a development)
  always route to `:hold`. Escalation invariants (public safety flags,
  safety-critical traffic systems, or low advisor confidence) always route to
  `:request-approval` — an `interrupt-before` node that the graph
  checkpoints and only resumes on explicit human approval
  (`actor/approve!`), matching the README's robotics-premise statement
  that binding planning authority always remains the
  planning authority's sole responsibility.
- `src/traffic/actor.kotoba` — `build-graph`, `run-request!`,
  `approve!`: the `langgraph.graph/state-graph` wiring itself.

```bash
clojure -M:test
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
