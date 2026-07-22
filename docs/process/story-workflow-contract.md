# Story Workflow Contract

**Contract version:** 1.0
**Effective:** when the commit containing this version is an ancestor of synchronized `main`
**Scope:** repository Story planning, development, Repair, Review, evidence, merge, and governance changes

This is the canonical workflow contract. `AGENTS.md` and the role prompt templates point here instead of copying changing process rules. A Story prompt supplies only the variables and scope unique to that Story.

The terms **MUST**, **MUST NOT**, **SHOULD**, and **MAY** are normative.

## Skill role mapping

When installed, `$bmad-method`, `$supervised-story-delivery`, and `huashu-design` support distinct responsibilities:

- `$bmad-method` supports planning, readiness, horizontal contract review, Story shaping, and correct-course analysis. It does not grant implementation or merge authority.
- `$supervised-story-delivery` may orchestrate an explicitly user-authorized approved Story or finite ordered Story sequence. In that mode the management supervisor remains read-only and dispatches separate preflight, writer, fresh independent reviewer, integration, and validation roles under this contract.
- `huashu-design` remains the UI/visual-design skill. It applies to visual and interaction work without replacing product decisions, Story scope, runtime validation, or independent Review.

The repository's pinned structured scope manifest, protected manifest, accepted validator, and evidence contract are the project-specific delivery gates. They satisfy and specialize the delivery skill's manifest/check requirements. A global skill inspector is supplemental only and MUST NOT replace or weaken the accepted repository validator. No skill overrides this contract, accepted decisions, exact scope, evidence requirements, or user authority.

## 1. Contract authority and provenance

Every workflow pins these values separately:

- `ACCEPTED_RULES_SHA`: the synchronized `origin/main` commit whose governance and decisions control the current Dev or Review operation.
- `STORY_BASE_SHA`: the exact commit from which development started.
- `STORY_SHA`: the immutable full commit SHA submitted for Review.
- `REVIEW_BASE_SHA`: the exact `origin/main` commit against which Review and integration are performed.
- `MERGE_SHA`: the final two-parent no-fast-forward merge commit.
- `EVIDENCE_SOURCE_SHA`: the executable source used to build or capture a behavior claim.

For a new Story, `ACCEPTED_RULES_SHA` and `STORY_BASE_SHA` normally match. For a Repair on an older branch, they may differ: current accepted rules come from `ACCEPTED_RULES_SHA`, while Story history and scope still retain the original `STORY_BASE_SHA` plus the exact expected Repair parent.

Accepted governance, decisions, and status inputs MUST be read from `ACCEPTED_RULES_SHA` with `git show <SHA>:<path>` or an equivalent clean checkout. During Review and integration, `ACCEPTED_RULES_SHA` equals `REVIEW_BASE_SHA`. A dirty working-tree copy, a pending branch, a handoff report, and conversational memory are overlays, not accepted facts.

Authority is applied in this order:

1. system and user instructions;
2. accepted `AGENTS.md` files and this contract at `ACCEPTED_RULES_SHA`;
3. accepted decisions, architecture, readiness, and the nominated current-status index at that SHA;
4. the current Story contract and immutable Story delta;
5. tests and evidence, limited to the behavior they actually prove;
6. historical or sealed material, limited to explicitly adopted facts.

An accepted superseding decision MAY narrowly replace an older baseline statement. Lower-level or historical text MUST NOT override it.

Governance changes are reviewed under the previously accepted contract. Proposed changes MUST NOT relax or approve their own Review. Version 1.0 is the bootstrap exception only in the sense that no earlier canonical contract exists: its Review is governed by the fixed accepted-base `AGENTS.md` and role templates, while this candidate contract and validator are advisory evidence until merged. All later versions use the previously accepted canonical contract.

During Review and integration, `ACCEPTED_RULES_SHA` equals `REVIEW_BASE_SHA`; Dev does not invent a Review base before Review exists.

## 2. Required Story manifest fields

A generated Dev or Review prompt MUST resolve all of the following. `none` is a valid explicit value; an empty field is not.

