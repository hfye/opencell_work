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
package org.meveo.admin.action.admin.module;

import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.model.admin.MeveoModule;
import org.meveo.model.admin.MeveoModuleItem;
import org.meveo.model.admin.ModuleItemTypeEnum;
import org.meveo.model.communication.MeveoInstance;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.filter.Filter;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.notification.Notification;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.service.admin.impl.MeveoModuleService;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.custom.CustomEntityTemplateService;
import org.meveo.service.filter.FilterService;
import org.meveo.service.job.JobInstanceService;
import org.meveo.service.notification.GenericNotificationService;
import org.meveo.service.script.ScriptInstanceService;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 * Meveo module bean
 * 
 * @author Tyshan Shi(tyshan@manaty.net)
 *
 */

@Named
@ViewScoped
public class MeveoModuleBean extends BaseBean<MeveoModule> {

	private static final long serialVersionUID = 1L;
	/**
	 * Injected @{link MeveoModule} service. Extends {@link PersistenceService}.
	 */
	@Inject
	private MeveoModuleService meveoModuleService;
	@Inject
	private CustomEntityTemplateService customEntityTemplateService;
	@Inject
	private FilterService filterService;
	@Inject
	private ScriptInstanceService scriptInstanceService;
	@Inject
	private JobInstanceService jobInstanceService;
	@Inject
	private GenericNotificationService notificationService;
	
	private CustomEntityTemplate customEntity;
	private CustomFieldTemplate customField;
	private Filter filter;
	private ScriptInstance script;
	private JobInstance job;
	private Notification notification;
	
	private TreeNode root;
	private TreeNode cetnode;
	private TreeNode cftnode;
	private TreeNode filternode;
	private TreeNode scriptnode;
	private TreeNode jobnode;
	private TreeNode notificationnode;
	
	protected MeveoInstance meveoInstance;


	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */
	public MeveoModuleBean() {
		super(MeveoModule.class);

	}
	
	@PostConstruct
	public void init(){
		root=new DefaultTreeNode(new CustomizedModuleItem("meveoModule.title",true));
		cetnode=new DefaultTreeNode(new CustomizedModuleItem("meveoModule.customEntities",true),root);
		cetnode.setExpanded(true);
		cftnode=new DefaultTreeNode(new CustomizedModuleItem("meveoModule.customFields",true),root);
		cftnode.setExpanded(true);
		filternode=new DefaultTreeNode(new CustomizedModuleItem("meveoModule.filters",true),root);
		filternode.setExpanded(true);
		scriptnode=new DefaultTreeNode(new CustomizedModuleItem("meveoModule.scriptInstances",true),root);
		scriptnode.setExpanded(true);
		jobnode=new DefaultTreeNode(new CustomizedModuleItem("meveoModule.jobInstances",true),root);
		jobnode.setExpanded(true);
		notificationnode=new DefaultTreeNode(new CustomizedModuleItem("meveoModule.notifications",true),root);
		notificationnode.setExpanded(true);
	}
	
	public MeveoInstance getMeveoInstance() {
		return meveoInstance;
	}

	public void setMeveoInstance(MeveoInstance meveoInstance) {
		this.meveoInstance = meveoInstance;
	}

