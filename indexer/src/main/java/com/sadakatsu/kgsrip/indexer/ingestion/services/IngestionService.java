package com.sadakatsu.kgsrip.indexer.ingestion.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sadakatsu.kgsrip.indexer.domain.Game;
import com.sadakatsu.kgsrip.indexer.domain.User;
import com.sadakatsu.kgsrip.indexer.repositories.GameRepository;
import com.sadakatsu.kgsrip.indexer.repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Profile("ingest")
@Service
@Slf4j
public class IngestionService implements DisposableBean {
    @AllArgsConstructor
    @Getter
    private static class PlayerEntry {
        User user;
        String rank;
    }

    private final AtomicBoolean run;
    private final ZonedDateTime endDate;
    private final ZonedDateTime startDate;
    private final GameRepository gameRepository;
    private final HtmlCleaner cleaner;
    private final Pattern gameTimestampRegex;
    private final Pattern playerRankRegex;
    private final PlatformTransactionManager transactionManager;
    private final Set<Long> reservations;
    private final String urlFormat;
    private final ThreadPoolExecutor executor;
    private final UserRepository userRepository;
    private final XPathExpression blackExpression;
    private final XPathExpression gameRow;
    private final XPathExpression gameSetup;
    private final XPathExpression gameResult;
    private final XPathExpression gameTimestamp;
    private final XPathExpression gameType;
    private final XPathExpression gameUrl;
    private final XPathExpression hasNoGames;
    private final XPathExpression whiteExpression;

    public IngestionService(
        @Value("#{T(java.time.ZonedDateTime).parse('${indexer.ingest.end_date}')}") ZonedDateTime endDate,
        @Value("#{T(java.time.ZonedDateTime).parse('${indexer.ingest.start_date}')}") ZonedDateTime startDate,
        @Value("${indexer.ingest.threads}") int ingestionThreads,
        @Value("${indexer.ingest.url}") String urlFormat,
        GameRepository gameRepository,
        PlatformTransactionManager transactionManager,
        UserRepository userRepository
    ) {
        this.cleaner = new HtmlCleaner();
        this.endDate = endDate;
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(ingestionThreads);
        this.gameRepository = gameRepository;
        this.gameTimestampRegex = Pattern.compile(
            "(?<month>\\d+)/(?<day>\\d+)/(?<year>\\d+)\\s+(?<hour>\\d+):(?<minute>\\d+)\\s+(?<period>[AP]M)"
        );
        this.playerRankRegex = Pattern.compile("(?<player>[^\\s\\[]+)(?:\\s*\\[(?<rank>[^]]+)])?");
        this.reservations = Sets.newConcurrentHashSet();
        this.run = new AtomicBoolean(false);
        this.startDate = startDate;
        this.transactionManager = transactionManager;
        this.urlFormat = urlFormat;
        this.userRepository = userRepository;

        final var xpathFactory = XPathFactory.newInstance();
        final var xpath = xpathFactory.newXPath();
        try {
            this.blackExpression = xpath.compile("./td[3]/a");
            this.gameRow = xpath.compile("/html/body/table[1]/tbody/tr[td]");
            this.gameSetup = xpath.compile("./td[4]/text()");
            this.gameResult = xpath.compile("./td[7]/text()");
            this.gameTimestamp = xpath.compile("./td[5]/text()");
            this.gameType = xpath.compile("./td[6]/text()");
            this.gameUrl = xpath.compile("./td[1]/a");
            this.hasNoGames = xpath.compile("//*[contains(text(), 'did not play any games')]");
            this.whiteExpression = xpath.compile("./td[2]/a");
        } catch (XPathExpressionException e) {
            log.error("Tried to build an invalid XPath.", e);
            throw new IllegalStateException(e);
        }

        for (var i = 0; i < ingestionThreads; ++i) {
            final var worker = createWorker();
            executor.execute(worker);
        }
    }

    public void startIngestion() {
        run.set(true);
    }

    public void pauseIngestion() {
        run.set(false);
    }

    @Override
    public void destroy() throws Exception {
        log.info("Attempting to stop workers...");
        run.set(false);
        executor.shutdownNow();
        final var succeeded = executor.awaitTermination(3600, TimeUnit.SECONDS);
        log.info("Shutdown " + (succeeded ? "succeeded." : "failed."));
    }

