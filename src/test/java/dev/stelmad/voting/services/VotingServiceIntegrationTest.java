package dev.stelmad.voting.services;

import dev.stelmad.voting.exceptions.VotingException;
import dev.stelmad.voting.models.Vote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DisplayName("Voting Service - Multithreaded Environment Simulation")
class VotingServiceIntegrationTest {

    private static final int THREAD_COUNT = 1000;
    private static final int VOTES_PER_THREAD = 10;

    @Autowired
    private VotingService votingService;

    @Test
    void shouldProcessAllVotes() throws InterruptedException, VotingException {
        var totalExpectedVotes = THREAD_COUNT * VOTES_PER_THREAD;
        var countDownLatch = new CountDownLatch(THREAD_COUNT);
        var shareholdersVoted = new HashSet<String>();
        var recordDate = LocalDate.now().plusDays(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var tid = 0; tid < THREAD_COUNT; tid++) {
                final var threadId = tid;
                executor.submit(() -> {
                    try {
                        for (var voteId = 0; voteId < VOTES_PER_THREAD; voteId++) {
                            var shareholderId = "shareholder#" + threadId + voteId;
                            var vote = new Vote(shareholderId, "meeting#" + threadId, "proposal#" + voteId);
                            votingService.process(vote, shareholdersVoted, recordDate);
                        }
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }
        }

        assertTrue(countDownLatch.await(10, TimeUnit.SECONDS), "Test timed out waiting for threads to complete");
        assertEquals(totalExpectedVotes, votingService.retrieveAllVotes().size(), "Total number of successfully processed votes expected is invalid");
        assertEquals(totalExpectedVotes, shareholdersVoted.size(), "The set of shareholder IDs successfully votes is invalid");
    }
}
