# Contributing to Throwable Mobs

Thanks for wanting to help! This document explains how we work. Please read it before opening an issue or pull request.

For the design, physics model, behavior API and roadmap, see [THROWABLE_MOBS.md](THROWABLE_MOBS.md).

## Table of contents

- [Code of conduct](#code-of-conduct)
- [Ways to contribute](#ways-to-contribute)
- [Development setup](#development-setup)
- [Branching: GitHub flow](#branching-github-flow)
- [Commits: atomic & conventional](#commits-atomic--conventional)
- [Pull requests](#pull-requests)
- [Code style](#code-style)
- [Testing](#testing)
- [Reporting bugs](#reporting-bugs)
- [Suggesting features & mob behaviors](#suggesting-features--mob-behaviors)
- [Releasing](#releasing)
- [License](#license)

---

## Code of conduct

Be kind, be constructive, assume good intent. Harassment, discrimination or personal attacks are not tolerated. Maintainers may remove comments, close issues, or block users who don't follow this.

## Ways to contribute

- Report bugs and crashes (with logs!).
- Suggest mob behaviors — especially for vanilla mobs we haven't covered yet.
- Write datapack mob profiles for popular modded mobs.
- Improve docs, translations (`assets/throwablemobs/lang/*.json`), or the example datapack.
- Fix bugs or implement features from the issue tracker. Issues labelled `good first issue` are a good start.

For anything larger than a small fix, **open an issue first** so we can agree on the approach before you spend time on it.

## Development setup

Requirements: **JDK 25**, Git. Gradle is provided by the wrapper.

```sh
git clone https://github.com/SafetImamovic/ThrowableMobs.git
cd ThrowableMobs
./gradlew build
```

Useful tasks:

```sh
./gradlew :physics:test             # unit tests
./gradlew :fabric:runClient         # dev client (Fabric)
./gradlew :neoforge:runClient       # dev client (NeoForge)
./gradlew :fabric:runServer         # dedicated server — test multiplayer features here
./gradlew :fabric:runClient --debug-jvm   # waits for a debugger on port 5005
```

**Docs site:** the guide in `docs/guide` is also a [VitePress](https://vitepress.dev) site, deployed to GitHub Pages
from `master`. Previewing it needs Node 24:

```sh
npm ci
npm run docs:dev                    # live preview at http://localhost:5173/ThrowableMobs/
```

New guide pages also go in the sidebar in `docs/.vitepress/config.mts`.

Any editor works. IntelliJ IDEA (with the Minecraft Development plugin) and Neovim (nvim-jdtls) are both used; see [THROWABLE_MOBS.md §5](THROWABLE_MOBS.md#5-dev-environment-jdk-gradle-neovim).

**Project layout:** almost all code lives in `common/` and must only use vanilla Minecraft. Loader-specific code goes in `fabric/` or `neoforge/` and is reached from `common` through `platform/Services`. If you find yourself writing the same logic twice in both loader modules, it probably belongs in `common`.

## Branching: GitHub flow

We use [GitHub flow](https://docs.github.com/en/get-started/using-github/github-flow): one long-lived branch,
`master`, which always builds and is always releasable. Everything else is a short branch off `master` that comes back
through a pull request.

| Prefix | For | Example |
|---|---|---|
| `feat/` | New features and behaviors | `feat/creeper-impact-explosion` |
| `fix/` | Bug fixes | `fix/slime-split-loop` |
| `docs/` | Documentation only | `docs/cookbook-alexs-mobs` |
| `ci/` | GitHub Actions and release automation | `ci/build-workflow` |
| `chore/` | Build, dependencies, repo housekeeping | `chore/bump-neoforge` |

**Rules:**

- Never commit directly to `master`. Everything goes through a pull request.
- Branch names are lowercase kebab-case and short. The prefix matches the main commit type.
- Keep branches short-lived. Rebase onto `master` if it moved on:
  ```sh
  git fetch origin
  git rebase origin/master
  ```
- Releases are tags on `master` (see [Releasing](#releasing)); there are no release or develop branches.
- Versions follow [Semantic Versioning](https://semver.org). The public API (`api` package) and the datapack JSON schema are what count as "breaking".

## Commits: atomic & conventional

### Atomic commits

Each commit is **one logical change** that:

- does exactly one thing (one fix, one feature step, one refactor — not a mix),
- **builds and passes tests on its own**, so `git bisect` and reverts always work,
- contains everything that change needs (code + tests + docs/config for it) and nothing else.

Practical guidelines:

- Don't mix refactoring/formatting with behavior changes. Refactor in one commit, change behavior in the next.
- Don't commit "WIP", "fix typo", or "address review" noise into the final history. Clean up with `git commit --fixup` + `git rebase -i --autosquash` (or `git add -p` to split changes) before requesting review.
- Stage selectively with `git add -p` when a file contains more than one change.

### Commit messages

We use [Conventional Commits](https://www.conventionalcommits.org):

```
<type>(<optional scope>): <summary>

<optional body: what and why, not how>

<optional footer: BREAKING CHANGE: ..., Closes #123>
```

- **Summary:** imperative mood ("add", not "added"/"adds"), lowercase, no trailing period, ≤ 72 characters.
- **Body:** wrap at 72 characters. Explain *why* the change is needed if it isn't obvious.

| Type | Use for |
|---|---|
| `feat` | New feature or behavior |
| `fix` | Bug fix |
| `perf` | Performance improvement |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `test` | Adding or fixing tests |
| `docs` | Documentation only |
| `build` | Gradle, dependencies, version bumps |
| `ci` | GitHub Actions / publishing workflows |
| `chore` | Anything else that doesn't touch `src/` (e.g. `.gitignore`) |

Common scopes: `common`, `fabric`, `neoforge`, `api`, `physics`, `behavior`, `config`, `network`, `client`, `data`.

Examples:

```
feat(behavior): explode creepers on impact

Explosion power scales with impact energy and is capped by the
creeper's mob profile. Respects the mobGriefing gamerule.

Closes #12
```

```
fix(physics): use reduced mass for mob-vs-mob collisions
```

```
feat(api)!: pass ImpactContext to ThrowBehavior#onImpact

BREAKING CHANGE: onImpact(ThrowContext, HitResult) was replaced by
onImpact(ImpactContext).
```

## Pull requests

1. Fork the repo (or create a branch if you have write access) from `master`.
2. Make your changes as atomic commits.
3. Make sure `./gradlew build` passes locally, and test in-game on **both Fabric and NeoForge** if you touched gameplay.
4. Open a PR **against `master`**. CI doesn't run on PRs (to save Actions minutes); a maintainer can run the Build
   workflow on the branch by hand, which attaches the jars to the run so reviewers can test them.
5. Fill in the PR template:
   - What does it change and why? Link the issue (`Closes #123`).
   - How did you test it? (Loader(s), single-player / dedicated server, screenshots or a short clip for gameplay changes.)
   - Any config, API, or datapack schema changes?
6. Keep the PR focused. One feature or fix per PR; unrelated cleanups go in a separate PR.
7. Address review feedback with fixup commits, then squash them before merge so the history stays atomic.

PRs are merged once `./gradlew build` passes and a maintainer has approved, with a **merge commit**, so your atomic commits stay
as they are and the PR shows up in the history.

If you add something players will notice, add a line under `## [Unreleased]` in [CHANGELOG.md](CHANGELOG.md).

## Code style

- **Java 25**, 8-space indentation, braces on their own line (Allman), ≤ 120 characters per line.
- Follow the style of the surrounding code. Use Mojang's names (the game is unobfuscated since 26.1).
- Names: `PascalCase` classes, `camelCase` methods/fields, `UPPER_SNAKE_CASE` constants.
- Keep `common` free of loader imports. No `net.fabricmc` or `net.neoforged` imports outside the loader modules. One exception: NeoForge's config API (`ModConfigSpec`) in `common/config`, which Forge Config API Port provides on Fabric.
- Keep client-only code (`net.minecraft.client.*`) out of code that runs on dedicated servers. Always test with `runServer`.
- Prefer loader events over mixins. If a mixin is necessary, use [MixinExtras](https://github.com/LlamaLad7/MixinExtras) (`@WrapOperation`, `@ModifyExpressionValue`) instead of `@Redirect` or `@Overwrite`, for compatibility with other mods.
- Every gameplay number belongs in the config or a mob profile, not hard-coded.
- Public API (`api` package) needs Javadoc. Elsewhere, comment the *why*, not the *what*.
- User-facing text goes through translation keys (`throwablemobs.*`) in `en_us.json`, never hard-coded strings.

## Testing

- **Unit tests** (JUnit 5, `physics/src/test`) for pure logic — physics math, catch geometry. The `physics` module has no Minecraft imports so it stays testable; keep it that way.
- **GameTests** for in-game behavior (e.g. "a thrown creeper explodes on impact").
- **Manual testing:** for multiplayer features, run a dedicated server and connect two clients.
- Bug fixes should come with a test that fails without the fix whenever practical.

## Reporting bugs

[Open a bug report](https://github.com/SafetImamovic/ThrowableMobs/issues/new/choose). The form asks for:

- Minecraft version, loader (Fabric/NeoForge) and loader version, Throwable Mobs version.
- Other installed mods (or the modpack name and version).
- Steps to reproduce, what you expected, and what happened.
- The **full** `latest.log` and any crash report, uploaded to [mclo.gs](https://mclo.gs) rather than pasted into the issue.

Check existing issues first to avoid duplicates.

**Security issues** (e.g. a way to crash or exploit servers via packets): please don't open a public issue. See
[SECURITY.md](SECURITY.md).

## Suggesting features & mob behaviors

Use the **feature request** or **mob behavior** issue form. For a mob behavior, describe:

- The mob (vanilla or modded — include the mod).
- What should happen on pickup, in flight, on impact, and when caught.
- Why it fits the mob's character.
- Whether it should be on by default for balanced survival, or opt-in for chaotic packs.

Behaviors that can be built from existing datapack atoms can often be added as a mob profile JSON without Java code at all.

## Releasing

Maintainers only.

1. On a `chore/release-<version>` branch: set `version` in `gradle.properties`, and in `CHANGELOG.md` rename
   `## [Unreleased]` to `## [<version>] - <date>` and add a fresh empty `## [Unreleased]` above it. Merge the PR.
2. Tag the merge commit on `master` and push the tag:
   ```sh
   git tag v<version>
   git push origin v<version>
   ```
3. The release workflow checks that the tag matches `gradle.properties`, builds both loaders and creates a GitHub
   Release with both jars and the changelog section. The docs workflow deploys the docs site from the same tag, so
   docs changes merged since the last release go live with it. Versions with a `-` (like `0.2.0-beta.1`) and every `0.x`
   version are marked as pre-releases.
4. Upload the same jars to Modrinth by hand, with the same changelog.

## License

By contributing, you agree that your contributions are licensed under the [MIT License](LICENSE), the same license as the project.
