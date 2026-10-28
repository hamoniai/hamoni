import { inject, Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';


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

    /**
     * Since all the routes are guarded, and the guard has an observable to get the status, thre is no need to dely the
     * navigation here. As such, just perform the initial navigation. We might need some form of setup in the future.
     */
    constructor() {
        this.#router.initialNavigation();
    }
}