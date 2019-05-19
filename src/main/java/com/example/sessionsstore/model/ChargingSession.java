package com.example.sessionsstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargingSession {
    private UUID id;
    private String stationId;
    private LocalDateTime startedAt;
    @JsonIgnore
    private LocalDateTime stoppedAt;
    private Status status;

    public enum Status {
        IN_PROGRESS, STOPPED
    }

    ChargingSession(UUID id, String stationId, LocalDateTime startedAt, Status status) {
        this.id = id;
        this.stationId = stationId;
        this.startedAt = startedAt;
        this.status = status;
    }
}

