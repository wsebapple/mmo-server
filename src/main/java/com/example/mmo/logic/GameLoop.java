package com.example.mmo.logic;

import com.example.mmo.world.World;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class GameLoop {
    private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private final World world;

    public GameLoop(World world) {
        this.world = world;
        start();
    }

    private void start() {
        long periodMs = 50; // 20 TPS
        ses.scheduleAtFixedRate(() -> {
            try {
                world.step(0.05f);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, periodMs, periodMs, TimeUnit.MILLISECONDS);
    }
}
