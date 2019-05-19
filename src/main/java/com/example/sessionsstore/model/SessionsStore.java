package com.example.sessionsstore.model;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.sessionsstore.model.ChargingSession.*;

@Component
public class SessionsStore {

    private Map<String, Map<String, ChargingSession>> store = new HashMap<>();
    private Map<String, String> index = new HashMap<>();

    synchronized public ChargingSession addSession(String stationId, LocalDateTime startedAt) {
        ChargingSession session = new ChargingSession(UUID.randomUUID(), stationId, startedAt, Status.IN_PROGRESS);
        addSessionToStoreAndIndex(session, getDateStringWithoutSeconds(startedAt));
        return session;
    }

    private String getDateStringWithoutSeconds(LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).substring(0,16);
    }

    private void addSessionToStoreAndIndex(ChargingSession session, String lastUpdateTime) {
        Map<String, ChargingSession> sessions = store.computeIfAbsent(lastUpdateTime, k -> new HashMap<>());
        sessions.put(session.getId().toString(), session);
        index.put(session.getId().toString(), lastUpdateTime);
    }

    synchronized public void stopSession(String id, LocalDateTime stopTime) {
        String date = index.get(id);
        Map<String, ChargingSession> sessions = store.get(date);
        ChargingSession session = sessions.get(id);
        if (session == null) {
           throw new IllegalArgumentException("No sessions with such Id");
        }
        index.remove(id);
        sessions.remove(id);
        session.setStatus(Status.STOPPED);
        session.setStoppedAt(stopTime);

        addSessionToStoreAndIndex(session, getDateStringWithoutSeconds(stopTime));
    }

    synchronized public ArrayList<ChargingSession> getAllSessions() {
        ArrayList<ChargingSession> allSessions = new ArrayList<>();
        for (String key : store.keySet()) {
           allSessions.addAll(new ArrayList<>(store.get(key).values()));
        }
        return allSessions;
    }

    synchronized public ArrayList<ChargingSession> getSessionsUpdatedLastMinute() {
        String dateTimeForCurrentMinute = getDateStringWithoutSeconds(LocalDateTime.now());
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1L);
        String dateTimeForOneMinuteAgo = getDateStringWithoutSeconds(oneMinuteAgo);


        ArrayList<ChargingSession> sessionsUpdatedDuringCurrentMinute = getListOfSessions(store.get(dateTimeForCurrentMinute));
        ArrayList<ChargingSession> sessionsUpdatedDuringPreviousMinute = getListOfSessions(store.get(dateTimeForOneMinuteAgo));

        List<ChargingSession> filteredSessions = sessionsUpdatedDuringPreviousMinute.stream()
                .filter(s -> s.getStoppedAt() == null ? s.getStartedAt().isAfter(oneMinuteAgo) : s.getStoppedAt().isAfter(oneMinuteAgo))
                .collect(Collectors.toList());

        ArrayList<ChargingSession> result = new ArrayList<>();
        result.addAll(sessionsUpdatedDuringCurrentMinute);
        result.addAll(filteredSessions);
        return result;
    }

    private ArrayList<ChargingSession> getListOfSessions(Map<String, ChargingSession> sessions) {
       if(sessions != null) {
           return new ArrayList<>(sessions.values());
       } else {
           return new ArrayList<>();
       }
    }

}
