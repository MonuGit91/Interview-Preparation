package com.supai.app.dao.dto;

import lombok.Data;

@Data
public class DocumentRequest implements Cloneable, Request {
    private String otcsTicket;
    private String otcsDocId;
    private String userId;
    private String baseUrl;
    private String email;
    private String categoryId;
    private String feildIdOf_GDriveDocId;
    private String feildIdOf_EditorId;

    @Override
    public DocumentRequest clone() {
        try {
            return (DocumentRequest) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
