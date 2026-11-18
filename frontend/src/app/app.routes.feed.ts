import { Routes } from '@angular/router';
import { signedIn, signedOut } from './core/guards';
import { activeOnly, feedOn, goHome, notActive, societyAdminOnly } from './core/roles';

export const routes: Routes = [
  { path: 'login', canActivate: [signedOut], loadComponent: () => import('./features/login/login.page') },
  { path: 'register', canActivate: [signedIn, notActive], loadComponent: () => import('./features/register/register.page') },
  // the public feed: everyone, signed in or not, reads, reacts and reports
  {
    path: 'public',
    loadComponent: () => import('./features/public/public-layout'),
    children: [
      { path: '', pathMatch: 'full', loadComponent: () => import('./features/public/public-feed.page') },
      { path: ':slug', data: { openToAll: true }, loadComponent: () => import('./features/feed/post.page') },
    ],
  },
  {
    path: '',
    canActivate: [signedIn],
    loadComponent: () => import('./features/shell/shell'),
    children: [
      { path: '', pathMatch: 'full', canActivate: [goHome], children: [] },
      // the public homepage sends signed-in visitors here: on to their own home
      { path: 'start', pathMatch: 'full', canActivate: [goHome], children: [] },
      { path: 'feed', canActivate: [feedOn], loadComponent: () => import('./features/feed/feed.page') },
      { path: 'feed/new', canActivate: [feedOn, activeOnly], loadComponent: () => import('./features/feed/new-post.page') },
      { path: 'feed/mine', canActivate: [feedOn], loadComponent: () => import('./features/feed/my-posts.page') },
      { path: 'feed/moderation', canActivate: [feedOn, societyAdminOnly], loadComponent: () => import('./features/feed/moderation.page') },
      { path: 'feed/:slug', canActivate: [feedOn], loadComponent: () => import('./features/feed/post.page') },
      { path: 'signups', canActivate: [societyAdminOnly], loadComponent: () => import('./features/signups/signups.page') },
      { path: 'profile', loadComponent: () => import('./features/profile/profile.page') },
      { path: 'help', loadComponent: () => import('./features/help/help.page') },
    ],
  },
  { path: '**', redirectTo: '' },
];
