package com.example.sessionsstore;

import com.example.sessionsstore.model.ChargingSession;
import com.example.sessionsstore.model.SessionsStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static com.example.sessionsstore.model.ChargingSession.Status.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SessionsStoreApplicationTests {

    private SessionsStore sessionsStore;

    @BeforeEach
     void setUp() {
        sessionsStore = new SessionsStore();
    }

    @Test
     void testGivenNoSessionsInStoreUpdatedThisMinute_WhenAddSession_ThenAddSessionToStoreAndToIndex() {
        ChargingSession sessionSubmittedCurrentMinute1 = sessionsStore.addSession("ABC-12345", LocalDateTime.now());
        ArrayList<ChargingSession> sessions = sessionsStore.getAllSessions();
        assertEquals(1L, sessions.size());
        assertEquals(IN_PROGRESS, sessionSubmittedCurrentMinute1.getStatus());
    }

    @Test
     void testGiven1SessionInStoreUpdatedCurrentMinute_WhenAddSession_ThenAddSessionToStoreAndToIndex() {
        ChargingSession sessionSubmittedCurrentMinute1 = sessionsStore.addSession("ABC-12345", LocalDateTime.now());
        assertEquals(1L, sessionsStore.getAllSessions().size());

        ChargingSession sessionSubmittedCurrentMinute2 = sessionsStore.addSession("ABC-12345", LocalDateTime.now());

        assertEquals(2L, sessionsStore.getAllSessions().size());
    }

    @Test
     void testGiven2SessionsInProgressAddedWithIntervalOfTwoMinutes_WhenStopThisSessions_ThenBothOfThemAreStopped() {
        ChargingSession sessionSubmittedCurrentMinute1 = sessionsStore.addSession("ABC-12345", LocalDateTime.now());
        ChargingSession sessionSubmitted1MinutesAgo1 = sessionsStore.addSession("ABC-12345", LocalDateTime.now().minusMinutes(1L));

        sessionsStore.stopSession(sessionSubmittedCurrentMinute1.getId().toString(), LocalDateTime.now());
        sessionsStore.stopSession(sessionSubmitted1MinutesAgo1.getId().toString(), LocalDateTime.now());

        ArrayList<ChargingSession> sessions = sessionsStore.getAllSessions();
        assertEquals(2L, sessions.size());
        sessions.forEach(s -> assertEquals(ChargingSession.Status.STOPPED, s.getStatus()));
    }

    @Test
    void testGivenNoSessionWithGivenIdInStore_WhenStopThisSession_ThenThrowRuntimeException() {
        assertThrows(IllegalArgumentException.class, () -> sessionsStore.stopSession(UUID.randomUUID().toString(), LocalDateTime.now()));
    }

    @Test
    void testGivenEmptyStore_WhenGetAllSessions_ThenReturnEmptyList() {
        ArrayList<ChargingSession> sessions = sessionsStore.getAllSessions();
        assertTrue(sessions.isEmpty());
    }

    @Test
     void testGivenSeveralSessionsStartedInDifferentTimeAndWithDifferentStatus_WhenGetSessionsUpdatedLastMinute_ThenValidLotsAreReturned() {
        LocalDateTime now = LocalDateTime.now();
        ChargingSession inProgressSessionSubmittedCurrentMinute = createInProgressSession(now);
        ChargingSession inProgressSessionSubmitted59SecondsAgo = createInProgressSession(now.minusSeconds(59L));
        ChargingSession inProgressSessionSubmitted61SecondsAgo = createInProgressSession(now.minusSeconds(61L));
        ChargingSession inProgressSessionSubmitted2MinutesAgo = createInProgressSession(now.minusMinutes(2L));
        ChargingSession stoppedSessionSubmittedCurrentMinuteStoppedNow = createStoppedSession(now, now);
        ChargingSession stoppedSessionSubmitted59SecondsAgoStoppedNow = createStoppedSession(now.minusSeconds(59L), now);
        ChargingSession stoppedSessionSubmitted61SecondsAgoStoppedNow = createStoppedSession(now.minusSeconds(61L), now);
        ChargingSession stoppedSessionSubmitted61SecondsAgoStopped61SecondsAgo = createStoppedSession(now.minusSeconds(61L), now.minusSeconds(61L));
        ChargingSession stoppedSessionSubmitted2MinutesAgoStoppedNow = createStoppedSession(now.minusMinutes(2L), now);
        ChargingSession stoppedSessionSubmitted2MinutesAgoStopped2MinutesAgo = createStoppedSession(now.minusMinutes(2L), now.minusMinutes(2L));
        assertEquals(10L, sessionsStore.getAllSessions().size());

        ArrayList<ChargingSession> sessionsUpdatedLastMinute = sessionsStore.getSessionsUpdatedLastMinute();

        assertEquals(6L, sessionsUpdatedLastMinute.size());
        assertTrue(sessionsUpdatedLastMinute.contains(inProgressSessionSubmittedCurrentMinute));
        assertTrue(sessionsUpdatedLastMinute.contains(inProgressSessionSubmitted59SecondsAgo));
        assertTrue(sessionsUpdatedLastMinute.contains(stoppedSessionSubmittedCurrentMinuteStoppedNow));
        assertTrue(sessionsUpdatedLastMinute.contains(stoppedSessionSubmitted59SecondsAgoStoppedNow));
        assertTrue(sessionsUpdatedLastMinute.contains(stoppedSessionSubmitted61SecondsAgoStoppedNow));
        assertTrue(sessionsUpdatedLastMinute.contains(stoppedSessionSubmitted2MinutesAgoStoppedNow));
        assertFalse(sessionsUpdatedLastMinute.contains(inProgressSessionSubmitted61SecondsAgo));
        assertFalse(sessionsUpdatedLastMinute.contains(inProgressSessionSubmitted2MinutesAgo));
        assertFalse(sessionsUpdatedLastMinute.contains(stoppedSessionSubmitted61SecondsAgoStopped61SecondsAgo));
        assertFalse(sessionsUpdatedLastMinute.contains(stoppedSessionSubmitted2MinutesAgoStopped2MinutesAgo));
    }

    @Test
    void testGivenNoSessionsUpdatedInLast60Seconds_WhenGetSessionsUpdatedLastMinute_ThenReturnEmptyList() {
        LocalDateTime now = LocalDateTime.now();
        ChargingSession inProgressSessionSubmitted2MinutesAgo = createInProgressSession(now.minusMinutes(2L));
        ChargingSession stoppedSessionSubmitted2MinutesAgoStopped2MinutesAgo = createStoppedSession(now.minusMinutes(2L), now.minusMinutes(2L));

        ArrayList<ChargingSession> sessionsUpdatedLastMinute = sessionsStore.getSessionsUpdatedLastMinute();
        assertTrue(sessionsUpdatedLastMinute.isEmpty());
    }

    private ChargingSession createInProgressSession(LocalDateTime time) {
        return sessionsStore.addSession("ABC-12345", time);
    }

    private ChargingSession createStoppedSession(LocalDateTime startTime, LocalDateTime stopTime) {
        ChargingSession session = createInProgressSession(startTime);
        sessionsStore.stopSession(session.getId().toString(), stopTime);
        return session;
    }

}
