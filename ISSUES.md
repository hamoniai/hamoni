# Raised issues

Upstream issues raised by the Hamoni platform, their impact on this repository
and the current handling. Add an entry when an issue is raised, and update the
current handling when the workaround changes.

| Issue | Raised | Impact | Current handling | Status |
| --- | --- | --- | --- | --- |
| [quarkusio/quarkus#58349 — Config mapping error in extension subproject](https://github.com/quarkusio/quarkus/issues/58349) | 2026-11-12 | In a Gradle extension subproject, a `@ConfigMapping` class injected into an `@Record` build step fails with `SRCFG00027` because the config mapping class and the build-step parameter are loaded by different classloaders. The same classloader identity mismatch can prevent a recording proxy being cast to the recorder class. | The placeholder is read by property name (`hamoni.ext.quarkus.multi-tenant.placeholder`) rather than through the mapping. The recorder is disabled for now, so the default placeholder is used until the Quarkus issue is resolved. | Open |
| [quarkusio/quarkus#58249 — Panache Reactive multi-tenant support](https://github.com/quarkusio/quarkus/issues/58249) | 2026-11-09 | Quarkus Hibernate Reactive disables multi-tenancy (`MultiTenancyStrategy.NONE`), and `QuarkusSqlClientPool` has no `getTenantPool` implementation, so schema-based multi-tenancy throws. | A temporary Java agent in `build-logic/quarkus-multi-tenant-agent` rewrites the Hibernate Reactive deployment processor's hardcoded `MultiTenancyStrategy.NONE` to `SCHEMA`. It targets Quarkus 3.x method names, and the agent must be built before the relevant Quarkus build/dev tasks. The local `extension/quarkus-multi-tenant` still instruments `QuarkusSqlClientPool` and binds tenant SQL by rewriting a quoted placeholder schema below Hibernate. | Open |

These issues are upstream. The workarounds above are local to the platform and
should be removed when the upstream issues are resolved.
