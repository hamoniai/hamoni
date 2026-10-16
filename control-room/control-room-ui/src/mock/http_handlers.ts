import { http, HttpResponse } from 'msw'
import { User } from '@/app/services/auth.service'
import { Organisation } from '@/app/services/organisation.service';

/**
 * Create an API url
 * @param path The service path
 * @returns The full URI.
 */
function api(path: string) {
  return new URL('/api' + path, window.location.href).href
}

/**
 * Perform type erasure on an object
 * @param obj The object to erase
 * @returns A basic object with the same properties, sanitised and ready for wire sending.
 */
function toObject(obj: any) {
  const serialised = JSON.stringify(obj, (key, value) => {
    if (value instanceof Date) {
      return value.toISOString();
    }
    return value;
  });
  return JSON.parse(serialised);
}

// Objects for client success (cs) cases
const defaultUserUUID = window.crypto.randomUUID()

const crUser: User = {
  id: defaultUserUUID,
  name: 'Bob Smith',
  email: "bob.smith@example.com"
}

const crOrganisations: Organisation[] = [
  {
    id: window.crypto.randomUUID(),
    name: 'ABC Limited'
  }
]

export const success_handlers = [
  // Auth services
  http.get(api(`/auth/me`), () => {
    return HttpResponse.json(toObject(crUser))
  }),
  http.get(api(`/auth/logout`), () => {
    return HttpResponse.json(toObject(true), { status: 200 });
  }),

  // Organisation services
  http.get(api(`/organisation`), () => {
    return HttpResponse.json(toObject(crOrganisations), { status: 200 });
  }),
  http.post(api(`/organisation/select`), () => {
    return new HttpResponse(null, { status: 204 });
  })

  // http.get<{ id: string }>(api(`/teams/:id`), ({ params }) => {
  //   const { id } = params;
  //   if (id == defaultClientUUID || id === 'default') {
  //     return HttpResponse.json(toObject(csClient));
  //   } else {
  //     return HttpResponse.json(null, { status: 404 });
  //   }
  // }),
  // http.post(api(`/teams/:id`), () => {
  //   return new HttpResponse(null, { status: 204 });
  // }),
];
