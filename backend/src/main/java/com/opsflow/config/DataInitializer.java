package com.opsflow.config;

import com.opsflow.approval.ApprovalService;
import com.opsflow.identity.Role;
import com.opsflow.identity.User;
import com.opsflow.identity.UserRepository;
import com.opsflow.knowledge.KnowledgeChunk;
import com.opsflow.knowledge.KnowledgeChunkRepository;
import com.opsflow.knowledge.KnowledgeDocument;
import com.opsflow.knowledge.KnowledgeDocumentRepository;
import com.opsflow.request.Category;
import com.opsflow.request.Priority;
import com.opsflow.request.Request;
import com.opsflow.request.RequestRepository;
import com.opsflow.request.RequestStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final RequestRepository requestRepository;
    private final ApprovalService approvalService;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           KnowledgeDocumentRepository documentRepository,
                           KnowledgeChunkRepository chunkRepository,
                           RequestRepository requestRepository,
                           ApprovalService approvalService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.requestRepository = requestRepository;
        this.approvalService = approvalService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            seedUsers();
        }
        if (documentRepository.count() == 0) {
            seedRunbooks();
        }
        if (requestRepository.count() == 0) {
            seedRequests();
        }
    }

    private void seedUsers() {
        logger.info("Seeding default application users...");

        userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .fullName("System Administrator")
                .email("admin@opsflow.ai")
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        userRepository.save(User.builder()
                .username("manager")
                .password(passwordEncoder.encode("manager123"))
                .fullName("Sudheer Manager")
                .email("manager@opsflow.ai")
                .role(Role.MANAGER)
                .enabled(true)
                .build());

        userRepository.save(User.builder()
                .username("agent")
                .password(passwordEncoder.encode("agent123"))
                .fullName("Alex Agent")
                .email("agent@opsflow.ai")
                .role(Role.AGENT)
                .enabled(true)
                .build());

        userRepository.save(User.builder()
                .username("operator")
                .password(passwordEncoder.encode("operator123"))
                .fullName("Op User")
                .email("operator@opsflow.ai")
                .role(Role.OPERATOR)
                .enabled(true)
                .build());

        userRepository.save(User.builder()
                .username("viewer")
                .password(passwordEncoder.encode("viewer123"))
                .fullName("Read-Only Auditor")
                .email("viewer@opsflow.ai")
                .role(Role.VIEWER)
                .enabled(true)
                .build());
    }

    private void seedRunbooks() {
        logger.info("Seeding operational runbooks for RAG knowledge vector store...");

        KnowledgeDocument paymentDoc = documentRepository.save(KnowledgeDocument.builder()
                .title("Payment Synchronization Troubleshooting Runbook")
                .category("PAYMENT")
                .content("When a customer reports payment deduction while order status remains PENDING: 1. Verify Payment Gateway transaction status using Gateway TXN ID. 2. Check Kafka consumer lag on 'opsflow.payment.events' topic. 3. If Gateway authorized but order events were missed due to consumer lag, trigger REPLAY_PAYMENT_EVENT. 4. Execution requires Manager approval.")
                .tags("payment, sync, kafka, lag, replay")
                .build());

        chunkRepository.save(KnowledgeChunk.builder()
                .documentId(paymentDoc.getId())
                .chunkIndex(0)
                .text(paymentDoc.getContent())
                .build());

        KnowledgeDocument bookingDoc = documentRepository.save(KnowledgeDocument.builder()
                .title("Booking Failure & Inventory Recovery Guide")
                .category("BOOKING")
                .content("Booking failures with debited accounts usually occur during concurrent inventory lock timeouts. Action steps: 1. Verify seat/inventory state in Booking DB. 2. If inventory is still available, manually trigger INVENTORY_ALLOCATION_RETRY. 3. If inventory is exhausted, issue automatic REFUND_INITIATE. Requires Manager approval.")
                .tags("booking, flight, hotel, inventory, refund")
                .build());

        chunkRepository.save(KnowledgeChunk.builder()
                .documentId(bookingDoc.getId())
                .chunkIndex(0)
                .text(bookingDoc.getContent())
                .build());

        KnowledgeDocument kafkaDoc = documentRepository.save(KnowledgeDocument.builder()
                .title("Kafka Consumer Lag & Outbox Event Recovery Guide")
                .category("TECHNICAL")
                .content("For elevated Kafka lag or dead-letter queue events: 1. Inspect OutboxEvent table for FAILED events. 2. Verify consumer group partition assignment. 3. Execute REPLAY_FAILED_OUTBOX_EVENTS after resolving downstream bottlenecks.")
                .tags("kafka, outbox, consumer, lag, dlq")
                .build());

        chunkRepository.save(KnowledgeChunk.builder()
                .documentId(kafkaDoc.getId())
                .chunkIndex(0)
                .text(kafkaDoc.getContent())
                .build());
    }

    private void seedRequests() {
        logger.info("Seeding sample operational requests...");

        Request r1 = requestRepository.save(Request.builder()
                .externalReference("REQ-948210")
                .title("I was charged ₹500 but my order still says payment pending.")
                .description("Customer attempted payment at 10:31 AM. Money was deducted from bank account (Txn #TXN-99482104), but order status has not updated from PENDING to CONFIRMED.")
                .category(Category.PAYMENT)
                .priority(Priority.HIGH)
                .status(RequestStatus.INVESTIGATING)
                .createdBy("customer_user_99")
                .assignedTo("agent")
                .classificationSummary("Payment authorization succeeded but downstream order status synchronization appears delayed.")
                .confidenceScore(0.95)
                .build());

        Request r2 = requestRepository.save(Request.builder()
                .externalReference("REQ-948211")
                .title("Flight booking failed on checkout but bank account debited.")
                .description("Customer tried booking flight SQ-321. Booking failed with 'Timeout', but money was deducted from wallet.")
                .category(Category.BOOKING)
                .priority(Priority.HIGH)
                .status(RequestStatus.PENDING_APPROVAL)
                .createdBy("customer_user_102")
                .assignedTo("manager")
                .classificationSummary("Inventory lock timeout during payment settlement.")
                .confidenceScore(0.91)
                .build());

        Request r3 = requestRepository.save(Request.builder()
                .externalReference("REQ-948212")
                .title("Driver marked order delivered but customer never received it.")
                .description("E-commerce package #ORD-8842 marked DELIVERED by courier at 2:15 PM. Customer checked doorstep and front desk, no package received.")
                .category(Category.DELIVERY)
                .priority(Priority.MEDIUM)
                .status(RequestStatus.ASSIGNED)
                .createdBy("support_agent_03")
                .assignedTo("agent")
                .classificationSummary("Driver delivery mismatch exception.")
                .confidenceScore(0.88)
                .build());

        Request r4 = requestRepository.save(Request.builder()
                .externalReference("REQ-948213")
                .title("Our API integration suddenly stopped responding with HTTP 504 Gateway Timeout.")
                .description("SaaS partner integration receiving intermittent 504 gateway timeouts when invoking /v2/orders endpoint.")
                .category(Category.TECHNICAL)
                .priority(Priority.CRITICAL)
                .status(RequestStatus.CREATED)
                .createdBy("api_partner_saas")
                .classificationSummary("Technical service gateway timeout error.")
                .confidenceScore(0.96)
                .build());

        // Seed an active pending approval for R2
        approvalService.createApprovalRequest(
                r2.getId(),
                "REPLAY_PAYMENT_EVENT",
                "AI Investigation recommended event replay for failed flight reservation sync. Bank transaction TXN-88421 confirmed.",
                "{\"recommendedAction\":\"REPLAY_PAYMENT_EVENT\",\"requestId\":" + r2.getId() + "}"
        );
    }
}
