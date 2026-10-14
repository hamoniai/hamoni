import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

@Component({
    selector: 'app-logout',
    standalone: true,
    imports: [CommonModule, FormsModule, TableModule, TabsModule, InputTextModule, TextareaModule, SelectModule, ButtonModule, ToggleSwitchModule],
    template: `
        <h1>Logged out</h1>
        <p>You have been logged out successfully. Please close this page/window.</p>
    `
})
export class LogoutComponent {

}