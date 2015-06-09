package org.meveo.api.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseApi;
import org.meveo.api.MeveoApiErrorCode;
import org.meveo.api.dto.job.JobInstanceDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.TimerEntity;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.job.Job;
import org.meveo.service.job.JobInstanceService;
import org.meveo.service.job.TimerEntityService;

@Stateless
public class JobInstanceApi extends BaseApi {

	@Inject
	private JobInstanceService jobInstanceService;
	
	@Inject
	private TimerEntityService timerEntityService;

	@Inject
	private CustomFieldTemplateService customFieldTemplateService;

	public void create(JobInstanceDto jobInstanceDto, User currentUser) throws MeveoApiException {
		if (StringUtils.isBlank(jobInstanceDto.getJobCategory()) || StringUtils.isBlank(jobInstanceDto.getJobTemplate()) || StringUtils.isBlank(jobInstanceDto.getCode())) {
			if (StringUtils.isBlank(jobInstanceDto.getJobCategory())) {
				missingParameters.add("JobCategory");
			}
			if ( StringUtils.isBlank(jobInstanceDto.getJobTemplate())) {
				missingParameters.add("JobTemplate");
			}
			if (StringUtils.isBlank(jobInstanceDto.getCode())) {
				missingParameters.add("Code");
			}			
			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}

		Provider provider = currentUser.getProvider();

		if (jobInstanceService.findByCode(jobInstanceDto.getCode(), provider) != null) {
			throw new EntityAlreadyExistsException(JobInstance.class, jobInstanceDto.getCode());
		}
		JobCategoryEnum jobCategory = null;
		try {
			jobCategory = JobCategoryEnum.valueOf(jobInstanceDto.getJobCategory().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new MeveoApiException(MeveoApiErrorCode.BUSINESS_API_EXCEPTION, "Invalid job category=" + jobInstanceDto.getJobCategory());
		}
		if (jobCategory == null) {
			throw new MeveoApiException(MeveoApiErrorCode.BUSINESS_API_EXCEPTION, "Invalid job name=" + jobInstanceDto.getJobTemplate());
		}

		List<CustomFieldTemplate> customFieldTemplates = new ArrayList<CustomFieldTemplate>();
		HashMap<String, String> jobs = new HashMap<String, String>();
		jobs = JobInstanceService.jobEntries.get(jobCategory);

		// Create or add missing custom field templates for a job
		if (jobs.containsKey(jobInstanceDto.getJobTemplate())) {
			customFieldTemplates.clear();
			Job job = jobInstanceService.getJobByName(jobInstanceDto.getJobTemplate());
			if (job.getCustomFields(currentUser) != null) {
				customFieldTemplates = customFieldTemplateService.findByJobName(jobInstanceDto.getJobTemplate());
				if (customFieldTemplates != null && customFieldTemplates.size() != job.getCustomFields(currentUser).size()) {
					for (CustomFieldTemplate cf : job.getCustomFields(currentUser)) {
						if (!customFieldTemplates.contains(cf)) {
							try {
								customFieldTemplateService.create(cf);
								customFieldTemplates.add(cf);
							} catch (BusinessException e) {
								log.error("Failed  to init custom fields", e);
							}
						}
					}
				}
			}
		}
		
		TimerEntity timerEntity = timerEntityService.findByCode(jobInstanceDto.getTimerCode(), provider);
		JobInstance jobInstance = new JobInstance();
		jobInstance.setUserId(currentUser.getId());
		jobInstance.setActive(jobInstanceDto.isActive());
		jobInstance.setParametres(jobInstanceDto.getParameter());  
		jobInstance.setJobCategoryEnum(jobCategory);
		jobInstance.setJobTemplate(jobInstanceDto.getJobTemplate());
		jobInstance.setCode(jobInstanceDto.getCode());
		jobInstance.setDescription(jobInstanceDto.getDescription());
		jobInstance.setTimerEntity(timerEntity);
		
		
		
		if (jobInstanceDto.getFollowingJobs() != null &&  jobInstanceDto.getFollowingJobs().isEmpty()) {
			for(String s : jobInstanceDto.getFollowingJobs().keySet() ){
				jobInstanceDto.getFollowingJobs().get(s);
				//JobInstance nextJob = jobInstanceService.findByCode(postData.getFollowingJobs().get(s), provider);
				//TODO
				// JobInstance nextJob = jobInstanceService.findByName(postData.getFollowingTimer(), provider);
				// jobInstance.setFollowingTimer(jobInstanceService.findByName(postData.getFollowingTimer(), provider));
				//if (nextJob == null) {
				//  throw new MeveoApiException(MeveoApiErrorCode.BUSINESS_API_EXCEPTION, "Invalid next job=" + postData.getFollowingTimer());
				//}
			}
		}

		if (jobInstanceDto.getCustomFields() != null) {
			// populate customFields
			if (jobInstanceDto.getCustomFields() != null) {
				try {
					populateCustomFields(customFieldTemplates, jobInstanceDto.getCustomFields().getCustomField(), jobInstance, "jobInstance", currentUser);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					log.error("Failed to associate custom field instance to an entity", e);
					throw new MeveoApiException("Failed to associate custom field instance to an entity");
				}
			}
		}

		jobInstanceService.create(jobInstance, currentUser, provider);
	}
}