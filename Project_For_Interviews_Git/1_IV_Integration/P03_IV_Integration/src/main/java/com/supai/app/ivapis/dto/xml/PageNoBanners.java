package com.supai.app.ivapis.dto.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "IsoBannersAndWatermarks", namespace = "http://www.opentext.com/renditions/cdl/banners")
public class PageNoBanners {

    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private String xmlns = "http://www.opentext.com/renditions/cdl/banners";

    @JacksonXmlProperty(localName = "xmlns:xsi", isAttribute = true)
    @Builder.Default
    private String xmlnsXsi = "http://www.w3.org/2001/XMLSchema-instance";

    @JacksonXmlProperty(localName = "xsi:schemaLocation", isAttribute = true)
    @Builder.Default
    private String schemaLocation = "http://www.opentext.com/renditions/cdl/banners OTCDLIsoBanners.xsd";

    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private String version = "1.0";

    @JacksonXmlProperty(localName = "IsoBannersPageGroup")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<IsoBannersPageGroup> pageGroups;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IsoBannersPageGroup {
        @JacksonXmlProperty(isAttribute = true)
        private int min_page;
        @JacksonXmlProperty(isAttribute = true)
        private int max_page;

        @JacksonXmlProperty(localName = "PublishBanners")
        private PublishBanners publishBanners;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublishBanners {
        @JacksonXmlProperty(localName = "Banners")
        private Banners banners;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Banners {
        @JacksonXmlProperty(localName = "Banner")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Banner> bannerList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Banner {
        @JacksonXmlProperty(isAttribute = true)
        private String location;

        @JacksonXmlProperty(localName = "TextFragment")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<TextFragment> textFragments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextFragment {
        @JacksonXmlProperty(isAttribute = true)
        private String index;
        @JacksonXmlProperty(isAttribute = true)
        private String opacity;
        @JacksonXmlProperty(isAttribute = true)
        private String size;
        @JacksonXmlProperty(isAttribute = true)
        private String font;
        @JacksonXmlProperty(isAttribute = true)
        private String bold;
        @JacksonXmlProperty(isAttribute = true)
        private String italic;
        @JacksonXmlProperty(isAttribute = true)
        private String underline;
        @JacksonXmlProperty(isAttribute = true)
        private String color;

        @JacksonXmlText
        private String content;
    }

    /**
     * Creates an IsoBannersAndWatermarks object with a page group for each page,
     * containing a "Page:X of N" banner in the BottomRight location.
     * 
     * @param totalPages The total number of pages in the document.
     * @return An IsoBannersAndWatermarks object populated with page groups.
     */
    public static PageNoBanners generateForPageCount(int totalPages) {
        List<IsoBannersPageGroup> groups = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            TextFragment textFragment = TextFragment.builder()
                    .index("1")
                    .opacity("NaN")
                    .size("12")
                    .font("sans-serif")
                    .bold("false")
                    .italic("false")
                    .underline("false")
                    .content("Page:" + (i + 1) + " of " + totalPages)
                    .build();

            Banner banner = Banner.builder()
                    .location("BottomRight")
                    .textFragments(List.of(textFragment))
                    .build();

            Banners banners = Banners.builder()
                    .bannerList(List.of(banner))
                    .build();

            PublishBanners publishBanners = PublishBanners.builder()
                    .banners(banners)
                    .build();

            groups.add(IsoBannersPageGroup.builder()
                    .min_page(i)
                    .max_page(i)
                    .publishBanners(publishBanners)
                    .build());
        }
        return PageNoBanners.builder()
                .pageGroups(groups)
                .build();
    }

    public String toXmlString(XmlMapper xmlMapper) {
        try {
            String xmlContent = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
            // Post-process to ensure numeric entities like &#10; are not double-escaped as
            // &amp;#10;
            return xmlContent.replaceAll("&amp;#", "&#");
        } catch (Exception e) {
            throw new RuntimeException("Error converting PageNoBanners to XML string", e);
        }
    }
}
