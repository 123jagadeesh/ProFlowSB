package com.proflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proflow.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoomService {

    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder().build();

    private String cachedToken = null;
    private long tokenExpiryTime = 0;

    private synchronized String getZoomAccessToken() {
        if (cachedToken != null && tokenExpiryTime > System.currentTimeMillis()) {
            return cachedToken;
        }

        try {
            ApplicationProperties.Zoom zoom = applicationProperties.getZoom();
            String tokenUrl = String.format("https://zoom.us/oauth/token?grant_type=account_credentials&account_id=%s",
                    zoom.getAccountId());

            String authHeader = Base64.getEncoder().encodeToString(
                    (zoom.getClientId() + ":" + zoom.getClientSecret()).getBytes(StandardCharsets.UTF_8));

            String response = restClient.post()
                    .uri(tokenUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + authHeader)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .retrieve()
                    .body(String.class);

            JsonNode jsonNode = objectMapper.readTree(response);
            cachedToken = jsonNode.get("access_token").asText();
            int expiresIn = jsonNode.has("expires_in") ? jsonNode.get("expires_in").asInt() : 3600;
            tokenExpiryTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L;

            return cachedToken;
        } catch (Exception ex) {
            log.error("Error retrieving Zoom access token: {}", ex.getMessage());
            throw new RuntimeException("Failed to obtain Zoom access token: " + ex.getMessage(), ex);
        }
    }

    public ZoomMeetingDetails createZoomMeeting(String topic, Instant startTime, int durationMinutes) {
        try {
            String token = getZoomAccessToken();
            String hostEmail = applicationProperties.getZoom().getHostEmail();
            String meetingUrl = String.format("https://api.zoom.us/v2/users/%s/meetings", hostEmail);

            Map<String, Object> meetingPayload = new HashMap<>();
            meetingPayload.put("topic", topic);
            meetingPayload.put("type", 2); // Scheduled meeting
            meetingPayload.put("start_time", startTime.toString());
            meetingPayload.put("duration", durationMinutes);
            meetingPayload.put("timezone", "Asia/Kolkata");

            Map<String, Object> settings = new HashMap<>();
            settings.put("host_video", true);
            settings.put("participant_video", true);
            settings.put("join_before_host", true);
            settings.put("waiting_room", false);
            settings.put("jbh_time", 0);
            meetingPayload.put("settings", settings);

            String response = restClient.post()
                    .uri(meetingUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(meetingPayload)
                    .retrieve()
                    .body(String.class);

            JsonNode jsonNode = objectMapper.readTree(response);
            String joinUrl = jsonNode.has("join_url") ? jsonNode.get("join_url").asText() : "";
            String startUrl = jsonNode.has("start_url") ? jsonNode.get("start_url").asText() : "";

            return new ZoomMeetingDetails(joinUrl, startUrl);
        } catch (Exception ex) {
            log.error("Error creating Zoom meeting: {}", ex.getMessage());
            throw new RuntimeException("Failed to schedule Zoom meeting: " + ex.getMessage(), ex);
        }
    }

    public record ZoomMeetingDetails(String joinUrl, String startUrl) {}
}
