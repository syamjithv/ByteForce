package com.byteforce.web.controller;

import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.domain.Topic;
import com.byteforce.domain.User;
import com.byteforce.service.AttemptService;
import com.byteforce.service.BookmarkService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.TopicService;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for the Practice module:
 * Topic & Difficulty filtering, interactive question solving,
 * answer submission, bookmarking, and result verification.
 */
@Controller
@RequestMapping("/practice")
public class PracticeController {

    private static final Logger log = LoggerFactory.getLogger(PracticeController.class);

    private final TopicService topicService;
    private final QuestionService questionService;
    private final AttemptService attemptService;
    private final BookmarkService bookmarkService;
    private final com.byteforce.service.MemoryService memoryService;
    private final com.byteforce.service.LearnService learnService;

    @org.springframework.beans.factory.annotation.Autowired
    public PracticeController(TopicService topicService,
                              QuestionService questionService,
                              AttemptService attemptService,
                              BookmarkService bookmarkService,
                              com.byteforce.service.MemoryService memoryService,
                              com.byteforce.service.LearnService learnService) {
        this.topicService = topicService;
        this.questionService = questionService;
        this.attemptService = attemptService;
        this.bookmarkService = bookmarkService;
        this.memoryService = memoryService;
        this.learnService = learnService;
    }

    public PracticeController(TopicService topicService,
                              QuestionService questionService,
                              AttemptService attemptService,
                              BookmarkService bookmarkService) {
        this(topicService, questionService, attemptService, bookmarkService, null, null);
    }

    @GetMapping
    public String practiceHome(@RequestParam(value = "topicId", required = false) Long topicId,
                               @RequestParam(value = "difficulty", required = false) String difficultyStr,
                               @RequestParam(value = "status", required = false) String statusStr,
                               @RequestParam(value = "bookmarked", required = false) Boolean bookmarked,
                               HttpSession session,
                               Model model) {
        model.addAttribute("topics", topicService.getAllTopics());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("selectedTopicId", topicId);
        model.addAttribute("selectedDifficulty", difficultyStr);
        model.addAttribute("selectedStatus", statusStr);
        model.addAttribute("selectedBookmarked", bookmarked);

        Difficulty difficulty = parseDifficulty(difficultyStr);
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        UUID userId = userOpt.map(User::getId).orElse(null);

        List<Question> previewQuestions = fetchFilteredQuestions(topicId, difficulty, statusStr, bookmarked, userId);
        model.addAttribute("previewQuestions", previewQuestions);
        model.addAttribute("totalAvailable", previewQuestions.size());

        if (userId != null) {
            List<com.byteforce.domain.QuestionAttempt> attempts = attemptService.getAttemptsForUser(userId);
            Set<Long> solvedIds = new HashSet<>();
            Set<Long> failedIds = new HashSet<>();
            for (com.byteforce.domain.QuestionAttempt a : attempts) {
                if (a.getStatus() == AttemptStatus.SOLVED) {
                    solvedIds.add(a.getQuestionId());
                } else if (a.getStatus() == AttemptStatus.FAILED) {
                    failedIds.add(a.getQuestionId());
                }
            }
            failedIds.removeAll(solvedIds);

            Set<Long> bookmarkedIds = bookmarkService.getBookmarksForUser(userId).stream()
                    .map(com.byteforce.domain.Bookmark::getQuestionId)
                    .collect(Collectors.toSet());

            model.addAttribute("solvedIds", solvedIds);
            model.addAttribute("failedIds", failedIds);
            model.addAttribute("bookmarkedIds", bookmarkedIds);
        } else {
            model.addAttribute("solvedIds", Collections.emptySet());
            model.addAttribute("failedIds", Collections.emptySet());
            model.addAttribute("bookmarkedIds", Collections.emptySet());
        }

        return "practice/index";
    }

    @GetMapping("/session")
    public String practiceSession(@RequestParam(value = "topicId", required = false) Long topicId,
                                  @RequestParam(value = "difficulty", required = false) String difficultyStr,
                                  @RequestParam(value = "status", required = false) String statusStr,
                                  @RequestParam(value = "bookmarked", required = false) Boolean bookmarked,
                                  @RequestParam(value = "index", required = false, defaultValue = "0") int index,
                                  Model model,
                                  HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            String returnUrl = "/practice/session"
                    + (topicId != null ? "?topicId=" + topicId : "")
                    + (difficultyStr != null ? (topicId != null ? "&" : "?") + "difficulty=" + difficultyStr : "");
            return "redirect:/auth-required?returnUrl=" + returnUrl + "&feature=practicing questions and recording progress";
        }

