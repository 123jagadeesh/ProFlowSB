package com.proflow.dto.zoom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleMeetingRequest {

    @NotBlank(message = "Topic is required")
    private String topic;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "Duration is required")
    private Integer duration;

    @NotEmpty(message = "At least one invitee is required")
    private List<Long> inviteeIds;
}
