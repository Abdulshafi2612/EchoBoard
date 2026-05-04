package com.echoboard.dto.session;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSessionRequest {

    @Size(max = 150)
    private String title;

    @Size(max = 1000)
    private String description;

    private Boolean moderationEnabled;

    private Boolean anonymousAllowed;
}
