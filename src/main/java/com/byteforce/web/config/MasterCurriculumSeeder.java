package com.byteforce.web.config;

import com.byteforce.domain.Concept;
import com.byteforce.domain.ConceptRelationship;
import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.RememberItem;
import com.byteforce.domain.RememberItemType;
import com.byteforce.domain.ResourceType;
import com.byteforce.domain.Topic;
import com.byteforce.repository.ConceptRelationshipRepository;
import com.byteforce.repository.ConceptRepository;
import com.byteforce.repository.RememberItemRepository;
import com.byteforce.service.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeder for the ByteForce Master CS Curriculum.
 * Grounded in ACM/IEEE CS2023, MIT OpenCourseWare, and official standards (IETF RFCs, Oracle, OWASP, W3C).
 * Preserves all 11 legacy concept IDs (1001-1004, 2001-2003, 3001-3004) and their relationships.
 */
@Component
public class MasterCurriculumSeeder {

    private static final Logger log = LoggerFactory.getLogger(MasterCurriculumSeeder.class);

    public void seedAll(TopicService topicService,
                        ConceptRepository conceptRepository,
                        RememberItemRepository rememberItemRepository,
                        ConceptRelationshipRepository conceptRelationshipRepository) {
        log.info("Seeding ByteForce Master CS Curriculum across all 18 subjects...");

        seedProgrammingFoundations(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedDataStructuresAndAlgorithms(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedJavaAndOop(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedDbmsAndSql(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedOperatingSystems(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedComputerNetworks(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedComputerOrganization(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedTheoryOfComputation(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedSoftwareEngineering(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedSystemDesign(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedCybersecurity(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedDistributedSystems(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedCompilersAndLanguages(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedWebAndApiFundamentals(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedAiMlFundamentals(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedMathematicsForCs(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedHciAndAccessibility(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);
        seedEthicsAndPrivacy(topicService, conceptRepository, rememberItemRepository, conceptRelationshipRepository);

        log.info("ByteForce Master CS Curriculum seeding completed.");
    }

    // --------------------------------------------------------------------------
    // 1. Programming Foundations
    // --------------------------------------------------------------------------
    private void seedProgrammingFoundations(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic asympTopic = findOrCreateTopic(ts, "programming-foundations", "Asymptotic Analysis & Complexity", "prog-asymptotics",
                "Formal asymptotic notations (Big-O, Omega, Theta), time complexity analysis, and space complexity.", 1);
        Topic funcTopic = findOrCreateTopic(ts, "programming-foundations", "Functions, Scope & Recursion", "prog-functions-recursion",
                "Activation records, call stacks, scope lifetimes, recursive decompositions, and stack overflow.", 2);

        // Concept 4001: Time Complexity & Big-O Notation
        saveConceptIfAbsent(cr, Concept.create(
                4001L,
                asympTopic.getId(),
                asympTopic.getName(),
                "Time Complexity & Big-O Notation",
                "A mathematical abstraction describing how an algorithm's execution time scales as input size approaches infinity.\n\n"
                        + "### Why This Matters\n"
                        + "Wall-clock execution time varies wildly across different CPUs, background processes, and programming language runtimes. Big-O notation eliminates hardware variance by counting fundamental computational operations relative to input size N, providing an objective comparison framework for technical placement interviews.\n\n"
                        + "### The Idea\n"
                        + "Big-O characterizes the upper bound of growth rate: f(N) is O(g(N)) if there exist positive constants c and n0 such that 0 <= f(N) <= c * g(N) for all N >= n0. When evaluating code, drop multiplicative constants and lower-order terms because as N grows large (e.g. 10^6), the highest-degree polynomial or exponential term completely dominates total runtime.\n\n"
                        + "### Real Example\n"
                        + "Searching for a student in an unsorted list of size N takes O(N) linear time (inspecting each entry). Searching the same list when sorted using binary search takes O(log N) time — dividing the search space in half at each step (only ~20 comparisons for 1,000,000 items).",
                List.of(
                        "Big-O establishes a theoretical asymptotic upper bound on runtime growth.",
                        "Constant factors (e.g., 2N vs 100N) and lower-order terms (N^2 + N) are omitted in asymptotic classification.",
                        "Hierarchy of standard complexities: O(1) < O(log N) < O(N) < O(N log N) < O(N^2) < O(2^N) < O(N!).",
                        "In placement coding interviews, aim for O(N log N) or O(N) solutions whenever N >= 10^5 to prevent Time Limit Exceeded (TLE)."
                ),
                "// O(log N) Binary Search example in Java 21\npublic static int binarySearch(int[] arr, int target) {\n    int low = 0, high = arr.length - 1;\n    while (low <= high) {\n        int mid = low + (high - low) / 2; // Prevents 32-bit integer overflow\n        if (arr[mid] == target) return mid;\n        if (arr[mid] < target) low = mid + 1;\n        else high = mid - 1;\n    }\n    return -1; // Target not found\n}",
                List.of(
                        LearningResource.create("MIT 6.006 Lecture 1: Algorithmic Thinking & Peak Finding", ResourceType.ARTICLE, "https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/", "MIT OpenCourseWare official algorithms curriculum."),
                        LearningResource.create("ACM/IEEE CS2023 Curricula: AL-Complexity", ResourceType.ARTICLE, "https://cs2023.org/", "Formal academic guidelines on complexity theory.")
                )
        ));

        seedRemember(rr, cr, 4001L, RememberItemType.KEY_FACT, "Big-O represents asymptotic upper bound; Big-Omega represents lower bound; Big-Theta represents asymptotically tight bound.", 1);
        seedRemember(rr, cr, 4001L, RememberItemType.COMMON_CONFUSION, "O(1) means constant time independent of input size N, not necessarily 'instantaneous' (a loop running exactly 10,000 times is still O(1)).", 2);
        seedRemember(rr, cr, 4001L, RememberItemType.INTERVIEW_REMINDER, "Always state both Time and Space complexity upfront before writing code during technical interviews.", 3);

        // Concept 4002: Recursion & Call Stack
        saveConceptIfAbsent(cr, Concept.create(
                4002L,
                funcTopic.getId(),
                funcTopic.getName(),
                "Recursion & Call Stack",
                "A programming technique where a method solves a problem by calling itself on smaller sub-instances until reaching a terminal base case.\n\n"
                        + "### Why This Matters\n"
                        + "Recursion is the foundational paradigm behind Divide & Conquer, Tree Traversals, Graph Depth-First Search, and Dynamic Programming. Understanding the call stack is critical to prevent StackOverflowError in recursive systems.\n\n"
                        + "### The Idea\n"
                        + "Every method invocation pushes a new stack frame (activation record) onto the thread's execution call stack. Each frame stores parameters, local variables, and the return address. Without a correctly reachable base case, recursive calls consume all allocated thread stack memory, terminating with a StackOverflowError.\n\n"
                        + "### Real Example\n"
                        + "Browsing a file system directory containing sub-directories: the file crawler visits each sub-folder recursively until reaching empty folders (the base case), then unwinds back to parent folders.",
                List.of(
                        "Every recursive function requires at least one terminating base case and an inductive progress step.",
                        "Each recursive call allocates a stack frame in memory; recursion depth D consumes O(D) auxiliary stack space.",
                        "Tail recursion occurs when the recursive call is the final statement in the function.",
                        "Tree and graph traversals naturally translate to recursive depth-first formulations."
                ),
                "// Recursive factorial with base case\npublic static long factorial(int n) {\n    if (n <= 1) return 1; // Base case: stops recursion\n    return n * factorial(n - 1); // Inductive step: consumes stack frame\n}",
                List.of(
                        LearningResource.create("MIT 6.0001: Recursion & Dictionaries", ResourceType.ARTICLE, "https://ocw.mit.edu/courses/6-0001-introduction-to-computer-science-and-programming-in-python-fall-2016/", "MIT OCW introduction to recursive call stacks.")
                )
        ));

        seedRemember(rr, cr, 4002L, RememberItemType.KEY_FACT, "Recursion depth determines auxiliary space complexity on the call stack (e.g. tree depth O(H)).", 1);
        seedRemember(rr, cr, 4002L, RememberItemType.COMMON_CONFUSION, "Recursion is not inherently slower than iteration, but does incur stack frame allocation overhead.", 2);
        seedRel(crel, cr, 4001L, 4002L, ConceptRelationshipType.RELATED, "Analyzing the time complexity of recursive algorithms via recurrence relations", 1);
    }

    // --------------------------------------------------------------------------
    // 2. Data Structures & Algorithms (Preserves 1001-1004)
    // --------------------------------------------------------------------------
    private void seedDataStructuresAndAlgorithms(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic arraysTopic = findOrCreateTopic(ts, "data-structures", "Arrays & Two Pointers", "arrays-two-pointers",
                "Contiguous memory layouts, sliding windows, prefix sums, and two-pointer patterns.", 1);
        Topic linkedListsTopic = findOrCreateTopic(ts, "data-structures", "Linked Lists", "linked-lists",
                "Node-based dynamic pointer structures, list reversals, and cycle detection.", 2);
        Topic stacksQueuesTopic = findOrCreateTopic(ts, "data-structures", "Stacks & Queues", "stacks-queues",
                "LIFO and FIFO linear collections, monotonic stacks, and circular buffers.", 3);
        Topic treesTopic = findOrCreateTopic(ts, "data-structures", "Trees & Binary Search Trees", "trees-bst",
                "Hierarchical tree structures, BST invariants, traversals, and balanced tree guarantees.", 4);

        // 1001: Array Traversal (Preserved with enriched sections)
        saveOrUpdateConcept(cr, Concept.create(
                1001L,
                arraysTopic.getId(),
                arraysTopic.getName(),
                "Array Traversal",
                "Iterating through sequential memory blocks using index offsets.\n\n"
                        + "### Why This Matters\n"
                        + "Contiguous sequential traversal is fundamental to linear scanning, searching, and filtering. Because array elements reside in adjacent physical memory addresses, CPU hardware prefetchers load entire cache lines into L1/L2 cache before your loop even requests the next element, providing peak cache locality.\n\n"
                        + "### The Idea\n"
                        + "An array stores elements of identical size in contiguous memory blocks. Every element's memory address is mathematically computed in constant time O(1) using the formula: Address(i) = BaseAddress + (i * ElementSize). When iterating through the array, a pointer or index increments by 1 step at each iteration.\n\n"
                        + "### Real Example\n"
                        + "A digital music player playlist where songs are stored in continuous order: the player scans from song index 0 to index 49 sequentially, buffering each track into memory.",
                List.of(
                        "Direct O(1) random access by array index formula: base_address + (index * element_size).",
                        "Contiguous memory layout provides superior hardware CPU cache-line locality.",
                        "Standard forward and reverse linear traversal runs in O(n) time complexity.",
                        "Boundary checks (0 to length - 1) prevent index out-of-bounds exceptions."
                ),
                "// Linear forward traversal in Java 21\nint[] arr = {10, 20, 30, 40, 50};\nfor (int i = 0; i < arr.length; i++) {\n    System.out.println(\"Index \" + i + \": \" + arr[i]);\n}",
                List.of(
                        LearningResource.create("MIT 6.006: Data Structures & Dynamic Arrays", ResourceType.ARTICLE, "https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/", "MIT OCW sequence data structures and spatial locality analysis."),
                        LearningResource.create("Java Language Specification SE 21: Arrays", ResourceType.ARTICLE, "https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html", "Official Java memory semantics for arrays.")
                )
        ));

        // 1002: Prefix Sum (Preserved with enriched sections)
        saveOrUpdateConcept(cr, Concept.create(
                1002L,
                arraysTopic.getId(),
                arraysTopic.getName(),
                "Prefix Sum",
                "Precomputing cumulative sums to answer range sum queries in constant time.\n\n"
                        + "### Why This Matters\n"
                        + "In placement problems where thousands of range sum queries are executed on static or infrequently modified arrays, calculating sums naively takes O(N) per query, leading to O(Q * N) overall time. Prefix Sum reduces each query to O(1) time.\n\n"
                        + "### The Idea\n"
                        + "Construct an auxiliary array `prefix` where `prefix[i]` stores the sum of all elements from index 0 to i. The sum of elements between indices L and R is simply `prefix[R] - prefix[L - 1]` (or `prefix[R]` when L == 0).\n\n"
                        + "### Real Example\n"
                        + "A vehicle odometer: to calculate the distance traveled between toll booth A (mile 120) and toll booth B (mile 350), you subtract 120 from 350 to get 230 miles without measuring every road segment in between.",
                List.of(
                        "Prefix array definition: prefix[i] = prefix[i-1] + arr[i] with prefix[0] = arr[0].",
                        "Reduces range sum query sum(L, R) from O(n) to O(1) time: prefix[R] - prefix[L-1].",
                        "Requires O(n) initial precomputation time and O(n) auxiliary space.",
                        "Essential pattern in placement problems involving subarrays and 2D matrix sum queries."
                ),
                "// Prefix sum array construction & query in Java 21\nint[] arr = {2, 4, 6, 8, 10};\nint[] prefix = new int[arr.length];\nprefix[0] = arr[0];\nfor (int i = 1; i < arr.length; i++) {\n    prefix[i] = prefix[i - 1] + arr[i];\n}\n// Query sum between index 1 and 3 (4 + 6 + 8 = 18)\nint sum = prefix[3] - prefix[0]; // Output: 18",
                List.of(
                        LearningResource.create("MIT 6.006: Dynamic Programming & Prefix Algorithms", ResourceType.ARTICLE, "https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/", "MIT OpenCourseWare prefix computations and range queries.")
                )
        ));

        // 1003: Singly Linked List (Preserved)
        saveOrUpdateConcept(cr, Concept.create(
                1003L,
                linkedListsTopic.getId(),
                linkedListsTopic.getName(),
                "Singly Linked List",
                "Linear collection of data nodes linked together sequentially using single next pointers.\n\n"
                        + "### Why This Matters\n"
                        + "Unlike static arrays, linked lists can grow or shrink dynamically in memory without reallocating contiguous blocks. Node insertion and deletion at the list head are true O(1) constant-time operations.\n\n"
                        + "### The Idea\n"
                        + "Each node contains two fields: the data payload and a reference pointer (`next`) to the succeeding node. The list terminates when a node's `next` pointer points to `null`.\n\n"
                        + "### Real Example\n"
                        + "A train with connected railroad cars: each car is hitched only to the car behind it. You cannot jump directly to car #10 without walking through cars 1 through 9.",
                List.of(
                        "Dynamic memory allocation eliminates fixed capacity limitations of static arrays.",
                        "Efficient O(1) time insertions and deletions at the head of the list.",
                        "No direct index random access; searching requires O(n) sequential pointer traversal.",
                        "Each node incurs pointer storage overhead (typically 8 bytes on 64-bit JVMs)."
                ),
                "class Node {\n    int data;\n    Node next;\n    Node(int data) {\n        this.data = data;\n        this.next = null;\n    }\n}\n// Insert at head in O(1)\nNode head = new Node(10);\nNode newNode = new Node(5);\nnewNode.next = head;\nhead = newNode;",
                List.of(
                        LearningResource.create("MIT 6.006: Linked Data Structures", ResourceType.ARTICLE, "https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/", "MIT OpenCourseWare pointer manipulation lectures.")
                )
        ));

        // 1004: Doubly Linked List (Preserved)
        saveOrUpdateConcept(cr, Concept.create(
                1004L,
                linkedListsTopic.getId(),
                linkedListsTopic.getName(),
                "Doubly Linked List",
                "Bidirectional linked sequence with references to both succeeding and preceding nodes.\n\n"
                        + "### Why This Matters\n"
                        + "Doubly linked lists enable bidirectional traversal (forward and backward) and allow O(1) deletion of any node when a reference pointer to that node is already held, making them the standard choice for LRU (Least Recently Used) caches.\n\n"
                        + "### The Idea\n"
                        + "Each node contains three fields: payload data, a `next` pointer, and a `prev` pointer. Sentinel dummy head and tail nodes are commonly utilized to eliminate edge-case checks during node insertions and deletions.\n\n"
                        + "### Real Example\n"
                        + "Web browser navigation history: you can move forward to the next visited page or back to the previous page seamlessly because each history entry references both adjacent pages.",
                List.of(
                        "Enables bidirectional traversal (both forward from head and backward from tail).",
                        "Allows O(1) node deletion when a pointer to the target node is already available.",
                        "Common foundation for LRU Cache implementations and Java's LinkedList / Deque collections.",
                        "Increased memory overhead: two reference pointers per node."
                ),
                "class DNode {\n    int val;\n    DNode prev, next;\n    DNode(int val) { this.val = val; }\n}\n// Remove a known node in O(1)\nvoid removeNode(DNode node) {\n    if (node.prev != null) node.prev.next = node.next;\n    if (node.next != null) node.next.prev = node.prev;\n}",
                List.of(
                        LearningResource.create("MIT 6.006: Doubly Linked Lists & LRU Caches", ResourceType.ARTICLE, "https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/", "MIT OpenCourseWare cache data structures.")
                )
        ));

        // Concept 1005: Two-Pointer Technique
        saveConceptIfAbsent(cr, Concept.create(
                1005L,
                arraysTopic.getId(),
                arraysTopic.getName(),
                "Two-Pointer Technique",
                "Algorithmic pattern using two coordinate references iterating through an array simultaneously to optimize search or partition operations.\n\n"
                        + "### Why This Matters\n"
                        + "Brute force checks of pairs in an array take O(N^2) time. The Two-Pointer technique leverages array ordering to solve pair sum, string reversal, palindrome checks, and container partitioning in O(N) linear time and O(1) auxiliary space.\n\n"
                        + "### The Idea\n"
                        + "Initialize two pointers (typically `left` at index 0 and `right` at index N-1). Depending on the evaluation of `arr[left] + arr[right]`, conditionally move either pointer inward, pruning the search space by half at each comparison.\n\n"
                        + "### Real Example\n"
                        + "Two people starting at opposite ends of a row of lockers, walking toward the center until they find a matching pair of numbers.",
                List.of(
                        "Requires a sorted array or sequence for directional convergence.",
                        "Reduces pair search problems from O(N^2) quadratic time to O(N) linear time.",
                        "Uses O(1) auxiliary memory because no additional collections are allocated.",
                        "Standard interview pattern for Two Sum II, 3Sum, Container With Most Water, and Trapping Rainwater."
                ),
                "// Two-pointer target sum on sorted array\npublic static boolean hasTargetSum(int[] sortedArr, int target) {\n    int left = 0, right = sortedArr.length - 1;\n    while (left < right) {\n        int currentSum = sortedArr[left] + sortedArr[right];\n        if (currentSum == target) return true;\n        if (currentSum < target) left++; // Need larger sum\n        else right--; // Need smaller sum\n    }\n    return false;\n}",
                List.of(
                        LearningResource.create("MIT 6.006: Linear Sorting & Two-Pointer Methods", ResourceType.ARTICLE, "https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/", "MIT OCW linear scanning patterns.")
                )
        ));
        seedRemember(rr, cr, 1005L, RememberItemType.KEY_FACT, "Two-pointer convergence requires sorted input; if array is unsorted, sorting first incurs O(N log N) time.", 1);
        seedRemember(rr, cr, 1005L, RememberItemType.INTERVIEW_REMINDER, "Beware of infinite loops: ensure pointer increment/decrement conditions always guarantee convergence (left < right).", 2);
        seedRel(crel, cr, 1002L, 1005L, ConceptRelationshipType.RELATED, "Two-pointer scanning and prefix sum range query techniques", 1);
    }

    // --------------------------------------------------------------------------
    // 3. Java & OOP
    // --------------------------------------------------------------------------
    private void seedJavaAndOop(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic oopPrinciples = findOrCreateTopic(ts, "java-oop", "Core OOP Principles", "oop-principles",
                "Encapsulation, inheritance, runtime polymorphism, virtual method dispatch, and abstract classes.", 1);
        Topic solidTopic = findOrCreateTopic(ts, "java-oop", "SOLID & Clean Architecture", "oop-solid",
                "Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion.", 2);
        Topic modernJvmTopic = findOrCreateTopic(ts, "java-oop", "Modern Java & JVM Internals", "java-modern-jvm",
                "Java Collections Framework, HashMap mechanics, Generics type erasure, and JVM memory zones.", 3);

        // Concept 5001: Polymorphism: Overloading vs Overriding
        saveConceptIfAbsent(cr, Concept.create(
                5001L,
                oopPrinciples.getId(),
                oopPrinciples.getName(),
                "Polymorphism: Overloading vs Overriding",
                "The object-oriented capability of an entity to take on multiple computational forms at compile time or runtime.\n\n"
                        + "### Why This Matters\n"
                        + "Polymorphism enables loose coupling and extensibility. Systems can define high-level contracts that interact with parent interfaces without needing to know which concrete subclasses are being executed at runtime.\n\n"
                        + "### The Idea\n"
                        + "Compile-time (static) polymorphism is resolved by the compiler via **Method Overloading** (same method name, different parameter types within the same class).\n\n"
                        + "Runtime (dynamic) polymorphism is resolved dynamically via **Method Overriding** (subclass provides an identical method signature). The JVM inspects the object's virtual method table (`vtable`) at runtime to invoke the actual subclass implementation.\n\n"
                        + "### Real Example\n"
                        + "A universal payment gateway: `PaymentProcessor` defines `processPayment()`. At runtime, concrete subclasses `CreditCardPayment`, `UpiPayment`, and `CryptoPayment` override the method with their own execution logic without altering the merchant checkout controller.",
                List.of(
                        "Overloading: Compile-time resolution based on static parameter types; return type alone cannot distinguish overloads.",
                        "Overriding: Runtime dynamic dispatch based on actual heap object instance type via the JVM virtual method table (vtable).",
                        "`private`, `static`, and `final` methods cannot be overridden in Java (resolved via static binding).",
                        "Use the `@Override` annotation to catch signature mismatches at compile time."
                ),
                "// Method Overriding (Runtime Dynamic Dispatch) in Java 21\ninterface Shape {\n    double area();\n}\n\nrecord Circle(double radius) implements Shape {\n    @Override\n    public double area() { return Math.PI * radius * radius; }\n}\n\nrecord Rectangle(double width, double height) implements Shape {\n    @Override\n    public double area() { return width * height; }\n}\n\n// Usage: polymorphic invocation\nShape s = new Circle(5.0);\nSystem.out.println(s.area()); // Dispatches dynamically to Circle.area()",
                List.of(
                        LearningResource.create("Oracle Java SE 21 Specification: Polymorphism", ResourceType.ARTICLE, "https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8", "Official JLS specification for method overriding and virtual methods.")
                )
        ));
        seedRemember(rr, cr, 5001L, RememberItemType.KEY_FACT, "Static polymorphism = Overloading (compile-time); Dynamic polymorphism = Overriding (runtime vtable dispatch).", 1);
        seedRemember(rr, cr, 5001L, RememberItemType.COMMON_CONFUSION, "In Java, variables (fields) are NOT polymorphic; field access is resolved based on the reference type, not the object instance type.", 2);
        seedRemember(rr, cr, 5001L, RememberItemType.INTERVIEW_REMINDER, "Be prepared to explain why static methods cannot be overridden (they belong to the class, not instances, and use invokestatic bytecode).", 3);

        // Concept 5002: Interfaces vs Abstract Classes
        saveConceptIfAbsent(cr, Concept.create(
                5002L,
                oopPrinciples.getId(),
                oopPrinciples.getName(),
                "Interfaces vs Abstract Classes",
                "Contractual and architectural mechanisms for abstraction and code reuse in object-oriented systems.\n\n"
                        + "### Why This Matters\n"
                        + "One of the most frequent placement interview questions. Choosing incorrectly between an interface and an abstract class leads to rigid, fragile inheritance hierarchies and violated design principles.\n\n"
                        + "### The Idea\n"
                        + "An **Interface** represents a capability contract ('CAN-DO'): a class can implement multiple interfaces. Interfaces cannot hold instance state (non-static mutable fields).\n\n"
                        + "An **Abstract Class** represents an identity hierarchy ('IS-A'): a class can extend only one abstract class (single inheritance). Abstract classes can declare instance variables, constructors, and maintain mutable state.\n\n"
                        + "### Real Example\n"
                        + "An abstract class `Vehicle` has fields like `vinNumber` and `fuelLevel`. Interfaces like `ElectricChargeable` and `AutonomousNavigable` define capabilities implemented across disparate vehicles.",
                List.of(
                        "Java supports multiple interface inheritance, but only single class inheritance.",
                        "Abstract classes can declare constructors and instance state; interfaces cannot store non-static fields.",
                        "Since Java 8, interfaces can provide `default` and `static` methods; Java 9 added `private` interface methods.",
                        "Rule of thumb: Favor interfaces for defining decoupled system API contracts; use abstract classes for sharing internal implementation state."
                ),
                "// Abstract Class vs Interface comparison in Java 21\npublic abstract class BaseRepository {\n    protected final String connectionUrl;\n    public BaseRepository(String url) { this.connectionUrl = url; }\n    public abstract void connect();\n}\n\npublic interface Auditable {\n    void audit(String action);\n    default void logAudit(String action) {\n        System.out.println(\"Audit timestamp: \" + java.time.Instant.now() + \" Action: \" + action);\n    }\n}",
                List.of(
                        LearningResource.create("Oracle Java Tutorial: Interfaces", ResourceType.ARTICLE, "https://docs.oracle.com/javase/tutorial/java/IandI/createinterface.html", "Official Java interface and abstract class design guide.")
                )
        ));
        seedRemember(rr, cr, 5002L, RememberItemType.KEY_FACT, "Abstract classes have constructors and instance fields; Interfaces define contracts and can be implemented multiply.", 1);
        seedRemember(rr, cr, 5002L, RememberItemType.COMMON_CONFUSION, "Default methods in interfaces do not make them abstract classes because interfaces still cannot hold mutable instance state.", 2);

        // Concept 5003: Java Collections: HashMap Mechanics
        saveConceptIfAbsent(cr, Concept.create(
                5004L,
                modernJvmTopic.getId(),
                modernJvmTopic.getName(),
                "Java Collections: HashMap Mechanics",
                "An associative key-value hash table providing average O(1) time complexity for insertions, deletions, and retrievals.\n\n"
                        + "### Why This Matters\n"
                        + "HashMap internal mechanics (hashing, collision resolution, treeification) are among the top 3 most frequently asked Java placement questions by tier-1 product companies.\n\n"
                        + "### The Idea\n"
                        + "HashMap uses an array of buckets (`Node<K,V>[] table`). When `put(key, value)` is called, Java computes `hash = key.hashCode() ^ (hash >>> 16)` and calculates the index via `(n - 1) & hash`. When collisions occur (multiple keys hash to the same bucket index), entries are stored in a linked list. If a bucket's collision chain exceeds the threshold of 8 nodes and the table capacity is at least 64, Java converts the linked list into a balanced Red-Black Tree (`TreeNode`), guaranteeing worst-case O(log N) operations.\n\n"
                        + "### Real Example\n"
                        + "Storing a university student directory by student ID: looking up `studentMap.get(\"CS-2026-001\")` takes O(1) average time via hash calculation.",
                List.of(
                        "Default initial capacity is 16; default load factor is 0.75.",
                        "Rehashing occurs when `size > capacity * loadFactor`, doubling the bucket array size.",
                        "Treeification threshold: when a bucket chain reaches 8 nodes and capacity >= 64, it converts to a Red-Black tree.",
                        "The contract between `equals()` and `hashCode()`: if two objects are equal according to `equals()`, their `hashCode()` must return identical integer values."
                ),
                "// HashMap contract demonstration in Java 21\npublic final class EmployeeKey {\n    private final int id;\n    public EmployeeKey(int id) { this.id = id; }\n    @Override\n    public boolean equals(Object o) {\n        if (this == o) return true;\n        if (!(o instanceof EmployeeKey that)) return false;\n        return this.id == that.id;\n    }\n    @Override\n    public int hashCode() {\n        return Integer.hashCode(id); // Must match equals criteria\n    }\n}",
                List.of(
                        LearningResource.create("OpenJDK Source: HashMap Implementation", ResourceType.ARTICLE, "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html", "Official Java SE 21 HashMap API documentation.")
                )
        ));
        seedRemember(rr, cr, 5004L, RememberItemType.KEY_FACT, "HashMap index calculation: (n - 1) & hash; bucket treeification occurs at threshold 8 when table capacity >= 64.", 1);
        seedRemember(rr, cr, 5004L, RememberItemType.COMMON_CONFUSION, "HashMap is not thread-safe; concurrent modifications without synchronization can corrupt bucket structures. Use ConcurrentHashMap for multithreaded environments.", 2);
    }

    // --------------------------------------------------------------------------
    // 4. DBMS & SQL (Preserves 2001-2003)
    // --------------------------------------------------------------------------
    private void seedDbmsAndSql(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic sqlTopic = findOrCreateTopic(ts, "dbms", "SQL & Database Queries", "sql-databases",
                "Relational schema queries, complex joins, aggregation, grouping, and subqueries.", 1);
        Topic schemaTopic = findOrCreateTopic(ts, "dbms", "Database Schema & Normalization", "dbms-schema-design",
                "Functional dependencies, normal forms (1NF, 2NF, 3NF, BCNF), and schema integrity constraints.", 2);
        Topic txTopic = findOrCreateTopic(ts, "dbms", "Transactions, ACID & Storage Engines", "dbms-transactions-storage",
                "ACID properties, isolation levels, concurrency anomalies, write-ahead logging, and B+ Tree indexes.", 3);

        // 2001: SELECT Statement (Preserved)
        saveOrUpdateConcept(cr, Concept.create(
                2001L,
                sqlTopic.getId(),
                sqlTopic.getName(),
                "SELECT Statement",
                "The core declarative clause used to retrieve and project attributes from database tables.\n\n"
                        + "### Why This Matters\n"
                        + "SQL is a declarative language: you specify *what* data you need, and the database query optimizer plans *how* to physically retrieve it using indexes and table scans.\n\n"
                        + "### The Idea\n"
                        + "Although written starting with `SELECT`, the logical execution order of an SQL query is: FROM/JOIN -> WHERE -> GROUP BY -> HAVING -> SELECT -> DISTINCT -> ORDER BY -> LIMIT.\n\n"
                        + "### Real Example\n"
                        + "Retrieving active graduating students sorted alphabetically to print degree certificates.",
                List.of(
                        "Column projection filters returned attributes to minimize network payload.",
                        "WHERE clause applies boolean predicates to filter tuples prior to grouping.",
                        "ORDER BY sorts the returned records ascending (ASC) or descending (DESC).",
                        "LIMIT / OFFSET manages pagination for performant desktop and web data loading."
                ),
                "-- Retrieve active graduating students\nSELECT id, full_name, email, graduation_year\nFROM student_profiles\nWHERE graduation_year = 2026\nORDER BY full_name ASC\nLIMIT 20;",
                List.of(
                        LearningResource.create("PostgreSQL SQL SELECT Syntax Reference", ResourceType.ARTICLE, "https://www.postgresql.org/docs/current/sql-select.html", "Authoritative PostgreSQL SQL command specification.")
                )
        ));

        // 2002: JOIN Operations (Preserved)
        saveOrUpdateConcept(cr, Concept.create(
                2002L,
                sqlTopic.getId(),
                sqlTopic.getName(),
                "JOIN Operations",
                "Relational algebra operation combining records from multiple tables based on matching foreign key keys.\n\n"
                        + "### Why This Matters\n"
                        + "Relational normalization separates entities into distinct tables (e.g. `users`, `orders`). JOIN operations reconstruct connected business records efficiently across relationships.\n\n"
                        + "### The Idea\n"
                        + "INNER JOIN keeps only rows with matching join keys in both tables. LEFT OUTER JOIN keeps all rows from the left table and inserts NULLs for unmatched right-table attributes.\n\n"
                        + "### Real Example\n"
                        + "Generating an order receipt showing student name, email, and the course they purchased.",
                List.of(
                        "INNER JOIN returns only tuples having matching values in both tables.",
                        "LEFT OUTER JOIN returns all rows from the left table and matched rows from the right table.",
                        "RIGHT OUTER JOIN preserves all records from the right table.",
                        "FULL OUTER JOIN combines matching and unmatched rows from both participating tables."
                ),
                "-- Join question attempts with student details\nSELECT u.email, sp.full_name, a.status, a.attempted_at\nFROM question_attempts a\nINNER JOIN users u ON a.user_id = u.id\nLEFT JOIN student_profiles sp ON u.id = sp.user_id\nWHERE a.status = 'SOLVED';",
                List.of(
                        LearningResource.create("PostgreSQL Documentation: Table Joins", ResourceType.ARTICLE, "https://www.postgresql.org/docs/current/tutorial-join.html", "Official relational join reference.")
                )
        ));

        // 2003: GROUP BY Clause (Preserved)
        saveOrUpdateConcept(cr, Concept.create(
                2003L,
                sqlTopic.getId(),
                sqlTopic.getName(),
                "GROUP BY Clause",
                "Aggregates rows that have matching values in specified columns into concise summary rows.\n\n"
                        + "### Why This Matters\n"
                        + "Essential for analytical queries, reporting dashboards, and computing statistical aggregations across categorized data.\n\n"
                        + "### The Idea\n"
                        + "Groups rows by unique values in grouping columns. Aggregate functions (COUNT, SUM, AVG, MIN, MAX) then compute summary statistics per group. The HAVING clause filters groups post-aggregation.\n\n"
                        + "### Real Example\n"
                        + "Calculating average salary per department where the department has more than 5 employees.",
                List.of(
                        "Collaborates with aggregate functions: COUNT(), SUM(), AVG(), MIN(), and MAX().",
                        "HAVING clause filters aggregated group results after grouping occurs.",
                        "WHERE filters individual rows before grouping, while HAVING filters post-aggregation groups.",
                        "All non-aggregated columns in SELECT must appear in the GROUP BY clause."
                ),
                "-- Count questions per topic having more than 3 questions\nSELECT topic_id, COUNT(*) AS question_count, MAX(created_at) AS latest_addition\nFROM questions\nGROUP BY topic_id\nHAVING COUNT(*) >= 3\nORDER BY question_count DESC;",
                List.of(
                        LearningResource.create("PostgreSQL Documentation: Aggregate Functions", ResourceType.ARTICLE, "https://www.postgresql.org/docs/current/tutorial-agg.html", "Official documentation on aggregation and GROUP BY.")
                )
        ));

        // Concept 2004: Database Normalization (1NF to 3NF & BCNF)
        saveConceptIfAbsent(cr, Concept.create(
                2004L,
                schemaTopic.getId(),
                schemaTopic.getName(),
                "Database Normalization (1NF to 3NF & BCNF)",
                "A formal relational database design technique organizing tables to minimize redundancy and prevent update, insertion, and deletion anomalies.\n\n"
                        + "### Why This Matters\n"
                        + "Un-normalized schemas store duplicate facts (e.g. repeated customer addresses across 10,000 order rows). If a customer updates their address, updating only some rows causes data inconsistency (Update Anomaly).\n\n"
                        + "### The Idea\n"
                        + "- **1NF**: Eliminate repeating groups; ensure all column values are atomic (scalar).\n"
                        + "- **2NF**: Must be in 1NF AND have no partial functional dependencies (every non-key attribute must depend on the whole primary key, not a subset of a composite key).\n"
                        + "- **3NF**: Must be in 2NF AND have no transitive functional dependencies (non-key attributes must depend only on the primary key, not on another non-key attribute: 'The Key, the Whole Key, and Nothing But the Key').\n"
                        + "- **BCNF**: Every determinant in a functional dependency (X -> Y) must be a candidate key.\n\n"
                        + "### Real Example\n"
                        + "Separating an `orders` table containing customer phone numbers into `orders` (referencing `customer_id`) and `customers` (storing `customer_id` and `phone_number` once).",
                List.of(
                        "1NF requires atomic values and no repeating column groups.",
                        "2NF eliminates partial dependencies on composite primary keys.",
                        "3NF eliminates transitive dependencies between non-prime attributes.",
                        "BCNF is a stricter version of 3NF where every determinant must be a candidate key."
                ),
                "-- Demonstrating 3NF decomposition:\n-- Un-normalized Table: Orders(order_id, product_id, product_name, unit_price)\n-- Decomposed into 3NF Tables:\nCREATE TABLE products (\n    id INT PRIMARY KEY,\n    name VARCHAR(100) NOT NULL,\n    unit_price DECIMAL(10, 2) NOT NULL\n);\n\nCREATE TABLE orders (\n    id INT PRIMARY KEY,\n    product_id INT NOT NULL REFERENCES products(id),\n    quantity INT NOT NULL\n);",
                List.of(
                        LearningResource.create("ACM/IEEE CS2023 Curricula: DM-Schema Design", ResourceType.ARTICLE, "https://cs2023.org/", "Formal relational normalization standards.")
                )
        ));
        seedRemember(rr, cr, 2004L, RememberItemType.KEY_FACT, "3NF rule: 'Every attribute must depend on the Key, the Whole Key, and Nothing But the Key, so help me Codd.'", 1);
        seedRemember(rr, cr, 2004L, RememberItemType.COMMON_CONFUSION, "Normalization minimizes redundancy and update anomalies, but excessive joins can decrease read performance, leading to deliberate denormalization in OLAP/analytics.", 2);
    }

    // --------------------------------------------------------------------------
    // 5. Operating Systems (Preserves 3001-3004)
    // --------------------------------------------------------------------------
    private void seedOperatingSystems(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic osTopic = findOrCreateTopic(ts, "operating-systems", "Operating Systems & Concurrency", "os-concurrency",
                "Processes, threads, synchronization, deadlocks, virtual memory, and CPU scheduling.", 1);
        Topic syncTopic = findOrCreateTopic(ts, "operating-systems", "Synchronization & Deadlocks", "os-synchronization",
                "Mutual exclusion, race conditions, semaphores, mutexes, and Coffman conditions.", 2);
        Topic vmTopic = findOrCreateTopic(ts, "operating-systems", "Virtual Memory & Memory Management", "os-memory-management",
                "Paging, page tables, TLB, page faults, and page replacement policies.", 3);

        // 3001: Process States (Preserved with visual steps)
        saveOrUpdateConcept(cr, Concept.create(
                3001L,
                osTopic.getId(),
                osTopic.getName(),
                "Process States",
                "The distinct execution phases of an active program as managed by the OS process scheduler.\n\n"
                        + "### Why This Matters\n"
                        + "A fundamental OS concept tested in every technical interview. The OS scheduler continually shifts processes across states to maximize CPU utilization and provide responsive time-sharing.\n\n"
                        + "### The Idea\n"
                        + "A process transitions through 5 canonical states: NEW (being created), READY (in memory waiting for CPU), RUNNING (executing on core), WAITING (blocked on I/O or event), and TERMINATED (finished execution).\n\n"
                        + "### How It Works\n"
                        + "[NEW] -> [READY] -> [RUNNING] -> [TERMINATED]\n\n"
                        + "### Real Example\n"
                        + "When a program reads a file from disk, the CPU does not wait idle for slow disk sectors; the OS moves the process from RUNNING to WAITING and schedules another READY process.",
                List.of(
                        "NEW: The program is being created and loaded into memory.",
                        "READY: The process is in memory waiting to be assigned to a CPU core.",
                        "RUNNING: Instructions are actively being executed on the CPU core.",
                        "WAITING / BLOCKED: The process cannot proceed until an I/O event or signal completes.",
                        "TERMINATED: The process has finished execution and OS reclaims its resources."
                ),
                "// Process Lifecycle State Flow:\n// [NEW] -> [READY] -> [RUNNING] -> [TERMINATED]\n//            ^           |\n//            |--[WAITING]<--",
                List.of(
                        LearningResource.create("Silberschatz OS Concepts: Process Lifecycle", ResourceType.ARTICLE, "https://www.os-book.com/", "Authoritative operating systems textbook reference.")
                )
        ));

        // 3002: Context Switching (Preserved)
        saveOrUpdateConcept(cr, Concept.create(
                3002L,
                osTopic.getId(),
                osTopic.getName(),
                "Context Switching",
                "Saving the context of a preempted process and restoring the context of the next scheduled process.\n\n"
                        + "### Why This Matters\n"
                        + "Enables preemptive multitasking on modern multi-user, multi-core operating systems. Context switching represents pure computational overhead during which zero user work is executed.\n\n"
                        + "### The Idea\n"
                        + "When a timer tick or hardware interrupt occurs, the OS kernel saves the executing process's Program Counter (PC), CPU registers, and stack pointers into its Process Control Block (PCB), then restores the saved PCB state of the next ready process.\n\n"
                        + "### Real Example\n"
                        + "A single chef cooking two complex dishes: before pausing dish A to stir dish B, the chef notes temperature and timer reading on a notepad (PCB) so they can resume dish A without burning it.",
                List.of(
                        "Saves CPU registers, Program Counter (PC), and stack pointer into the Process Control Block (PCB).",
                        "Restores the saved state from the next process's PCB before resuming execution.",
                        "Pure computational overhead: the CPU performs zero user application work during a context switch.",
                        "Hardware interrupts, system calls, and timer ticks trigger scheduler context switches."
                ),
                "// Pseudocode of Context Switch flow:\nvoid contextSwitch(PCB* currentProcess, PCB* nextProcess) {\n    saveRegisters(currentProcess->registers);\n    currentProcess->state = READY;\n    \n    loadRegisters(nextProcess->registers);\n    nextProcess->state = RUNNING;\n    jumpTo(nextProcess->programCounter);\n}",
                List.of(
                        LearningResource.create("Linux Kernel Documentation: Scheduler Context Switching", ResourceType.ARTICLE, "https://www.kernel.org/doc/html/latest/scheduler/index.html", "Linux kernel scheduler context switch mechanics.")
                )
        ));

        // 3003: CPU Scheduling (Preserved)
        saveOrUpdateConcept(cr, Concept.create(
                3003L,
                osTopic.getId(),
                osTopic.getName(),
                "CPU Scheduling",
                "OS algorithms allocating CPU execution time among processes in the ready queue.\n\n"
                        + "### Why This Matters\n"
                        + "Determines system throughput, fairness, turnaround time, and latency. Schedulers prevent starvation while optimizing CPU burst utilization.\n\n"
                        + "### The Idea\n"
                        + "Non-preemptive algorithms (FCFS, SJF) allow a process to hold the CPU until voluntary release or I/O. Preemptive algorithms (Round Robin, SRTF) allocate fixed time slices (quanta), preempting processes when their quantum expires.\n\n"
                        + "### Real Example\n"
                        + "A bank teller serving customers: FCFS serves whoever queued first; Round Robin spends 2 minutes with each customer in rotation so nobody waits indefinitely.",
                List.of(
                        "Non-preemptive algorithms (FCFS, SJF) allow a process to run until it finishes or waits for I/O.",
                        "Preemptive algorithms (Round Robin, SRTF) allocate CPU bursts based on time quanta or priority.",
                        "Key scheduling metrics: Turnaround Time, Waiting Time, Response Time, and CPU Throughput.",
                        "Convoy effect in FCFS occurs when short processes queue behind a long CPU-bound process."
                ),
                "// Round Robin Scheduling simulation\nint timeQuantum = 4; // 4 ms\nQueue<Process> readyQueue = new LinkedList<>();\n// Process executes up to quantum before preemption",
                List.of(
                        LearningResource.create("Silberschatz OS Concepts: CPU Scheduling", ResourceType.ARTICLE, "https://www.os-book.com/", "Standard university textbook reference on scheduling.")
                )
        ));

        // 3004: Threads & Concurrency (Preserved)
        saveOrUpdateConcept(cr, Concept.create(
                3004L,
                osTopic.getId(),
                osTopic.getName(),
                "Threads & Concurrency",
                "Lightweight execution contexts within a single process sharing address space and open files.\n\n"
                        + "### Why This Matters\n"
                        + "Threads allow multi-core CPU parallel execution without the heavy memory overhead of spawning separate processes.\n\n"
                        + "### The Idea\n"
                        + "Threads within the same process share code, global data, and heap memory, but retain private thread registers, program counter, and stack frames. Concurrent access to shared mutable data requires synchronization locks.\n\n"
                        + "### Real Example\n"
                        + "A word processor: one thread handles user keyboard typing, another thread continuously spell-checks text in the background, and a third thread auto-saves to disk.",
                List.of(
                        "Threads in the same process share code, data, and OS resources, but each retains its own registers and stack.",
                        "Thread context switching has significantly lower overhead than full process context switching.",
                        "User threads are scheduled by runtime libraries; kernel threads are managed by the OS scheduler.",
                        "Concurrent access to shared memory requires synchronization mechanisms like mutexes and semaphores."
                ),
                "// Java 21 Thread execution\nThread thread = Thread.ofVirtual().start(() -> {\n    System.out.println(\"Running concurrently: \" + Thread.currentThread());\n});\nthread.join();",
                List.of(
                        LearningResource.create("Oracle Java Concurrency Tutorial", ResourceType.ARTICLE, "https://docs.oracle.com/javase/tutorial/essential/concurrency/", "Official Java multithreading and thread synchronization guide.")
                )
        ));

        // Concept 3005: Mutex vs Semaphore
        saveConceptIfAbsent(cr, Concept.create(
                3005L,
                syncTopic.getId(),
                syncTopic.getName(),
                "Mutex vs Semaphore",
                "Synchronization primitives controlling concurrent access to shared critical resources in multithreaded systems.\n\n"
                        + "### Why This Matters\n"
                        + "One of the classic technical placement questions. Confusing a mutex with a binary semaphore in an interview indicates a lack of understanding regarding thread ownership and synchronization semantics.\n\n"
                        + "### The Idea\n"
                        + "A **Mutex** (Mutual Exclusion lock) is an ownership-based locking mechanism: only the thread that locked the mutex can unlock it. It ensures exclusive access to a single resource.\n\n"
                        + "A **Semaphore** is a signaling mechanism backed by an integer counter: it controls access to a finite pool of identical resources. A thread increments the counter (`signal()` / `release()`) or decrements it (`wait()` / `acquire()`). A semaphore can be signaled by a thread other than the one that acquired it.\n\n"
                        + "### Real Example\n"
                        + "A single restroom key (Mutex): only the person inside holding the key can unlock the door. A parking garage with a sign showing 10 available spots (Counting Semaphore): entering cars decrement the count, exiting cars increment the count.",
                List.of(
                        "Mutex provides mutual exclusion with strict ownership (only the lock owner can release it).",
                        "Semaphore is a signaling mechanism with an integer counter (no ownership restriction).",
                        "Binary Semaphore (counter 0 or 1) can be used for signaling, but does not enforce ownership like a Mutex.",
                        "Priority inversion can occur with mutexes if a low-priority thread holds a lock needed by a high-priority thread."
                ),
                "// Java 21 Semaphore controlling 3 database connections\njava.util.concurrent.Semaphore pool = new java.util.concurrent.Semaphore(3);\n\nvoid executeQuery() throws InterruptedException {\n    pool.acquire(); // Decrements counter; blocks if 0\n    try {\n        // Access shared database resource\n    } finally {\n        pool.release(); // Increments counter\n    }\n}",
                List.of(
                        LearningResource.create("Silberschatz OS Concepts: Synchronization Primitives", ResourceType.ARTICLE, "https://www.os-book.com/", "Operating systems synchronization mechanisms.")
                )
        ));
        seedRemember(rr, cr, 3005L, RememberItemType.KEY_FACT, "Mutex has ownership (lock/unlock by same thread); Semaphore is a signaling counter (can be signaled by different threads).", 1);
        seedRemember(rr, cr, 3005L, RememberItemType.COMMON_CONFUSION, "A binary semaphore is NOT identical to a mutex: binary semaphores lack ownership and can be released by any thread.", 2);
    }

    // --------------------------------------------------------------------------
    // 6. Computer Networks
    // --------------------------------------------------------------------------
    private void seedComputerNetworks(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic layersTopic = findOrCreateTopic(ts, "computer-networks", "Network Architectures & Protocols", "net-layers",
                "OSI 7-layer and TCP/IP 4-layer models, packet encapsulation, and DNS resolution.", 1);
        Topic transportTopic = findOrCreateTopic(ts, "computer-networks", "Transport Layer: TCP & UDP", "net-transport",
                "TCP 3-way handshake, connection teardown, sliding window flow control, and UDP.", 2);

        // Concept 6001: TCP 3-Way Handshake
        saveConceptIfAbsent(cr, Concept.create(
                6001L,
                transportTopic.getId(),
                transportTopic.getName(),
                "TCP 3-Way Handshake & Connection Lifecycle",
                "The protocol sequence establishing a reliable, full-duplex, sequence-synchronized connection between client and server before data transfer begins.\n\n"
                        + "### Why This Matters\n"
                        + "The TCP handshake is the fundamental connection mechanism powering HTTP/1.1, HTTP/2, SSH, and database connection pools. Every backend software engineer must understand its packet exchanges and error states.\n\n"
                        + "### The Idea\n"
                        + "1. **SYN**: Client sends a SYN packet with Initial Sequence Number (ISN_c) to synchronize.\n"
                        + "2. **SYN-ACK**: Server acknowledges with ACK (ISN_c + 1) and sends its own SYN with ISN_s.\n"
                        + "3. **ACK**: Client acknowledges with ACK (ISN_s + 1). The connection is now ESTABLISHED.\n\n"
                        + "### How It Works\n"
                        + "[CLOSED] -> [SYN_SENT] -> [ESTABLISHED] -> [TIME_WAIT]\n\n"
                        + "### Real Example\n"
                        + "A telephone conversation opening: 'Can you hear me?' -> 'Yes, I hear you, can you hear me?' -> 'Yes, I hear you loud and clear. Let's talk.'",
                List.of(
                        "Step 1: Client sends SYN (Synchronize sequence number).",
                        "Step 2: Server responds with SYN-ACK (Synchronize + Acknowledge).",
                        "Step 3: Client replies with ACK. Connection enters ESTABLISHED state.",
                        "Teardown uses a 4-way FIN/ACK handshake to gracefully terminate both halves of the full-duplex connection."
                ),
                "// TCP 3-Way Handshake Packet Exchange Flow:\n// Client                      Server\n//   | -------- [SYN] -------->  |  (Client: SYN_SENT)\n//   | <---- [SYN-ACK] -------  |  (Server: SYN_RCVD)\n//   | -------- [ACK] -------->  |  (Both: ESTABLISHED)",
                List.of(
                        LearningResource.create("IETF RFC 9293: Transmission Control Protocol (TCP)", ResourceType.ARTICLE, "https://datatracker.ietf.org/doc/html/rfc9293", "Authoritative IETF internet standard specification for TCP.")
                )
        ));
        seedRemember(rr, cr, 6001L, RememberItemType.KEY_FACT, "TCP 3-Way Handshake: SYN -> SYN-ACK -> ACK synchronizes Initial Sequence Numbers (ISNs) bidirectionally.", 1);
        seedRemember(rr, cr, 6001L, RememberItemType.COMMON_CONFUSION, "TCP connection teardown takes 4 steps (FIN -> ACK -> FIN -> ACK) because TCP is full-duplex and each direction closes independently.", 2);
    }

    // --------------------------------------------------------------------------
    // 7. Computer Organization & Architecture
    // --------------------------------------------------------------------------
    private void seedComputerOrganization(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic cpuTopic = findOrCreateTopic(ts, "computer-organization", "CPU Datapath & Pipelining", "arch-cpu-pipeline",
                "Instruction cycle (Fetch, Decode, Execute), pipelining, and hazards.", 1);
        Topic cacheTopic = findOrCreateTopic(ts, "computer-organization", "Memory Hierarchy & Caches", "arch-memory-cache",
                "L1/L2/L3 cache hierarchies, spatial and temporal locality, and cache mapping schemes.", 2);

        // Concept 7001: Memory Hierarchy & Cache Locality
        saveConceptIfAbsent(cr, Concept.create(
                7001L,
                cacheTopic.getId(),
                cacheTopic.getName(),
                "Memory Hierarchy & Cache Locality",
                "The layered pyramid of computer memory balancing storage capacity, cost, and access latency via locality of reference.\n\n"
                        + "### Why This Matters\n"
                        + "A CPU register access takes ~0.5 ns, L1 cache takes ~1 ns, RAM takes ~100 ns, and disk takes ~10,000,000 ns. Software that respects hardware cache locality runs up to 100x faster than code with frequent cache misses.\n\n"
                        + "### The Idea\n"
                        + "- **Temporal Locality**: If a memory location is accessed once, it is likely to be accessed again in the near future (e.g. loop counter variable).\n"
                        + "- **Spatial Locality**: If a memory location is accessed, nearby memory locations are likely to be accessed soon (e.g. iterating an array sequentially).\n\n"
                        + "### Real Example\n"
                        + "A student studying at a library desk: frequently needed books are placed directly on the desk (L1 cache); less frequent books are in their backpack (L2 cache); rarely needed books require walking to library bookshelves (RAM).",
                List.of(
                        "Memory pyramid from fastest to slowest: Registers < L1 Cache < L2 Cache < L3 Cache < DRAM < NVMe/SSD.",
                        "CPUs load data into cache lines (typically 64 bytes at a time), not individual bytes.",
                        "Row-major 2D array traversal provides superior spatial locality over column-major traversal in C/Java.",
                        "Cache misses are classified by the 3 Cs: Compulsory (cold), Capacity, and Conflict misses."
                ),
                "// Cache-friendly row-major traversal (fast spatial locality)\nint[][] matrix = new int[1000][1000];\nlong sum = 0;\nfor (int r = 0; r < 1000; r++) {\n    for (int c = 0; c < 1000; c++) {\n        sum += matrix[r][c]; // Sequential contiguous memory read\n    }\n}",
                List.of(
                        LearningResource.create("MIT 6.004: Memory Hierarchy & Caches", ResourceType.ARTICLE, "https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/", "MIT OCW hardware computation structures course.")
                )
        ));
        seedRemember(rr, cr, 7001L, RememberItemType.KEY_FACT, "Cache lines are typically 64 bytes; contiguous array reads exploit spatial locality to maximize L1/L2 hits.", 1);
    }

    // --------------------------------------------------------------------------
    // 8. Theory of Computation
    // --------------------------------------------------------------------------
    private void seedTheoryOfComputation(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic automataTopic = findOrCreateTopic(ts, "theory-of-computation", "Finite Automata & Languages", "toc-automata",
                "Deterministic and non-deterministic finite automata, regular expressions, and formal grammars.", 1);
        Topic complexityTopic = findOrCreateTopic(ts, "theory-of-computation", "Computability & Complexity", "toc-complexity",
                "Turing machines, the Halting problem, undecidability, and P vs NP complexity classes.", 2);

        // Concept 8001: Deterministic Finite Automata (DFA)
        saveConceptIfAbsent(cr, Concept.create(
                8001L,
                automataTopic.getId(),
                automataTopic.getName(),
                "Deterministic Finite Automata (DFA)",
                "A formal computational state machine that transitions deterministically between a finite set of states based on input symbols.\n\n"
                        + "### Why This Matters\n"
                        + "DFAs are the mathematical foundation of compiler lexical analyzers (lexers/tokenizers), regular expression engines, protocol state machines, and hardware digital circuit controllers.\n\n"
                        + "### The Idea\n"
                        + "A DFA is formally defined as a 5-tuple: (Q, Sigma, delta, q0, F) where Q is the finite set of states, Sigma is the alphabet, delta is the transition function, q0 is the initial state, and F is the set of accepting states. For every state and input symbol, exactly one transition exists.\n\n"
                        + "### Real Example\n"
                        + "A turnstile at a subway station: states are [LOCKED] and [UNLOCKED]. Input 'Coin' transitions from LOCKED to UNLOCKED. Input 'Push' transitions from UNLOCKED to LOCKED.",
                List.of(
                        "DFA is deterministic: for each state and input symbol, there is exactly one state transition.",
                        "Recognizes the class of Regular Languages (equivalent in computational power to NFAs and Regular Expressions).",
                        "Operates with zero auxiliary memory beyond the current active state.",
                        "Cannot recognize non-regular languages requiring unbounded counting (e.g. {a^n b^n})."
                ),
                "// DFA transition function simulation in Java 21\n// Recognizes strings ending in '01'\npublic static boolean acceptsEnding01(String binary) {\n    int state = 0; // State 0 (start), 1 (saw '0'), 2 (accept: saw '01')\n    for (char c : binary.toCharArray()) {\n        state = switch (state) {\n            case 0 -> (c == '0') ? 1 : 0;\n            case 1 -> (c == '1') ? 2 : 1;\n            case 2 -> (c == '0') ? 1 : 0;\n            default -> 0;\n        };\n    }\n    return state == 2;\n}",
                List.of(
                        LearningResource.create("MIT 18.404J: Theory of Computation", ResourceType.ARTICLE, "https://ocw.mit.edu/courses/18-404j-theory-of-computation-fall-2020/", "MIT OCW Michael Sipser automata lectures.")
                )
        ));
        seedRemember(rr, cr, 8001L, RememberItemType.KEY_FACT, "DFAs recognize exactly the Regular Languages; Subset Construction proves NFA and DFA have identical language recognition power.", 1);
    }

    // --------------------------------------------------------------------------
    // 9. Software Engineering
    // --------------------------------------------------------------------------
    private void seedSoftwareEngineering(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic agileTopic = findOrCreateTopic(ts, "software-engineering", "SDLC & Agile Scrum", "se-sdlc-agile",
                "Agile Scrum sprint lifecycles, standups, retrospectives, and version control workflows.", 1);
        Topic designPatterns = findOrCreateTopic(ts, "software-engineering", "Design Patterns", "se-design-patterns",
                "Creational, structural, and behavioral software engineering design patterns.", 2);

        // Concept 9001: Core Design Patterns: Singleton, Factory & Strategy
        saveConceptIfAbsent(cr, Concept.create(
                9001L,
                designPatterns.getId(),
                designPatterns.getName(),
                "Core Design Patterns: Singleton, Factory & Strategy",
                "Reusable architectural solutions to common software design problems in object-oriented development.\n\n"
                        + "### Why This Matters\n"
                        + "Design patterns provide a standardized architectural vocabulary among software engineers and prevent reinvention of proven solutions during system architecture and code reviews.\n\n"
                        + "### The Idea\n"
                        + "- **Singleton**: Ensures a class has only one instance and provides a global point of access.\n"
                        + "- **Factory Method**: Defines an interface for creating an object, but lets subclasses decide which class to instantiate.\n"
                        + "- **Strategy**: Defines a family of algorithms, encapsulates each one, and makes them interchangeable at runtime without modifying the client context.\n\n"
                        + "### Real Example\n"
                        + "A navigation GPS app calculating routes: the client context switches strategies between `DrivingRouteStrategy`, `WalkingRouteStrategy`, and `TransitRouteStrategy` dynamically based on user selection.",
                List.of(
                        "Singleton: Private constructor, static instance holder, thread-safe initialization (e.g. enum singleton).",
                        "Factory Method: Encapsulates object creation logic, decoupling clients from concrete class names.",
                        "Strategy: Implements Open/Closed Principle by allowing algorithm substitution without modifying the caller.",
                        "Prefer composition of behavioral strategies over rigid class inheritance trees."
                ),
                "// Strategy Pattern in Java 21\n@FunctionalInterface\ninterface DiscountStrategy {\n    double applyDiscount(double price);\n}\n\npublic class CheckoutService {\n    public double calculateTotal(double price, DiscountStrategy strategy) {\n        return strategy.applyDiscount(price);\n    }\n}\n\n// Usage via lambdas\nCheckoutService service = new CheckoutService();\ndouble total = service.calculateTotal(100.0, p -> p * 0.90); // 10% discount",
                List.of(
                        LearningResource.create("Refactoring.Guru: Design Patterns Guide", ResourceType.ARTICLE, "https://refactoring.guru/design-patterns", "Comprehensive structural breakdown of standard GoF design patterns.")
                )
        ));
        seedRemember(rr, cr, 9001L, RememberItemType.KEY_FACT, "Strategy pattern encapsulates interchangeable algorithms via interfaces; Factory pattern encapsulates object instantiation.", 1);
    }

    // --------------------------------------------------------------------------
    // 10. System Design
    // --------------------------------------------------------------------------
    private void seedSystemDesign(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic scalingTopic = findOrCreateTopic(ts, "system-design", "Scalability & Caching", "sd-foundations",
                "Horizontal vs vertical scaling, load balancers, caching strategies, and data partitioning.", 1);

        // Concept 10001: Vertical vs Horizontal Scaling
        saveConceptIfAbsent(cr, Concept.create(
                10001L,
                scalingTopic.getId(),
                scalingTopic.getName(),
                "Vertical vs Horizontal Scaling",
                "Architectural strategies for expanding system capacity to handle increasing user traffic and data volumes.\n\n"
                        + "### Why This Matters\n"
                        + "The primary architectural decision in every technical system design interview. Understanding the limitations, costs, and failure modes of each scaling approach is essential.\n\n"
                        + "### The Idea\n"
                        + "**Vertical Scaling (Scale-Up)** increases the computing power of a single machine (more CPU cores, RAM, faster NVMe). It requires no distributed code changes, but encounters hard physical hardware limits and introduces a Single Point of Failure (SPOF).\n\n"
                        + "**Horizontal Scaling (Scale-Out)** adds more commodity server nodes to a distributed cluster behind a load balancer. It enables linear elasticity and high availability, but requires stateless application architecture and distributed consistency management.\n\n"
                        + "### Real Example\n"
                        + "Scale-Up: Replacing your delivery truck with a larger 18-wheel truck (still only one truck that can break down). Scale-Out: Purchasing a fleet of 50 delivery vans operating in parallel across the city.",
                List.of(
                        "Vertical scaling: Simpler operations, no network partitioning, but hits physical hardware ceiling.",
                        "Horizontal scaling: Infinite theoretical scale, fault tolerance, but requires stateless services and distributed caching.",
                        "Stateless application servers store user session state in distributed caches (Redis) or database tiers.",
                        "Load balancers distribute incoming HTTP traffic evenly across horizontal worker nodes."
                ),
                "// Stateless microservice pattern: state stored externally in Redis/DB\n@RestController\npublic class OrderController {\n    // Application tier is stateless; any horizontal instance can handle request\n    @PostMapping(\"/orders\")\n    public ResponseEntity<Void> createOrder(@RequestBody OrderRequest req) {\n        // Delegate persistence to database tier\n        return ResponseEntity.ok().build();\n    }\n}",
                List.of(
                        LearningResource.create("MIT 6.033: Computer System Engineering", ResourceType.ARTICLE, "https://ocw.mit.edu/courses/6-033-computer-system-engineering-spring-2018/", "MIT OCW distributed systems and scalability engineering.")
                )
        ));
        seedRemember(rr, cr, 10001L, RememberItemType.KEY_FACT, "Horizontal scaling requires stateless application tiers; session state is offloaded to distributed stores (Redis).", 1);
    }

    // --------------------------------------------------------------------------
    // 11. Cybersecurity
    // --------------------------------------------------------------------------
    private void seedCybersecurity(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic webSecTopic = findOrCreateTopic(ts, "cybersecurity", "Web Application Security", "sec-web-app",
                "OWASP Top 10 vulnerabilities, SQL injection, XSS, CSRF, and authentication standards.", 1);

        // Concept 11001: SQL Injection (SQLi) & Defense
        saveConceptIfAbsent(cr, Concept.create(
                11001L,
                webSecTopic.getId(),
                webSecTopic.getName(),
                "SQL Injection (SQLi) Prevention",
                "A critical web vulnerability occurring when untrusted user input is directly concatenated into dynamic database query strings.\n\n"
                        + "### Why This Matters\n"
                        + "Consistently ranked in the OWASP Top 10. A successful SQL injection attack allows malicious actors to bypass authentication, dump sensitive database tables, modify data, and execute administrative operations.\n\n"
                        + "### The Idea\n"
                        + "When an application concatenates user strings into SQL commands, an attacker can input SQL syntax (such as `' OR '1'='1`) to alter query logic. The absolute defense is **Parameterized Queries (Prepared Statements)**: the database driver compiles query structure first and treats user parameters strictly as literal data, rendering code injection impossible.\n\n"
                        + "### Real Example\n"
                        + "A login query: `SELECT * FROM users WHERE email = '\" + email + \"'`. Inputting `admin@byteforce.com' --` comments out the password verification entirely.",
                List.of(
                        "Occurs when untrusted data is treated as executable SQL command syntax by the database parser.",
                        "Primary defense: Always use PreparedStatement with parameterized placeholders (?).",
                        "Never construct queries using string concatenation or StringBuilder.",
                        "Apply the Principle of Least Privilege: application database user accounts should only possess necessary table permissions."
                ),
                "// SECURE: Parameterized Prepared Statement in JDBC (ByteForce pattern)\nString sql = \"SELECT id, email FROM users WHERE email = ? AND active = true\";\ntry (PreparedStatement ps = conn.prepareStatement(sql)) {\n    ps.setString(1, userSuppliedEmail); // Treated strictly as literal data\n    try (ResultSet rs = ps.executeQuery()) {\n        // Process results\n    }\n}",
                List.of(
                        LearningResource.create("OWASP SQL Injection Prevention Cheat Sheet", ResourceType.ARTICLE, "https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html", "Official OWASP security guidelines.")
                )
        ));
        seedRemember(rr, cr, 11001L, RememberItemType.KEY_FACT, "Always use Parameterized Queries (Prepared Statements) to separate SQL query structure from data values.", 1);
    }

    // --------------------------------------------------------------------------
    // 12. Distributed Systems
    // --------------------------------------------------------------------------
    private void seedDistributedSystems(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic consensusTopic = findOrCreateTopic(ts, "distributed-systems", "Distributed Consensus & Coordination", "dist-consensus-coordination",
                "Consensus algorithms, the Raft protocol, distributed transactions, and two-phase commit.", 1);

        // Concept 12001: Distributed Consensus & The Raft Protocol
        saveConceptIfAbsent(cr, Concept.create(
                12001L,
                consensusTopic.getId(),
                consensusTopic.getName(),
                "Distributed Consensus & The Raft Protocol",
                "An algorithm enabling a cluster of distributed nodes to agree on a shared state machine log even in the presence of node failures and network partitions.\n\n"
                        + "### Why This Matters\n"
                        + "Distributed databases (etcd, Consul, Kafka KRaft) require consensus to elect leaders, ensure data replication consistency, and prevent split-brain anomalies.\n\n"
                        + "### The Idea\n"
                        + "Raft decomposes consensus into three distinct sub-problems: Leader Election, Log Replication, and Safety. Nodes exist in one of three states: Follower, Candidate, or Leader. If followers receive no heartbeat within a randomized election timeout, they transition to Candidate and request votes. A candidate receiving a majority quorum of votes becomes the Leader.\n\n"
                        + "### Real Example\n"
                        + "A corporate board of 5 directors: decisions require at least 3 matching votes (majority quorum of 5) to pass. If 2 directors are absent or on a broken phone line, the remaining 3 can still legally conduct company business.",
                List.of(
                        "Raft nodes transition between three states: Follower -> Candidate -> Leader.",
                        "Majority quorum requirement: a cluster of N nodes requires floor(N/2) + 1 nodes to reach consensus.",
                        "Randomized election timeouts prevent split-vote deadlocks during leader elections.",
                        "Only leaders accept client writes; logs are appended and committed once replicated to a majority of nodes."
                ),
                "// Raft state transitions representation:\n// [Follower] --(election timeout)--> [Candidate]\n// [Candidate] --(receives majority votes)--> [Leader]\n// [Leader] --(discovers higher term)--> [Follower]",
                List.of(
                        LearningResource.create("Ongaro & Ousterhout: In Search of an Understandable Consensus Algorithm (Raft)", ResourceType.ARTICLE, "https://raft.github.io/raft.pdf", "Original Stanford Raft consensus paper.")
                )
        ));
        seedRemember(rr, cr, 12001L, RememberItemType.KEY_FACT, "Raft requires a majority quorum (N/2 + 1) to elect a leader and commit log entries.", 1);
    }

    // --------------------------------------------------------------------------
    // 13. Programming Languages & Compilers
    // --------------------------------------------------------------------------
    private void seedCompilersAndLanguages(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic pipelineTopic = findOrCreateTopic(ts, "compilers-languages", "Compiler Pipeline & Architecture", "comp-pipeline",
                "Phases of a compiler: Lexing, Parsing, AST generation, Semantic Analysis, and Code Generation.", 1);

        // Concept 13001: Compiler Pipeline: Lexing to Code Generation
        saveConceptIfAbsent(cr, Concept.create(
                13001L,
                pipelineTopic.getId(),
                pipelineTopic.getName(),
                "Compiler Pipeline: Lexing to Code Generation",
                "The multi-stage translation sequence converting human-readable source code into executable machine instructions or virtual machine bytecode.\n\n"
                        + "### Why This Matters\n"
                        + "Understanding compilers dispels the 'magic' of programming languages, explaining how syntax errors, type checkers, memory optimizations, and bytecode generation function under the hood.\n\n"
                        + "### The Idea\n"
                        + "The compiler pipeline operates in sequential stages: Lexical Analysis (converts character stream to tokens) -> Syntax Analysis / Parsing (builds Abstract Syntax Tree based on grammar rules) -> Semantic Analysis (checks types, scopes) -> Intermediate Representation (IR optimization) -> Code Generation (outputs machine code or bytecode).\n\n"
                        + "### Real Example\n"
                        + "Translating a book from French to English: first identify words and punctuation (Lexing), verify grammatical sentence structures (Parsing), check semantic meaning (Semantics), write draft notes (IR), and produce final publication print (Code Generation).",
                List.of(
                        "Lexer (Scanner): Matches regex patterns to emit discrete tokens (IDENTIFIER, KEYWORD, OPERATOR).",
                        "Parser: Enforces Context-Free Grammar (CFG) rules to construct an Abstract Syntax Tree (AST).",
                        "Semantic Analyzer: Validates type correctness, variable scope declarations, and method contracts.",
                        "Optimizer & Code Generator: Emits optimized target bytecode (e.g. Java .class files) or native machine assembly."
                ),
                "// Compiler Pipeline Flow:\n// Source Code -> [Lexer] -> Tokens -> [Parser] -> AST -> [Semantic Analyzer] -> [Code Generator] -> Target Bytecode",
                List.of(
                        LearningResource.create("Aho, Lam, Sethi, Ullman: Compilers (Dragon Book)", ResourceType.ARTICLE, "https://cs2023.org/", "Canonical compiler construction reference.")
                )
        ));
        seedRemember(rr, cr, 13001L, RememberItemType.KEY_FACT, "Lexing produces Tokens from characters; Parsing produces an Abstract Syntax Tree (AST) from Tokens.", 1);
    }

    // --------------------------------------------------------------------------
    // 14. Web & API Fundamentals
    // --------------------------------------------------------------------------
    private void seedWebAndApiFundamentals(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic apiTopic = findOrCreateTopic(ts, "web-api-fundamentals", "Web Protocols & REST APIs", "web-api-design",
                "HTTP request-response lifecycle, REST architectural constraints, idempotency, and CORS.", 1);

        // Concept 14001: RESTful API Principles & Idempotency
        saveConceptIfAbsent(cr, Concept.create(
                14001L,
                apiTopic.getId(),
                apiTopic.getName(),
                "RESTful API Principles & Idempotency",
                "An architectural style for network-based hypermedia systems relying on stateless communication and standardized HTTP semantics.\n\n"
                        + "### Why This Matters\n"
                        + "Virtually every modern backend service communicates via REST APIs. Designing clean endpoints, adhering to HTTP status code conventions, and understanding idempotency are essential for backend placement interviews.\n\n"
                        + "### The Idea\n"
                        + "REST treats resources as nouns in URI paths (e.g. `/api/users/42`). Standard HTTP methods declare the action: `GET` (read), `POST` (create), `PUT` (replace), `PATCH` (partial update), `DELETE` (remove). An operation is **idempotent** if making multiple identical requests has the same side-effect on server state as making a single request (`GET`, `PUT`, `DELETE` are idempotent; `POST` is not).\n\n"
                        + "### Real Example\n"
                        + "Payment checkout: if a user double-clicks 'Pay Now' during network lag, an idempotent API using an idempotency key guarantees the credit card is only billed once.",
                List.of(
                        "URIs should use resource nouns (`/api/assessments`), never action verbs (`/api/getAssessments`).",
                        "Statelessness: each request must contain all context and authentication necessary to process it.",
                        "Idempotency: GET, PUT, and DELETE are idempotent; POST is non-idempotent.",
                        "Status codes: 2xx (Success), 3xx (Redirection), 4xx (Client Error), 5xx (Server Error)."
                ),
                "// RESTful Spring Controller in ByteForce\n@GetMapping(\"/api/topics/{id}\")\npublic ResponseEntity<Topic> getTopic(@PathVariable Long id) {\n    return topicService.getTopicById(id)\n            .map(ResponseEntity::ok)\n            .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found\n}",
                List.of(
                        LearningResource.create("IETF RFC 9110: HTTP Semantics", ResourceType.ARTICLE, "https://datatracker.ietf.org/doc/html/rfc9110", "Official IETF HTTP protocol specification.")
                )
        ));
        seedRemember(rr, cr, 14001L, RememberItemType.KEY_FACT, "Idempotent HTTP methods (GET, PUT, DELETE) produce the same server state regardless of how many times they are repeated.", 1);
    }

    // --------------------------------------------------------------------------
    // 15. AI & Machine Learning Fundamentals
    // --------------------------------------------------------------------------
    private void seedAiMlFundamentals(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic mlTopic = findOrCreateTopic(ts, "ai-ml-fundamentals", "Machine Learning Paradigms & Evaluation", "ml-paradigms",
                "Supervised, unsupervised, reinforcement learning, overfitting, underfitting, and evaluation metrics.", 1);

        // Concept 15001: Supervised vs Unsupervised Learning & Metrics
        saveConceptIfAbsent(cr, Concept.create(
                15001L,
                mlTopic.getId(),
                mlTopic.getName(),
                "Supervised vs Unsupervised Learning & Metrics",
                "The core learning paradigms of machine learning and their statistical validation metrics.\n\n"
                        + "### Why This Matters\n"
                        + "Understanding the distinction between labeled prediction (supervised) and latent pattern discovery (unsupervised), along with precision/recall tradeoffs, is foundational for all AI/ML engineering.\n\n"
                        + "### The Idea\n"
                        + "**Supervised Learning** trains models on labeled input-output pairs (X, Y) to learn a predictive mapping function (e.g. classification, regression).\n\n"
                        + "**Unsupervised Learning** discovers hidden structures or groupings in unlabeled data (X) without target output guidance (e.g. K-Means clustering, PCA dimensionality reduction).\n\n"
                        + "### Real Example\n"
                        + "Supervised: Training a spam filter on 100,000 emails labeled 'SPAM' or 'NOT SPAM'. Unsupervised: Segmenting 1,000,000 shoppers into shopping affinity groups without prior labels.",
                List.of(
                        "Supervised tasks: Classification (discrete categorical outputs) and Regression (continuous numeric outputs).",
                        "Unsupervised tasks: Clustering, Dimensionality Reduction, Anomaly Detection.",
                        "Accuracy = (TP + TN) / Total. Misleading when classes are heavily imbalanced (e.g. fraud detection).",
                        "Precision = TP / (TP + FP) (quality of positive predictions); Recall = TP / (TP + FN) (coverage of actual positives)."
                ),
                "// Mathematical formulations:\n// Accuracy = (TP + TN) / (TP + TN + FP + FN)\n// Precision = TP / (TP + FP)\n// Recall = TP / (TP + FN)\n// F1-Score = 2 * (Precision * Recall) / (Precision + Recall)",
                List.of(
                        LearningResource.create("Russell & Norvig: Artificial Intelligence: A Modern Approach", ResourceType.ARTICLE, "https://aima.cs.berkeley.edu/", "Canonical university AI reference text.")
                )
        ));
        seedRemember(rr, cr, 15001L, RememberItemType.KEY_FACT, "Accuracy is deceptive on imbalanced datasets; use Precision, Recall, and F1-Score instead.", 1);
    }

    // --------------------------------------------------------------------------
    // 16. Mathematics for Computer Science
    // --------------------------------------------------------------------------
    private void seedMathematicsForCs(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic mathTopic = findOrCreateTopic(ts, "mathematics-for-cs", "Mathematical Logic & Recurrences", "math-logic-proofs",
                "Inductive proofs, propositional logic, recurrence relations, and the Master Theorem.", 1);

        // Concept 16001: Recurrence Relations & The Master Theorem
        saveConceptIfAbsent(cr, Concept.create(
                16001L,
                mathTopic.getId(),
                mathTopic.getName(),
                "Recurrence Relations & The Master Theorem",
                "Mathematical framework for characterizing the asymptotic time complexity of divide-and-conquer algorithms.\n\n"
                        + "### Why This Matters\n"
                        + "Divide-and-conquer algorithms (Merge Sort, Binary Search, Strassen's Matrix Multiplication) express runtime recursively. The Master Theorem solves these recurrence relations instantly without expanding infinite recursion trees.\n\n"
                        + "### The Idea\n"
                        + "For recurrences of the form T(n) = a * T(n / b) + O(n^d), where a >= 1 is subproblems, b > 1 is division factor, and n^d is combination work:\n"
                        + "1. If d < log_b(a), T(n) = Theta(n^(log_b a)) [Tree leaf work dominates]\n"
                        + "2. If d = log_b(a), T(n) = Theta(n^d * log n) [Work balanced across levels]\n"
                        + "3. If d > log_b(a), T(n) = Theta(n^d) [Root partition work dominates]\n\n"
                        + "### Real Example\n"
                        + "Merge Sort: splits into 2 halves (a=2, b=2) and merges in O(n) linear time (d=1). Here log_2(2) = 1 == d, yielding Case 2: Theta(n log n).",
                List.of(
                        "Master Theorem applies to recurrences of the form T(n) = a*T(n/b) + f(n).",
                        "Case 1 (Leaf-heavy): Subproblem proliferation dominates runtime.",
                        "Case 2 (Balanced): Work is evenly distributed across recursion tree levels (e.g. Merge Sort O(n log n)).",
                        "Case 3 (Root-heavy): Splitting/combining work at the top level dominates."
                ),
                "// Master Theorem Cases Summary:\n// Case 1: log_b(a) > d  =>  T(n) = Theta(n^(log_b(a)))\n// Case 2: log_b(a) == d =>  T(n) = Theta(n^d * log(n))   [e.g. Merge Sort]\n// Case 3: log_b(a) < d  =>  T(n) = Theta(n^d)",
                List.of(
                        LearningResource.create("MIT 6.042J: Mathematics for Computer Science", ResourceType.ARTICLE, "https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-fall-2010/", "MIT OpenCourseWare discrete mathematics and recurrence relations.")
                )
        ));
        seedRemember(rr, cr, 16001L, RememberItemType.FORMULA, "T(n) = a*T(n/b) + O(n^d); if log_b(a) == d, then T(n) = Theta(n^d * log n).", 1);
    }

    // --------------------------------------------------------------------------
    // 17. HCI & Accessibility
    // --------------------------------------------------------------------------
    private void seedHciAndAccessibility(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic hciTopic = findOrCreateTopic(ts, "hci-accessibility", "Usability & Web Accessibility", "hci-usability",
                "Nielsen's 10 usability heuristics, cognitive load principles, WCAG 2.2 standards, and semantic ARIA.", 1);

        // Concept 17001: Nielsen's 10 Usability Heuristics & WCAG
        saveConceptIfAbsent(cr, Concept.create(
                17001L,
                hciTopic.getId(),
                hciTopic.getName(),
                "Nielsen's 10 Usability Heuristics & WCAG Standards",
                "Foundational principles of user interface evaluation and web accessibility for inclusive digital software.\n\n"
                        + "### Why This Matters\n"
                        + "Great software engineers don't just write functional code; they build intuitive, accessible interfaces that minimize cognitive user friction and comply with international accessibility laws.\n\n"
                        + "### The Idea\n"
                        + "Jakob Nielsen's 10 heuristics include: Visibility of System Status (feedback loops), Match Between System and Real World, User Control & Freedom (undo/redo), Consistency & Standards, and Error Prevention.\n\n"
                        + "The W3C Web Content Accessibility Guidelines (WCAG 2.2) mandate the **POUR** framework: Perceivable, Operable, Understandable, and Robust.\n\n"
                        + "### Real Example\n"
                        + "Showing a progress bar during file uploads (Visibility of System Status) and ensuring color contrast ratio >= 4.5:1 for visually impaired users.",
                List.of(
                        "Visibility of system status: the system should always keep users informed through prompt visual feedback.",
                        "Error prevention is vastly superior to good error messages.",
                        "WCAG 2.2 core framework: Perceivable, Operable, Understandable, Robust (POUR).",
                        "Always use semantic HTML elements (<button>, <nav>, <header>) before resorting to generic <div> elements with ARIA roles."
                ),
                "<!-- Semantic, accessible HTML markup in ByteForce -->\n<button type=\"button\" class=\"btn btn-primary\" aria-label=\"Submit assessment answers\">\n    <span>Submit Assessment</span>\n</button>",
                List.of(
                        LearningResource.create("W3C Web Content Accessibility Guidelines (WCAG 2.2)", ResourceType.ARTICLE, "https://www.w3.org/TR/WCAG22/", "Official W3C web accessibility technical standard.")
                )
        ));
        seedRemember(rr, cr, 17001L, RememberItemType.KEY_FACT, "WCAG POUR: Perceivable, Operable, Understandable, Robust; Minimum contrast ratio for normal text is 4.5:1 (AA level).", 1);
    }

    // --------------------------------------------------------------------------
    // 18. Ethics, Privacy & Professional Practice
    // --------------------------------------------------------------------------
    private void seedEthicsAndPrivacy(TopicService ts, ConceptRepository cr, RememberItemRepository rr, ConceptRelationshipRepository crel) {
        Topic ethicsTopic = findOrCreateTopic(ts, "ethics-privacy", "Open Source Licensing & Data Privacy", "ethics-licensing-ip",
                "Permissive vs copyleft open source licenses, GDPR principles, and the ACM Code of Ethics.", 1);

        // Concept 18001: Open Source Software Licensing: MIT, Apache & GPL
        saveConceptIfAbsent(cr, Concept.create(
                18001L,
                ethicsTopic.getId(),
                ethicsTopic.getName(),
                "Open Source Software Licensing: MIT, Apache & GPL",
                "Legal frameworks governing the rights, permissions, and obligations of distributing, modifying, and using software code.\n\n"
                        + "### Why This Matters\n"
                        + "Incorporating an open-source library without understanding its license can legally compel a commercial company to release its entire proprietary codebase (GPL Copyleft effect) or expose them to copyright infringement lawsuits.\n\n"
                        + "### The Idea\n"
                        + "- **Permissive Licenses (MIT, BSD, Apache 2.0)**: Allow anyone to use, modify, and distribute code in closed-source proprietary software with minimal restrictions (primarily preserving copyright notices; Apache 2.0 adds explicit patent protection).\n"
                        + "- **Copyleft Licenses (GPL v2, GPL v3, AGPL)**: Require that any derivative work or distributed application incorporating the code must also be released under the identical open-source license.\n\n"
                        + "### Real Example\n"
                        + "The Linux kernel is licensed under GPL v2. Spring Boot and React are licensed under permissive licenses (Apache 2.0 and MIT respectively), enabling commercial companies to build proprietary products on top of them without releasing application source code.",
                List.of(
                        "MIT: Highly permissive; allows proprietary closed-source redistribution with copyright preservation.",
                        "Apache 2.0: Permissive license with explicit patent rights grants from contributors.",
                        "GPL v3 (Strong Copyleft): Requires all derivative works to be released under GPL v3.",
                        "AGPL (Affero GPL): Extends copyleft obligations to network software accessed over a network/cloud API."
                ),
                "// Open Source License Distinction:\n// Permissive:  [MIT] / [Apache 2.0]  --> Commercial Closed-Source Allowed\n// Copyleft:    [GPL v3] / [AGPL]      --> Derivative Works Must Be Open Source",
                List.of(
                        LearningResource.create("Open Source Initiative (OSI) Approved Licenses", ResourceType.ARTICLE, "https://opensource.org/licenses/", "Official open source license definitions."),
                        LearningResource.create("ACM Code of Ethics and Professional Conduct", ResourceType.ARTICLE, "https://www.acm.org/code-of-ethics", "Official ACM professional code.")
                )
        ));
        seedRemember(rr, cr, 18001L, RememberItemType.KEY_FACT, "MIT and Apache 2.0 are permissive licenses (allow closed source); GPL is copyleft (forces derivative works to be open source).", 1);
    }

    // --------------------------------------------------------------------------
    // Helper Seeding Methods
    // --------------------------------------------------------------------------
    private Topic findOrCreateTopic(TopicService ts, String subjectId, String name, String slug, String description, int order) {
        return ts.getTopicBySlug(slug)
                .orElseGet(() -> ts.createTopic(subjectId, name, slug, description, order));
    }

    private void saveConceptIfAbsent(ConceptRepository cr, Concept concept) {
        if (!cr.existsById(concept.getId())) {
            cr.save(concept);
            log.info("Seeded concept ID {}: {}", concept.getId(), concept.getTitle());
        }
    }

    private void saveOrUpdateConcept(ConceptRepository cr, Concept concept) {
        cr.save(concept);
        log.info("Saved/updated concept ID {}: {}", concept.getId(), concept.getTitle());
    }

    private void seedRemember(RememberItemRepository rr, ConceptRepository cr, long conceptId, RememberItemType type, String content, int order) {
        if (!cr.existsById(conceptId)) return;
        List<RememberItem> existing = rr.findByConceptId(conceptId);
        boolean exists = existing.stream().anyMatch(i -> i.getType() == type && i.getContent().equalsIgnoreCase(content.trim()));
        if (!exists) {
            rr.save(RememberItem.create(conceptId, type, content, order));
        }
    }

    private void seedRel(ConceptRelationshipRepository crel, ConceptRepository cr, long source, long target, ConceptRelationshipType type, String desc, int order) {
        if (!cr.existsById(source) || !cr.existsById(target)) return;
        List<ConceptRelationship> existing = crel.findBySourceConceptId(source);
        boolean exists = existing.stream().anyMatch(r -> r.getTargetConceptId() == target && r.getRelationshipType() == type);
        if (!exists) {
            crel.save(ConceptRelationship.create(source, target, type, desc, order));
        }
    }
}
