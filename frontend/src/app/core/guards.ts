import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionStore } from './session';

export const signedIn: CanActivateFn = () =>
  inject(SessionStore).signedIn() || inject(Router).createUrlTree(['/login']);

export const signedOut: CanActivateFn = () =>
  !inject(SessionStore).signedIn() || inject(Router).createUrlTree(['/']);
