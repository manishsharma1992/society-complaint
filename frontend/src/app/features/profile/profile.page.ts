import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { Flat } from '../../core/models';
import { ResidencyApi } from '../../core/residency';
import { SessionStore } from '../../core/session';
import { LanguageSwitch } from '../../shared/language-switch';

/** Who is signed in, their flats and roles, the language, and the way out. */
@Component({
  selector: 'app-profile-page',
  imports: [TranslocoDirective, HlmButtonImports, LanguageSwitch],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container *transloco="let t">
      <h1>{{ user()?.name ?? t('profile.title') }}</h1>
      <dl class="facts ruled">
        <div><dt>{{ t('profile.phone') }}</dt><dd class="num">{{ user()?.phone }}</dd></div>
        <div>
          <dt>{{ t('profile.roles') }}</dt>
          <dd>
            @for (role of user()?.roles ?? []; track $index) {
              <span class="role">{{ t('role.' + role.role) }}</span>
            }
          </dd>
        </div>
        <div>
          <dt>{{ t('profile.flats') }}</dt>
          <dd>
            @for (flat of flats(); track flat.id) {
              <span class="flat num">{{ flat.label }}</span>
            } @empty {
              <span class="muted">{{ t('profile.noFlats') }}</span>
            }
          </dd>
        </div>
        <div><dt>{{ t('profile.language') }}</dt><dd><app-language-switch /></dd></div>
      </dl>
      <button hlmBtn variant="outline" size="lg" type="button" class="sign-out" (click)="signOut()">{{ t('profile.signOut') }}</button>
    </ng-container>
  `,
  styles: `
    :host { display: block; padding-top: var(--space-sm); }
    h1 { font-size: var(--text-2xl); margin-bottom: var(--space-lg); }
    .facts { margin: 0 0 var(--space-xl); }
    .facts > div { display: grid; gap: var(--space-2xs); padding-block: var(--space-sm); }
    dt { font-size: var(--text-sm); color: var(--color-ink-muted); }
    dd { margin: 0; display: flex; flex-wrap: wrap; gap: var(--space-xs); color: var(--color-ink); }
    .muted { color: var(--color-ink-muted); }
    .sign-out { margin-top: var(--space-md); }
    @media (min-width: 48rem) { .facts > div { grid-template-columns: 12rem 1fr; align-items: center; } }
  `,
})
export default class ProfilePage {
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);
  protected readonly user = this.session.user;
  protected readonly flats = signal<Flat[]>([]);

  constructor() {
    const user = this.user();
    if (user) {
      inject(ResidencyApi).flatsOf(user.id).subscribe({ next: (flats) => this.flats.set(flats), error: () => {} });
    }
  }

  protected signOut(): void {
    this.session.signOut();
    void this.router.navigate(['/login']);
  }
}
