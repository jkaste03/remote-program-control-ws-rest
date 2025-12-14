package com.github.jkaste03.exceptions;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * 
 */
public class InvalidTokenException extends BadCredentialsException {

    private static final long serialVersionUID = 1L;
    private final String messageKey;
    private final transient Object[] messageArgs;

    public InvalidTokenException(String messageKey, Throwable cause, Object... messageArgs) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    public InvalidTokenException(String messageKey, Object... messageArgs) {
        super(messageKey);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return messageArgs;
    }

}
