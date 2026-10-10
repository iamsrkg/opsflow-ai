// Load test for the request API. Run by load-tests/run.sh against the full Docker Compose stack.
//
// Each virtual user repeats what an operator does: create a request, read it back, list requests,
// and move it one step along the workflow. Every create and every transition also writes an event
// to the outbox, so this load is what the event pipeline has to keep up with.
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE || 'http://localhost:3000';
const API = `${BASE}/api/v1`;
const VUS = Number(__ENV.VUS || 50);

export const options = {
  scenarios: {
    operators: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: VUS },   // ramp up
        { duration: '60s', target: VUS },   // hold
        { duration: '5s', target: 0 },      // ramp down
      ],
      gracefulRampDown: '5s',
    },
  },
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // one threshold per request type makes k6 report each of them separately
    'http_req_duration{name:create}': ['p(95)<2000'],
    'http_req_duration{name:get}': ['p(95)<2000'],
    'http_req_duration{name:list}': ['p(95)<2000'],
    'http_req_duration{name:transition}': ['p(95)<2000'],
  },
};

// Sign in once and share the token: hashing a password is deliberately slow and is not what is being measured.
export function setup() {
  const res = http.post(`${API}/auth/login`, JSON.stringify({ username: 'agent', password: 'agent123' }),
    { headers: { 'Content-Type': 'application/json' } });
  check(res, { 'signed in': (r) => r.status === 200 });
  return { token: res.json('data.accessToken') };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}`, 'Content-Type': 'application/json' };

  const created = http.post(`${API}/requests`, JSON.stringify({
    title: `Load test: charged but order still pending (vu ${__VU}, iteration ${__ITER})`,
    description: 'Payment was deducted from the card, the order status did not update.',
  }), { headers, tags: { name: 'create' } });
  if (!check(created, { 'created': (r) => r.status === 201 })) return;
  const id = created.json('data.id');
  const version = created.json('data.version');

  check(http.get(`${API}/requests/${id}`, { headers, tags: { name: 'get' } }), { 'read back': (r) => r.status === 200 });
  check(http.get(`${API}/requests?size=20`, { headers, tags: { name: 'list' } }), { 'listed': (r) => r.status === 200 });
  check(http.post(`${API}/requests/${id}/transition`,
    JSON.stringify({ targetStatus: 'CLASSIFIED', comment: 'load test', expectedVersion: version }),
    { headers, tags: { name: 'transition' } }), { 'moved to CLASSIFIED': (r) => r.status === 200 });
}

export function handleSummary(data) {
  const m = data.metrics;
  const ms = (metric, stat) => (m[metric] ? Math.round(m[metric].values[stat] * 10) / 10 : null);
  const result = {
    virtualUsers: VUS,
    requests: m.http_reqs.values.count,
    requestsPerSecond: Math.round(m.http_reqs.values.rate * 10) / 10,
    failedPercent: Math.round(m.http_req_failed.values.rate * 10000) / 100,
    iterations: m.iterations.values.count,
    all: { med: ms('http_req_duration', 'med'), p95: ms('http_req_duration', 'p(95)'), p99: ms('http_req_duration', 'p(99)'), max: ms('http_req_duration', 'max') },
  };
  for (const name of ['create', 'get', 'list', 'transition']) {
    const key = `http_req_duration{name:${name}}`;
    result[name] = { med: ms(key, 'med'), p95: ms(key, 'p(95)'), p99: ms(key, 'p(99)') };
  }
  return { '/results/k6.json': JSON.stringify(result, null, 2), stdout: JSON.stringify(result, null, 2) + '\n' };
}
