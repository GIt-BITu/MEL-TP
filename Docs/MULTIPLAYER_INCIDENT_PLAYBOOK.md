# Multiplayer critical-incident playbook

## Purpose and boundary

This is a one-maintainer, first-hour playbook for a critical defect discovered
after releasing multiplayer. Its order of priority is: protect people and
data, stop further harm, restore a safe service, and only then repair the
feature. It applies the provided MEL guides' distinction between urgent
containment and later, test-backed improvement; the guides are reference
material, not operating instructions for this repository.

There is an important present-state constraint: this repository contains a
single-player Java Swing client only. It has no multiplayer transport, server,
authentication, database, feature-flag service, telemetry, deployment
configuration, or online state. Therefore this document cannot diagnose a
specific live fault from this checkout. It defines the safe response and the
minimum architecture and deployment controls required before multiplayer is
released.

No finite runbook can enumerate every defect. The scenario matrix covers the
operational failure classes. A new or ambiguous symptom follows the same safe
fallback: contain the feature, preserve evidence, classify impact, then make a
small reversible change.

## Non-negotiable rules for a lone maintainer

- Do not debug by changing production data, deleting logs, or trying patches
  directly on the live release.
- Do not make an unrelated refactor during the incident. A rollback or
  feature disable is safer than a clever fix.
- Prefer the narrowest reversible control: disable matchmaking, then new
  sessions, then in-match commands, before disabling the whole game.
- Treat a possible authentication bypass, data disclosure, payment or ranking
  corruption, and irreversible data loss as a security/data incident until
  disproved. Disable the affected path first.
- Keep a timestamped incident log: observed fact, source, action, reason,
  result, release ID, flag/configuration value, and next review time. Facts
  and guesses must be visibly different.
- Communicate only what is known. One maintainer cannot both investigate and
  answer every message; publish a short status update and a next-update time.

## What to do first: 60-minute timeline

The timeline is a target, not a reason to wait. Skip ahead when the impact is
already known.

| Time | Goal | Actions | Exit condition |
|---|---|---|---|
| 0-5 min | Contain harm | Declare the incident, freeze deployments and schema changes, capture the release/build ID and flag state, save dashboards/log queries, and disable multiplayer through a server-side kill switch. If no kill switch exists, rollback the multiplayer backend/route to the last known-good server while preserving evidence. Leave single-player available if it is isolated. | New multiplayer impact has stopped or a documented reason explains why it cannot be stopped. |
| 5-15 min | Classify severity | Determine whether the problem is security, data integrity, availability, game correctness, or a client-only defect. Bound the affected time window, release versions, regions, match IDs, and user population. Check authentication failures, error rate, latency, crash rate, queue depth, duplicate commands, invalid state transitions, and storage errors. | A severity and containment scope are recorded; unknowns are explicit. |
| 15-30 min | Choose recovery | Use the decision matrix below. Confirm rollback compatibility before rolling back. For data issues, stop writes before restoring or repairing. Reproduce only against a captured/sanitized state or staging, never by replaying risky traffic on production. | One recovery path has an owner, a verification query, and a rollback path. |
| 30-45 min | Make the smallest safe change | Deploy a flag change, rollback, or tightly scoped hotfix. Run a smoke test: create a match, join it, make valid and invalid moves, reconnect, complete/abort it, and verify no unintended score/reward/data writes. For a security issue, also confirm the denied path is truly denied. | Health and correctness checks meet their pre-set thresholds for the affected scope. |
| 45-60 min | Stabilize and hand over | Keep multiplayer disabled or limited until observation is clean. Publish status, save the incident timeline and artifacts, open follow-up work with priorities, and schedule a blameless review. Do not re-enable merely because the first error disappeared. | A documented decision says either “re-enable gradually” or “remain disabled,” with measurable conditions. |

If the fault is not contained by 15 minutes, keep the feature disabled and
escalate through the product/security/on-call contacts. A single maintainer
should not trade prolonged data or security exposure for feature availability.

## Severity and immediate decision tree

Classify from the worst credible impact, not the easiest-to-fix symptom.

