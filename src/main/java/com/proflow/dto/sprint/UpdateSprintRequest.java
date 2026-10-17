package com.proflow.dto.sprint;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSprintRequest {
    private String title;
    private String goal;
    private Integer duration;
    private Instant startDate;
    private Instant endDate;
    private String status;
}
