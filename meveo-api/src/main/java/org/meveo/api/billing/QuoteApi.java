package org.meveo.api.billing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.IncorrectServiceInstanceException;
import org.meveo.admin.exception.IncorrectSusbcriptionException;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.CustomFieldDto;
import org.meveo.api.dto.CustomFieldsDto;
import org.meveo.api.exception.ActionForbiddenException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.order.OrderProductCharacteristicEnum;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.admin.User;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.ProductInstance;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.ProductOffering;
import org.meveo.model.catalog.ProductTemplate;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.Provider;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.crm.custom.CustomFieldValue;
import org.meveo.model.quote.Quote;
import org.meveo.model.quote.QuoteItem;
import org.meveo.model.quote.QuoteStatusEnum;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.billing.impl.ProductInstanceService;
import org.meveo.service.billing.impl.ServiceInstanceService;
import org.meveo.service.billing.impl.UserAccountService;
import org.meveo.service.catalog.impl.ProductOfferingService;
import org.meveo.service.catalog.impl.ServiceTemplateService;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.quote.QuoteService;
import org.meveo.service.wf.WorkflowService;
import org.meveo.util.EntityCustomizationUtils;
import org.slf4j.Logger;
import org.tmf.dsmapi.catalog.resource.order.Product;
import org.tmf.dsmapi.catalog.resource.order.ProductCharacteristic;
import org.tmf.dsmapi.catalog.resource.order.ProductRelationship;
import org.tmf.dsmapi.catalog.resource.product.BundledProductReference;
import org.tmf.dsmapi.quote.ProductQuote;
import org.tmf.dsmapi.quote.ProductQuoteItem;

@Stateless
public class QuoteApi extends BaseApi {

    @Inject
    private Logger log;

    @Inject
    private ProductOfferingService productOfferingService;

    @Inject
    private UserAccountService userAccountService;

    @Inject
    private ProductInstanceService productInstanceService;

    @Inject
    private CustomFieldTemplateService customFieldTemplateService;

    @Inject
    private QuoteService quoteService;

    @Inject
    private WorkflowService workflowService;

    @Inject
    private ServiceTemplateService serviceTemplateService;

    @Inject
    private ServiceInstanceService serviceInstanceService;

    @Inject
    InvoiceService invoiceService;

    private ParamBean paramBean = ParamBean.getInstance();

