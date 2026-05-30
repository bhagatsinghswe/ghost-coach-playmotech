package com.playmotech.ghostcoach.repository;

import com.playmotech.ghostcoach.entity.CoachingSession;
import com.playmotech.ghostcoach.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoachingSessionRepository extends JpaRepository<CoachingSession, Long> {

    /** All sessions for a user, newest first — used for history view. */
    Page<CoachingSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /** Lightweight query for progress chart (score over time). */
    @Query("SELECT s.id, s.overallScore, s.createdAt FROM CoachingSession s " +
           "WHERE s.user = :user AND s.status = 'COMPLETED' " +
           "ORDER BY s.createdAt ASC")
    List<Object[]> findScoreProgressByUser(@Param("user") User user);

    Optional<CoachingSession> findByIdAndUser(Long id, User user);

    long countByUser(User user);
}
