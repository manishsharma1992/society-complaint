import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { BrnInputOtp } from '@spartan-ng/brain/input-otp';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputOtpImports } from '@spartan-ng/helm/input-otp';
import { HlmSpinnerImports } from '@spartan-ng/helm/spinner';
import { SessionStore } from '../../core/session';
import { LanguageSwitch } from '../../shared/language-switch';

type Step = 'phone' | 'code';

/**
 * Phone + one-time code. One card, two steps: the number, then the 6 digits (filled in by the phone's
 * SMS autofill where it can). A wrong code keeps the number; "Change number" goes back.
 */
@Component({
  selector: 'app-login-page',
  imports: [TranslocoDirective, BrnInputOtp, HlmInputOtpImports, HlmButtonImports, HlmSpinnerImports, LanguageSwitch],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.page.html',
  styleUrl: './login.page.css',
})
export default class LoginPage {
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);

  protected readonly step = signal<Step>('phone');
  protected readonly phone = signal('');
  protected readonly code = signal('');
  protected readonly sentTo = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly secondsLeft = signal(0);
  protected readonly phoneValid = computed(() => /^[6-9]\d{9}$/.test(this.phone()));

  private timer: ReturnType<typeof setInterval> | undefined;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearInterval(this.timer));
  }

  protected onPhoneInput(value: string): void {
    // keep digits only; people paste "+91 98765 43210" or "098765-43210"
    let digits = value.replace(/\D/g, '');
    if (digits.length > 10 && digits.startsWith('91')) {
      digits = digits.slice(2);
    }
    if (digits.length > 10 && digits.startsWith('0')) {
      digits = digits.slice(1);
    }
    this.phone.set(digits.slice(0, 10));
    this.error.set(null);
  }

  protected sendCode(): void {
    if (!this.phoneValid()) {
      this.error.set('errors.invalidPhone');
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.session.requestCode(this.phone()).subscribe({
      next: (sent) => {
        this.busy.set(false);
        this.sentTo.set(sent.sentTo);
        this.code.set('');
        this.step.set('code');
        this.countdown(sent.resendAfterSeconds);
      },
      error: (e) => this.fail(e, 'errors.invalidPhone'),
    });
  }

  protected verify(): void {
    if (this.code().length !== 6 || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.session.verify(this.phone(), this.code()).subscribe({
      next: () => void this.router.navigate(['/']),
      error: (e) => {
        this.code.set('');
        this.fail(e, 'errors.wrongCode');
      },
    });
  }

  protected changeNumber(): void {
    this.step.set('phone');
    this.error.set(null);
    clearInterval(this.timer);
    this.secondsLeft.set(0);
  }

  private countdown(seconds: number): void {
    clearInterval(this.timer);
    this.secondsLeft.set(seconds);
    this.timer = setInterval(() => {
      const left = this.secondsLeft() - 1;
      this.secondsLeft.set(Math.max(left, 0));
      if (left <= 0) {
        clearInterval(this.timer);
      }
    }, 1000);
  }

  private fail(error: unknown, fallback: string): void {
    this.busy.set(false);
    const status = error instanceof HttpErrorResponse ? error.status : 0;
    this.error.set(status === 429 ? 'errors.tooMany' : status === 0 || status >= 500 ? 'errors.network' : fallback);
  }
}
