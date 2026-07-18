package com.supai.app.ivapis.dto.xml;

import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XmlBannersJson {

    @JsonProperty("Banners")
    private List<Banner> banners;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Banner {
        private String location;
        @JsonProperty("TextFragment")
        private TextFragment textFragment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextFragment {
        @Builder.Default
        private Boolean includesKey = false;
        @Builder.Default
        private Boolean horizontal = false;
        @Builder.Default
        private Integer index = 1;
        @Builder.Default
        private String opacity = "NaN";
        @Builder.Default
        private Integer size = 12;
        @Builder.Default
        private String font = "sans-serif";
        @Builder.Default
        private Boolean bold = false;
        @Builder.Default
        private Boolean italic = false;
        @Builder.Default
        private Boolean underline = false;
        @Builder.Default
        private String color = "#000000";
        @Builder.Default
        private LinkedHashMap<String, Object> content = new LinkedHashMap<>();
        private String string;
    }
}
