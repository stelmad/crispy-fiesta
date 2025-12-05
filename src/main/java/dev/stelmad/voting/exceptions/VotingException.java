package dev.stelmad.voting.exceptions;

public class VotingException extends Exception {
    public VotingException() {
    }

    public VotingException(String message) {
        super(message);
    }

    public VotingException(String message, Throwable cause) {
        super(message, cause);
    }
}
