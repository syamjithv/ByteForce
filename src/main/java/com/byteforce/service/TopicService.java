package com.byteforce.service;

import com.byteforce.domain.Topic;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Topic business operations.
 */
public interface TopicService {

    Topic createTopic(String name, String slug, String description, int displayOrder);

    Topic updateTopic(long id, String name, String slug, String description, int displayOrder);

    Optional<Topic> getTopicById(long id);

    Optional<Topic> getTopicBySlug(String slug);

    List<Topic> getAllTopics();

    void deleteTopic(long id);
}
