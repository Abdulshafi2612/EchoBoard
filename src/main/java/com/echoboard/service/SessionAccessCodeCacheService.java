package com.echoboard.service;

import com.echoboard.entity.Session;

public interface SessionAccessCodeCacheService {

    Session getSessionByAccessCode(String accessCode);
}