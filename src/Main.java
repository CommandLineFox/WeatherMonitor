import memory.Memory;
import threads.Cli;

public class Main {
    public static void main(String[] args) {
        Memory memory = Memory.getInstance();
        Cli cli = new Cli();

        memory.setCliThread(new Thread(cli));
        memory.getCliThread().start();
    }
}