    /**
     * Register a quote from TMForumApi
     * 
     * @param productQuote Quote
     * @param currentUser Current user
     * @return Quote updated
     * @throws MissingParameterException
     * @throws IncorrectSusbcriptionException
     * @throws IncorrectServiceInstanceException
     * @throws BusinessException
     * @throws MeveoApiException
     */
    public ProductQuote createQuote(ProductQuote productQuote, User currentUser) throws MeveoApiException, BusinessException {

        if (productQuote.getQuoteItem() == null || productQuote.getQuoteItem().isEmpty()) {
            missingParameters.add("orderItem");
        }
        if (productQuote.getQuoteDate() == null) {
            missingParameters.add("orderDate");
        }

        handleMissingParameters();
        Provider provider = currentUser.getProvider();

        Quote quote = new Quote();
        quote.setCode(UUID.randomUUID().toString());
        quote.setCategory(productQuote.getCategory());
        quote.setNotificationContact(productQuote.getNotificationContact());
        quote.setDescription(productQuote.getDescription());
        quote.setExternalId(productQuote.getExternalId());
        quote.setReceivedFromApp("API");
        quote.setQuoteDate(productQuote.getQuoteDate() != null ? productQuote.getQuoteDate() : new Date());
        quote.setRequestedCompletionDate(productQuote.getQuoteCompletionDate());
        quote.setFulfillmentStartDate(productQuote.getFulfillmentStartDate());        
        if (productQuote.getValidFor() != null) {
            quote.setValidFrom(productQuote.getValidFor().getStartDateTime());
            quote.setValidTo(productQuote.getValidFor().getEndDateTime());
        }

        if (productQuote.getState() != null) {
            quote.setStatus(QuoteStatusEnum.valueByApiState(productQuote.getState()));
        } else {
            quote.setStatus(QuoteStatusEnum.IN_PROGRESS);
        }

        for (ProductQuoteItem productQuoteItem : productQuote.getQuoteItem()) {
        	
            if (productQuoteItem.getBillingAccount() == null || productQuoteItem.getBillingAccount().isEmpty()) {
                missingParameters.add("billingAccount");
            }
            String billingAccountId = productQuoteItem.getBillingAccount().get(0).getId();
            if (StringUtils.isEmpty(billingAccountId)) {
                missingParameters.add("billingAccount");
            }
            handleMissingParameters();
            UserAccount userAccount = userAccountService.findByCode(billingAccountId, currentUser.getProvider());
            if (userAccount == null) {
                throw new EntityDoesNotExistsException(UserAccount.class, billingAccountId);
            }

            List<ProductOffering> productOfferings = new ArrayList<>();
            // For modify and delete actions, product offering might not be specified
            if (productQuoteItem.getProductOffering() != null) {
                ProductOffering productOfferingInDB = productOfferingService.findByCode(productQuoteItem.getProductOffering().getId(), provider);
                if (productOfferingInDB == null) {
                    throw new EntityDoesNotExistsException(ProductOffering.class, productQuoteItem.getProductOffering().getId());
                }
                productOfferings.add(productOfferingInDB);

                if (productQuoteItem.getProductOffering().getBundledProductOffering() != null) {
                    for (BundledProductReference bundledProductOffering : productQuoteItem.getProductOffering().getBundledProductOffering()) {
                        productOfferingInDB = productOfferingService.findByCode(bundledProductOffering.getReferencedId(), provider);
                        if (productOfferingInDB == null) {
                            throw new EntityDoesNotExistsException(ProductOffering.class, bundledProductOffering.getReferencedId());
                        }
                        productOfferings.add(productOfferingInDB);
                    }
                }
            } else {
                // We need productOffering so we know if product is subscription or productInstance - NEED TO FIX IT
                throw new MissingParameterException("productOffering");
            }

            QuoteItem quoteItem = new QuoteItem();
            quoteItem.setItemId(productQuoteItem.getId());

            quoteItem.setQuote(quote);
            quoteItem.setSource(ProductQuoteItem.serializeQuoteItem(productQuoteItem));
            quoteItem.setProductOfferings(productOfferings);
            quoteItem.setProvider(currentUser.getProvider());
            quoteItem.setUserAccount(userAccount);

            if (productQuoteItem.getState() != null) {
                quoteItem.setStatus(QuoteStatusEnum.valueByApiState(productQuoteItem.getState()));
            } else {
                quoteItem.setStatus(QuoteStatusEnum.IN_PROGRESS);
            }

            // Extract products that are not services. For each product offering there must be a product. Products that exceed the number of product offerings are treated as
            // services.
            //
            // Sample of ordering a single product:
            // productOffering
            // product with product characteristics
            //
            // Sample of ordering two products bundled under an offer template:
            // productOffering bundle (offer template)
            // ...productOffering (product1)
            // ...productOffering (product2)
            // product with subscription characteristics
            // ...product with product1 characteristics
            // ...product with product2 characteristics
            // ...product for service with service1 characteristics - not considered as product/does not required ID for modify/delete opperation
            // ...product for service with service2 characteristics - not considered as product/does not required ID for modify/delete opperation

            List<Product> products = new ArrayList<>();
            products.add(productQuoteItem.getProduct());
            if (productOfferings.size() > 1 && productQuoteItem.getProduct().getProductRelationship() != null && !productQuoteItem.getProduct().getProductRelationship().isEmpty()) {
                for (ProductRelationship productRelationship : productQuoteItem.getProduct().getProductRelationship()) {
                    products.add(productRelationship.getProduct());
                    if (productOfferings.size() >= products.size()) {
                        break;
                    }
                }
            }

            quote.addQuoteItem(quoteItem);
        }

        quoteService.create(quote, currentUser);

        // populate customFields
        try {
            populateCustomFields(productQuote.getCustomFields(), quote, true, currentUser);
        } catch (Exception e) {
            log.error("Failed to associate custom field instance to an entity", e);
            throw e;
        }

        // Commit before initiating workflow/quote processing
        quoteService.commit();

        quote = initiateWorkflow(quote, currentUser);

        return quoteToDto(quote);
    }

