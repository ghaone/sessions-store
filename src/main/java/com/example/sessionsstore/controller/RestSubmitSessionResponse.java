package com.example.sessionsstore.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
class RestSubmitSessionResponse {
    private String id;
    private String stationId;
    private LocalDateTime timestamp;
}
