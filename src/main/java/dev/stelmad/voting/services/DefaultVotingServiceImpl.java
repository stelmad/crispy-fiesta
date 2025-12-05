package dev.stelmad.voting.services;

import dev.stelmad.voting.exceptions.InvalidProposalException;
import dev.stelmad.voting.models.Vote;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/// Default in-memory implementation of {@link VotingService}.
/// The service maintains a {@link HashMap} as the internal storage.
/// It's a thread-safe implementation.
///
/// @apiNote Invalid proposals are validated against the predefined map of proposals per meeting ID.
/// @see VotingService
@Service
@NullMarked
public class DefaultVotingServiceImpl implements VotingService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultVotingServiceImpl.class);

    private final static Map<String, Set<String>> INVALID_PROPOSALS = Map.ofEntries(
        Map.entry("meeting1", Set.of("proposal1", "proposal2")),
        Map.entry("meeting2", Set.of("proposal3", "proposal4")),
        Map.entry("meeting3", Set.of("proposal5", "proposal6"))
    );

    private final Clock clock;
    private final ReadWriteLock rwl;
    private final Map<String, Vote> shareholderVoteMap;

    /// Constructs a new instance using the system default time zone clock.
    ///
    /// It's primarily used by Spring Framework for dependency injection.
    public DefaultVotingServiceImpl() {
        this(Clock.systemDefaultZone());
        logger.debug("DefaultVotingServiceImpl initialized with system default time zone clock");
    }

    /// Constructs a new instance with the specified clock.
    ///
    /// This constructor is primarily intended for unit testing, allowing injection of a controlled clock.
    ///
    /// @param clock the clock to use for determining the current date.
    /// @throws NullPointerException if the clock is null
    public DefaultVotingServiceImpl(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        this.rwl = new ReentrantReadWriteLock();
        this.shareholderVoteMap = new HashMap<>();
        logger.debug("DefaultVotingServiceImpl initialized with custom clock");
    }

    /// {@inheritDoc}
    ///
    /// The `Map` returned represents an unmodifiable copy of the underlying map.
    /// The {@link Vote} objects being stored as values are immutable to avoid external mutation.
    ///
    /// @implNote This implementation uses the {@link ReentrantReadWriteLock} to ensure thread-safe access to the underlying map.
    @Override
    public Map<String, Vote> retrieveAllVotes() {
        logger.debug("Retrieving all votes");
        rwl.readLock().lock();
        logger.trace("ReadLock is acquired inside 'retrieveAllVotes()'");
        try {
            var unmodifiableMapCopy = Map.copyOf(shareholderVoteMap);
            logger.info("Retrieved {} votes", unmodifiableMapCopy.size());
            return unmodifiableMapCopy;
        } finally {
            rwl.readLock().unlock();
            logger.trace("ReadLock is released inside 'retrieveAllVotes()'");
        }
    }

    /// {@inheritDoc}
    ///
    /// @throws InvalidProposalException if the proposal ID is not valid for the given meeting ID
    /// @throws NullPointerException     if any of the parameters is null
    /// @apiNote The implementation always accepts new votes and adds them to the underlying map. Whenever a new vote is accepted, the corresponding shareholder ID is added to the `shareholdersVoted` set as well.
    /// @implNote - This implementation uses a double-check locking pattern.
    /// The validation check is performed twice: once under a read lock for early rejection, and again under a write lock to handle potential race conditions.
    /// - Invalid proposals are validated against an internal predefined map of invalid proposals
    /// per meeting ID.
    @Override
    public boolean process(Vote vote, Set<String> shareholdersVoted, LocalDate recordDate) throws InvalidProposalException {
        logger.debug("Processing vote for shareholder: {}, meeting: {}, proposal: {}",
            vote.shareholderID(), vote.meetingID(), vote.proposalID());

        Objects.requireNonNull(vote);
        Objects.requireNonNull(shareholdersVoted);
        Objects.requireNonNull(recordDate);

        if (!isValidProposal(vote)) {
            logger.warn("Invalid proposal detected: {}", vote);
            throw new InvalidProposalException(vote.meetingID(), vote.proposalID());
        }

        final var currentDate = LocalDate.now(clock);
        logger.debug("Current date: {}, record date: {}", currentDate, recordDate);

        rwl.readLock().lock();
        logger.trace("ReadLock is acquired inside 'process()'");
        try {
            if (isChangeForbidden(vote, shareholdersVoted, recordDate, currentDate)) {
                return false;
            }
        } finally {
            rwl.readLock().unlock();
            logger.trace("ReadLock is released inside 'process()'");
        }

        rwl.writeLock().lock();
        logger.trace("WriteLock is acquired inside 'process()'");
        try {
            if (isChangeForbidden(vote, shareholdersVoted, recordDate, currentDate)) {
                // reject forbidden changes again because the state may have changed since the read phase
                return false;
            } else if (!shareholdersVoted.contains(vote.shareholderID())) {
                // add new votes
                shareholdersVoted.add(vote.shareholderID());
                shareholderVoteMap.put(vote.shareholderID(), vote);
                logger.debug("New vote: {}", vote);
            } else {
                // change existing votes
                shareholderVoteMap.replace(vote.shareholderID(), vote);
                logger.info("Updated vote: {}", vote);
            }
        } finally {
            rwl.writeLock().unlock();
            logger.trace("WriteLock is released inside 'process()'");
        }
        logger.info("Vote accepted: {}", vote);
        return true;
    }

    private boolean isChangeForbidden(Vote vote, Set<String> existingVoters, LocalDate recordDate, LocalDate currentDate) {
        var isForbidden = existingVoters.contains(vote.shareholderID()) && !currentDate.isBefore(recordDate);
        if (isForbidden) {
            logger.warn("Vote change rejected for shareholder - {}, currentDate - {}, recordDate - {}",
                vote.shareholderID(), currentDate, recordDate);
        }
        return isForbidden;
    }

    private boolean isValidProposal(Vote vote) {
        return Optional.ofNullable(INVALID_PROPOSALS.get(vote.meetingID()))
            .map(s -> !s.contains(vote.proposalID()))
            .orElse(true);
    }
}
