import { effect, inject, Component } from '@angular/core';
import { AppStateService } from '@/app/services/app-state.service';
import { RouterModule } from '@angular/router';


/**
 * Main home/landing page.
 */
@Component({
    selector: 'app-home',
    standalone: true,
    imports: [RouterModule],
    template: `
    <router-outlet></router-outlet>
    `
})
export class HomeComponent {
    readonly #appState = inject(AppStateService);
}
