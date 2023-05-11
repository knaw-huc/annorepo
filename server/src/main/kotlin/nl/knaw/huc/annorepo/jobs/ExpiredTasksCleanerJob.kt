package nl.knaw.huc.annorepo.jobs

import io.dropwizard.jobs.Job
import io.dropwizard.jobs.annotations.Every
import org.quartz.JobExecutionContext
import nl.knaw.huc.annorepo.api.SearchTaskIndex

@Every(value = "1h", jobName = "purgeExpiredTasks")
class ExpiredTasksCleanerJob : Job() {

    override fun doJob(context: JobExecutionContext?) {
        SearchTaskIndex.purgeExpiredTasks()
    }
}