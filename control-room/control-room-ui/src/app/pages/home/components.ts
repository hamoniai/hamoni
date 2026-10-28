import { effect, inject, Component } from '@angular/core';
import { AppStateService } from '@/app/services/app-state.service';

import { FormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ToggleButtonModule } from 'primeng/togglebutton';
import { SelectButtonModule } from 'primeng/selectbutton';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import {DatePickerModule} from 'primeng/datepicker';
import {AutoCompleteModule} from 'primeng/autocomplete';
import {IconFieldModule} from 'primeng/iconfield';
import {InputIconModule} from 'primeng/inputicon';
import {CheckboxModule} from 'primeng/checkbox';
import {RadioButtonModule} from 'primeng/radiobutton';
import {ToggleSwitchModule} from 'primeng/toggleswitch';
import {TabsModule} from 'primeng/tabs';
import {TableModule} from 'primeng/table';
import {PaginatorModule} from 'primeng/paginator';
import {SkeletonModule} from 'primeng/skeleton';
import {TagModule} from 'primeng/tag';
import {BadgeModule} from 'primeng/badge';
import {AvatarModule} from 'primeng/avatar';
import {ProgressBarModule} from 'primeng/progressbar';
import {MessageModule} from 'primeng/message';
import {ToastModule} from 'primeng/toast';
import {DialogModule} from 'primeng/dialog';
import {PopoverModule} from 'primeng/popover';
import {MenuModule} from 'primeng/menu';
import {TooltipModule} from 'primeng/tooltip';
import {AccordionModule} from 'primeng/accordion';


/**
 * Main component board for visualisation.
 */
@Component({
    selector: 'app-components',
    standalone: true,
    imports: [
        FormsModule,
            ButtonModule, ToggleButtonModule, SelectButtonModule,
    InputTextModule, TextareaModule, InputNumberModule, SelectModule,
    DatePickerModule, AutoCompleteModule, IconFieldModule, InputIconModule,
    CheckboxModule, RadioButtonModule, ToggleSwitchModule,
    TabsModule, TableModule, PaginatorModule, SkeletonModule,
    TagModule, BadgeModule, AvatarModule, ProgressBarModule,
    MessageModule, ToastModule, DialogModule, PopoverModule, MenuModule,
    TooltipModule, AccordionModule
    ],
    providers: [MessageService],
    templateUrl: './components.html'
})
export class ComponentsComponent {
      readonly toast = inject(MessageService);

      text = 'CLM-2026-44871';
      search = '';
      notes = '';
      quantity = 3;
      date: Date | null = new Date(2026, 9, 5);
      city: { name: string } | null = null;
      cities = [{ name: 'London' }, { name: 'Manchester' }, { name: 'Leeds' }];

      checked = true;
      unchecked = false;
      radio = 'own';
      on = true;
      scope = 'all';
      scopeOptions = [
        { label: 'All', value: 'all' },
        { label: 'Mine', value: 'mine' },
        { label: 'Team', value: 'team' },
      ];

      dialogVisible = false;
      menuItems = [
        { label: 'Open process' },
        { label: 'Reschedule' },
        { separator: true },
        { label: 'Cancel process' },
      ];

      rows = [
        { id: 'CLM-2026-44871', name: 'Motor claim review', state: 'Active', owner: 'Celia Yates', due: 'Today, 2:00 PM' },
        { id: 'HR-2026-2210', name: 'Onboarding approval', state: 'Waiting', owner: 'Tom Alvarez', due: 'Tomorrow, 9:30 AM' },
        { id: 'UT-2026-0092', name: 'Meter exception', state: 'Pended', owner: 'Unassigned', due: 'None' },
      ];
      expandedRows: Record<string, boolean> = {};

      onToggleChange(event: any) {
        console.log("Got event", event)
        this.expandedRows = event.data;
    }

      statePill(state: string): { cls: string; icon: string } {
        switch (state) {
          case 'Pended':    return { cls: 'badge badge--alt',  icon: 'pi pi-pause' };
          case 'Exception': return { cls: 'badge badge--up',   icon: 'pi pi-exclamation-triangle' };
          case 'Completed': return { cls: 'badge badge--down', icon: 'pi pi-check' };
          case 'Cancelled': return { cls: 'badge badge--down', icon: 'pi pi-times' };
          default:          return { cls: 'badge',             icon: 'pi pi-play' };
        }
      }
}
