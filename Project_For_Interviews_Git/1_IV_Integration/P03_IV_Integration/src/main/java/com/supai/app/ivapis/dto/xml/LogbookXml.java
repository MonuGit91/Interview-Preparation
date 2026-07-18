package com.supai.app.ivapis.dto.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "IGCSecurityDocument", namespace = "http://www.infograph.com")
public class LogbookXml {

    @JacksonXmlProperty(localName = "IsoBanners")
    private IsoBanners isoBanners;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IsoBanners {
        @JacksonXmlProperty(localName = "PublishBanners")
        private PublishBanners publishBanners;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublishBanners {
        @JacksonXmlProperty(localName = "IsoBannerFontName")
        private StringValue fontName = new StringValue("Arial");

        @JacksonXmlProperty(localName = "IsoBannerFontHeight")
        private StringValue fontHeight = new StringValue("9");

        @JacksonXmlProperty(localName = "IsoBannerColor")
        private StringValue color = new StringValue("0,0,0");

        @JacksonXmlProperty(localName = "BottomLeft")
        private StringValue bottomLeft;

        @JacksonXmlProperty(localName = "BottomRight")
        private StringValue bottomRight = new StringValue("Page:%Page of %TotalPages&#10;&#1;");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StringValue {
        @JacksonXmlProperty(isAttribute = true)
        private String string;
    }
}