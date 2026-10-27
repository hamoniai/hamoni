import { HttpStatusCode } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { AuthService, User, UserOrganisation } from '@/app/services/auth.service';


/**
 * This component stores the whole application state of which client/subscription is being modified.
 */
export interface AppState {
    user: User | null,
    organisations: UserOrganisation[] | null,
    selectedOrganisation: string | null,
    theme: string,
}

const STOREKEY_THEME = 'app.theme';
const STOREKEY_DEFAULT_ORGANISATION = 'app.default-organisation';
const THEME_DARK = 'dark';
const THEME_LIGHT = 'light';

/**
 * Manage the application state.
 */
@Injectable({
    providedIn: 'root'
})
export class AppStateService {
    readonly #authService = inject(AuthService);

    // global state, with a public read only view. Initial state is loaded from local storage.
    // Operations on global state, such as accepting an invite should update/refresh the application state object by reloading.
    // Store the user and organisations independently to allow a dual stage update, the profile is quick, organisations are not.
    readonly #appState = signal<AppState>({
        user: null,
        organisations: null,
        selectedOrganisation: localStorage.getItem(STOREKEY_DEFAULT_ORGANISATION),
        theme: localStorage.getItem(STOREKEY_THEME) || THEME_DARK
    });
    readonly appState = this.#appState.asReadonly();
    // Store the last updated timestamp (whether the result is llogged in or not)
    readonly #appStateLoaded = signal<number | undefined>(undefined);
    readonly appStateLoaded = this.#appStateLoaded.asReadonly();

    // Private fields to track changes to the theme
    readonly #appStateTheme = computed(() => this.#appState().theme);
    readonly #appStateSelectedOrganisation = computed(() => this.#appState().selectedOrganisation);


    constructor() {
        // Start a login check on app start. This accelerates teh happy case where a user is already logged in.
        // In the case this fails, when a login flow is completed, this app state can be reset.
        // If the user is undefined, i.e. the user is not logged in, then we still set the loaded timestamp to show
        // that although null, it is complete. Need to be careful to read as anything could go wrong here, i.e. even
        // if the backend returns 200, btu the object is bad it could fail. 
        effect(() => {
            const hasValue = this.#authService.userResource.hasValue();
            const status = this.#authService.userResourceStatus();

            if (status == HttpStatusCode.Ok && hasValue) {
                // Only get this after we have checked to see if there is a value.
                const user = this.#authService.userResource.value();

                // Authenticated, get the organisations we can access. This is a heavier request and so
                // we start it in the background to accelerate the startup. Store the user information first
                // so that other processes can get going.
                this.#appState.update(currentState => ({
                    ...currentState,
                    user: user
                }));

                // Get the organisations, the organisation switcher will display the curent value selected.
                this.#authService.getProfileOrganisations().subscribe({
                    next: (data) => {
                        this.#appState.update(currentState => ({
                            ...currentState,
                            user: user,
                            organisations: data
                        }));
                    },
                    error: (err) => {
                        this.#appState.update(currentState => ({
                            ...currentState,
                            user: user,
                            organisations: null
                        }));
                    },
                    complete: () => {
                        this.#appStateLoaded.set(Date.now());
                    }
                });
            } else {
                // Effectively loaded, bot not logged in or error. Clear the user state.
                this.#appState.update(currentState => ({
                    ...currentState,
                    user: null,
                    organisations: null
                }));
                this.#appStateLoaded.set(Date.now());
            }
        });

        // Handlers to write back the theme and default organisation to local storage as an effect to make sure they stay in sync.
        effect(() => {
            const newTheme = this.#appStateTheme();
            localStorage.setItem(STOREKEY_THEME, newTheme);
        });
        effect(() => {
            const newSelectedOrganisation = this.#appStateSelectedOrganisation();
            if (newSelectedOrganisation) {
                localStorage.setItem(STOREKEY_DEFAULT_ORGANISATION, newSelectedOrganisation);
            }
        });

    }

    /**
     * Reload the application state. Used after a login, or accepting an invite.
     */
    reload(): void {
        this.#authService.reloadUser();
    }

    /**
     * Toggle the theme.
     */
    toggleTheme(): void {
        this.#appState.update(currentState => ({
            ...currentState,
            theme: currentState.theme === THEME_DARK ? THEME_LIGHT : THEME_DARK
        }));
    }

    /**
     * Change between organisations the user has access to.
     */
    setOrganisation(newOrganisation: string) {
        // TODO: needs to fail if the organisation is not in the list of allowed organisations.
        // this.#appState.update(currentState => ({
        //     ...currentState,
        //     client: newClient,
        //     subscription: undefined
        // }))
        // Needs to trigger a call to the backend
    }

    // Dark mode on/off
    // document.documentElement.classList.add('app-dark');
    // document.documentElement.classList.remove('app-dark');
    /*

    private handleDarkModeTransition(config: LayoutConfig): void {
            const supportsViewTransition = 'startViewTransition' in document;
    
            if (supportsViewTransition) {
                this.startViewTransition(config);
            } else {
                this.toggleDarkMode(config);
            }
        }
    
        private startViewTransition(config: LayoutConfig): void {
            document.startViewTransition(() => {
                this.toggleDarkMode(config);
            });
        }
    
        toggleDarkMode(config?: LayoutConfig): void {
            const _config = config || this.layoutConfig();
            if (_config.darkTheme) {
                document.documentElement.classList.add('app-dark');
            } else {
                document.documentElement.classList.remove('app-dark');
            }
        }
            */
}
