package dev.array21.jmac.parser.components;

import java.util.Optional;

public record MacroMatcher(String ident, String fragSpec, Optional<Character> repSep, Optional<Character> repOp) {}