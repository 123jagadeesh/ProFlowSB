package com.proflow.dto.sprint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSprintRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String goal;

    @NotNull(message = "Duration is required")
    private Integer duration;

    @NotNull(message = "Project ID is required")
    private Long project;

    @NotNull(message = "Start date is required")
    private Instant startDate;

    @NotNull(message = "End date is required")
    private Instant endDate;

    private Long company;
}
