package jobs;

import memory.Memory;
import types.Job;
import types.JobType;
import types.ParsedData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class ExportMapJob extends Job {
    private final String filePath;

    public ExportMapJob(String name, String filePath) {
        super(JobType.EXPORT_MAP, name);
        this.filePath = filePath;
    }

    @Override
    public void execute() {
        System.out.println("Export map job " + getName() + " executed.");

        Memory.getInstance().getLogFileLock().lock();
        try {
            File logFile = new File(filePath);

            if (!logFile.exists()) {
                boolean created = logFile.createNewFile();
                if (created) {
                    System.out.println("Fajl " + filePath + " je kreiran.");
                } else {
                    System.out.println("Fajl " + filePath + " već postoji.");
                }
            }

            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write("Letter,Station count,Sum\n");

                for (Map.Entry<Character, ParsedData> entry : Memory.getInstance().getData().entrySet()) {
                    char letter = entry.getKey();
                    ParsedData data = entry.getValue();

                    writer.write(letter + "," + data.getAppearanceCount() + "," + data.getValueSum() + "\n");
                }

                System.out.println("Podaci su uspešno eksportovani u fajl: " + filePath);

            } catch (IOException e) {
                System.err.println("Greška prilikom obrade fajla: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("Greška prilikom rada sa fajlom: " + e.getMessage());
        } finally {
            Memory.getInstance().getLogFileLock().unlock();
        }
    }
}