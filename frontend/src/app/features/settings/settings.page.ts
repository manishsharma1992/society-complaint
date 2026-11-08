import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { Observable } from 'rxjs';
import { Tower } from '../../core/models';
import { ResidencyApi } from '../../core/residency';
import { Roles } from '../../core/roles';
import { AddOn, AddOnName, Society, SocietyStore } from '../../core/society';

/**
 * A society's add-ons. The platform owner decides which are available to the society; the society admin switches
 * available ones on or off for everyone in the society. Switching off hides an add-on; its data stays.
 * Complaints can also be left off for single towers that handle complaints their own way (e.g. their own app).
 */
@Component({
  selector: 'app-settings-page',
  imports: [TranslocoDirective, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container *transloco="let t">
      <a routerLink="/profile" class="back">{{ t('settings.back') }}</a>
      <h1>{{ t('settings.title') }}</h1>
      <p class="lead">{{ t('settings.lead', { society: store.society()?.name ?? '' }) }}</p>

      @if (store.society(); as society) {
        <ul class="list ruled">
          @for (a of society.addOns; track a.addOn) {
            <li class="row">
              <div class="text">
                <h2>{{ t('settings.addOn.' + a.addOn + '.name') }}</h2>
                <p class="muted">{{ t('settings.addOn.' + a.addOn + '.about') }}</p>
                @if (!a.available) { <p class="muted small">{{ t('settings.notAvailable') }}</p> }
              </div>
              <div class="switches">
                @if (roles.isPlatformOwner()) {
                  <label class="switch">
                    <input type="checkbox" role="switch" [checked]="a.available" [disabled]="busy() === a.addOn"
                           (change)="offer(a, $any($event.target))" />
                    <span>{{ t('settings.available') }}</span>
                  </label>
                }
                <label class="switch">
                  <input type="checkbox" role="switch" [checked]="a.on" [disabled]="!a.available || busy() === a.addOn"
                         (change)="turn(a, $any($event.target))" />
                  <span>{{ t('settings.on') }}</span>
                </label>
              </div>
              @if (a.addOn === 'COMPLAINTS' && a.on && roles.isSocietyAdmin() && towers().length > 0) {
                <fieldset class="towers">
                  <legend>{{ t('settings.towers.title') }}</legend>
                  <p class="muted small">{{ t('settings.towers.lead') }}</p>
                  <ul class="tower-list">
                    @for (tower of towers(); track tower.id) {
                      <li>
                        <label class="switch">
                          <input type="checkbox" role="switch" [checked]="!society.complaintsOffInTowers.includes(tower.id)"
                                 [disabled]="busyTower() === tower.id" (change)="turnTower(tower, $any($event.target))" />
                          <span><span class="num">{{ tower.code }}</span> {{ tower.name }}</span>
                        </label>
                      </li>
                    }
                  </ul>
                </fieldset>
              }
            </li>
          }
        </ul>
        @if (failed()) { <p class="warn" role="alert">{{ t('settings.failed') }}</p> }
        <p class="muted small note">{{ t('settings.note') }}</p>
      } @else {
        <p class="muted" aria-busy="true">{{ t('feed.loading') }}</p>
      }
    </ng-container>
  `,
  styles: `
    :host { display: block; padding-top: var(--space-xs); max-width: 46rem; }
    .back { min-height: 44px; display: inline-flex; align-items: center; color: var(--color-brand-deep); }
    h1 { font-size: var(--text-2xl); }
    .lead { color: var(--color-ink-2); margin-bottom: var(--space-md); }
    .list { list-style: none; margin: 0; padding: 0; }
    .row { padding-block: var(--space-md); display: grid; gap: var(--space-sm); }
    h2 { font-size: var(--text-lg); }
    .switches { display: flex; flex-wrap: wrap; gap: var(--space-md); }
    .switch { display: inline-flex; align-items: center; gap: var(--space-xs); min-height: 44px; cursor: pointer; }
    .switch input { width: 44px; height: 26px; accent-color: var(--color-brand); cursor: pointer; }
    .switch input:disabled { cursor: not-allowed; }
    .muted { color: var(--color-ink-muted); }
    .small { font-size: var(--text-sm); }
    .warn { color: var(--color-error); }
    .note { margin-top: var(--space-md); }
    .towers { grid-column: 1 / -1; border: 0; margin: 0; padding: var(--space-sm) 0 0; }
    .towers legend { font-weight: 600; padding: 0; }
    .tower-list { list-style: none; margin: var(--space-xs) 0 0; padding: 0; display: grid;
      grid-template-columns: repeat(auto-fill, minmax(12rem, 1fr)); gap: 0 var(--space-md); }
    .num { font-variant-numeric: tabular-nums; font-weight: 600; }
    @media (min-width: 48rem) { .row { grid-template-columns: 1fr auto; align-items: center; } }
  `,
})
export default class SettingsPage {
  protected readonly store = inject(SocietyStore);
  protected readonly roles = inject(Roles);
  protected readonly busy = signal<AddOnName | null>(null);
  protected readonly busyTower = signal<number | null>(null);
  protected readonly towers = signal<Tower[]>([]);
  protected readonly failed = signal(false);

  constructor() {
    this.store.ensure().subscribe();
    inject(ResidencyApi).towers().subscribe((towers) => this.towers.set(towers));
  }

  protected turnTower(tower: Tower, input: HTMLInputElement): void {
    this.busyTower.set(tower.id);
    this.failed.set(false);
    this.store.switchComplaintsInTower(tower.id, input.checked).subscribe({
      next: () => this.busyTower.set(null),
      error: () => {
        this.failed.set(true);
        this.busyTower.set(null);
        input.checked = !input.checked; // nothing changed on the server: put the switch back
      },
    });
  }

  protected offer(addOn: AddOn, input: HTMLInputElement): void {
    this.run(addOn.addOn, input, this.store.offer(addOn.addOn, input.checked));
  }

  protected turn(addOn: AddOn, input: HTMLInputElement): void {
    this.run(addOn.addOn, input, this.store.switch(addOn.addOn, input.checked));
  }

  private run(name: AddOnName, input: HTMLInputElement, request: Observable<Society>): void {
    this.busy.set(name);
    this.failed.set(false);
    request.subscribe({
      next: () => this.busy.set(null),
      error: () => {
        this.failed.set(true);
        this.busy.set(null);
        input.checked = !input.checked; // nothing changed on the server: put the switch back
      },
    });
  }
}
