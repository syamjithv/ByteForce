package com.byteforce.service;

import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.QuestionRepository;
import com.byteforce.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production implementation of {@link QuestionService}.
 */
public class QuestionServiceImpl implements QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionServiceImpl.class);

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;

    public QuestionServiceImpl(QuestionRepository questionRepository, TopicRepository topicRepository) {
        this.questionRepository = Objects.requireNonNull(questionRepository, "questionRepository must not be null");
        this.topicRepository = Objects.requireNonNull(topicRepository, "topicRepository must not be null");
    }

    @Override
    public Question createQuestion(long topicId, String title, String slug, String description,
                                   Difficulty difficulty, String solution) {
        return createQuestion(topicId, title, slug, description, difficulty, QuestionType.CODING, solution);
    }

    @Override
    public Question createQuestion(long topicId, String title, String slug, String description,
                                   Difficulty difficulty, QuestionType questionType, String solution) {
        if (!topicRepository.existsById(topicId)) {
            throw new ValidationException("Cannot create question: Topic not found with ID: " + topicId);
        }
        if (title == null || title.isBlank()) {
            throw new ValidationException("Question title must not be blank.");
        }
        if (slug == null || slug.isBlank()) {
            throw new ValidationException("Question slug must not be blank.");
        }
        if (description == null || description.isBlank()) {
            throw new ValidationException("Question description must not be blank.");
        }
        if (difficulty == null) {
            throw new ValidationException("Question difficulty must not be null.");
        }
        if (questionType == null) {
            throw new ValidationException("Question type must not be null.");
        }

        String normalizedSlug = slug.trim().toLowerCase();
        if (questionRepository.existsBySlug(normalizedSlug)) {
            throw new ValidationException("A question with slug '" + normalizedSlug + "' already exists.");
        }

        Question question = Question.create(topicId, title.trim(), normalizedSlug, description.trim(), difficulty, questionType, solution);
        Question saved = questionRepository.save(question);
        log.info("Created question with ID {} of type {} under topic ID {}", saved.getId(), questionType, topicId);
        return saved;
    }

    @Override
    public Question updateQuestion(long id, long topicId, String title, String slug, String description,
                                   Difficulty difficulty, String solution) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with ID: " + id));
        return updateQuestion(id, topicId, title, slug, description, difficulty, existing.getQuestionType(), solution);
    }

    @Override
    public Question updateQuestion(long id, long topicId, String title, String slug, String description,
                                   Difficulty difficulty, QuestionType questionType, String solution) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with ID: " + id));

        if (!topicRepository.existsById(topicId)) {
            throw new ValidationException("Cannot update question: Topic not found with ID: " + topicId);
        }
        if (title == null || title.isBlank()) {
            throw new ValidationException("Question title must not be blank.");
        }
        if (slug == null || slug.isBlank()) {
            throw new ValidationException("Question slug must not be blank.");
        }
        if (description == null || description.isBlank()) {
            throw new ValidationException("Question description must not be blank.");
        }
        if (difficulty == null) {
            throw new ValidationException("Question difficulty must not be null.");
        }
        if (questionType == null) {
            throw new ValidationException("Question type must not be null.");
        }

        String normalizedSlug = slug.trim().toLowerCase();
        if (!existing.getSlug().equalsIgnoreCase(normalizedSlug) && questionRepository.existsBySlug(normalizedSlug)) {
            throw new ValidationException("A question with slug '" + normalizedSlug + "' already exists.");
        }

        Question updatedQuestion = existing
                .withTopicId(topicId)
                .withTitle(title.trim())
                .withSlug(normalizedSlug)
                .withDescription(description.trim())
                .withDifficulty(difficulty)
                .withQuestionType(questionType)
                .withSolution(solution);

        Question saved = questionRepository.save(updatedQuestion);
        log.info("Updated question with ID {}", saved.getId());
        return saved;
    }

    @Override
    public Optional<Question> getQuestionById(long id) {
        return questionRepository.findById(id);
    }

    @Override
    public Optional<Question> getQuestionBySlug(String slug) {
        return questionRepository.findBySlug(slug);
    }

    @Override
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    @Override
    public List<Question> getQuestionsByTopic(long topicId) {
        return questionRepository.findByTopicId(topicId);
    }

    @Override
    public List<Question> getQuestionsByDifficulty(Difficulty difficulty) {
        return questionRepository.findByDifficulty(difficulty);
    }

    @Override
    public List<Question> getQuestionsByType(QuestionType questionType) {
        return questionRepository.findByQuestionType(questionType);
    }

    @Override
    public List<Question> getQuestionsByTopicAndDifficulty(long topicId, Difficulty difficulty) {
        return questionRepository.findByTopicIdAndDifficulty(topicId, difficulty);
    }

    @Override
    public List<Question> searchQuestions(String keyword) {
        return questionRepository.search(keyword);
    }

    @Override
    public void deleteQuestion(long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Question not found with ID: " + id);
        }
        questionRepository.deleteById(id);
        log.info("Deleted question with ID {}", id);
    }

    @Override
    public long getTotalQuestionCount() {
        return questionRepository.count();
    }
}
