package com.example.sessionsstore.controller;

import com.example.sessionsstore.model.ChargingSession;
import com.example.sessionsstore.model.SessionsStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.sessionsstore.model.ChargingSession.Status.IN_PROGRESS;
import static com.example.sessionsstore.model.ChargingSession.Status.STOPPED;
import static java.util.stream.Collectors.counting;

@RestController
public class SessionsRestController {

    private final SessionsStore sessionsStore;

    public SessionsRestController(SessionsStore sessionsStore) {
        this.sessionsStore = sessionsStore;
    }

    @PostMapping("/chargingSessions")
    public ResponseEntity submitSession(@RequestBody RestSubmitSessionRequest requestBody) {
        if(StringUtils.isEmpty(requestBody.getTimestamp()) || StringUtils.isEmpty(requestBody.getStationId())) {
            return new ResponseEntity<>(new RestError("Timestamp and stationId cannot be empty"), HttpStatus.BAD_REQUEST);
        }
        LocalDateTime startedAt;
        try {
            startedAt = LocalDateTime.parse(requestBody.getTimestamp(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return new ResponseEntity<>(new RestError("Cannot parse timestamp"), HttpStatus.BAD_REQUEST);
        }
        ChargingSession session = sessionsStore.addSession(requestBody.getStationId(), startedAt);
        RestSubmitSessionResponse response = new RestSubmitSessionResponse(session.getId().toString(), session.getStationId(), session.getStartedAt());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/chargingSessions/{id}")
    public ResponseEntity stopSession(@PathVariable String id) {
        try {
            sessionsStore.stopSession(id, LocalDateTime.now());
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new RestError("No sessions found with id=" + id), HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/chargingSessions")
    public ArrayList<ChargingSession> getAllSessions() {
        return sessionsStore.getAllSessions();
    }

    @GetMapping("/chargingSessions/summary")
    public RestSummary getSessionsSummary() {
        return assembleRestSummary(sessionsStore.getSessionsUpdatedLastMinute());
    }

    private RestSummary assembleRestSummary(List<ChargingSession> chargingSessions) {
        Map<ChargingSession.Status, Long> sessionsByStatus = chargingSessions.stream().collect(Collectors.groupingBy(ChargingSession::getStatus, counting()));
        return new RestSummary(chargingSessions.size(), sessionsByStatus.getOrDefault(IN_PROGRESS, 0L).intValue(), sessionsByStatus.getOrDefault(STOPPED, 0L).intValue());
    }

}
