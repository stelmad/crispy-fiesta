package dev.stelmad.voting.exceptions;

import lombok.Getter;

import java.util.Objects;

@Getter
public class InvalidProposalException extends VotingException {
    private final String meetingID;
    private final String proposalID;

    public InvalidProposalException(String meetingID, String proposalID) {
        super("The proposalID is not valid for the given meetingID.");
        this.meetingID = Objects.requireNonNull(meetingID);
        this.proposalID = Objects.requireNonNull(proposalID);
    }

}
