import { Injectable, effect, inject, linkedSignal, signal } from '@angular/core';
import { LoggingService } from '@/app/services/logging.service';
import { AuthService, User } from '@/app/services/auth.service';


/**
 * This component stores the whole application state of which client/subscription is being modified.
 */
export interface AppState {
    user: User | undefined,
    selectedOrganisation: string | undefined,
    darkMode: boolean,
}

@Injectable({
    providedIn: 'root'
})
export class AppStateService {
    readonly #authService = inject(AuthService);
    readonly #logging = inject(LoggingService);

    // global state, with a public read only view.
    readonly #appState = signal<AppState>({
        user: undefined,
        selectedOrganisation: undefined,
        darkMode: true // load from local storage
    });
    readonly appState = this.#appState.asReadonly();

    // Private field for tracking changes to the state. This should not re-fire if the calculated
    // value does not change - therefore preventing a loop.
    readonly #currentOrganisation = linkedSignal(() => this.#appState().selectedOrganisation);

    constructor() {
        // When the client changes, get the default subscription
        // Again, this is a shortcut to avoid an extra screen.
        // This will start on app start, therefore setting the client to undefined.
        effect(() => {
            const user = this.#authService.userResource.value();

            if (user) {
                // Switch to the default organisation. If that fails, redirect to the error page.
                // TODO
                /*this.#subscriptionService.search({clientId: client.id}).subscribe(data => {
                    if (data.length > 0) {
                        this.#appState.update(currentState => ({
                            ...currentState,
                            subscription: data[0]
                        }));
                    } else {
                        this.#logging.subscriptionNoSubscriptionsFound({rootCause: null});
                    }
                });*/
            }
        })
    }

    /**
     * This gets invoked at app start.
     * Get the current user and try to load the defaults from the organisation. If the organisation does not allow access,
     * redirect to the error page.
     */
    loadDefaults(): void {
        // get the user and change to the default organisation. Need to be careful, the httpResource will only resolve once.
        //this.#clientService.getDefaultClient().subscribe(data => this.setClient(data));
    }

    /**
     * Change between organisations the user has access to.
     */
    setOrganisation(newOrganisation: string) {
        // this.#appState.update(currentState => ({
        //     ...currentState,
        //     client: newClient,
        //     subscription: undefined
        // }))
        // Needs to trigger a call to the backend
    }
}
