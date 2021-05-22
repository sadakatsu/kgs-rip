package com.sadakatsu.kgsrip.indexer.domain;

import com.google.common.collect.Lists;
import lombok.*;

import javax.persistence.*;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.List;

@Entity
@Table(name = "user")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class User {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private String name;
    private ZonedDateTime indexed;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "black")
    private List<Game> gamesAsBlack;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "white")
    private List<Game> gamesAsWhite;

    public List<Game> getGames() {
        final List<Game> games = Lists.newArrayList(getGamesAsBlack());
        games.addAll(getGamesAsWhite());
        return games;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<Rating> ratings;
}
