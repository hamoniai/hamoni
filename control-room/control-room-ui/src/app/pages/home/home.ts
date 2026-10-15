import { effect, inject, Component } from '@angular/core';
import { AppStateService } from '@/app/services/app-state.service';

/**
 * Main home/landing page.
 */
@Component({
    selector: 'app-dashboard',
    imports: [],
    template: `
        <div class="grid grid-cols-12 gap-8">
        </div>
    `
})
export class Dashboard {
    //readonly #appStateService = inject(AppStateService);

    constructor() {
    }
}
