# Project Agent Routing

When the user asks for code review, implementation planning, architecture review, Flow/coroutines review, UI review, senior Android review, or test strategy, prefer using the project-scoped Codex agents from `.codex/agents`.

Task workflow:
- Start each new task in a dedicated git branch before making changes.
- Use the `codex/` branch prefix by default unless the user asks for a different branch name.
- Keep unrelated existing working-tree changes intact and do not revert them unless the user explicitly asks.

Default routing:
- Use `android-senior` for general Android engineering review across correctness, maintainability, performance, lifecycle, release risk, and regression risk.
- Use `android-architect` for Android architecture, module boundaries, lifecycle, Compose state, DI, Gradle structure, and public module API review.
- Use `android-designer` for Android UI/UX, visual hierarchy, interaction ergonomics, accessibility, UI states, and design consistency.
- Use `flow-reviewer` for Kotlin Coroutines/Flow, lifecycle collection, cancellation, sharing/state operators, replay/buffer, debounce/delay, and race-condition review.
- Use `implementation-worker` for minimal safe code changes.
- Use `test-reviewer` for reducer/state tests, Flow tests, lifecycle regression scenarios, fake repositories, test dispatchers, and manual verification checklists.

Feature workflow:
1. Before implementing a new feature, use `android-designer` to shape the UX, screen behavior, states, interaction details, and accessibility concerns.
2. After the designer pass, use `android-architect` to define the Android architecture, state/effect model, module boundaries, lifecycle handling, and API surface.
3. Before development starts, use `android-senior` to review the proposed UX and architecture for correctness, maintainability, performance, lifecycle risk, and release risk.
4. Start `implementation-worker` only after the designer, architect, and senior review passes are complete or their blockers are resolved.
5. When Flow/coroutines or lifecycle-sensitive behavior is involved, include `flow-reviewer` before implementation.
6. Before finalizing the feature, use `test-reviewer` to define focused automated tests and a manual verification checklist.

For non-trivial Android changes, consider running relevant reviewer agents in parallel before finalizing.
Keep implementation diffs minimal and do not touch unrelated files.
