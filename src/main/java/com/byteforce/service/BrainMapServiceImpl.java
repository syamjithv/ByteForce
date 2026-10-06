package com.byteforce.service;

import com.byteforce.domain.Concept;
import com.byteforce.domain.ConceptRelationship;
import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.domain.RelatedConceptView;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.ConceptRelationshipRepository;
import com.byteforce.repository.ConceptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production implementation of {@link BrainMapService}.
 */
public class BrainMapServiceImpl implements BrainMapService {

    private static final Logger log = LoggerFactory.getLogger(BrainMapServiceImpl.class);

    private final ConceptRelationshipRepository relationshipRepository;
    private final ConceptRepository conceptRepository;
    private final com.byteforce.repository.TopicRepository topicRepository;
    private final com.byteforce.repository.SubjectRepository subjectRepository;
    private final com.byteforce.repository.UserRememberReviewRepository userRememberReviewRepository;
    private final com.byteforce.service.brainmap.BrainMapPdfService pdfService;

    public BrainMapServiceImpl(ConceptRelationshipRepository relationshipRepository,
                               ConceptRepository conceptRepository) {
        this(relationshipRepository, conceptRepository, null, null, null);
    }

    public BrainMapServiceImpl(ConceptRelationshipRepository relationshipRepository,
                               ConceptRepository conceptRepository,
                               com.byteforce.repository.TopicRepository topicRepository,
                               com.byteforce.repository.SubjectRepository subjectRepository,
                               com.byteforce.repository.UserRememberReviewRepository userRememberReviewRepository) {
        this.relationshipRepository = Objects.requireNonNull(relationshipRepository, "relationshipRepository must not be null");
        this.conceptRepository = Objects.requireNonNull(conceptRepository, "conceptRepository must not be null");
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
        this.userRememberReviewRepository = userRememberReviewRepository;
        this.pdfService = new com.byteforce.service.brainmap.BrainMapPdfService();
    }

    @Override
    public List<RelatedConceptView> getRelatedConceptsForConcept(long conceptId) {
        if (conceptId <= 0) {
            return List.of();
        }

        List<RelatedConceptView> views = new ArrayList<>();

        // 1. Outgoing edges (this concept -> other concept)
        List<ConceptRelationship> outgoing = relationshipRepository.findBySourceConceptId(conceptId);
        for (ConceptRelationship rel : outgoing) {
            Optional<Concept> targetOpt = conceptRepository.findById(rel.getTargetConceptId());
            targetOpt.ifPresent(target -> views.add(new RelatedConceptView(rel, target, true)));
        }

        // 2. Incoming edges (other concept -> this concept)
        List<ConceptRelationship> incoming = relationshipRepository.findByTargetConceptId(conceptId);
        for (ConceptRelationship rel : incoming) {
            Optional<Concept> sourceOpt = conceptRepository.findById(rel.getSourceConceptId());
            sourceOpt.ifPresent(source -> views.add(new RelatedConceptView(rel, source, false)));
        }

        return views;
    }

    @Override
    public List<ConceptRelationship> getAllRelationships() {
        return relationshipRepository.findAll();
    }

    @Override
    public ConceptRelationship createRelationship(long sourceConceptId,
                                                  long targetConceptId,
                                                  ConceptRelationshipType type,
                                                  String description,
                                                  int displayOrder) {
        if (sourceConceptId <= 0 || targetConceptId <= 0) {
            throw new ValidationException("Source and target concept IDs must be valid positive numbers.");
        }
        if (sourceConceptId == targetConceptId) {
            throw new ValidationException("Cannot create a relationship from a concept to itself.");
        }
        if (type == null) {
            throw new ValidationException("Relationship type must not be null.");
        }

        if (!conceptRepository.existsById(sourceConceptId)) {
            throw new ResourceNotFoundException("Source concept not found with ID: " + sourceConceptId);
        }
        if (!conceptRepository.existsById(targetConceptId)) {
            throw new ResourceNotFoundException("Target concept not found with ID: " + targetConceptId);
        }

        ConceptRelationship rel = ConceptRelationship.create(sourceConceptId, targetConceptId, type, description, displayOrder);
        ConceptRelationship saved = relationshipRepository.save(rel);
        log.info("Created concept relationship ID {} ({} -> {} as {})", saved.getId(), sourceConceptId, targetConceptId, type);
        return saved;
    }

