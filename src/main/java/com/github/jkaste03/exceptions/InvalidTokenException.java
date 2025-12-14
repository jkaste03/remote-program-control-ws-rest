package com.github.jkaste03.exceptions;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * 
 */
public class InvalidTokenException extends BadCredentialsException {

    private static final long serialVersionUID = 1L;
    private final String messageKey;

    public InvalidTokenException(String messageKey, Throwable cause) {
        super(messageKey, cause); // Sender nøkkelen som melding til super
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }

}
