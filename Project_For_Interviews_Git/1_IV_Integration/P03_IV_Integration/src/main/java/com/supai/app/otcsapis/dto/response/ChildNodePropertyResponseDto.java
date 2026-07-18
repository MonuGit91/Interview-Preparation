package com.supai.app.otcsapis.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChildNodePropertyResponseDto {
    private Collection collection;
    private Links links;
    private List<Results> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Collection {
        private Paging paging;
        private Sorting sorting;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Paging {
            private int limit;
            private int page;
            private int page_total;
            private int range_max;
            private int range_min;
            private int total_count;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Sorting {
            private List<Sort> sort;

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Sort {
                private String key;
                private String value;
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        private Data data;

        @lombok.Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Data {
            private Self self;

            @lombok.Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Self {
                private String body;
                private String content_type;
                private String href;
                private String method;
                private String name;
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Results {
        private Data data;

        @lombok.Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Data {
            private Properties properties;

            @lombok.Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Properties {

                @lombok.Data
                @Builder
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Multilingual {
                    private String en;
                    private String es_AR;
                    private String pt_BR;
                }

                private boolean advanced_versioning;
                private boolean container;
                private int container_size;
                private String create_date;
                private int create_user_id;
                private String description;
                private Multilingual description_multilingual;
                private String external_create_date;
                private String external_identity;
                private String external_identity_type;
                private String external_modify_date;
                private String external_source;
                private boolean favorite;
                private boolean hidden;
                private String icon;
                private String icon_large;
                private int id;
                private String mime_type;
                private String modify_date;
                private int modify_user_id;
                private String name;
                private Multilingual name_multilingual;
                private String owner;
                private int owner_group_id;
                private int owner_user_id;
                private int parent_id;
                private String permissions_model;
                private List<String> preferred_rendition_type;
                private boolean reserved;
                private String reserved_date;
                private boolean reserved_shared_collaboration;
                private int reserved_user_id;
                private int size;
                private String size_formatted;
                private String status;
                private int type;
                private String type_name;
                private boolean versionable;
                private boolean versions_control_advanced;
                private int volume_id;
                private int wnd_version;
                private String wnd_version_formatted;
            }
        }
    }
}
