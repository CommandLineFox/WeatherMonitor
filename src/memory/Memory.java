package memory;

import lombok.Getter;
import lombok.Setter;
import types.Job;
import types.ParsedData;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class Memory {
    private static volatile Memory instance = null;

    private final BlockingQueue<Job> jobQueue;
    private final ConcurrentHashMap<Character, ParsedData> data;
    private final Lock logFileLock;

    @Setter
    private Thread cliThread;
    @Setter
    private Thread logThread;
    @Setter
    private Thread jobDispatcherThread;
    @Setter
    private Thread directoryMonitorThread;
    @Setter
    private Thread periodicMonitorThread;

    private Memory() {
        jobQueue = new LinkedBlockingQueue<>();
        data = new ConcurrentHashMap<>();

        logFileLock = new ReentrantLock();
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