    @Override
    public ConceptRelationship updateRelationship(long id,
                                                  long sourceConceptId,
                                                  long targetConceptId,
                                                  ConceptRelationshipType type,
                                                  String description,
                                                  int displayOrder) {
        if (sourceConceptId <= 0 || targetConceptId <= 0) {
            throw new ValidationException("Source and target concept IDs must be valid positive numbers.");
        }
        if (sourceConceptId == targetConceptId) {
            throw new ValidationException("Cannot create a relationship from a concept to itself.");
        }
        if (type == null) {
            throw new ValidationException("Relationship type must not be null.");
        }

        if (!conceptRepository.existsById(sourceConceptId)) {
            throw new ResourceNotFoundException("Source concept not found with ID: " + sourceConceptId);
        }
        if (!conceptRepository.existsById(targetConceptId)) {
            throw new ResourceNotFoundException("Target concept not found with ID: " + targetConceptId);
        }

        ConceptRelationship existing = relationshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concept relationship not found with ID: " + id));

        ConceptRelationship updated = existing
                .withSourceConceptId(sourceConceptId)
                .withTargetConceptId(targetConceptId)
                .withRelationshipType(type)
                .withDescription(description != null ? description.trim() : "")
                .withDisplayOrder(displayOrder);

        ConceptRelationship saved = relationshipRepository.save(updated);
        log.info("Updated concept relationship ID {}", saved.getId());
        return saved;
    }

