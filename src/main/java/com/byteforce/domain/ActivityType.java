package com.byteforce.domain;

/**
 * Types of user events tracked in the ByteForce activity log.
 */
public enum ActivityType {
    REGISTRATION,
    LOGIN,
    SOLVED_QUESTION,
    ATTEMPTED_QUESTION,
    BOOKMARK_ADDED,
    BOOKMARK_REMOVED,
    PROFILE_UPDATED
}
