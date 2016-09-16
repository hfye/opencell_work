package org.meveo.service.catalog.impl;

import java.util.Arrays;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.collections.CollectionUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.admin.User;
import org.meveo.model.catalog.OfferTemplateCategory;
import org.meveo.model.crm.Provider;
import org.meveo.service.base.BusinessService;

/**
 * @author Edward P. Legaspi
 **/
@Stateless
public class OfferTemplateCategoryService extends BusinessService<OfferTemplateCategory> {

	@Override
	public void create(OfferTemplateCategory e, User creator) throws BusinessException {
		if (e.getOfferTemplateCategory() != null) {
			e.setLevel(e.getOfferTemplateCategory().getLevel() + 1);
		}

		super.create(e, creator);
	}

	@SuppressWarnings("unchecked")
	public List<OfferTemplateCategory> listAllExceptId(Long id, boolean isParent) {
		QueryBuilder qb = new QueryBuilder(OfferTemplateCategory.class, "o", null, getCurrentProvider());
		qb.addCriterion("id", "<>", id, true);
		if (isParent) {
			qb.addCriterion("level", "<>", 3, true);
		}

		try {
			return (List<OfferTemplateCategory>) qb.getQuery(getEntityManager()).getResultList();
		} catch (NoResultException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<OfferTemplateCategory> listAllRootsExceptId(Long id) {
		QueryBuilder qb = new QueryBuilder(OfferTemplateCategory.class, "o", null, getCurrentProvider());
		qb.addCriterion("level", "=", 1, true);
		if (id != null) {
			qb.addCriterion("id", "<>", id, true);
		}

		try {
			return (List<OfferTemplateCategory>) qb.getQuery(getEntityManager()).getResultList();
		} catch (NoResultException e) {
			return null;
		}
	}

    @SuppressWarnings("rawtypes")
    public boolean canDeleteOfferTemplateCategory(Long id) {
        List<String> fieldsFetch = Arrays.asList("children");

        OfferTemplateCategory offerTemplateCategory = findById(id, fieldsFetch);
        if (offerTemplateCategory != null && CollectionUtils.isNotEmpty(offerTemplateCategory.getChildren())) {
           return false;
        }

       return true;
    }

    public List<OfferTemplateCategory> findRoots(Provider provider) {
        Query query = getEntityManager()
                .createQuery(
                        "from " + OfferTemplateCategory.class.getSimpleName()
                                + " where offerTemplateCategory.id IS NULL and provider=:provider");
        query.setParameter("provider", provider);
        if (query.getResultList().size() == 0) {
            return null;
        }

        return query.getResultList();
    }
}
