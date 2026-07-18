//package com.supai.app.modal;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//
////import org.springframework.data.annotation.Id;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "documents")
//@Data
//@NoArgsConstructor      // For JPA
//@AllArgsConstructor     // For convenience
//public class Document {
////	@Id
////	@GeneratedValue(strategy = GenerationType.SEQUENCE)
////	private Integer serial_no; 
//	
//	@Id
//    @Column(name = "doc_id")
//    private String docId;
//	
//	@Column(name = "parent_id")
//    private String parentId;
//
//    @Column(name = "upload_date")
//    private LocalDate uploadDate;
//
//    @Column(name = "name")
//    private String name;
//
//    @Column(name = "link")
//    private String link;
//
//    @Column(name = "path")
//    private String path;
//
//    @Column(name = "created_by")
//    private String createdBy;
//
//    @Column(name = "created_on")
//    private String createdOn;
//
//    @Column(name = "contract_title")
//    private String contractTitle;
//
//    @Column(name = "ml")
//    private String ml;
//
//    @Column(name = "parents_ml")
//    private String parentsMl;
//
//    @Column(name = "agreement_type")
//    private String agreementType;
//
//    @Column(name = "document_type")
//    private String documentType;
//
//    @Column(name = "contract_status")
//    private String contractStatus;
//
//    @Column(name = "responsible_yahoos")
//    private String responsibleYahoos;
//
//    @Column(name = "executed_agreement")
//    private String executedAgreement;
//
//    @Column(name = "transacting")
//    private String transacting;
//
//    @Column(name = "standard_terms_and_conditions")
//    private String standardTermsAndConditions;
//
//    @Column(name = "contract_description_overview")
//    private String contractDescriptionOverview;
//
//    @Column(name = "americas")
//    private String americas;
//
//    @Column(name = "asia")
//    private String asia;
//
//    @Column(name = "emea")
//    private String emea;
//
//    @Column(name = "latin_america")
//    private String latinAmerica;
//
//    @Column(name = "contracting_party")
//    private String contractingParty;
//
//    @Column(name = "counter_party")
//    private String counterParty;
//
//    @Column(name = "transaction_currency")
//    private String transactionCurrency;
//
//    @Column(name = "transaction_rate")
//    private String transactionRate;
//
//    @Column(name = "effective_end_date")
//    private String effectiveEndDate;
//
//    @Column(name = "derogation")
//    private String derogation;
//
//    @Column(name = "derogation_date")
//    private String derogationDate;
//
//    @Column(name = "vat")
//    private String vat;
//
//    @Column(name = "with_holding_tax")
//    private String withHoldingTax;
//
//    @Column(name = "signing_date")
//    private String signingDate;
//
//    @Column(name = "effective_date")
//    private String effectiveDate;
//
//    @Column(name = "invoice_required")
//    private String invoiceRequired;
//
//    @Column(name = "invoicing_frequency")
//    private String invoicingFrequency;
//
//    @Column(name = "comments_vat")
//    private String commentsVat;
//
//    @Column(name = "comments_wht")
//    private String commentsWht;
//
//    @Column(name = "other_comments")
//    private String otherComments;
//
//    public Document(JsonNode docNode) {
//        this.uploadDate = LocalDate.now();
//        this.docId = getString(docNode, "DocId");
//        this.parentId = getString(docNode, "ParentId");
//        this.name = getString(docNode, "Name");
//        this.path = getString(docNode, "Path");
//        this.link = getString(docNode, "Link");
//        this.createdBy = getString(docNode, "Created By");
//        this.createdOn = getString(docNode, "Created On");
//        this.contractTitle = getString(docNode, "ContractTitle");
//        this.ml = getString(docNode, "ML");
//        this.parentsMl = getString(docNode, "ParentsML");
//        this.agreementType = getString(docNode, "AgreementType");
//        this.documentType = getString(docNode, "DocumentType");
//        this.contractStatus = getString(docNode, "ContractStatus");
//        this.responsibleYahoos = getArrayAsCsv(docNode, "ResponsibleYahoos");
//        this.executedAgreement = getString(docNode, "ExecutedAgreement");
//        this.transacting = getString(docNode, "Transacting");
//        this.standardTermsAndConditions = getString(docNode, "StandardTermsandConditions");
//        this.contractDescriptionOverview = getString(docNode, "ContractDescriptionOverview");
//        this.americas = getBooleanAsString(docNode, "Americas");
//        this.asia = getBooleanAsString(docNode, "ASIA");
//        this.emea = getBooleanAsString(docNode, "EMEA");
//        this.latinAmerica = getBooleanAsString(docNode, "LatinAmerica");
//        this.contractingParty = getString(docNode, "ContractingParty");
//        this.counterParty = getString(docNode, "CounterParty");
//        this.transactionCurrency = getString(docNode, "TransactionCurrency");
//        this.transactionRate = getString(docNode, "TransactionRate");
//        this.effectiveEndDate = getString(docNode, "EffectiveEndDate");
//        this.derogation = getString(docNode, "Derogation");
//        this.derogationDate = getString(docNode, "DerogationDate");
//        this.vat = getString(docNode, "VAT");
//        this.withHoldingTax = getString(docNode, "WithHoldingTax");
//        this.signingDate = getString(docNode, "SigningDate");
//        this.effectiveDate = getString(docNode, "EffectiveDate");
//        this.invoiceRequired = getString(docNode, "InvoiceRequired");
//        this.invoicingFrequency = getString(docNode, "InvoicingFrequency");
//        this.commentsVat = getString(docNode, "CommentsVAT");
//        this.commentsWht = getString(docNode, "CommentsWHT");
//        this.otherComments = getString(docNode, "OtherComments");
//    }
//
//
//	private String getString(JsonNode node, String fieldName) {
//	    JsonNode valueNode = node.path(fieldName);
//	    return (!valueNode.isMissingNode() && !valueNode.isNull()) ? valueNode.asText() : null;
//	}
//
//	private String getArrayAsCsv(JsonNode node, String fieldName) {
//	    JsonNode arrayNode = node.get(fieldName);
//	    if (arrayNode != null && arrayNode.isArray()) {
//	        List<String> values = new ArrayList<>();
//	        for (JsonNode element : arrayNode) {
//	            values.add(element.asText());
//	        }
//	        return String.join(", ", values);
//	    }
//	    return null;
//	}
//
//	private String getBooleanAsString(JsonNode node, String fieldName) {
//	    JsonNode valueNode = node.path(fieldName);
//	    return (!valueNode.isMissingNode() && !valueNode.isNull() && valueNode.isBoolean()) ? String.valueOf(valueNode.booleanValue()) : null;
//	}
//
//
//	@Override
//	public String toString() {
//		ObjectMapper mapper = new ObjectMapper();
//		try {
////			return mapper.writeValueAsString(this);
//			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
//		} catch (JsonProcessingException e) {
//			e.printStackTrace();
//			return super.toString(); // fallback
//		}
//	}
//
//}
