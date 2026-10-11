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
        logger.info("Seeding operational runbooks for the knowledge base...");

        runbook("Payment Synchronization Troubleshooting Runbook", "PAYMENT", "payment, sync, kafka, lag, replay",
                "When a customer reports that money was taken but the order still shows as pending or unpaid: 1. Look up the transaction at the payment gateway by its transaction id and confirm it was authorized. 2. Check whether the payment event reached the order service, and whether its Kafka consumer is lagging. 3. If the gateway authorized the payment but the order never received the event, the fix is REPLAY_PAYMENT_EVENT, which sends the event again. 4. Replaying needs a manager's approval.");
        runbook("Payment Gateway Webhook Not Received", "PAYMENT", "payment, webhook, gateway, callback, wallet, top-up",
                "When a wallet top-up, subscription renewal or card payment is confirmed by the bank but the balance or plan in our system did not change: the gateway's confirmation callback (webhook) was probably never delivered or was rejected. 1. Find the transaction in the gateway dashboard and check its webhook delivery log. 2. Check our webhook endpoint logs for a signature failure or a timeout. 3. Once the cause is fixed, use REPLAY_PAYMENT_EVENT to apply the confirmed payment. 4. Needs a manager's approval. Never ask the customer to pay again.");
        runbook("Booking Failure & Inventory Recovery Guide", "BOOKING", "booking, flight, seat, inventory, refund, checkout",
                "When a flight or ticket booking fails at checkout but the customer was charged: this usually happens when the seat lock times out while the payment is still in progress. 1. Check the state of the seat or ticket in the booking database. 2. A seat that is held by a failed booking must be released so others can buy it. 3. Refund the customer in full. The action for both steps is RELEASE_INVENTORY_AND_REFUND, and it needs a manager's approval.");
        runbook("Hotel Overbooking and Double Allocation Guide", "BOOKING", "hotel, room, overbooking, double booking, reservation",
                "When two guests hold a confirmed reservation for the same room, or a guest arrives and the property has no record of the reservation: the room was allocated twice because two reservations were confirmed at the same moment. 1. Confirm both reservations and their timestamps. 2. Keep the earlier one. 3. For the later guest, free the allocation and refund the stay using RELEASE_INVENTORY_AND_REFUND, then offer an alternative property. Needs a manager's approval.");
        runbook("Parcel Marked Delivered but Not Received", "DELIVERY", "delivery, courier, parcel, delivered, not received, proof of delivery",
                "When tracking shows a parcel as delivered but the recipient says nothing arrived: 1. Get the proof of delivery from the courier: the photo, the signature and the GPS position of the scan. 2. Compare the GPS position with the delivery address. 3. Ask the recipient to check with neighbours and the building's reception. 4. If the parcel is still not found, the action is OPEN_COURIER_INVESTIGATION, which opens a formal case with the courier company. Needs a manager's approval.");
        runbook("Parcel Stuck in Transit or Misrouted", "DELIVERY", "shipment, transit, stuck, misrouted, wrong city, tracking",
                "When a shipment has shown no tracking update for several days, or tracking shows it travelling to the wrong city or sorting centre: 1. Check the last scan and which depot holds the parcel. 2. A parcel with no scan for more than 72 hours is treated as lost in the courier's network. 3. Use OPEN_COURIER_INVESTIGATION so the courier traces it and reroutes or replaces it. Needs a manager's approval. Tell the customer the expected time for the trace.");
        runbook("API Gateway Timeouts and 5xx Errors", "TECHNICAL", "api, gateway, timeout, 504, 502, 500, integration, partner",
                "When a partner or customer integration receives HTTP 504 Gateway Timeout, 502 or 500 from our API: this is a fault on our side, not the customer's. 1. Check the gateway dashboard for the error rate and which upstream service is slow. 2. Check recent deployments. 3. Do not ask the customer to change their integration. The action is ESCALATE_TO_ON_CALL so the on-call engineer investigates immediately. Needs a manager's approval because it pages a person.");
        runbook("Kafka Consumer Lag & Outbox Event Recovery Guide", "TECHNICAL", "kafka, outbox, consumer, lag, dead letter, events, notifications delayed",
                "When notifications, status updates or other background processing arrive late or not at all for many customers at once: events are queuing. 1. Check the outbox table for a growing number of pending events. 2. Check the consumer group's lag and the dead-letter topic for failed events. 3. This needs an engineer: the action is ESCALATE_TO_ON_CALL. After the cause is fixed, the waiting events are delivered automatically.");
        runbook("Database Connection Pool Exhaustion", "TECHNICAL", "database, connections, slow, pool, outage, errors",
                "When every page of the application is very slow or fails with errors for all users at the same time, and the logs show that no database connection could be obtained: the connection pool is exhausted, usually by a slow query or a traffic spike. 1. Check the number of active connections and the longest-running queries. 2. This is a platform incident, not a problem with one customer's request. The action is ESCALATE_TO_ON_CALL. Needs a manager's approval because it pages a person.");
        runbook("Sign-in Failures and Locked Accounts", "ACCOUNT", "login, sign in, password, locked, account, access, verification code",
                "When a customer cannot sign in, says their password is rejected, did not receive a verification code, or their account is locked: we cannot act until we know which account it is and what they see. 1. Ask for the email address on the account and the exact error message. 2. Ask when it last worked and whether the device or phone number changed. The action is REQUEST_MORE_INFORMATION. Never reset a password or unlock an account from a message alone.");
    }

    /** One runbook, stored as one chunk: each is a single short procedure. */
    private void runbook(String title, String category, String tags, String content) {
        KnowledgeDocument document = documentRepository.save(KnowledgeDocument.builder()
                .title(title).category(category).content(content).tags(tags)
                .build());
        chunkRepository.save(KnowledgeChunk.builder()
                .documentId(document.getId()).chunkIndex(0).text(content)
                .build());
    }

    private void seedRequests() {
        logger.info("Seeding sample operational requests...");

        requestRepository.save(Request.builder()
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

        requestRepository.save(Request.builder()
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

        requestRepository.save(Request.builder()
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
