import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { HomeApi, HomeContentResponse } from './home.api';

/** SPEC-0005: Home content source. Frozen contract — GET /api/content/home. */
describe('HomeApi', () => {
  let api: HomeApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(HomeApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('calls GET /api/content/home and returns banners + notices', () => {
    const payload: HomeContentResponse = {
      banners: [
        {
          title: 'Alerta de golpe!',
          text: 'A operadora não solicita dados ou pagamentos por WhatsApp.',
          buttonLabel: 'Saiba mais',
          destination: '/atendimento#antifraude',
          imageUrl: null,
          order: 1,
        },
      ],
      notices: [
        {
          title: 'Instabilidade momentânea da Telemedicina',
          severity: 'ALERT',
          body: 'Estamos normalizando o serviço.',
          order: 1,
        },
      ],
    };
    let result: HomeContentResponse | undefined;
    api.getHomeContent().subscribe((response) => (result = response));

    http.expectOne({ url: '/api/content/home', method: 'GET' }).flush(payload);

    expect(result).toEqual(payload);
  });
});