    @SuppressWarnings({ "java:S1121", "java:S2142" })
    private Runnable createWorker() {
        return () -> {
            final var thread = Thread.currentThread();
            final var threadName = thread.getName();

            while (!Thread.interrupted()) {
                try {
                    Optional<User> reservation;
                    if (run.get() && (reservation = reserve()).isPresent()) {
                        var user = reservation.get();
                        final var id = user.getId();
                        final var username = user.getName();
                        log.info("Thread {} reserved User {} ( {} ).", threadName, id, username);

                        var done = false;
                        while (
                            run.get() &&
                            !(done = hasIndexedLastMonth(user.getIndexed()))
                        ) {
                            final var nextDate = getNextDate(user);
                            final var year = nextDate.getYear();
                            final var month = nextDate.getMonthValue();
                            final var url = composeUrl(username, year, month);
                            final var document = loadArchivePageFailFast(url);
                            if (document != null) {
                                var truncated = false;
                                if (hasGames(document)) {
                                    final var gameRows = getGameRows(document);
                                    for (
                                        int i = 0, count = gameRows.getLength();
                                        !truncated && i < count;
                                        ++i, truncated = !run.get()
                                    ) {
                                        final var row = gameRows.item(i);
                                        if (isDemonstration(row)) {
                                            continue;
                                        }

                                        final var type = getGameType(row);
                                        final var timestamp = getGameTimestamp(row);
                                        final var allWhite = getPlayers(row, whiteExpression);
                                        final var allBlack = getPlayers(row, blackExpression);

                                        if (allWhite.size() != 1 || allBlack.size() != 1) {
                                            log.info(
                                                "Thread {} found a {} game on {} with {} total players.  Skipping.",
                                                threadName,
                                                type,
                                                timestamp,
                                                allWhite.size() + allBlack.size()
                                            );
                                            continue;
                                        }

                                        if (!isAcceptableType(type)) {
                                            log.info(
                                                "Thread {} found a {} game on {}.  Skipping.",
                                                threadName,
                                                type,
                                                timestamp
                                            );
                                            continue;
                                        }

                                        final var setup = getGameSetup(row);
                                        if (!setup.startsWith("19")) {
                                            log.info(
                                                "Thread {} found a {} game on {}.  Skipping.",
                                                threadName,
                                                setup,
                                                timestamp
                                            );
                                            continue;
                                        }

                                        final var result = getGameResult(row);
                                        if (!(result.startsWith("B+") || result.startsWith("W+"))) {
                                            log.info(
                                                "Thread {} found a {} game on {}.  Skipping.",
                                                threadName,
                                                result,
                                                timestamp
                                            );
                                            continue;
                                        }

                                        final var white = allWhite.get(0);
                                        final var whiteUser = white.getUser();
                                        final var black = allBlack.get(0);
                                        final var blackUser = black.getUser();

                                        final var builder = Game.builder()
                                            .black(blackUser)
                                            .blackRank(black.getRank())
                                            .result(result)
                                            .setup(setup)
                                            .startTime(timestamp)
                                            .type(type)
                                            .white(whiteUser)
                                            .whiteRank(white.getRank());

                                        final var downloadUrl = getGameUrl(row);
                                        final var gameUrl = downloadUrl.orElse(null);
                                        if (gameUrl != null) {
                                            builder.url(gameUrl);
                                        }

                                        final var game = builder.build();
                                        Optional<Game> found = gameRepository.seekExisting(
                                            gameUrl,
                                            blackUser,
                                            whiteUser,
                                            timestamp
                                        );
                                        if (found.isEmpty()) {
                                            try {
                                                gameRepository.save(game);
                                                log.info(
                                                    "Thread {} discovered a new Game {} - {} with {} at {}!",
                                                    threadName,
                                                    whiteUser.getName(),
                                                    blackUser.getName(),
                                                    setup,
                                                    timestamp
                                                );
                                            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                                                // Developer's Note: (J. Craig, 2021-05-23)
                                                // This means another thread found this and created it after the guard check.  This should be
                                                // fine.
                                            } catch (RuntimeException e) {
                                                // Developer's Note: (J. Craig, 2021-05-23)
                                                // I do not know what this would be.  This is bad.
                                                log.error("Failed to save a valid, desirable Game!", e);
                                                throw new IllegalStateException(e);
                                            }
                                        }
                                    }
                                }

                                if (!truncated) {
                                    user.setIndexed(nextDate);
                                    userRepository.save(user);
                                    log.info(
                                        "Thread {} completed User {} ( {} ) for {}-{}.",
                                        threadName,
                                        id,
                                        username,
                                        year,
                                        month
                                    );
                                } else {
                                    log.info(
                                        "Thread {} was interrupted for User {} ( {} ) for {}-{}.",
                                        threadName,
                                        id,
                                        username,
                                        year,
                                        month
                                    );
                                }
                            }

                            if (document == null) {
                                sleepRandomDuration();
                            }
                        }

                        reservations.remove(id);
                        log.info(
                            "Thread {} {} User {} ( {} ).",
                            threadName,
                            done ? "completed" : "released",
                            id,
                            username
                        );
                    } else {
                        sleepRandomDuration();
                    }
                } catch (InterruptedException e) {
                    log.info("Received an InterruptedException.", e);
                }
            }
            log.info("Thread {} has completed.", Thread.currentThread().getName());
        };
    }

