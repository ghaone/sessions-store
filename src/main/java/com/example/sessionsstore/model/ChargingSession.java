package com.example.sessionsstore.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChargingSession {
    UUID id;
    String stationId;
    LocalDateTime startedAt;
    StatusEnum status;
}

enum StatusEnum {
    STARTED, STOPPED
}