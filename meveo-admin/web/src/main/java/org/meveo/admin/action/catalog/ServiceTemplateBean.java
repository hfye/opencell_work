/*
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.admin.action.catalog;

import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.catalog.RecurringChargeTemplate;
import org.meveo.model.catalog.ServiceChargeTemplate;
import org.meveo.model.catalog.ServiceChargeTemplateRecurring;
import org.meveo.model.catalog.ServiceChargeTemplateSubscription;
import org.meveo.model.catalog.ServiceChargeTemplateTermination;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.catalog.ServiceChargeTemplateUsage;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.catalog.impl.ServiceChargeTemplateRecurringService;
import org.meveo.service.catalog.impl.ServiceChargeTemplateSubscriptionService;
import org.meveo.service.catalog.impl.ServiceChargeTemplateTerminationService;
import org.meveo.service.catalog.impl.ServiceChargeTemplateUsageService;
import org.meveo.service.catalog.impl.ServiceTemplateService;

@Named
@ConversationScoped
public class ServiceTemplateBean extends BaseBean<ServiceTemplate> {

	private static final long serialVersionUID = 1L;

	@Produces
	@Named
	private ServiceChargeTemplateRecurring serviceChargeTemplateRecurring = new ServiceChargeTemplateRecurring();

	public void newServiceChargeTemplateRecurring() {
		this.serviceChargeTemplateRecurring = new ServiceChargeTemplateRecurring();
	}
	
	@Produces
	@Named
	private ServiceChargeTemplateSubscription serviceChargeTemplateSubscription = new ServiceChargeTemplateSubscription();

	public void newServiceChargeTemplateSubscription() {
		this.serviceChargeTemplateSubscription = new ServiceChargeTemplateSubscription();
	}
	
	@Produces
	@Named
	private ServiceChargeTemplateTermination serviceChargeTemplateTermination = new ServiceChargeTemplateTermination();

	public void newServiceChargeTemplateTermination() {
		this.serviceChargeTemplateTermination = new ServiceChargeTemplateTermination();
	}
	
	@Produces
	@Named
	private ServiceChargeTemplateUsage serviceChargeTemplateUsage = new ServiceChargeTemplateUsage();

	public void newServiceChargeTemplateUsage() {
		this.serviceChargeTemplateUsage = new ServiceChargeTemplateUsage();
	}

	@Inject
	private ServiceChargeTemplateSubscriptionService serviceChargeTemplateSubscriptionService;
	@Inject
	private ServiceChargeTemplateTerminationService serviceChargeTemplateTerminationService;
	@Inject
	private ServiceChargeTemplateRecurringService serviceChargeTemplateRecurringService;
	@Inject
	private ServiceChargeTemplateUsageService serviceChargeTemplateUsageService;

	/**
	 * Injected
	 * 
	 * @{link ServiceTemplate} service. Extends {@link PersistenceService}.
	 */
	@Inject
	private ServiceTemplateService serviceTemplateService;




	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */
	public ServiceTemplateBean() {
		super(ServiceTemplate.class);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.meveo.admin.action.BaseBean#saveOrUpdate(boolean)
	 */
	@Override
	public String saveOrUpdate(boolean killConversation) {

		List<ServiceChargeTemplate<RecurringChargeTemplate>> recurringCharges = entity.getRecurringCharges();
		for (ServiceChargeTemplate<RecurringChargeTemplate> recurringCharge : recurringCharges) {
			if (!recurringCharge.getChargeTemplate().getApplyInAdvance()) {
				break;
			}
		}

		return super.saveOrUpdate(killConversation);
	}

	public void saveServiceChargeTemplateSubscription() {
		log.info("saveServiceChargeTemplateSubscription getObjectId=#0", getObjectId());

		try {
			if (serviceChargeTemplateSubscription != null) {
				for (ServiceChargeTemplate<OneShotChargeTemplate> inc : entity.getSubscriptionCharges()) {
					if (inc.getChargeTemplate()
							.getCode()
							.equalsIgnoreCase(
									serviceChargeTemplateSubscription.getChargeTemplate().getCode())
							&& !inc.getId().equals(serviceChargeTemplateSubscription.getId())) {
						throw new Exception();
					}
				}
				if (serviceChargeTemplateSubscription.getId() != null) {
					serviceChargeTemplateSubscriptionService.update(serviceChargeTemplateSubscription);
					messages.info(new BundleKey("messages", "update.successful"));
				} else {
					serviceChargeTemplateSubscription.setServiceTemplate(entity);
					serviceChargeTemplateSubscriptionService.create(serviceChargeTemplateSubscription);
					entity.getSubscriptionCharges().add(serviceChargeTemplateSubscription);
					messages.info(new BundleKey("messages", "save.successful"));
				}
			}
		} catch (Exception e) {
			log.error("exception when applying one serviceUsageChargeTemplate !", e);
			messages.error(new BundleKey("messages", "serviceTemplate.uniqueUsageCounterFlied"));
		}
		serviceChargeTemplateUsage = new ServiceChargeTemplateUsage();
	}

	public void deleteServiceSubscriptionChargeTemplate(
			ServiceChargeTemplateSubscription serviceSubscriptionChargeTemplate) {
		serviceChargeTemplateSubscriptionService.remove(serviceSubscriptionChargeTemplate);
		entity.getSubscriptionCharges().remove(serviceSubscriptionChargeTemplate);
	}

	public void editServiceSubscriptionChargeTemplate(ServiceChargeTemplateSubscription serviceSubscriptionChargeTemplate) {
		this.serviceChargeTemplateSubscription = serviceSubscriptionChargeTemplate;
	}
	
	public void saveServiceChargeTemplateTermination() {
		log.info("saveServiceChargeTemplateTermination getObjectId=#0", getObjectId());

		try {
			if (serviceChargeTemplateTermination != null) {
				for (ServiceChargeTemplate<OneShotChargeTemplate> inc : entity.getTerminationCharges()) {
					if (inc.getChargeTemplate()
							.getCode()
							.equalsIgnoreCase(
									serviceChargeTemplateTermination.getChargeTemplate().getCode())
							&& !inc.getId().equals(serviceChargeTemplateTermination.getId())) {
						throw new Exception();
					}
				}
				if (serviceChargeTemplateTermination.getId() != null) {
					serviceChargeTemplateTerminationService.update(serviceChargeTemplateTermination);
					messages.info(new BundleKey("messages", "update.successful"));
				} else {
					serviceChargeTemplateTermination.setServiceTemplate(entity);
					serviceChargeTemplateTerminationService.create(serviceChargeTemplateTermination);
					entity.getTerminationCharges().add(serviceChargeTemplateTermination);
					messages.info(new BundleKey("messages", "save.successful"));
				}
			}
		} catch (Exception e) {
			log.error("exception when applying one serviceUsageChargeTemplate !", e);
			messages.error(new BundleKey("messages", "serviceTemplate.uniqueUsageCounterFlied"));
		}
		serviceChargeTemplateUsage = new ServiceChargeTemplateUsage();
	}

	public void deleteServiceTerminationChargeTemplate(
			ServiceChargeTemplateTermination serviceTerminationChargeTemplate) {
		serviceChargeTemplateTerminationService.remove(serviceTerminationChargeTemplate);
		entity.getTerminationCharges().remove(serviceTerminationChargeTemplate);
	}

	public void editServiceTerminationChargeTemplate(ServiceChargeTemplateTermination serviceTerminationChargeTemplate) {
		this.serviceChargeTemplateTermination = serviceTerminationChargeTemplate;
	}

	public void saveServiceChargeTemplateRecurring() {
		log.info("saveServiceChargeTemplateRecurring getObjectId=#0", getObjectId());

		try {
			if (serviceChargeTemplateRecurring != null) {
				for (ServiceChargeTemplate<RecurringChargeTemplate> inc : entity.getRecurringCharges()) {
					if (inc.getChargeTemplate()
							.getCode()
							.equalsIgnoreCase(
									serviceChargeTemplateRecurring.getChargeTemplate().getCode())
							&& !inc.getId().equals(serviceChargeTemplateRecurring.getId())) {
						throw new Exception();
					}
				}
				if (serviceChargeTemplateRecurring.getId() != null) {
					serviceChargeTemplateRecurringService.update(serviceChargeTemplateRecurring);
					messages.info(new BundleKey("messages", "update.successful"));
				} else {
					serviceChargeTemplateRecurring.setServiceTemplate(entity);
					serviceChargeTemplateRecurringService.create(serviceChargeTemplateRecurring);
					entity.getRecurringCharges().add(serviceChargeTemplateRecurring);
					messages.info(new BundleKey("messages", "save.successful"));
				}
			}
		} catch (Exception e) {
			log.error("exception when applying one serviceUsageChargeTemplate !", e);
			messages.error(new BundleKey("messages", "serviceTemplate.uniqueUsageCounterFlied"));
		}
		serviceChargeTemplateUsage = new ServiceChargeTemplateUsage();
	}

	public void deleteServiceRecurringChargeTemplate(
			ServiceChargeTemplateRecurring serviceRecurringChargeTemplate) {
		serviceChargeTemplateRecurringService.remove(serviceRecurringChargeTemplate);
		entity.getRecurringCharges().remove(serviceRecurringChargeTemplate);
	}

	public void editServiceRecurringChargeTemplate(ServiceChargeTemplateRecurring serviceRecurringChargeTemplate) {
		this.serviceChargeTemplateRecurring = serviceRecurringChargeTemplate;
	}

	
	public void saveServiceChargeTemplateUsage() {
		log.info("saveServiceChargeTemplateUsage getObjectId=#0", getObjectId());

		try {
			if (serviceChargeTemplateUsage != null) {
				for (ServiceChargeTemplateUsage inc : entity.getServiceUsageCharges()) {
					if (inc.getChargeTemplate()
							.getCode()
							.equalsIgnoreCase(
									serviceChargeTemplateUsage.getChargeTemplate().getCode())
							&& inc.getCounterTemplate()
									.getCode()
									.equalsIgnoreCase(
											serviceChargeTemplateUsage.getCounterTemplate()
													.getCode())
							&& !inc.getId().equals(serviceChargeTemplateUsage.getId())) {
						throw new Exception();
					}
				}
				if (serviceChargeTemplateUsage.getId() != null) {
					serviceChargeTemplateUsageService.update(serviceChargeTemplateUsage);
					messages.info(new BundleKey("messages", "update.successful"));
				} else {
					serviceChargeTemplateUsage.setServiceTemplate(entity);
					serviceChargeTemplateUsageService.create(serviceChargeTemplateUsage);
					entity.getServiceUsageCharges().add(serviceChargeTemplateUsage);
					messages.info(new BundleKey("messages", "save.successful"));
				}
			}
		} catch (Exception e) {
			log.error("exception when applying one serviceUsageChargeTemplate !", e);
			messages.error(new BundleKey("messages", "serviceTemplate.uniqueUsageCounterFlied"));
		}
		serviceChargeTemplateUsage = new ServiceChargeTemplateUsage();
	}

	public void deleteServiceUsageChargeTemplate(
			ServiceChargeTemplateUsage serviceUsageChargeTemplate) {
		serviceChargeTemplateUsageService.remove(serviceUsageChargeTemplate);
		entity.getServiceUsageCharges().remove(serviceUsageChargeTemplate);
	}

	public void editServiceUsageChargeTemplate(ServiceChargeTemplateUsage serviceUsageChargeTemplate) {
		this.serviceChargeTemplateUsage = serviceUsageChargeTemplate;
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<ServiceTemplate> getPersistenceService() {
		return serviceTemplateService;
	}

	@Override
	protected String getDefaultSort() {
		return "code";
	}

}
