package dev.array21.jmac.exception;

public class MacroSyntaxException extends RuntimeException {
	private static final long serialVersionUID = -2366751838481557021L;
		
	public MacroSyntaxException(String message) {
		super(message);
	}
	
	public MacroSyntaxException(String pat, Object... objects) {
		super(String.format(pat, objects));
	}
}
