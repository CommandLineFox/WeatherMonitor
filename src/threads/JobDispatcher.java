package threads;

import types.Job;
import types.JobType;

import java.util.concurrent.*;

public class JobDispatcher implements Runnable {
    private final BlockingQueue<Job> jobQueue;
    private final ExecutorService fileExecutor;
    private final ExecutorService generalExecutor;

    public JobDispatcher(int fileThreads, int generalThreads, BlockingQueue<Job> jobQueue) {
        this.jobQueue = jobQueue;
        this.fileExecutor = Executors.newFixedThreadPool(fileThreads);
        this.generalExecutor = Executors.newFixedThreadPool(generalThreads);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Job job = jobQueue.take();

                if (job.getType() == JobType.POISON_PILL) {
                    shutdownExecutors();
                    break;
                }

                if (job.getType() == JobType.READ_FILE) {
                    System.out.println("Dodat za citanje");
                    fileExecutor.submit(job::execute);
                } else {
                    System.out.println("Dodat za izvrsavanje");
                    generalExecutor.submit(job::execute);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void shutdownExecutors() {
        fileExecutor.shutdown();
        generalExecutor.shutdown();
    }

    public void submitJob(Job job) throws InterruptedException {
        jobQueue.put(job);
    }
}