	@Override
	public MeveoModule initEntity() {
		MeveoModule module = super.initEntity();
		if (module.getModuleItems() != null) {
			List<MeveoModuleItem> items=module.getModuleItems();
			Iterator<MeveoModuleItem> itr=items.iterator();
			while (itr.hasNext()) {
				MeveoModuleItem item=itr.next();
				switch (item.getItemType()) {
				case CET:
					CustomEntityTemplate cet = customEntityTemplateService.findByCode(item.getItemCode(),getCurrentProvider());
					if(cet!=null){
						TreeNode node=new DefaultTreeNode(new CustomizedModuleItem(cet.getCode(),cet.getDescription(), ModuleItemTypeEnum.CET),cetnode);
					}else{
						item.setMeveoModule(null);
						itr.remove();
					}
					break;
				case CFT:
					CustomFieldTemplate cft=customFieldTemplateService.findByCodeAndAppliesTo(item.getItemCode(),item.getAppliesTo(),getCurrentProvider());
					if(cft!=null){
						TreeNode node=new DefaultTreeNode(new CustomizedModuleItem(cft.getCode(),cft.getDescription(),cft.getAppliesTo(),ModuleItemTypeEnum.CFT),cftnode);
					}else{
						item.setMeveoModule(null);
						itr.remove();
					}
					break;
				case FILTER:
					Filter filter = filterService.findByCode(item.getItemCode(),getCurrentProvider());
					if(filter!=null){
						TreeNode node=new DefaultTreeNode(new CustomizedModuleItem(filter.getCode(),filter.getDescription(),ModuleItemTypeEnum.FILTER),filternode);
					}else{
						item.setMeveoModule(null);
						itr.remove();
					}
					break;
				case SCRIPT:
					ScriptInstance script = scriptInstanceService.findByCode(item.getItemCode(),getCurrentProvider());
					if(script!=null){
						TreeNode node=new DefaultTreeNode(new CustomizedModuleItem(script.getCode(),script.getDescription(),ModuleItemTypeEnum.SCRIPT),scriptnode);
					}else{
						item.setMeveoModule(null);
						itr.remove();
					}
					break;
				case JOBINSTANCE:
					JobInstance job=jobInstanceService.findByCode(item.getItemCode(),getCurrentProvider());
					if(job!=null){
						TreeNode node=new DefaultTreeNode(new CustomizedModuleItem(job.getCode(),job.getDescription(),ModuleItemTypeEnum.JOBINSTANCE),jobnode);
					}else{
						item.setMeveoModule(null);
						itr.remove();
					}
					break;
				case NOTIFICATION:
					Notification notification=notificationService.findByCode(item.getItemCode(),getCurrentProvider());
					if(notification!=null){
						TreeNode node=new DefaultTreeNode(new CustomizedModuleItem(notification.getCode(),notification.getDescription(),ModuleItemTypeEnum.NOTIFICATION),notificationnode);
					}else{
						item.setMeveoModule(null);
						itr.remove();
					}
					break;
				default:
				}
			}
		}
		return module;
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<MeveoModule> getPersistenceService() {
		return meveoModuleService;
	}
//
	public CustomEntityTemplate getCustomEntity() {
		return customEntity;
	}

	public void setCustomEntity(CustomEntityTemplate customEntity) {
		// this.customEntity = customEntity;
		if (customEntity != null) {
			MeveoModuleItem item=new MeveoModuleItem(customEntity.getCode(),ModuleItemTypeEnum.CET);
			if(!entity.getModuleItems().contains(item)){
				entity.addModuleItem(item);
				TreeNode node=new DefaultTreeNode(new CustomizedModuleItem(customEntity.getCode(),customEntity.getDescription(),ModuleItemTypeEnum.CET),cetnode);
			}
			
		}
	}
	
	public TreeNode getRoot() {
		return root;
	}
	public void setRoot(TreeNode root) {
		this.root = root;
	}
	
	public CustomFieldTemplate getCustomField() {
		return customField;
	}
	public void setCustomField(CustomFieldTemplate customField) {
		if (customField != null) {
			MeveoModuleItem item=new MeveoModuleItem(customField.getCode(),customField.getAppliesTo(),ModuleItemTypeEnum.CFT);
			if(!entity.getModuleItems().contains(item)){
				entity.addModuleItem(item);
				TreeNode node=new DefaultTreeNode(new CustomizedModuleItem(customField.getCode(),customField.getDescription(),customField.getAppliesTo(),ModuleItemTypeEnum.CFT),cftnode);
			}
		}
	}
	public Filter getFilter() {
		return filter;
	}
	public void setFilter(Filter filter) {
		if (filter != null) {
			MeveoModuleItem item=new MeveoModuleItem(filter.getCode(),ModuleItemTypeEnum.FILTER);
			if(!entity.getModuleItems().contains(item)){
				entity.addModuleItem(item);
				TreeNode node=new DefaultTreeNode(new CustomizedModuleItem(filter.getCode(),filter.getDescription(),ModuleItemTypeEnum.FILTER),filternode);
			}
		}
	}
	public ScriptInstance getScript() {
		return script;
	}
	public void setScript(ScriptInstance script) {
		if (script != null) {
			MeveoModuleItem item=new MeveoModuleItem(script.getCode(),ModuleItemTypeEnum.SCRIPT);
			if(!entity.getModuleItems().contains(item)){
				entity.addModuleItem(item);
				TreeNode node=new DefaultTreeNode(new CustomizedModuleItem(script.getCode(),script.getDescription(),ModuleItemTypeEnum.SCRIPT),scriptnode);
			}
		}
	}
	public JobInstance getJob() {
		return job;
	}
	public void setJob(JobInstance job) {
		if (job != null) {
			MeveoModuleItem item=new MeveoModuleItem(job.getCode(),ModuleItemTypeEnum.JOBINSTANCE);
			if(!entity.getModuleItems().contains(item)){
				entity.addModuleItem(item);
				TreeNode node=new DefaultTreeNode(new CustomizedModuleItem(job.getCode(),job.getDescription(),ModuleItemTypeEnum.JOBINSTANCE),jobnode);
			}
		}
	}
	public Notification getNotification() {
		return notification;
	}
	public void setNotification(Notification notification) {
		if (notification != null) {
			MeveoModuleItem item=new MeveoModuleItem(notification.getCode(),ModuleItemTypeEnum.NOTIFICATION);
			if(!entity.getModuleItems().contains(item)){
				entity.addModuleItem(item);
				TreeNode node=new DefaultTreeNode(new CustomizedModuleItem(notification.getCode(),notification.getDescription(),ModuleItemTypeEnum.NOTIFICATION),notificationnode);
			}
		}
	}
	private void removeItemFromEntity(String code,String applyTo,ModuleItemTypeEnum type){
		for(MeveoModuleItem item:entity.getModuleItems()){
			if(type==item.getItemType()){
				if(ModuleItemTypeEnum.CFT.equals(type)){
					if(code.equalsIgnoreCase(item.getItemCode())&&applyTo.equalsIgnoreCase(item.getAppliesTo())){
						entity.removeItem(item);
						break;
					}
				}else if(code.equalsIgnoreCase(item.getItemCode())){
					entity.removeItem(item);
					break;
				}
			}
		}
	}
	private void removeItemFromEntity(String code,ModuleItemTypeEnum type){
		removeItemFromEntity(code,null,type);
	}

	public void removeTreeNode(CustomizedModuleItem item) {
		if(item!=null&& item.getType()!=null){
			switch(item.getType()){
			case CET:
				for (TreeNode node:cetnode.getChildren()) {
					CustomizedModuleItem dest=(CustomizedModuleItem) node.getData();
					if (item.getCode().equals(dest.getCode())) {
						cetnode.getChildren().remove(node);
						log.debug("start to remove cet from entity by {}",item.getCode());
						removeItemFromEntity(item.getCode(),ModuleItemTypeEnum.CET);
						break;
					}
				}
				break;
			case CFT:
				for (TreeNode node:cftnode.getChildren()) {
					CustomizedModuleItem dest=(CustomizedModuleItem) node.getData();
					if (item.getCode().equals(dest.getCode())&&item.getAppliesTo().equals(dest.getAppliesTo())) {
						cftnode.getChildren().remove(node);
						removeItemFromEntity(item.getCode(),item.getAppliesTo(),ModuleItemTypeEnum.CFT);
						break;
					}
				}
				break;
			case FILTER:
				for (TreeNode node:filternode.getChildren()) {
					CustomizedModuleItem dest=(CustomizedModuleItem) node.getData();
					if (item.getCode().equals(dest.getCode())) {
						filternode.getChildren().remove(node);
						removeItemFromEntity(item.getCode(),ModuleItemTypeEnum.FILTER);
						break;
					}
				}
				break;
			case JOBINSTANCE:
				for (TreeNode node:jobnode.getChildren()) {
					CustomizedModuleItem dest=(CustomizedModuleItem) node.getData();
					if (item.getCode().equals(dest.getCode())) {
						jobnode.getChildren().remove(node);
						removeItemFromEntity(item.getCode(),ModuleItemTypeEnum.JOBINSTANCE);
						break;
					}
				}
				break;
			case SCRIPT:
				for (TreeNode node:scriptnode.getChildren()) {
					CustomizedModuleItem dest=(CustomizedModuleItem) node.getData();
					if (item.getCode().equals(dest.getCode())) {
						scriptnode.getChildren().remove(node);
						removeItemFromEntity(item.getCode(),ModuleItemTypeEnum.SCRIPT);
						break;
					}
				}
				break;
			case NOTIFICATION:
				for (TreeNode node:notificationnode.getChildren()) {
					CustomizedModuleItem dest=(CustomizedModuleItem) node.getData();
					if (item.getCode().equals(dest.getCode())) {
						notificationnode.getChildren().remove(node);
						removeItemFromEntity(item.getCode(),ModuleItemTypeEnum.NOTIFICATION);
						break;
					}
				}
				break;
			default:
			}
		}
	}
	public void exportModule(){
		log.debug("export module {} to {}",entity.getCode(),meveoInstance.getCode());
		if(meveoInstance!=null){
			try{
				meveoModuleService.exportModule2MeveoInstance(entity, meveoInstance);
				messages.info(new BundleKey("messages", "meveoModule.exportSuccess"), entity.getCode(),meveoInstance.getCode());
			} catch (Exception e) {
				log.error("Error when export module {} to {}",entity.getCode(), meveoInstance,e);
				messages.error(new BundleKey("messages", "meveoModule.exportFailed"),entity.getCode(), meveoInstance.getCode(),(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));
			}
			
		}
	}
}