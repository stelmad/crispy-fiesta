package dev.stelmad.voting.services;

import dev.stelmad.voting.exceptions.InvalidProposalException;
import dev.stelmad.voting.models.Vote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Voting Service - Unit Tests")
class DefaultVotingServiceImplTest {

    private DefaultVotingServiceImpl votingService;
    private LocalDate fixedLocalDate;
    private Set<String> shareholdersVoted;

    @BeforeEach
    void setUp() {
        fixedLocalDate = LocalDate.of(2025, 12, 5);
        votingService = new DefaultVotingServiceImpl(
            Clock.fixed(
                fixedLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            )
        );
        shareholdersVoted = new HashSet<>();
    }

    @Test
    void shouldReturnEmptyVotes() {
        Map<String, Vote> result = votingService.retrieveAllVotes();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldAcceptAndReturnAllVotes() throws InvalidProposalException {
        var vote1 = new Vote("shareholder1", "meeting1", "proposal3");
        var vote2 = new Vote("shareholder2", "meeting2", "proposal2");
        var vote3 = new Vote("shareholder3", "meeting3", "proposal1");
        var recordDate = fixedLocalDate.plusDays(1);

        votingService.process(vote1, shareholdersVoted, recordDate);
        votingService.process(vote2, shareholdersVoted, recordDate);
        votingService.process(vote3, shareholdersVoted, recordDate);
        Map<String, Vote> result = votingService.retrieveAllVotes();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(vote1, result.get("shareholder1"));
        assertEquals(vote2, result.get("shareholder2"));
        assertEquals(vote3, result.get("shareholder3"));
    }

    @Test
    void shouldReturnUnmodifiableMap() throws InvalidProposalException {
        var vote1 = new Vote("shareholder1", "meeting1", "proposal3");
        var recordDate = fixedLocalDate.plusDays(1);

        votingService.process(vote1, shareholdersVoted, recordDate);
        Map<String, Vote> result = votingService.retrieveAllVotes();

        assertThrows(UnsupportedOperationException.class, () -> result.put("shareholder2", vote1));
    }

    @Test
    void shouldChangeVote() throws InvalidProposalException {
        var initialVote = new Vote("shareholder1", "meeting1", "proposal3");
        var updatedVote = new Vote("shareholder1", "meeting1", "proposal4");
        var recordDate = fixedLocalDate.plusDays(1);

        votingService.process(initialVote, shareholdersVoted, recordDate);
        var result = votingService.process(updatedVote, shareholdersVoted, recordDate);

        assertTrue(result);
        assertEquals(updatedVote, votingService.retrieveAllVotes().get("shareholder1"));
    }

    @Test
    void shouldRejectVoteChangesOnRecordDate() throws InvalidProposalException {
        var initialVote = new Vote("shareholder1", "meeting1", "proposal3");
        var updatedVote = new Vote("shareholder1", "meeting1", "proposal4");
        var recordDate = fixedLocalDate; // Same as the test current date

        votingService.process(initialVote, shareholdersVoted, recordDate);
        boolean result = votingService.process(updatedVote, shareholdersVoted, recordDate);

        assertFalse(result);
        assertEquals(initialVote, votingService.retrieveAllVotes().get("shareholder1"));
    }

    @Test
    void shouldRejectVoteChangesAfterRecordDate() throws InvalidProposalException {
        var initialVote = new Vote("shareholder1", "meeting1", "proposal3");
        var updatedVote = new Vote("shareholder1", "meeting1", "proposal4");
        var recordDate = fixedLocalDate.minusDays(1); // Before the test current date

        votingService.process(initialVote, shareholdersVoted, recordDate);
        var result = votingService.process(updatedVote, shareholdersVoted, recordDate);

        assertFalse(result);
        assertEquals(initialVote, votingService.retrieveAllVotes().get("shareholder1"));
    }

    @Test
    void shouldThrowInvalidProposalExceptions() {
        var m1p1Vote = new Vote("anyShareholderId", "meeting1", "proposal1");
        var m1p2Vote = new Vote("anyShareholderId", "meeting1", "proposal2");
        var m2p3Vote = new Vote("anyShareholderId", "meeting2", "proposal3");
        var m2p4Vote = new Vote("anyShareholderId", "meeting2", "proposal4");
        var m3p5Vote = new Vote("anyShareholderId", "meeting3", "proposal5");
        var m3p6Vote = new Vote("anyShareholderId", "meeting3", "proposal6");
        var recordDate = fixedLocalDate.plusDays(1);

        assertThrows(InvalidProposalException.class, () -> votingService.process(m1p1Vote, shareholdersVoted, recordDate));
        assertThrows(InvalidProposalException.class, () -> votingService.process(m1p2Vote, shareholdersVoted, recordDate));
        assertThrows(InvalidProposalException.class, () -> votingService.process(m2p3Vote, shareholdersVoted, recordDate));
        assertThrows(InvalidProposalException.class, () -> votingService.process(m2p4Vote, shareholdersVoted, recordDate));
        assertThrows(InvalidProposalException.class, () -> votingService.process(m3p5Vote, shareholdersVoted, recordDate));
        assertThrows(InvalidProposalException.class, () -> votingService.process(m3p6Vote, shareholdersVoted, recordDate));
    }

    @Test
    void shouldThrowNullPointerExceptions() {
        var vote = new Vote("shareholder1", "meeting1", "proposal3");
        var recordDate = fixedLocalDate.plusDays(1);

        assertThrows(NullPointerException.class, () -> votingService.process(null, shareholdersVoted, recordDate));
        assertThrows(NullPointerException.class, () -> votingService.process(vote, null, recordDate));
        assertThrows(NullPointerException.class, () -> votingService.process(vote, shareholdersVoted, null));
    }

    @Test
    void shouldMutateShareholdersSet() throws InvalidProposalException {
        var vote1 = new Vote("shareholder1", "meeting1", "proposal3");
        var vote2 = new Vote("shareholder2", "meeting2", "proposal5");
        var vote3 = new Vote("shareholder3", "meeting3", "proposal7");
        var recordDate = fixedLocalDate.plusDays(1);

        votingService.process(vote1, shareholdersVoted, recordDate);
        assertEquals(1, shareholdersVoted.size());
        assertTrue(shareholdersVoted.contains("shareholder1"));
        assertFalse(shareholdersVoted.contains("shareholder2"));
        assertFalse(shareholdersVoted.contains("shareholder3"));

        votingService.process(vote2, shareholdersVoted, recordDate);
        assertEquals(2, shareholdersVoted.size());
        assertTrue(shareholdersVoted.contains("shareholder1"));
        assertTrue(shareholdersVoted.contains("shareholder2"));
        assertFalse(shareholdersVoted.contains("shareholder3"));

        votingService.process(vote3, shareholdersVoted, recordDate);
        assertEquals(3, shareholdersVoted.size());
        assertTrue(shareholdersVoted.contains("shareholder1"));
        assertTrue(shareholdersVoted.contains("shareholder2"));
        assertTrue(shareholdersVoted.contains("shareholder3"));
    }
}