    private void sleepRandomDuration() throws InterruptedException {
        var duration = Math.round(
            ThreadLocalRandom.current()
                .nextGaussian() * 200 + 3000
        );
        if (duration < 150) {
            duration = 150;
        }
        Thread.sleep(duration);
    }

    public Optional<User> reserve() {
        Optional<User> found;

        final var thread = Thread.currentThread();
        final var name = thread.getName();
        log.info("Thread {} attempting to reserve a user...", name);

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        template.setReadOnly(true);

        found = template.execute(
            status -> {
                try (Stream<User> stream = userRepository.findByIndexedIsNullOrIndexedLessThan(endDate)) {
                    return stream.filter(this::_attemptToReserve)
                        .findFirst();
                }
            }
        );

        if (found.isEmpty()) {
            log.info("Thread {} found no user to reserve.", name);
        }

        return found;
    }

    private boolean _attemptToReserve(User user) {
        final var id = user.getId();
        return reservations.add(id);
    }

    private boolean hasIndexedLastMonth(ZonedDateTime indexed) {
        return indexed != null && indexed.compareTo(endDate) >= 0;
    }

    private ZonedDateTime getNextDate(User user) {
        ZonedDateTime next;

        final var last = user.getIndexed();
        if (last == null) {
            next = startDate;
        } else {
            final var gmt = last.withZoneSameInstant(ZoneId.of("GMT"));
            next = gmt.plusMonths(1L);
            if (gmt.getDayOfMonth() != 1 || next.getDayOfMonth() != 1) {
                log.error("I'm having the same problem as before: {}, {}", gmt, next);
            }
        }

        return next;
    }

    private URL composeUrl(String username, int year, int month) {
        URL url;

        // DEBUG: Can random parameters bypass DOS protection?  Also, why on earth do I need to try something like this?
        final var uuid = UUID.randomUUID();
        final var urlString = String.format(urlFormat, username, year, month, uuid);
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            log.error("Failed to create a valid URL.  This is a configuration error: {}", urlString);
            throw new IllegalStateException(e);
        }

