import filemonitor.DirectoryMonitor;
import types.ReadFile;

import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        String dirPath = "C:\\Users\\Korisnik\\Documents\\Projects\\Fakultet\\KIDS\\KIDS_1\\testdata";
        BlockingQueue<ReadFile> queue = new LinkedBlockingQueue<>();

        DirectoryMonitor directoryMonitor = new DirectoryMonitor(dirPath, queue);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(directoryMonitor);

        executorService.shutdown();
    }
}
