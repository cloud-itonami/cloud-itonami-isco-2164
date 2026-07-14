# Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork and branch**: Create a feature branch for your work.
2. **Tests**: All changes must include tests. Run `clojure -M:test` before pushing.
3. **Audit trail**: Remember that this is an open-source occupation blueprint. Design decisions should be durable and transparent.
4. **Governor discipline**: Any proposal that violates the hard invariants (site registration, `:propose` effect, no binding authority) will be blocked.

## Testing

```bash
clojure -M:test
```

All tests must pass before merging.
