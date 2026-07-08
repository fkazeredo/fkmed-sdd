# Frontend Architecture - Angular

> Read when: writing or changing Angular code, UI behavior, forms, state, HTTP or UX/keyboard
> handling.

## Stack (current FKMed POC - v0.12.0)

| Concern | Choice | Version / source |
|---|---|---|
| Framework | Angular standalone components, zoneless, signals | 22.0.x |
| UI kit | PrimeNG Aura + primeicons | PrimeNG 21.1.x / primeicons 7 |
| Utility CSS | Tailwind CSS | 4.3.x |
| A11y/behavior primitives | Angular CDK | 22.0.x |
| i18n | `@ngx-translate/core` with in-memory pt-BR translations | 18 |
| Auth | `angular-oauth2-oidc`, Authorization Code + PKCE against embedded AS | 22.0.x |
| Unit tests | Vitest + jsdom + coverage v8 | 4.x |
| E2E | Playwright against `compose.e2e.yaml` isolated stack | 1.61.x |
| Lint/format | angular-eslint, ESLint, Prettier | see `package.json` |
| Language | TypeScript | 6.0.x |

Gates: `npm run lint`, `npm test`, `npm run build`. E2E runs with
`npm run e2e:up`, `npm run e2e`, `npm run e2e:down`.

## Structure

```txt
src/app
  core/
    auth       OIDC config, guards, token handling
    context    active beneficiary state
    http       interceptors and API helpers
    i18n       in-memory translation bundle
    layout     shell, navigation, selector and notification bell
  shared/
    reusable components, validators and utilities
  features/
    home
    my-plan
    first-access / email-verification / password-recovery / security
    perfil
    card
    rede
    agendamento
    telemedicina
    minha-saude
    guias
    financas
    atendimento
    reembolso
```

`core` is for application-wide singletons and infrastructure. `shared` is for genuinely reusable UI
or utilities. Feature-specific behavior stays inside the feature.

## Product UX Rules

The UI represents the beneficiary workflow, not the database model. Every screen must handle loading,
empty, error, permission-denied and success states where relevant. Every write action must prevent
duplicate submission and give visible feedback.

User-facing text lives in the translation bundle. Dates, times, currency and CPF/CNPJ/phone masks
must follow pt-BR product conventions. Do not render raw enum names.

The app uses restrained operational UI. Prefer clear flows, predictable controls and compact content
over decorative sections. Health-plan tasks are repetitive and sensitive; clarity wins.

## State

Use the simplest state holder that fits:

- local signals for local UI;
- feature services when state is shared inside a feature;
- RxJS when stream semantics are clearer;
- no global store unless the product grows into real cross-feature state complexity.

## Forms

Template-driven forms with `ngModel` are the project norm for operational screens. Extract validators
and mappers when forms grow. Destructive actions need confirmation. Dirty mutation screens should use
the existing guard pattern when leaving would lose work.

## HTTP and Errors

Feature API services own domain-oriented HTTP calls. Components do not scatter raw `HttpClient`
calls. The backend contract is the committed OpenAPI snapshot and the TypeScript models/services
should reflect it directly.

Global error handling normalizes transport errors; feature screens decide how to present them.
Errors that block a form belong near the field/action. Toasts are mainly for successful writes.

## Accessibility and Responsive Behavior

Every interactive control needs a label or an accessible name. Icon-only buttons need clear
`aria-label`s. Keyboard navigation must work without pointer-only traps. Text must fit on mobile and
desktop without overlap.

Manual QA should exercise at least:

- keyboard-only navigation in every critical flow;
- focus after submit/error/dialog close;
- screen states at 320 px mobile width and desktop;
- long beneficiary/provider names and long translated strings;
- reduced-motion preference where animation exists.

Automated a11y tooling is not yet part of the gate; add it in a future hardening slice when the
project decides on the tool and threshold.

## Styling

PrimeNG + Tailwind is the standard. Do not add another component library without an ADR. Keep global
styles limited to tokens, CSS layers and app-wide layout. Feature styles stay close to the component.

The frontend build has a warning budget (`initial` warning at 900 kB, error at 1.5 MB). Phase 6
recorded a warning above 900 kB; treat further growth as performance debt and prefer route-level
lazy loading or dependency trimming over simply raising the budget.