        User user = userOpt.get();
        Difficulty difficulty = parseDifficulty(difficultyStr);
        List<Question> questions = fetchFilteredQuestions(topicId, difficulty, statusStr, bookmarked, user.getId());

        model.addAttribute("topicId", topicId);
        model.addAttribute("difficulty", difficultyStr);
        model.addAttribute("status", statusStr);
        model.addAttribute("bookmarked", bookmarked);
        model.addAttribute("index", index);

        if (questions.isEmpty()) {
            model.addAttribute("isEmpty", true);
            return "practice/session";
        }

        int safeIndex = Math.max(0, Math.min(index, questions.size() - 1));
        Question currentQuestion = questions.get(safeIndex);

        model.addAttribute("isEmpty", false);
        model.addAttribute("question", currentQuestion);
        model.addAttribute("currentIndex", safeIndex);
        model.addAttribute("totalQuestions", questions.size());
        model.addAttribute("isBookmarked", bookmarkService.isBookmarked(user.getId(), currentQuestion.getId()));

        // Resolve topic name
        String topicName = topicService.getTopicById(currentQuestion.getTopicId())
                .map(Topic::getName)
                .orElse("Placement Preparation");
        model.addAttribute("topicName", topicName);

        // Parse MCQ choices if applicable
        if (currentQuestion.getQuestionType() == QuestionType.MCQ) {
            model.addAttribute("mcqChoices", extractMcqChoices(currentQuestion.getDescription()));
        }

