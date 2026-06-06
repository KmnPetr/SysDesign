package com.messenger.loadtest.creators;

import com.messenger.loadtest.LoadtestApplication;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class MessagesCreator {

    private int getCountMsg() {
        return ThreadLocalRandom.current().nextInt(
                LoadtestApplication.MIN_COUNT_MSG,
                LoadtestApplication.MAX_COUNT_MSG + 1
        );
    }
}
