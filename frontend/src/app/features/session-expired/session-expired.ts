import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';

/**
 * "Sessão expirada" notice (SPEC-0002 BR12): the session-expiry interceptor sends the user here
 * after a mid-use 401 on `/api/**`. "Entrar novamente" restarts the OIDC code flow; the return
 * route saved by the interceptor is restored by `authGuard` once re-authenticated.
 */
@Component({
  selector: 'app-session-expired',
  imports: [TranslatePipe],
  templateUrl: './session-expired.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionExpired {
  private readonly auth = inject(AuthService);

  login(): void {
    this.auth.login();
  }
}
