/**
 * In-memory pt-BR translation source (product locale — docs/specs/README.md §UI norms).
 * Every user-facing string of the app lives here; the i18n completeness spec fails the build
 * when a rendered key is missing (SPEC-0001 BR7/AC5).
 */
export const TRANSLATIONS: Record<string, Record<string, string>> = {
  'pt-BR': {
    'app.title': 'FKMed',
    'shell.logout': 'Sair',
    'shell.loggedUser': 'Usuário autenticado',
    'nav.meuPlano': 'Meu Plano',
    'nav.placeholder': 'Mais funcionalidades em breve',
    'common.loading': 'Carregando…',
    'common.error': 'Não foi possível carregar os dados. Tente novamente.',
    'common.retry': 'Tentar novamente',
    'common.yes': 'Sim',
    'common.no': 'Não',
    'meuPlano.title': 'Meu Plano',
    'meuPlano.plan.ansRegistration': 'Registro ANS',
    'meuPlano.plan.coverage': 'Abrangência',
    'meuPlano.plan.copay': 'Coparticipação',
    'meuPlano.plan.reimbursement': 'Reembolso',
    'meuPlano.plan.additives': 'Adicionais',
    'meuPlano.coverage.MUNICIPAL': 'Municipal',
    'meuPlano.coverage.ESTADUAL': 'Estadual',
    'meuPlano.coverage.NACIONAL': 'Nacional',
    'meuPlano.members.title': 'Integrantes do plano',
    'meuPlano.members.name': 'Nome',
    'meuPlano.members.role': 'Papel',
    'meuPlano.members.card': 'Carteirinha',
    'meuPlano.role.TITULAR': 'Titular',
    'meuPlano.role.DEPENDENT': 'Dependente',
  },
};
