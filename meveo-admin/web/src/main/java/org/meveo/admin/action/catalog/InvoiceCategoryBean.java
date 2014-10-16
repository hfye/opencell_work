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

import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.billing.CatMessages;
import org.meveo.model.billing.InvoiceCategory;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.catalog.impl.CatMessagesService;
import org.meveo.service.catalog.impl.InvoiceCategoryService;

@Named
@ConversationScoped
public class InvoiceCategoryBean extends BaseBean<InvoiceCategory> {

    private static final long serialVersionUID = 1L;

    @Inject
    private CatMessagesService catMessagesService;

    /**
     * Injected @{link InvoiceCategory} service. Extends {@link PersistenceService}.
     */
    @Inject
    private InvoiceCategoryService invoiceCategoryService;

    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link BaseBean}.
     */
    public InvoiceCategoryBean() {
        super(InvoiceCategory.class);
    }

    /**
     * Factory method for entity to edit. If objectId param set load that entity from database, otherwise create new.
     * 
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public InvoiceCategory initEntity() {
        InvoiceCategory invoicecat = super.initEntity();
        languageMessagesMap.clear();
        if (invoicecat.getId() != null) {
            for (CatMessages msg : catMessagesService.getCatMessagesList(InvoiceCategory.class.getSimpleName() + "_" + invoicecat.getId())) {
                languageMessagesMap.put(msg.getLanguageCode(), msg.getDescription());
            }
        }

        return invoicecat;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.meveo.admin.action.BaseBean#saveOrUpdate(boolean)
     */
    @Override
    public String saveOrUpdate(boolean killConversation) {
        String back = null;
        if (entity.getId() != null) {
            for (String msgKey : languageMessagesMap.keySet()) {
                String description = languageMessagesMap.get(msgKey);
                CatMessages catMsg = catMessagesService.getCatMessages(entity.getClass().getSimpleName() + "_" + entity.getId(), msgKey);
                if (catMsg != null) {
                    catMsg.setDescription(description);
                    catMessagesService.update(catMsg);
                } else {
                    CatMessages catMessages = new CatMessages(entity.getClass().getSimpleName() + "_" + entity.getId(), msgKey, description);
                    catMessagesService.create(catMessages);
                }
            }
            back = super.saveOrUpdate(killConversation);

        } else {
            back = super.saveOrUpdate(killConversation);
            for (String msgKey : languageMessagesMap.keySet()) {
                String description = languageMessagesMap.get(msgKey);
                CatMessages catMessages = new CatMessages(entity.getClass().getSimpleName() + "_" + entity.getId(), msgKey, description);
                catMessagesService.create(catMessages);
            }

        }

        return back;
    }

    /**
     * Override default list view name. (By default its class name starting lower case + 's').
     * 
     * @see org.meveo.admin.action.BaseBean#getDefaultViewName()
     */
    protected String getDefaultViewName() {
        return "invoiceCategories";
    }

    /**
     * @see org.meveo.admin.action.BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<InvoiceCategory> getPersistenceService() {
        return invoiceCategoryService;
    }

    @Override
    protected String getListViewName() {
        return "invoiceCategories";
    }

    @Override
    public String getNewViewName() {
        return "invoiceCategoryDetail";
    }

    @Override
    protected List<String> getListFieldsToFetch() {
        return Arrays.asList("invoiceSubCategories");
    }

    @Override
    protected List<String> getFormFieldsToFetch() {
        return Arrays.asList("invoiceSubCategories");
    }
    
    @Override
    protected String getDefaultSort() {
    	return "code";
    }

}
