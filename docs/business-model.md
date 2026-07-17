# Business Model: Independent Employment Placement Coordination Practice

## Classification

- Repository: `cloud-itonami-isco-3333`
- ISCO-08: `3333`
- Occupation: Employment Agents and Contractors
- Social impact: fair-hiring-access, local-employment, small-business-support

## Customer

- job candidates
- employers (small/medium businesses)

## Offer

- candidate application intake and record logging
- candidate-to-employer match coordination
- interview scheduling coordination
- compliance-concern flagging (discrimination/screening-bias surfacing)

## Revenue

- monthly retainer
- per-placement coordination fee

## Trust Controls

- no proposal commits or escalates without an independently registered AND
  verified candidate/employer record
- the closed proposal-op allowlist never includes an op that could finalize a
  hiring decision, a candidate-rejection decision, or any placement decision
  that could constitute discriminatory screening
- `:flag-compliance-concern` always requires human sign-off, never auto-
  resolved
- high-stake candidate-to-employer matches always require human sign-off
- placement-coordination records are auditable, not editable