```text
Possible credential exposure, unauthorised access, or data disclosure?
  Yes -> disable multiplayer/authenticated endpoint; preserve evidence;
         revoke or rotate affected credentials; follow security escalation.
  No  -> Are durable match, score, reward, or account data being corrupted?
           Yes -> stop writes; disable the relevant command/reward flow;
                  snapshot evidence; reconcile from an authoritative log/backup.
           No  -> Is the service broadly unavailable or crashing?
                    Yes -> rollback or disable the feature; protect capacity.
                    No  -> Is a match unfair, inconsistent, or stuck?
                             Yes -> pause ranking/rewards and new matches;
                                    terminate/repair only affected matches.
                             No  -> Limit by client/version/region and repair
                                    with a canary after a reproducible test.
```

The highest level applies whenever the answer is uncertain. For example, a
desynchronised match is a data-integrity incident if it can grant a reward.

## Scenario matrix

| Observed class | Examples | First safe action | Recovery and verification | Prevention |
|---|---|---|---|---|
| Security or privacy | Account takeover, a player acts as another, exposed match data, injection, leaked secret | Disable the endpoint/feature and any compromised credential; preserve immutable audit evidence. | Rotate secrets/tokens, invalidate affected sessions, patch, and have security review access logs before gradual re-enable. Do not claim affected users or scope without evidence. | Server-side authorization on every command, TLS, secret manager, least privilege, dependency and security scanning, audit logs, threat-model review. |
| Integrity or financial/ranking impact | Duplicate rewards, incorrect score, lost moves, impossible board state | Fence writes for the affected command, pause ranks/rewards, and capture a consistent snapshot. | Reconcile from append-only command/event history and a known backup; use idempotent compensations, not ad-hoc SQL. Verify totals and sample affected matches before reopening writes. | Server authority, transaction boundaries, idempotency keys, state revisions, immutable event/audit log, tested backup restore. |
| Full outage or crash loop | All joins fail, server restart loop, connection pool exhaustion | Roll back the last server/config change or disable matchmaking. Rate-limit at the edge if load is amplifying failure. | Verify login, create/join, move, reconnect and completion on the previous version; monitor errors, saturation and latency before opening traffic. | Canary deploys, autoscaling/capacity tests, dependency timeouts and circuit breakers, liveness/readiness checks, load tests. |
| Partial outage | One region, platform, app version, or a subset of matches fails | Route/disable the affected cohort only; stop automatic rollout. | Roll back or gate that cohort and compare its metrics with an unaffected control group. | Version-aware routing, per-region rollout, client compatibility telemetry, feature flags scoped by cohort. |
| Desynchronisation or unfair play | Players see different cells, stale moves overwrite new moves, cheating client sends impossible move | Stop new ranked matches and server-side reject unsafe commands; do not trust client board state. | End or restore affected matches from the authoritative event stream; validate sequence/revision checks and replay a failing trace in staging. | One authoritative server state per match, serialized command processing, monotonic revision number, command IDs, deterministic replay, anti-cheat validation. |
| Stuck lobby, match, or reconnect loop | Ghost player, match never starts/ends, reconnect duplicates a player | Pause the affected queue and expire stuck sessions safely; do not repeatedly retry without a cap. | Sweep with an idempotent reconciliation job and verify participant/state invariants. | Explicit match state machine, deadlines, leases/heartbeats, bounded retries, idempotent join/leave/reconnect. |
| Ordering, duplication, or loss | Double-click registers twice, delayed packet undoes a later move, message broker redelivers | Disable or rate-limit the command if it changes durable state. | Deduplicate by command ID, reject stale state revisions, replay the event sequence against a copy, and reconcile only proven anomalies. | At-least-once tolerant handlers, idempotency store, optimistic concurrency control, ordered per-match processing, chaos tests. |
| Dependency failure | Identity, database, queue, DNS, certificate, or cloud region fails | Degrade safely: no new multiplayer sessions or read-only status, rather than accepting unverified commands. | Follow dependency rollback/failover procedure and confirm no split-brain or duplicate writes. | Health checks that distinguish dependency failure, tested failover, connection limits, certificate-expiry alerts, vendor runbooks. |
| Client presentation or protocol bug | Only one released client misrenders state or sends invalid payload | Block that feature/client version server-side or direct it to single-player; do not rely on a desktop update reaching everyone immediately. | Release a compatible client fix, verify server accepts supported old versions only, then retire the bad version by policy. | Versioned protocol, server-side capability negotiation, crash reporting by build, forced-update/feature-disable policy. |
| Unknown or unreproducible symptom | A single credible report, sparse logs, contradictory telemetry | Disable or narrow the feature if impact could be material; preserve request IDs, timing, version, and sanitized payload shape. | Add safe observability, reproduce in staging from a trace, and re-enable only after a hypothesis is disproved or fixed. | Correlation IDs, structured logs, traces, invariant metrics, privacy-safe replay fixtures, alert playbooks. |

