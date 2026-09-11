# cloud-itonami-isco-3333

Open Occupation Blueprint for **ISCO-08 3333**: Employment Agents and Contractors.

This repository designs a forkable OSS business for an independent employment placement coordination practice: a candidate-intake and coordination robot manages candidate/employer records and match proposals under a governor-gated actor, so the practice keeps its own placement-coordination records instead of renting a closed applicant-tracking SaaS — and structurally never hires, rejects or screens anyone itself.

**Maturity: `:implemented`.** `src/placementops/` implements the
`PlacementOpsActor` as a `langgraph.graph/state-graph`
(`placementops.actor`) wired to a `Placement Advisor`
(`placementops.advisor`) and an independent `PlacementOpsGovernor`
(`placementops.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 20 tests / 51 assertions green (`kbb -M:test`).

HARD invariants (always hold, never overridable): candidate provenance
(a proposal referencing a candidate must resolve to an independently
registered AND verified candidate record), employer provenance (same,
for any proposal referencing an employer), no-actuation (`:effect`
must be `:propose`), a closed four-op proposal allowlist (any op
outside it — including anything that would finalize a hiring or
rejection decision — is a permanent HARD block), and a content-based
scope-exclusion check: any proposal whose rationale names a
finalization/execution action for a hiring, rejection or other
placement decision that could constitute discriminatory screening is a
permanent HARD block, independent of the op-allowlist check. This
actor **never** finalizes a hiring decision, a candidate-rejection
decision, or any placement decision that could constitute
discriminatory screening — it only coordinates candidate-to-employer
matching, application logging and interview scheduling.

Always-escalate (human sign-off regardless of confidence, mapping this
repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)): `:flag-
compliance-concern` (surfacing a discrimination/screening-bias concern
always requires human review — never auto-resolved, never in any
phase's auto-commit set) and any `:propose-placement-match` at `:stake
:high`.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical/administrative domain work**. Here a candidate-intake and coordination robot performs application logging, match drafting and interview-schedule coordination under an actor that proposes actions and an independent **PlacementOps Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as flagging a compliance concern, or a high-stake candidate-to-employer match) require human sign-off — and no action in this actor's closed op allowlist can ever finalize a hiring, rejection or screening decision.

## Core Contract

```text
candidate application + employer opening + matching policy
        |
        v
Placement Advisor -> PlacementOps Governor -> log record/coordinate match, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, finalize
a hiring/rejection/screening decision, suppress an operating record, or
disclose sensitive data without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3333`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
