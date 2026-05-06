package com.echoboard.dto.poll;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PollOptionRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String text;
}
