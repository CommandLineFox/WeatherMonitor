package jobs;

import types.Job;
import types.JobType;

public class ExportMapJob extends Job {
    public ExportMapJob(String name) {
        super(JobType.EXPORT_MAP, name);
    }

    @Override
    public void execute() {
        System.out.println("Export map job " + getName() + " executed.");
    }
}
