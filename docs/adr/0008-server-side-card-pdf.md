# ADR 0008: Server-side digital-card PDF via openhtmltopdf (HTML/CSS template)

## Status

Proposed

## Context

SPEC-0007 BR3 requires "Salvar Carteirinha" to produce a **PDF** the beneficiary can use offline
at a provider's reception, containing at least name, plan, card number, CNS, ANS registration,
coverage and additives, with the card's visual identity. The frozen contract already places the
artifact on the server: `GET /api/cards/{beneficiaryId}/pdf → 200 application/pdf`. That leaves a
technical decision: how the backend renders the PDF. Constraints: CNS must not leak beyond the
card screen and this PDF (BR8), the endpoint must enforce SPEC-0003 scope and audit dependent
views (BR7), the output must resemble the on-screen card, and the tests must assert the BR3 fields
are present — all without a heavyweight, imperative layout library.

## Decision

We will generate the card PDF **server-side** from a **self-contained HTML/CSS template rendered
by `openhtmltopdf`** (`com.openhtmltopdf:openhtmltopdf-pdfbox`, Apache-2.0). The card screen's
visual identity is expressed once as an HTML/CSS fragment; the renderer fills it with the same
`CardView` data the JSON endpoint returns and lays it **card-on-A4 portrait** (SPEC-0007 OQ1,
resolved): the front visual on top, the complementary data block below. The renderer is a driven
adapter (`CardPdfRenderer`) owned by the card slice; the `/pdf` endpoint reuses the same scope
check and dependent-view audit as `GET /api/cards/{beneficiaryId}`. Field presence is verified by
extracting text from the produced PDF (PDFBox) and asserting every BR3 field, so the test is
deterministic and needs no visual diff.

## Consequences

- **Positive:** the template is HTML/CSS — readable, close to the Angular card, easy to keep in
  visual sync; a single audited server endpoint keeps CNS off the client except on the card screen;
  the PDF is byte-testable (text extraction) without image comparison.
- **Negative:** adds a runtime dependency (openhtmltopdf + its PDFBox transitive), a few MB to the
  artifact; HTML/CSS-to-PDF supports a subset of CSS, so the template must stay simple (no modern
  layout tricks the engine cannot render). Fonts for accented pt-BR text must be embedded.

## Alternatives Considered

- **Client-side generation (jsPDF / print CSS)** — rejected: the frozen contract is server-side;
  a client PDF would re-assemble the data in the browser, widen CNS exposure beyond the card
  screen, and lose the single audited endpoint (BR7/BR8).
- **OpenPDF / iText low-level API** — rejected: building the card layout imperatively (draw
  cells/positions) is verbose and brittle to change versus editing an HTML/CSS template.
- **Flying Saucer (xhtmlrenderer)** — rejected: effectively superseded by openhtmltopdf, which is
  its maintained fork with better CSS and PDFBox support.

## Revision Triggers

- The card needs print fidelity or vector assets the CSS engine cannot render (move to a
  template/report engine).
- A second spec needs server-side PDF (invoices, reimbursement statements) — extract a shared
  `PdfRenderer` infra port instead of a card-local adapter.

## References

SPEC-0007 (BR3, BR7, BR8, OQ1) · ADR-0007 (`domain.card`) · SPEC-0003 (scope + sensitive-data
audit) · `docs/architecture/modules-and-apis.md`.
