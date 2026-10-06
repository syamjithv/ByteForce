package com.byteforce.repository;

import com.byteforce.domain.Concept;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.ResourceType;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository implementing {@link LearnRepository} seeded with a deterministic
 * placement curriculum covering core CS subjects, topics, concepts, and resources.
 */
public class InMemoryLearnRepository implements LearnRepository {

    private final Map<String, Subject> subjectsById = new LinkedHashMap<>();
    private final Map<Long, Topic> topicsById = new LinkedHashMap<>();
    private final Map<String, List<Long>> subjectToTopicIds = new LinkedHashMap<>();
    private final Map<Long, Concept> conceptsById = new LinkedHashMap<>();
    private final Map<Long, List<Long>> topicToConceptIds = new LinkedHashMap<>();

    public InMemoryLearnRepository() {
        seedSampleDataset();
    }

    private void seedSampleDataset() {
        Instant now = Instant.now();

        // -------------------------------------------------------------
        // 1. DATA STRUCTURES
        // -------------------------------------------------------------
        Topic arraysTopic = new Topic(101L, "Arrays", "arrays", "Contiguous memory allocations and indexing", 1, now);
        Topic linkedListsTopic = new Topic(102L, "Linked Lists", "linked-lists", "Node-based dynamic pointer structures", 2, now);

        Subject dataStructures = new Subject(
                "data-structures",
                "Data Structures",
                "Fundamental memory representations, algorithmic complexity, and dynamic collections.",
                1,
                List.of(arraysTopic, linkedListsTopic)
        );

        // Concept: Array Traversal
        Concept arrayTraversal = Concept.create(
                1001L,
                arraysTopic.getId(),
                arraysTopic.getName(),
                "Array Traversal",
                "Iterating through sequential memory blocks using index offsets.",
                List.of(
                        "Direct O(1) random access by array index formula: base_address + (index * element_size).",
                        "Contiguous memory layout provides superior hardware CPU cache-line locality.",
                        "Standard forward and reverse linear traversal runs in O(n) time complexity.",
                        "Boundary checks (0 to length - 1) prevent index out-of-bounds exceptions."
                ),
                "// Linear forward traversal\nint[] arr = {10, 20, 30, 40, 50};\nfor (int i = 0; i < arr.length; i++) {\n    System.out.println(\"Index \" + i + \": \" + arr[i]);\n}",
                List.of(
                        LearningResource.create("Array Data Structure Guide", ResourceType.ARTICLE, "https://www.geeksforgeeks.org/array-data-structure-guide/", "Comprehensive guide on arrays and spatial locality."),
                        LearningResource.create("Arrays & Memory Representation", ResourceType.VIDEO, "https://www.youtube.com/watch?v=array-intro", "Video breakdown of array storage in memory.")
                )
        );

        // Concept: Prefix Sum
        Concept prefixSum = Concept.create(
                1002L,
                arraysTopic.getId(),
                arraysTopic.getName(),
                "Prefix Sum",
                "Precomputing cumulative sums to answer range sum queries in constant time.",
                List.of(
                        "Prefix array definition: prefix[i] = prefix[i-1] + arr[i] with prefix[0] = arr[0].",
                        "Reduces range sum query sum(L, R) from O(n) to O(1) time: prefix[R] - prefix[L-1].",
                        "Requires O(n) initial precomputation time and O(n) auxiliary space.",
                        "Essential pattern in placement problems involving subarrays and 2D matrix sum queries."
                ),
                "// Prefix sum array construction & query\nint[] arr = {2, 4, 6, 8, 10};\nint[] prefix = new int[arr.length];\nprefix[0] = arr[0];\nfor (int i = 1; i < arr.length; i++) {\n    prefix[i] = prefix[i - 1] + arr[i];\n}\n// Query sum between index 1 and 3 (4 + 6 + 8 = 18)\nint sum = prefix[3] - prefix[0]; // Output: 18",
                List.of(
                        LearningResource.create("Prefix Sum Technique & Use Cases", ResourceType.ARTICLE, "https://en.wikipedia.org/wiki/Prefix_sum", "Mathematical foundation and algorithmic applications."),
                        LearningResource.create("Subarray Sums Cheatsheet", ResourceType.PDF, "https://byteforce.dev/resources/prefix-sum.pdf", "Interview preparation reference sheet for prefix algorithms.")
                )
        );

        // Concept: Singly Linked List
        Concept singlyLinkedList = Concept.create(
                1003L,
                linkedListsTopic.getId(),
                linkedListsTopic.getName(),
                "Singly Linked List",
                "Linear collection of data nodes linked together sequentially using single next pointers.",
                List.of(
                        "Dynamic memory allocation eliminates fixed capacity limitations of static arrays.",
                        "Efficient O(1) time insertions and deletions at the head of the list.",
                        "No direct index random access; searching requires O(n) sequential pointer traversal.",
                        "Each node incurs pointer storage overhead (typically 8 bytes on 64-bit JVMs)."
                ),
                "class Node {\n    int data;\n    Node next;\n    Node(int data) {\n        this.data = data;\n        this.next = null;\n    }\n}\n// Insert at head\nNode head = new Node(10);\nNode newNode = new Node(5);\nnewNode.next = head;\nhead = newNode;",
                List.of(
                        LearningResource.create("Singly Linked List Implementation Guide", ResourceType.ARTICLE, "https://en.wikipedia.org/wiki/Linked_list", "Full explanation of node insertion, deletion, and reversal."),
                        LearningResource.create("Linked List Visualizer", ResourceType.VIDEO, "https://www.youtube.com/watch?v=linkedlist-vis", "Step-by-step visual pointer animations.")
                )
        );

        // Concept: Doubly Linked List
        Concept doublyLinkedList = Concept.create(
                1004L,
                linkedListsTopic.getId(),
                linkedListsTopic.getName(),
                "Doubly Linked List",
                "Bidirectional linked sequence with references to both succeeding and preceding nodes.",
                List.of(
                        "Enables bidirectional traversal (both forward from head and backward from tail).",
                        "Allows O(1) node deletion when a pointer to the target node is already available.",
                        "Common foundation for LRU Cache implementations and Java's LinkedList / Deque collections.",
                        "Increased memory overhead: two reference pointers per node."
                ),
                "class DNode {\n    int val;\n    DNode prev, next;\n    DNode(int val) { this.val = val; }\n}\n// Remove a known node in O(1)\nvoid removeNode(DNode node) {\n    if (node.prev != null) node.prev.next = node.next;\n    if (node.next != null) node.next.prev = node.prev;\n}",
                List.of(
                        LearningResource.create("Doubly Linked List Mechanics", ResourceType.ARTICLE, "https://en.wikipedia.org/wiki/Doubly_linked_list", "Detailed structural guide and edge case analysis.")
                )
        );

        // -------------------------------------------------------------
        // 2. DBMS (Database Management Systems)
        // -------------------------------------------------------------
        Topic sqlTopic = new Topic(201L, "SQL", "sql", "Structured query language for relational schemas", 1, now);

        Subject dbms = new Subject(
                "dbms",
                "Database Management Systems",
                "Relational schema architecture, normalization, SQL querying, and transaction ACID properties.",
                2,
                List.of(sqlTopic)
        );

        // Concept: SELECT
        Concept selectConcept = Concept.create(
                2001L,
                sqlTopic.getId(),
                sqlTopic.getName(),
                "SELECT Statement",
                "The core declarative clause used to retrieve and project attributes from database tables.",
                List.of(
                        "Column projection filters returned attributes to minimize network payload.",
                        "WHERE clause applies boolean predicates to filter tuples prior to grouping.",
                        "ORDER BY sorts the returned records ascending (ASC) or descending (DESC).",
                        "LIMIT / OFFSET manages pagination for performant desktop and web data loading."
                ),
                "-- Retrieve active graduating students\nSELECT id, full_name, email, graduation_year\nFROM student_profiles\nWHERE graduation_year = 2026\nORDER BY full_name ASC\nLIMIT 20;",
                List.of(
                        LearningResource.create("W3Schools SQL SELECT Reference", ResourceType.ARTICLE, "https://www.w3schools.com/sql/sql_select.asp", "Core syntax and practical query examples."),
                        LearningResource.create("SQL Query Execution Order", ResourceType.PDF, "https://byteforce.dev/resources/sql-execution-order.pdf", "Cheatsheet showing FROM -> WHERE -> GROUP BY -> SELECT order.")
                )
        );

        // Concept: JOIN
        Concept joinConcept = Concept.create(
                2002L,
                sqlTopic.getId(),
                sqlTopic.getName(),
                "JOIN Operations",
                "Relational algebra operation combining records from multiple tables based on matching foreign key keys.",
                List.of(
                        "INNER JOIN returns only tuples having matching values in both tables.",
                        "LEFT OUTER JOIN returns all rows from the left table and matched rows from the right table.",
                        "RIGHT OUTER JOIN preserves all records from the right table.",
                        "FULL OUTER JOIN combines matching and unmatched rows from both participating tables."
                ),
                "-- Join question attempts with student details\nSELECT u.email, sp.full_name, a.status, a.attempted_at\nFROM question_attempts a\nINNER JOIN users u ON a.user_id = u.id\nLEFT JOIN student_profiles sp ON u.id = sp.user_id\nWHERE a.status = 'SOLVED';",
                List.of(
                        LearningResource.create("Visual Guide to SQL Joins", ResourceType.ARTICLE, "https://en.wikipedia.org/wiki/Join_(SQL)", "Venn diagrams illustrating table join behaviors."),
                        LearningResource.create("SQL Joins Explained Visually", ResourceType.VIDEO, "https://www.youtube.com/watch?v=sql-joins", "10-minute video explaining JOIN mechanics.")
                )
        );

        // Concept: GROUP BY
        Concept groupByConcept = Concept.create(
                2003L,
                sqlTopic.getId(),
                sqlTopic.getName(),
                "GROUP BY Clause",
                "Aggregates rows that have matching values in specified columns into concise summary rows.",
                List.of(
                        "Collaborates with aggregate functions: COUNT(), SUM(), AVG(), MIN(), and MAX().",
                        "HAVING clause filters aggregated group results after grouping occurs.",
                        "WHERE filters individual rows before grouping, while HAVING filters post-aggregation groups.",
                        "All non-aggregated columns in SELECT must appear in the GROUP BY clause."
                ),
                "-- Count questions per topic having more than 3 questions\nSELECT topic_id, COUNT(*) AS question_count, MAX(created_at) AS latest_addition\nFROM questions\nGROUP BY topic_id\nHAVING COUNT(*) >= 3\nORDER BY question_count DESC;",
                List.of(
                        LearningResource.create("SQL GROUP BY & Aggregation Guide", ResourceType.ARTICLE, "https://www.w3schools.com/sql/sql_groupby.asp", "Aggregation functions and HAVING filters.")
                )
        );

        // -------------------------------------------------------------
        // 3. OPERATING SYSTEMS
        // -------------------------------------------------------------
        Topic processesTopic = new Topic(301L, "Processes", "processes", "Process models, scheduling, and lifecycle states", 1, now);

        Subject operatingSystems = new Subject(
                "operating-systems",
                "Operating Systems",
                "Process lifecycle, CPU scheduling, virtual memory, threads, synchronization, and deadlocks.",
                3,
                List.of(processesTopic)
        );

        // Concept: Process States
        Concept processStates = Concept.create(
                3001L,
                processesTopic.getId(),
                processesTopic.getName(),
                "Process States",
                "The distinct execution phases of an active program as managed by the OS process scheduler.",
                List.of(
                        "NEW: The program is being created and loaded into memory.",
                        "READY: The process is in memory waiting to be assigned to a CPU core.",
                        "RUNNING: Instructions are actively being executed on the CPU core.",
                        "WAITING / BLOCKED: The process cannot proceed until an I/O event or signal completes.",
                        "TERMINATED: The process has finished execution and OS reclaims its resources."
                ),
                "// Process Lifecycle State Diagram:\n// [NEW] -> [READY] <---> [RUNNING] -> [TERMINATED]\n//            ^             |\n//            |-- [WAITING] <-",
                List.of(
                        LearningResource.create("Process Lifecycle & PCB", ResourceType.ARTICLE, "https://en.wikipedia.org/wiki/Process_state", "Detailed breakdown of the 5-state process model."),
                        LearningResource.create("Operating Systems Process Notes", ResourceType.PDF, "https://byteforce.dev/resources/os-processes.pdf", "Summary notes for placement interviews.")
                )
        );

        // Concept: Context Switching
        Concept contextSwitching = Concept.create(
                3002L,
                processesTopic.getId(),
                processesTopic.getName(),
                "Context Switching",
                "Saving the context of a preempted process and restoring the context of the next scheduled process.",
                List.of(
                        "Saves CPU registers, Program Counter (PC), and stack pointer into the Process Control Block (PCB).",
                        "Restores the saved state from the next process's PCB before resuming execution.",
                        "Pure computational overhead: the CPU performs zero user application work during a context switch.",
                        "Hardware interrupts, system calls, and timer ticks trigger scheduler context switches."
                ),
                "// Pseudocode of Context Switch flow:\nvoid contextSwitch(PCB* currentProcess, PCB* nextProcess) {\n    saveRegisters(currentProcess->registers);\n    currentProcess->state = READY;\n    \n    loadRegisters(nextProcess->registers);\n    nextProcess->state = RUNNING;\n    jumpTo(nextProcess->programCounter);\n}",
                List.of(
                        LearningResource.create("Context Switch Mechanisms", ResourceType.ARTICLE, "https://en.wikipedia.org/wiki/Context_switch", "Architectural explanation of kernel context switches."),
                        LearningResource.create("How Context Switching Works", ResourceType.VIDEO, "https://www.youtube.com/watch?v=context-switch", "Animated video showing CPU registers and memory swapping.")
                )
        );

        // Register Subjects
        registerSubject(dataStructures);
        registerSubject(dbms);
        registerSubject(operatingSystems);

        // Register Topics
        registerTopic(arraysTopic, dataStructures.getId());
        registerTopic(linkedListsTopic, dataStructures.getId());
        registerTopic(sqlTopic, dbms.getId());
        registerTopic(processesTopic, operatingSystems.getId());

        // Register Concepts
        registerConcept(arrayTraversal);
        registerConcept(prefixSum);
        registerConcept(singlyLinkedList);
        registerConcept(doublyLinkedList);
        registerConcept(selectConcept);
        registerConcept(joinConcept);
        registerConcept(groupByConcept);
        registerConcept(processStates);
        registerConcept(contextSwitching);
    }

