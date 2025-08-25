package com.wego.wego.domain.member.entity;

import com.wego.wego.domain.plan.entity.TravelPlan;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Table
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "chemi_id")
    @Builder.Default
    private Long chemiId=null;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    List<TravelPlan> travelPlans=new ArrayList<>();

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private String role="USER";

    @Column(name = "created_at")
    @Builder.Default
    private LocalDate created_at=LocalDate.now();



    public void updateChemiId(Long chemiId) {
        this.chemiId=chemiId;
    }
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }
    public void changeName(String newName) {
        this.name = newName;
    }
}
