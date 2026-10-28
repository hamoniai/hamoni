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
import { TranslocoDirective } from '@jsverse/transloco';

@Component({
  selector: 'app-logout',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    TabsModule,
    InputTextModule,
    TextareaModule,
    SelectModule,
    ButtonModule,
    ToggleSwitchModule,
    TranslocoDirective,
  ],
  template: `
    <ng-container *transloco="let t">
      <h1>{{ t('pages.logout.title') }}</h1>
      <p>{{ t('pages.logout.description') }}</p>
    </ng-container>
  `,
})
export class LogoutComponent {}
