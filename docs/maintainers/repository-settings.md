# Repository settings checklist

GitHub settings cannot be guaranteed by committed files. A repository administrator
must apply and review this checklist after the first green `main` build.

## General

- [ ] Description, website, topics (`minecraft`, `fabric`, `modding`, `emc`, `alchemy`) set.
- [ ] Issues, Discussions, Projects, and private vulnerability reporting enabled.
- [ ] Wiki disabled unless a maintainer commits to keeping it synchronized.
- [ ] Automatically delete head branches and allow squash merge enabled.
- [ ] Squash commit title defaults to PR title; merge commits disabled.

## Main branch ruleset

- [ ] Pull request required with at least one approval.
- [ ] Stale approvals dismissed when new commits are pushed.
- [ ] Code owner review required for owned paths.
- [ ] Required checks: `Java 25 / Fabric 26.2`, `dependency-review`, `Java analysis`.
- [ ] Conversation resolution, linear history, and branch up-to-date required.
- [ ] Force push and deletion blocked; administrator bypass limited to emergencies.
- [ ] Signed commits/tags considered once all maintainers have signing configured.

## Security and automation

- [ ] Dependabot alerts, security updates, secret scanning, and push protection enabled.
- [ ] Workflow token default is read-only; per-workflow write permission retained.
- [ ] Actions restricted to GitHub-authored or explicitly reviewed actions.
- [ ] Environments `release`, `modrinth`, and `curseforge` require maintainer approval.
- [ ] Release secrets recorded only in environments, never repository files.

## Community setup

- [ ] Create labels from `.github/labels.yml` and preserve exact names.
- [ ] Create milestones M0–M5 from `ROADMAP.md`.
- [ ] Convert PX tasks from `TASKS.md` to issues as their dependency becomes actionable.
- [ ] Pin a welcome Discussion linking README, roadmap, support, and contribution docs.
