package nl.knaw.huc.annorepo.jobs

import io.dropwizard.jobs.Job
import io.dropwizard.jobs.annotations.Every
import org.quartz.JobExecutionContext
import nl.knaw.huc.annorepo.api.IndexChoreIndex
import nl.knaw.huc.annorepo.api.SearchChoreIndex

@Every(value = "1h", jobName = "purgeExpiredChores")
class ExpiredChoresCleanerJob : Job() {

    override fun doJob(context: JobExecutionContext?) {
        SearchChoreIndex.purgeExpiredChores()
        IndexChoreIndex.purgeExpiredChores()
    }
}