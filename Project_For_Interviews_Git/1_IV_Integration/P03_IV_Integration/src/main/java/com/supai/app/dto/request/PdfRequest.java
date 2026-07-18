package com.supai.app.dto.request;

import com.supai.app.ivapis.dto.xml.XmlBannersJson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PdfRequest {
    private ToPdfRequest pdfRequest;
    private XmlBannersJson bannerJson;
}
