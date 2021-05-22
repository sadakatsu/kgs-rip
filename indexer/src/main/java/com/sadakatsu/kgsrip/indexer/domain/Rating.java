package com.sadakatsu.kgsrip.indexer.domain;

import lombok.*;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "rating")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Rating {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @JoinColumn(name = "user")
    @ManyToOne(fetch = FetchType.EAGER)
    private User user;

    private ZonedDateTime date;

    private double rating;
}
