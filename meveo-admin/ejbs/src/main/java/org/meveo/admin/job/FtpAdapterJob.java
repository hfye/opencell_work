package org.meveo.admin.job;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.commons.utils.ParamBean;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.admin.User;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.job.Job;

@Startup
@Singleton
public class FtpAdapterJob extends Job {

	@Inject
	FtpAdapterJobBean ftpAdapterJobBean;

	@Inject
	private ResourceBundle resourceMessages;

	@Override
	@Asynchronous
	public void execute(JobInstance jobInstance, User currentUser) {
		super.execute(jobInstance, currentUser);
	}

	@Override
	@Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
	protected void execute(JobExecutionResultImpl result, JobInstance jobInstance, User currentUser) throws BusinessException {
		String distDirectory = null;
		String remoteServer = null;
		int remotePort = 21;
		String removeDistantFile = null;
		String ftpInputDirectory = null;
		String ftpExtension = null;
		String ftpUsername = null;
		String ftpPassword = null;
		String ftpProtocol = null;
		
		try {
			distDirectory = ParamBean.getInstance().getProperty("providers.rootDir", "/tmp/meveo/") + File.separator + currentUser.getProvider().getCode() + ((String) jobInstance.getCFValue("FtpAdapterJob_distDirectory")).replaceAll("\\..", "");
		    remoteServer = (String) jobInstance.getCFValue("FtpAdapterJob_remoteServer");
			remotePort = ((Long) jobInstance.getCFValue("FtpAdapterJob_remotePort")).intValue();
			removeDistantFile = (String) jobInstance.getCFValue("FtpAdapterJob_removeDistantFile");
			ftpInputDirectory = (String) jobInstance.getCFValue("FtpAdapterJob_ftpInputDirectory");
			ftpExtension = (String) jobInstance.getCFValue("FtpAdapterJob_fileExtension");
			ftpUsername = (String) jobInstance.getCFValue("FtpAdapterJob_ftpUsername");
			ftpPassword = (String) jobInstance.getCFValue("FtpAdapterJob_ftpPassword");
			ftpProtocol = (String) jobInstance.getCFValue("FtpAdapterJob_ftpProtocol");

		} catch (Exception e) {
			log.warn("Cant get customFields for " + jobInstance.getJobTemplate(), e);
		}
		ftpAdapterJobBean.execute(result, jobInstance, currentUser, distDirectory, remoteServer, remotePort, "true".equalsIgnoreCase(removeDistantFile), ftpInputDirectory,ftpExtension, ftpUsername, ftpPassword,ftpProtocol);
	}

	@Override
	public JobCategoryEnum getJobCategory() {
		return JobCategoryEnum.UTILS;
	}

