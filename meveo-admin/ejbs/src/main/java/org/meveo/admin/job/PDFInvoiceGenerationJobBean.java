package org.meveo.admin.job;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.async.PdfInvoiceAsync;
import org.meveo.admin.async.SubListCreator;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.admin.User;
import org.meveo.model.billing.Invoice;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.service.billing.impl.BillingRunService;
import org.meveo.service.billing.impl.InvoiceService;
import org.slf4j.Logger;

@Stateless
public class PDFInvoiceGenerationJobBean {

	@Inject
	private Logger log;

	@Inject
	private InvoiceService invoiceService;

	@Inject
	private PDFParametersConstruction pDFParametersConstruction;

	@Inject
	private PDFFilesOutputProducer pDFFilesOutputProducer;

	@Inject
	private BillingRunService billingRunService;

	@Inject
	private PdfInvoiceAsync pdfInvoiceAsync;

	@Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void execute(JobExecutionResultImpl result, String parameter,User currentUser) {
		List<Invoice> invoices = new ArrayList<Invoice>();

		if (parameter != null && parameter.trim().length() > 0) {
			try {
				invoices = invoiceService.getInvoices(billingRunService
						.getBillingRunById(Long.parseLong(parameter),
								currentUser.getProvider()));
			} catch (Exception e) {
				log.error(e.getMessage());
				result.registerError(e.getMessage());
			}
		} else {
			invoices = invoiceService.getValidatedInvoicesWithNoPdf(null,currentUser.getProvider());
		}

		log.info("PDFInvoiceGenerationJob number of invoices to process="+ invoices.size());
		try{
			Long nbRuns = null;//timerEntity.getLongCustomValue("nbRuns").longValue();
			Long waitingMillis = null;//timerEntity.getLongCustomValue("waitingMillis").longValue();

			if(nbRuns == null ){
				nbRuns = new Long(8);
			}
			if(waitingMillis == null ){
				waitingMillis = new Long(0);
			}

			SubListCreator subListCreator = new SubListCreator(invoices,nbRuns.intValue());

			while (subListCreator.isHasNext()) {
				pdfInvoiceAsync.launchAndForget((List<Invoice>) subListCreator.getNextWorkSet(),currentUser, result );
			}

		} catch (Exception e) {
			log.error(e.getMessage());
			result.registerError(e.getMessage());
			e.printStackTrace();
		}

	}

}
