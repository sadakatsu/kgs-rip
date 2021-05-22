package com.sadakatsu.kgsrip.indexer.repositories;

import com.sadakatsu.kgsrip.indexer.domain.Game;
import com.sadakatsu.kgsrip.indexer.domain.User;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface GameRepository extends PagingAndSortingRepository<Game, Long> {
    Optional<Game> findFirstByUrl(String url);
    Optional<Game> findFirstByBlackAndWhiteAndStartTime(
        User black,
        User white,
        ZonedDateTime startTime
    );

    default Optional<Game> seekExisting(String url, User black, User white, ZonedDateTime timestamp) {
        return url != null ? findFirstByUrl(url) : findFirstByBlackAndWhiteAndStartTime(black, white, timestamp);
    }
}
