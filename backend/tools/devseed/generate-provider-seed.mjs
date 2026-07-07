#!/usr/bin/env node
// Generates the heavy dev-only provider-network seed.
//
// Output: backend/src/main/resources/db/dev/R__seed_dev_provider_network.sql
// Loaded ONLY by the `devdata` Spring profile (local /dev-env) -- never by prod, the
// Testcontainers integration suite, or the E2E stack (SPEC-0008; docs/architecture/persistence.md
// "local dev fake data MUST NOT mix with production migrations"). Regenerate and recommit the
// SQL whenever SEED_COUNT or the reference lists below change; the script is deterministic
// (seeded PRNG + uuid5 namespace) so reruns produce byte-identical output and idempotent inserts.
//
// Usage: node backend/tools/devseed/generate-provider-seed.mjs

import { createHash } from 'node:crypto';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const SEED_COUNT = 10_000;
const RNG_SEED = 20260707;
const NAMESPACE = '6b1f7e2a-6e9d-4f8a-9c2d-9b6a2f7e1a3d';
const BATCH_SIZE = 500;

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const OUTPUT_PATH = resolve(
  SCRIPT_DIR,
  '..',
  '..',
  'src',
  'main',
  'resources',
  'db',
  'dev',
  'R__seed_dev_provider_network.sql',
);

