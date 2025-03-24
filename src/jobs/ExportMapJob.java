package jobs;

import memory.Memory;
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
        Memory memory = Memory.getInstance();

        memory.getLogFileLock().lock();
        try (FileWriter writer = new FileWriter(filePath, false)) {
            writer.write("Letter,Station count,Sum\n");

            for (Map.Entry<Character, ParsedData> entry : memory.getData().entrySet()) {
                char letter = entry.getKey();
                ParsedData data = entry.getValue();
                writer.write(letter + "," + data.getAppearanceCount() + "," + String.format("%.2f", data.getValueSum()) + "\n");
            }

            System.out.println("Podaci su uspešno eksportovani u fajl: " + filePath);
        } catch (IOException e) {
            System.err.println("Greška prilikom pisanja u fajl: " + e.getMessage());
        } finally {
            memory.getLogFileLock().unlock();
        }
    }
}