- Story ID, title, type, objective, and acceptance assertions. Story type is one of `implementation`, `repair`, `planning`, `governance`, or `evidence`.
- repository root and Story branch.
- `ACCEPTED_RULES_SHA`, `STORY_BASE_SHA`, expected Story parent/remote tip; Review also requires `REVIEW_BASE_SHA` and `STORY_SHA`.
- every prerequisite as a full immutable commit SHA.
- exact full-Story and current-segment production, debug/fixture, test, documentation, and governance paths, mirrored in one pinned structured scope manifest outside every repository worktree.
- run-only paths that MUST NOT be edited.
- Story-specific exclusions and stop conditions.
- expected file, line-churn, method/type, abstraction, and dependency envelope.
- mandatory, optional, and forbidden validation.
- external acceptance and evidence requirements, or explicit `none`.
- applicable accepted decisions and current-status index.
- current-status strategy: `unchanged_stable`, `merge_stable_update`, or an explicit planning stop.
- protected-manifest path and SHA-256, expected primary protected root and capture HEAD, exact adopted user-overlay paths, and the user's exact adoption authorization reference or explicit `none`.
- scope-manifest path and SHA-256. Its full-Story and current-segment JSON entries bind exact path, `add|modify|delete`, requiredness, category, and responsibility; each scope set has per-category integer `expected` and `hardMax` envelopes, and the current segment has an immutable base SHA.
- complete ignored-path classification: exact protected ignored roots and exact ephemeral generated-output roots, each or both explicitly `none` when empty.
- total agent-tree limit, role allocation, and exact write ownership.

Open-ended authorization such as “related files”, “necessary tests”, or “update docs as needed” is invalid. If an exact path cannot yet be named, planning is incomplete.

Scope entries are repository-relative exact file paths with an intended `add`, `modify`, or `delete` operation, required/optional flag, category, and responsibility. The structured scope manifest is pinned by SHA-256 and is the mechanical source for final name-status, required-entry, commit-history touched-path, and hard-max checks. Its `fullStory` scope describes the complete Story from `STORY_BASE_SHA`; its `currentSegment` scope describes only the current Dev or Repair segment from its pinned base. Each scope set has one integer `expected`/`hardMax` envelope for every canonical category; `hardMax` cannot be open-ended and applies to the union of paths touched by commits in that range, not only the final tree delta. A Repair cannot mark its impact scan `not_applicable`.

The scope-manifest JSON schema is version 2. Its exact root properties are `schemaVersion`, `fullStory`, and `currentSegment`; both scope objects have exactly `baseSha`, `entries`, and `envelopes`. `fullStory.baseSha == STORY_BASE_SHA`. Canonical categories are `production`, `debug`, `test`, `docs`, and `governance`; every category has exactly one envelope in each scope set, including zero-valued envelopes. Each entry has exactly `path`, `operation`, `required` (JSON boolean), `category`, and non-empty `responsibility`. `operation` describes that range's final net delta when the path remains changed; intermediate commits are separately constrained by exact path and accept only Git `A/M/D` statuses. A current-segment path must also exist in the full-Story scope with the same category. For a new Story, the two scope sets and envelopes are identical and both base SHAs equal `STORY_BASE_SHA`; for a Repair, `currentSegment.baseSha == EXPECTED_STORY_PARENT` and the segment entries are the exact Repair authorization.

Generated prompts MUST contain no unresolved `{{PLACEHOLDER}}`, `TBD`, `TODO`, or unselected alternatives.

## 3. Orthogonal status model

Do not encode unrelated facts in one free-form status string. Track them independently:

| Dimension | Allowed values |
|---|---|
| Lifecycle | `planned`, `implementing`, `implemented`, `completed`, `superseded`, `cancelled` |
| Review | `not_started`, `in_progress`, `blocked`, `changes_requested`, `passed` |
| Merge | `not_merged`, `pending_merge`, `merged` |
| Gate | `locked`, `prerequisite_gated`, `pending_external_acceptance`, `satisfied`, `blocked` |
| Evidence | `not_required`, `pending`, `partial`, `passed`, `invalidated` |
| Archive | `current`, `historical`, `sealed` |

