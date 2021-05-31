package com.sadakatsu.kgsrip.indexer.repositories;

import com.sadakatsu.kgsrip.indexer.domain.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;

public interface UserRepository extends PagingAndSortingRepository<User, Long> {
    long countByIndexedIsNotNullAndIndexedLessThan(ZonedDateTime endDate);
    long countByIndexedIsNotNullAndIndexedGreaterThanEqual(ZonedDateTime endDate);
    Stream<User> findByIndexedIsNullOrIndexedLessThan(ZonedDateTime endDate);
    Optional<User> findFirstByName(String name);

    @Query(nativeQuery = true, value = "explain select count(*) from user")
    Object[][] explainCount();

    default long guessCount() {
        final var explanation = explainCount()[0];
        final var value = (BigInteger) explanation[9];
        return value.longValue();
    }
}
