import { DashboardMetrics, RequestResponseDTO, AIInvestigation, ApprovalRequest, AuditEvent, RunbookSearchResult, SessionUser } from './types';

// Where the API lives. Same origin by default (the dev server and the Docker image proxy /api to the backend);
// set VITE_API_BASE at build time to point a statically hosted dashboard at a backend elsewhere.
const REMOTE_API_BASE = ((import.meta as any).env?.VITE_API_BASE as string | undefined) || '';
const API_BASE: string = REMOTE_API_BASE || '/api/v1';
// A backend on free hosting sleeps when idle and can take a few minutes to wake, so keep asking that long.
const WAKE_DEADLINE_MS = REMOTE_API_BASE ? 300_000 : 0;
const HEALTH_URL = API_BASE.replace(/\/api\/v1\/?$/, '') + '/actuator/health';
const TOKEN_KEY = 'opsflow.token';

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

let live = false;                 // true once a backend has answered
let token: string | null = null;
let user: SessionUser | null = null;
const errorListeners: Array<(message: string) => void> = [];

try { token = sessionStorage.getItem(TOKEN_KEY); } catch { /* storage can be blocked; the session then lasts until reload */ }

function explain(status: number, fallback: string): string {
  if (status === 401) return 'Your session has ended. Sign in again.';
  if (status === 403) return 'Your role is not allowed to do that.';
  if (status === 409) return fallback || 'That change conflicts with the current state of the request.';
  if (status === 429) return fallback || 'Too many requests. Wait a moment and try again.';
  return fallback || `The server answered ${status}.`;
}

/** One call to the backend. Throws ApiError with a message a person can read, and tells the page about it. */
async function call<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { ...(init.headers as Record<string, string> | undefined) };
  if (init.body) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, { ...init, headers });
  } catch {
    const error = new ApiError(0, 'The backend did not answer.');
    errorListeners.forEach((listener) => listener(error.message));
    throw error;
  }
  let body: any = null;
  try { body = await res.json(); } catch { /* an empty body is fine */ }
  if (!res.ok) {
    const error = new ApiError(res.status, explain(res.status, body?.message));
    if (res.status === 401) Session.logout();
    errorListeners.forEach((listener) => listener(error.message));
    throw error;
  }
  return body?.data as T;
}

/** One look at /actuator/health. While a free host wakes the backend, it answers with an error page, not JSON. */
async function healthAnswers(): Promise<boolean> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 30_000);
  try {
    const res = await fetch(HEALTH_URL, { headers: { Accept: 'application/json' }, signal: controller.signal });
    const body = await res.json();
    return typeof body?.status === 'string';
  } catch {
    return false;
  } finally {
    clearTimeout(timer);
  }
}

export const Session = {
  isLive: () => live,
  isSignedIn: () => live && !!token && !!user,
  user: () => user,
  onError(listener: (message: string) => void) { errorListeners.push(listener); },

  /** Is there a backend behind this page? Decides between the real API and the sample data.
   *  onWaiting is called while a sleeping backend is still waking up. */
  async probe(onWaiting?: () => void): Promise<boolean> {
    const deadline = Date.now() + WAKE_DEADLINE_MS;
    for (;;) {
      live = await healthAnswers();
      if (live || Date.now() >= deadline) break;
      onWaiting?.();
      await new Promise((resolve) => setTimeout(resolve, 5000));
    }
    if (live && token) {
      try { user = await call<SessionUser>('/auth/me'); } catch { token = null; user = null; }
    }
    return live;
  },

  async login(username: string, password: string): Promise<SessionUser> {
    const result = await call<{ accessToken: string; user: SessionUser }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    token = result.accessToken;
    user = result.user;
    try { sessionStorage.setItem(TOKEN_KEY, token); } catch { /* see above */ }
    return user;
  },

  logout() {
    token = null;
    user = null;
    try { sessionStorage.removeItem(TOKEN_KEY); } catch { /* see above */ }
  },
};