`merge=merged` is computed from Git; a document cannot declare it into existence. `review=passed`, `evidence=passed`, and a pushed branch do not imply merge. A downstream gate is satisfied only when every named immutable prerequisite is an ancestor of synchronized `main` and every independent acceptance predicate is satisfied.

Human-facing phrases such as `implemented / needs review` MAY summarize these dimensions in a handoff, but MUST NOT replace the underlying facts.

## 4. Merge-stable truth

Long-lived current documents MUST remain true both before and after an allowed merge.

- A branch-local development status in a Story document MUST be labeled as a pre-merge snapshot, not unconditional project truth.
- A current document MUST NOT unconditionally say “needs Review”, keep the next gate locked, or say “the only next step is Review” if those statements become false immediately after the same content is merged.
- A conditional transition MAY depend on objective Git and external-acceptance facts. It MUST NOT depend on “all documents agree”, because that creates a self-referential lock.
- A Story document cannot embed its own final SHA. The immutable tip belongs in the handoff and Review receipt.
- Story completion MUST include a post-merge truth simulation. A stale current instruction is a finding, not a reason to plan a routine recursive docs-sync.

Docs-sync is an exceptional Repair for legacy or externally introduced inconsistency. It is not the normal lifecycle mechanism and MUST NOT recursively require another closeout.

The repository MUST nominate one current-status index in accepted governance. That index stores stable sequencing and gate predicates; merge status itself is computed from Git. A Story that changes lifecycle or a downstream gate MUST either prove the index remains stable unchanged or include an exact merge-stable index update in its allowlist. Reviewer-authored merge-time edits are forbidden.

## 5. Preflight and user-worktree protection

Before branch creation, editing, or stateful validation, the main agent MUST:

1. run `git fetch --prune origin`;
2. resolve exact accepted base and prerequisite SHAs;
3. verify every prerequisite with `git merge-base --is-ancestor`;
4. record current branch, HEAD, status, staged paths, tracked dirty paths, ordinary untracked paths, classified ignored paths, and hashes for pre-existing user files;
5. confirm the index is empty;
6. distinguish paths explicitly adopted into this Story from paths that remain protected user assets.

All governed Git reads run with replacement objects disabled. Any `refs/replace/*` entry or legacy `info/grafts` file fails closed; a displayed raw SHA must always resolve to its original commit, tree, and message objects.

All pre-existing dirty and untracked content belongs to the user by default, not only files on a fixed denylist.

The scope manifest and protected manifest MUST be stored outside every worktree's content area, retained through PostMerge, and pinned by their own SHA-256 values. The one allowed metadata exception is the dedicated `codex-story-gates` child of the verified Git common directory; other paths lexically inside a worktree are forbidden. An automatically cleaned temporary path is invalid. The entire existing ancestor chain must contain no symlink/reparse point. Capture MUST refuse an existing output path rather than overwrite it. The protected manifest is captured before editing; its primary root, capture HEAD, scope-manifest identity, adopted paths, and authorization reference are also supplied independently by the prompt and compared exactly. The manifest records the complete remaining tracked-dirty/ordinary-untracked leaf-file inventory with existence, type, length, and content hash, and rejects symlinks/reparse points.

Ignored content is not silently exempt. Every ignored leaf file MUST fall under an exact protected-ignored root or an exact ephemeral generated-output root. Protected ignored roots record root kind plus complete leaf inventory and hashes; ephemeral roots may change because they are rebuildable outputs. Broad roots that mix user assets with generated output are invalid. Additions, deletion, movement, type change, hash change, an unclassified ignored leaf, or a mismatch with the prompt fails. Git-irrelevant empty directories are outside the Story contract.

Adopting a pre-existing user change into a Story requires an exact path list, a required current-segment entry whose operation matches the captured overlay, presence in the full-Story scope, proof that each path was tracked-dirty or ordinary-untracked at capture, and an explicit user authorization reference. Adoption is never inferred from a broad allowlist. An ignored path is never adopted in place: it remains an exact protected input or an ephemeral output; an authorized Story may instead add a separately allowlisted destination derived from that input.

