# ADR 0007: Server-side PDF generation library — OpenPDF

## Status

Accepted

## Context

SPEC-0007 BR3 requires a **server-generated PDF** of the digital card (name, plan, card number,
CNS, ANS registration, coverage, additives) with the card's visual identity, downloadable at
`GET /api/cards/{beneficiaryId}/pdf`. The backend has no PDF capability today, so a library must
be chosen. Forces: JVM-native (no native binaries in the container), a license compatible with a
redistributed commercial-ish product, low transitive weight (Rule Zero), able to lay out a
branded card block on A4 (DL-0009), and deterministic output so content can be asserted in tests.

## Decision

We will use **OpenPDF** (`com.github.librepdf:openpdf`) for server-side PDF generation, invoked
from the `domain.card` module's PDF service to render the card-on-A4 layout programmatically. No
HTML/CSS→PDF engine is introduced; the layout is built directly against the card data.

## Consequences

- **Positive:** small, pure-Java dependency with no native binaries; LGPL/MPL license is fine for
  redistribution; deterministic byte output lets the content test assert the BR3 fields are
  present; nothing else in the stack is disturbed.
- **Negative:** programmatic layout is more verbose than a template engine, so the PDF does not
  attempt pixel-parity with the on-screen card (BR3 asks for the fields + visual identity, not
  screen parity); a future need for many richly-styled PDFs would argue for revisiting.

## Alternatives Considered

- **Apache PDFBox** — rejected: even lower-level; more layout boilerplate for a styled card with
  no license or weight advantage over OpenPDF.
- **openhtmltopdf / Flying Saucer (XHTML+CSS→PDF)** — rejected for this POC: heavier transitive
  tree and a second templating surface for a single screen; reconsider if many styled PDFs appear.
- **iText 7** — rejected: AGPL/commercial licensing friction for a redistributed product.

## Revision Triggers

- Several distinct styled PDFs across specs (statements, guides, IR declaration) → reconsider an
  HTML→PDF engine to share styling.
- A licensing review objects to LGPL/MPL in the distribution.

## References

SPEC-0007 BR3 · DL-0009 (card-on-A4 layout) · `docs/architecture/delivery.md` (dependency policy)
· `backend/src/main/java/com/fkmed/domain/card/`.
