import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Ramp-up to 50 concurrent users
    { duration: '1m', target: 200 },  // Peak load at 200 users
    { duration: '30s', target: 0 },   // Cool down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
    http_req_failed: ['rate<0.01'],   // Error rate must be less than 1%
  },
};

const BASE_URL = 'http://localhost:8080/api/v1';

export function setup() {
  // Login to acquire JWT token
  const payload = JSON.stringify({
    username: 'operator',
    password: 'operator123',
  });
  const params = { headers: { 'Content-Type': 'application/json' } };
  const res = http.post(`${BASE_URL}/auth/login`, payload, params);
  
  if (res.status === 200) {
    const body = JSON.parse(res.body);
    return { token: body.data.accessToken };
  }
  return { token: '' };
}

export default function (data) {
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.token}`,
      'X-Correlation-ID': `K6-LOAD-${Math.floor(Math.random() * 1000000)}`,
    },
  };

  // Step 1: Create Operational Request
  const createPayload = JSON.stringify({
    title: 'k6 Synthetic Load Request ' + Math.floor(Math.random() * 10000),
    description: 'Automated performance benchmark checking outbox transactional persistence and API latency.',
    category: 'PAYMENT',
    priority: 'HIGH',
  });

  const createRes = http.post(`${BASE_URL}/requests`, createPayload, params);
  check(createRes, {
    'request created successfully': (r) => r.status === 201 || r.status === 200,
  });

  // Step 2: Search Active Requests
  const searchRes = http.get(`${BASE_URL}/requests?page=0&size=10`, params);
  check(searchRes, {
    'search requests successful': (r) => r.status === 200,
  });

  sleep(1);
}