The workflow MUST NOT use `git add .`, `git add -A`, `git commit -am`, stash, reset, clean, rebase, force push, or deletion/movement to hide an inconvenient worktree. Only exact allowlisted paths may be staged.

Before commit and again before merge:

- cached paths MUST be an allowlisted subset and match the intended commit exactly;
- protected path hashes and existence MUST be unchanged;
- the index MUST be empty after commit;
- Story scope MUST be computed with a merge-base/three-dot diff;
- the union of paths touched by every commit from `STORY_BASE_SHA..STORY_SHA`, and separately from the current segment base to `STORY_SHA`, MUST remain inside its corresponding pinned scope set even when a path is restored before the final tip;
- generated, secret, ignored evidence, and user assets MUST NOT enter the Story.

If the exact Story or integration tree cannot be verified without contaminating it with user changes, the agent MUST stop or use an explicitly approved isolated clean worktree. A dirty allowlist does not make executable test evidence equivalent to a clean immutable tree. An immutable validation tree contains no tracked changes, staged changes, ordinary untracked files, or unclassified ignored files; any present protected ignored input must match the pinned inventory, and only declared ephemeral output roots may vary.

## 6. Development and Repair

### Development

- A new Story branch MUST start at the declared `STORY_BASE_SHA`.
- New Story commits after `STORY_BASE_SHA`, and new Repair commits after the declared Repair parent, MUST form a linear no-merge segment. Development MUST NOT merge another branch into the Story.
- Every final Story path and every path touched by an intermediate commit MUST be authorized by the full-Story scope. Every path touched by the current Repair segment MUST additionally be authorized by the segment scope. A final restoration does not erase Git-object exposure or make an out-of-scope edit acceptable.
- The Dev agent owns implementation, focused integration, complete Story validation, exact staging, commit, and Story-branch push.
- Dev MUST NOT merge `main`, claim Review passed, claim merge, or unlock a downstream Story.
- Exceeding the hard scope envelope, needing a new unapproved owner/interface/wrapper/seam/dependency, or discovering contradictory accepted contracts requires a stop before structural expansion.
- Completion MUST report planned versus actual scope and map each acceptance assertion to implementation plus test/evidence.

### Repair

“Minimal change” means the smallest causal closure that corrects the finding and keeps its direct consumers, contract tests, evidence assertions, and compatibility surfaces coherent. It does not mean “only the cited line/file,” and it does not authorize opportunistic refactoring.

Before editing, a Repair MUST scan:

1. the directly cited finding locations;
2. production consumers;
3. direct and contract tests;
4. documentation and evidence assertions;
5. compile-time or runtime compatibility surfaces.

The prompt distinguishes direct finding files from transitive files required to keep compilation or the accepted contract coherent. The full-Story scope preserves the complete candidate delta, while the current-segment scope is the Repair's exact edit authorization. If an unapproved consumer or conflicting run-only test appears, stop and request a newly pinned exact scope expansion before editing. Do not add message checks, caller checks, test-only production branches, or new abstraction layers to avoid that gate.

After two unsuccessful Review rounds, the manager MUST perform a root-cause and same-risk-axis audit before issuing another point Repair. Correct-course is required when ownership, core abstraction, data model, or cross-module structure must change; an isolated small defect does not mechanically force correct-course.

## 7. Subagents in a shared worktree

All agents share the same filesystem, branch, index, build outputs, and connected runtime state.

