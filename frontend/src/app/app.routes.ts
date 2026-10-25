import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { signedIn, signedOut } from './core/guards';
import { Roles, adminOnly, notActive, societyAdminOnly, staffOnly } from './core/roles';

export const routes: Routes = [
  { path: 'login', canActivate: [signedOut], loadComponent: () => import('./features/login/login.page') },
  { path: 'raise', canActivate: [signedIn], loadComponent: () => import('./features/raise/raise.page') },
  { path: 'register', canActivate: [signedIn, notActive], loadComponent: () => import('./features/register/register.page') },
  {
    path: '',
    canActivate: [signedIn],
    loadComponent: () => import('./features/shell/shell'),
    children: [
      { path: '', pathMatch: 'full', redirectTo: () => inject(Roles).home() },
      { path: 'complaints', loadComponent: () => import('./features/complaints/complaints.page') },
      { path: 'complaints/:id', loadComponent: () => import('./features/complaint/complaint.page') },
      { path: 'jobs', canActivate: [staffOnly], data: { queue: 'jobs' }, loadComponent: () => import('./features/queue/queue.page') },
      { path: 'assign', canActivate: [adminOnly], data: { queue: 'assign' }, loadComponent: () => import('./features/queue/queue.page') },
      { path: 'signups', canActivate: [societyAdminOnly], loadComponent: () => import('./features/signups/signups.page') },
      { path: 'profile', loadComponent: () => import('./features/profile/profile.page') },
    ],
  },
  { path: '**', redirectTo: '' },
];
