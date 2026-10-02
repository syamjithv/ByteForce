package com.byteforce.web.config;

import com.byteforce.domain.Concept;
import com.byteforce.domain.ConceptRelationship;
import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.domain.RememberItem;
import com.byteforce.domain.RememberItemType;
import com.byteforce.domain.ResourceType;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.repository.ConceptRelationshipRepository;
import com.byteforce.repository.ConceptRepository;
import com.byteforce.repository.RememberItemRepository;
import com.byteforce.repository.SubjectRepository;
import com.byteforce.service.AssessmentService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Initializes default placement preparation subjects, topics, concepts, questions, mock assessments,
 * Remember quick-revision items, and Brain Map concept relationships on application startup if missing from the database.
 * This ensures a fully functional, database-backed Learn, Practice, Remember, and Brain Map experience.
 */
@Component
public class ByteForceDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ByteForceDataInitializer.class);

    private final TopicService topicService;
    private final QuestionService questionService;
    private final AssessmentService assessmentService;
    private final SubjectRepository subjectRepository;
    private final ConceptRepository conceptRepository;
    private final RememberItemRepository rememberItemRepository;
    private final ConceptRelationshipRepository conceptRelationshipRepository;
    private final com.byteforce.repository.UserRepository userRepository;
    private final com.byteforce.security.PasswordHasher passwordHasher;
    private final com.byteforce.repository.AptitudeQuestionRepository aptitudeQuestionRepository;

    public ByteForceDataInitializer(TopicService topicService,
                                    QuestionService questionService,
                                    AssessmentService assessmentService,
                                    SubjectRepository subjectRepository,
                                    ConceptRepository conceptRepository,
                                    RememberItemRepository rememberItemRepository,
                                    ConceptRelationshipRepository conceptRelationshipRepository,
                                    com.byteforce.repository.UserRepository userRepository,
                                    com.byteforce.security.PasswordHasher passwordHasher,
                                    com.byteforce.repository.AptitudeQuestionRepository aptitudeQuestionRepository) {
        this.topicService = topicService;
        this.questionService = questionService;
        this.assessmentService = assessmentService;
        this.subjectRepository = subjectRepository;
        this.conceptRepository = conceptRepository;
        this.rememberItemRepository = rememberItemRepository;
        this.conceptRelationshipRepository = conceptRelationshipRepository;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.aptitudeQuestionRepository = aptitudeQuestionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedAdminUser();
            seedSubjects();

            List<Topic> existingTopics = topicService.getAllTopics();
            if (existingTopics.isEmpty()) {
                log.info("No topics found in database. Seeding initial ByteForce placement curriculum...");
                seedPlacementContent();
            } else {
                log.info("Database contains {} topics. Ensuring subject links...", existingTopics.size());
                linkExistingTopicsToSubjects();
            }

            seedConceptsAndResources();
            seedRememberItems();
            seedConceptRelationships();
            seedAptitudeQuestions();
            log.info("ByteForce data initialization completed successfully.");
        } catch (Exception e) {
            log.warn("Notice: Data initializer completed with note: {}", e.getMessage(), e);
        }
    }

    private void seedAdminUser() {
        if (userRepository.findByEmail("admin@byteforce.com").isEmpty()) {
            com.byteforce.domain.User admin = com.byteforce.domain.User.create(
                    "admin@byteforce.com",
                    passwordHasher.hash("adminPassword123!"),
                    "ByteForce Administrator",
                    com.byteforce.domain.Role.ADMIN
            );
            userRepository.save(admin);
            log.info("Seeded default administrator user: admin@byteforce.com");
        }
    }

    private void seedAptitudeQuestions() {
        if (aptitudeQuestionRepository.count() == 0) {
            log.info("Seeding initial placement aptitude questions...");
            aptitudeQuestionRepository.save(com.byteforce.domain.AptitudeQuestion.create(
                    com.byteforce.domain.AptitudeCategory.QUANTITATIVE,
                    "Time & Work",
                    Difficulty.EASY,
                    "A can complete a piece of work in 12 days and B can complete the same work in 24 days. If they work together, in how many days will the work be completed?",
                    "6 days",
                    "8 days",
                    "10 days",
                    "12 days",
                    "B",
                    "1/A + 1/B = 1/12 + 1/24 = 3/24 = 1/8. Working together, they will finish the work in 8 days."
            ));
            aptitudeQuestionRepository.save(com.byteforce.domain.AptitudeQuestion.create(
                    com.byteforce.domain.AptitudeCategory.QUANTITATIVE,
                    "Percentages",
                    Difficulty.MEDIUM,
                    "If the price of a commodity increases by 25%, by what percentage must consumption be reduced so that the expenditure remains constant?",
                    "20%",
                    "25%",
                    "16.67%",
                    "33.33%",
                    "A",
                    "Reduction percentage = (R / (100 + R)) * 100 = (25 / 125) * 100 = 20%."
            ));
            aptitudeQuestionRepository.save(com.byteforce.domain.AptitudeQuestion.create(
                    com.byteforce.domain.AptitudeCategory.LOGICAL,
                    "Number Series",
                    Difficulty.EASY,
                    "Find the next number in the sequence: 2, 6, 12, 20, 30, ?",
                    "40",
                    "42",
                    "44",
                    "46",
                    "B",
                    "Differences between consecutive numbers are 4, 6, 8, 10, ... So the next difference is 12: 30 + 12 = 42."
            ));
            aptitudeQuestionRepository.save(com.byteforce.domain.AptitudeQuestion.create(
                    com.byteforce.domain.AptitudeCategory.LOGICAL,
                    "Blood Relations",
                    Difficulty.MEDIUM,
                    "Pointing to a photograph of a boy, Suresh said, 'He is the son of the only son of my mother.' How is Suresh related to that boy?",
                    "Brother",
                    "Uncle",
                    "Father",
                    "Grandfather",
                    "C",
                    "Mother's only son is Suresh himself. Therefore, the boy is Suresh's son, making Suresh the father."
            ));
            aptitudeQuestionRepository.save(com.byteforce.domain.AptitudeQuestion.create(
                    com.byteforce.domain.AptitudeCategory.VERBAL,
                    "Sentence Correction",
                    Difficulty.EASY,
                    "Choose the correct sentence: 'Neither of the candidates ______ qualified.'",
                    "is",
                    "are",
                    "were",
                    "have been",
                    "A",
                    "'Neither' takes a singular verb when referring to individual entities. Hence, 'Neither of the candidates is qualified.'"
            ));
            log.info("Seeded initial placement aptitude questions.");
        }
    }

    private void seedSubjects() {
        List<Subject> subjects = List.of(
                new Subject("data-structures", "Data Structures & Algorithms",
                        "Fundamental memory representations, algorithmic complexity, and dynamic collections.", 1, List.of()),
                new Subject("dbms", "Database Management Systems",
                        "Relational schema architecture, normalization, SQL querying, and transaction ACID properties.", 2, List.of()),
                new Subject("operating-systems", "Operating Systems",
                        "Process lifecycle, CPU scheduling, virtual memory, threads, synchronization, and deadlocks.", 3, List.of()),
                new Subject("computer-networks", "Computer Networks",
                        "OSI & TCP/IP layered architecture, routing protocols, HTTP/HTTPS, and socket communication.", 4, List.of()),
                new Subject("computer-organization", "Computer Organization & Architecture",
                        "Instruction set architectures, CPU pipelining, cache hierarchy, memory management, and I/O systems.", 5, List.of()),
                new Subject("software-engineering", "Software Engineering & System Design",
                        "Design patterns, agile development, architectural styles, API design, and distributed systems fundamentals.", 6, List.of()),
                new Subject("aptitude", "Quantitative & Logical Aptitude",
                        "Aptitude problem solving, logical reasoning, numerical ability, and placement interview speed math.", 7, List.of())
        );

        for (Subject s : subjects) {
            if (!subjectRepository.existsById(s.getId())) {
                subjectRepository.save(s);
                log.info("Seeded subject: {} ({})", s.getName(), s.getId());
            }
        }
    }

    private void linkExistingTopicsToSubjects() {
        List<Topic> topics = topicService.getAllTopics();
        for (Topic t : topics) {
            if (t.getSubjectId() == null || t.getSubjectId().isBlank()) {
                String subjectId = determineSubjectForTopic(t.getSlug());
                topicService.updateTopic(t.getId(), subjectId, t.getName(), t.getSlug(), t.getDescription(), t.getDisplayOrder());
                log.info("Linked existing topic '{}' to subject '{}'", t.getName(), subjectId);
            }
        }
    }

    private String determineSubjectForTopic(String slug) {
        if (slug == null) return "data-structures";
        String s = slug.toLowerCase();
        if (s.contains("sql") || s.contains("data") && s.contains("base") || s.contains("db")) {
            return "dbms";
        }
        if (s.contains("os") || s.contains("process") || s.contains("thread") || s.contains("concur")) {
            return "operating-systems";
        }
        if (s.contains("oop") || s.contains("java") || s.contains("design") || s.contains("arch")) {
            return "software-engineering";
        }
        if (s.contains("network")) {
            return "computer-networks";
        }
        if (s.contains("aptitude") || s.contains("quant") || s.contains("logic")) {
            return "aptitude";
        }
        return "data-structures";
    }

    private void seedPlacementContent() {
        // 1. Create Topics with Subject relationships
        Topic dsaTopic = topicService.createTopic("data-structures", "Data Structures & Algorithms", "dsa",
                "Core algorithms, array manipulations, searches, and asymptotic complexity analysis.", 1);
        Topic arraysTopic = topicService.createTopic("data-structures", "Arrays & Two Pointers", "arrays-two-pointers",
                "Contiguous memory layouts, sliding windows, prefix sums, and two-pointer patterns.", 2);
        Topic sqlTopic = topicService.createTopic("dbms", "SQL & Database Queries", "sql-databases",
                "Relational schema queries, complex joins, aggregation, grouping, and subqueries.", 3);
        Topic oopTopic = topicService.createTopic("software-engineering", "OOP & Java Concepts", "oop-java",
                "Object-oriented design, inheritance, polymorphism, encapsulation, collections, and JVM fundamentals.", 4);
        Topic osTopic = topicService.createTopic("operating-systems", "Operating Systems & Concurrency", "os-concurrency",
                "Processes, threads, synchronization, deadlocks, virtual memory, and CPU scheduling.", 5);

        // 2. Create Questions
        Question q1 = questionService.createQuestion(
                arraysTopic.getId(),
                "Two Sum — Pair with Target Sum",
                "two-sum-pair",
                "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.\n\n"
                        + "Example:\nInput: nums = [2, 7, 11, 15], target = 9\nOutput: [0, 1]\nExplanation: nums[0] + nums[1] == 9, return [0, 1].",
                Difficulty.EASY,
                QuestionType.CODING,
                "public int[] twoSum(int[] nums, int target) {\n    Map<Integer, Integer> map = new HashMap<>();\n    for (int i = 0; i < nums.length; i++) {\n        int complement = target - nums[i];\n        if (map.containsKey(complement)) {\n            return new int[] { map.get(complement), i };\n        }\n        map.put(nums[i], i);\n    }\n    throw new IllegalArgumentException(\"No two sum solution\");\n}"
        );

        Question q2 = questionService.createQuestion(
                arraysTopic.getId(),
                "Best Time to Buy and Sell Stock",
                "buy-sell-stock",
                "You are given an array prices where prices[i] is the price of a given stock on the ith day.\n"
                        + "You want to maximize your profit by choosing a single day to buy one stock and choosing a different day in the future to sell that stock.\n\n"
                        + "Return the maximum profit you can achieve from this transaction. If you cannot achieve any profit, return 0.",
                Difficulty.EASY,
                QuestionType.CODING,
                "public int maxProfit(int[] prices) {\n    int minPrice = Integer.MAX_VALUE;\n    int maxProfit = 0;\n    for (int price : prices) {\n        if (price < minPrice) {\n            minPrice = price;\n        } else if (price - minPrice > maxProfit) {\n            maxProfit = price - minPrice;\n        }\n    }\n    return maxProfit;\n}"
        );

        Question q3 = questionService.createQuestion(
                dsaTopic.getId(),
                "Time Complexity of Binary Search",
                "binary-search-complexity",
                "What is the worst-case and average-case time complexity of standard Binary Search on a sorted array of size n?\n\n"
                        + "A) O(1)\nB) O(log n)\nC) O(n)\nD) O(n log n)",
                Difficulty.EASY,
                QuestionType.MCQ,
                "B) O(log n). At each step, Binary Search divides the search space in half. Hence, the maximum number of comparisons is log2(n)."
        );

        Question q4 = questionService.createQuestion(
                sqlTopic.getId(),
                "Second Highest Salary",
                "second-highest-salary",
                "Write an SQL query to report the second highest salary from the Employee table. If there is no second highest salary, return null.\n\n"
                        + "Schema:\nEmployee(id INT, salary INT)",
                Difficulty.MEDIUM,
                QuestionType.SQL,
                "SELECT MAX(salary) AS SecondHighestSalary FROM Employee WHERE salary < (SELECT MAX(salary) FROM Employee);"
        );

        Question q5 = questionService.createQuestion(
                oopTopic.getId(),
                "Polymorphism in Object-Oriented Programming",
                "oop-polymorphism",
                "Explain the difference between compile-time (static) polymorphism and runtime (dynamic) polymorphism in Java. Provide standard examples of each.",
                Difficulty.EASY,
                QuestionType.CONCEPTUAL,
                "Compile-time polymorphism is achieved through Method Overloading (same method name, different parameter signature in the same class). It is resolved at compile time.\n\nRuntime polymorphism is achieved through Method Overriding (subclass provides specific implementation of a parent class method with the exact same signature). It is resolved dynamically at runtime using virtual method invocation (vtable)."
        );

        Question q6 = questionService.createQuestion(
                osTopic.getId(),
                "Deadlock Necessary Conditions (Coffman Conditions)",
                "os-deadlock-conditions",
                "Which of the following is NOT one of the four Coffman conditions required for a deadlock to occur?\n\n"
                        + "A) Mutual Exclusion\nB) Hold and Wait\nC) Preemption\nD) Circular Wait",
                Difficulty.MEDIUM,
                QuestionType.MCQ,
                "C) Preemption. The actual condition is 'No Preemption' (resources cannot be forcibly taken away from a process holding them; they must be released voluntarily)."
        );

        Question q7 = questionService.createQuestion(
                dsaTopic.getId(),
                "Valid Parentheses Matching",
                "valid-parentheses",
                "Given a string s containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid.\n\n"
                        + "An input string is valid if open brackets are closed by the same type of brackets in the correct order.",
                Difficulty.EASY,
                QuestionType.CODING,
                "public boolean isValid(String s) {\n    Deque<Character> stack = new ArrayDeque<>();\n    for (char c : s.toCharArray()) {\n        if (c == '(') stack.push(')');\n        else if (c == '{') stack.push('}');\n        else if (c == '[') stack.push(']');\n        else if (stack.isEmpty() || stack.pop() != c) return false;\n    }\n    return stack.isEmpty();\n}"
        );

        // 3. Create Sample Assessments
        var assessment1 = assessmentService.createAssessment(
                "Technical Placement Readiness Mock Test 1",
                "Comprehensive diagnostic assessment covering core Data Structures, SQL queries, OOP principles, and Operating Systems fundamentals.",
                45,
                50,
                Difficulty.MEDIUM,
                dsaTopic.getId()
        );

        assessmentService.addQuestionToAssessment(assessment1.getId(), q1.getId(), 10, 1);
        assessmentService.addQuestionToAssessment(assessment1.getId(), q2.getId(), 10, 2);
        assessmentService.addQuestionToAssessment(assessment1.getId(), q3.getId(), 5, 3);
        assessmentService.addQuestionToAssessment(assessment1.getId(), q4.getId(), 15, 4);
        assessmentService.addQuestionToAssessment(assessment1.getId(), q5.getId(), 10, 5);

        var assessment2 = assessmentService.createAssessment(
                "CS Fundamentals Speed Assessment",
                "Quick 30-minute evaluation focusing on algorithmic complexity, operating system concepts, and foundational programming knowledge.",
                30,
                30,
                Difficulty.EASY,
                osTopic.getId()
        );

        assessmentService.addQuestionToAssessment(assessment2.getId(), q3.getId(), 10, 1);
        assessmentService.addQuestionToAssessment(assessment2.getId(), q6.getId(), 10, 2);
        assessmentService.addQuestionToAssessment(assessment2.getId(), q7.getId(), 10, 3);

        log.info("Placement seed data created successfully: 5 topics, 7 questions, 2 mock assessments.");
    }

    private void seedConceptsAndResources() {
        // Find or create appropriate topics for concepts
        Topic arraysTopic = findOrCreateTopic("data-structures", "Arrays & Two Pointers", "arrays-two-pointers",
                "Contiguous memory layouts, sliding windows, prefix sums, and two-pointer patterns.", 1);

        Topic linkedListsTopic = findOrCreateTopic("data-structures", "Linked Lists", "linked-lists",
                "Node-based dynamic pointer structures, list reversals, and cycle detection.", 2);

        Topic sqlTopic = findOrCreateTopic("dbms", "SQL & Database Queries", "sql-databases",
                "Relational schema queries, complex joins, aggregation, grouping, and subqueries.", 1);

        Topic osTopic = findOrCreateTopic("operating-systems", "Operating Systems & Concurrency", "os-concurrency",
                "Processes, threads, synchronization, deadlocks, virtual memory, and CPU scheduling.", 1);

        // 1. Array Traversal (id 1001)
        if (!conceptRepository.existsById(1001L)) {
            conceptRepository.save(Concept.create(
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
            ));
        }

        // 2. Prefix Sum (id 1002)
        if (!conceptRepository.existsById(1002L)) {
            conceptRepository.save(Concept.create(
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
            ));
        }

        // 3. Singly Linked List (id 1003)
        if (!conceptRepository.existsById(1003L)) {
            conceptRepository.save(Concept.create(
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
            ));
        }

        // 4. Doubly Linked List (id 1004)
        if (!conceptRepository.existsById(1004L)) {
            conceptRepository.save(Concept.create(
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
            ));
        }

        // 5. SELECT Statement (id 2001)
        if (!conceptRepository.existsById(2001L)) {
            conceptRepository.save(Concept.create(
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
            ));
        }

        // 6. JOIN Operations (id 2002)
        if (!conceptRepository.existsById(2002L)) {
            conceptRepository.save(Concept.create(
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
            ));
        }

        // 7. GROUP BY Clause (id 2003)
        if (!conceptRepository.existsById(2003L)) {
            conceptRepository.save(Concept.create(
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
            ));
        }

        // 8. Process States (id 3001)
        if (!conceptRepository.existsById(3001L)) {
            conceptRepository.save(Concept.create(
                    3001L,
                    osTopic.getId(),
                    osTopic.getName(),
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
            ));
        }

        // 9. Context Switching (id 3002)
        if (!conceptRepository.existsById(3002L)) {
            conceptRepository.save(Concept.create(
                    3002L,
                    osTopic.getId(),
                    osTopic.getName(),
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
            ));
        }

        // 10. CPU Scheduling (id 3003)
        if (!conceptRepository.existsById(3003L)) {
            conceptRepository.save(Concept.create(
                    3003L,
                    osTopic.getId(),
                    osTopic.getName(),
                    "CPU Scheduling",
                    "OS algorithms allocating CPU execution time among processes in the ready queue.",
                    List.of(
                            "Non-preemptive algorithms (FCFS, SJF) allow a process to run until it finishes or waits for I/O.",
                            "Preemptive algorithms (Round Robin, SRTF) allocate CPU bursts based on time quanta or priority.",
                            "Key scheduling metrics: Turnaround Time, Waiting Time, Response Time, and CPU Throughput.",
                            "Convoy effect in FCFS occurs when short processes queue behind a long CPU-bound process."
                    ),
                    "// Round Robin Scheduling simulation\nint timeQuantum = 4;\nQueue<Process> readyQueue = new LinkedList<>();\n// Process executes up to quantum before preemption",
                    List.of(
                            LearningResource.create("CPU Scheduling Basics", ResourceType.ARTICLE, "https://en.wikipedia.org/wiki/Scheduling_(computing)", "Comprehensive overview of scheduling criteria and algorithms."),
                            LearningResource.create("Scheduling Visualizer", ResourceType.VIDEO, "https://www.youtube.com/watch?v=cpu-scheduling", "Gantt charts and turnaround calculation animations.")
                    )
            ));
        }

        // 11. Threads & Concurrency (id 3004)
        if (!conceptRepository.existsById(3004L)) {
            conceptRepository.save(Concept.create(
                    3004L,
                    osTopic.getId(),
                    osTopic.getName(),
                    "Threads & Concurrency",
                    "Lightweight execution contexts within a single process sharing address space and open files.",
                    List.of(
                            "Threads in the same process share code, data, and OS resources, but each retains its own registers and stack.",
                            "Thread context switching has significantly lower overhead than full process context switching.",
                            "User threads are scheduled by runtime libraries; kernel threads are managed by the OS scheduler.",
                            "Concurrent access to shared memory requires synchronization mechanisms like mutexes and semaphores."
                    ),
                    "// Java Thread execution\nThread thread = new Thread(() -> {\n    System.out.println(\"Running concurrently in thread: \" + Thread.currentThread().getName());\n});\nthread.start();",
                    List.of(
                            LearningResource.create("Processes vs Threads", ResourceType.ARTICLE, "https://en.wikipedia.org/wiki/Thread_(computing)", "Architectural breakdown of multithreading and thread models."),
                            LearningResource.create("Concurrency Cheatsheet", ResourceType.PDF, "https://byteforce.dev/resources/concurrency.pdf", "Placement guide on race conditions and thread safety.")
                    )
            ));
        }
    }

    private void seedRememberItems() {
        // 1001: Array Traversal
        seedRememberItemIfAbsent(1001L, RememberItemType.KEY_FACT,
                "Array index access is O(1) direct memory calculation via base address + (index * element_size).", 1);
        seedRememberItemIfAbsent(1001L, RememberItemType.COMMON_CONFUSION,
                "Arrays are fixed-size in contiguous memory; dynamic resizing (like ArrayList) incurs O(n) reallocation overhead.", 2);
        seedRememberItemIfAbsent(1001L, RememberItemType.INTERVIEW_REMINDER,
                "Always check edge cases: empty array (length == 0), single-element array, and off-by-one boundary indices.", 3);

        // 1002: Prefix Sum
        seedRememberItemIfAbsent(1002L, RememberItemType.FORMULA,
                "Range sum query: sum(L, R) = prefix[R] - prefix[L - 1] (when L > 0, else prefix[R]).", 1);
        seedRememberItemIfAbsent(1002L, RememberItemType.KEY_FACT,
                "Reduces repeated subarray sum queries from O(N) to O(1) after O(N) preprocessing.", 2);
        seedRememberItemIfAbsent(1002L, RememberItemType.MEMORY_TRICK,
                "Think of prefix sum like an odometer reading — subtract start reading from end reading to find distance traveled.", 3);

        // 1003: Singly Linked List
        seedRememberItemIfAbsent(1003L, RememberItemType.KEY_FACT,
                "Insertion and deletion at the head are O(1); access by index is O(N).", 1);
        seedRememberItemIfAbsent(1003L, RememberItemType.COMMON_CONFUSION,
                "Inserting after a given node is O(1), but finding that node in a singly linked list takes O(N) traversal.", 2);
        seedRememberItemIfAbsent(1003L, RememberItemType.INTERVIEW_REMINDER,
                "Use a dummy head pointer (sentinel node) to eliminate edge case checks when modifying the list head.", 3);

        // 1004: Doubly Linked List
        seedRememberItemIfAbsent(1004L, RememberItemType.KEY_FACT,
                "Each node contains references to both next and prev nodes, enabling O(1) bidirectional deletion.", 1);
        seedRememberItemIfAbsent(1004L, RememberItemType.MEMORY_TRICK,
                "Doubly linked list = Two pointers per node; foundation for LRU Cache implementations with HashMaps.", 2);

        // 2001: SELECT Statement
        seedRememberItemIfAbsent(2001L, RememberItemType.KEY_FACT,
                "Logical SQL execution order starts at FROM/JOIN, then WHERE, GROUP BY, HAVING, and finally SELECT.", 1);
        seedRememberItemIfAbsent(2001L, RememberItemType.COMMON_CONFUSION,
                "WHERE filters rows BEFORE aggregation; HAVING filters groups AFTER aggregation.", 2);
        seedRememberItemIfAbsent(2001L, RememberItemType.INTERVIEW_REMINDER,
                "Avoid 'SELECT *' in production queries to minimize network payload and leverage covering indexes.", 3);

        // 2002: JOIN Operations
        seedRememberItemIfAbsent(2002L, RememberItemType.KEY_FACT,
                "INNER JOIN returns matching rows only; LEFT JOIN retains all rows from the left table with NULLs for unmatched right rows.", 1);
        seedRememberItemIfAbsent(2002L, RememberItemType.COMMON_CONFUSION,
                "CROSS JOIN produces a Cartesian product (M * N rows), which can cause massive memory consumption.", 2);

        // 2003: GROUP BY Clause
        seedRememberItemIfAbsent(2003L, RememberItemType.KEY_FACT,
                "Every non-aggregated column in the SELECT list must appear in the GROUP BY clause.", 1);
        seedRememberItemIfAbsent(2003L, RememberItemType.FORMULA,
                "SQL Execution Order: FROM → WHERE → GROUP BY → HAVING → SELECT → DISTINCT → ORDER BY → LIMIT.", 2);

        // 3001: Process States
        seedRememberItemIfAbsent(3001L, RememberItemType.KEY_FACT,
                "The 5 standard states are NEW, READY, RUNNING, WAITING (BLOCKED), and TERMINATED.", 1);
        seedRememberItemIfAbsent(3001L, RememberItemType.COMMON_CONFUSION,
                "A process goes from RUNNING to READY when preempted (time quantum), but to WAITING when awaiting I/O.", 2);
        seedRememberItemIfAbsent(3001L, RememberItemType.INTERVIEW_REMINDER,
                "A process in the WAITING state cannot directly move to RUNNING; it must transition to READY first when I/O completes.", 3);

        // 3002: Context Switching
        seedRememberItemIfAbsent(3002L, RememberItemType.KEY_FACT,
                "Context switching overhead is pure system time during which no useful user computation occurs.", 1);
        seedRememberItemIfAbsent(3002L, RememberItemType.KEY_FACT,
                "Process Control Block (PCB) preserves Program Counter, CPU registers, and memory management state.", 2);
        seedRememberItemIfAbsent(3002L, RememberItemType.INTERVIEW_REMINDER,
                "Thread context switching is faster than process context switching because threads share the same address space.", 3);

        // 3003: CPU Scheduling
        seedRememberItemIfAbsent(3003L, RememberItemType.KEY_FACT,
                "FCFS is non-preemptive and suffers from the convoy effect.", 1);
        seedRememberItemIfAbsent(3003L, RememberItemType.KEY_FACT,
                "Round Robin is preemptive and relies on a time quantum; too small quantum causes excessive context-switching.", 2);
        seedRememberItemIfAbsent(3003L, RememberItemType.COMMON_CONFUSION,
                "SJF (Shortest Job First) is non-preemptive, whereas SRTF (Shortest Remaining Time First) is preemptive.", 3);
        seedRememberItemIfAbsent(3003L, RememberItemType.INTERVIEW_REMINDER,
                "Be able to explain starvation in priority scheduling and how aging resolves it.", 4);

        // 3004: Threads & Concurrency
        seedRememberItemIfAbsent(3004L, RememberItemType.KEY_FACT,
                "Threads within the same process share heap, global variables, and code, but have independent stacks and registers.", 1);
        seedRememberItemIfAbsent(3004L, RememberItemType.COMMON_CONFUSION,
                "Multithreading on a single CPU core is concurrent via time-slicing, not parallel.", 2);
        seedRememberItemIfAbsent(3004L, RememberItemType.INTERVIEW_REMINDER,
                "Race conditions occur when multiple threads access shared mutable state without synchronization.", 3);
    }

    private void seedRememberItemIfAbsent(long conceptId, RememberItemType type, String content, int order) {
        if (!conceptRepository.existsById(conceptId)) {
            return;
        }
        List<RememberItem> existing = rememberItemRepository.findByConceptId(conceptId);
        boolean alreadyExists = existing.stream().anyMatch(i -> i.getType() == type && i.getContent().equalsIgnoreCase(content.trim()));
        if (!alreadyExists) {
            rememberItemRepository.save(RememberItem.create(conceptId, type, content, order));
        }
    }

    private void seedConceptRelationships() {
        // Data Structures
        seedRelationshipIfAbsent(1001L, 1002L, ConceptRelationshipType.PREREQUISITE,
                "Array indexing is required to build prefix sum arrays", 1);
        seedRelationshipIfAbsent(1001L, 1003L, ConceptRelationshipType.RELATED,
                "Alternative linear sequential data structure representations", 2);
        seedRelationshipIfAbsent(1003L, 1004L, ConceptRelationshipType.PREREQUISITE,
                "Singly linked pointers foundation for bidirectional pointer nodes", 1);
        seedRelationshipIfAbsent(1003L, 1004L, ConceptRelationshipType.COMMONLY_CONFUSED,
                "Node pointer overhead and deletion mechanics differ between singly and doubly linked lists", 2);

        // DBMS
        seedRelationshipIfAbsent(2001L, 2002L, ConceptRelationshipType.PREREQUISITE,
                "Single table projection and filtering precede multi-table joins", 1);
        seedRelationshipIfAbsent(2001L, 2003L, ConceptRelationshipType.PREREQUISITE,
                "SELECT column projection and WHERE clauses precede aggregate grouping", 2);
        seedRelationshipIfAbsent(2002L, 2003L, ConceptRelationshipType.RELATED,
                "Aggregating tuples across joined relational tables", 1);

        // Operating Systems
        seedRelationshipIfAbsent(3001L, 3004L, ConceptRelationshipType.CHILD,
                "Threads are lightweight execution units within a process", 1);
        seedRelationshipIfAbsent(3001L, 3003L, ConceptRelationshipType.PREREQUISITE,
                "Process lifecycle states (Ready, Running, Waiting) are foundation for CPU scheduling", 2);
        seedRelationshipIfAbsent(3003L, 3002L, ConceptRelationshipType.RELATED,
                "CPU schedulers trigger context switches between ready and running states", 1);
        seedRelationshipIfAbsent(3001L, 3002L, ConceptRelationshipType.PREREQUISITE,
                "Understanding process state transitions is required to understand context switching", 3);
    }

    private void seedRelationshipIfAbsent(long source, long target, ConceptRelationshipType type, String desc, int order) {
        if (!conceptRepository.existsById(source) || !conceptRepository.existsById(target)) {
            return;
        }
        if (!conceptRelationshipRepository.exists(source, target, type)) {
            conceptRelationshipRepository.save(ConceptRelationship.create(source, target, type, desc, order));
        }
    }

    private Topic findOrCreateTopic(String subjectId, String name, String slug, String description, int order) {
        Optional<Topic> bySlug = topicService.getTopicBySlug(slug);
        if (bySlug.isPresent()) {
            Topic topic = bySlug.get();
            if (topic.getSubjectId() == null || !topic.getSubjectId().equalsIgnoreCase(subjectId)) {
                return topicService.updateTopic(topic.getId(), subjectId, topic.getName(), topic.getSlug(), topic.getDescription(), topic.getDisplayOrder());
            }
            return topic;
        }

        // Check by partial match (e.g. "arrays" vs "arrays-two-pointers")
        for (Topic t : topicService.getAllTopics()) {
            if (t.getSlug().contains(slug) || slug.contains(t.getSlug())) {
                if (t.getSubjectId() == null || !t.getSubjectId().equalsIgnoreCase(subjectId)) {
                    return topicService.updateTopic(t.getId(), subjectId, t.getName(), t.getSlug(), t.getDescription(), t.getDisplayOrder());
                }
                return t;
            }
        }

        return topicService.createTopic(subjectId, name, slug, description, order);
    }
}
