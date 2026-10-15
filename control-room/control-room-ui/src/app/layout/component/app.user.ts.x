import { CommonModule } from '@angular/common';
import { Component, computed, inject, PLATFORM_ID, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PrimeNG } from 'primeng/config';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { PopoverModule } from 'primeng/popover';
import { take, tap } from 'rxjs';
import { AuthService } from '@/app/services/auth.service';

@Component({
    selector: 'app-user',
    standalone: true,
    imports: [ButtonModule, CommonModule, PopoverModule],
    template: `
    <div class="flex flex-col gap-3">
        <button type="button" class="layout-topbar-action" (click)="op.toggle($event)">
            <i class="pi pi-user"></i>
        </button>
        <p-popover #op>
            <div class="flex flex-col gap-3 p-3">
                <div class="text-surface-900 dark:text-surface-0 font-semibold">{{ authService.user()?.name }}</div>
                <p-button class="w-full" icon="pi pi-sign-out" label="Logout" (click)="logout()" />
            </div>
        </p-popover>
    </div>
    `
})
export class AppUser {
    authService = inject(AuthService);

    router = inject(Router);

    config: PrimeNG = inject(PrimeNG);

    platformId = inject(PLATFORM_ID);

    primeng = inject(PrimeNG);

    logout() {
        this.authService.doLogout().subscribe({
            complete: () => this.router.navigateByUrl('/auth/logout')
        });
    }
}
