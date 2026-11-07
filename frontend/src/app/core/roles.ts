import { Injectable, computed, inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SOCIETY_ID } from './api';
import { SessionStore } from './session';

/**
 * What the signed-in user's roles let them do, used only to decide what to show: the server checks every action
 * again. (Roles come from the login token, so a new role shows after signing in again.)
 */
@Injectable({ providedIn: 'root' })
export class Roles {
  private readonly user = inject(SessionStore).user;
  private readonly roles = computed(() => (this.user()?.status === 'ACTIVE' ? this.user()!.roles.filter((r) => r.societyId === SOCIETY_ID) : []));

  readonly isStaff = computed(() => this.roles().some((r) => r.role === 'STAFF'));
  readonly isSocietyAdmin = computed(() => this.roles().some((r) => r.role === 'SOCIETY_ADMIN'));
  /** The towers a tower admin looks after. */
  readonly adminTowers = computed(() => this.roles().filter((r) => r.role === 'TOWER_ADMIN' && r.towerId !== null).map((r) => r.towerId!));
  readonly isAdmin = computed(() => this.isSocietyAdmin() || this.adminTowers().length > 0);

  /** A society admin manages everything; a tower admin manages complaints in their tower. */
  manages(towerId: number | null): boolean {
    return this.isSocietyAdmin() || (towerId !== null && this.adminTowers().includes(towerId));
  }

  /** Where each person starts: their own work queue, or the complaints list. */
  home(): string {
    if (this.user() && this.user()!.status !== 'ACTIVE') return '/register';
    if (this.isAdmin()) return '/assign';
    if (this.isStaff()) return '/jobs';
    return '/complaints';
  }
}

/** Signing up is for people not yet approved. */
export const notActive: CanActivateFn = () =>
  inject(SessionStore).user()?.status !== 'ACTIVE' || inject(Router).createUrlTree(['/']);
export const staffOnly: CanActivateFn = () => inject(Roles).isStaff() || inject(Router).createUrlTree(['/']);
export const adminOnly: CanActivateFn = () => inject(Roles).isAdmin() || inject(Router).createUrlTree(['/']);
export const societyAdminOnly: CanActivateFn = () => inject(Roles).isSocietyAdmin() || inject(Router).createUrlTree(['/']);
export const adminOrStaff: CanActivateFn = () =>
  inject(Roles).isAdmin() || inject(Roles).isStaff() || inject(Router).createUrlTree(['/']);
