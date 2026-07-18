package com.supai.app.ivapis.dto.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "IsoBannersAndWatermarks", namespace = "http://www.opentext.com/renditions/cdl/banners")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XmlBanner {

    public static final String PAGE_NUMBER_TEMPLATE = "Page:%Page of %TotalPages";

    @Builder.Default
    @JacksonXmlProperty(isAttribute = true)
    private String version = "1.0";

    @JacksonXmlProperty(localName = "IsoBannersPageGroup")
    private IsoBannersPageGroup isoBannersPageGroup;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IsoBannersPageGroup {
        @Builder.Default
        @JacksonXmlProperty(isAttribute = true)
        private String min_page = "";
        @Builder.Default
        @JacksonXmlProperty(isAttribute = true)
        private String max_page = "";
        @JacksonXmlProperty(localName = "PublishBanners")
        private PublishBanners publishBanners;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublishBanners {
        @JacksonXmlElementWrapper(localName = "Banners")
        @JacksonXmlProperty(localName = "Banner")
        private List<Banner> banners;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Banner {
        @JacksonXmlProperty(isAttribute = true)
        private String location;
        @JacksonXmlProperty(localName = "TextFragment")
        private TextFragment textFragment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TextFragment {
        @JacksonXmlProperty(isAttribute = true)
        private Integer index;
        @JacksonXmlProperty(isAttribute = true)
        private String opacity;
        @JacksonXmlProperty(isAttribute = true)
        private Integer size;
        @JacksonXmlProperty(isAttribute = true)
        private String font;
        @JacksonXmlProperty(isAttribute = true)
        private Boolean bold;
        @JacksonXmlProperty(isAttribute = true)
        private Boolean italic;
        @JacksonXmlProperty(isAttribute = true)
        private Boolean underline;
        @JacksonXmlProperty(isAttribute = true)
        private String color;

        @com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
        private String content;
    }
}
