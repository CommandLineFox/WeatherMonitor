package memory;

import lombok.Getter;
import types.Job;
import types.ParsedData;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Getter
public class Memory {
    private static volatile Memory instance = null;
    private final BlockingQueue<Job> jobQueue;
    private final ConcurrentHashMap<Character, ParsedData> data = new ConcurrentHashMap<>();

    private Memory() {
        jobQueue = new LinkedBlockingQueue<>();
    }

    public static Memory getInstance() {
        if (instance == null) {
            synchronized (Memory.class) {
                if (instance == null) {
                    instance = new Memory();
                }
            }
        }
        return instance;
    }
}