// [uf, ibgeCode, city, ddd, cepPrefix5, tierWeight, neighborhoods[]]
const CAPITALS = [
  ['SP', 3550308, 'São Paulo', '11', '01310', 3, [
    'Bela Vista', 'Pinheiros', 'Moema', 'Tatuapé', 'Itaim Bibi', 'Santana',
    'Vila Mariana', 'Liberdade', 'Perdizes', 'Santo Amaro', 'Vila Madalena', 'Brooklin',
  ]],
  ['RJ', 3304557, 'Rio de Janeiro', '21', '22040', 3, [
    'Copacabana', 'Ipanema', 'Leblon', 'Tijuca', 'Botafogo', 'Barra da Tijuca',
    'Flamengo', 'Recreio dos Bandeirantes', 'Méier', 'Centro',
  ]],
  ['MG', 3106200, 'Belo Horizonte', '31', '30130', 3, [
    'Savassi', 'Funcionários', 'Lourdes', 'Santo Antônio', 'Sion', 'Buritis',
    'Pampulha', 'Cidade Jardim',
  ]],
  ['BA', 2927408, 'Salvador', '71', '40140', 3, [
    'Barra', 'Pituba', 'Itaigara', 'Rio Vermelho', 'Ondina', 'Graça',
    'Caminho das Árvores', 'Brotas',
  ]],
  ['DF', 5300108, 'Brasília', '61', '70390', 3, [
    'Asa Sul', 'Asa Norte', 'Sudoeste', 'Lago Sul', 'Águas Claras', 'Taguatinga', 'Guará',
  ]],
  ['CE', 2304400, 'Fortaleza', '85', '60160', 3, [
    'Aldeota', 'Meireles', 'Cocó', 'Papicu', 'Varjota', 'Dionísio Torres',
  ]],
  ['PE', 2611606, 'Recife', '81', '51020', 3, [
    'Boa Viagem', 'Casa Forte', 'Graças', 'Espinheiro', 'Boa Vista', 'Aflitos',
  ]],
  ['PR', 4106902, 'Curitiba', '41', '80420', 3, [
    'Batel', 'Água Verde', 'Centro Cívico', 'Bigorrilho', 'Cabral', 'Juvevê',
  ]],
  ['RS', 4314902, 'Porto Alegre', '51', '90570', 3, [
    'Moinhos de Vento', 'Bela Vista', 'Cidade Baixa', 'Petrópolis', 'Auxiliadora', 'Bom Fim',
  ]],
  ['AM', 1302603, 'Manaus', '92', '69057', 3, [
    'Adrianópolis', 'Ponta Negra', 'Parque Dez de Novembro', 'Aleixo',
    'Nossa Senhora das Graças',
  ]],
  ['PA', 1501402, 'Belém', '91', '66055', 3, [
    'Nazaré', 'Umarizal', 'Batista Campos', 'Marco', 'Reduto',
  ]],
  ['GO', 5208707, 'Goiânia', '62', '74223', 3, [
    'Setor Bueno', 'Setor Oeste', 'Jardim Goiás', 'Setor Marista', 'Setor Sul',
  ]],
  ['ES', 3205309, 'Vitória', '27', '29055', 1.5, [
    'Praia do Canto', 'Jardim Camburi', 'Enseada do Suá', 'Bento Ferreira', 'Jardim da Penha',
  ]],
  ['SC', 4205407, 'Florianópolis', '48', '88010', 1.5, [
    'Centro', 'Trindade', 'Coqueiros', 'Agronômica', 'Itacorubi', 'Lagoa da Conceição',
  ]],
  ['RN', 2408102, 'Natal', '84', '59020', 1.5, [
    'Petrópolis', 'Tirol', 'Lagoa Nova', 'Capim Macio', 'Ponta Negra',
  ]],
  ['PB', 2507507, 'João Pessoa', '83', '58039', 1.5, [
    'Manaíra', 'Tambaú', 'Bessa', 'Cabo Branco', 'Miramar',
  ]],
  ['AL', 2704302, 'Maceió', '82', '57035', 1.5, [
    'Ponta Verde', 'Jatiúca', 'Pajuçara', 'Farol', 'Jaraguá',
  ]],
  ['SE', 2800308, 'Aracaju', '79', '49025', 1.5, [
    'Jardins', 'Salgado Filho', 'Atalaia', 'Grageru', 'São José',
  ]],
  ['PI', 2211001, 'Teresina', '86', '64000', 1.5, [
    'Jóquei', 'Fátima', 'Ilhotas', 'Cabral', 'Horto Florestal',
  ]],
  ['MA', 2111300, 'São Luís', '98', '65075', 1.5, [
    'Renascença', 'Calhau', "Ponta D'Areia", 'Cohama', 'Jaracaty',
  ]],
  ['MS', 5002704, 'Campo Grande', '67', '79002', 1.5, [
    'Jardim dos Estados', 'Vila Cidade', 'Amambaí', 'Chácara Cachoeira',
  ]],
  ['MT', 5103403, 'Cuiabá', '65', '78015', 1.5, [
    'Centro Sul', 'Duque de Caxias', 'Jardim Aclimação', 'Araés',
  ]],
  ['RO', 1100205, 'Porto Velho', '69', '76801', 1, [
    'Centro', 'Nova Porto Velho', 'Olaria', 'Embratel',
  ]],
  ['AC', 1200401, 'Rio Branco', '68', '69900', 1, [
    'Centro', 'Bosque', 'Bahia Nova',
  ]],
  ['RR', 1400100, 'Boa Vista', '95', '69301', 1, [
    'Centro', 'São Pedro', 'São Vicente',
  ]],
  ['AP', 1600303, 'Macapá', '96', '68900', 1, [
    'Central', 'Trem', 'Jesus de Nazaré',
  ]],
  ['TO', 1721000, 'Palmas', '63', '77001', 1, [
    'Plano Diretor Sul', 'Plano Diretor Norte', 'Taquaralto',
  ]],
];

// Existing specialty codes (V15) -- kept disjoint from EXTRA_SPECIALTIES so `on conflict do
// nothing` never fires spuriously.
const EXISTING_SPECIALTIES = new Set([
  'ALERGOLOGIA', 'CARDIOLOGIA', 'CIRURGIA_GERAL', 'CLINICA_MEDICA', 'DERMATOLOGIA',
  'ENDOCRINOLOGIA', 'GASTROENTEROLOGIA', 'GERIATRIA', 'GINECOLOGIA_OBSTETRICIA',
  'NEUROLOGIA', 'OFTALMOLOGIA', 'ORTOPEDIA_TRAUMATOLOGIA', 'OTORRINOLARINGOLOGIA',
  'PEDIATRIA', 'PNEUMOLOGIA', 'PSIQUIATRIA', 'UROLOGIA',
]);

