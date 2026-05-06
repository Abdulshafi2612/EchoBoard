package com.echoboard.dto.poll;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PollOptionResponse {

    private Long id;

    private String text;

    private int voteCount;
}
