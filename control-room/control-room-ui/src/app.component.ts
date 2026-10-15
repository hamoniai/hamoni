import { inject, effect, Component } from '@angular/core';
import { Location } from '@angular/common';
import { HttpStatusCode, HttpErrorResponse } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from '@/app/services/auth.service';
import { getProblemDetailsFromHttpError,  } from '@/app/services/logging.service';
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
    readonly #authService = inject(AuthService);

    constructor() {
        // This is a nasty HttpBackend, but the Navigation target is not ready yet, so need to check the URL.
        const currentPath = this.#location.path();
        if ("/error" == currentPath || currentPath.startsWith("/auth/")) {
            this.#router.initialNavigation();
        } else {
            // Check the auth whoami service to obtain the user information. We need to check direct as appState only holds a value if the
            // user is authenticated, the failure call from teh whoami service is hidden.
            effect(() => {
                const data = this.#authService.userResource.value();
                const error = this.#authService.userResource.error() as HttpErrorResponse;

                if (data) {
                    // TODO: need to select the organisation - need to confirm the organisation load has finished in app state (should be a non-null array)
                    // then follow the authorisation rules.
                    this.#router.initialNavigation();
                } else if (error) {
                    // If the error is unauthorised. If a 401, then goto the login page. Anything else, goto the error page.
                    if (error.status == HttpStatusCode.Unauthorized) {
                        this.#router.navigateByUrl(environment.loginPage, { skipLocationChange: false });
                    } else {
                        const problemDetails = getProblemDetailsFromHttpError(error); // No need to log here, that is handled in the auth service.
                        this.#router.navigateByUrl(environment.errorPage, { skipLocationChange: true, state: problemDetails });
                    }                    
                }
            });
        }
    }
}