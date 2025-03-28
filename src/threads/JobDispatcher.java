package threads;

import jobs.ReadFileJob;
import utility.Memory;
import types.Job;
import types.JobStatus;
import types.JobType;

import java.util.concurrent.*;

public class JobDispatcher implements Runnable {
    private final BlockingQueue<Job> jobQueue;
    private final ExecutorService fileExecutor;
    private final ExecutorService generalExecutor;
    private final ConcurrentHashMap<String, Future<?>> activeFileJobs;

    public JobDispatcher(int fileThreads, int generalThreads, BlockingQueue<Job> jobQueue) {
        this.jobQueue = jobQueue;
        this.fileExecutor = Executors.newFixedThreadPool(fileThreads);
        this.generalExecutor = Executors.newFixedThreadPool(generalThreads);
        this.activeFileJobs = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        Memory memory = Memory.getInstance();
        try {
            while (memory.getRunning().get()) {
                Job job = jobQueue.take();
                memory.getJobHistory().put(job.getName(), job);

                if (job.getType() == JobType.READ_FILE) {
                    handleReadFileJob((ReadFileJob) job);
                } else {
                    handleGeneralJob(job);
                }
            }
            if (!memory.getRunning().get()) {
                shutdownExecutors();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Metod za pokretanje poslova za citanje novih ili updatovanih fajlova
     *
     * @param job Job koji treba da se pokrene
     */
    private void handleReadFileJob(ReadFileJob job) {
        String filePath = job.getReadFile().getPath();

        try {
            Future<?> existingJob = activeFileJobs.get(filePath);
            if (existingJob != null) {
                System.out.println("Waiting for another job processing " + filePath);
                existingJob.get();
            }

            Future<?> future = fileExecutor.submit(() -> {
                try {
                    job.execute();
                } finally {
                    job.setJobStatus(JobStatus.COMPLETED);
                    Memory.getInstance().getJobHistory().put(job.getName(), job);
                    activeFileJobs.remove(filePath);
                }
            });

            activeFileJobs.put(filePath, future);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Metod za pokretanje ostalih poslova
     *
     * @param job Job koji treba da se pokrene
     */
    private void handleGeneralJob(Job job) {
        generalExecutor.submit(() -> {
            try {
                job.execute();
            } finally {
                job.setJobStatus(JobStatus.COMPLETED);
                Memory.getInstance().getJobHistory().put(job.getName(), job);
            }
        });
    }

    /**
     * Metod za gasenje executora
     */
    private void shutdownExecutors() {
        fileExecutor.shutdown();
        generalExecutor.shutdown();
    }
}