package com.proflow.dto.project;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProjectStatusesRequest {

    @NotEmpty(message = "Statuses must be a non-empty array of strings.")
    private List<String> statuses;
}
