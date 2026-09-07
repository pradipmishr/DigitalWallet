package com.project.digitalwallet.service;

import java.time.Duration;

public interface TokenBlacklistService {

    void blacklist(String jti, Duration ttl);

    boolean isBlacklisted(String jti);
}
