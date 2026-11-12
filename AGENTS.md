# Project instructions

## Shared project memory

Recall relevant history through the local agentmemory MCP server when available. Read this file, MEMORY.md, and HANDOFF.md before changing the project. Verify the current working tree and origin remote; memory is context, not authorization.

Use the origin-derived project ID `MphoCodes/AceTimeTracker` for scoped memory searches. Preserve pre-existing local changes. Update the project handoff when application behavior changes. Do not store credentials or raw private transcripts.

## Development

Use the pinned Gradle wrapper and follow docs/development.md. Keep behavior in ViewModels/repositories and presentation in Compose screens. Preserve debug/release authentication separation.

Run relevant local tests before the full app build. Release verification includes lintRelease, assembleRelease, and bundleRelease. Do not equate successful compilation with signed artifacts, verified backend behavior, or a published release.

The historical modernization roadmap contains unimplemented work. Check current source and HANDOFF.md before claiming a stage is complete. Package identity changes, live Firebase deployment/data migration, and publication require explicit scope.