        return "practice/session";
    }

    @PostMapping("/submit")
    public String submitAnswer(@RequestParam("questionId") long questionId,
                               @RequestParam(value = "answer", required = false, defaultValue = "") String answer,
                               @RequestParam(value = "topicId", required = false) Long topicId,
                               @RequestParam(value = "difficulty", required = false) String difficultyStr,
                               @RequestParam(value = "index", required = false, defaultValue = "0") int index,
                               Model model,
                               HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/practice";
        }

        User user = userOpt.get();
        Optional<Question> questionOpt = questionService.getQuestionById(questionId);
        if (questionOpt.isEmpty()) {
            return "redirect:/practice";
        }

        Question question = questionOpt.get();
        String submittedAnswer = answer.trim();

        // Determine correctness
        AttemptStatus status;
        if (question.getQuestionType() == QuestionType.MCQ) {
            boolean isCorrect = isMcqCorrect(submittedAnswer, question.getSolution());
            status = isCorrect ? AttemptStatus.SOLVED : AttemptStatus.FAILED;
        } else {
            status = submittedAnswer.isBlank() ? AttemptStatus.ATTEMPTED : AttemptStatus.SOLVED;
        }

        // Record attempt using AttemptService
        attemptService.recordAttempt(user.getId(), questionId, status, submittedAnswer, 25);

        // Record practice weakness signal for related concepts if attempt failed
        if (status == AttemptStatus.FAILED && memoryService != null && learnService != null) {
            try {
                List<com.byteforce.domain.Concept> concepts = learnService.getConceptsForTopic(question.getTopicId());
                for (com.byteforce.domain.Concept c : concepts) {
                    memoryService.recordPracticeWeaknessSignal(user.getId(), c.getId());
                }
            } catch (Exception e) {
                log.warn("Could not record practice weakness signal for question ID {}: {}", questionId, e.getMessage());
            }
        }

        // Populate result page
        model.addAttribute("question", question);
        model.addAttribute("status", status);
        model.addAttribute("submittedAnswer", submittedAnswer);
        model.addAttribute("solution", question.getSolution());
        model.addAttribute("topicId", topicId);
        model.addAttribute("difficulty", difficultyStr);
        model.addAttribute("nextIndex", index + 1);

        String topicName = topicService.getTopicById(question.getTopicId())
                .map(Topic::getName)
                .orElse("General");
        model.addAttribute("topicName", topicName);

        return "practice/result";
    }

    @PostMapping("/skip")
    public String skipQuestion(@RequestParam("questionId") long questionId,
                               @RequestParam(value = "topicId", required = false) Long topicId,
                               @RequestParam(value = "difficulty", required = false) String difficultyStr,
                               @RequestParam(value = "index", required = false, defaultValue = "0") int index,
                               HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isPresent()) {
            attemptService.recordAttempt(userOpt.get().getId(), questionId, AttemptStatus.SKIPPED, "", 0);
        }

        String redirectUrl = "/practice/session?index=" + (index + 1);
        if (topicId != null) redirectUrl += "&topicId=" + topicId;
        if (difficultyStr != null && !difficultyStr.isBlank()) redirectUrl += "&difficulty=" + difficultyStr;
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/bookmark")
    public String toggleBookmark(@RequestParam("questionId") long questionId,
                                 @RequestParam(value = "topicId", required = false) Long topicId,
                                 @RequestParam(value = "difficulty", required = false) String difficultyStr,
                                 @RequestParam(value = "index", required = false, defaultValue = "0") int index,
                                 HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/practice";
        }

        bookmarkService.toggleBookmark(userOpt.get().getId(), questionId, "Saved from practice mode");

        String redirectUrl = "/practice/session?index=" + index;
        if (topicId != null) redirectUrl += "&topicId=" + topicId;
        if (difficultyStr != null && !difficultyStr.isBlank()) redirectUrl += "&difficulty=" + difficultyStr;
        return "redirect:" + redirectUrl;
    }

    private List<Question> fetchQuestions(Long topicId, Difficulty difficulty) {
        if (topicId != null && difficulty != null) {
            return questionService.getQuestionsByTopicAndDifficulty(topicId, difficulty);
        } else if (topicId != null) {
            return questionService.getQuestionsByTopic(topicId);
        } else if (difficulty != null) {
            return questionService.getQuestionsByDifficulty(difficulty);
        } else {
            return questionService.getAllQuestions();
        }
    }

    private List<Question> fetchFilteredQuestions(Long topicId, Difficulty difficulty, String statusStr, Boolean bookmarked, UUID userId) {
        List<Question> questions = fetchQuestions(topicId, difficulty);
        if (userId == null) {
            return questions;
        }

        Set<Long> solvedIds = null;
        Set<Long> failedIds = null;
        Set<Long> attemptedIds = null;
        Set<Long> bookmarkedIds = null;

        if (bookmarked != null && bookmarked) {
            bookmarkedIds = bookmarkService.getBookmarksForUser(userId).stream()
                    .map(com.byteforce.domain.Bookmark::getQuestionId)
                    .collect(Collectors.toSet());
        }

        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
            List<com.byteforce.domain.QuestionAttempt> attempts = attemptService.getAttemptsForUser(userId);
            solvedIds = new HashSet<>();
            failedIds = new HashSet<>();
            attemptedIds = new HashSet<>();
            for (com.byteforce.domain.QuestionAttempt a : attempts) {
                attemptedIds.add(a.getQuestionId());
                if (a.getStatus() == AttemptStatus.SOLVED) {
                    solvedIds.add(a.getQuestionId());
                } else if (a.getStatus() == AttemptStatus.FAILED) {
                    failedIds.add(a.getQuestionId());
                }
            }
            failedIds.removeAll(solvedIds);
        }

        final Set<Long> fSolved = solvedIds;
        final Set<Long> fFailed = failedIds;
        final Set<Long> fAttempted = attemptedIds;
        final Set<Long> fBm = bookmarkedIds;

        return questions.stream()
                .filter(q -> {
                    if (fBm != null && !fBm.contains(q.getId())) {
                        return false;
                    }
                    if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
                        String s = statusStr.trim().toUpperCase(Locale.ROOT);
                        if ("SOLVED".equals(s) && (fSolved == null || !fSolved.contains(q.getId()))) {
                            return false;
                        }
                        if ("FAILED".equals(s) && (fFailed == null || !fFailed.contains(q.getId()))) {
                            return false;
                        }
                        if (("NOT_ATTEMPTED".equals(s) || "UNATTEMPTED".equals(s)) && fAttempted != null && fAttempted.contains(q.getId())) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }

    private Difficulty parseDifficulty(String diff) {
        if (diff == null || diff.isBlank()) return null;
        try {
            return Difficulty.valueOf(diff.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<String> extractMcqChoices(String description) {
        List<String> choices = new ArrayList<>();
        if (description == null) return choices;

        String[] lines = description.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.matches("^[A-D][\\)\\].].*")) {
                choices.add(trimmed);
            }
        }
        return choices;
    }

    private boolean isMcqCorrect(String submitted, String solution) {
        if (submitted == null || solution == null) return false;
        String cleanSubmitted = submitted.trim().toLowerCase(Locale.ROOT);
        String cleanSolution = solution.trim().toLowerCase(Locale.ROOT);
        if (cleanSubmitted.isEmpty()) return false;

        // Check single letter match e.g. "B" or "b"
        if (cleanSubmitted.length() == 1) {
            return cleanSolution.startsWith(cleanSubmitted) || cleanSolution.contains(cleanSubmitted + ")") || cleanSolution.contains(cleanSubmitted + ".");
        }
        return cleanSolution.contains(cleanSubmitted);
    }
}
