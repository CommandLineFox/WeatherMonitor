import memory.Memory;
import threads.Cli;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Memory memory = Memory.getInstance();
        Cli cli = new Cli(memory.getJobQueue());

        memory.setCliThread(new Thread(cli));
        memory.getCliThread().start();
    }
}