package dev.array21.jmac.util;

import java.nio.CharBuffer;

/**
 * Utilities to aid in using various Buffers
 * @author Tobias de Bruijn
 */
public class Buffers {

	/**
	 * Zero a CharBuffer. Every position in the buffer will be set to the NULL character.
	 * @param The buffer to zero
	 */
	public static void zero(CharBuffer b) {
		for(int i = 0; i < b.capacity(); i++) {
			b.position(i);
			b.put('\0');
		}
		
		b.position(0);
	}
	
	public static String stringify(CharBuffer b) {
		b.position(0);
		return b.toString();
	}
}
