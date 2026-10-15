import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ProblemMessage, createUnknownProblemDetails } from '@/app/services/logging.service';

/**
 * Display an error page. The state parameter will be a problem object.
 */
@Component({
    selector: 'app-error',
    standalone: true,
    imports: [CommonModule, ButtonModule],
    template: `
        <div class="flex flex-col items-center justify-center min-h-[80vh] p-6 text-center select-none">
            <div class="flex items-center gap-3 mb-8">
                <svg class="w-10 h-10 text-[var(--primary-color)]" viewBox="0 0 280 280" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M140.318 0C217.955 0.286611 281.99 63.648 279.953 143.664C278.008 220.25 215.591 280.397 139.151 279.998C60.6943 279.589 -3.52437 214.467 0.150242 133.294C3.47683 59.83 64.0515 0.0307183 140.307 0.0102461L140.318 0ZM239.061 77.7124C239.962 51.6308 216.164 29.1012 190.237 39.6956C179.572 44.0459 170.963 53.1355 166.357 63.5968C160.81 76.1975 164.576 85.7887 164.73 98.635C164.924 115.207 148.035 141.975 130.143 128.545C108.516 112.31 121.249 91.224 115.609 69.7282C110.829 51.4978 92.6193 34.9562 72.9873 37.0444C49.3634 39.5625 38.0632 64.0779 41.3898 85.4611C44.9108 108.083 58.9644 128.115 76.6517 142.097C93.1208 155.118 119.529 168.517 141.055 167.933C187.013 166.684 236.85 124.235 239.071 77.7022L239.061 77.7124ZM238.826 196.39C237.342 179.961 227.976 162.641 213.994 147.471C212.387 145.721 211.21 144.564 208.631 146.652C196.338 156.612 182.417 163.685 167.391 168.517C164.279 169.52 163.45 170.881 164.157 174.034C165.283 179.07 164.822 184.158 163.757 189.214C159.591 208.99 169.121 229.329 186.992 238.726C194.444 242.647 202.325 244.264 210.698 242.288C226.594 238.521 240.341 223.28 238.826 196.4V196.39ZM76.3035 242.984C81.8513 243.384 87.3479 241.521 92.5169 238.818C111.074 229.125 120.42 209.123 116.131 188.549C115.107 183.666 114.401 178.732 115.844 173.901C116.858 170.503 115.466 169.479 112.569 168.558C97.8394 163.829 84.1953 156.94 72.107 147.215C69.4662 145.097 67.8693 145.117 65.4947 147.717C57.2243 156.755 50.7759 166.92 46.1391 178.2C40.9087 190.934 38.6876 204.005 42.9251 217.568C47.5721 232.41 61.2571 243.015 76.3035 242.984Z" fill="currentColor"/>
                </svg>
                <span class="text-xl font-bold tracking-widest text-[var(--text-color)]">HAMONI</span>
            </div>

            <div class="w-full max-w-md p-8 bg-[var(--surface-card)] border border-[var(--surface-border)] rounded-3xl shadow-xl transition-all duration-300">
                <h1 class="text-4xl font-extrabold text-[var(--primary-color)] mb-4 tracking-tight">{{ problemMessage.title }}</h1>
                <p class="text-[var(--text-color-secondary)] text-base font-medium mb-8 leading-relaxed">
                    {{ problemMessage.detail }}
                </p>
                <button pButton class="px-6 py-2.5 rounded-xl font-bold" (click)="closeTab()">Close</button>
            </div>
        </div>
    `
})
export class ErrorComponent {
    readonly #router = inject(Router);

    problemMessage: ProblemMessage

    constructor() {
        const nav = this.#router.currentNavigation()
        this.problemMessage = (nav?.extras?.state as ProblemMessage | undefined) || createUnknownProblemDetails()
    }

    closeTab() {
        window.close();
        window.open('', '_self')?.close();

        // Redirect to about:blank if the window is still open
        setTimeout(() => {
            window.location.href = 'about:blank';
        }, 100);
    }
}

