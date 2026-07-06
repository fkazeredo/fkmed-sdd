import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AvatarStateService } from '../../core/context/avatar-state.service';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import {
  centerSquare,
  checkPhoto,
  PhotoContentType,
  sniffImageType,
} from './photo-validation';
import { ProfileApi } from './profile.api';

/**
 * "Alterar Foto" (SPEC-0006 BR2/BR3): upload a JPG/PNG (client pre-check ≤ 5 MB + magic-byte
 * content sniffing) with a square-crop preview, replace or remove — for the **active** beneficiary
 * (a titular may change a dependent's via the header selector, SPEC-0003). On success the shared
 * AvatarStateService is updated so the avatar changes everywhere without a new login (BR3). The
 * current photo is blob-fetched through AvatarStateService (the photo endpoint needs a Bearer
 * token, so `<img src>` cannot load it directly).
 */
@Component({
  selector: 'app-alterar-foto',
  imports: [TranslatePipe],
  templateUrl: './alterar-foto.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlterarFoto {
  private readonly api = inject(ProfileApi);
  private readonly avatar = inject(AvatarStateService);
  protected readonly context = inject(BeneficiaryContextService);

  readonly loading = signal(false);
  readonly success = signal<'saved' | 'removed' | null>(null);
  readonly errorKey = signal<string | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly pendingBlob = signal<Blob | null>(null);

  readonly activeId = computed(() => this.context.active()?.beneficiaryId);
  readonly activeName = computed(() => this.context.active()?.firstName ?? '');

  /** Current avatar for the active beneficiary (shared blob state); null → placeholder. */
  readonly currentAvatarUrl = computed(() => this.avatar.avatarUrl(this.activeId()));

  constructor() {
    // Load the active beneficiary's current photo (and reload on switch) so it can be shown.
    effect(() => {
      const id = this.activeId();
      if (id) {
        this.avatar.load(id);
      }
    });
  }

  async onFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) {
      await this.acceptFile(file);
    }
  }

  /** Validates content+size (BR2), then stages a centered square-cropped blob with a preview. */
  async acceptFile(file: File | Blob): Promise<void> {
    this.errorKey.set(null);
    this.success.set(null);
    const header = new Uint8Array(await file.arrayBuffer());
    const rejection = checkPhoto(file.size, header.subarray(0, 8));
    if (rejection) {
      this.errorKey.set(rejection);
      this.clearPreview();
      return;
    }
    const type = sniffImageType(header.subarray(0, 8)) as PhotoContentType;
    const cropped = await this.cropToSquare(file, type);
    this.stagePhoto(cropped);
  }

  /** Stages the ready-to-upload blob and its preview (the seam unit tests drive directly). */
  stagePhoto(blob: Blob): void {
    this.clearPreview();
    this.pendingBlob.set(blob);
    this.previewUrl.set(this.toObjectUrl(blob));
  }

  save(): void {
    const id = this.activeId();
    const blob = this.pendingBlob();
    if (!id || !blob || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.uploadPhoto(id, blob).subscribe({
      next: () => {
        // Publish the exact bytes we just uploaded (no extra fetch) — propagates everywhere (BR3).
        this.avatar.setFromBlob(id, blob);
        this.loading.set(false);
        this.success.set('saved');
        this.clearPreview();
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.applyError(error);
      },
    });
  }

  remove(): void {
    const id = this.activeId();
    if (!id || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.errorKey.set(null);
    this.success.set(null);
    this.api.removePhoto(id).subscribe({
      next: () => {
        this.avatar.remove(id);
        this.loading.set(false);
        this.success.set('removed');
        this.clearPreview();
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.applyError(error);
      },
    });
  }

  initialOf(name: string): string {
    return name ? name.charAt(0).toUpperCase() : '?';
  }

  private clearPreview(): void {
    const url = this.previewUrl();
    if (url) {
      this.revokeObjectUrl(url);
    }
    this.previewUrl.set(null);
    this.pendingBlob.set(null);
  }

  private applyError(error: HttpErrorResponse): void {
    const code = error.error?.code;
    this.errorKey.set(
      code === 'profile.photo-invalid-content' || code === 'profile.photo-too-large'
        ? code
        : 'common.error',
    );
  }

  /** Draws the largest centered square of the image onto a canvas (BR2). Not exercised under
   * jsdom (no canvas) — the geometry it relies on is unit-tested via `centerSquare`. */
  private async cropToSquare(file: Blob, type: PhotoContentType): Promise<Blob> {
    const bitmap = await createImageBitmap(file);
    const { sx, sy, size } = centerSquare(bitmap.width, bitmap.height);
    const target = Math.min(size, 512);
    const canvas = document.createElement('canvas');
    canvas.width = target;
    canvas.height = target;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      bitmap.close();
      return file;
    }
    ctx.drawImage(bitmap, sx, sy, size, size, 0, 0, target, target);
    bitmap.close();
    return new Promise<Blob>((resolve) => canvas.toBlob((blob) => resolve(blob ?? file), type));
  }

  private toObjectUrl(blob: Blob): string | null {
    try {
      return URL.createObjectURL(blob);
    } catch {
      return null;
    }
  }

  private revokeObjectUrl(url: string): void {
    try {
      URL.revokeObjectURL(url);
    } catch {
      // no-op when the environment (jsdom) has no object-URL support
    }
  }
}