        return url;
    }

    private Document loadArchivePageFailFast(URL url) {
        Document contents = null;

        try {
            final var connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            final var status = connection.getResponseCode();

            if (status != 200) {
                log.debug("Received status {} for URL {}.", status, url);
            } else {
                TagNode root;
                try (var is = connection.getInputStream()) {
                    root = cleaner.clean(is);
                }

                contents = new DomSerializer(new CleanerProperties())
                    .createDOM(root);
            }
        } catch (IOException e) {
            log.error("Failed to load a KGS Archives page.", e);
            throw new IllegalStateException(e);
        } catch (ParserConfigurationException e) {
            log.error("Failed to convert the loaded page into a Document.", e);
            throw new IllegalStateException(e);
        }

        return contents;
    }

    private boolean hasGames(Document document) {
        boolean result = false;

        if (document != null) {
            try {
                final var found = (NodeList) hasNoGames.evaluate(document, XPathConstants.NODESET);
                result = found.getLength() == 0;
            } catch (XPathExpressionException e) {
                log.error("An error occurred while trying to evaluate the hasNoGames expression.", e);
                throw new IllegalStateException(e);
            }
        }

        return result;
    }

    private NodeList getGameRows(Document document) {
        try {
            return (NodeList) gameRow.evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            log.error("An error occurred while trying to evaluate the gameRow expression.", e);
            throw new IllegalStateException(e);
        }
    }

    private boolean isDemonstration(Node row) {
        // Developer's Note: (J. Craig, 2021-05-23)
        // KGS Archives uses a cell that spans two columns for White and Black for demonstrations.  I could also check
        // the type, but that requires a variable reference to `cells.item(-2)` after checking the length to determine
        // what index is -2, anyway.
        final var cells = row.getChildNodes();
        return cells.getLength() == 6;
    }

    private String getGameType(Node row) {
        String type;

        try {
            type = (String) gameType.evaluate(row, XPathConstants.STRING);
            type = type.strip();
        } catch (XPathExpressionException e) {
            log.error("An error occurred while trying to get the game's type.", e);
            throw new IllegalStateException(e);
        }

        return type;
    }

    private ZonedDateTime getGameTimestamp(Node row) {
        ZonedDateTime timestamp = null;

        String original = null;
        try {
            original = (String) gameTimestamp.evaluate(row, XPathConstants.STRING);
            original = original.strip();

            final var matcher = gameTimestampRegex.matcher(original);
            if (!matcher.matches()) {
                final var message = String.format("The timestamp did not match the regex: `%s`", original);
                throw new IllegalArgumentException(message);
            }

            final var month = Integer.parseInt(matcher.group("month"));
            final var day = Integer.parseInt(matcher.group("day"));
            final var minute = Integer.parseInt(matcher.group("minute"));
            final var period = matcher.group("period");

            var year = Integer.parseInt(matcher.group("year"));
            if (year < 100) {
                year += 2000;
            }


            var hour = Integer.parseInt(matcher.group("hour"));
            if ("AM".equals(period) && hour == 12) {
                hour = 0;
            }
            if ("PM".equals(period) && hour != 12) {
                hour += 12;
            }

            timestamp = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("GMT"));
        } catch (XPathExpressionException | RuntimeException e) {
            log.error(
                "An error occurred while trying to parse the game's timestamp: {}",
                original
            );
            throw new IllegalStateException(e);
        }

        return timestamp;
    }

    private List<PlayerEntry> getPlayers(Node row, XPathExpression expression) {
        List<PlayerEntry> players = Lists.newArrayList();

        final var thread = Thread.currentThread();
        final var threadName = thread.getName();
        try {
            final var found = (NodeList) expression.evaluate(row, XPathConstants.NODESET);
            for (int i = 0, count = found.getLength(); i < count; ++i) {
                final var address = found.item(i);
                final var userAndRank = address.getTextContent().strip();
                final var match = playerRankRegex.matcher(userAndRank);
                if (!match.matches()) {
                    log.error(
                        "Thread {} found a player entry that doesn't follow the known format: {}",
                        threadName,
                        userAndRank
                    );
                    throw new IllegalStateException("Could not parse PlayerEntry.");
                }

                final var username = match.group("player");
                final var user = getOrCreateUser(username);
                final var rank = match.group("rank");
                final var entry = new PlayerEntry(user, rank);
                players.add(entry);
            }
        } catch (XPathExpressionException e) {
            log.error("An error occurred while trying to extract the game's players.", e);
            throw new IllegalStateException(e);
        }

        return players;
    }

    private User getOrCreateUser(String username) {
        User user = null;

        while (user == null) {
            try {
                final var found = userRepository.findFirstByName(username);
                if (found.isPresent()) {
                    user = found.get();
                } else {
                    User candidate = User.builder()
                        .name(username)
                        .build();
                    user = userRepository.save(candidate);
                    log.info(
                        "Thread {} found new user {}!",
                        Thread.currentThread().getName(),
                        username
                    );
                }
            } catch (RuntimeException e) {
                // Developer's Note: (J. Craig, 2021-05-23)
                // It is possible that two threads can try to create the same user at the same time.  This causes a
                // constraint violation.  The thread that receives the constraint violation will loop and fetch the
                // entry the other thread created.
            }
        }

        return user;
    }

    private boolean isAcceptableType(String type) {
        return "Ranked".equals(type) || "Free".equals(type);
    }

    private String getGameSetup(Node row) {
        String setup;

        try {
            setup = (String) gameSetup.evaluate(row, XPathConstants.STRING);
            setup = setup.strip();
        } catch (XPathExpressionException e) {
            log.error("An error occurred while trying to get the game's setup.", e);
            throw new IllegalStateException(e);
        }

        return setup;
    }

    private String getGameResult(Node row) {
        String result;

        try {
            result = (String) gameResult.evaluate(row, XPathConstants.STRING);
            result = result.strip();
        } catch (XPathExpressionException e) {
            log.error("An error occurred while trying to get the game's result.", e);
            throw new IllegalStateException(e);
        }

        return result;
    }

    private Optional<String> getGameUrl(Node row) {
        Optional<String> url = Optional.empty();

        try {
            final var found = (NodeList) gameUrl.evaluate(row, XPathConstants.NODESET);
            if (found.getLength() > 0) {
                final var address = found.item(0);
                final var attributes = address.getAttributes();
                final var href = attributes.getNamedItem("href");
                final var value = href.getNodeValue();
                url = Optional.of(value);
            }
        } catch (XPathExpressionException e) {
            log.error("An error occurred while trying to check whether a game has a download URL.", e);
            throw new IllegalStateException(e);
        }

        return url;
    }
}