// Sample data, used only when no backend answers (the GitHub Pages demo).
let mockRequests: RequestResponseDTO[] = [
  {
    id: 1,
    externalReference: 'REQ-948210',
    title: 'I was charged ₹500 but my order still says payment pending.',
    description: 'Customer attempted payment at 10:31 AM. Money was deducted from bank account (Txn #TXN-99482104), but order status has not updated from PENDING to CONFIRMED.',
    category: 'PAYMENT',
    priority: 'HIGH',
    status: 'INVESTIGATING',
    createdBy: 'customer_user_99',
    assignedTo: 'agent',
    classificationSummary: 'Payment authorization succeeded but downstream order status synchronization appears delayed.',
    confidenceScore: 0.95,
    version: 1,
    createdAt: new Date(Date.now() - 3600000).toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 2,
    externalReference: 'REQ-948211',
    title: 'Flight booking failed on checkout but bank account debited.',
    description: 'Customer tried booking flight SQ-321. Booking failed with Timeout, but money was deducted from wallet.',
    category: 'BOOKING',
    priority: 'HIGH',
    status: 'PENDING_APPROVAL',
    createdBy: 'customer_user_102',
    assignedTo: 'manager',
    classificationSummary: 'Inventory lock timeout during payment settlement.',
    confidenceScore: 0.91,
    version: 1,
    createdAt: new Date(Date.now() - 7200000).toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 3,
    externalReference: 'REQ-948212',
    title: 'Driver marked order delivered but customer never received it.',
    description: 'E-commerce package #ORD-8842 marked DELIVERED by courier at 2:15 PM. Customer checked doorstep and front desk, no package received.',
    category: 'DELIVERY',
    priority: 'MEDIUM',
    status: 'ASSIGNED',
    createdBy: 'support_agent_03',
    assignedTo: 'agent',
    classificationSummary: 'Driver delivery mismatch exception.',
    confidenceScore: 0.88,
    version: 1,
    createdAt: new Date(Date.now() - 10800000).toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 4,
    externalReference: 'REQ-948213',
    title: 'Our API integration suddenly stopped responding with HTTP 504 Gateway Timeout.',
    description: 'SaaS partner integration receiving intermittent 504 gateway timeouts when invoking /v2/orders endpoint.',
    category: 'TECHNICAL',
    priority: 'CRITICAL',
    status: 'CREATED',
    createdBy: 'api_partner_saas',
    classificationSummary: 'Technical service gateway timeout error.',
    confidenceScore: 0.96,
    version: 1,
    createdAt: new Date(Date.now() - 14400000).toISOString(),
    updatedAt: new Date().toISOString(),
  }
];

let mockApprovals: ApprovalRequest[] = [
  {
    id: 1,
    requestId: 2,
    actionType: 'REPLAY_PAYMENT_EVENT',
    proposedBy: 'ai-assistant',
    status: 'PENDING',
    reasoning: 'AI Investigation recommended event replay for failed flight reservation sync. Bank transaction TXN-88421 confirmed.',
    payloadJson: '{"recommendedAction":"REPLAY_PAYMENT_EVENT","requestId":2}',
    createdAt: new Date(Date.now() - 1800000).toISOString(),
  }
];

let mockAuditEvents: AuditEvent[] = [
  {
    id: 1,
    who: 'ai-assistant',
    what: 'AI_INVESTIGATION_COMPLETED',
    why: 'Payment authorization succeeded but downstream order status synchronization delayed',
    requestId: 1,
    correlationId: 'CORR-884920A1',
    action: 'INVESTIGATE_AI',
    result: 'SUCCESS',
    createdAt: new Date(Date.now() - 1200000).toISOString(),
  },
  {
    id: 2,
    who: 'manager',
    what: 'APPROVAL_REQUESTED',
    why: 'Replay payment event recommended',
    requestId: 2,
    correlationId: 'CORR-772109F2',
    action: 'REPLAY_PAYMENT_EVENT',
    result: 'PENDING',
    createdAt: new Date(Date.now() - 1800000).toISOString(),
  }
];

