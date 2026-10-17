package com.proflow.dto.sprint;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SprintResponse {
    private Long id;
    private String title;
    private String name; // alias for title
    private String goal;
    private Integer duration;
    private Long project;
    private Instant startDate;
    private Instant endDate;
    private String status;
    private Long company;
    private List<Object> issues;
    private Instant createdAt;

    @JsonProperty("_id")
    public String getUnderscoreId() {
        return id != null ? String.valueOf(id) : null;
    }
}