    /**
     * Initiate workflow on quote. If workflow is enabled on Quote class, then execute workflow. If workflow is not enabled - then process the quote right away.
     * 
     * @param quote Quote
     * @param currentUser
     * @return
     * @throws BusinessException
     * @throws MeveoApiException
     */
    public Quote initiateWorkflow(Quote quote, User currentUser) throws BusinessException {

        if (workflowService.isWorkflowSetup(Quote.class, currentUser.getProvider())) {
            quote = (Quote) workflowService.executeMatchingWorkflows(quote, currentUser);

        } else {
            try {
                quote = processQuote(quote, currentUser);
            } catch (MeveoApiException e) {
                throw new BusinessException(e);
            }
        }

        return quote;

    }

    /**
     * Process the quote for workflow
     * 
     * @param quote
     * @param currentUser
     * @throws BusinessException
     * @throws MeveoApiException
     */
    public Quote processQuote(Quote quote, User currentUser) throws BusinessException, MeveoApiException {

        // Nothing to process in final state
        if (quote.getStatus() == QuoteStatusEnum.CANCELLED || quote.getStatus() == QuoteStatusEnum.ACCEPTED || quote.getStatus() == QuoteStatusEnum.REJECTED) {
            return quote;
        }

        log.info("Processing quote {}", quote.getCode());

        for (QuoteItem quoteItem : quote.getQuoteItems()) {
            processQuoteItem(quote, quoteItem, currentUser);
        }

        quote.setStatus(QuoteStatusEnum.PENDING);
        for (QuoteItem quoteItem : quote.getQuoteItems()) {
            quoteItem.setStatus(QuoteStatusEnum.PENDING);
        }

        quote = invoiceQuote(quote, currentUser);

        quote = quoteService.update(quote, currentUser);

        log.trace("Finished processing quote {}", quote.getCode());

        return quote;
    }

    /**
     * Process quote item for workflow
     * 
     * @param quote Quote
     * @param quoteItem Quote item
     * @param currentUser
     * @throws BusinessException
     * @throws MeveoApiException
     */
    private void processQuoteItem(Quote quote, QuoteItem quoteItem, User currentUser) throws BusinessException, MeveoApiException {

        log.info("Processing quote item {} {}", quote.getCode(), quoteItem.getItemId());

        log.info("Finished processing quote item {} {}", quote.getCode(), quoteItem.getItemId());
    }

    /**
     * Create invoices for the quote
     * 
     * @param quote Quote
     * @param currentUser Current user
     * @throws BusinessException
     * @throws MeveoApiException
     */
    public Quote invoiceQuote(Quote quote, User currentUser) throws BusinessException {

        log.info("Creating invoices for quote {}", quote.getCode());

        try {
            for (QuoteItem quoteItem : quote.getQuoteItems()) {
                invoiceQuoteItem(quote, quoteItem, currentUser);
            }

            quote = quoteService.update(quote, currentUser);

        } catch (MeveoApiException e) {
            throw new BusinessException(e);
        }

        log.trace("Finished creating invoices for quote {}", quote.getCode());

        return quote;
    }

