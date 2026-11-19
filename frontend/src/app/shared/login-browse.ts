import { ChangeDetectionStrategy, Component } from '@angular/core';
import { LegalLinks } from './legal-links';

/** Garden City Seva has no homepage, so its legal pages are linked here. The feed build swaps in login-browse.feed.ts. */
@Component({
  selector: 'app-login-browse',
  imports: [LegalLinks],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<app-legal-links />',
})
export class LoginBrowse {}
