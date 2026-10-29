# Merge conflict resolution

Date: 2026-10-29

## Request

Resolve the active merge conflicts between `dev-j` and `develop`.

## Resolved files

- `.knowledge/README.md`
- `.knowledge/index/by-id.yaml`
- `.knowledge/index/by-tag.yaml`
- `app/src/main/java/com/elp/datatool/DataToolApplication.kt`
- `app/src/main/java/com/elp/datatool/data/repository/InMemoryRepositories.kt`
- `app/src/main/java/com/elp/datatool/domain/model/DataSource.kt`
- `app/src/main/java/com/elp/datatool/presentation/results/ResultDetailScreen.kt`
- `app/src/main/res/values/strings.xml`

## Resolution decisions

- Preserved all existing Ave, Tokyo-Hot, FC2, and Heyzo adapters and added the GOG adapter alongside them.
- Preserved browser verification and cookie import while integrating per-adapter DuckDuckGo fallback settings.
- Combined all data-source validation constants and imports required by both branches.
- Preserved performer and seller detail labels and added the GOG developer label.
- Combined the GOG knowledge entries with the existing adapter research entries and tag indexes.
- Added DuckDuckGo scopes and trusted hosts for Ave, Tokyo-Hot, FC2, and Heyzo because the merged `DataSource` enum made those branches required.

## Verification

- Confirmed no merge conflict markers remain outside `.git` and generated build directories.
- Ran `gradlew.bat testDebugUnitTest --no-daemon` with the available local JDK.
- Result: `BUILD SUCCESSFUL`.
- Git finalized the resolved merge as commit `6ec1ead` (`Merge branch 'develop' into dev-j`).
- The post-merge DuckDuckGo enum integration fix and this memory entry were staged separately; no additional commit was created.
