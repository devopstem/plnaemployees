## Summary
<!-- What & why (1–3 bullets) -->

-

## Type
- [ ] Feature
- [ ] Bugfix
- [ ] Refactor / docs
- [ ] CI / deps

## Test plan
- [ ] `./gradlew detekt` green
- [ ] `./gradlew :app:testDebugUnitTest` green
- [ ] Manual smoke: home → departments → planogram / search / scanner
- [ ] SonarCloud quality gate passes on this PR

## Risk
- [ ] Low
- [ ] Medium — note rollback
- [ ] High — feature flag / staged rollout

## Checklist
- [ ] No secrets / keystores in the diff
- [ ] Strings updated for RU / EN / BE if user-facing
- [ ] CHANGELOG note if user-visible
