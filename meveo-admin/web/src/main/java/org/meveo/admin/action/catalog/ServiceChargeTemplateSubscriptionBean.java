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

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.catalog.ServiceChargeTemplateRecurring;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.catalog.impl.ServiceChargeTemplateRecurringService;


@Named
@ConversationScoped
public class ServiceChargeTemplateSubscriptionBean extends BaseBean<ServiceChargeTemplateRecurring> {

    private static final long serialVersionUID = 1L;

   
    @Inject
    private ServiceChargeTemplateRecurringService serviceChargeTemplateRecurringService;

    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link BaseBean}.
     */
    public ServiceChargeTemplateSubscriptionBean() {
        super(ServiceChargeTemplateRecurring.class);
    }

    /**
     * Factory method for entity to edit. If objectId param set load that entity from database, otherwise create new.
     * 
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
   
    public ServiceChargeTemplateRecurring initEntity() {
        return super.initEntity();
    }

    /**
     * Override default list view name. (By default its class name starting lower case + 's').
     * 
     * @see org.meveo.admin.action.BaseBean#getDefaultViewName()
     */
    protected String getDefaultViewName() {
        return "serviceChargeTemplatesRecurring";
    }

    /**
     * @see org.meveo.admin.action.BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<ServiceChargeTemplateRecurring> getPersistenceService() {
        return serviceChargeTemplateRecurringService;
    }
    
    @Override
    protected String getListViewName() {
    	 return "serviceChargeTemplatesRecurring";
    }

}