package com.playmotech.ghostcoach.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/** One data-point on the player's score-over-time progress chart. */
@Data
@AllArgsConstructor
public class ScoreProgressPoint {
    private Long sessionId;
    private Integer score;
    private Instant date;
}
