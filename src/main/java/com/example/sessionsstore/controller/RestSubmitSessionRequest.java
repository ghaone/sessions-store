package com.example.sessionsstore.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
class RestSubmitSessionRequest {
    private String stationId;
    private String timestamp;
}
