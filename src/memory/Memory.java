package memory;

import lombok.Getter;
import lombok.Setter;
import types.Job;
import types.ParsedData;

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class Memory {
    private static volatile Memory instance = null;

    private final BlockingQueue<Job> jobQueue;
    private final ConcurrentHashMap<Character, ParsedData> data;

    private final ConcurrentHashMap<String, Boolean> processingFiles;
    private final ReentrantLock lock;
    private final Condition fileAvailable;

    @Setter
    private Thread cliThread;
    @Setter
    private Thread logThread;
    @Setter
    private Thread jobDispatcherThread;
    @Setter
    private Thread directoryMonitorThread;

    private Memory() {
        jobQueue = new LinkedBlockingQueue<>();
        data = new ConcurrentHashMap<>();

        processingFiles = new ConcurrentHashMap<>();
        lock = new ReentrantLock();
        fileAvailable = lock.newCondition();
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

    public boolean markFileAsProcessing(String fileName) {
        lock.lock();
        try {
            while (processingFiles.containsKey(fileName)) {
                try {
                    fileAvailable.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            processingFiles.put(fileName, true);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void unmarkFileAsProcessing(String fileName) {
        lock.lock();
        try {
            processingFiles.remove(fileName);
            fileAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
