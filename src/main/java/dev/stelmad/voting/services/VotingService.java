package dev.stelmad.voting.services;

import dev.stelmad.voting.exceptions.VotingException;
import dev.stelmad.voting.models.Vote;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/// This `VotingService` provides vote processing capabilities.
///
/// @implSpec Implementations should ensure thread-safety if required by an underlying vote storage.
/// @see DefaultVotingServiceImpl
public interface VotingService {

    /// Returns a `Map` view of shareholder IDs and corresponding {@link Vote} objects.
    ///
    /// @return a map view of all votes processed votes.
    Map<String, Vote> retrieveAllVotes();

    /// Processes a vote for a shareholder meeting.
    ///
    /// @param vote              the vote object to process.
    /// @param shareholdersVoted a set of shareholder IDs who have already voted.
    /// @param recordDate        the record date of the meeting.
    /// @return `true` if the vote was accepted,
    ///         `false` otherwise
    /// @throws VotingException if the vote object can't be processed
    /// @implSpec - **New votes:** If the shareholder ID is not in `shareholdersVoted`,
    ///   the vote is always accepted and added to the internal storage.
    /// - **Vote changes:** If the shareholder ID already exists in `shareholdersVoted`,
    ///   the vote can only be changed if the current date is before the `recordDate`.
    ///   Changes on or after the record date are rejected.
    /// - **Invalid proposals:** If the proposal ID is invalid for the given meeting ID,
    ///   a `InvalidProposalException` is thrown.
    boolean process(Vote vote, Set<String> shareholdersVoted, LocalDate recordDate) throws VotingException;
}
