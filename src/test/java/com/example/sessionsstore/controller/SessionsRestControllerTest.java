package com.example.sessionsstore.controller;

import com.example.sessionsstore.model.ChargingSession;
import com.example.sessionsstore.model.SessionsStore;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static com.example.sessionsstore.model.ChargingSession.Status.IN_PROGRESS;
import static com.example.sessionsstore.model.ChargingSession.Status.STOPPED;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionsRestController.class)
public class SessionsRestControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SessionsStore sessionsStore;

    @Test
    public void givenStationIdAndTimestamp_WhenSubmitSession_ThenReturnChargingSessionWithCode201() throws Exception {
        RestSubmitSessionRequest validRequest = new RestSubmitSessionRequest("ABC-12345", "2019-05-06T19:00:20.529");
        LocalDateTime parsedTimestamp = LocalDateTime.parse(validRequest.getTimestamp(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        ChargingSession chargingSession = new ChargingSession(UUID.randomUUID(), validRequest.getStationId(), parsedTimestamp, null, IN_PROGRESS);
        Mockito.when(sessionsStore.addSession(validRequest.getStationId(), parsedTimestamp)).thenReturn(chargingSession);

        mvc.perform(post("/chargingSessions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(APPLICATION_JSON_VALUE)
                .content(new Gson().toJson(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(chargingSession.getId().toString())))
                .andExpect(jsonPath("$.stationId", is(chargingSession.getStationId())))
                .andExpect(jsonPath("$.timestamp", is(chargingSession.getStartedAt().toString())));
    }

    @Test
    public void givenStationIdIsEmpty_WhenSubmitSession_ThenReturnErrorCode400() throws Exception {
        RestSubmitSessionRequest invalidRequest = new RestSubmitSessionRequest("", "2019-05-06T19:00:20.529");
        mvc.perform(post("/chargingSessions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(APPLICATION_JSON_VALUE)
                .content(new Gson().toJson(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenTimestampIsEmpty_WhenSubmitSession_ThenReturnErrorCode400() throws Exception {
        RestSubmitSessionRequest invalidRequest = new RestSubmitSessionRequest("ABC-12345", "");
        mvc.perform(post("/chargingSessions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(APPLICATION_JSON_VALUE)
                .content(new Gson().toJson(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenTimeStampInWrongFormat_WhenSubmitSession_ThenReturnErrorCode400() throws Exception {
        RestSubmitSessionRequest invalidRequest = new RestSubmitSessionRequest("ABC-12345", "2019-99-99T19:00:20.529");
        mvc.perform(post("/chargingSessions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(APPLICATION_JSON_VALUE)
                .content(new Gson().toJson(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenValidSessionId_WhenStopSession_ThenReturnCode200() throws Exception {
        mvc.perform(put("/chargingSessions/" + UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    public void givenNotExistingSessionId_WhenStopSession_ThenReturnErrorCode404() throws Exception {
        UUID id = UUID.randomUUID();
        Mockito.doThrow(IllegalArgumentException.class).when(sessionsStore).stopSession(anyString(), any(LocalDateTime.class));
        mvc.perform(put("/chargingSessions/" + id.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void givenTwoSession_WhenGetAllSessions_ThenReturnListWithCode200() throws Exception {
        ChargingSession inProgressSession1 = new ChargingSession(UUID.randomUUID(), "ABC-12345", LocalDateTime.now(), null, IN_PROGRESS);
        ChargingSession stoppedSession1 = new ChargingSession(UUID.randomUUID(), "ABC-12345", LocalDateTime.now().minusMinutes(1), LocalDateTime.now(), STOPPED);
        ArrayList<ChargingSession> sessions = new ArrayList<>(Arrays.asList(inProgressSession1, stoppedSession1));
        Mockito.when(sessionsStore.getAllSessions()).thenReturn(sessions);

        mvc.perform(get("/chargingSessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    public void givenTwoInProgressAndTwoStoppedSessionsUpdatedLastMinute_WhenGetSessionsSummary_ThenReturnSummaryWithCode200() throws Exception {
        ChargingSession inProgressSession1 = new ChargingSession(UUID.randomUUID(), "ABC-12345", LocalDateTime.now(), null, IN_PROGRESS);
        ChargingSession inProgressSession2 = new ChargingSession(UUID.randomUUID(), "ABC-12345", LocalDateTime.now(), null, IN_PROGRESS);
        ChargingSession stoppedSession1 = new ChargingSession(UUID.randomUUID(), "ABC-12345", LocalDateTime.now().minusMinutes(1), LocalDateTime.now(), STOPPED);
        ChargingSession stoppedSession2 = new ChargingSession(UUID.randomUUID(), "ABC-12345", LocalDateTime.now().minusMinutes(1), LocalDateTime.now(), STOPPED);
        ArrayList<ChargingSession> sessions = new ArrayList<ChargingSession>(Arrays.asList(inProgressSession1, inProgressSession2, stoppedSession1, stoppedSession2));
        Mockito.when(sessionsStore.getSessionsUpdatedLastMinute()).thenReturn(sessions);

        mvc.perform(get("/chargingSessions/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", is(4)))
                .andExpect(jsonPath("$.startedCount", is(2)))
                .andExpect(jsonPath("$.stoppedCount", is(2)));
    }

    @Test
    public void givenNoSessionsUpdatedLastMinute_WhenGetSessionsSummary_ThenReturnSummaryWithCode200() throws Exception {
        Mockito.when(sessionsStore.getSessionsUpdatedLastMinute()).thenReturn(new ArrayList<>());

        mvc.perform(get("/chargingSessions/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", is(0)))
                .andExpect(jsonPath("$.startedCount", is(0)))
                .andExpect(jsonPath("$.stoppedCount", is(0)));
    }

}