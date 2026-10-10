import { DashboardMetrics, RequestResponseDTO, AIInvestigation, ApprovalRequest, AuditEvent, RunbookSearchResult } from './types';

const API_BASE = '/api/v1';

// Initial Mock Data Fallback
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
    try {
      const res = await fetch(`${API_BASE}/analytics/dashboard`);
      if (res.ok) {
        const json = await res.json();
        return json.data;
      }
    } catch (e) {
      console.warn("Using offline mock data fallback for analytics");
    }

    const total = mockRequests.length;
    const resolved = mockRequests.filter(r => r.status === 'RESOLVED' || r.status === 'CLOSED').length;
    return {
      totalRequests: total,
      activeRequests: total - resolved,
      resolvedRequests: resolved,
      pendingApprovals: mockApprovals.filter(a => a.status === 'PENDING').length,
      resolutionRatePercentage: 75.0,
      kafkaConsumerLag: 14,
      aiAvgConfidence: 0.93,
      priorityDistribution: { LOW: 0, MEDIUM: 1, HIGH: 2, CRITICAL: 1 },
      statusDistribution: { CREATED: 1, CLASSIFIED: 0, ASSIGNED: 1, INVESTIGATING: 1, PENDING_APPROVAL: 1, ACTION_IN_PROGRESS: 0, RESOLVED: 0, CLOSED: 0, ESCALATED: 0, WAITING_FOR_USER: 0 },
      categoryDistribution: { PAYMENT: 1, BOOKING: 1, DELIVERY: 1, TECHNICAL: 1, ORDER: 0, ACCOUNT: 0, SERVICE: 0, ACCESS: 0, OTHER: 0 },
    };
  },

  async getRequests(): Promise<RequestResponseDTO[]> {
    try {
      const res = await fetch(`${API_BASE}/requests`);
      if (res.ok) {
        const json = await res.json();
        return json.data.content || json.data;
      }
    } catch (e) {}
    return [...mockRequests];
  },

  async createRequest(title: string, description: string, category: string, priority: string): Promise<RequestResponseDTO> {
    try {
      const res = await fetch(`${API_BASE}/requests`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title, description, category, priority })
      });
      if (res.ok) {
        const json = await res.json();
        return json.data;
      }
    } catch (e) {}

    const newReq: RequestResponseDTO = {
      id: Date.now(),
      externalReference: `REQ-${Math.floor(Math.random() * 900000 + 100000)}`,
      title,
      description,
      category: category as any || 'OTHER',
      priority: priority as any || 'MEDIUM',
      status: 'CREATED',
      createdBy: 'operator',
      version: 1,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    mockRequests.unshift(newReq);
    return newReq;
  },

  async investigateRequestWithAI(requestId: number): Promise<AIInvestigation> {
    try {
      const res = await fetch(`${API_BASE}/requests/${requestId}/investigate`, { method: 'POST' });
      if (res.ok) {
        const json = await res.json();
        return json.data;
      }
    } catch (e) {}

    // Mock AI Investigation Response
    const req = mockRequests.find(r => r.id === requestId);
    if (req) {
      req.status = 'PENDING_APPROVAL';
    }

    const newInv: AIInvestigation = {
      id: Date.now(),
      requestId,
      investigatedBy: 'ai-assistant',
      summary: 'AI Investigation analyzed payment authorization records, Kafka event consumer lag, and matched payment synchronization runbooks.',
      evidenceJson: JSON.stringify([
        "Payment authorization TXN-99482104 succeeded at 10:31:22 in Payment Gateway.",
        "Order status remained PENDING at 10:31:25 due to Kafka consumer group lag.",
        "Kafka consumer lag observed on topic 'opsflow.payment.events' (142 uncommitted messages).",
        "Matched Runbook 'Payment Synchronization Troubleshooting Runbook' with 94% vector similarity score."
      ]),
      likelyCause: 'Payment-to-order event synchronization delay due to temporary Kafka consumer lag.',
      confidenceScore: 0.89,
      recommendedAction: 'REPLAY_PAYMENT_EVENT',
      requiresApproval: true,
      relevantRunbookTitle: 'Payment Synchronization Troubleshooting Runbook',
      createdAt: new Date().toISOString(),
    };

    // Auto-create pending approval
    mockApprovals.unshift({
      id: Date.now(),
      requestId,
      actionType: 'REPLAY_PAYMENT_EVENT',
      proposedBy: 'ai-assistant',
      status: 'PENDING',
      reasoning: newInv.likelyCause,
      payloadJson: JSON.stringify({ action: 'REPLAY_PAYMENT_EVENT', requestId }),
      createdAt: new Date().toISOString(),
    });

    return newInv;
  },

  async getPendingApprovals(): Promise<ApprovalRequest[]> {
    try {
      const res = await fetch(`${API_BASE}/approvals/pending`);
      if (res.ok) {
        const json = await res.json();
        return json.data;
      }
    } catch (e) {}
    return mockApprovals.filter(a => a.status === 'PENDING');
  },

  async approveAction(approvalId: number): Promise<void> {
    try {
      await fetch(`${API_BASE}/approvals/${approvalId}/approve`, { method: 'POST' });
    } catch (e) {}
    const app = mockApprovals.find(a => a.id === approvalId);
    if (app) {
      app.status = 'APPROVED';
      const req = mockRequests.find(r => r.id === app.requestId);
      if (req) req.status = 'RESOLVED';
    }
  },

  async getAuditTrail(): Promise<AuditEvent[]> {
    try {
      const res = await fetch(`${API_BASE}/audit/events`);
      if (res.ok) {
        const json = await res.json();
        return json.data.content || json.data;
      }
    } catch (e) {}
    return mockAuditEvents;
  },

  async searchRunbooks(query: string): Promise<RunbookSearchResult[]> {
    try {
      const res = await fetch(`${API_BASE}/knowledge/search?query=${encodeURIComponent(query)}`);
      if (res.ok) {
        const json = await res.json();
        return json.data;
      }
    } catch (e) {}

    return [
      {
        chunkId: 1,
        documentId: 101,
        documentTitle: 'Payment Synchronization Troubleshooting Runbook',
        snippet: 'When a customer reports payment deduction while order status remains PENDING: Verify Payment Gateway transaction status using Gateway TXN ID. Check Kafka consumer lag on opsflow.payment.events topic.',
        score: 0.95
      },
      {
        chunkId: 2,
        documentId: 102,
        documentTitle: 'Booking Failure & Inventory Recovery Guide',
        snippet: 'Booking failures with debited accounts usually occur during concurrent inventory lock timeouts. Action steps: Verify seat/inventory state in Booking DB.',
        score: 0.88
      }
    ];
  }
};
