package org.meveo.model.notification;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.meveo.model.AuditableEntity;

@Entity
@Table(name="ADM_NOTIF_HISTORY")
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "ADM_NOTIF_HISTORY_SEQ")
public class NotificationHistory extends AuditableEntity {
	
	private static final long serialVersionUID = -6882236977852466160L;

	@ManyToOne(fetch=FetchType.LAZY)
	@NotNull
	@JoinColumn(name="NOTIFICATION_ID")
	private Notification notification;
	
	@Column(name="ENTITY_CLASSNAME",length=255)
	@Size(max=255)
	private String entityClassName;
	

	@Column(name="SERIALIZED_ENTITY")
	@Lob @Basic(fetch=FetchType.LAZY) 
	private String serializedEntity;

	@Column(name="NB_RETRY")
	@Max(10)
	private int nbRetry;

	@Column(name="RESULT",length=1000)
	@Size(max=1000)
	private String result;
	
	@Column(name="STATUS")
	@Enumerated(EnumType.STRING)
	private NotificationHistoryStatusEnum status;

	public Notification getNotification() {
		return notification;
	}

	public void setNotification(Notification notification) {
		this.notification = notification;
	}

	public String getEntityClassName() {
		return entityClassName;
	}

	public void setEntityClassName(String entityClassName) {
		this.entityClassName = entityClassName;
	}

	public String getSerializedEntity() {
		return serializedEntity;
	}

	public void setSerializedEntity(String serializedEntity) {
		this.serializedEntity = serializedEntity;
	}

	public int getNbRetry() {
		return nbRetry;
	}

	public void setNbRetry(int nbRetry) {
		this.nbRetry = nbRetry;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public NotificationHistoryStatusEnum getStatus() {
		return status;
	}

	public void setStatus(NotificationHistoryStatusEnum status) {
		this.status = status;
	}
	
}