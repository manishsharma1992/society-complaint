import { Injectable, computed, inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { Observable, map } from 'rxjs';
import { SOCIETY_ID } from './api';
import { SessionStore } from './session';
import { SocietyStore } from './society';
import { HAS_COMPLAINTS, HAS_FEED } from '../site';

/**
 * What the signed-in user's roles let them do, used only to decide what to show: the server checks every action
 * again. (Roles come from the login token, so a new role shows after signing in again.)
 */
@Injectable({ providedIn: 'root' })
export class Roles {
  private readonly user = inject(SessionStore).user;
  private readonly society = inject(SocietyStore);
  private readonly roles = computed(() => (this.user()?.status === 'ACTIVE' ? this.user()!.roles.filter((r) => r.societyId === SOCIETY_ID) : []));

  readonly isStaff = computed(() => this.roles().some((r) => r.role === 'STAFF'));
  /** Residents write feed posts (the server checks it too). */
  readonly isResident = computed(() => this.roles().some((r) => r.role === 'RESIDENT'));
  readonly isSocietyAdmin = computed(() => this.roles().some((r) => r.role === 'SOCIETY_ADMIN'));
  /** The towers a tower admin looks after. */
  readonly adminTowers = computed(() => this.roles().filter((r) => r.role === 'TOWER_ADMIN' && r.towerId !== null).map((r) => r.towerId!));
  readonly isAdmin = computed(() => this.isSocietyAdmin() || this.adminTowers().length > 0);
  /** Runs the platform (above any society): decides which add-ons a society may use. */
  readonly isPlatformOwner = computed(() => this.user()?.status === 'ACTIVE' && this.user()?.platformOwner === true);

  /** A society admin manages everything; a tower admin manages complaints in their tower. */
  manages(towerId: number | null): boolean {
    return this.isSocietyAdmin() || (towerId !== null && this.adminTowers().includes(towerId));
  }

  /**
   * Where each person starts: the feed when the society uses it; otherwise their own work queue or the complaints
   * list; their profile when the society uses neither add-on. (Call after SocietyStore.ensure().)
   */
  home(): string {
    if (this.user() && this.user()!.status !== 'ACTIVE') return '/register';
    if (HAS_FEED && this.society.feedOn()) return '/feed';
    if (HAS_COMPLAINTS && this.society.complaintsOn()) {
      if (this.isAdmin()) return '/assign';
      if (this.isStaff()) return '/jobs';
      return '/complaints';
    }
    return '/profile';
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

/** Waits for the society's add-ons, then decides. */
const afterSociety = (decide: () => boolean | UrlTree): Observable<boolean | UrlTree> =>
  inject(SocietyStore).ensure().pipe(map(decide));

/** The empty path: everyone lands on their own home (see Roles.home). */
export const goHome: CanActivateFn = () => {
  const roles = inject(Roles);
  const router = inject(Router);
  return afterSociety(() => router.createUrlTree([roles.home()]));
};
export const feedOn: CanActivateFn = () => {
  const store = inject(SocietyStore);
  const router = inject(Router);
  return afterSociety(() => store.feedOn() || router.createUrlTree(['/']));
};
export const complaintsOn: CanActivateFn = () => {
  const store = inject(SocietyStore);
  const router = inject(Router);
  return afterSociety(() => store.complaintsOn() || router.createUrlTree(['/']));
};
/** Approved members post to the feed (the server checks they are residents). */
export const activeOnly: CanActivateFn = () =>
  inject(SessionStore).user()?.status === 'ACTIVE' || inject(Router).createUrlTree(['/register']);
export const settingsAccess: CanActivateFn = () =>
  inject(Roles).isSocietyAdmin() || inject(Roles).isPlatformOwner() || inject(Router).createUrlTree(['/']);
