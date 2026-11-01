# AGENTS.md — `docs` (documentation project)

This directory is the Hamoni **Antora documentation component**. It is written
for the people who read, deploy, integrate and contribute to Hamoni. It is not a
second copy of the repository documents or the source code.

Read the repository-level [`../AGENTS.md`](../AGENTS.md),
[`../CODING-STANDARDS.md`](../CODING-STANDARDS.md) (especially Part III),
[`../ARCHITECTURE-DECISIONS.md`](../ARCHITECTURE-DECISIONS.md) and
[`../API-AND-DATABASE.md`](../API-AND-DATABASE.md) before changing content.

## Documentation structure

The documentation has five top-level sections:

| Section | Pages |
| --- | --- |
| Introduction | `index.adoc` |
| Overview | Concepts · Design principles · Architecture · Contributing · Security |
| Installation | Requirements · Kubernetes deployments · Virtual machine deployments · Docker deployments · Maintenance |
| Integration | APIs · Plugin development · Agent connectors |
| Process designer | Key blocks · JSON Process |

### Consumers

Each page has a primary consumer. The consumer determines the depth and the
technical language of the page.

Technical language can and should be used, but only language that the consumer
would be expected to know or understand. Documentation is written at a level that
is relevant and understandable to the consumer.

| Section or page | Consumer |
| --- | --- |
| Introduction | All viewers. |
| Overview — Concepts | All readers. |
| Overview — Design principles | All readers. |
| Overview — Architecture | Technical staff who deploy or support Hamoni. |
| Overview — Contributing | People and organisations outside Hamoni who contribute to the project. |
| Overview — Security | IT security and risk staff evaluating Hamoni. |
| Installation | Technical staff in a business deploying Hamoni internally. |
| Integration | Technical staff integrating systems with Hamoni, inbound through APIs or outbound through plugins and agent connectors. |
| Process designer | Business analysts and process owners who build and design processes. |

**Do not create a new top-level section without explicit confirmation.** Do not
add, rename or remove a page outside the agreed structure without confirmation.

A page may use headings freely to separate its content. A page is a navigation
entry; headings within it are not navigation entries.

## Page conventions

- The page title is `= <Title>` and the navigation label is `:navtitle:`.
- Use `==` for main sections and `===` for sub-sections.
- Keep one concern per page. Do not merge unrelated pages.
- Use tables for structured comparisons, definition lists for terms, and short
  paragraphs for explanation.
- Use the product's terms consistently: **organisation**, not tenant; **team**,
  **project**, **process**, **task**, **agent**, **actor**.
- Do not use emojis.

## Tone of voice

All content is:

1. **Factual.** It describes the feature, the way of working or the code. It does
   not offer advice, recommendations or TODO lists. It may state what is out of
   scope.
2. **Plain.** It is simple and direct. It avoids hyperbole and vague terms. It
   uses technical language appropriate to the page's intended consumer.
3. **Present tense.** It describes the current state of the project or code. A
   feature that is not built yet is still described in the present tense, as it
   should operate.

Do not write marketing copy. Do not use "we" or "our".

## Documentation properties and links

The build supplies these AsciiDoc attributes from `docs/build.gradle.kts`:

| Attribute | Use |
| --- | --- |
| `{project-version}` | The generated documentation version. |
| `{hamoni-org-url}` | The Hamoni GitHub organisation. |
| `{project-base-url}` | The project source repository. |
| `{project-clone-url}` | The clone URL for the project source. |
| `{openapi-url}` | The generated OpenAPI/Swagger documentation. |
| `{design-system-url}` | The Design System for Hamoni. |

Use the properties. Do not hard-code the version, repository URL or external
documentation URL.

Project documents are linked directly from the project source:

```asciidoc
link:{project-base-url}/blob/main/AGENTS.md[Repository rules]
link:{project-base-url}/blob/main/CODING-STANDARDS.md[Coding standards]
link:{project-base-url}/blob/main/ARCHITECTURE-DECISIONS.md[Architecture decisions]
link:{project-base-url}/blob/main/API-AND-DATABASE.md[API and database contract]
link:{project-base-url}/blob/main/README.md[README]
```

Do not import project `.md` files as Antora attachments. Do not copy project
documents or source code into the documentation. The only collector import is
the Hamoni logo.

Cross-page links use `xref:`. External links use `link:`.

## Project document links

Reference the project documents rather than repeating them:

- `AGENTS.md` — repository rules and directory map.
- `CODING-STANDARDS.md` — code style, structure and testing. Reference it; do not
  re-iterate its detailed rules.
- `ARCHITECTURE-DECISIONS.md` — the architecture decisions.
- `API-AND-DATABASE.md` — the interface and database contract.
- `README.md` — build and test commands.
- `specs/README.md` — the spec lifecycle and templates.
- The local `control-room/AGENTS.md`, `process-runner/AGENTS.md` and
  `common/AGENTS.md` — the rules for each area.

## API documentation

Do not fully document the API. The generated OpenAPI/Swagger site is the
authoritative reference for operations, request bodies and response schemas.

The APIs page describes the API shape, authentication, organisation context,
resource shape and error format. It links to the generated site with
`{openapi-url}`.

## Images and diagrams

- Draw diagrams in the page source using `[svgbob]` blocks. Antora renders them
  through Kroki into inline SVG.
- Do not check in pre-rendered SVG files for diagrams.
- Style generated SVG through `docs/ui/css/site.css`, using the Hamoni design
  tokens.
- The Hamoni logo is the only collected image. It is imported from
  `screens/assets/logo/hamoni-logo.svg` into `modules/ROOT/images`.
- Use descriptive alternative text and a block title for every diagram.
- Do not use a diagram to repeat a table or a paragraph.

## Navigation

Navigation is defined in `docs/modules/ROOT/nav.adoc`. The page path mirrors the
navigation structure:

```text
docs/modules/ROOT/pages/
  index.adoc
  overview/
  installation/
  integration/
  process-designer/
```

A new top-level section requires explicit confirmation. A new page within an
existing section requires confirmation unless the task names it.

## Build and verification

Build the documentation with:

```bash
./gradlew docs:build
```

The build must complete with no AsciiDoc errors or warnings. Inspect the rendered
site under `docs/build/site` for layout, links and diagrams.

Do not change `docs/build.gradle.kts`, `docs/antora.yml` or
`docs/antora-playbook.yml` unless the task concerns the documentation build.
Do not change `docs/ui/` unless the task concerns the documentation UI.

## Content quality

- **Do not assume.** If a requirement, configuration option, service name or
  behaviour is missing, stop and ask. Do not invent a value, key or URL.
- **Do not overwrite user edits.** Check the current page before replacing a
  table, section or paragraph. Preserve changes made by the project owner.
- **No placeholders.** A placeholder is allowed only when the agreed page is
  explicitly a holding page; state the missing design factually.
- **No secrets.** Never add credentials, keys, tokens or personal data.
- **No dead content.** Remove pages and navigation entries that are no longer
  relevant when asked to do so.
- **No duplication.** Do not duplicate the coding standards, the API reference or
  the project documents. Link to them.
- **Precise scope.** State out-of-scope items when they clarify the page. Do not
  use a TODO list as content.

## Agent workflow

1. Read this file and the governing documents.
2. Read the current page and its navigation entry.
3. Confirm the page belongs to the agreed structure and consumer.
4. Update only `docs/`.
5. Build and inspect the rendered page.
6. Report the files changed and any unresolved question.

The **Technical Writer** agent is the primary agent for this project. It works
only inside `docs/` and does not change application code, build logic or CI.
