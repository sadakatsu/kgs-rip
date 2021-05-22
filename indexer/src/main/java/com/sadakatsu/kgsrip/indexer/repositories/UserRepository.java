package com.sadakatsu.kgsrip.indexer.repositories;

import com.sadakatsu.kgsrip.indexer.domain.User;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;

public interface UserRepository extends PagingAndSortingRepository<User, Long> {
    Stream<User> findByIndexedIsNullOrIndexedLessThan(ZonedDateTime endDate);
    Optional<User> findFirstByName(String name);
}
