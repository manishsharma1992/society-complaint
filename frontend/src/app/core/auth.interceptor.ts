import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { API } from './api';
import { SessionStore } from './session';

/** Sends the login token with every API call; an expired or refused token signs the user out. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionStore);
  const router = inject(Router);
  const token = session.token();
  const authorised =
    token && request.url.startsWith(API) ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
  return next(authorised).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && token) {
        session.signOut();
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
