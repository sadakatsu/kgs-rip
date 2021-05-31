package com.sadakatsu.kgsrip.indexer.repositories;

import com.sadakatsu.kgsrip.indexer.domain.Game;
import com.sadakatsu.kgsrip.indexer.domain.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;
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

    @Query(nativeQuery = true, value = "explain select count(*) from game")
    Object[][] explainCount();

    default long guessCount() {
        final var explanation = explainCount()[0];
        final var value = (BigInteger) explanation[9];
        return value.longValue();
    }
}
