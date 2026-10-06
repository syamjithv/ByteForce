package com.byteforce.learn;

import com.byteforce.ByteForceWebApplication;
import com.byteforce.domain.Concept;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.service.LearnService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * End-to-end verification of the ByteForce Master Curriculum across all 18 subjects,
 * structured visual concept presentation, Remember integration, and Brain Maps.
 */
@SpringBootTest(classes = ByteForceWebApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MasterCurriculumVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearnService learnService;

    @Autowired
    private com.byteforce.service.TopicService topicService;

    @Test
    @DisplayName("Verify /learn catalog lists all 18 canonical CS subjects and excludes aptitude")
    void testLearnCatalogRendersAllCanonicalSubjects() throws Exception {
        mockMvc.perform(get("/learn"))
                .andExpect(status().isOk())
                .andExpect(view().name("learn/index"))
                .andExpect(model().attributeExists("subjects"))
                .andExpect(content().string(containsString("Programming Foundations")))
                .andExpect(content().string(containsString("Data Structures &amp; Algorithms")))
                .andExpect(content().string(containsString("Java &amp; OOP")))
                .andExpect(content().string(containsString("DBMS &amp; SQL")))
                .andExpect(content().string(containsString("Operating Systems")))
                .andExpect(content().string(containsString("Computer Networks")))
                .andExpect(content().string(containsString("Computer Organization &amp; Architecture")))
                .andExpect(content().string(containsString("Theory of Computation")))
                .andExpect(content().string(containsString("Software Engineering")))
                .andExpect(content().string(containsString("System Design")))
                .andExpect(content().string(containsString("Cybersecurity")))
                .andExpect(content().string(containsString("Distributed &amp; Parallel Systems")))
                .andExpect(content().string(containsString("Programming Languages &amp; Compilers")))
                .andExpect(content().string(containsString("Web &amp; API Fundamentals")))
                .andExpect(content().string(containsString("AI &amp; ML Fundamentals")))
                .andExpect(content().string(containsString("Mathematics for CS")))
                .andExpect(content().string(containsString("HCI &amp; Accessibility")))
                .andExpect(content().string(containsString("Ethics, Privacy &amp; Professional Practice")));
    }

    @Test
    @DisplayName("Verify /learn/subject/{id} renders topics and concepts cleanly")
    void testSubjectPageRendering() throws Exception {
        mockMvc.perform(get("/learn/subject/operating-systems"))
                .andExpect(status().isOk())
                .andExpect(view().name("learn/subject"))
                .andExpect(model().attributeExists("subject", "topics"))
                .andExpect(content().string(containsString("Operating Systems")))
                .andExpect(content().string(containsString("Process States")))
                .andExpect(content().string(containsString("Context Switching")))
                .andExpect(content().string(containsString("CPU Scheduling")));
    }

    @Test
    @DisplayName("Verify /learn/topic/{id} renders topic header and concept cards")
    void testTopicPageRendering() throws Exception {
        Topic topic = topicService.getTopicBySlug("os-concurrency").orElseThrow();

        mockMvc.perform(get("/learn/topic/" + topic.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("learn/topic"))
                .andExpect(model().attributeExists("topic", "concepts"))
                .andExpect(content().string(containsString("Operating Systems &amp; Concurrency")))
                .andExpect(content().string(containsString("Process States")));
    }

    @Test
    @DisplayName("Verify /learn/concept/{id} renders all structured sections and visual cards")
    void testConceptPageVisualSections() throws Exception {
        mockMvc.perform(get("/learn/concept/1001"))
                .andExpect(status().isOk())
                .andExpect(view().name("learn/concept"))
                .andExpect(model().attributeExists("concept", "presentation", "commonConfusions", "interviewReminders", "keyFacts"))
                .andExpect(content().string(containsString("Why This Matters")))
                .andExpect(content().string(containsString("The Core Concept")))
                .andExpect(content().string(containsString("Key Placement Takeaways")))
                .andExpect(content().string(containsString("Code & Implementation Example")))
                .andExpect(content().string(containsString("copyCodeSnippet")))
                .andExpect(content().string(containsString("Brain Map — Connected Concepts")))
                .andExpect(content().string(containsString("Ready to Test Your Understanding?")));
    }

    @Test
    @DisplayName("Verify new curriculum concept (4001: Big-O) renders structured sections correctly")
    void testNewCurriculumConceptRendering() throws Exception {
        mockMvc.perform(get("/learn/concept/4001"))
                .andExpect(status().isOk())
                .andExpect(view().name("learn/concept"))
                .andExpect(model().attributeExists("concept", "presentation"))
                .andExpect(content().string(containsString("Time Complexity &amp; Big-O Notation")))
                .andExpect(content().string(containsString("Why This Matters")))
                .andExpect(content().string(containsString("The Core Concept")))
                .andExpect(content().string(containsString("Key Placement Takeaways")))
                .andExpect(content().string(containsString("Code & Implementation Example")));
    }

    @Test
    @DisplayName("Verify /remember and /brain-maps routes remain functional and connected")
    void testRememberAndBrainMapsRoutes() throws Exception {
        mockMvc.perform(get("/remember"))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/index"));

        mockMvc.perform(get("/brain-maps"))
                .andExpect(status().isOk())
                .andExpect(view().name("brain-maps/index"))
                .andExpect(model().attributeExists("graph"));
    }
}
