package com.sadakatsu.kgsrip.indexer.domain;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "game")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Game {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private String url;

    @JoinColumn(name = "white")
    @ManyToOne(fetch = FetchType.EAGER)
    private User white;

    @Column(name = "white_rank")
    private String whiteRank;

    @JoinColumn(name = "black")
    @ManyToOne(fetch = FetchType.EAGER)
    private User black;

    @Column(name = "black_rank")
    private String blackRank;

    private String setup;

    @Column(name = "start_time")
    private ZonedDateTime startTime;

    private String type;

    private String result;

    @Builder.Default
    private double random = ThreadLocalRandom.current().nextDouble();
}
