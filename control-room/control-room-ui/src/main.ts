import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app.config';
import { AppComponent } from './app.component';
import { preStart } from '@/environments/startup';

// Ensure that the preStart sets up and mocks etc.
preStart.then(() => {
    bootstrapApplication(AppComponent, appConfig).catch((err) => console.error(err));
});