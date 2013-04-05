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
package org.meveo.admin.action.admin;

import java.sql.BatchUpdateException;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.model.admin.Currency;
import org.meveo.model.billing.TradingCurrency;
import org.meveo.model.crm.Provider;
import org.meveo.service.admin.impl.TradingCurrencyService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.crm.impl.ProviderService;

/**
 * @author Marouane ALAMI
 */
@Named
@ConversationScoped
public class TradingCurrencyBean extends BaseBean<TradingCurrency> {

	private static final long serialVersionUID = 1L;

	@Inject
	private TradingCurrencyService tradingCurrencyService;

	@Inject
	ProviderService providerService;

	public TradingCurrencyBean() {
		super(TradingCurrency.class);
	}

	public TradingCurrency initEntity() {
		return super.initEntity();
	}

	public List<TradingCurrency> listAll() {
		getFilters();
		if (filters.containsKey("currencyCode")) {
			filters.put("currency.currencyCode", filters.get("currencyCode"));
			filters.remove("currencyCode");
		} else if (filters.containsKey("currency.currencyCode")) {
			filters.remove("currency.currencyCode");
		}
		return super.listAll();
	}

	public String saveOrUpdate() {
		String back = null;
		try {
			Provider currentProvider = providerService.findById(getCurrentProvider().getId());
			for (TradingCurrency tr : currentProvider.getTradingCurrencies()) {
				if (tr.getCurrency().getCurrencyCode()
						.equalsIgnoreCase(entity.getCurrency().getCurrencyCode())
						&& !tr.getId().equals(entity.getId())) {
					throw new Exception();
				}
			}
			currentProvider.addTradingCurrency(entity);
			back = saveOrUpdate(entity);

		} catch (Exception e) {
			messages.error(new BundleKey("messages", "tradingCurrency.uniqueField"));
		}
		return back;

	}

	/**
	 * Override default list view name. (By default its class name starting
	 * lower case + 's').
	 * 
	 * @see org.meveo.admin.action.BaseBean#getDefaultViewName()
	 */
	protected String getDefaultViewName() {
		return "tradingCurrencies";
	}

	public void populateCurrencies(Currency currency) {
		log.info("populatCurrencies currency", currency != null ? currency.getCurrencyCode() : null);
		if (currency != null) {
			entity.setCurrency(currency);
			entity.setPrDescription(currency.getDescriptionEn());
		}
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<TradingCurrency> getPersistenceService() {
		return tradingCurrencyService;
	}

	public void test() throws BatchUpdateException {
		throw new BatchUpdateException();
	}

}