	@Override
	public Map<String, CustomFieldTemplate> getCustomFields() {
		Map<String, CustomFieldTemplate> result = new HashMap<String, CustomFieldTemplate>();

		CustomFieldTemplate distDirectory = new CustomFieldTemplate();
		distDirectory.setCode("FtpAdapterJob_distDirectory");
		distDirectory.setAppliesTo("JOB_FtpAdapterJob");
		distDirectory.setActive(true);
		distDirectory.setDescription(resourceMessages.getString("FtpAdapter.distDirectory"));
		distDirectory.setFieldType(CustomFieldTypeEnum.STRING);
		distDirectory.setValueRequired(true);
		distDirectory.setMaxValue(150L);
		result.put("FtpAdapterJob_distDirectory", distDirectory);
		

		CustomFieldTemplate remoteServer = new CustomFieldTemplate();
		remoteServer.setCode("FtpAdapterJob_remoteServer");
		remoteServer.setAppliesTo("JOB_FtpAdapterJob");
		remoteServer.setActive(true);
		remoteServer.setDescription(resourceMessages.getString("FtpAdapter.remoteServer"));
		remoteServer.setFieldType(CustomFieldTypeEnum.STRING);
		remoteServer.setValueRequired(true);
		remoteServer.setMaxValue(150L);
		result.put("FtpAdapterJob_remoteServer", remoteServer);

		CustomFieldTemplate remotePort = new CustomFieldTemplate();
		remotePort.setCode("FtpAdapterJob_remotePort");
		remotePort.setAppliesTo("JOB_FtpAdapterJob");
		remotePort.setActive(true);
		remotePort.setDescription(resourceMessages.getString("FtpAdapter.remotePort"));
		remotePort.setFieldType(CustomFieldTypeEnum.LONG);
		remotePort.setValueRequired(true);
		result.put("FtpAdapterJob_remotePort", remotePort);

		CustomFieldTemplate removeDistantFile = new CustomFieldTemplate();
		removeDistantFile.setCode("FtpAdapterJob_removeDistantFile");
		removeDistantFile.setAppliesTo("JOB_FtpAdapterJob");
		removeDistantFile.setActive(true);
		removeDistantFile.setDescription(resourceMessages.getString("FtpAdapter.removeDistantFile"));
		removeDistantFile.setFieldType(CustomFieldTypeEnum.LIST);
		Map<String, String> removeDistantFileListValues = new HashMap<String, String>();
		removeDistantFileListValues.put("TRUE", "True");
		removeDistantFileListValues.put("FALSE", "False");
		removeDistantFile.setListValues(removeDistantFileListValues);
		removeDistantFile.setValueRequired(true);
		result.put("FtpAdapterJob_removeDistantFile", removeDistantFile);

		CustomFieldTemplate ftpInputDirectory = new CustomFieldTemplate();
		ftpInputDirectory.setCode("FtpAdapterJob_ftpInputDirectory");
		ftpInputDirectory.setAppliesTo("JOB_FtpAdapterJob");
		ftpInputDirectory.setActive(true);
		ftpInputDirectory.setDescription(resourceMessages.getString("FtpAdapter.ftpInputDirectory"));
		ftpInputDirectory.setFieldType(CustomFieldTypeEnum.STRING);
		ftpInputDirectory.setValueRequired(true);
		ftpInputDirectory.setMaxValue(100L);
		result.put("FtpAdapterJob_ftpInputDirectory", ftpInputDirectory);

		CustomFieldTemplate ftpUsername = new CustomFieldTemplate();
		ftpUsername.setCode("FtpAdapterJob_ftpUsername");
		ftpUsername.setAppliesTo("JOB_FtpAdapterJob");
		ftpUsername.setActive(true);
		ftpUsername.setDescription(resourceMessages.getString("FtpAdapter.ftpUsername"));
		ftpUsername.setFieldType(CustomFieldTypeEnum.STRING);
		ftpUsername.setValueRequired(true);
		ftpUsername.setMaxValue(50L);
		result.put("FtpAdapterJob_ftpUsername", ftpUsername);

		CustomFieldTemplate ftpPassword = new CustomFieldTemplate();
		ftpPassword.setCode("FtpAdapterJob_ftpPassword");
		ftpPassword.setAppliesTo("JOB_FtpAdapterJob");
		ftpPassword.setActive(true);
		ftpPassword.setDescription(resourceMessages.getString("FtpAdapter.ftpPassword"));
		ftpPassword.setFieldType(CustomFieldTypeEnum.STRING);
		ftpPassword.setValueRequired(true);
		ftpPassword.setMaxValue(50L);
		result.put("FtpAdapterJob_ftpPassword", ftpPassword);

		CustomFieldTemplate ftpExtension = new CustomFieldTemplate();
		ftpExtension.setCode("FtpAdapterJob_fileExtension");
		ftpExtension.setAppliesTo("JOB_FtpAdapterJob");
		ftpExtension.setActive(true);
		ftpExtension.setDescription(resourceMessages.getString("FtpAdapter.fileExtension"));
		ftpExtension.setFieldType(CustomFieldTypeEnum.STRING);
		ftpExtension.setValueRequired(true);
		ftpExtension.setMaxValue(50L);
		result.put("FtpAdapterJob_fileExtension", ftpExtension);
		
		CustomFieldTemplate ftpProtocol = new CustomFieldTemplate();
		ftpProtocol.setCode("FtpAdapterJob_ftpProtocol");
		ftpProtocol.setAppliesTo("JOB_FtpAdapterJob");
		ftpProtocol.setActive(true);
		ftpProtocol.setDescription(resourceMessages.getString("FtpAdapter.ftpProtocol"));
		ftpProtocol.setFieldType(CustomFieldTypeEnum.LIST);
		Map<String, String> ftpProtocolListValues = new HashMap<String, String>();
		ftpProtocolListValues.put("FTP", "FTP");
		ftpProtocolListValues.put("SFTP", "SFTP");
		ftpProtocol.setListValues(ftpProtocolListValues);
		ftpProtocol.setValueRequired(true);
		result.put("FtpAdapterJob_ftpProtocol", ftpProtocol);	
		
		return result;
	}
}