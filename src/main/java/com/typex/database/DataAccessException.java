package com.typex.database;

/**
 * Unchecked runtime exception indicating a database access or persistence failure.
 * Preserves the underlying SQLException and contextual error message.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
