package com.example.sessionsstore;

import com.example.sessionsstore.model.ChargingSession;
import com.example.sessionsstore.model.SessionsStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import sun.reflect.Reflection;

import static com.example.sessionsstore.model.ChargingSession.Status.IN_PROGRESS;
import static com.example.sessionsstore.model.ChargingSession.Status.STOPPED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class SessionsStoreApplicationTests {

    private SessionsStore sessionsStore;

    private ChargingSession sessionSubmittedCurrentMinute1;
    private ChargingSession sessionSubmittedCurrentMinute2;
    private ChargingSession sessionSubmitted1MinutesAgo1;


    @BeforeEach
    public void setUp() {
        sessionsStore = new SessionsStore();
        sessionSubmittedCurrentMinute1 = new ChargingSession(UUID.randomUUID(), "ABC-12345", LocalDateTime.now());
        sessionSubmittedCurrentMinute2 = new ChargingSession(UUID.randomUUID(), "ABC-12345", LocalDateTime.now());
        sessionSubmitted1MinutesAgo1 = new ChargingSession(UUID.randomUUID(), "ABC-12345", LocalDateTime.now().minusMinutes(1L));
    }

    @Test
    public void testGivenNoSessionsInStoreUpdatedThisMinute_WhenAddSession_ThenAddSessionToStoreAndToIndex() {
        sessionsStore.addSession(sessionSubmittedCurrentMinute1, sessionSubmittedCurrentMinute1.getStartedAt());
        ArrayList<ChargingSession> sessions = sessionsStore.getAllSessions();
        assertEquals(1L, sessions.size());
        assertEquals(IN_PROGRESS, sessions.get(0).getStatus());
    }

    @Test
    public void testGiven1SessionInStoreUpdatedCurrentMinute_WhenAddSession_ThenAddSessionToStoreAndToIndex() {
        sessionsStore.addSession(sessionSubmittedCurrentMinute1, sessionSubmittedCurrentMinute1.getStartedAt());
        sessionsStore.addSession(sessionSubmittedCurrentMinute2, sessionSubmittedCurrentMinute2.getStartedAt());
        ArrayList<ChargingSession> sessions = sessionsStore.getAllSessions();
        assertEquals(2L, sessions.size());
    }

    @Test
    public void testGiven2SessionsInProgressAddedWithIntervalOfTwoMinutes_WhenStopThisSessions_ThenBothOfThemAreStopped() {
        sessionsStore.addSession(sessionSubmittedCurrentMinute1, sessionSubmittedCurrentMinute1.getStartedAt());
        sessionsStore.addSession(sessionSubmitted1MinutesAgo1, sessionSubmitted1MinutesAgo1.getStartedAt());

        sessionsStore.stopSession(sessionSubmittedCurrentMinute1.getId(), LocalDateTime.now());
        sessionsStore.stopSession(sessionSubmitted1MinutesAgo1.getId(), LocalDateTime.now());

        ArrayList<ChargingSession> sessions = sessionsStore.getAllSessions();
        assertEquals(2L, sessions.size());
        sessions.forEach(s -> assertEquals(ChargingSession.Status.STOPPED, s.getStatus()));
    }

    @Test
    void testGivenNoSessionWithGivenIdInStore_WhenStopThisSession_ThenThrowRuntimeException() {
        assertThrows(RuntimeException.class, () -> sessionsStore.stopSession(UUID.randomUUID(), LocalDateTime.now()));
    }

    @Test
    void testGivenEmptyStore_WhenGetAllSessions_ThenReturnEmptyList() {
        ArrayList<ChargingSession> sessions = sessionsStore.getAllSessions();
        assertTrue(sessions.isEmpty());
    }

    @Test
    public void testGivenSeveralSessionsStartedInDifferentTimeAndWithDifferentStatus_WhenGetSessionsUpdatedLastMinute_ThenValidLotsAreReturned() {
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
        ChargingSession session = new ChargingSession(UUID.randomUUID(), "ABC-12345", time, null, IN_PROGRESS);
        sessionsStore.addSession(session, session.getStartedAt());
        return session;
    }

    private ChargingSession createStoppedSession(LocalDateTime startTime, LocalDateTime stopTime) {
        ChargingSession session = createInProgressSession(startTime);
        sessionsStore.stopSession(session.getId(), stopTime);
        return session;

    }

}
