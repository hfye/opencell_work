package org.meveo.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "accountOperation")
@XmlAccessorType(XmlAccessType.FIELD)
public class AccountOperationDto {

	private Date dueDate;
	private String type;
	private Date transactionDate;
	private String transactionCategory;
	private String reference;
	private String accountCode;
	private String accountCodeClientSide;
	private BigDecimal amount;
	private BigDecimal matchingAmount = BigDecimal.ZERO;
	private BigDecimal unMatchingAmount = BigDecimal.ZERO;
	private String matchingStatus;
	private List<MatchingAmountDto> matchingAmounts = new ArrayList<MatchingAmountDto>();
	private String occCode;
	private String occDescription;

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Date getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(Date transactionDate) {
		this.transactionDate = transactionDate;
	}

	public String getTransactionCategory() {
		return transactionCategory;
	}

	public void setTransactionCategory(String transactionCategory) {
		this.transactionCategory = transactionCategory;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getAccountCode() {
		return accountCode;
	}

	public void setAccountCode(String accountCode) {
		this.accountCode = accountCode;
	}

	public String getAccountCodeClientSide() {
		return accountCodeClientSide;
	}

	public void setAccountCodeClientSide(String accountCodeClientSide) {
		this.accountCodeClientSide = accountCodeClientSide;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getMatchingAmount() {
		return matchingAmount;
	}

	public void setMatchingAmount(BigDecimal matchingAmount) {
		this.matchingAmount = matchingAmount;
	}

	public BigDecimal getUnMatchingAmount() {
		return unMatchingAmount;
	}

	public void setUnMatchingAmount(BigDecimal unMatchingAmount) {
		this.unMatchingAmount = unMatchingAmount;
	}

	public String getMatchingStatus() {
		return matchingStatus;
	}

	public void setMatchingStatus(String matchingStatus) {
		this.matchingStatus = matchingStatus;
	}

	public List<MatchingAmountDto> getMatchingAmounts() {
		return matchingAmounts;
	}

	public void setMatchingAmounts(List<MatchingAmountDto> matchingAmounts) {
		this.matchingAmounts = matchingAmounts;
	}

	public void addMatchingAmounts(MatchingAmountDto matchingAmount) {
		if (matchingAmounts == null) {
			matchingAmounts = new ArrayList<MatchingAmountDto>();
		}
		this.matchingAmounts.add(matchingAmount);
	}

	public String getOccCode() {
		return occCode;
	}

	public void setOccCode(String occCode) {
		this.occCode = occCode;
	}

	public String getOccDescription() {
		return occDescription;
	}

	public void setOccDescription(String occDescription) {
		this.occDescription = occDescription;
	}

}