export const ApiService = {
  async getDashboardMetrics(): Promise<DashboardMetrics> {
    if (live) return call<DashboardMetrics>('/analytics/dashboard');

    const total = mockRequests.length;
    const resolved = mockRequests.filter(r => r.status === 'RESOLVED' || r.status === 'CLOSED').length;
    return {
      totalRequests: total,
      activeRequests: total - resolved,
      resolvedRequests: resolved,
      pendingApprovals: mockApprovals.filter(a => a.status === 'PENDING').length,
      resolutionRatePercentage: total > 0 ? Math.round((resolved / total) * 1000) / 10 : 100,
      outboxPending: 0,
      eventsPublished: 0,
      eventsConsumed: 0,
      aiAvgConfidence: null,
      priorityDistribution: { LOW: 0, MEDIUM: 1, HIGH: 2, CRITICAL: 1 },
      statusDistribution: { CREATED: 1, CLASSIFIED: 0, ASSIGNED: 1, INVESTIGATING: 1, PENDING_APPROVAL: 1, ACTION_IN_PROGRESS: 0, RESOLVED: 0, CLOSED: 0, ESCALATED: 0, WAITING_FOR_USER: 0 },
      categoryDistribution: { PAYMENT: 1, BOOKING: 1, DELIVERY: 1, TECHNICAL: 1, ORDER: 0, ACCOUNT: 0, SERVICE: 0, ACCESS: 0, OTHER: 0 },
    };
  },

  async getRequests(): Promise<RequestResponseDTO[]> {
    if (live) {
      const page = await call<{ content: RequestResponseDTO[] }>('/requests?size=50');
      return page.content;
    }
    return [...mockRequests];
  },

  async createRequest(title: string, description: string, category: string, priority: string): Promise<RequestResponseDTO> {
    if (live) {
      return call<RequestResponseDTO>('/requests', { method: 'POST', body: JSON.stringify({ title, description, category, priority }) });
    }

    const newReq: RequestResponseDTO = {
      id: Date.now(),
      externalReference: `REQ-${Math.floor(Math.random() * 900000 + 100000)}`,
      title,
      description,
      category: category as any || 'OTHER',
      priority: priority as any || 'MEDIUM',
      status: 'CREATED',
      createdBy: 'sample-user',
      version: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    mockRequests.unshift(newReq);
    return newReq;
  },

  async investigateRequestWithAI(requestId: number): Promise<AIInvestigation> {
    if (live) return call<AIInvestigation>(`/requests/${requestId}/investigate`, { method: 'POST' });

    const req = mockRequests.find(r => r.id === requestId);
    if (req) {
      req.status = 'PENDING_APPROVAL';
    }

    const newInv: AIInvestigation = {
      id: Date.now(),
      requestId,
      investigatedBy: 'sample-provider',
      summary: 'Sample result. With a backend connected, this comes from the investigation service: it reads the request and its audit history, searches the runbooks, and asks the AI provider for a recommendation.',
      evidenceJson: JSON.stringify([
        'The request and its audit history, read through the tool registry.',
        'The closest runbook found by the runbook search.',
      ]),
      likelyCause: 'Sample cause: the payment succeeded but the order was not updated.',
      confidenceScore: 0.89,
      recommendedAction: 'REPLAY_PAYMENT_EVENT',
      requiresApproval: true,
      relevantRunbookTitle: 'Payment Synchronization Troubleshooting Runbook',
      createdAt: new Date().toISOString(),
    };

    mockApprovals.unshift({
      id: Date.now(),
      requestId,
      actionType: 'REPLAY_PAYMENT_EVENT',
      proposedBy: 'sample-provider',
      status: 'PENDING',
      reasoning: newInv.likelyCause,
      payloadJson: JSON.stringify({ action: 'REPLAY_PAYMENT_EVENT', requestId }),
      createdAt: new Date().toISOString(),
    });

    return newInv;
  },

  async getPendingApprovals(): Promise<ApprovalRequest[]> {
    if (live) return call<ApprovalRequest[]>('/approvals/pending');
    return mockApprovals.filter(a => a.status === 'PENDING');
  },

  async approveAction(approvalId: number): Promise<void> {
    if (live) {
      await call<ApprovalRequest>(`/approvals/${approvalId}/approve`, { method: 'POST', body: JSON.stringify({ comment: 'Approved from the dashboard' }) });
      return;
    }
    const app = mockApprovals.find(a => a.id === approvalId);
    if (app) {
      app.status = 'APPROVED';
      const req = mockRequests.find(r => r.id === app.requestId);
      if (req) req.status = 'RESOLVED';
    }
  },

  async getAuditTrail(): Promise<AuditEvent[]> {
    if (live) {
      const page = await call<{ content: AuditEvent[] }>('/audit/events');
      return page.content;
    }
    return mockAuditEvents;
  },

  async searchRunbooks(query: string): Promise<RunbookSearchResult[]> {
    if (live) return call<RunbookSearchResult[]>(`/knowledge/search?query=${encodeURIComponent(query)}`);

    return [
      {
        chunkId: 1,
        documentId: 101,
        documentTitle: 'Payment Synchronization Troubleshooting Runbook',
        snippet: 'When a customer reports payment deduction while order status remains PENDING: verify the payment gateway transaction status using the gateway transaction id, then check whether the order event was published and consumed.',
        score: 0.62
      },
      {
        chunkId: 2,
        documentId: 102,
        documentTitle: 'Booking Failure & Inventory Recovery Guide',
        snippet: 'Booking failures with debited accounts usually occur during concurrent inventory lock timeouts. Action steps: verify the seat or inventory state in the booking database.',
        score: 0.31
      }
    ];
  }
};
