package com.byteforce.service;

import com.byteforce.domain.AptitudeCategory;
import com.byteforce.domain.AptitudeQuestion;
import com.byteforce.domain.Difficulty;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.AptitudeQuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production implementation of {@link AptitudeQuestionService}.
 */
public class AptitudeQuestionServiceImpl implements AptitudeQuestionService {

    private static final Logger log = LoggerFactory.getLogger(AptitudeQuestionServiceImpl.class);

    private final AptitudeQuestionRepository aptitudeRepository;

    public AptitudeQuestionServiceImpl(AptitudeQuestionRepository aptitudeRepository) {
        this.aptitudeRepository = Objects.requireNonNull(aptitudeRepository, "aptitudeRepository must not be null");
    }

    @Override
    public List<AptitudeQuestion> getAllQuestions() {
        return aptitudeRepository.findAll();
    }

    @Override
    public List<AptitudeQuestion> getQuestionsByCategory(AptitudeCategory category) {
        if (category == null) {
            return getAllQuestions();
        }
        return aptitudeRepository.findByCategory(category);
    }

    @Override
    public List<AptitudeQuestion> getQuestionsByDifficulty(Difficulty difficulty) {
        if (difficulty == null) {
            return getAllQuestions();
        }
        return aptitudeRepository.findByDifficulty(difficulty);
    }

    @Override
    public List<AptitudeQuestion> getQuestionsByCategoryAndDifficulty(AptitudeCategory category, Difficulty difficulty) {
        return aptitudeRepository.findByCategoryAndDifficulty(category, difficulty);
    }

    @Override
    public Optional<AptitudeQuestion> getQuestionById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        return aptitudeRepository.findById(id);
    }

    @Override
    public AptitudeQuestion createQuestion(AptitudeCategory category,
                                           String topic,
                                           Difficulty difficulty,
                                           String question,
                                           String optionA,
                                           String optionB,
                                           String optionC,
                                           String optionD,
                                           String correctAnswer,
                                           String explanation) {
        validateQuestionFields(category, topic, difficulty, question, optionA, optionB, optionC, optionD, correctAnswer);

        AptitudeQuestion q = AptitudeQuestion.create(category, topic.trim(), difficulty, question.trim(),
                optionA.trim(), optionB.trim(), optionC.trim(), optionD.trim(), correctAnswer.trim(), explanation);

        AptitudeQuestion saved = aptitudeRepository.save(q);
        log.info("Created aptitude question ID {} under topic '{}' ({})", saved.getId(), saved.getTopic(), saved.getCategory());
        return saved;
    }

    @Override
    public AptitudeQuestion updateQuestion(long id,
                                           AptitudeCategory category,
                                           String topic,
                                           Difficulty difficulty,
                                           String question,
                                           String optionA,
                                           String optionB,
                                           String optionC,
                                           String optionD,
                                           String correctAnswer,
                                           String explanation,
                                           boolean active) {
        AptitudeQuestion existing = aptitudeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aptitude question not found with ID: " + id));

        validateQuestionFields(category, topic, difficulty, question, optionA, optionB, optionC, optionD, correctAnswer);

        AptitudeQuestion updated = existing.withDetails(category, topic.trim(), difficulty, question.trim(),
                optionA.trim(), optionB.trim(), optionC.trim(), optionD.trim(), correctAnswer.trim(), explanation, active);

        AptitudeQuestion saved = aptitudeRepository.save(updated);
        log.info("Updated aptitude question ID {}", saved.getId());
        return saved;
    }

    @Override
    public void deleteQuestion(long id) {
        if (!aptitudeRepository.deleteById(id)) {
            throw new ResourceNotFoundException("Aptitude question not found with ID: " + id);
        }
        log.info("Deleted aptitude question ID {}", id);
    }

    @Override
    public long getTotalCount() {
        return aptitudeRepository.count();
    }

    @Override
    public long getCountByCategory(AptitudeCategory category) {
        return aptitudeRepository.countByCategory(category);
    }

    private void validateQuestionFields(AptitudeCategory category, String topic, Difficulty difficulty,
                                        String question, String optionA, String optionB, String optionC,
                                        String optionD, String correctAnswer) {
        if (category == null) {
            throw new ValidationException("Category is required.");
        }
        if (topic == null || topic.isBlank()) {
            throw new ValidationException("Topic is required.");
        }
        if (difficulty == null) {
            throw new ValidationException("Difficulty is required.");
        }
        if (question == null || question.isBlank()) {
            throw new ValidationException("Question text is required.");
        }
        if (optionA == null || optionA.isBlank() ||
                optionB == null || optionB.isBlank() ||
                optionC == null || optionC.isBlank() ||
                optionD == null || optionD.isBlank()) {
            throw new ValidationException("All 4 options (A, B, C, D) are required.");
        }
        if (correctAnswer == null || correctAnswer.isBlank()) {
            throw new ValidationException("Correct answer option is required.");
        }
        String ans = correctAnswer.trim().toUpperCase();
        if (!ans.equals("A") && !ans.equals("B") && !ans.equals("C") && !ans.equals("D")) {
            throw new ValidationException("Correct answer must be one of: A, B, C, or D.");
        }
    }
}