    /**
     * Create invoice for quote item
     * 
     * @param quote Quote
     * @param quoteItem Quote item
     * @param currentUser Current user
     * @throws BusinessException
     * @throws MeveoApiException
     */
    private void invoiceQuoteItem(Quote quote, QuoteItem quoteItem, User currentUser) throws BusinessException, MeveoApiException {

        log.info("Processing quote item {} {}", quote.getCode(), quoteItem.getItemId());

        List<ProductInstance> productInstances = new ArrayList<>();
        Subscription subscription = null;

        ProductQuoteItem productQuoteItem = ProductQuoteItem.deserializeQuoteItem(quoteItem.getSource());

        // Ordering a new product
        ProductOffering primaryOffering = quoteItem.getProductOfferings().get(0);

        // Just a simple case of ordering a single product
        if (primaryOffering instanceof ProductTemplate) {

            ProductInstance productInstance = instantiateVirtualProduct((ProductTemplate) primaryOffering, productQuoteItem.getProduct(), quoteItem, productQuoteItem, null,
                currentUser);
            productInstances.add(productInstance);

            // A complex case of ordering from offer template with services and optional products
        } else {

            // Distinguish bundled products which could be either services or products

            List<Product> products = new ArrayList<>();
            List<Product> services = new ArrayList<>();
            int index = 1;
            if (productQuoteItem.getProduct().getProductRelationship() != null && !productQuoteItem.getProduct().getProductRelationship().isEmpty()) {
                for (ProductRelationship productRelationship : productQuoteItem.getProduct().getProductRelationship()) {
                    if (index < quoteItem.getProductOfferings().size()) {
                        products.add(productRelationship.getProduct());
                    } else {
                        services.add(productRelationship.getProduct());
                    }
                    index++;
                }
            }

            // Instantiate a service
            subscription = instantiateVirtualSubscription((OfferTemplate) primaryOffering, productQuoteItem.getProduct(), services, quoteItem, productQuoteItem, currentUser);

            // Instantiate products - find a matching product offering. The order of products must match the order of productOfferings
            index = 1;
            for (@SuppressWarnings("unused")
            Product product : products) {
                ProductTemplate productOffering = (ProductTemplate) quoteItem.getProductOfferings().get(index);
                ProductInstance productInstance = instantiateVirtualProduct(productOffering, product, quoteItem, productQuoteItem, subscription, currentUser);
                productInstances.add(productInstance);
                index++;
            }
        }

        // Use either subscription start/end dates from subscription/products or subscription start/end from quote item
        Date fromDate = null;
        Date toDate = null;
        if (subscription != null) {
            fromDate = subscription.getSubscriptionDate();
            toDate = subscription.getEndAgreementDate();
        }
        for (ProductInstance productInstance : productInstances) {
            if (fromDate == null) {
                fromDate = productInstance.getApplicationDate();
            } else if (productInstance.getApplicationDate().before(fromDate)) {
                fromDate = productInstance.getApplicationDate();
            }
        }
        if (productQuoteItem.getSubscriptionPeriod() != null && productQuoteItem.getSubscriptionPeriod().getStartDateTime() != null
                && productQuoteItem.getSubscriptionPeriod().getStartDateTime().before(fromDate)) {
            fromDate = productQuoteItem.getSubscriptionPeriod().getStartDateTime();
        }
        if (toDate == null && productQuoteItem.getSubscriptionPeriod() != null) {
            productQuoteItem.getSubscriptionPeriod().getEndDateTime();
        }

        log.error("AKK date from {} to {}", fromDate, toDate);
        // Create invoices for simulated charges for product instances and subscriptions
        Invoice invoice = quoteService.provideQuote(quote.getCode(), productQuoteItem.getConsumptionCdr(), subscription, productInstances, fromDate, toDate, currentUser);

        // invoiceService.create(invoice, currentUser);
        quoteItem.setInvoice(invoice);

        // Serialize back the productOrderItem with updated invoice attachments
        quoteItem.setSource(ProductQuoteItem.serializeQuoteItem(productQuoteItem));

        log.info("Finished processing quote item {} {}", quote.getCode(), quoteItem.getItemId());
    }

