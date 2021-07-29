package dev.array21.jmac.parser.matcher;

import java.nio.CharBuffer;
import java.util.LinkedList;
import java.util.List;

import dev.array21.jmac.parser.components.MacroStructure;
import dev.array21.jmac.util.Buffers;
import dev.array21.jmac.util.Lists;

public class MatcherParser {
	
	public static void parseMatcher(MacroStructure struct) {
		TokenValue[] tokens = tokenize(struct.matcher());
		for(TokenValue tv : tokens) {
			System.out.println(String.format("TYPE %s VALUE %s", tv.type(), tv.value()));
		}
	}	
	
	private static TokenValue[] tokenize(String matcher) {
		List<TokenValue> tokenBuffer = new LinkedList<>();		
		char[] tokens = matcher.trim().toCharArray();
		
		CharBuffer charBuffer = CharBuffer.allocate(tokens.length);
		
		for(int i = 0; i < tokens.length; i++) {
			char c = tokens[i];
			if(c == ' ') {
				continue;
			}
			
			TokenValue previousToken = Lists.getLast(tokenBuffer);
			if(c == '$') {
				tokenBuffer.add(new TokenValue(TokenType.VARIABLE_PREFIX, "$"));
			}
			
			if(c == '(' || c == '{' || c == '[') {
				tokenBuffer.add(new TokenValue(TokenType.OPENING_DELIMITER, String.valueOf(c)));
			}
			
			if(c == ')' || c == '}' || c == ']') {
				tokenBuffer.add(new TokenValue(TokenType.CLOSING_DELIMITER, String.valueOf(c)));
			}
			
			if(c == ':') {
				tokenBuffer.add(new TokenValue(TokenType.IDENT_SEPERATOR, String.valueOf(c)));
			}
			
			if(c == '*' || c == '+' || c == '?') {
				tokenBuffer.add(new TokenValue(TokenType.MACRO_REP_OP, String.valueOf(c)));
			}
			
			if(c == ',') {
				tokenBuffer.add(new TokenValue(TokenType.COMMA, String.valueOf(c)));
			}
			
			System.out.println(c);
			if(previousToken != null) {
				
				if(previousToken.type() == TokenType.IDENT_SEPERATOR && charBuffer.position() != 0) {
					System.out.println("VAR IDENT");
					
					TokenValue identSepToken = Lists.pop(tokenBuffer);					
					String ident = Buffers.stringify(charBuffer);
					Buffers.zero(charBuffer);
					
					tokenBuffer.add(new TokenValue(TokenType.VARIABLE_IDENT, ident));
					tokenBuffer.add(identSepToken);
					
				} else if((previousToken.type() == TokenType.CLOSING_DELIMITER || previousToken.type() == TokenType.COMMA) && charBuffer.position() != 0) {
					System.out.println("VAR FRAG SPEC");
					String fragSpec = Buffers.stringify(charBuffer);
					Buffers.zero(charBuffer);

					TokenValue closingDelimOrComma = Lists.pop(tokenBuffer);
					tokenBuffer.add(new TokenValue(TokenType.VARIABLE_FRAG_SPEC, fragSpec));
					tokenBuffer.add(closingDelimOrComma);
				} else if(previousToken.type() == TokenType.CLOSING_DELIMITER && c != '*' && c != '+' && c != '?' && c != '(' && c != '{' && c != '[' && c != '(' && c != '{' && c != '[' ) {
					System.out.println("MACRO REP SEP");
					tokenBuffer.add(new TokenValue(TokenType.MACRO_REP_SEP, String.valueOf(c)));
				} else if(previousToken.type() == TokenType.VARIABLE_PREFIX || previousToken.type() == TokenType.IDENT_SEPERATOR) {
					charBuffer.append(c);
					System.out.println("CHARBUFFER APPEND");
				}
			}
			
			if(i == tokens.length -1) {
				System.out.println("EOF");
				
				charBuffer.append(c);
				String fragSpec = Buffers.stringify(charBuffer);
				Buffers.zero(charBuffer);
				tokenBuffer.add(new TokenValue(TokenType.VARIABLE_FRAG_SPEC, fragSpec));
			}
		}
		
		return tokenBuffer.toArray(new TokenValue[0]);
	}
}