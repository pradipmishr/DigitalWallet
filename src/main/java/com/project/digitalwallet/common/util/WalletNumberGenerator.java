package com.project.digitalwallet.common.util;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class WalletNumberGenerator {

    public String generate() {

        String timestamp = String.valueOf(System.currentTimeMillis());

        String random = String.format("%04d",
                new Random().nextInt(10000));

        return "WAL" + timestamp + random;
    }
}