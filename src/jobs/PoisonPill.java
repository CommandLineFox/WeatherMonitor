package jobs;

import types.Job;
import types.JobType;

public class PoisonPill extends Job {
    public PoisonPill() {
        super(JobType.POISON_PILL, "Poison Pill");
    }

    @Override
    public void execute() {

    }
}
