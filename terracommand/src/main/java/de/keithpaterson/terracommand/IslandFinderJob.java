package de.keithpaterson.terracommand;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.MethodInvokingTaskletAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Configuration
@EnableBatchProcessing
@EnableScheduling
@Component
public class IslandFinderJob {

	private static final String JOB_NAME = "findIslandsJob";

	Logger logger = Logger.getLogger(IslandFinderJob.class.getName());
	@Autowired
	IslandFinderTasklet tasklet;

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	public JobExplorer jobExplorer;
	@Autowired
	public JobRepository jobRepo;
	
	@Autowired
	private JobLauncher jobLauncher;

	@Scheduled(cron = "0 0 * * * ?")
	public void importTiles() throws Exception {
		logger.info(" Job Started at :" + new Date());
		JobParameters param = new JobParametersBuilder().addString("JobID", String.valueOf(System.currentTimeMillis()))
				.toJobParameters();
		JobExecution execution = jobLauncher.run(taskletJob(), param);
		logger.info("Job finished with status :" + execution.getStatus());
	}

	/**
	 * loadProperties(); buildIndex(); walkDirectory(); mergeIslands(); drawBoxes();
	 * 
	 * @return
	 */

	@Bean
	public Job taskletJob() {
//		killOld(JOB_NAME);
		return this.jobBuilderFactory.get(JOB_NAME).start(step1()).next(step3()).next(step4())
				.next(step5()).build();
	}

	
	public MethodInvokingTaskletAdapter createTasklet(String methodName) {
		MethodInvokingTaskletAdapter adapter = new MethodInvokingTaskletAdapter();

		adapter.setTargetObject(tasklet);
		adapter.setTargetMethod(methodName);

		return adapter;
	}

	@Bean
	public Step step1() {
		return this.stepBuilderFactory.get("loadProperties").tasklet(createTasklet("loadProperties"))
				.build();
	}

	@Bean
	public Step step2() {
		return this.stepBuilderFactory.get("buildIndex").tasklet(createTasklet("buildIndex"))
				.build();
	}

	@Bean
	public Step step3() {
		return this.stepBuilderFactory.get("walkDirectory").tasklet(createTasklet("walkDirectory"))
				.build();
	}

	@Bean
	public Step step4() {
		return this.stepBuilderFactory.get("mergeIslands").tasklet(createTasklet("mergeIslands"))
				.build();
	}

	@Bean
	public Step step5() {
		return this.stepBuilderFactory.get("drawBoxes").tasklet(createTasklet("drawBoxes"))
				.build();
	}

	private void killOld(String j) {
		try {
			List<JobInstance> findJobInstancesByJobName = jobExplorer.findJobInstancesByJobName(j, 0, 1000);
			for (JobInstance jobInstance : findJobInstancesByJobName) {
				List<org.springframework.batch.core.JobExecution> lastJobExecution = jobExplorer
						.getJobExecutions(jobInstance);
				for (org.springframework.batch.core.JobExecution jobExecution : lastJobExecution) {
					if (jobExecution.getStatus().equals(BatchStatus.COMPLETED)
							|| jobExecution.getStatus().equals(BatchStatus.FAILED))
						continue;
					for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
						stepExecution.setStatus(BatchStatus.FAILED);
						jobRepo.update(stepExecution);
					}

					jobExecution.setStatus(BatchStatus.FAILED);
					jobExecution.setEndTime(new Date());
					jobRepo.update(jobExecution);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