const EXTRA_SPECIALTIES = [
  ['ANESTESIOLOGIA', 'Anestesiologia'],
  ['ANGIOLOGIA', 'Angiologia'],
  ['CIRURGIA_CARDIACA', 'Cirurgia Cardíaca'],
  ['CIRURGIA_PEDIATRICA', 'Cirurgia Pediátrica'],
  ['CIRURGIA_PLASTICA', 'Cirurgia Plástica'],
  ['CIRURGIA_TORACICA', 'Cirurgia Torácica'],
  ['CIRURGIA_VASCULAR', 'Cirurgia Vascular'],
  ['COLOPROCTOLOGIA', 'Coloproctologia'],
  ['ENDOSCOPIA', 'Endoscopia'],
  ['FISIATRIA', 'Fisiatria'],
  ['GENETICA_MEDICA', 'Genética Médica'],
  ['HEMATOLOGIA', 'Hematologia'],
  ['HOMEOPATIA', 'Homeopatia'],
  ['INFECTOLOGIA', 'Infectologia'],
  ['MASTOLOGIA', 'Mastologia'],
  ['MEDICINA_DE_FAMILIA', 'Medicina de Família'],
  ['MEDICINA_DO_TRABALHO', 'Medicina do Trabalho'],
  ['MEDICINA_ESPORTIVA', 'Medicina Esportiva'],
  ['MEDICINA_INTENSIVA', 'Medicina Intensiva'],
  ['NEFROLOGIA', 'Nefrologia'],
  ['NEONATOLOGIA', 'Neonatologia'],
  ['NEUROCIRURGIA', 'Neurocirurgia'],
  ['NUTROLOGIA', 'Nutrologia'],
  ['ONCOLOGIA', 'Oncologia'],
  ['RADIOLOGIA', 'Radiologia'],
  ['REUMATOLOGIA', 'Reumatologia'],
];
for (const [code] of EXTRA_SPECIALTIES) {
  if (EXISTING_SPECIALTIES.has(code)) {
    throw new Error(`Specialty code collides with V15 seed: ${code}`);
  }
}

const ALL_SPECIALTY_CODES = [...EXISTING_SPECIALTIES].sort()
  .concat(EXTRA_SPECIALTIES.map(([code]) => code));

// [serviceTypeCode, weight, hasSpecialty]
const SERVICE_TYPES = [
  ['CONSULTORIOS', 55, true],
  ['LABORATORIOS_EXAMES', 11, false],
  ['EXAMES_ESPECIAIS', 8, false],
  ['PRONTO_ATENDIMENTO_COMERCIAL', 7, false],
  ['HOSPITAIS_INTERNACAO', 6, false],
  ['PRONTO_SOCORRO_24H', 5, false],
  ['HEMODIALISE', 4, false],
  ['TEA', 4, false],
];

