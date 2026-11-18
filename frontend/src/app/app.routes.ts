import { Routes } from '@angular/router';
import { signedIn, signedOut } from './core/guards';
import { adminOnly, adminOrStaff, complaintsOn, goHome, notActive, settingsAccess, societyAdminOnly, staffOnly } from './core/roles';

export const routes: Routes = [
  { path: 'login', canActivate: [signedOut], loadComponent: () => import('./features/login/login.page') },
  { path: 'raise', canActivate: [signedIn, complaintsOn], loadComponent: () => import('./features/raise/raise.page') },
  { path: 'register', canActivate: [signedIn, notActive], loadComponent: () => import('./features/register/register.page') },
  {
    path: '',
    canActivate: [signedIn],
    loadComponent: () => import('./features/shell/shell'),
    children: [
      { path: '', pathMatch: 'full', canActivate: [goHome], children: [] },
      // the sign-in page sends signed-in visitors here: on to their own home
      { path: 'start', pathMatch: 'full', canActivate: [goHome], children: [] },
      { path: 'complaints', canActivate: [complaintsOn], loadComponent: () => import('./features/complaints/complaints.page') },
      { path: 'complaints/:id', canActivate: [complaintsOn], loadComponent: () => import('./features/complaint/complaint.page') },
      { path: 'jobs', canActivate: [complaintsOn, staffOnly], data: { queue: 'jobs' }, loadComponent: () => import('./features/queue/queue.page') },
      { path: 'assign', canActivate: [complaintsOn, adminOnly], data: { queue: 'assign' }, loadComponent: () => import('./features/queue/queue.page') },
      { path: 'staff', canActivate: [complaintsOn, adminOrStaff], loadComponent: () => import('./features/staff-report/staff-report.page') },
      { path: 'shifts', canActivate: [complaintsOn, adminOnly], loadComponent: () => import('./features/shifts/shifts.page') },
      { path: 'settings', canActivate: [settingsAccess], loadComponent: () => import('./features/settings/settings.page') },
      { path: 'signups', canActivate: [societyAdminOnly], loadComponent: () => import('./features/signups/signups.page') },
      { path: 'profile', loadComponent: () => import('./features/profile/profile.page') },
      { path: 'help', loadComponent: () => import('./features/help/help.page') },
    ],
  },
  { path: '**', redirectTo: '' },
];
