package com.supai.app.modal;

import java.time.LocalDateTime;
//import org.springframework.data.annotation.Id;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity  // Warning {can data loss} -> [it can modify original table structure.] use @Entity when you want to *match or create a table structure
@Table(name = "VM_Document_Metadata")
@Data
@NoArgsConstructor // For JPA
@AllArgsConstructor // For convenience
public class Document {
//	@Id
//	@SequenceGenerator(name = "serial_seq_gen", sequenceName = "serial_seq", allocationSize = 1)
//	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "serial_seq_gen")
//	private Integer serial_no;

	@Id
    @Column(name = "doc_id")
    private String docId;
	@Column(name = "parent_id")
    private String parentId;
	
    @UpdateTimestamp // @CreationTimestamp
    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Column(name = "name", length = 1000)
    private String name;

    @Column(name = "link", length = 1000)
    private String link;
    @Column(name = "path", length = 1000)
    private String path;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_on")
    private String createdOn;

    @Column(name = "contract_title", length = 1000)
    private String contractTitle;

    @Column(name = "ml", length = 1000)
    private String ml;

    @Column(name = "parents_ml", length = 1000)
    private String parentsMl;

    @Column(name = "agreement_type", length = 1000)
    private String agreementType;

    @Column(name = "document_type", length = 1000)
    private String documentType;

    @Column(name = "contract_status", length = 1000)
    private String contractStatus;

    @Column(name = "responsible_yahoos", length = 1000)
    private String responsibleYahoos;

    @Column(name = "executed_agreement", length = 1000)
    private String executedAgreement;

    @Column(name = "transacting", length = 1000)
    private String transacting;

    @Column(name = "standard_terms_and_conditions", length = 1000)
    private String standardTermsAndConditions;

    @Column(name = "contract_description_or_overview", length = 5000)
    private String contractDescriptionOverview;

    @Column(name = "americas", length = 1000)
    private String americas;

    @Column(name = "asia", length = 1000)
    private String asia;

    @Column(name = "emea", length = 1000)
    private String emea;

    @Column(name = "latin_america", length = 1000)
    private String latinAmerica;

    @Column(name = "contracting_party", length = 1000)
    private String contractingParty;

    @Column(name = "counter_party", length = 1000)
    private String counterParty;

    @Column(name = "transaction_currency", length = 1000)
    private String transactionCurrency;

    @Column(name = "transaction_rate", length = 1000)
    private String transactionRate;

    @Column(name = "effective_end_date", length = 1000)
    private String effectiveEndDate;

    @Column(name = "derogation", length = 1000)
    private String derogation;

    @Column(name = "derogation_date", length = 1000)
    private String derogationDate;

    @Column(name = "vat", length = 1000)
    private String vat;

    @Column(name = "with_holding_tax", length = 1000)
    private String withHoldingTax;

    @Column(name = "signing_date", length = 1000)
    private String signingDate;

    @Column(name = "effective_date", length = 1000)
    private String effectiveDate;

    @Column(name = "invoice_required", length = 1000)
    private String invoiceRequired;

    @Column(name = "invoicing_frequency", length = 1000)
    private String invoicingFrequency;

    @Column(name = "comments_vat", length = 1000)
    private String commentsVat;

    @Column(name = "comments_wht", length = 1000)
    private String commentsWht;

    @Column(name = "other_comments", length = 1000)
    private String otherComments;

    public Document(JsonNode docNode) {
        //this.uploadDate = LocalDate.now(); 
        this.docId = getString(docNode, "Doc_Id");
        this.parentId = getString(docNode, "Parent_Id");
        this.name = getString(docNode, "Name");
        this.path = getString(docNode, "Path");
        this.link = getString(docNode, "Link");
        this.createdBy = getString(docNode, "Created_By");
        this.createdOn = getString(docNode, "Created_On");
        this.contractTitle = getString(docNode, "Contract_Title");
        this.ml = getString(docNode, "ML");
        this.parentsMl = getString(docNode, "Parents_ML");
        this.agreementType = getString(docNode, "Agreement_Type");
        this.documentType = getString(docNode, "Document_Type");
        this.contractStatus = getString(docNode, "Contract_Status");
        this.responsibleYahoos = getArrayAsCsv(docNode, "Responsible_Yahoos");
        this.executedAgreement = getString(docNode, "Executed_Agreement");
        this.transacting = getString(docNode, "Transacting");
        this.standardTermsAndConditions = getString(docNode, "Standard_Termsand_Conditions");
        this.contractDescriptionOverview = getString(docNode, "Contract_Description_Or_Overview");
        this.americas = getBooleanAsString(docNode, "Americas");
        this.asia = getBooleanAsString(docNode, "ASIA");
        this.emea = getBooleanAsString(docNode, "EMEA");
        this.latinAmerica = getBooleanAsString(docNode, "Latin_America");
        this.contractingParty = getString(docNode, "Contracting_Party");
        this.counterParty = getString(docNode, "Counter_Party");
        this.transactionCurrency = getString(docNode, "Transaction_Currency");
        this.transactionRate = getString(docNode, "Transaction_Rate");
        this.effectiveEndDate = getString(docNode, "Effective_End_Date");
        this.derogation = getString(docNode, "Derogation");
        this.derogationDate = getString(docNode, "Derogation_Date");
        this.vat = getString(docNode, "VAT");
        this.withHoldingTax = getString(docNode, "With_Holding_Tax");
        this.signingDate = getString(docNode, "Signing_Date");
        this.effectiveDate = getString(docNode, "Effective_Date");
        this.invoiceRequired = getString(docNode, "Invoice_Required");
        this.invoicingFrequency = getString(docNode, "Invoicing_Frequency");
        this.commentsVat = getString(docNode, "Comments_VAT");
        this.commentsWht = getString(docNode, "Comments_WHT");
        this.otherComments = getString(docNode, "Other_Comments");
    }

	private String getString(JsonNode node, String fieldName) {
		JsonNode valueNode = node.path(fieldName);
		return (!valueNode.isMissingNode() && !valueNode.isNull()) ? valueNode.asText() : null;
	}

	private String getArrayAsCsv(JsonNode node, String fieldName) {
		JsonNode arrayNode = node.get(fieldName);
		if (arrayNode != null && arrayNode.isArray()) {
			List<String> values = new ArrayList<>();
			for (JsonNode element : arrayNode) {
				values.add(element.asText());
			}
			return String.join(", ", values);
		}
		return null;
	}

	private String getBooleanAsString(JsonNode node, String fieldName) {
		JsonNode valueNode = node.path(fieldName);
		return (!valueNode.isMissingNode() && !valueNode.isNull() && valueNode.isBoolean())
				? String.valueOf(valueNode.booleanValue())
				: null;
	}

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
//			return mapper.writeValueAsString(this);
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return super.toString(); // fallback
		}
	}

}