    private void registerSubject(Subject subject) {
        subjectsById.put(subject.getId(), subject);
    }

    private void registerTopic(Topic topic, String subjectId) {
        topicsById.put(topic.getId(), topic);
        subjectToTopicIds.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(topic.getId());
    }

    private void registerConcept(Concept concept) {
        conceptsById.put(concept.getId(), concept);
        topicToConceptIds.computeIfAbsent(concept.getTopicId(), k -> new ArrayList<>()).add(concept.getId());
    }

    @Override
    public List<Subject> findAllSubjects() {
        return subjectsById.values().stream()
                .sorted(Comparator.comparingInt(Subject::getDisplayOrder))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Subject> findSubjectById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(subjectsById.get(id.trim().toLowerCase()));
    }

    @Override
    public List<Topic> findTopicsBySubjectId(String subjectId) {
        if (subjectId == null) return List.of();
        List<Long> topicIds = subjectToTopicIds.get(subjectId.trim().toLowerCase());
        if (topicIds == null || topicIds.isEmpty()) {
            return List.of();
        }
        return topicIds.stream()
                .map(topicsById::get)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(Topic::getDisplayOrder))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Topic> findTopicById(long topicId) {
        return Optional.ofNullable(topicsById.get(topicId));
    }

    @Override
    public List<Concept> findConceptsByTopicId(long topicId) {
        List<Long> conceptIds = topicToConceptIds.get(topicId);
        if (conceptIds == null || conceptIds.isEmpty()) {
            return List.of();
        }
        return conceptIds.stream()
                .map(conceptsById::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Concept> findConceptById(long conceptId) {
        return Optional.ofNullable(conceptsById.get(conceptId));
    }

    @Override
    public List<LearningResource> findResourcesByConceptId(long conceptId) {
        Concept concept = conceptsById.get(conceptId);
        return concept != null ? concept.getResources() : List.of();
    }

    @Override
    public List<Concept> searchConcepts(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(conceptsById.values());
        }
        String term = query.trim().toLowerCase(Locale.ROOT);
        return conceptsById.values().stream()
                .filter(c -> c.getTitle().toLowerCase(Locale.ROOT).contains(term) ||
                        c.getShortExplanation().toLowerCase(Locale.ROOT).contains(term) ||
                        c.getTopicName().toLowerCase(Locale.ROOT).contains(term) ||
                        c.getKeyPoints().stream().anyMatch(kp -> kp.toLowerCase(Locale.ROOT).contains(term)))
                .collect(Collectors.toList());
    }
}
