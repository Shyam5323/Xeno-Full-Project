# Xeno Frontend — Shopify Insights Dashboard

Next.js App Router dashboard for the Xeno multi‑tenant Shopify ingestion service. Users onboard shops, trigger syncs, and view analytics.

## Features

- Secure auth using backend‑issued JWT (HttpOnly cookie `auth_token`)
- Multi‑tenant: connect multiple Shopify stores and scope analytics with `X-Tenant-ID`
- Analytics: revenue totals and daily, order status breakdown, top customers, stockouts, AOV, UPT, cancellation rate, top products, new vs returning
- Simple, responsive UI; charts via Recharts

## Requirements

- Node.js 20+
- A running backend (see `xenoBackend/README.md`) and its base URL

## Environment

Create `.env.local` in `XenoTaskFrontend/`:

```
NEXT_PUBLIC_API_BASE_URL=https://xenotask-production.up.railway.app/api
```

Notes
- Don’t include a trailing slash; the app joins paths safely.
- All server actions and fetches derive from this base.

## Run

Install deps and start dev:

```
npm install
npm run dev
```

Open http://localhost:3000

Routes
- `/` → redirects to `/tenants` if authenticated, else `/login`
- `/login` → login form; on success sets `auth_token` and redirects
- `/register` → registration form
- `/tenants` → list & onboard tenants (validates Shopify token before calling backend)
- `/tenants/[tenantId]` → per‑tenant analytics dashboard

Auth & middleware
- `middleware.ts` protects `/tenants`, `/analytics`, etc.; unauthenticated users are redirected to `/login?next=…`
- Server actions in `app/login/page.tsx` and `app/actions/auth.ts` set/clear cookies

API usage (derived from backend controllers)
- Auth: POST `${API_BASE}/auth/login`, `${API_BASE}/auth/register`
- Tenant access: GET `${API_BASE}/tenant-access/my-tenants`, POST `${API_BASE}/tenant-access/onboard`, etc.
- Analytics: GET `${API_BASE}/analytics/*` with header `X-Tenant-ID: <tenantId>`

Example requests the app makes (headers abbreviated):
- Daily revenue: `GET /analytics/revenue/daily?start=YYYY-MM-DD&end=YYYY-MM-DD` + `Authorization: Bearer <JWT>`, `X-Tenant-ID`
- Top customers: `GET /analytics/customers/top?limit=5` + headers above
- Top products: `GET /analytics/products/top?by=revenue&limit=5&start=ISO&end=ISO` + headers above

Build

```
npm run build
npm start
```

Deploy (Vercel suggested)
- Set `NEXT_PUBLIC_API_BASE_URL` in Vercel project settings
- Use default build command `npm run build` and start `npm start` (or Vercel serverless)

## Project structure (key paths)

```
src/
  app/
    login/page.tsx            # Login (server action)
    register/page.tsx         # Register
    tenants/page.tsx          # Onboard/list tenants
    tenants/[tenantId]/page.tsx  # Analytics dashboard
    actions/auth.ts           # Logout server action
    layout.tsx, page.tsx      # Root layout/redirect
  components/analytics/       # Recharts components
  components/auth/            # Auth forms
```

## Troubleshooting

- 401 on analytics calls: login again; ensure `auth_token` cookie is present
- 403 on analytics: user lacks access to tenant; check `/tenant-access/my-tenants`
- Blank charts: ensure `NEXT_PUBLIC_API_BASE_URL` points to the backend `/api` and requests include `X-Tenant-ID`

---

For architecture and APIs, see the root `documentation.md`.