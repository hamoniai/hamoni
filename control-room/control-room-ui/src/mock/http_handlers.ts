import { http, HttpResponse } from 'msw'
import { User, Organisation } from '@/app/services/auth.service'

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

// Objects for auth services
const defaultUserUUID = window.crypto.randomUUID()

const authUser: User = {
  id: defaultUserUUID,
  name: 'Bob Smith',
  email: "bob.smith@example.com",
  issuer: "https://accounts.google.com",
  issuerSubject: "107654321098765432109"
}

const authUserOrganisations: Organisation[] = [
  {
    id: window.crypto.randomUUID(),
    name: 'ABC Limited'
  }
]

export const success_handlers = [
  // Auth services
  http.get(api(`/auth/profile`), ({ request }) => {
    const url = new URL(request.url);
    const hydrate = url.searchParams.get('hydrate')?.split(" ") || []

    if (hydrate.includes("organisations")) {
      return HttpResponse.json(toObject({...authUser, organisations: authUserOrganisations}))
    } else {
      return HttpResponse.json(toObject(authUser))
    }
  }),
  http.get(api(`/auth/profile/organisations`), () => {
    return HttpResponse.json(toObject(authUserOrganisations), { status: 200 });
  }),
  http.get(api(`/auth/logout`), () => {
    return HttpResponse.json(toObject(true), { status: 200 });
  }),

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
