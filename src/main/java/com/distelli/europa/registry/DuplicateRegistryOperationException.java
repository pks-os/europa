package com.distelli.europa.registry;

public class DuplicateRegistryOperationException extends RuntimeException {
    public DuplicateRegistryOperationException(String op) {
        super(formatMessage(op));
    }

    public DuplicateRegistryOperationException(String op, String info) {
        super(formatMessage(op, info));
    }

    public DuplicateRegistryOperationException(String op, Throwable throwable) {
        super(formatMessage(op), throwable);
    }

    public DuplicateRegistryOperationException(String op, String info, Throwable throwable) {
        super(formatMessage(op, info), throwable);
    }

    private static String formatMessage(String op) {
        return String.format("Cannot perform %s operation again", op);
    }

    private static String formatMessage(String op, String info) {
        return String.format("%s for data %s", formatMessage(op), info);
    }
}
