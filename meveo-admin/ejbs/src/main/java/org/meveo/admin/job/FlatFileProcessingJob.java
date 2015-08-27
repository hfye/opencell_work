package org.meveo.admin.job;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.meveo.admin.async.FlatFileProcessingAsync;
import org.meveo.admin.async.SubListCreator;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.commons.utils.FileUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.admin.User;
import org.meveo.model.crm.AccountLevelEnum;
import org.meveo.model.crm.CustomFieldInstance;
import org.meveo.model.crm.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.job.Job;

@Startup
@Singleton
public class FlatFileProcessingJob extends Job {

	@Inject
	private FlatFileProcessingAsync flatFileProcessingAsync;

	@Inject
	private ResourceBundle resourceMessages;

	@Override
	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void execute(JobInstance jobInstance, User currentUser) {
		super.execute(jobInstance, currentUser);
	}

	@SuppressWarnings("unchecked")
	@Override
	@TransactionAttribute(TransactionAttributeType.NEVER)
	protected void execute(JobExecutionResultImpl result, JobInstance jobInstance, User currentUser) throws BusinessException {
		try {
			Long nbRuns = new Long(1);
			Long waitingMillis = new Long(0);
			String mappingConf = null;
			String inputDir = null, scriptInstanceFlowCode = null;
			String fileNameExtension = null;
			String recordVariableName = null;
			String originFilename = null;
			Map<String, Object> context = new HashMap<String, Object>();
			try {
				nbRuns = jobInstance.getLongCustomValue("FlatFileProcessingJob_nbRuns").longValue();
				waitingMillis = jobInstance.getLongCustomValue("FlatFileProcessingJob_waitingMillis").longValue();
				if (nbRuns == -1) {
					nbRuns = (long) Runtime.getRuntime().availableProcessors();
				}
				recordVariableName = jobInstance.getStringCustomValue("FlatFileProcessingJob_recordVariableName");
				originFilename = jobInstance.getStringCustomValue("FlatFileProcessingJob_originFilename");
				context = new HashMap<String, Object>();
				CustomFieldInstance variablesCFI = jobInstance.getCustomFields().get("FlatFileProcessingJob_variables");
				if (variablesCFI != null) {
					context = variablesCFI.getMapValue();
				}
				mappingConf = jobInstance.getStringCustomValue("FlatFileProcessingJob_mappingConf");
				inputDir = ParamBean.getInstance().getProperty("providers.rootDir", "/tmp/meveo/") + File.separator + jobInstance.getProvider().getCode() + jobInstance.getStringCustomValue("FlatFileProcessingJob_inputDir").replaceAll("..", "");
				fileNameExtension = jobInstance.getStringCustomValue("FlatFileProcessingJob_fileNameExtension");
				scriptInstanceFlowCode = jobInstance.getStringCustomValue("FlatFileProcessingJob_scriptsFlow");

			} catch (Exception e) {
				log.warn("Cant get customFields for " + jobInstance.getJobTemplate());
			}

			ArrayList<String> fileExtensions = new ArrayList<String>();
			fileExtensions.add(fileNameExtension);

			File f = new File(inputDir);
			if (!f.exists()) {
				f.mkdirs();
			}
			File[] files = FileUtils.getFilesForParsing(inputDir, fileExtensions);
			if (files == null || files.length == 0) {
				return;
			}
			SubListCreator subListCreator = new SubListCreator(Arrays.asList(files), nbRuns.intValue());

			List<Future<String>> futures = new ArrayList<Future<String>>();
			while (subListCreator.isHasNext()) {
				futures.add(flatFileProcessingAsync.launchAndForget((List<File>) subListCreator.getNextWorkSet(), result, inputDir, currentUser, mappingConf, scriptInstanceFlowCode, recordVariableName, context, originFilename));
				if (subListCreator.isHasNext()) {
					try {
						Thread.sleep(waitingMillis.longValue());
					} catch (InterruptedException e) {
						log.error("", e);
					}
				}
			}
			// Wait for all async methods to finish
			for (Future<String> future : futures) {
				try {
					future.get();
				} catch (InterruptedException e) {
					// It was cancelled from outside - no interest

				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					result.registerError(cause.getMessage());
					log.error("Failed to execute async method", cause);
				}
			}

		} catch (Exception e) {
			log.error("Failed to run mediation", e);
			result.registerError(e.getMessage());
		}
	}

	@Override
	public JobCategoryEnum getJobCategory() {
		return JobCategoryEnum.FLAT_FILE_PROCESSING;
	}

