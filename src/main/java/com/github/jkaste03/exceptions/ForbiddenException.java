package com.github.jkaste03.exceptions;

/**
 * 
 */
public class ForbiddenException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE_KEY = "problem.forbidden.detail";

    private final String messageKey;
    private final transient Object[] messageArgs;

    public ForbiddenException() {
        this(DEFAULT_MESSAGE_KEY);
    }

    public ForbiddenException(String messageKey, Object... messageArgs) {
        super(messageKey);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs == null ? new Object[0] : messageArgs.clone();
    }

    public String messageKey() {
        return messageKey;
    }

    public Object[] messageArgs() {
        return messageArgs.clone();
    }

}