    private Subscription instantiateVirtualSubscription(OfferTemplate offerTemplate, Product product, List<Product> services, QuoteItem quoteItem,
            ProductQuoteItem productQuoteItem, User currentUser) throws BusinessException, MeveoApiException {

        log.debug("Instantiating virtual subscription from offer template {} for quote {} line {}", offerTemplate.getCode(), quoteItem.getQuote().getCode(), quoteItem.getItemId());

        String subscriptionCode = (String) getProductCharacteristic(productQuoteItem.getProduct(), OrderProductCharacteristicEnum.SUBSCRIPTION_CODE.getCharacteristicName(),
            String.class, UUID.randomUUID().toString());

        Subscription subscription = new Subscription();
        subscription.setCode(subscriptionCode);
        subscription.setUserAccount(quoteItem.getUserAccount());
        subscription.setOffer(offerTemplate);
        subscription.setSubscriptionDate((Date) getProductCharacteristic(productQuoteItem.getProduct(), OrderProductCharacteristicEnum.SUBSCRIPTION_DATE.getCharacteristicName(),
            Date.class, DateUtils.setTimeToZero(quoteItem.getQuote().getQuoteDate())));
        subscription.setEndAgreementDate((Date) getProductCharacteristic(productQuoteItem.getProduct(),
            OrderProductCharacteristicEnum.SUBSCRIPTION_END_DATE.getCharacteristicName(), Date.class, null));
        subscription.setProvider(currentUser.getProvider());

        // // Validate and populate customFields
        // CustomFieldsDto customFields = extractCustomFields(productQuoteItem.getProduct(), Subscription.class, currentUser.getProvider());
        // try {
        // populateCustomFields(customFields, subscription, true, currentUser, true);
        // } catch (Exception e) {
        // log.error("Failed to associate custom field instance to an entity", e);
        // throw new BusinessException("Failed to associate custom field instance to an entity", e);
        // }

        // instantiate and activate services
        processServices(subscription, services, currentUser);

        return subscription;
    }

    private ProductInstance instantiateVirtualProduct(ProductTemplate productTemplate, Product product, QuoteItem quoteItem, ProductQuoteItem productQuoteItem,
            Subscription subscription, User currentUser) throws BusinessException {

        log.debug("Instantiating virtual product from product template {} for quote {} line {}", productTemplate.getCode(), quoteItem.getQuote().getCode(), quoteItem.getItemId());

        BigDecimal quantity = ((BigDecimal) getProductCharacteristic(product, OrderProductCharacteristicEnum.SERVICE_PRODUCT_QUANTITY.getCharacteristicName(), BigDecimal.class,
            new BigDecimal(1)));
        Date chargeDate = ((Date) getProductCharacteristic(product, OrderProductCharacteristicEnum.SUBSCRIPTION_DATE.getCharacteristicName(), Date.class,
            DateUtils.setTimeToZero(new Date())));

        String code = (String) getProductCharacteristic(product, OrderProductCharacteristicEnum.PRODUCT_INSTANCE_CODE.getCharacteristicName(), String.class, UUID.randomUUID()
            .toString());
        ProductInstance productInstance = new ProductInstance(quoteItem.getUserAccount(), subscription, productTemplate, quantity, chargeDate, code,
            productTemplate.getDescription(), null, currentUser);
        productInstance.setProvider(currentUser.getProvider());

        productInstanceService.instantiateProductInstance(productInstance, null, null, null, currentUser, true);

        // try {
        // CustomFieldsDto customFields = extractCustomFields(product, ProductInstance.class, currentUser.getProvider());
        // populateCustomFields(customFields, productInstance, true, currentUser, true);
        // } catch (Exception e) {
        // log.error("Failed to associate custom field instance to an entity", e);
        // throw new BusinessException("Failed to associate custom field instance to an entity", e);
        // }
        return productInstance;
    }