- In `manual_prompt` mode, the active Dev or Review role's main agent owns its role and may use bounded delegation. In `supervised_automatic` mode, the management supervisor is read-only and `$supervised-story-delivery` dispatches every mutating repository or external action to an explicitly authorized role agent.
- The management supervisor decides whether read-only exploration improves speed or quality; the user does not need to request individual explorers. Automatic Dev/Review/Repair/integration requires the user's Story or finite-sequence authority and does not extend beyond it.
- The prompt sets a total agent-tree limit. Child agents MUST NOT delegate again unless the main agent explicitly approves it within that total.
- Only the currently authorized Dev/Repair writer or integration role may change branch, HEAD, index, worktrees, commits, pushes, or merges. A preflight/gate role may fetch declared remote-tracking refs and create pinned manifests outside worktree content, but it MUST NOT change branch, HEAD, index, tracked content, or user-owned state. There is never more than one writer. Stash remains forbidden unless a later explicit accepted rule narrowly authorizes it.
- Read-only agents MUST NOT mutate files or global runtime state.
- Editing agents require non-overlapping exact file ownership and MUST report every touched path.
- Full build/toolchain integration, runtime control, artifact installation, final validation, staging, and Git delivery are serialized by the active role owner unless the Story contract explicitly proves isolation.
- Every agent receives the same pinned accepted base and, during Review, the same `STORY_SHA`.
- The active role owner waits for all started explorers, integrates their evidence, reviews every diff, and reruns the authoritative validation. In supervised mode the management supervisor independently verifies returned immutable facts without mutating repository or external state.

Delegation assists a Dev or Review workflow; it does not replace the independent Dev/Review separation.

## 8. Validation profiles

Every Story chooses mandatory, optional, and forbidden validation. Profiles are composable baselines, not fixed device or command names.

### `DOCS_ONLY`

- exact Story scope and `git diff --check`;
- strict UTF-8, BOM/NUL, headings, lists, tables, links, and fence structure as applicable;
- cross-document and post-merge stable-truth review;
- no SDK, build, emulator, or external-system requirement unless the docs change executable configuration.

### `PURE_LOGIC`

- finding/feature-focused tests;
- affected-module tests;
- full unit suite and compile/check steps selected by the repository contract;
- exact immutable-tree scope.

### `PLATFORM_BUILD`

- applicable `PURE_LOGIC` checks;
- platform boundary tests, assemble/build, lint, and repository checks;
- concrete exception, permission, lifecycle, and cleanup paths matching the Story risk.

### `UI_RUNTIME`

- applicable build checks;
- a prompt-selected runtime environment and exact flow;
- UI hierarchy/screenshot/log evidence and crash/hang/fatal-runtime checks;
- no claim beyond that runtime environment.

### `EXTERNAL_ACCEPTANCE`

- applicable code/build validation;
- explicitly named environment supplied by the Story prompt;
- artifact/source identity, repeatable steps, raw evidence, limitations, and user observations separated from automated facts;
- no lower evidence layer may substitute for the external acceptance.

### `EVIDENCE_ONLY`

- production behavior changes are forbidden;
- allowed fixture/harness changes MUST NOT alter the behavior being accepted;
- a production finding returns to the responsible implementation Story, then requires a new artifact and rerun of every affected gate.

Environment bootstrap instructions are injected by the Story profile. Ignored local scripts MUST NOT be executed blindly; inspect them first or set required variables explicitly. A docs-only task does not execute an ignored environment script.

## 9. Evidence identity and invalidation

Every artifact, runtime, or external behavior record MUST include, as applicable:

- evidence kind and assertions;
- `EVIDENCE_SOURCE_SHA`;
- artifact SHA-256, byte size, build variant/configuration, application/artifact identity, and entry point;
- toolchain and environment identity;
- runtime/device identity, OS/API/version facts known and unknown, timestamp, and timezone;
- exact commands or manual steps;
- raw evidence location and limitations.

Build success, unit tests, a simulated runtime, a physical environment, and a manual observation are separate evidence kinds and do not substitute for one another. A hash proves artifact identity, not behavior.

Any relevant production, debug/harness, manifest, dependency, or build-configuration change invalidates older behavior evidence. A later docs-only commit does not invalidate it only when a mechanical diff proves the executable tree unchanged from `EVIDENCE_SOURCE_SHA` to `STORY_SHA`.

Source search, helper existence, or a potentially no-op invocation is not behavior coverage.

## 10. Development handoff gate

Before reporting `implemented`, Dev verifies:

