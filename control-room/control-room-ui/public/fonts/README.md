# Fonts

The faces bundled with the design set. They are **self-hosted** woff2 files —
the screens and the Angular application import no font CDN. The reasoning lives
in `design-decisions.md` §38.8 and `CODING-STANDARDS.md` §18.6: privacy for an
on-prem product, version pinning, reachability under a strict content-security
policy, and the monochrome, CSS-tintable emoji fallback the type system needs
(§38.6, §38.23).

| Family | Files | Role | Licence |
| --- | --- | --- | --- |
| Inter | `inter-latin-{400,700}-normal.woff2` | display + body — the content voice | SIL Open Font License 1.1 |
| JetBrains Mono | `jetbrains-mono-latin-{400,700}-normal.woff2` | identifiers, versions, code | SIL Open Font License 1.1 |
| Noto Emoji | `noto-emoji/noto-emoji-{0..10}.woff2` | monochrome emoji fallback in the display/body stacks | SIL Open Font License 1.1 |
| PrimeIcons | `../icons/fonts/primeicons.{woff2,woff,ttf}` | the one UI icon set | MIT (PrimeNG / PrimeTek) |

Inter, JetBrains Mono and Noto Emoji are all registered under the **SIL Open
Font License 1.1** (<https://scripts.sil.org/OFL>) and may be bundled and
redistributed with the product. The Noto Emoji subsets were generated from
Google Fonts' **Noto Emoji** (also OFL 1.1); it is a monochrome face only, so
the glyph can be tinted by CSS (§38.23). These font licences are separate from
the project's own Apache 2.0 licence.

A new third-party face or icon set is not added without its licence recorded in
this file and in the front-end's `src/assets/NOTICE.md`.