## Evidence to collect before it expires

Record the following once, then avoid gathering personal data that is not
needed for recovery:

- incident start/end times in UTC, reporter, observable symptom, and scope;
- client build/version/platform, backend build/commit, deployment ID, region,
  protocol version, relevant configuration and feature-flag state;
- request/trace/correlation IDs, match IDs represented as protected identifiers,
  error stacks, latency/error/saturation graphs, and dependency health;
- command type, command ID, expected state revision, received revision, and
  resulting state/rejection reason; never log credentials or raw secrets;
- database migration version, backup/snapshot point, queue offsets or event
  positions, and the exact rollback candidate;
- the release artifact, configuration diff, and commands run during recovery.

Preserve raw evidence under the organisation's retention/access policy. Do not
paste production records into tickets, chat, tests, or this repository.

## Safe deployment and rollback rules

### Before any hotfix

1. Create an immutable release with a build/commit identifier and a short
   change description. Freeze all unrelated changes.
2. Make the server compatible with both the current and previous supported
   client protocol. A desktop client already downloaded cannot be rolled back
   by GitHub Actions; it must be safely rejected, limited, or supported by the
   service.
3. Separate code deployment, schema migration, and feature enablement. Use an
   expand-contract migration: add compatible fields/tables first, deploy
   compatible code, backfill, then remove old structures only in a later
   release. Never couple an irreversible migration to the first multiplayer
   rollout.
4. Confirm a server-side kill switch exists for at least: new matchmaking,
   joining, in-match commands, rank/reward writes, and the whole multiplayer
   feature. Test each switch before relying on it.
5. Take/verify a restorable backup and prove the rollback path in staging.
   “Backup exists” is not proof it can be restored safely.

### Rollout pattern

Deploy the fix dark, run integration smoke checks, enable it for internal
accounts, then a small canary cohort. Compare canary metrics with a control
group over a defined observation window. Expand by region/version only while
all guardrails pass; stop automatically if error, integrity, latency, or
security thresholds breach. Keep the previous server release and compatible
schema available until the canary is complete.

The current GitHub Actions workflow packages the desktop client for three
operating systems and proves a launcher exists. That is useful build
validation, but it does **not** deploy or validate a multiplayer service. A
multiplayer release pipeline needs separate server image/artifact provenance,
staging integration tests, migration checks, canary controls, health gates,
and a documented rollback action.

## Architecture required before re-enabling multiplayer

Do not share the current `GameBoard` instance between Swing clients or server
threads. It is an in-memory single-player model with mutable state and no
network or concurrency contract. Treat it as a rules engine only.

A maintainable multiplayer design should establish these boundaries:

```text
untrusted client command
        -> authenticated API / protocol adapter
        -> per-match serialized command handler
        -> authoritative game rules and persistent match state
        -> versioned event/result -> client rendering
```

- The server owns the canonical match state, validates player identity and
  turn/role, and validates every action against the latest revision. Clients
  submit intents; they never submit a trusted board, score, or outcome.
- Each command has a match ID, player ID, protocol version, command ID, and
  expected state revision. The result is idempotent: a retry returns the
  original result rather than applying the move twice.