- `HEAD == origin/<story-branch> == STORY_SHA` and divergence is `0 0`;
- all required prerequisite SHAs remain ancestors of the accepted base;
- three-dot Story paths are within the exact allowlist;
- validation ran against the stated executable source and results include counts/exit status;
- evidence identity is complete or explicitly not required;
- protected user files and excluded artifacts are unchanged;
- index is empty;
- post-merge truth simulation passes.

The handoff includes exact commits, paths, planned/actual envelope, acceptance mapping, validation, evidence limits, Git gates, risks, and the next independent Review gate.

## 11. Independent Review

Review is read-only with respect to tracked Story content. If the reviewer edits a tracked file, creates a Repair commit, or advances the Story tip, that Review permanently loses approval and merge authority. The new tip requires a new independent Review.

At Review start:

1. fetch and pin `REVIEW_BASE_SHA` and `STORY_SHA`;
2. verify the local and remote Story refs both equal `STORY_SHA` exactly;
3. read accepted rules from `REVIEW_BASE_SHA`;
4. materialize and execute the accepted validator blob from `REVIEW_BASE_SHA`, outside the candidate tree; a changed candidate validator is only review subject/regression input and cannot produce its own gate PASS;
5. reconstruct prerequisites from the prompt, accepted decisions/readiness, Story acceptance, and evidence contract, taking their union;
6. compute full scope and commit-history touched paths from `STORY_BASE_SHA..STORY_SHA`, plus current-segment scope and history from the manifest's pinned segment base;
7. review and validate a clean tree whose HEAD is exactly `STORY_SHA`.

A prompt omission cannot erase an accepted or technically inherent gate. All read-only explorer agents use the same pinned pair and report it back.

Review findings use `blocker`, `must-fix`, `should-fix`, and `nice-to-have`. Any blocker, must-fix, or should-fix prevents merge. Inability to complete mandatory verification is `review blocked / verification incomplete`, not a code finding and not a pass.

`review=passed` is orthogonal to evidence and merge: it may coexist with `evidence=pending` and `merge=not_merged` once semantic Review and mandatory Story-tree verification pass. A later integration or evidence failure must block or revise the affected status. Only successful PostMerge plus all independent gates may set `merge=merged` and a downstream gate to `satisfied`.

Before merge, Review MUST simulate post-merge truth and rerun exact-tip equality. Any new Story commit invalidates the Review.

An authorized merge commit is the machine-readable Review receipt. Its message contains these exact trailers:

- `Story-Id: <id>`
- `Story-Tip: <full SHA>`
- `Reviewed-Base: <full SHA>`
- `Review-Mode: independent`
- `Review-Result: passed`
- `Evidence-Gate: not_required|passed`
- `Scope-Manifest-SHA256: <64 uppercase hex characters>`
- `Workflow-Contract: <version>`
- `Workflow-Validator: <accepted validator Git blob SHA>`

The nine trailers are one contiguous final block, separated from a non-empty subject/body by a blank line, with no blank lines inside the block. Governance keys are case-insensitively unique. They bind the assertion to an exact base, Story, prior accepted contract/validator, scope authorization, and evidence outcome. They do not replace the reviewer's semantic report, but their absence or mismatch prevents merge/post-merge PASS.

## 12. Safe integration and merge transaction

Review MUST NOT test a mutable branch name or merge directly into a dirty local `main` before integration succeeds.

1. Prefer an approved isolated clean integration worktree at `REVIEW_BASE_SHA`. A temporary branch in the primary worktree is allowed only when its tracked tree and index are completely clean; a branch alone is not isolation.
2. Merge exact `STORY_SHA` with `--no-ff --no-commit`; do not merge the branch name. After Review authorization, create the merge commit with the required receipt trailers.
3. On conflict, abort. Do not repair conflicts inside Review.
4. Verify the integration commit has exactly two parents: parent 1 is `REVIEW_BASE_SHA`, parent 2 is `STORY_SHA`. Its tree MUST equal Git's clean merge result for that exact pair; manual conflict fixes or extra edits require a new Story tip and Review.
5. Run all mandatory integration validation on that exact merge tree.
6. Confirm the integration tree and index are clean and post-merge text remains true.
7. Fetch again. If `origin/main` no longer equals `REVIEW_BASE_SHA`, or the remote Story ref no longer equals `STORY_SHA`, discard the approval for that integration base and repeat integration-sensitive Review.
8. Push the exact merge commit to `refs/heads/main` with an ordinary non-force push.
9. A rejected push MUST NOT trigger force, rebase, or reuse of the stale integration result.
10. Only after remote acceptance may local `main` be fast-forwarded safely.

