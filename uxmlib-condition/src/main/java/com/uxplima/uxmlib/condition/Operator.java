package com.uxplima.uxmlib.condition;

import java.util.List;
import java.util.Objects;

/**
 * The comparison operators a {@link Comparison} understands. The symbols are deliberately the familiar ones
 * from config files. Ordering of {@link #values()} matters for parsing: the two-character operators are
 * declared before their one-character prefixes so a longest-match scan finds {@code >=} before {@code >}.
 */
public enum Operator {

    /** Equal: numeric equality when both sides are numbers, otherwise case-sensitive string equality. */
    EQUAL("=="),

    /** Not equal: the negation of {@link #EQUAL}. */
    NOT_EQUAL("!="),

    /** Greater-than-or-equal. Numeric only; a non-numeric operand makes it false. */
    GREATER_OR_EQUAL(">="),

    /** Less-than-or-equal. Numeric only; a non-numeric operand makes it false. */
    LESS_OR_EQUAL("<="),

    /** Strictly greater-than. Numeric only; a non-numeric operand makes it false. */
    GREATER(">"),

    /** Strictly less-than. Numeric only; a non-numeric operand makes it false. */
    LESS("<");

    private static final List<Operator> BY_SYMBOL_LENGTH =
            List.of(EQUAL, NOT_EQUAL, GREATER_OR_EQUAL, LESS_OR_EQUAL, GREATER, LESS);

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    /** The literal operator symbol as it appears in a condition string. */
    public String symbol() {
        return symbol;
    }

    /**
     * Whether this operator orders its operands numerically. Only {@link #EQUAL}/{@link #NOT_EQUAL} fall back
     * to string comparison; the four ordering operators are numeric-only.
     */
    public boolean isOrdering() {
        return this != EQUAL && this != NOT_EQUAL;
    }

    /**
     * The operators in the order a longest-symbol-first parser should try them: two-character operators
     * before the one-character ones that prefix them.
     */
    static List<Operator> bySymbolLengthDescending() {
        return BY_SYMBOL_LENGTH;
    }

    /** Resolve an operator from its exact symbol, or throw if it is not one we recognise. */
    public static Operator fromSymbol(String symbol) {
        Objects.requireNonNull(symbol, "symbol");
        for (Operator operator : values()) {
            if (operator.symbol.equals(symbol)) {
                return operator;
            }
        }
        throw new IllegalArgumentException("unknown operator symbol: " + symbol);
    }
}
