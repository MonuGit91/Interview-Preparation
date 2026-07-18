package com.supai.app.otcsapis.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOrCopyNodeResponseDto {
    private Links links;
    private Results results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Links {
        private Data data;

        @lombok.Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public class Data {
            private Self self;

            @lombok.Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public class Self {
                private String body;
                private String content_type;
                private String href;
                private String method;
                private String name;
            }
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Results {
        private Data data;

        @lombok.Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public class Data {
            private Properties properties;

            @lombok.Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public class Properties {
                private boolean advanced_versioning;
                private boolean container;
                private int container_size;
                private String create_date;
                private int create_user_id;
                private String description;
                private DescriptionMultilingual description_multilingual;
                private Object external_create_date;
                private String external_identity;
                private String external_identity_type;
                private Object external_modify_date;
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
                private NameMultilingual name_multilingual;
                private String owner;
                private int owner_group_id;
                private int owner_user_id;
                private int parent_id;
                private String permissions_model;
                private List<String> preferred_rendition_type;
                private boolean reserved;
                private Object reserved_date;
                private boolean reserved_shared_collaboration;
                private int reserved_user_id;
                private int size;
                private String size_formatted;
                private Object status;
                private int type;
                private String type_name;
                private boolean versionable;
                private boolean versions_control_advanced;
                private int volume_id;

                @lombok.Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public class DescriptionMultilingual {
                    private String en;
                    private String es_AR;
                    private String pt_BR;
                }

                @lombok.Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public class NameMultilingual {
                    private String en;
                    private String es_AR;
                    private String pt_BR;
                }
            }
        }
    }

}   
