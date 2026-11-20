# Project Instructions for AI Agents

This file provides instructions and context for AI coding agents working on this project.

<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:7510c1e2 -->
## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

**Architecture in one line:** issues live in a local Dolt DB; sync uses `refs/dolt/data` on your git remote; `.beads/issues.jsonl` is a passive export. See https://github.com/gastownhall/beads/blob/main/docs/SYNC_CONCEPTS.md for details and anti-patterns.

## Session Completion

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
   ```bash
   git pull --rebase
   git push
   git status  # MUST show "up to date with origin"
   ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work is NOT complete until `git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds
<!-- END BEADS INTEGRATION -->


## Release

Release instructions (core SDK and FCM/HCM modules): [How to release wonderpush-android-sdk](https://app.notion.com/p/3fa449002dcb81f79c26ee98c870b255) (Notion, Core Push team). Signing/publishing credentials live in `~/.gradle/gradle.properties`, never in the repo.

Notes from the 4.7.2 release (2026-11-20):
- The git remote is `origin` (the Notion page says `github`).
- Signing uses the GPG key `0823F63F`, not an Android keystore. No `android-release.keystore` is needed.
- On this Mac: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`, and `sdk.dir=/opt/homebrew/share/android-commandlinetools` in the gitignored `local.properties`.
- The `nvd_apikey` property is only for the dependency check; publishing works without it.
- After releasing, bump to the next `-SNAPSHOT` (`Bump to SDK vX.Y.Z-SNAPSHOT`) and push master.
- Verify the release is live: `curl -s -o /dev/null -w '%{http_code}' https://repo1.maven.org/maven2/com/wonderpush/wonderpush-android-sdk/X.Y.Z/wonderpush-android-sdk-X.Y.Z.pom` should return 200.

## Build & Test

_Add your build and test commands here_

```bash
# Example:
# npm install
# npm test
```

## Architecture Overview

_Add a brief overview of your project architecture_

## Conventions & Patterns

_Add your project-specific conventions here_
