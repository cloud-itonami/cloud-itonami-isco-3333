# Governance

`cloud-itonami-isco-3333` is an OSS open-occupation blueprint. Governance covers
both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions, finalize a hiring/
  rejection/screening decision, or disclose records.
- PlacementOps Governor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval.
- the closed proposal-op allowlist never gains an op that could finalize a
  hiring decision, a candidate-rejection decision, or any placement decision
  that could constitute discriminatory screening.
- every commit, hold and approval path is auditable.
- real candidate/employer data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support, data-flow
and anti-discrimination review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling candidate/employer data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
- any attempt to make this actor finalize a hiring, rejection or screening
  decision
