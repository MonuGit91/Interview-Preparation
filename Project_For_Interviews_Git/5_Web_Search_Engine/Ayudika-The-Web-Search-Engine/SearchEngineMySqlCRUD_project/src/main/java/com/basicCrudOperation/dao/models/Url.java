package com.basicCrudOperation.dao.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "url")
@Data
@JsonIgnoreProperties
public class Url {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "url_id")
    private int id;
    @Column(name = "url")
    private String url;
}
