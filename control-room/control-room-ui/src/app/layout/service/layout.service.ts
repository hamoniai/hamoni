import { Injectable, effect, signal, computed } from '@angular/core';

/*
 * Service to track and assist layout transitions on the screens.
 */

interface LayoutState {
    //staticMenuDesktopInactive: boolean;
}

// TODO: move the dark mode selector out. Keep this as a basic setup for the layout (menus, initialised etc, everything transient.)
// Non-transient should be in app-state.
@Injectable({
    providedIn: 'root'
})
export class LayoutService {
    layoutState = signal<LayoutState>({
        //staticMenuDesktopInactive: false,
    });

    //isSidebarActive = computed(() => this.layoutState().overlayMenuActive || this.layoutState().mobileMenuActive);

    //isOverlay = computed(() => this.layoutConfig().menuMode === 'overlay');

    constructor() {
    }

    onMenuToggle() {
        /*
        if (this.isOverlay()) {
            this.layoutState.update((prev) => ({ ...prev, overlayMenuActive: !this.layoutState().overlayMenuActive }));
        }

        if (this.isDesktop()) {
            this.layoutState.update((prev) => ({ ...prev, staticMenuDesktopInactive: !this.layoutState().staticMenuDesktopInactive }));
        } else {
            this.layoutState.update((prev) => ({ ...prev, mobileMenuActive: !this.layoutState().mobileMenuActive }));
        }
            */
    }

    isDesktop() {
        return window.innerWidth > 991;
    }

    isMobile() {
        return !this.isDesktop();
    }

    // TODO: we have a three level view.
}
