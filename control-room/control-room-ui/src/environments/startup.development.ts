/**
 * Startup code, only for development mode.
 */
import { setupWorker } from 'msw/browser'
import { success_handlers } from '@/mock/http_handlers'

// Configure MSW. Not the best place for this, but needs to be included in only this configuration.
export const worker = setupWorker(...success_handlers)
export const preStart: Promise<any> = worker.start();
