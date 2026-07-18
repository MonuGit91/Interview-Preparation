package com.supai.app.dto.request;

import com.supai.app.ivapis.dto.xml.XmlBanners1Json;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PdfRequest1 {
    private ToPdfRequest pdfRequest;
    private XmlBanners1Json banner1Json;
}
