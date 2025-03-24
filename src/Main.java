import memory.Memory;
import threads.Cli;

public class Main {
    public static void main(String[] args) {
        Memory memory = Memory.getInstance();
        memory.setCli(new Cli());

        memory.setCliThread(new Thread(memory.getCli()));
        memory.getCliThread().start();
    }
}