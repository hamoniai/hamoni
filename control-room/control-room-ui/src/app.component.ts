import { inject, effect, Component } from '@angular/core';
import { Location } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';
import { AppStateService } from '@/app/services/app-state.service';
import { AuthService } from '@/app/services/auth.service';
import { environment } from '@/environments/environment';


/**
 * Initialiser widget. This is to ensure that any initial state is loaded from the backend before
 * redirecting to the main page. This is a simple empty page as it is expected to be fast. If this
 * gets slower, then it will need some kind of loading page placeholder, which will require a new
 * layout being registered.
 */
@Component({
    selector: 'app-root',
    standalone: true,
    imports: [RouterModule],
    template: `<router-outlet></router-outlet>`
})
export class AppComponent {
    readonly #router = inject(Router);
    readonly #location = inject(Location);
    readonly #appState = inject(AppStateService);
    readonly #authService = inject(AuthService);

    constructor() {
        // This is a nasty HttpBackend, but the Navigation target is not ready yet, so need to check the URL.
        const currentPath = this.#location.path();
        if ("/error" == currentPath || currentPath.startsWith("/auth/")) {
            this.#router.initialNavigation();
        } else {
            // Check the auth whoami service to obtain the user information. If this returns not authenticated, then redirect the user to the
            // default login page. Do thi sinstead of checking the appState as we do not need the full state here. That also gives time for
            // the full appState to load in the background.
            effect(() => {
                const data = this.#authService.userResource.value();
                const error = this.#authService.userResource.error();

                if (data) {
                    this.#appState.loadDefaults();
                    this.#router.initialNavigation();
                } else if (error) {
                    // If the error is unauthorised. If a 401, then goto the login page. Anything else, goto the error page.
                    // FIXME: redirect update
                    this.#router.navigateByUrl(environment.errorPage, { skipLocationChange: true });
                }
            });
        }
    }
}