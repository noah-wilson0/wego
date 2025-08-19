package com.wego.wego.domain.chemi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="chemi")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Chemi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chemi_id")
    private Long id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "image", nullable = false)
    private String image;
    @Column(name = "description", nullable = false)
    private String description;

    @ManyToMany
    @JoinTable(
            name = "chemi_similar",
            joinColumns = @JoinColumn(name = "chemi_id"),
            inverseJoinColumns = @JoinColumn(name = "similar_chemi_id")
    )
    @JsonIgnore
    private Set<Chemi> similarChemis = new HashSet<>();
}