	@Override
	public List<CustomFieldTemplate> getCustomFields() {
		List<CustomFieldTemplate> result = new ArrayList<CustomFieldTemplate>();

		CustomFieldTemplate nbRuns = new CustomFieldTemplate();
		nbRuns.setCode("FlatFileProcessingJob_nbRuns");
		nbRuns.setAccountLevel(AccountLevelEnum.TIMER);
		nbRuns.setActive(true);
		nbRuns.setDescription(resourceMessages.getString("jobExecution.nbRuns"));
		nbRuns.setFieldType(CustomFieldTypeEnum.LONG);
		nbRuns.setDefaultValue("1");
		nbRuns.setValueRequired(false);
		result.add(nbRuns);

		CustomFieldTemplate waitingMillis = new CustomFieldTemplate();
		waitingMillis.setCode("FlatFileProcessingJob_waitingMillis");
		waitingMillis.setAccountLevel(AccountLevelEnum.TIMER);
		waitingMillis.setActive(true);
		waitingMillis.setDescription(resourceMessages.getString("jobExecution.waitingMillis"));
		waitingMillis.setFieldType(CustomFieldTypeEnum.LONG);
		waitingMillis.setDefaultValue("0");
		waitingMillis.setValueRequired(false);
		result.add(waitingMillis);

		CustomFieldTemplate inputDirectoryCF = new CustomFieldTemplate();
		inputDirectoryCF.setCode("FlatFileProcessingJob_inputDir");
		inputDirectoryCF.setAccountLevel(AccountLevelEnum.TIMER);
		inputDirectoryCF.setActive(true);
		inputDirectoryCF.setDescription(resourceMessages.getString("flatFile.inputDir"));
		inputDirectoryCF.setFieldType(CustomFieldTypeEnum.STRING);
		inputDirectoryCF.setDefaultValue(null);
		inputDirectoryCF.setValueRequired(true);
		result.add(inputDirectoryCF);

		CustomFieldTemplate fileNamePrefixCF = new CustomFieldTemplate();
		fileNamePrefixCF.setCode("FlatFileProcessingJob_fileNameExtension");
		fileNamePrefixCF.setAccountLevel(AccountLevelEnum.TIMER);
		fileNamePrefixCF.setActive(true);
		fileNamePrefixCF.setDescription(resourceMessages.getString("flatFile.fileNameExtension"));
		fileNamePrefixCF.setFieldType(CustomFieldTypeEnum.STRING);
		fileNamePrefixCF.setDefaultValue("csv");
		fileNamePrefixCF.setValueRequired(true);
		result.add(fileNamePrefixCF);

		CustomFieldTemplate mappingConf = new CustomFieldTemplate();
		mappingConf.setCode("FlatFileProcessingJob_mappingConf");
		mappingConf.setAccountLevel(AccountLevelEnum.TIMER);
		mappingConf.setActive(true);
		mappingConf.setDescription(resourceMessages.getString("flatFile.mappingConf"));
		mappingConf.setFieldType(CustomFieldTypeEnum.TEXT_AREA);
		mappingConf.setDefaultValue("");
		mappingConf.setValueRequired(true);
		result.add(mappingConf);

		// CustomFieldTemplate scriptInstanceFlowCF = new CustomFieldTemplate();
		// scriptInstanceFlowCF.setCode("FlatFileProcessingJob_scriptsFlow");
		// scriptInstanceFlowCF.setAccountLevel(AccountLevelEnum.TIMER);
		// scriptInstanceFlowCF.setActive(true);
		// scriptInstanceFlowCF.setDescription(resourceMessages.getString("mediation.scriptsFlow"));
		// scriptInstanceFlowCF.setFieldType(CustomFieldTypeEnum.ENTITY);
		// scriptInstanceFlowCF.setEntityClazz("org.meveo.model.jobs.ScriptInstance");
		// scriptInstanceFlowCF.setDefaultValue(null);
		// scriptInstanceFlowCF.setValueRequired(true);
		// result.add(scriptInstanceFlowCF);

		CustomFieldTemplate ss = new CustomFieldTemplate();
		ss.setCode("FlatFileProcessingJob_scriptsFlow");
		ss.setAccountLevel(AccountLevelEnum.TIMER);
		ss.setActive(true);
		ss.setDescription(resourceMessages.getString("flatFile.scriptsFlow"));
		ss.setFieldType(CustomFieldTypeEnum.STRING);
		ss.setDefaultValue(null);
		ss.setValueRequired(true);
		result.add(ss);

		CustomFieldTemplate variablesCF = new CustomFieldTemplate();
		variablesCF.setCode("FlatFileProcessingJob_variables");
		variablesCF.setAccountLevel(AccountLevelEnum.TIMER);
		variablesCF.setActive(true);
		variablesCF.setDescription("Init and finalize variables");
		variablesCF.setFieldType(CustomFieldTypeEnum.STRING);
		variablesCF.setStorageType(CustomFieldStorageTypeEnum.MAP);
		variablesCF.setValueRequired(false);
		result.add(variablesCF);

		CustomFieldTemplate recordVariableName = new CustomFieldTemplate();
		recordVariableName.setCode("FlatFileProcessingJob_recordVariableName");
		recordVariableName.setAccountLevel(AccountLevelEnum.CUST);
		recordVariableName.setActive(true);
		recordVariableName.setDefaultValue("record");
		recordVariableName.setDescription("Record variable name");
		recordVariableName.setFieldType(CustomFieldTypeEnum.STRING);
		recordVariableName.setValueRequired(false);
		result.add(recordVariableName);

		CustomFieldTemplate originFilename = new CustomFieldTemplate();
		originFilename.setCode("FlatFileProcessingJob_originFilename");
		originFilename.setAccountLevel(AccountLevelEnum.CUST);
		originFilename.setActive(true);
		originFilename.setDefaultValue("origin_filename");
		originFilename.setDescription("Filename variable name");
		originFilename.setFieldType(CustomFieldTypeEnum.STRING);
		originFilename.setValueRequired(false);
		result.add(originFilename);

		return result;
	}
}