- Process each match serially or through equivalent optimistic concurrency
  control. Persist state transitions atomically with the event/audit record.
- Use explicit lobby/match lifecycle states such as `CREATED`, `WAITING`,
  `ACTIVE`, `FINISHED`, `ABORTED`, and `EXPIRED`, with permitted transitions
  and timeouts. Never infer lifecycle from UI activity.
- Store a replayable seed plus ruleset version, or the generated board/state
  snapshot, before the first move. The current first-click mine placement is
  intentionally random; multiplayer recovery must be reproducible.
- Keep transport DTOs, database records, authentication, and UI code outside
  `GameBoard`. Add a new testable application/service layer instead of putting
  network calls into `Board` or `Mines`.

## Tests and gates that prevent a repeat

`mvn verify` and the present unit coverage gate remain necessary but are not
sufficient for multiplayer. Before enabling online play, add and enforce:

- rule and property tests for every legal/illegal command and game-state
  invariant, including no negative counters, one terminal outcome, and no
  state change after completion;
- deterministic replay tests using a recorded seed/state and command stream;
- API contract tests for current and previous protocol versions;
- integration tests using the real persistence, authentication, and queue
  boundaries in an isolated environment;
- concurrency tests for simultaneous moves, duplicate/reordered/dropped
  messages, timeout, reconnect, server restart, and partial failure;
- authorization, input-fuzzing, dependency, migration/rollback, backup-restore,
  load, and fault-injection tests;
- an end-to-end smoke test after each canary and a deployment gate that fails
  closed when it cannot run.

Coverage is a guardrail, not proof of distributed-system correctness. The
release condition is passing tests plus observed staging/canary invariants and
a rehearsed rollback.

## Observability and re-enable criteria

Measure and alert by release, region, client version, protocol version, and
feature cohort. At minimum track: login/join/move success rates; end-to-end
latency; disconnect/reconnect rate; active/stuck matches; retry and duplicate
command rates; stale-revision/invalid-transition rejections; server errors;
database/queue saturation; and score/reward reconciliation discrepancies.

Set numeric baselines and stop thresholds before release. Do not invent them
during the incident. Re-enable only when all of the following are true:

1. The root cause is fixed or a durable guard prevents the harmful path.
2. The fix has focused regression tests for the observed failure and the
   relevant scenario matrix row.
3. Rollback/kill-switch, backup/restore, and compatible-client behavior were
   tested for this release.
4. A controlled cohort meets its error, latency, integrity, and security
   thresholds for the declared observation window.
5. The incident timeline, residual risks, owner, and follow-up date are
   documented. Risky unknowns mean the feature stays limited or off.

## Communication templates

**Initial status:** “We are investigating an issue affecting multiplayer. We
have limited the feature to protect matches and data. Single-player is
[available/unaffected if verified]. Next update by [UTC time].”

**Recovery status:** “A mitigation is active for multiplayer. We are verifying
match integrity and monitoring the service before restoring access gradually.
We will update by [UTC time].”

**Closure summary:** “Multiplayer was affected from [start] to [end]. We
[contained action] and [recovery action]. [Scope only if verified]. We added
[guard/test/deployment control] and will track [remaining action] by [date].”

Do not publish root-cause speculation, personal data, exploitable details, or
security remediation steps before the appropriate review.

## After the first hour: corrective work

Hold a blameless review while the evidence is fresh. Build a factual timeline,
identify detection and decision gaps, and turn each contributing condition into
an owned, testable action with a due date. Prioritise security and reliability
first; then add coverage/observability; then make maintainability
improvements. Keep the hotfix small and create separate reviewed changes for
architecture work. The incident is not closed until the prevention controls
are verified in a drill or release rehearsal.

## Reference basis

The attached English and French MEL guides informed the incident structure:
rapid criticality/impact assessment; security, reliability, then coverage as
urgent priorities; rollback and feature flags for containment; and complete
non-regression work after stabilisation. This playbook adapts those ideas to a
production multiplayer release; it does not treat the guides' educational
text as repository requirements.
