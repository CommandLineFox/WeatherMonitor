package jobs;

import types.JobStatus;
import utility.Memory;
import types.Job;
import types.JobType;
import types.ParsedData;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

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

        memory.getLogFileLock().lock();
        try (FileWriter writer = new FileWriter(filePath, false)) {
            writer.write("Letter,Station count,Sum\n");

            for (Map.Entry<Character, ParsedData> entry : memory.getData().entrySet()) {
                char letter = entry.getKey();
                ParsedData data = entry.getValue();
                writer.write(letter + "," + data.getAppearanceCount() + "," + String.format("%.2f", data.getValueSum()) + "\n");
            }

            System.out.println("Data written into file: " + filePath);
        } catch (IOException e) {
            System.err.println("Error during writing into file: " + e.getMessage());
        } finally {
            memory.getLogFileLock().unlock();
        }
    }
}