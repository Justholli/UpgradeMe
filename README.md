# UpgradeMe

Minimal JVM-based Android sample that demonstrates how to cover business logic with unit tests and Activity behaviour with Robolectric UI tests.

## Test commands

```bash
gradle test
```

The test suite contains:

- pure JVM unit tests for `UpgradeCalculator`;
- Robolectric tests for `MainActivity`, including the upgrade-eligible, upgrade-not-eligible, and empty-input UI states.
