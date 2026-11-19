import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { Roles } from '../../core/roles';
import { SessionStore } from '../../core/session';
import { SocietyStore } from '../../core/society';
import { LegalLinks } from '../../shared/legal-links';

export interface HelpSection {
  id: string;
  steps: number;
}

export interface Viewer {
  active: boolean;
  resident: boolean;
  staff: boolean;
  admin: boolean;
  societyAdmin: boolean;
  platformOwner: boolean;
  feedOn: boolean;
  complaintsOn: boolean;
}

/** The how-to sections this person needs: their role, and only the add-ons their society uses. */
export function sectionsFor(v: Viewer): HelpSection[] {
  const all: [boolean, HelpSection][] = [
    [true, { id: 'start', steps: 4 }],
    [v.active && v.feedOn, { id: 'feed', steps: 3 }],
    [v.active && v.feedOn && v.resident, { id: 'post', steps: 4 }],
    [v.active && v.complaintsOn, { id: 'complaint', steps: 4 }],
    [v.active && v.complaintsOn && v.staff, { id: 'jobs', steps: 3 }],
    [v.active && v.complaintsOn && v.staff, { id: 'shift', steps: 4 }],
    [v.active && v.complaintsOn && v.admin, { id: 'assign', steps: 3 }],
    [v.active && v.complaintsOn && v.admin, { id: 'review', steps: 3 }],
    [v.active && v.feedOn && v.societyAdmin, { id: 'moderate', steps: 3 }],
    [v.active && v.societyAdmin, { id: 'signups', steps: 2 }],
    [v.active && v.complaintsOn && (v.societyAdmin || v.platformOwner), { id: 'addons', steps: 2 }],
    [true, { id: 'privacy', steps: 4 }],
  ];
  return all.filter(([shown]) => shown).map(([, section]) => section);
}

/** How to use the app, in the user's language, showing only what their role and their society's add-ons allow. */
@Component({
  selector: 'app-help-page',
  imports: [TranslocoDirective, RouterLink, LegalLinks],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container *transloco="let t">
      <a routerLink="/profile" class="back">{{ t('settings.back') }}</a>
      <h1>{{ t('help.title') }}</h1>
      <p class="lead">{{ t('help.lead') }}</p>
      <div class="sections ruled">
        @for (s of sections(); track s.id; let first = $first) {
          <details [open]="first">
            <summary>{{ t('help.' + s.id + '.title') }}</summary>
            <ol>
              @for (n of steps(s); track n) {
                <li>{{ t('help.' + s.id + '.' + n) }}</li>
              }
            </ol>
          </details>
        }
      </div>
      <p class="muted small">{{ t('help.more') }}</p>
      <app-legal-links />
    </ng-container>
  `,
  styles: `
    :host { display: block; padding-top: var(--space-xs); max-width: 46rem; }
    .back { min-height: 44px; display: inline-flex; align-items: center; color: var(--color-brand-deep); }
    h1 { font-size: var(--text-2xl); }
    .lead { color: var(--color-ink-2); margin-bottom: var(--space-md); }
    .sections { margin-bottom: var(--space-lg); }
    summary {
      min-height: 56px; display: flex; align-items: center; justify-content: space-between; gap: var(--space-sm);
      cursor: pointer; list-style: none; font-family: var(--font-display); font-size: var(--text-lg); color: var(--color-ink);
    }
    summary::-webkit-details-marker { display: none; }
    summary::after { content: '+'; font-family: var(--font-body); color: var(--color-brand-deep); font-size: 1.5em; line-height: 1; }
    details[open] summary::after { content: '−'; }
    summary:focus-visible { outline: 2px solid var(--color-focus); outline-offset: 2px; }
    ol { margin: 0 0 var(--space-md); padding-inline-start: 1.4em; display: grid; gap: var(--space-xs); color: var(--color-ink-2); max-width: 62ch; }
    .muted { color: var(--color-ink-muted); }
    .small { font-size: var(--text-sm); }
  `,
})
export default class HelpPage {
  private readonly roles = inject(Roles);
  private readonly society = inject(SocietyStore);
  private readonly user = inject(SessionStore).user;

  protected readonly sections = computed(() =>
    sectionsFor({
      active: this.user()?.status === 'ACTIVE',
      resident: this.roles.isResident(),
      staff: this.roles.isStaff(),
      admin: this.roles.isAdmin(),
      societyAdmin: this.roles.isSocietyAdmin(),
      platformOwner: this.roles.isPlatformOwner(),
      feedOn: this.society.feedOn(),
      complaintsOn: this.society.complaintsOn(),
    }),
  );

  protected steps(section: HelpSection): number[] {
    return Array.from({ length: section.steps }, (_, i) => i + 1);
  }
}
