package com.echoboard.dto.participant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class JoinSessionRequest {

    @NotBlank
    @Size(min=6, max=6)
    private String accessCode;

    @Size(max=100)
    private String displayName;
}
