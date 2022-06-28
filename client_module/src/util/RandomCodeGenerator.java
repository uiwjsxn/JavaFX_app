package util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RandomCodeGenerator{
	private char[] chars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l','m', 'n', 'o', 'p', 'q', 'r', 's', 
							't', 'u', 'v', 'w', 'x', 'y','z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L','M', 'N', 'O', 
							'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y','Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
							'!', '@', '#', '$', '%', '^', '*'};
	private SecureRandom randomGen = null;
	private static RandomCodeGenerator generator = null;
	
	public static RandomCodeGenerator getInstance() {
		if(generator == null) {
			generator = new RandomCodeGenerator();
		}
		return generator;
	}
	
	private RandomCodeGenerator(){
		try{
			randomGen = SecureRandom.getInstanceStrong();
		}catch(NoSuchAlgorithmException e){
			randomGen = new SecureRandom();
		}
	}
	private char genRandomChar(){
		int  num = randomGen.nextInt();
		num = (num < 0 ? -num : num);
		return chars[num%chars.length];
	}
/******************************************************public methods******************************************************/	
	public char[] genRandomCode(int codeLength){
		char[] codes = new char[codeLength];
		for(int i = 0;i < codeLength;++i){
			codes[i] = genRandomChar();
		}
		return codes;
	}
}
