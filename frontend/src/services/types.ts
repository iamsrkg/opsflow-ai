export type Category = 'PAYMENT' | 'ORDER' | 'BOOKING' | 'DELIVERY' | 'ACCOUNT' | 'TECHNICAL' | 'SERVICE' | 'ACCESS' | 'OTHER';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type RequestStatus = 'CREATED' | 'CLASSIFIED' | 'ASSIGNED' | 'INVESTIGATING' | 'PENDING_APPROVAL' | 'ACTION_IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'ESCALATED' | 'WAITING_FOR_USER';

export interface UserDTO {
  id: number;
  username: string;
  fullName: string;
  email: string;
  role: 'ADMIN' | 'MANAGER' | 'AGENT' | 'OPERATOR' | 'VIEWER';
}

export interface RequestResponseDTO {
  id: number;
  externalReference: string;
  title: string;
  description: string;
  category: Category;
  priority: Priority;
  status: RequestStatus;
  createdBy: string;
  assignedTo?: string;
  classificationSummary?: string;
  confidenceScore?: number;
  version: number;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
}

export interface AIInvestigation {
  id: number;
  requestId: number;
  investigatedBy: string;
  summary: string;
  evidenceJson: string; // stringified array
  likelyCause: string;
  confidenceScore: number;
  recommendedAction: string;
  requiresApproval: boolean;
  relevantRunbookTitle: string;
  createdAt: string;
}

export interface ApprovalRequest {
  id: number;
  requestId: number;
  actionType: string;
  proposedBy: string;
  approvedBy?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reasoning: string;
  payloadJson: string;
  createdAt: string;
}

export interface AuditEvent {
  id: number;
  who: string;
  what: string;
  why?: string;
  requestId?: number;
  correlationId?: string;
  action?: string;
  result: string;
  metadataJson?: string;
  createdAt: string;
}

export interface RunbookSearchResult {
  chunkId: number;
  documentId: number;
  documentTitle: string;
  snippet: string;
  score: number;
}

export interface DashboardMetrics {
  totalRequests: number;
  activeRequests: number;
  resolvedRequests: number;
  pendingApprovals: number;
  resolutionRatePercentage: number;
  /** events written to the outbox that Kafka has not acknowledged yet */
  outboxPending: number;
  eventsPublished: number;
  /** events a consumer has finished handling */
  eventsConsumed: number;
  /** average over the stored investigations; null when there are none */
  aiAvgConfidence: number | null;
  priorityDistribution: Record<Priority, number>;
  statusDistribution: Record<RequestStatus, number>;
  categoryDistribution: Record<Category, number>;
}

export interface SessionUser {
  username: string;
  fullName: string;
  role: string;
}
