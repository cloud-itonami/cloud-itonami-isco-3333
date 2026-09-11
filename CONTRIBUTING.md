# Contributing

`cloud-itonami-isco-3333` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
kbb -M:test
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real candidate, employer or credential data.
- Keep production writes and disclosures behind PlacementOps Governor.
- Treat this occupation's workflows as high-risk: add tests for permission,
  purpose, safety, anti-discrimination and audit logging.
- Never add an op to the closed proposal allowlist that could finalize a
  hiring decision, a candidate-rejection decision, or any placement decision
  that could constitute discriminatory screening. Any such op must always be
  a hard permanent block or an always-escalate op, never auto-commit-eligible.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