    @Override
    public Optional<ConceptRelationship> getRelationshipById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        return relationshipRepository.findById(id);
    }

    @Override
    public void deleteRelationship(long id) {
        if (!relationshipRepository.deleteById(id)) {
            throw new ResourceNotFoundException("Concept relationship not found with ID: " + id);
        }
        log.info("Deleted concept relationship ID {}", id);
    }

    @Override
    public long getTotalRelationshipCount() {
        return relationshipRepository.count();
    }

    @Override
    public com.byteforce.domain.brainmap.BrainMapGraph getBrainMapGraph(long centerConceptId, java.util.UUID currentUserId) {
        Concept center = conceptRepository.findById(centerConceptId)
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found with ID: " + centerConceptId));

        com.byteforce.domain.Topic topic = (topicRepository != null)
                ? topicRepository.findById(center.getTopicId()).orElse(null)
                : null;

        com.byteforce.domain.Subject subject = (subjectRepository != null && topic != null && topic.getSubjectId() != null)
                ? subjectRepository.findById(topic.getSubjectId()).orElse(null)
                : null;

        List<RelatedConceptView> relatedViews = getRelatedConceptsForConcept(centerConceptId);

        // Group into hierarchical roles
        List<RelatedConceptView> prereqList = new ArrayList<>();
        List<RelatedConceptView> successorList = new ArrayList<>();
        List<RelatedConceptView> confusedList = new ArrayList<>();
        List<RelatedConceptView> relatedList = new ArrayList<>();

        for (RelatedConceptView view : relatedViews) {
            ConceptRelationshipType type = view.getType();
            if (type == ConceptRelationshipType.PREREQUISITE) {
                if (view.isOutgoing()) {
                    prereqList.add(view);
                } else {
                    successorList.add(view);
                }
            } else if (type == ConceptRelationshipType.PARENT) {
                if (view.isOutgoing()) {
                    prereqList.add(view);
                } else {
                    successorList.add(view);
                }
            } else if (type == ConceptRelationshipType.CHILD) {
                if (view.isOutgoing()) {
                    successorList.add(view);
                } else {
                    prereqList.add(view);
                }
            } else if (type == ConceptRelationshipType.COMMONLY_CONFUSED) {
                confusedList.add(view);
            } else {
                relatedList.add(view);
            }
        }

        double nodeW = 200.0;
        double nodeH = 68.0;
        double cx = 480.0;
        double cy = 290.0;

        List<com.byteforce.domain.brainmap.BrainMapNode> nodes = new ArrayList<>();
        List<com.byteforce.domain.brainmap.BrainMapEdge> edges = new ArrayList<>();

        // Center Node
        String centerMemoryStatus = resolveMemoryStatus(centerConceptId, currentUserId);
        com.byteforce.domain.brainmap.BrainMapNode centerNode = new com.byteforce.domain.brainmap.BrainMapNode(
                center.getId(),
                center.getTitle(),
                center.getShortExplanation(),
                true,
                cx - (nodeW / 2.0),
                cy - (nodeH / 2.0),
                nodeW,
                nodeH,
                centerMemoryStatus,
                "CENTER",
                center.getTopicId()
        );
        nodes.add(centerNode);

        // 1. Prerequisites (Above center: Tier 1, y = 70)
        double prereqY = 70.0;
        layoutTier(prereqList, nodes, edges, centerNode, cx, prereqY, nodeW, nodeH, "PREREQUISITE", currentUserId);

        // 2. Successors (Below center: Tier 2, y = 490)
        double succY = 490.0;
        layoutTier(successorList, nodes, edges, centerNode, cx, succY, nodeW, nodeH, "CHILD", currentUserId);

        // 3. Flanks: Confused (left) and Related (right)
        layoutFlanks(confusedList, relatedList, nodes, edges, centerNode, cx, cy, nodeW, nodeH, currentUserId);

        // Calculate dynamic viewBox bounding box
        double minX = 0;
        double maxX = 960;
        double minY = 0;
        double maxY = 620;

        for (com.byteforce.domain.brainmap.BrainMapNode n : nodes) {
            minX = Math.min(minX, n.getX() - 40);
            maxX = Math.max(maxX, n.getX() + n.getWidth() + 40);
            minY = Math.min(minY, n.getY() - 40);
            maxY = Math.max(maxY, n.getY() + n.getHeight() + 40);
        }

        double totalW = Math.max(960.0, maxX - minX);
        double totalH = Math.max(620.0, maxY - minY);

        return new com.byteforce.domain.brainmap.BrainMapGraph(
                center,
                topic,
                subject,
                nodes,
                edges,
                minX,
                minY,
                totalW,
                totalH
        );
    }

    private void layoutTier(List<RelatedConceptView> views,
                            List<com.byteforce.domain.brainmap.BrainMapNode> nodes,
                            List<com.byteforce.domain.brainmap.BrainMapEdge> edges,
                            com.byteforce.domain.brainmap.BrainMapNode centerNode,
                            double cx,
                            double y,
                            double nodeW,
                            double nodeH,
                            String role,
                            java.util.UUID currentUserId) {
        int count = views.size();
        if (count == 0) return;

        double spacing = 240.0;
        double startX = cx - ((count - 1) * spacing / 2.0) - (nodeW / 2.0);

        for (int i = 0; i < count; i++) {
            RelatedConceptView v = views.get(i);
            Concept c = v.getConnectedConcept();
            double x = startX + (i * spacing);

            String mem = resolveMemoryStatus(c.getId(), currentUserId);
            com.byteforce.domain.brainmap.BrainMapNode node = new com.byteforce.domain.brainmap.BrainMapNode(
                    c.getId(),
                    c.getTitle(),
                    c.getShortExplanation(),
                    false,
                    x,
                    y,
                    nodeW,
                    nodeH,
                    mem,
                    role,
                    c.getTopicId()
            );
            nodes.add(node);

            // Edge coordinates
            if ("PREREQUISITE".equalsIgnoreCase(role)) {
                // From prereq bottom to center top
                edges.add(new com.byteforce.domain.brainmap.BrainMapEdge(
                        node.getId(),
                        centerNode.getId(),
                        v.getType(),
                        v.getDescription(),
                        node.getCenterX(),
                        node.getY() + nodeH,
                        centerNode.getCenterX(),
                        centerNode.getY(),
                        v.isOutgoing()
                ));
            } else {
                // From center bottom to successor top
                edges.add(new com.byteforce.domain.brainmap.BrainMapEdge(
                        centerNode.getId(),
                        node.getId(),
                        v.getType(),
                        v.getDescription(),
                        centerNode.getCenterX(),
                        centerNode.getY() + centerNode.getHeight(),
                        node.getCenterX(),
                        node.getY(),
                        v.isOutgoing()
                ));
            }
        }
    }

    private void layoutFlanks(List<RelatedConceptView> confusedList,
                              List<RelatedConceptView> relatedList,
                              List<com.byteforce.domain.brainmap.BrainMapNode> nodes,
                              List<com.byteforce.domain.brainmap.BrainMapEdge> edges,
                              com.byteforce.domain.brainmap.BrainMapNode centerNode,
                              double cx,
                              double cy,
                              double nodeW,
                              double nodeH,
                              java.util.UUID currentUserId) {
        // Confused placed on left: x = cx - 350
        double leftX = cx - 360.0 - (nodeW / 2.0);
        int cCount = confusedList.size();
        double cStartY = cy - ((cCount - 1) * 90.0 / 2.0) - (nodeH / 2.0);

        for (int i = 0; i < cCount; i++) {
            RelatedConceptView v = confusedList.get(i);
            Concept c = v.getConnectedConcept();
            double y = cStartY + (i * 90.0);

            String mem = resolveMemoryStatus(c.getId(), currentUserId);
            com.byteforce.domain.brainmap.BrainMapNode node = new com.byteforce.domain.brainmap.BrainMapNode(
                    c.getId(),
                    c.getTitle(),
                    c.getShortExplanation(),
                    false,
                    leftX,
                    y,
                    nodeW,
                    nodeH,
                    mem,
                    "COMMONLY_CONFUSED",
                    c.getTopicId()
            );
            nodes.add(node);

            edges.add(new com.byteforce.domain.brainmap.BrainMapEdge(
                    centerNode.getId(),
                    node.getId(),
                    v.getType(),
                    v.getDescription(),
                    centerNode.getX(),
                    centerNode.getCenterY(),
                    node.getX() + nodeW,
                    node.getCenterY(),
                    v.isOutgoing()
            ));
        }

        // Related placed on right: x = cx + 350
        double rightX = cx + 360.0 - (nodeW / 2.0);
        int rCount = relatedList.size();
        double rStartY = cy - ((rCount - 1) * 90.0 / 2.0) - (nodeH / 2.0);

        for (int i = 0; i < rCount; i++) {
            RelatedConceptView v = relatedList.get(i);
            Concept c = v.getConnectedConcept();
            double y = rStartY + (i * 90.0);

            String mem = resolveMemoryStatus(c.getId(), currentUserId);
            com.byteforce.domain.brainmap.BrainMapNode node = new com.byteforce.domain.brainmap.BrainMapNode(
                    c.getId(),
                    c.getTitle(),
                    c.getShortExplanation(),
                    false,
                    rightX,
                    y,
                    nodeW,
                    nodeH,
                    mem,
                    "RELATED",
                    c.getTopicId()
            );
            nodes.add(node);

            edges.add(new com.byteforce.domain.brainmap.BrainMapEdge(
                    centerNode.getId(),
                    node.getId(),
                    v.getType(),
                    v.getDescription(),
                    centerNode.getX() + centerNode.getWidth(),
                    centerNode.getCenterY(),
                    node.getX(),
                    node.getCenterY(),
                    v.isOutgoing()
            ));
        }
    }

    private String resolveMemoryStatus(long conceptId, java.util.UUID userId) {
        if (userRememberReviewRepository == null || userId == null) {
            return "NONE";
        }
        var reviews = userRememberReviewRepository.findByUserIdAndConceptId(userId, conceptId);
        if (reviews.isEmpty()) {
            return "NONE";
        }
        Instant now = Instant.now();
        boolean hasDue = reviews.stream().anyMatch(r -> r.isDue(now));
        if (hasDue) {
            return "DUE";
        }
        boolean allReviewed = reviews.stream().allMatch(r -> r.getState() == com.byteforce.domain.ReviewState.REVIEW);
        if (allReviewed) {
            return "REVIEWED";
        }
        return "NEW";
    }

    @Override
    public byte[] exportBrainMapPdf(long centerConceptId) {
        com.byteforce.domain.brainmap.BrainMapGraph graph = getBrainMapGraph(centerConceptId, null);
        return pdfService.generateBrainMapPdf(graph);
    }
}
