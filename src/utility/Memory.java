package utility;

import lombok.Getter;
import lombok.Setter;
import threads.Cli;
import threads.DirectoryMonitor;
import threads.JobDispatcher;
import threads.PeriodicReport;
import types.Job;
import types.ParsedData;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class Memory {
    private static volatile Memory instance = null;

    private final ConcurrentHashMap<String, Job> jobHistory;
    private final BlockingQueue<Job> jobQueue;
    private final ConcurrentHashMap<Character, ParsedData> data;
    private final Lock logFileLock;

    private final AtomicBoolean running;

    @Setter
    private String searchDirPath;

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
    @Setter
    private Cli cli;
    @Setter
    private DirectoryMonitor directoryMonitor;
    @Setter
    private JobDispatcher jobDispatcher;
    @Setter
    private PeriodicReport periodicReport;

    private Memory() {
        jobHistory = new ConcurrentHashMap<>();
        jobQueue = new LinkedBlockingQueue<>();
        data = new ConcurrentHashMap<>();

        logFileLock = new ReentrantLock();
        running = new AtomicBoolean(true);
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
