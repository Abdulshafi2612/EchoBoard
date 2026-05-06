package com.echoboard.dto.poll;

import com.echoboard.enums.PollType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatePollRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String title;

    @NotNull
    private PollType type;

    @NotEmpty
    @Size(min = 2, max = 10)
    private List<@Valid PollOptionRequest> options;

}
