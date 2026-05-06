package com.echoboard.repository;

import com.echoboard.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    List<PollOption> findByPoll_Id(Long pollId);
}