const FIRST_NAMES_M = [
  'João', 'Pedro', 'Lucas', 'Gabriel', 'Rafael', 'Bruno', 'André', 'Carlos', 'Ricardo',
  'Eduardo', 'Marcelo', 'Fernando', 'Felipe', 'Diego', 'Thiago', 'Rodrigo', 'Gustavo',
  'Leonardo', 'Vitor', 'Daniel', 'Alexandre', 'Marcos', 'Paulo', 'Roberto', 'Sérgio',
  'Fábio', 'Caio', 'Igor', 'Renato', 'Vinícius', 'Antônio', 'José', 'Francisco', 'Hugo',
  'Rogério', 'Cláudio', 'Wagner', 'Márcio', 'Anderson', 'Leandro',
];
const FIRST_NAMES_F = [
  'Maria', 'Ana', 'Juliana', 'Fernanda', 'Camila', 'Beatriz', 'Patrícia', 'Renata',
  'Larissa', 'Amanda', 'Carolina', 'Aline', 'Débora', 'Cristina', 'Vanessa', 'Priscila',
  'Bianca', 'Letícia', 'Gabriela', 'Mariana', 'Sandra', 'Márcia', 'Adriana', 'Simone',
  'Tatiana', 'Roberta', 'Luciana', 'Daniela', 'Raquel', 'Sônia', 'Rita', 'Vera',
  'Regina', 'Silvia', 'Cláudia', 'Eliane', 'Viviane', 'Natália', 'Isabela', 'Paula',
];
const LAST_NAMES = [
  'Silva', 'Santos', 'Oliveira', 'Souza', 'Rodrigues', 'Ferreira', 'Alves', 'Pereira',
  'Lima', 'Gomes', 'Costa', 'Ribeiro', 'Martins', 'Carvalho', 'Almeida', 'Lopes',
  'Soares', 'Fernandes', 'Vieira', 'Barbosa', 'Rocha', 'Dias', 'Nascimento', 'Andrade',
  'Moreira', 'Nunes', 'Marques', 'Machado', 'Mendes', 'Freitas', 'Cardoso', 'Ramos',
  'Gonçalves', 'Teixeira', 'Correia', 'Cavalcanti', 'Castro', 'Pinto', 'Araújo', 'Monteiro',
  'Melo', 'Farias', 'Guimarães', 'Xavier', 'Batista', 'Tavares', 'Duarte', 'Peixoto',
  'Azevedo', 'Campos', 'Prado', 'Nogueira', 'Sales', 'Brandão', 'Siqueira', 'Bezerra',
  'Cunha', 'Moraes', 'Barros', 'Vasconcelos',
];
const STREET_NAMES = [
  'Rua das Flores', 'Avenida Brasil', 'Rua Sete de Setembro', 'Rua XV de Novembro',
  'Avenida Getúlio Vargas', 'Rua Marechal Deodoro', 'Rua Barão do Rio Branco',
  'Avenida Presidente Vargas', 'Rua Tiradentes', 'Rua Duque de Caxias',
  'Avenida Rio Branco', 'Rua Santos Dumont', 'Rua São José', 'Rua Voluntários da Pátria',
  'Avenida Independência', 'Rua Coronel Fulgêncio', 'Rua Doutor Assis',
  'Avenida das Nações', 'Rua Vinte e Cinco de Março', 'Rua Bahia',
  'Avenida Amazonas', 'Rua Paraná', 'Rua Minas Gerais', 'Avenida Goiás',
  'Rua Ceará', 'Rua Pernambuco', 'Avenida Paulista', 'Rua Padre Anchieta',
  'Rua Doutor Ovídio Pires', 'Avenida das Américas', 'Rua General Osório',
];
const INSTITUTION_KIND = ['Clínica', 'Instituto', 'Centro Médico', 'Policlínica'];
const INSTITUTION_TOPIC = [
  'Vida', 'Saúde', 'Bem-Estar', 'São Lucas', 'Santa Fé', 'Nova Esperança', 'Renascer',
  'Excelência', 'Referência', 'Modelo', 'União', 'Progresso', 'Harmonia', 'Central',
  'Premium', 'Especializada',
];

