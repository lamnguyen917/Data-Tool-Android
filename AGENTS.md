- All text and label must be written in English
- All comments must be written in English
- All changes to the code must be written in .agents/memory

## Branch policy

- `develop` holds shared features only: the app shell, domain model, queue, settings, copy and export, search fallback, browser verification, and the adapters that belong to `develop`.
- Feature branches (for example `dev-j`) each hold their own adapters for a different purpose. Adapter code, fixtures, tests, and research for those adapters stay on that branch.
- Implement shared features on `develop` first, then merge `develop` into the feature branches. Do not build shared features on a feature branch.
- When working on `develop`, use a feature branch only as a read-only reference for what a shared feature must support, such as Japanese content and encodings or an adapter's specific structure. Keep the shared code generic; do not copy feature-branch adapters onto `develop`.
- Do not write content about feature-branch adapters into `develop` documentation (`AGENTS.md`, `.knowledge/`, `.docs/`, `.agents/memory/`). Document those adapters on their own branch.
- When a shared change requires per-adapter work (for example a new exhaustive `when` over `DataSource`), make it on each feature branch after merging `develop`.
