/*
 * (C) Copyright 2009-2013 Manaty SARL (http://manaty.net/) and contributors.
 *
 * Licensed under the GNU Public Licence, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/gpl-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.meveo.admin.action;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.commons.utils.ParamBean;
import org.meveo.model.billing.InvoiceSubcategoryCountry;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.Tax;
import org.meveo.model.catalog.ChargeTemplate;
import org.meveo.service.billing.impl.InvoiceSubCategoryCountryService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.catalog.impl.ChargeTemplateServiceAll;

@Named
@SessionScoped
public class JavaScriptAction implements Serializable, JavaScriptActionLocal {
	private static final long serialVersionUID = 1566090776694906061L;

	@Inject
	private ParamBean paramBean;

	@Inject
	private ChargeTemplateServiceAll chargeTemplateService;

	@Inject
	private SubscriptionService subscriptionService;

	@Inject
	InvoiceSubCategoryCountryService invoiceSubCategoryCountryService;

	public String calculateOneShotChargeInstanceAmount(String subscriptionId,
			String chargeTemplateCode, String amountWithoutTaxString) {
		BigDecimal amountWithoutTax = bigDecimalConverterAsObject(amountWithoutTaxString);
		ChargeTemplate chargeTemplate = (ChargeTemplate) chargeTemplateService
				.findByCode(chargeTemplateCode);
		Subscription subscription = subscriptionService.findById(Long.valueOf(subscriptionId));
		// If there are values
		if (subscription != null && chargeTemplate != null && amountWithoutTax != null) {

			InvoiceSubcategoryCountry invoiceSubcategoryCountry = invoiceSubCategoryCountryService
					.findInvoiceSubCategoryCountry(chargeTemplate.getInvoiceSubCategory().getId(),
							subscription.getUserAccount().getBillingAccount().getTradingCountry()
									.getId());
			Tax tax = invoiceSubcategoryCountry.getTax();
			BigDecimal calculatedAmount = amountWithoutTax.multiply(tax.getPercent())
					.divide(new BigDecimal(100)).add(amountWithoutTax)
					.setScale(2, RoundingMode.HALF_UP);
			return getBigDecimalAsString(calculatedAmount);
		}
		return null;
	}

	public String calculateOneShotChargeInstanceAmountWithoutTax(String subscriptionId,
			String chargeTemplateCode, String amount2String) {
		BigDecimal amount2 = bigDecimalConverterAsObject(amount2String);
		ChargeTemplate chargeTemplate = (ChargeTemplate) chargeTemplateService
				.findByCode(chargeTemplateCode);
		Subscription subscription = subscriptionService.findById(Long.valueOf(subscriptionId));
		// If there are values

		if (subscription != null && chargeTemplate != null & amount2 != null) {
			InvoiceSubcategoryCountry invoiceSubcategoryCountry = invoiceSubCategoryCountryService
					.findInvoiceSubCategoryCountry(chargeTemplate.getInvoiceSubCategory().getId(),
							subscription.getUserAccount().getBillingAccount().getTradingCountry()
									.getId());

			Tax tax = invoiceSubcategoryCountry.getTax();
			BigDecimal aa = BigDecimal.ONE.add(tax.getPercent().divide(new BigDecimal(100)));
			BigDecimal calculatedAmountWithoutTax = amount2.divide(aa, 2, RoundingMode.HALF_UP);
			return getBigDecimalAsString(calculatedAmountWithoutTax);
		}
		return null;
	}

	private BigDecimal bigDecimalConverterAsObject(String str) {
		DecimalFormat df = (DecimalFormat) NumberFormat.getInstance();
		df.setParseBigDecimal(true);
		BigDecimal bd = null;
		try {
			bd = (BigDecimal) df.parse(str);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		bd.setScale(2, RoundingMode.HALF_UP);
		return bd;
	}

	public String getBigDecimalAsString(BigDecimal value) {
		String pattern = paramBean.getProperty("bigDecimal.format");
		DecimalFormat format = new DecimalFormat(pattern);
		String bigDecimalString = format.format(value);
		return bigDecimalString;

	}

	public String getFormatedAmountString(String value) {
		return getBigDecimalAsString(bigDecimalConverterAsObject(value));
	}

}