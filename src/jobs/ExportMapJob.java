package jobs;

import types.JobStatus;
import utility.Memory;
import types.Job;
import types.JobType;
import types.ParsedData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class ExportMapJob extends Job implements Serializable {
    private final String filePath;

    public ExportMapJob(String name, String filePath) {
        super(JobType.EXPORT_MAP, name);
        this.filePath = filePath;
    }

    @Override
    public void execute() {
        setJobStatus(JobStatus.RUNNING);
        Memory memory = Memory.getInstance();
        memory.getJobHistory().put(this.getName(), this);

        Map<Character, ParsedData> snapshot;
        ReentrantLock lock = memory.getLogFileLock();
        lock.lock();
        try {
            snapshot = Map.copyOf(memory.getData());
        } finally {
            lock.unlock();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            writer.write("Letter,Station count,Sum\n");

            for (Map.Entry<Character, ParsedData> entry : snapshot.entrySet()) {
                char letter = entry.getKey();
                ParsedData data = entry.getValue();
                writer.write(letter + "," + data.getAppearanceCount() + "," + String.format("%.2f", data.getValueSum()) + "\n");
            }

            System.out.println("Data successfully written into file: " + filePath);
        } catch (IOException e) {
            System.err.println("Error during writing into file: " + e.getMessage());
        }
    }
}