    @SuppressWarnings({ "rawtypes", "unused" })
    private CustomFieldsDto extractCustomFields(Product product, Class appliesToClass, Provider provider) {

        if (product.getProductCharacteristic() == null || product.getProductCharacteristic().isEmpty()) {
            return null;
        }

        CustomFieldsDto customFieldsDto = new CustomFieldsDto();

        Map<String, CustomFieldTemplate> cfts = customFieldTemplateService.findByAppliesTo(EntityCustomizationUtils.getAppliesTo(appliesToClass, null), provider);

        for (ProductCharacteristic characteristic : product.getProductCharacteristic()) {
            if (characteristic.getName() != null && cfts.containsKey(characteristic.getName())) {

                CustomFieldTemplate cft = cfts.get(characteristic.getName());
                CustomFieldDto cftDto = entityToDtoConverter.customFieldToDTO(characteristic.getName(), CustomFieldValue.parseValueFromString(cft, characteristic.getValue()),
                    cft.getFieldType() == CustomFieldTypeEnum.CHILD_ENTITY, provider);
                customFieldsDto.getCustomField().add(cftDto);
            }
        }

        return customFieldsDto;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object getProductCharacteristic(Product product, String code, Class valueClass, Object defaultValue) {

        if (product.getProductCharacteristic() == null || product.getProductCharacteristic().isEmpty()) {
            return defaultValue;
        }

        Object value = null;
        for (ProductCharacteristic productCharacteristic : product.getProductCharacteristic()) {
            if (productCharacteristic.getName().equals(code)) {
                value = productCharacteristic.getValue();
                break;
            }
        }

        if (value != null) {

            // Need to perform conversion
            if (!valueClass.isAssignableFrom(value.getClass())) {

                if (valueClass == BigDecimal.class) {
                    value = new BigDecimal((String) value);

                }
                if (valueClass == Date.class) {
                    value = DateUtils.parseDateWithPattern((String) value, paramBean.getProperty("meveo.dateFormat", "dd/MM/yyyy"));
                }
            }

        } else {
            value = defaultValue;
        }

        return value;
    }

    private void processServices(Subscription subscription, List<Product> services, User currentUser) throws IncorrectSusbcriptionException, IncorrectServiceInstanceException,
            BusinessException, MeveoApiException {

        for (Product serviceProduct : services) {

            String serviceCode = (String) getProductCharacteristic(serviceProduct, OrderProductCharacteristicEnum.SERVICE_CODE.getCharacteristicName(), String.class, null);

            if (StringUtils.isBlank(serviceCode)) {
                throw new MissingParameterException("serviceCode");
            }

            ServiceInstance serviceInstance = new ServiceInstance();
            serviceInstance.setCode(serviceCode);
            serviceInstance.setEndAgreementDate((Date) getProductCharacteristic(serviceProduct, OrderProductCharacteristicEnum.SUBSCRIPTION_DATE.getCharacteristicName(),
                Date.class, null));
            serviceInstance.setQuantity((BigDecimal) getProductCharacteristic(serviceProduct, OrderProductCharacteristicEnum.SERVICE_PRODUCT_QUANTITY.getCharacteristicName(),
                BigDecimal.class, new BigDecimal(1)));
            serviceInstance.setSubscriptionDate((Date) getProductCharacteristic(serviceProduct, OrderProductCharacteristicEnum.SUBSCRIPTION_DATE.getCharacteristicName(),
                Date.class, DateUtils.setTimeToZero(new Date())));
            serviceInstance.setSubscription(subscription);
            serviceInstance.setServiceTemplate(serviceTemplateService.findByCode(serviceCode, currentUser.getProvider()));
            serviceInstance.setProvider(currentUser.getProvider());

            serviceInstanceService.serviceInstanciation(serviceInstance, currentUser, null, null, true);
        }
    }

    public ProductQuote getQuote(String quoteId, User currentUser) throws EntityDoesNotExistsException, BusinessException {

        Quote quote = quoteService.findByCode(quoteId, currentUser.getProvider());

        if (quote == null) {
            throw new EntityDoesNotExistsException(ProductQuote.class, quoteId);
        }

        return quoteToDto(quote);
    }

    public List<ProductQuote> findQuotes(Map<String, List<String>> filterCriteria, User currentUser) throws BusinessException {

        List<Quote> quotes = quoteService.list(currentUser.getProvider());

        List<ProductQuote> productQuotes = new ArrayList<>();
        for (Quote quote : quotes) {
            productQuotes.add(quoteToDto(quote));
        }

        return productQuotes;
    }

    public ProductQuote updatePartiallyQuote(String quoteId, ProductQuote productQuote, User currentUser) throws BusinessException, MeveoApiException {

        Quote quote = quoteService.findByCode(quoteId, currentUser.getProvider());
        if (quote == null) {
            throw new EntityDoesNotExistsException(ProductQuote.class, quoteId);
        }

        // populate customFields
        try {
            populateCustomFields(productQuote.getCustomFields(), quote, true, currentUser);
        } catch (Exception e) {
            log.error("Failed to associate custom field instance to an entity", e);
            throw e;
        }

        // TODO Need to initiate workflow if there is one

        quote = quoteService.refreshOrRetrieve(quote);

        return quoteToDto(quote);

    }

    public void deleteQuote(String quoteId, User currentUser) throws EntityDoesNotExistsException, ActionForbiddenException, BusinessException {

        Quote quote = quoteService.findByCode(quoteId, currentUser.getProvider());

        if (quote.getStatus() == QuoteStatusEnum.IN_PROGRESS || quote.getStatus() == QuoteStatusEnum.PENDING) {
            quoteService.remove(quote, currentUser);
        }
    }

    /**
     * Convert quote stored in DB to quote DTO expected by tmForum api.
     * 
     * @param quote Quote to convert
     * @return Quote DTO object
     * @throws BusinessException
     */
    private ProductQuote quoteToDto(Quote quote) throws BusinessException {

        ProductQuote productQuote = new ProductQuote();

        productQuote.setId(quote.getCode().toString());
        productQuote.setCategory(quote.getCategory());
        productQuote.setDescription(quote.getDescription());
        productQuote.setNotificationContact(quote.getNotificationContact());
        productQuote.setExternalId(quote.getExternalId());
        productQuote.setQuoteDate(quote.getQuoteDate());
        productQuote.setEffectiveQuoteCompletionDate(quote.getCompletionDate());
        productQuote.setFulfillmentStartDate(quote.getFulfillmentStartDate());
        productQuote.setQuoteCompletionDate(quote.getRequestedCompletionDate());
        productQuote.setState(quote.getStatus().getApiState());

        List<ProductQuoteItem> productQuoteItems = new ArrayList<>();
        productQuote.setQuoteItem(productQuoteItems);

        for (QuoteItem quoteItem : quote.getQuoteItems()) {
            productQuoteItems.add(quoteItemToDto(quoteItem));
        }

        productQuote.setCustomFields(entityToDtoConverter.getCustomFieldsDTO(quote));

        return productQuote;
    }

    /**
     * Convert quote item stored in DB to quoteItem dto expected by tmForum api. As actual dto was serialized earlier, all need to do is to deserialize it and update the status.
     * 
     * @param quoteItem Quote item to convert to dto
     * @return Quote item Dto
     * @throws BusinessException
     */
    private ProductQuoteItem quoteItemToDto(QuoteItem quoteItem) throws BusinessException {

        ProductQuoteItem productQuoteItem = ProductQuoteItem.deserializeQuoteItem(quoteItem.getSource());

        productQuoteItem.setState(quoteItem.getQuote().getStatus().getApiState());

        return productQuoteItem;
    }

    /**
     * Distinguish bundled products which could be either services or products
     * 
     * @param productQuoteItem Product order item DTO
     * @param quoteItem Order item entity
     * @return An array of List<Product> elements, first being list of products, and second - list of services
     */
    @SuppressWarnings("unchecked")
    public List<Product>[] getProductsAndServices(ProductQuoteItem productQuoteItem, QuoteItem quoteItem) {

        List<Product> products = new ArrayList<>();
        List<Product> services = new ArrayList<>();
        if (productQuoteItem != null) {
            int index = 1;
            if (productQuoteItem.getProduct().getProductRelationship() != null && !productQuoteItem.getProduct().getProductRelationship().isEmpty()) {
                for (ProductRelationship productRelationship : productQuoteItem.getProduct().getProductRelationship()) {
                    if (index < quoteItem.getProductOfferings().size()) {
                        products.add(productRelationship.getProduct());
                    } else {
                        services.add(productRelationship.getProduct());
                    }
                    index++;
                }
            }
        }
        return new List[] { products, services };
    }
}