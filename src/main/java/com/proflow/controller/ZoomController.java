package com.proflow.controller;

import com.proflow.dto.zoom.MeetingResponse;
import com.proflow.dto.zoom.ScheduleMeetingRequest;
import com.proflow.security.UserPrincipal;
import com.proflow.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/zoom")
@RequiredArgsConstructor
@Tag(name = "Zoom", description = "Zoom meeting scheduling and invitation emails")
public class ZoomController {

    private final MeetingService meetingService;

    @PostMapping("/schedule")
    @Operation(summary = "Schedule a Zoom meeting and send email invites to participants")
    public ResponseEntity<Map<String, Object>> scheduleMeeting(
            @Valid @RequestBody ScheduleMeetingRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        MeetingResponse meeting = meetingService.scheduleMeeting(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Meeting scheduled successfully",
                "meeting", meeting
        ));
    }
}
