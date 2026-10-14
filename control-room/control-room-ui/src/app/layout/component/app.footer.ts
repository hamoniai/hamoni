import { DatePipe } from '@angular/common';
import { Component } from '@angular/core';

@Component({
    standalone: true,
    selector: 'app-footer',
    imports: [DatePipe],
    template: `<div class="layout-footer">
        Copyright &copy; {{ currentDate | date:'yyyy' }} <a href="http://hamoni.ai/" target="_blank">Hamoni</a> - All Rights Reserved
    </div>`
})
export class AppFooter {
    currentDate = new Date();
}
