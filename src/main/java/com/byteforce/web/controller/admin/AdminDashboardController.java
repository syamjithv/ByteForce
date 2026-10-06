package com.byteforce.web.controller.admin;

import com.byteforce.service.AptitudeQuestionService;
import com.byteforce.service.AssessmentService;
import com.byteforce.service.BrainMapService;
import com.byteforce.service.LearnService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.RememberService;
import com.byteforce.service.TopicService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for the central Admin Dashboard console.
 * Protected by AdminAuthInterceptor to ensure only Role.ADMIN users have access.
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final LearnService learnService;
    private final TopicService topicService;
    private final QuestionService questionService;
    private final AssessmentService assessmentService;
    private final RememberService rememberService;
    private final BrainMapService brainMapService;
    private final AptitudeQuestionService aptitudeQuestionService;

    public AdminDashboardController(LearnService learnService,
                                    TopicService topicService,
                                    QuestionService questionService,
                                    AssessmentService assessmentService,
                                    RememberService rememberService,
                                    BrainMapService brainMapService,
                                    AptitudeQuestionService aptitudeQuestionService) {
        this.learnService = learnService;
        this.topicService = topicService;
        this.questionService = questionService;
        this.assessmentService = assessmentService;
        this.rememberService = rememberService;
        this.brainMapService = brainMapService;
        this.aptitudeQuestionService = aptitudeQuestionService;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        // Real database-backed statistics (no hardcoded fake values)
        long subjectCount = learnService.getTotalSubjectCount();
        long topicCount = topicService.getTotalTopicCount();
        long conceptCount = learnService.getTotalConceptCount();
        long resourceCount = learnService.getTotalResourceCount();
        long rememberCount = rememberService.getTotalCount();
        long questionCount = questionService.getTotalQuestionCount();
        long assessmentCount = assessmentService.getTotalAssessmentCount();
        long aptitudeCount = aptitudeQuestionService.getTotalAptitudeQuestionCount();
        long relationshipCount = brainMapService.getTotalRelationshipCount();

        model.addAttribute("subjectCount", subjectCount);
        model.addAttribute("topicCount", topicCount);
        model.addAttribute("conceptCount", conceptCount);
        model.addAttribute("resourceCount", resourceCount);
        model.addAttribute("rememberCount", rememberCount);
        model.addAttribute("questionCount", questionCount);
        model.addAttribute("assessmentCount", assessmentCount);
        model.addAttribute("aptitudeCount", aptitudeCount);
        model.addAttribute("relationshipCount", relationshipCount);

        model.addAttribute("activeTab", "admin-dashboard");
        model.addAttribute("pageTitle", "Admin Dashboard");
        return "admin/dashboard";
    }
}
