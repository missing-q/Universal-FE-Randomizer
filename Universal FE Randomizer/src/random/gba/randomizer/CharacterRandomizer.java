package random.gba.randomizer;

import java.util.Random;

import fedata.gba.GBAFECharacterData;
import fedata.gba.GBAFEClassData;
import random.gba.loader.CharacterDataLoader;
import random.gba.loader.ClassDataLoader;

public class CharacterRandomizer {
	
	public static int rngSalt = 9002;
	
	public static void randomizeAffinity(CharacterDataLoader charactersData, Random rng) {
		GBAFECharacterData[] playableCharacters = charactersData.playableCharacters();
		int[] values = charactersData.validAffinityValues();
		for (GBAFECharacterData character : playableCharacters) {
			int affinity = values[rng.nextInt(values.length)];
			character.setAffinityValue(affinity);
		}
	}
	
	public static void randomizeConstitution(int minCON, int variance, CharacterDataLoader characterData, ClassDataLoader classData, Random rng) {
		GBAFECharacterData[] allPlayableCharacters = characterData.playableCharacters();
		for (GBAFECharacterData character : allPlayableCharacters) {
			GBAFEClassData currentClass = classData.classForID(character.getClassID());
			int classCON = currentClass.getCON();
			int personalCON = character.getConstitution();
			int totalCON = classCON + personalCON;
			
			int newCON = totalCON;
			
			int direction = rng.nextInt(2);
			if (direction == 0) {
				newCON += rng.nextInt(variance);
			} else {
				newCON -= rng.nextInt(variance);
			}
			
			newCON = Math.max(minCON, newCON);
			
			int newPersonalCON = newCON - classCON;
			
			character.setConstitution(newPersonalCON);
		}
	}
}
