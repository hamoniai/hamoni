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
    template: `
<div class="grid gap-5" data-od-id="component-board">

  <!-- ── Buttons & actions ─────────────────────────────────────────────── -->
  <section class="rounded-lg border border-border bg-surface p-5" data-od-id="board-buttons">
    <h2 class="m-0 mb-4 text-cap font-semibold uppercase tracking-[0.07em] text-muted">Buttons &amp; actions</h2>

    <div class="flex flex-wrap items-center gap-3">
      <p-button label="Start work" />
      <p-button label="Apply" />
      <p-button label="Open process" severity="info" [outlined]="true" />
      <p-button label="Filters" severity="secondary" [outlined]="true" />
      <p-button label="Cancel" severity="secondary" [text]="true" />
      <p-button label="Cancel process" severity="danger" />
    </div>

    <div class="mt-4 flex flex-wrap items-center gap-3">
      <p-button label="Small" size="small" />
      <p-button label="Default" />
      <p-button label="Large" size="large" />
      <p-button label="With icon" icon="pi pi-arrow-right" iconPos="right" />
    </div>

    <div class="mt-4 flex flex-wrap items-center gap-3">
      <p-button icon="pi pi-pencil" [text]="true" [rounded]="true" pTooltip="Edit" />
      <p-button icon="pi pi-refresh" [text]="true" [rounded]="true" pTooltip="Refresh" />
      <p-button icon="pi pi-download" [outlined]="true" [rounded]="true" pTooltip="Export" />
      <p-button label="Disabled" [disabled]="true" />
      <p-button label="Disabled next" severity="info" [outlined]="true" [disabled]="true" />
    </div>

    <!-- one scale, three steps (§38.1) -->
    <div class="mt-4 flex items-center gap-4 text-muted">
      <i class="pi pi-inbox" style="font-size:12px"></i>
      <i class="pi pi-check-square" style="font-size:14px"></i>
      <i class="pi pi-users" style="font-size:16px"></i>
      <span class="text-meta">for contrast, 12 / 14 / 16</span>
    </div>
  </section>

  <!-- ── Form fields ───────────────────────────────────────────────────── -->
  <section class="rounded-lg border border-border bg-surface p-5" data-od-id="board-fields">
    <h2 class="m-0 mb-4 text-cap font-semibold uppercase tracking-[0.07em] text-muted">Form fields</h2>

    <div class="grid gap-4 md:grid-cols-2">
      <label class="grid gap-1.5">
        <span class="text-sm text-muted">Process reference</span>
        <input pInputText [(ngModel)]="text" />
      </label>

      <label class="grid gap-1.5">
        <span class="text-sm text-muted">Search</span>
        <p-iconfield>
          <p-inputicon class="pi pi-search" />
          <input pInputText [(ngModel)]="search" placeholder="Search processes" />
        </p-iconfield>
      </label>

      <label class="grid gap-1.5">
        <span class="text-sm text-muted">Localised date</span>
        <p-datepicker [(ngModel)]="date" [showIcon]="true" iconDisplay="input" dateFormat="d M yy" />
      </label>

      <label class="grid gap-1.5">
        <span class="text-sm text-muted">Team</span>
        <p-select [options]="cities" [(ngModel)]="city" optionLabel="name" placeholder="Select a team" />
      </label>

      <label class="grid gap-1.5">
        <span class="text-sm text-muted">Quantity</span>
        <p-inputnumber [(ngModel)]="quantity" [showButtons]="true" [min]="0" />
      </label>

      <label class="grid gap-1.5">
        <span class="text-sm text-muted">Invalid</span>
        <input pInputText value="" class="ng-invalid ng-dirty" aria-invalid="true" />
        <span class="text-cap text-fg">A reference is required.</span>
      </label>

      <label class="grid gap-1.5 md:col-span-2">
        <span class="text-sm text-muted">Notes</span>
        <textarea pTextarea [(ngModel)]="notes" rows="3" placeholder="Add a note"></textarea>
      </label>

      <label class="grid gap-1.5 md:col-span-2">
        <span class="text-sm text-muted">Disabled</span>
        <input pInputText [value]="text" [disabled]="true" />
      </label>

      <p-autocomplete [(ngModel)]="city" [suggestions]="cities" optionLabel="name" placeholder="Team selector (typeahead)" class="md:col-span-2" />
    </div>
  </section>

  <!-- ── Selection & toggles ───────────────────────────────────────────── -->
  <section class="rounded-lg border border-border bg-surface p-5" data-od-id="board-selection">
    <h2 class="m-0 mb-4 text-cap font-semibold uppercase tracking-[0.07em] text-muted">Selection &amp; toggles</h2>

    <div class="grid gap-5 md:grid-cols-2">
      <div class="grid gap-3">
        <label class="check">
          <p-checkbox [(ngModel)]="checked" [binary]="true" inputId="cb-on" />
          <span>Overdue only (checked)</span>
        </label>
        <label class="check">
          <p-checkbox [(ngModel)]="unchecked" [binary]="true" inputId="cb-off" />
          <span>Unchecked</span>
        </label>
        <label class="check">
          <p-checkbox [binary]="true" [disabled]="true" inputId="cb-dis" />
          <span>Disabled</span>
        </label>
      </div>

      <div class="flex flex-wrap items-center gap-5">
        <label class="check">
          <p-radiobutton [(ngModel)]="radio" value="own" inputId="r-own" /><span>Own</span>
        </label>
        <label class="check">
          <p-radiobutton [(ngModel)]="radio" value="team" inputId="r-team" /><span>Team</span>
        </label>
      </div>

      <div class="flex flex-wrap items-center gap-5">
        <label class="flex items-center gap-2 text-sm text-muted">
          <p-toggleswitch [(ngModel)]="on" /><span>On</span>
        </label>
        <label class="flex items-center gap-2 text-sm text-muted">
          <p-toggleswitch [ngModel]="false" /><span>Off</span>
        </label>
        <label class="flex items-center gap-2 text-sm text-dim">
          <p-toggleswitch [ngModel]="false" [disabled]="true" /><span>Locked</span>
        </label>
      </div>

      <!-- scope control (segmented) -->
      <div class="flex items-center gap-3">
        <p-selectbutton [options]="scopeOptions" [(ngModel)]="scope" optionLabel="label" optionValue="value" />
      </div>

      <!-- filter chip — chip-wrapped checkbox, low-contrast (§38.2 extend) -->
      <div class="flex flex-wrap items-center gap-2 md:col-span-2">
        <span class="text-cap font-semibold uppercase tracking-[0.07em] text-muted">Chips</span>
        <label class="chip">
          <p-checkbox [(ngModel)]="checked" [binary]="true" inputId="chip-1" />
          <span>Overdue only</span>
        </label>
        <label class="chip">
          <p-checkbox [(ngModel)]="unchecked" [binary]="true" inputId="chip-2" />
          <span>Needs access</span>
        </label>
      </div>
    </div>
  </section>

  <!-- ── Tabs ─────────────────────────────────────────────────────────── -->
  <section class="rounded-lg border border-border bg-surface p-5" data-od-id="board-tabs">
    <h2 class="m-0 mb-4 text-cap font-semibold uppercase tracking-[0.07em] text-muted">Tabs</h2>
    <p-tabs value="0">
      <p-tablist>
        <p-tab value="0">Open</p-tab>
        <p-tab value="1">Waiting</p-tab>
        <p-tab value="2">Completed</p-tab>
      </p-tablist>
      <p-tabpanels>
        <p-tabpanel value="0"><p class="m-0 py-3 text-sm text-muted">Open processes.</p></p-tabpanel>
        <p-tabpanel value="1"><p class="m-0 py-3 text-sm text-muted">Waiting on a decision.</p></p-tabpanel>
        <p-tabpanel value="2"><p class="m-0 py-3 text-sm text-muted">Completed this week.</p></p-tabpanel>
      </p-tabpanels>
    </p-tabs>
  </section>

  <!-- ── Data display ─────────────────────────────────────────────────── -->
  <section class="rounded-lg border border-border bg-surface p-5" data-od-id="board-table">
    <h2 class="m-0 mb-4 text-cap font-semibold uppercase tracking-[0.07em] text-muted">Table — row expansion, sorting, pagination</h2>

    <p-table [value]="rows" dataKey="id" [expandedRowKeys]="expandedRows" (onRowToggle)="onToggleChange($event)" [paginator]="true" [rows]="10" [tableStyle]="{ 'min-width': '48rem' }">
      <ng-template pTemplate="header">
        <tr>
          <th class="w-10"></th>
          <th pSortableColumn="id">Process <p-sort-icon field="id" /></th>
          <th>State</th>
          <th>Owner</th>
          <th pSortableColumn="due">Due in <p-sort-icon field="due" /></th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-row let-expanded="expanded">
        <tr>
          <td>
            <p-button type="button" [text]="true" [rounded]="true" size="small" severity="secondary"
              [pRowToggler]="row"
              [icon]="expanded ? 'pi pi-chevron-down' : 'pi pi-chevron-right'" />
          </td>
          <td>
            <span class="font-mono text-cap text-muted">{{ row.id }}</span>
            <span class="mx-1 text-dim">·</span>
            <span class="font-medium text-fg">{{ row.name }}</span>
          </td>
          <td><span [class]="statePill(row.state).cls"><i [class]="statePill(row.state).icon"></i> {{ row.state }}</span></td>
          <td class="text-muted">{{ row.owner }}</td>
          <td class="tabular-nums text-muted">{{ row.due }}</td>
        </tr>
      </ng-template>
      <ng-template pTemplate="expandedrow" let-row>
        <tr>
          <td colspan="5">
            <p class="m-0 mb-3 text-meta text-muted">You started this process</p>
            <div class="flex flex-wrap items-center gap-2">
              <p-button label="Open process" severity="info" [outlined]="true" size="small" />
              <p-button label="Pause" severity="secondary" [text]="true" size="small" />
              <p-button label="Cancel" severity="secondary" [text]="true" size="small" />
            </div>
          </td>
        </tr>
      </ng-template>
      <ng-template pTemplate="emptymessage">
        <tr><td colspan="5"><p class="m-0 py-6 text-center text-muted">No processes match the filters.</p></td></tr>
      </ng-template>
    </p-table>
  </section>

  <!-- ── Status, metadata, loading ────────────────────────────────────── -->
  <section class="rounded-lg border border-border bg-surface p-5" data-od-id="board-status">
    <h2 class="m-0 mb-4 text-cap font-semibold uppercase tracking-[0.07em] text-muted">Status, metadata &amp; loading</h2>

    <div class="flex flex-wrap items-center gap-3">
      <span class="badge"><i class="pi pi-play"></i> Active</span>
      <span class="badge badge--alt"><i class="pi pi-pause"></i> Pended</span>
      <span class="badge badge--up"><i class="pi pi-exclamation-triangle"></i> Exception</span>
      <span class="badge badge--down"><i class="pi pi-check"></i> Completed</span>
      <span class="badge badge--down"><i class="pi pi-times"></i> Cancelled</span>
    </div>

    <div class="mt-5 flex flex-wrap items-center gap-3">
      <span class="badge badge--success"><i class="pi pi-check-circle"></i> On track</span>
      <span class="badge badge--warn"><i class="pi pi-exclamation-triangle"></i> At risk</span>
      <span class="badge badge--error"><i class="pi pi-times-circle"></i> Breached</span>
    </div>

    <div class="mt-5 flex flex-wrap items-center gap-3">
      <span class="inline-flex items-center rounded-xs border border-border px-1.5 py-0.5 font-mono text-cap text-fg">v1.4.2</span>
      <p-badge value="4" severity="secondary" />
      <p-avatar label="CY" shape="circle" />
      <span class="text-cap font-semibold uppercase tracking-[0.07em] text-muted">Label voice</span>
      <span class="font-mono text-cap text-muted">IDN-00092</span>
      <span class="tabular-nums text-fg">1,284</span>
    </div>

    <div class="mt-5 grid gap-4 md:grid-cols-2">
      <p-progressbar [value]="64" [showValue]="false" />
      <div class="grid gap-2">
        <p-skeleton width="100%" height="1rem" borderRadius="8px" />
        <p-skeleton width="70%" height="1rem" borderRadius="8px" />
      </div>
    </div>

    <p-paginator class="mt-5" [rows]="10" [totalRecords]="128" [first]="0" />
  </section>

  <!-- ── Feedback ─────────────────────────────────────────────────────── -->
  <section class="rounded-lg border border-border bg-surface p-5" data-od-id="board-feedback">
    <h2 class="m-0 mb-4 text-cap font-semibold uppercase tracking-[0.07em] text-muted">Feedback</h2>
    <div class="grid gap-3">
      <p-message severity="info" text="Uploads can take a few minutes — we will tell you when it is done." />
      <p-message severity="secondary" text="Nothing needs you right now." />
      <p-message severity="error" text="We could not save the change. Try again." />
      <div class="flex flex-wrap items-center gap-3 pt-1">
        <p-button label="Show toast" severity="secondary" [outlined]="true"
          (onClick)="toast.add({ severity: 'info', summary: 'Saved', detail: 'The design was saved.' })" />
        <p-button label="Show error toast" severity="secondary" [text]="true"
          (onClick)="toast.add({ severity: 'error', summary: 'Failed', detail: 'Could not reach the server.' })" />
      </div>
    </div>
  </section>

  <!-- ── Overlays ─────────────────────────────────────────────────────── -->
  <section class="rounded-lg border border-border bg-surface p-5" data-od-id="board-overlays">
    <h2 class="m-0 mb-4 text-cap font-semibold uppercase tracking-[0.07em] text-muted">Overlays</h2>

    <div class="flex flex-wrap items-center gap-3">
      <p-button label="Open dialog" severity="secondary" [outlined]="true" (onClick)="dialogVisible = true" />

      <p-button label="Open popover" severity="secondary" [outlined]="true" (onClick)="pop.toggle($event)" />
      <p-popover #pop>
        <div class="grid gap-1">
          <span class="text-cap font-semibold uppercase tracking-[0.07em] text-muted">This process</span>
          <a href="#" class="text-sm text-fg">Open in designer</a>
          <a href="#" class="text-sm text-fg">View history</a>
        </div>
      </p-popover>
    </div>

    <div class="mt-5 grid gap-4 md:grid-cols-2">
      <p-menu [model]="menuItems" />
      <p-accordion [value]="['0']">
        <p-accordion-panel value="0">
          <p-accordion-header>What happens on cancel?</p-accordion-header>
          <p-accordion-content>
            <p class="m-0 text-sm text-muted">A running process is terminated and cannot be resumed.</p>
          </p-accordion-content>
        </p-accordion-panel>
        <p-accordion-panel value="1">
          <p-accordion-header>Who is notified?</p-accordion-header>
          <p-accordion-content>
            <p class="m-0 text-sm text-muted">The process owner and the current assignee.</p>
          </p-accordion-content>
        </p-accordion-panel>
      </p-accordion>
    </div>

    <p-dialog header="Cancel process" [(visible)]="dialogVisible" [modal]="true" [draggable]="false" [style]="{ width: '28rem' }">
      <p class="m-0 mb-3 text-sm text-muted">This stops the process and cannot be undone. Add a reason for the record.</p>
      <textarea pTextarea [(ngModel)]="notes" rows="3" placeholder="Reason for cancelling"></textarea>
      <ng-template pTemplate="footer">
        <p-button label="Keep running" severity="secondary" [text]="true" (onClick)="dialogVisible = false" />
        <p-button label="Cancel process" severity="danger" (onClick)="dialogVisible = false" />
      </ng-template>
    </p-dialog>
  </section>

</div>

<p-toast position="bottom-right" />

    `
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
