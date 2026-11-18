import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Garden City Seva has nothing to browse without an account. The feed build swaps in login-browse.feed.ts. */
@Component({
  selector: 'app-login-browse',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '',
})
export class LoginBrowse {}
