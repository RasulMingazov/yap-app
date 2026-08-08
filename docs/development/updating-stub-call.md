# Updating StubCall

Yap resolves `stub-call` from the file-based Maven repository committed in `libs/`,
published from the sibling checkout at `../stub-call`. Do not `includeBuild` or
otherwise depend on that sibling path from Gradle configuration — the committed
`libs/` repo is what makes builds work without it present (e.g. in CI or another
checkout).

To update:

1. Make and commit the change in `../stub-call`, bumping `version` in its
   `build.gradle.kts`.
2. Publish the new version into yap's local repo:
   ```bash
   cd ../stub-call
   ./gradlew publishAllPublicationsToLocalRepository -PlocalMavenRepository=../yap/libs
   ```
3. Bump the `stubcall` version in `gradle/libs.versions.toml` to match.
4. Run the affected mobile and server tests.
5. Commit `stub-call` separately, then commit the yap dependency bump together with
   the newly published `libs/` artifacts.

Do not overwrite or remove a consumed version — each version gets its own directory
under `libs/io/github/rasulmingazov/`.