Post-merge checks require:

- `main == origin/main == MERGE_SHA` and divergence `0 0`;
- exact first and second parents as above;
- `STORY_SHA` is an ancestor of both local and remote main;
- index is empty and protected user state is unchanged.

Only then may Review report `reviewed / merged` or satisfy a downstream gate.

## 13. Historical and sealed material

Historical text does not generate current tasks. Existing accepted archive classifications remain in force. Any new classification or classification change MUST use an accepted exact path manifest with one classification per path:

- `sealed`: read-only historical evidence;
- `adopted`: current asset reused under its accepted contract;
- `adapted`: current asset with explicitly allowed evolution;
- `pending_retirement`: still present only for an accepted transition;
- `prohibited_merge`: retained outside accepted main history.

A sealed path in a Story delta without exact authorization is a finding. A directory or old phase name alone does not prove classification.

Legacy natural-language classifications SHOULD migrate through a separate scoped governance/planning Story. Until that migration is accepted, the latest accepted decision remains authoritative; this contract does not silently unseal or reclassify existing material.

## 14. Prompt packaging

A generated copy-ready prompt uses exactly one outer four-backtick `text` fence. All required content is inside it. Inner Markdown MAY use shorter fences but MUST NOT contain an equal-or-longer fence that closes the outer block.

The manager validates zero unresolved placeholders before sending. Prompt packaging is presentation; the canonical contract remains the authority.

## 15. Governance-change policy

- Governance changes use a dedicated Story and exact governance allowlist. The contract, thin templates, and validator migrate in the same Story when their interfaces change.
- They are reviewed under the prior accepted contract and validator and cannot self-approve. Review and PostMerge run a validator materialized from the pinned review base; the candidate validator is tested as changed code only. Bootstrap version 1.0 has no accepted validator, so its independent Review uses only the prior accepted `AGENTS.md` and role templates and MUST NOT report a candidate-script gate PASS.
- They MUST NOT change product scope, architecture, current feature status, or unlock a product Story unless separately authorized.
- Stable normative rules live here; role templates stay thin.
- Optional ignored skills are advisory methods only. Project-specific instructions from another project are ignored, and no local skill may change authority, scope, gate, evidence, or write permissions.
- Emergency override requires explicit user authorization, a recorded reason and scope, and a later independent Review. It never becomes the default path.

Every governance change MUST regression-check and report: moving Story tip, moving main, non-empty index, protected dirty/untracked inventory, protected deletion, self-fix, integration conflict, extra merge-tree edits, push rejection, stale evidence, post-merge truth, sealed-path authorization, shared-worktree delegation, an intermediate commit touching then restoring an unauthorized path, Repair-segment scope escape, candidate-validator self-use, and its own prior-contract Review.

## 16. Mechanical versus semantic checks

`scripts/validate-story-gate.ps1` reports a **mechanical subset PASS** only. It first proves that its running file matches the validator blob at the pinned accepted-rules SHA. It fetches explicit remote refs; separates accepted-rules, historical Story base, and current-segment base; checks exact refs and linear ancestry, clean immutable trees, pinned full-Story/segment operations, required entries, per-commit touched-path unions and hard maxima, index, optional materialized-prompt placeholders, protected and ignored inventories, exact merge tree/topology, canonical case-insensitive-unique Review-receipt trailers, the fixed accepted contract/validator identities, and pre-push/post-merge state. It deliberately does not decide whether required prompt fields are semantically complete, architecture is sound, tests are meaningful, evidence is sufficient, or prose is merge-stable. Those remain explicit manager and independent Review responsibilities.
