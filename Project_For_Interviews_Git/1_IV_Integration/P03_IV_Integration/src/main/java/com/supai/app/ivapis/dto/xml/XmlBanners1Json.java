package com.supai.app.ivapis.dto.xml;

import java.util.LinkedHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XmlBanners1Json {

    @Builder.Default
    private String fontName = "Arial";

    @Builder.Default
    private String fontHeight = "9";

    @Builder.Default
    private String color = "0,0,0";

    private BannerContent topCenter;
    private BannerContent bottomLeft;
    private BannerContent bottomCenter;
    private BannerContent bottomRight;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BannerContent {
        @Builder.Default
        private Boolean includesKey = false;
        @Builder.Default
        private Boolean horizontal = false;
        @Builder.Default
        private LinkedHashMap<String, Object> content = new LinkedHashMap<>();
    }
}
