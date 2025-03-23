package jobs;

import memory.Memory;
import types.Job;
import types.JobType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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

            // Dodajemo podatke u fajl
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write("Example Data\n");  // Primer podataka koji se eksportuju
            }

        } catch (IOException e) {
            System.err.println("Greška prilikom obrade fajla: " + e.getMessage());
        } finally {
            lock.unlock();  // Otključavanje nakon što se završi sa fajlom
        }
    }
}