// mulberry32: small deterministic PRNG (Math.random is not seedable in Node).
function mulberry32(seed) {
  let a = seed >>> 0;
  return function next() {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const rand = mulberry32(RNG_SEED);

function randInt(min, max) {
  return Math.floor(rand() * (max - min + 1)) + min;
}

function pick(list) {
  return list[randInt(0, list.length - 1)];
}

function weightedPick(items) {
  const total = items.reduce((sum, item) => sum + item[item.length - 2], 0);
  let r = rand() * total;
  for (const item of items) {
    r -= item[item.length - 2];
    if (r <= 0) return item;
  }
  return items[items.length - 1];
}

function uuidv5(name, namespace) {
  const nsBytes = Buffer.from(namespace.replace(/-/g, ''), 'hex');
  const nameBytes = Buffer.from(name, 'utf8');
  const hash = createHash('sha1').update(Buffer.concat([nsBytes, nameBytes])).digest();
  const bytes = Buffer.from(hash.subarray(0, 16));
  bytes[6] = (bytes[6] & 0x0f) | 0x50;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = bytes.toString('hex');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function genCep(prefix5) {
  return `${prefix5}${String(randInt(0, 999)).padStart(3, '0')}`;
}

function genPhone(ddd) {
  const local = rand() < 0.7
    ? `9${randInt(1000, 9999)}-${randInt(1000, 9999)}`
    : `${randInt(2000, 5999)}-${randInt(1000, 9999)}`;
  return `(${ddd}) ${local}`;
}

function genPersonName() {
  const isFemale = rand() < 0.5;
  const first = pick(isFemale ? FIRST_NAMES_F : FIRST_NAMES_M);
  const last = `${pick(LAST_NAMES)} ${pick(LAST_NAMES)}`;
  return { name: `${first} ${last}`, isFemale };
}

function sqlEscape(value) {
  return value.replace(/'/g, "''");
}

function sqlStr(value) {
  return value === null || value === undefined ? 'null' : `'${sqlEscape(String(value))}'`;
}

const PHONE_RE = /^\(\d{2}\) \d{4,5}-\d{4}$/;
const CEP_RE = /^\d{8}$/;

const providers = [];
const providerSpecialties = [];
const providerSeals = [];

for (let i = 0; i < SEED_COUNT; i += 1) {
  const [, ibgeCode, city, ddd, cepPrefix, , neighborhoods] = weightedPick(CAPITALS);
  const [serviceTypeCode, , hasSpecialty] = weightedPick(SERVICE_TYPES);
  const neighborhood = pick(neighborhoods);
  const street = pick(STREET_NAMES);
  const number = String(randInt(10, 4999));

  let complement = null;
  if (rand() < 0.22) {
    complement = pick([
      `Sala ${randInt(101, 1220)}`,
      `Conjunto ${randInt(11, 99)}`,
      `Bloco ${pick(['A', 'B', 'C', 'D'])}`,
      `Andar ${randInt(2, 18)}`,
    ]);
  }

  const cep = genCep(cepPrefix);
  const phone = genPhone(ddd);
  const active = rand() >= 0.10;

  let specialtyCode = null;
  if (hasSpecialty) {
    specialtyCode = pick(ALL_SPECIALTY_CODES);
  }

  let name;
  if (serviceTypeCode === 'CONSULTORIOS') {
    if (rand() < 0.7) {
      const { name: personName, isFemale } = genPersonName();
      name = `Consultório ${isFemale ? 'Dra.' : 'Dr.'} ${personName}`;
    } else {
      name = `${pick(INSTITUTION_KIND)} ${pick(INSTITUTION_TOPIC)} ${neighborhood}`;
    }
  } else if (serviceTypeCode === 'LABORATORIOS_EXAMES') {
    name = `Laboratório ${pick(INSTITUTION_TOPIC)} ${city}`;
  } else if (serviceTypeCode === 'EXAMES_ESPECIAIS') {
    name = `Clínica de Exames Especiais ${neighborhood}`;
  } else if (serviceTypeCode === 'HOSPITAIS_INTERNACAO') {
    name = `Hospital ${pick(INSTITUTION_TOPIC)} ${city}`;
  } else if (serviceTypeCode === 'PRONTO_SOCORRO_24H') {
    name = `Pronto-Socorro 24h ${neighborhood}`;
  } else if (serviceTypeCode === 'PRONTO_ATENDIMENTO_COMERCIAL') {
    name = `Pronto Atendimento ${neighborhood}`;
  } else if (serviceTypeCode === 'HEMODIALISE') {
    name = `Clínica de Hemodiálise ${neighborhood}`;
  } else {
    name = `Clínica TEA ${neighborhood}`;
  }
  name = name.slice(0, 140);

  if (!PHONE_RE.test(phone)) throw new Error(`Bad phone: ${phone}`);
  if (!CEP_RE.test(cep)) throw new Error(`Bad cep: ${cep}`);
  if (neighborhood.length > 100) throw new Error(`Neighborhood too long: ${neighborhood}`);
  if (street.length > 160) throw new Error(`Street too long: ${street}`);

  const providerId = uuidv5(`dev-provider-${i}`, NAMESPACE);
  providers.push([
    providerId, name, serviceTypeCode, ibgeCode, neighborhood, cep, street,
    number, complement, phone, active,
  ]);

  if (specialtyCode) {
    providerSpecialties.push([providerId, specialtyCode]);
  }

  if (active && rand() < 0.15) {
    let sealCode;
    if (serviceTypeCode === 'HOSPITAIS_INTERNACAO') {
      sealCode = 'ACR_HOSP';
    } else if (serviceTypeCode === 'LABORATORIOS_EXAMES' || serviceTypeCode === 'EXAMES_ESPECIAIS') {
      sealCode = 'ISO9001';
    } else {
      sealCode = pick(['P', 'R']);
    }
    providerSeals.push([providerId, sealCode]);
  }
}

function renderValue(value) {
  if (typeof value === 'boolean' || typeof value === 'number') return String(value);
  return sqlStr(value);
}

function renderBatches(rows, columns, table, conflictCols) {
  const statements = [];
  for (let start = 0; start < rows.length; start += BATCH_SIZE) {
    const chunk = rows.slice(start, start + BATCH_SIZE);
    const values = chunk
      .map((row) => `    (${row.map(renderValue).join(', ')})`)
      .join(',\n');
    statements.push(
      `insert into ${table} (${columns}) values\n${values}\non conflict ${conflictCols} do nothing;`,
    );
  }
  return statements;
}

const lines = [];
lines.push('-- GENERATED FILE -- do not edit by hand.');
lines.push('-- Regenerate with: node backend/tools/devseed/generate-provider-seed.mjs');
lines.push('--');
lines.push('-- Heavy dev-only provider-network seed (SPEC-0008): thousands of fictitious');
lines.push('-- providers/doctors across the 27 Brazilian state capitals, real neighborhoods,');
lines.push('-- varied specialties/service types -- for manual exploration via /dev-env.');
lines.push('-- Loaded ONLY by the `devdata` profile (classpath:db/dev, see');
lines.push('-- application-devdata.yaml) -- never by prod, tests or the E2E stack, per');
lines.push('-- docs/architecture/persistence.md ("local dev fake data MUST NOT mix with');
lines.push('-- production migrations"). Repeatable migration: reruns are idempotent');
lines.push('-- (deterministic uuid5 ids + `on conflict do nothing`).');
lines.push(`-- Generated rows: ${providers.length} providers, `
  + `${providerSpecialties.length} provider_specialty, ${providerSeals.length} provider_seal.`);
lines.push('');
lines.push('-- Extra specialty registry entries (beyond V15\'s 17) for a realistic funnel.');
const extraValues = EXTRA_SPECIALTIES
  .map(([code, name]) => `    (${sqlStr(code)}, ${sqlStr(name)})`)
  .join(',\n');
lines.push(`insert into specialty (code, name) values\n${extraValues}\non conflict (code) do nothing;`);
lines.push('');

lines.push('-- Provider directory.');
lines.push(...renderBatches(
  providers,
  'id, name, service_type_code, municipality_id, neighborhood, cep, street, '
    + 'address_number, complement, phone, active',
  'provider',
  '(id)',
));
lines.push('');

lines.push('-- Provider specialties (CONSULTORIOS only, per BR5).');
lines.push(...renderBatches(
  providerSpecialties, 'provider_id, specialty_code', 'provider_specialty',
  '(provider_id, specialty_code)',
));
lines.push('');

lines.push('-- Provider seals (subset, varied by service type).');
lines.push(...renderBatches(
  providerSeals, 'provider_id, seal_code', 'provider_seal',
  '(provider_id, seal_code)',
));
lines.push('');

lines.push(
  "-- Widen the dev beneficiary's plan coverage to NACIONAL so the nationwide seed above is\n"
  + '-- actually browsable/searchable in the funnel (BR4/DL-0014); dev-only, never touches the\n'
  + '-- ESTADUAL/RJ plan seeded by V1 outside this profile.',
);
lines.push(
  "update plan set coverage = 'NACIONAL', coverage_uf = null "
  + "where id = 'b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a' and coverage <> 'NACIONAL';",
);
lines.push('');

mkdirSync(dirname(OUTPUT_PATH), { recursive: true });
writeFileSync(OUTPUT_PATH, lines.join('\n'), 'utf8');

const inactiveCount = providers.filter((p) => p[10] === false).length;
console.log(
  `Wrote ${OUTPUT_PATH} -- ${providers.length} providers (${inactiveCount} inactive), `
  + `${providerSpecialties.length} specialty links, ${providerSeals.length} seals.`,
);
