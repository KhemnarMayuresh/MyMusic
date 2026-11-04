# Instructions for coding agents

Before changing this project, read [the project handoff](docs/AI_HANDOFF.md). It summarizes the product requirements, decisions, current repository state, architecture, and development workflow. Read [project context](docs/PROJECT_CONTEXT.md) for the durable decisions captured from the project conversations.

## Working rules

- Preserve the `my.music` application ID and package namespace. The current product version is `1.0.0` (version code `2`); change it only when a release task calls for a new version.
- Keep My Music local-first. Do not add advertising, purchases, accounts, analytics, premium locks, or internet access.
- Preserve recommendation opt-out and cleanup behavior, playback queue semantics, user-created data, and platform-confirmed file operations. Check the context and owning repository before changing persistence or destructive operations.
- Build and test on a dedicated emulator. Do not install test builds on a personal phone or alter the user's original media.
- Keep signing material, local SDK settings, emulator captures, and build outputs out of Git. Never generate or commit signing credentials.
- Do not push, publish, or move an existing release tag unless the user explicitly requests it. The user handles pushes manually.
- Make changes on the current `main` branch unless the user requests otherwise. Preserve unrelated user changes and inspect `git status` first.
- When a product decision changes, update `docs/PROJECT_CONTEXT.md` and the relevant feature documentation so future agents receive the same direction.

## Project references

- [Product and development handoff](docs/AI_HANDOFF.md)
- [Product requirements and decisions](docs/PROJECT_CONTEXT.md)
- [Architecture](DESIGN.md)
- [Build and tests](docs/TESTING.md)
- [Recommendation behavior](docs/RECOMMENDATIONS.md)
- [Verification and known limits](docs/VERIFICATION.md)
- [Screenshots and overview](README.md)
