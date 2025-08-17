package com.reliaquest.api.utils;

public class RegexUtil {
    // Regular expression to match alphanumeric characters and hyphens
    public static final String ALPHANUMERIC_HYPHEN_REGEX = "^[a-zA-Z0-9-]+$";

    // Regular expression to match alphanumeric characters, underscores, and hyphens
    public static final String ALPHANUMERIC_UNDERSCORE_HYPHEN_REGEX = "^[a-zA-Z0-9_-]+$";

    // Regular expression to match alphanumeric characters, underscores, hyphens, and periods
    public static final String ALPHANUMERIC_UNDERSCORE_HYPHEN_PERIOD_REGEX = "^[a-zA-Z0-9_.-]+$";

    // Regular expression to match letters and spaces only
    public static final String LETTERS_AND_SPACES_REGEX = "^[a-zA-Z\\s]+$";
}
