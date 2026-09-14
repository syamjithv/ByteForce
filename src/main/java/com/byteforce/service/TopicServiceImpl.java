package com.byteforce.service;

import com.byteforce.domain.Topic;
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
 * Production implementation of {@link TopicService}.
 */
public class TopicServiceImpl implements TopicService {

    private static final Logger log = LoggerFactory.getLogger(TopicServiceImpl.class);

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;

    public TopicServiceImpl(TopicRepository topicRepository, QuestionRepository questionRepository) {
        this.topicRepository = Objects.requireNonNull(topicRepository, "topicRepository must not be null");
        this.questionRepository = Objects.requireNonNull(questionRepository, "questionRepository must not be null");
    }

    @Override
    public Topic createTopic(String name, String slug, String description, int displayOrder) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Topic name must not be blank.");
        }
        if (slug == null || slug.isBlank()) {
            throw new ValidationException("Topic slug must not be blank.");
        }

        String normalizedName = name.trim();
        String normalizedSlug = slug.trim().toLowerCase();

        if (topicRepository.existsByName(normalizedName)) {
            throw new ValidationException("A topic with name '" + normalizedName + "' already exists.");
        }
        if (topicRepository.existsBySlug(normalizedSlug)) {
            throw new ValidationException("A topic with slug '" + normalizedSlug + "' already exists.");
        }

        Topic topic = Topic.create(normalizedName, normalizedSlug, description != null ? description.trim() : null, displayOrder);
        Topic saved = topicRepository.save(topic);
        log.info("Created new topic with ID {} and slug '{}'", saved.getId(), saved.getSlug());
        return saved;
    }

    @Override
    public Topic updateTopic(long id, String name, String slug, String description, int displayOrder) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Topic name must not be blank.");
        }
        if (slug == null || slug.isBlank()) {
            throw new ValidationException("Topic slug must not be blank.");
        }

        Topic existing = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with ID: " + id));

        String normalizedName = name.trim();
        String normalizedSlug = slug.trim().toLowerCase();

        if (!existing.getName().equalsIgnoreCase(normalizedName) && topicRepository.existsByName(normalizedName)) {
            throw new ValidationException("A topic with name '" + normalizedName + "' already exists.");
        }
        if (!existing.getSlug().equalsIgnoreCase(normalizedSlug) && topicRepository.existsBySlug(normalizedSlug)) {
            throw new ValidationException("A topic with slug '" + normalizedSlug + "' already exists.");
        }

        Topic updatedTopic = existing
                .withName(normalizedName)
                .withSlug(normalizedSlug)
                .withDescription(description != null ? description.trim() : null)
                .withDisplayOrder(displayOrder);

        Topic saved = topicRepository.save(updatedTopic);
        log.info("Updated topic with ID {}", saved.getId());
        return saved;
    }

    @Override
    public Optional<Topic> getTopicById(long id) {
        return topicRepository.findById(id);
    }

    @Override
    public Optional<Topic> getTopicBySlug(String slug) {
        return topicRepository.findBySlug(slug);
    }

    @Override
    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }

    @Override
    public void deleteTopic(long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with ID: " + id));

        long associatedQuestions = questionRepository.countByTopicId(id);
        if (associatedQuestions > 0) {
            throw new ValidationException("Cannot delete topic '" + topic.getName() + "' because it has " + associatedQuestions + " associated questions.");
        }

        topicRepository.deleteById(id);
        log.info("Deleted topic with ID {}", id);
    }
}
