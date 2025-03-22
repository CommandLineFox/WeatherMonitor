import memory.Memory;
import threads.Cli;

import java.util.concurrent.ExecutorService;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Memory memory = Memory.getInstance();
        Cli cliThread = new Cli(memory.getJobQueue());

        ExecutorService executorService = new
        cliThread.start();
    }
}
