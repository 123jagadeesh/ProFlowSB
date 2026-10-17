package com.proflow.dto.zoom;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.proflow.dto.auth.UserSummaryDto;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingResponse {
    private Long id;
    private String topic;
    private Instant startTime;
    private Integer duration;
    private String joinUrl;
    private String startUrl;
    private UserSummaryDto creator;
    private List<UserSummaryDto> invitees;
    private Long company;
    private Instant createdAt;

    @JsonProperty("_id")
    public String getUnderscoreId() {
        return id != null ? String.valueOf(id) : null;
    }
}
