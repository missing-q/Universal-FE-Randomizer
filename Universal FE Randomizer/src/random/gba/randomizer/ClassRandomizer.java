package random.gba.randomizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import fedata.gba.GBAFEChapterData;
import fedata.gba.GBAFEChapterItemData;
import fedata.gba.GBAFEChapterUnitData;
import fedata.gba.GBAFECharacterData;
import fedata.gba.GBAFEClassData;
import fedata.gba.GBAFEItemData;
import fedata.gba.general.WeaponRank;
import fedata.gba.general.WeaponType;
import fedata.general.FEBase.GameType;
import random.gba.loader.ChapterLoader;
import random.gba.loader.CharacterDataLoader;
import random.gba.loader.ClassDataLoader;
import random.gba.loader.ItemDataLoader;
import random.gba.loader.TextLoader;
import random.general.PoolDistributor;
import random.general.RelativeValueMapper;
import ui.model.ClassOptions;
import ui.model.ItemAssignmentOptions;
import ui.model.ItemAssignmentOptions.WeaponReplacementPolicy;
import util.DebugPrinter;

public class ClassRandomizer {
	
	static final int rngSalt = 874;
	
	public static void randomizeClassMovement(int minMOV, int maxMOV, ClassDataLoader classData, Random rng) {
		GBAFEClassData[] allClasses = classData.allClasses();
		for (GBAFEClassData currentClass : allClasses) {
			if (currentClass.getMOV() > 0) {
				int randomMOV = rng.nextInt(maxMOV - minMOV) + minMOV;
				currentClass.setMOV(randomMOV);
			}
		}
	}
	
	public static void randomizePlayableCharacterClasses(ClassOptions options, ItemAssignmentOptions inventoryOptions, GameType type, CharacterDataLoader charactersData, ClassDataLoader classData, ChapterLoader chapterData, ItemDataLoader itemData, TextLoader textData, Random rng) {
		GBAFECharacterData[] allPlayableCharacters = charactersData.playableCharacters();
		Map<Integer, GBAFEClassData> determinedClasses = new HashMap<Integer, GBAFEClassData>();
		
		Boolean includeLords = options.includeLords;
		Boolean includeThieves = options.includeThieves;
		Boolean includeSpecial = options.includeSpecial;
		Boolean hasMonsters = false;
		Boolean separateMonsters = false;
		
		Boolean forceChange = options.forceChange;
		
		if (type == GameType.FE8) {
			hasMonsters = true;
			separateMonsters = options.separateMonsters;
		}
		
		PoolDistributor<GBAFEClassData> classDistributor = new PoolDistributor<GBAFEClassData>();
		Arrays.asList(classData.allClasses()).stream().forEach(charClass -> {
			classDistributor.addItem(charClass);
		});
		
		for (GBAFECharacterData character : allPlayableCharacters) {
			
			Boolean isLordCharacter = charactersData.isLordCharacterID(character.getID());
			Boolean isThiefCharacter = charactersData.isThiefCharacterID(character.getID());
			Boolean isSpecialCharacter = charactersData.isSpecialCharacterID(character.getID());
			Boolean canChange = charactersData.canChangeCharacterID(character.getID());
			
			if (isLordCharacter && !includeLords) { continue; }
			if (isThiefCharacter && !includeThieves) { continue; }
			if (isSpecialCharacter && !includeSpecial) { continue; }
			if (!canChange) { continue; }
			
			Boolean characterRequiresRange = charactersData.characterIDRequiresRange(character.getID());
			Boolean characterRequiresMelee = charactersData.characterIDRequiresMelee(character.getID());
			
			int originalClassID = character.getClassID();
			GBAFEClassData originalClass = classData.classForID(originalClassID);
			
			GBAFEClassData targetClass = null;
			
			if (determinedClasses.containsKey(character.getID())) {
				continue;
			} else {
				GBAFEClassData[] possibleClasses = hasMonsters ? classData.potentialClasses(originalClass, !includeLords, !includeThieves, !includeSpecial, separateMonsters, forceChange, isLordCharacter, characterRequiresRange, characterRequiresMelee, character.isClassRestricted(), null) :
					classData.potentialClasses(originalClass, !includeLords, !includeThieves, !includeSpecial, forceChange, isLordCharacter, characterRequiresRange, characterRequiresMelee, character.isClassRestricted(), null);
				if (possibleClasses.length == 0) {
					continue;
				}
				
				if (options.assignEvenly) {
					Set<GBAFEClassData> classSet = new HashSet<GBAFEClassData>(Arrays.asList(possibleClasses));
					if (Collections.disjoint(classDistributor.possibleResults(), classSet)) {
						Arrays.asList(classData.allClasses()).stream().forEach(charClass -> {
							classDistributor.addItem(charClass);
						});
					}
					classSet.retainAll(classDistributor.possibleResults());
					List<GBAFEClassData> classList = classSet.stream().sorted(GBAFEClassData.defaultComparator).collect(Collectors.toList());
					PoolDistributor<GBAFEClassData> pool = new PoolDistributor<GBAFEClassData>();
					for (GBAFEClassData charClass : classList) {
						pool.addItem(charClass, classDistributor.itemCount(charClass));
					}
					targetClass = pool.getRandomItem(rng, true);
					classDistributor.removeItem(targetClass, false);
				} else {
					int randomIndex = rng.nextInt(possibleClasses.length);
					targetClass = possibleClasses[randomIndex];
				}
			}
			
			if (targetClass == null) {
				continue;
			}
			
			DebugPrinter.log(DebugPrinter.Key.CLASS_RANDOMIZER, "Assigning character 0x" + Integer.toHexString(character.getID()).toUpperCase() + " (" + textData.getStringAtIndex(character.getNameIndex(), true) + ") to class 0x" + Integer.toHexString(targetClass.getID()) + " (" + textData.getStringAtIndex(targetClass.getNameIndex(), true) + ")");
			
			for (GBAFECharacterData linked : charactersData.linkedCharactersForCharacter(character)) {
				determinedClasses.put(linked.getID(), targetClass);
				updateCharacterToClass(options, inventoryOptions, linked, originalClass, targetClass, characterRequiresRange, characterRequiresMelee, classData, chapterData, itemData, textData, false, rng);
				linked.setIsLord(isLordCharacter);
			}
		}
	}
	
	public static void randomizeBossCharacterClasses(ClassOptions options, ItemAssignmentOptions inventoryOptions, GameType type, CharacterDataLoader charactersData, ClassDataLoader classData, ChapterLoader chapterData, ItemDataLoader itemData, TextLoader textData, Random rng) {
		GBAFECharacterData[] allBossCharacters = charactersData.bossCharacters();
		
		Boolean includeLords = false;
		Boolean includeThieves = false;
		Boolean includeSpecial = false;
		Boolean hasMonsters = false;
		Boolean separateMonsters = false;
		Boolean forceChange = options.forceChange;
		if (type == GameType.FE8) {
			hasMonsters = true;
			separateMonsters = options.separateMonsters;
		}
		
		Map<Integer, GBAFEClassData> determinedClasses = new HashMap<Integer, GBAFEClassData>();
		
		for (GBAFECharacterData character : allBossCharacters) {
			
			Boolean canChange = charactersData.canChangeCharacterID(character.getID());
			if (!canChange) { continue; }
			
			Boolean characterRequiresRange = charactersData.characterIDRequiresRange(character.getID());
			Boolean characterRequiresMelee = charactersData.characterIDRequiresMelee(character.getID());
			
			int originalClassID = character.getClassID();
			GBAFEClassData originalClass = classData.classForID(originalClassID);
			if (originalClass == null) {
				System.err.println("Invalid Class found: Class ID = " + Integer.toHexString(originalClassID));
				continue;
			}
			
			GBAFEClassData targetClass = null;
			
			Boolean forceBasicWeaponry = false;
			Boolean shouldNerf = false;
			
			if (determinedClasses.containsKey(character.getID())) {
				continue;
			} else {			
				GBAFECharacterData mustLoseToCharacter = charactersData.characterRequiresCounterToCharacter(character);
				GBAFEClassData mustLoseToClass = null;
				if (mustLoseToCharacter != null) {
					mustLoseToClass = classData.classForID(mustLoseToCharacter.getClassID());
					forceBasicWeaponry = true;
					shouldNerf = true;
				}
				
				GBAFEClassData[] possibleClasses = hasMonsters ? 
						classData.potentialClasses(originalClass, !includeLords, !includeThieves, !includeSpecial, separateMonsters, forceChange, true, characterRequiresRange, characterRequiresMelee, character.isClassRestricted(), mustLoseToClass) :
					classData.potentialClasses(originalClass, !includeLords, !includeThieves, !includeSpecial, forceChange, true, characterRequiresRange, characterRequiresMelee, character.isClassRestricted(), mustLoseToClass);
				if (possibleClasses.length == 0) {
					continue;
				}
			
				int randomIndex = rng.nextInt(possibleClasses.length);
				targetClass = possibleClasses[randomIndex];
			}
			
			if (targetClass == null) {
				continue;
			}
			
			for (GBAFECharacterData linked : charactersData.linkedCharactersForCharacter(character)) {
				determinedClasses.put(linked.getID(), targetClass);
				updateCharacterToClass(options, inventoryOptions, linked, originalClass, targetClass, characterRequiresRange, characterRequiresMelee, classData, chapterData, itemData, textData, forceBasicWeaponry && linked.getID() == character.getID(), rng);
				if (shouldNerf) { // Halve skill, speed, defense, and resistance if we need to make sure he loses to us.
					linked.setBaseSKL(linked.getBaseSKL() >> 1);
					linked.setBaseSPD(linked.getBaseSPD() >> 1);
					linked.setBaseDEF(linked.getBaseDEF() >> 1);
					linked.setBaseRES(linked.getBaseRES() >> 1);
				}
			}
		}
	}
	
	public static void randomizeMinionClasses(ClassOptions options, ItemAssignmentOptions inventoryOptions, GameType type, CharacterDataLoader charactersData, ClassDataLoader classData, ChapterLoader chapterData, ItemDataLoader itemData, Random rng) {
		Boolean includeLords = false;
		Boolean includeThieves = false;
		Boolean includeSpecial = false;
		Boolean hasMonsters = false;
		Boolean separateMonsters = false;
		Boolean forceChange = options.forceChange;
		if (type == GameType.FE8) {
			hasMonsters = true;
			separateMonsters = options.separateMonsters;
		}
		
		for (GBAFEChapterData chapter : chapterData.allChapters()) {
			GBAFECharacterData lordCharacter = charactersData.characterWithID(chapter.lordLeaderID());
			GBAFEClassData lordClass = classData.classForID(lordCharacter.getClassID());
			for (GBAFEChapterUnitData chapterUnit : chapter.allUnits()) {
				// int leaderID = chapterUnit.getLeaderID();
				int characterID = chapterUnit.getCharacterNumber();
				int classID = chapterUnit.getStartingClass();
				// It's safe to check for boss leader ID in the case of FE7, but FE6 tends to put other IDs there (kind of like squad captains).
				// We're going to remove this safety check in the meantime, but we should be wary of any accidental changes.
				// Also check to make sure it's not any character we definitely don't want to change.
				// Finally, also make sure the starting class is valid. Classes we don't recognize, we shouldn't touch.
				if (!charactersData.isBossCharacterID(characterID) && /*charactersData.isBossCharacterID(leaderID) &&*/ !charactersData.isPlayableCharacterID(characterID) && 
						charactersData.canChangeCharacterID(characterID) && classData.isValidClass(classID)) {
					GBAFEClassData originalClass = classData.classForID(classID);
					if (originalClass == null) {
						continue;
					}
					
					if (classData.isThief(originalClass.getID())) {
						continue;
					}
					
					Boolean shouldRestrictToSafeClasses = !chapter.isClassSafe();
					Boolean shouldMakeEasy = chapter.shouldBeSimplified();
					GBAFEClassData loseToClass = shouldMakeEasy ? lordClass : null;
					GBAFEClassData[] possibleClasses = hasMonsters ? 
							classData.potentialClasses(originalClass, !includeLords, !includeThieves, !includeSpecial, separateMonsters, forceChange, true, false, false, shouldRestrictToSafeClasses, loseToClass) :
						classData.potentialClasses(originalClass, false, false, false, forceChange, true, false, false, shouldRestrictToSafeClasses, loseToClass);
					if (possibleClasses.length == 0) {
						continue;
					}
					
					int randomIndex = rng.nextInt(possibleClasses.length);
					GBAFEClassData targetClass = possibleClasses[randomIndex];
					
					if (classData.isFlying(originalClass.getID()) == false && classData.isFlying(targetClass.getID())) {
						// If this is a new flier, roll one more time. 
						// Reduce the number of non-flying minions that become fliers.
						randomIndex = rng.nextInt(possibleClasses.length);
						targetClass = possibleClasses[randomIndex];
					}
					
					updateMinionToClass(inventoryOptions, chapterUnit, targetClass, classData, itemData, rng);
				}
			}
		}
	}

	private static void updateCharacterToClass(ClassOptions classOptions, ItemAssignmentOptions inventoryOptions, GBAFECharacterData character, GBAFEClassData sourceClass, GBAFEClassData targetClass, Boolean ranged, Boolean melee, ClassDataLoader classData, ChapterLoader chapterData, ItemDataLoader itemData, TextLoader textData, Boolean forceBasicWeapons, Random rng) {
		
		character.prepareForClassRandomization();
		character.setClassID(targetClass.getID());
		transferWeaponLevels(character, sourceClass, targetClass, rng);
		switch (classOptions.basesTransfer) {
		case ADJUST_TO_MATCH:
			applyBaseCorrectionForCharacter(character, sourceClass, targetClass);
			break;
		case NO_CHANGE:
			break;
		case ADJUST_TO_CLASS:
			adjustBasesToMatchClass(character, sourceClass, targetClass);
			break;
		}
		
		
		for (GBAFEChapterData chapter : chapterData.allChapters()) {
			GBAFEChapterItemData reward = chapter.chapterItemGivenToCharacter(character.getID());
			if (reward != null) {
				GBAFEItemData item = itemData.getRandomWeaponForCharacter(character, ranged, melee, rng);
				reward.setItemID(item.getID());
			}
			
			for (GBAFEChapterUnitData chapterUnit : chapter.allUnits()) {
				if (chapterUnit.getCharacterNumber() == character.getID()) {
					if (chapterUnit.getStartingClass() != sourceClass.getID()) {
						System.err.println("Class mismatch for character with ID " + character.getID() + ". Expected Class " + sourceClass.getID() + " but found " + chapterUnit.getStartingClass());
					}
					chapterUnit.setStartingClass(targetClass.getID());
					validateCharacterInventory(inventoryOptions, character, targetClass, chapterUnit, ranged, melee, classData, itemData, textData, forceBasicWeapons, rng);
					if (classData.isThief(sourceClass.getID())) {
						validateFormerThiefInventory(chapterUnit, itemData);
					}
					validateSpecialClassInventory(chapterUnit, itemData, rng);
				}
			}
		}
	}
	
	private static void applyBaseCorrectionForCharacter(GBAFECharacterData character, GBAFEClassData sourceClass, GBAFEClassData targetClass) {
		int hpDelta = sourceClass.getBaseHP() - targetClass.getBaseHP();
		character.setBaseHP(character.getBaseHP() + hpDelta);
		int strDelta = sourceClass.getBaseSTR() - targetClass.getBaseSTR();
		character.setBaseSTR(character.getBaseSTR() + strDelta);
		int sklDelta = sourceClass.getBaseSKL() - targetClass.getBaseSKL();
		character.setBaseSKL(character.getBaseSKL() + sklDelta);
		int spdDelta = sourceClass.getBaseSPD() - targetClass.getBaseSPD();
		character.setBaseSPD(character.getBaseSPD() + spdDelta);
		int defDelta = sourceClass.getBaseDEF() - targetClass.getBaseDEF();
		character.setBaseDEF(character.getBaseDEF() + defDelta);
		int resDelta = sourceClass.getBaseRES() - targetClass.getBaseRES();
		character.setBaseRES(character.getBaseRES() + resDelta);
		int lckDelta = sourceClass.getBaseLCK() - targetClass.getBaseLCK();
		character.setBaseLCK(character.getBaseLCK() + lckDelta);
		
		// Only correct CON if it ends up being an invalid (i.e. negative) CON.
		// This is only really possible if the character had a negative CON adjustment to begin with.
		if (character.getConstitution() < 0 && Math.abs(character.getConstitution()) > targetClass.getCON()) {
			character.setConstitution(-1 * targetClass.getCON());
		}
	}
	
	private static void adjustBasesToMatchClass(GBAFECharacterData character, GBAFEClassData sourceClass, GBAFEClassData targetClass) {
		// HP transfers directly, as does LCK.
		int hpDelta = sourceClass.getBaseHP() - targetClass.getBaseHP();
		character.setBaseHP(character.getBaseHP() + hpDelta);
		int lckDelta = sourceClass.getBaseLCK() - targetClass.getBaseLCK();
		character.setBaseLCK(character.getBaseLCK() + lckDelta);
		
		// STR, SKL, SPD, DEF, and RES are transfered based on which one is highest on the target class.
		int effectiveSTR = character.getBaseSTR() + sourceClass.getBaseSTR();
		int effectiveSKL = character.getBaseSKL() + sourceClass.getBaseSKL();
		int effectiveSPD = character.getBaseSPD() + sourceClass.getBaseSPD();
		int effectiveDEF = character.getBaseDEF() + sourceClass.getBaseDEF();
		int effectiveRES = character.getBaseRES() + sourceClass.getBaseRES();
		
		List<Integer> mappedStats = RelativeValueMapper.mappedValues(Arrays.asList(effectiveSTR, effectiveSKL, effectiveSPD, effectiveDEF, effectiveRES),
				Arrays.asList(targetClass.getBaseSTR(), targetClass.getBaseSKL(), targetClass.getBaseSPD(), targetClass.getBaseDEF(), targetClass.getBaseRES()));
		
		character.setBaseSTR(mappedStats.get(0) - targetClass.getBaseSTR());
		character.setBaseSKL(mappedStats.get(1) - targetClass.getBaseSKL());
		character.setBaseSPD(mappedStats.get(2) - targetClass.getBaseSPD());
		character.setBaseDEF(mappedStats.get(3) - targetClass.getBaseDEF());
		character.setBaseRES(mappedStats.get(4) - targetClass.getBaseRES());
		
	}
	
	// TODO: Offer an option for sidegrade strictness?
	private static void updateMinionToClass(ItemAssignmentOptions inventoryOptions, GBAFEChapterUnitData chapterUnit, GBAFEClassData targetClass, ClassDataLoader classData, ItemDataLoader itemData, Random rng) {
		DebugPrinter.log(DebugPrinter.Key.CLASS_RANDOMIZER, "Updating minion from class 0x" + Integer.toHexString(chapterUnit.getStartingClass()) + " to class 0x" + Integer.toHexString(targetClass.getID()));
		DebugPrinter.log(DebugPrinter.Key.CLASS_RANDOMIZER, "Starting Inventory: [0x" + Integer.toHexString(chapterUnit.getItem1()) + ", 0x" + Integer.toHexString(chapterUnit.getItem2()) + ", 0x" + Integer.toHexString(chapterUnit.getItem3()) + ", 0x" + Integer.toHexString(chapterUnit.getItem4()) + "]");
		chapterUnit.setStartingClass(targetClass.getID());
		validateMinionInventory(inventoryOptions, chapterUnit, classData, itemData, rng);
		DebugPrinter.log(DebugPrinter.Key.CLASS_RANDOMIZER, "Minion update complete. Inventory: [0x" + Integer.toHexString(chapterUnit.getItem1()) + ", 0x" + Integer.toHexString(chapterUnit.getItem2()) + ", 0x" + Integer.toHexString(chapterUnit.getItem3()) + ", 0x" + Integer.toHexString(chapterUnit.getItem4()) + "]");
	}
	
	public static void validateFormerThiefInventory(GBAFEChapterUnitData chapterUnit, ItemDataLoader itemData) {
		Set<GBAFEItemData> itemsToRetain = itemsToRetain(chapterUnit, itemData);
		
		GBAFEItemData[] requiredItems = itemData.formerThiefInventory();
		if (requiredItems != null) {
			giveItemsToChapterUnit(chapterUnit, requiredItems);
		}
		
		GBAFEItemData[] thiefItemsToRemove = itemData.thiefItemsToRemove();
		for (GBAFEItemData item : thiefItemsToRemove) {
			chapterUnit.removeItem(item.getID());
		}
		
		itemsToGiveBack(chapterUnit, itemsToRetain, itemData);
		if (!itemsToRetain.isEmpty()) {
			int[] idsToGiveBack = itemsToRetain.stream().mapToInt(item -> (item.getID())).toArray();
			chapterUnit.giveItems(idsToGiveBack);
		}
	}
	
	private static Set<GBAFEItemData> itemsToRetain(GBAFEChapterUnitData chapterUnit, ItemDataLoader itemData) {
		int item1ID = chapterUnit.getItem1();
		GBAFEItemData item1 = itemData.itemWithID(item1ID);
		int item2ID = chapterUnit.getItem2();
		GBAFEItemData item2 = itemData.itemWithID(item2ID);
		int item3ID = chapterUnit.getItem3();
		GBAFEItemData item3 = itemData.itemWithID(item3ID);
		int item4ID = chapterUnit.getItem4();
		GBAFEItemData item4 = itemData.itemWithID(item4ID);
		
		Set<GBAFEItemData> existingItemSet = new HashSet<GBAFEItemData>();
		if (item1 != null) { existingItemSet.add(item1); }
		if (item2 != null) { existingItemSet.add(item2); }
		if (item3 != null) { existingItemSet.add(item3); }
		if (item4 != null) { existingItemSet.add(item4); }
		
		Set<GBAFEItemData> itemsToRetain = new HashSet<GBAFEItemData>(Arrays.asList(itemData.specialItemsToRetain()));
		itemsToRetain.retainAll(existingItemSet);
		return itemsToRetain;
	}
	
	private static void itemsToGiveBack(GBAFEChapterUnitData chapterUnit, Set<GBAFEItemData> itemsToRetain, ItemDataLoader itemData) {
		int item1ID = chapterUnit.getItem1();
		GBAFEItemData item1 = itemData.itemWithID(item1ID);
		int item2ID = chapterUnit.getItem2();
		GBAFEItemData item2 = itemData.itemWithID(item2ID);
		int item3ID = chapterUnit.getItem3();
		GBAFEItemData item3 = itemData.itemWithID(item3ID);
		int item4ID = chapterUnit.getItem4();
		GBAFEItemData item4 = itemData.itemWithID(item4ID);
		
		if (!itemsToRetain.isEmpty()) {
			if (item1 != null) { itemsToRetain.remove(item1); }
			if (item2 != null) { itemsToRetain.remove(item2); }
			if (item3 != null) { itemsToRetain.remove(item3); }
			if (item4 != null) { itemsToRetain.remove(item4); }
		}
	}
	
	public static void validateSpecialClassInventory(GBAFEChapterUnitData chapterUnit, ItemDataLoader itemData, Random rng) {
		Set<GBAFEItemData> itemsToRetain = itemsToRetain(chapterUnit, itemData);
		
		GBAFEItemData[] requiredItems = itemData.specialInventoryForClass(chapterUnit.getStartingClass(), rng);
		if (requiredItems != null && requiredItems.length > 0) {
			giveItemsToChapterUnit(chapterUnit, requiredItems);
		}
		
		itemsToGiveBack(chapterUnit, itemsToRetain, itemData);
		if (!itemsToRetain.isEmpty()) {
			int[] idsToGiveBack = itemsToRetain.stream().mapToInt(item -> (item.getID())).toArray();
			chapterUnit.giveItems(idsToGiveBack);
		}
	}
	
	private static void giveItemsToChapterUnit(GBAFEChapterUnitData chapterUnit, GBAFEItemData[] items) {
		int[] requiredItemIDs = new int[items.length];
		for (int i = 0; i < items.length; i++) {
			requiredItemIDs[i] = items[i].getID();
		}
		chapterUnit.giveItems(requiredItemIDs);
	}
	
	private static void validateMinionInventory(ItemAssignmentOptions inventoryOptions, GBAFEChapterUnitData chapterUnit, ClassDataLoader classData, ItemDataLoader itemData, Random rng) {
		int classID = chapterUnit.getStartingClass();
		GBAFEClassData unitClass = classData.classForID(classID);
		if (unitClass != null) {
			int item1ID = chapterUnit.getItem1();
			GBAFEItemData item1 = itemData.itemWithID(item1ID);
			if (item1 != null && (itemData.isWeapon(item1) || item1.getType() == WeaponType.STAFF)) {
				if (!unitClass.canUseWeapon(item1)) {
					GBAFEItemData replacementItem = itemData.getSidegradeWeapon(unitClass, item1, inventoryOptions.weaponPolicy == WeaponReplacementPolicy.STRICT, rng);
					if (replacementItem != null) {
						chapterUnit.setItem1(replacementItem.getID());
					} else {
						chapterUnit.setItem1(0);
					}
				}
			}
			
			int item2ID = chapterUnit.getItem2();
			GBAFEItemData item2 = itemData.itemWithID(item2ID);
			if (item2 != null && (itemData.isWeapon(item2) || item2.getType() == WeaponType.STAFF)) {
				if (!unitClass.canUseWeapon(item2)) {
					GBAFEItemData replacementItem = itemData.getSidegradeWeapon(unitClass, item2, inventoryOptions.weaponPolicy == WeaponReplacementPolicy.STRICT, rng);
					if (replacementItem != null) {
						chapterUnit.setItem2(replacementItem.getID());
					} else {
						chapterUnit.setItem2(0);
					}
				}
			}
			
			int item3ID = chapterUnit.getItem3();
			GBAFEItemData item3 = itemData.itemWithID(item3ID);
			if (item3 != null && (itemData.isWeapon(item3) || item3.getType() == WeaponType.STAFF)) {
				if (!unitClass.canUseWeapon(item3)) {
					GBAFEItemData replacementItem = itemData.getSidegradeWeapon(unitClass, item3, inventoryOptions.weaponPolicy == WeaponReplacementPolicy.STRICT, rng);
					if (replacementItem != null) {
						chapterUnit.setItem3(replacementItem.getID());
					} else {
						chapterUnit.setItem3(0);
					}
				}
			}
			
			int item4ID = chapterUnit.getItem4();
			GBAFEItemData item4 = itemData.itemWithID(item4ID);
			if (item4 != null && (itemData.isWeapon(item4) || item4.getType() == WeaponType.STAFF)) {
				if (!unitClass.canUseWeapon(item4)) {
					GBAFEItemData replacementItem = itemData.getSidegradeWeapon(unitClass, item4, inventoryOptions.weaponPolicy == WeaponReplacementPolicy.STRICT, rng);
					if (replacementItem != null) {
						chapterUnit.setItem4(replacementItem.getID());
					} else {
						chapterUnit.setItem4(0);
					}
				}
			}
		}
	}
	
	public static void validateCharacterInventory(ItemAssignmentOptions inventoryOptions, GBAFECharacterData character, GBAFEClassData charClass, GBAFEChapterUnitData chapterUnit, Boolean ranged, Boolean melee, ClassDataLoader classData, ItemDataLoader itemData, TextLoader textData, Boolean forceBasic, Random rng) {
		int item1ID = chapterUnit.getItem1();
		GBAFEItemData item1 = itemData.itemWithID(item1ID);
		int item2ID = chapterUnit.getItem2();
		GBAFEItemData item2 = itemData.itemWithID(item2ID);
		int item3ID = chapterUnit.getItem3();
		GBAFEItemData item3 = itemData.itemWithID(item3ID);
		int item4ID = chapterUnit.getItem4();
		GBAFEItemData item4 = itemData.itemWithID(item4ID);
		
		GBAFEItemData[] prfWeapons = itemData.prfWeaponsForClass(charClass.getID());
		Set<Integer> prfIDs = new HashSet<Integer>();
		for (GBAFEItemData prfWeapon : prfWeapons) {
			prfIDs.add(prfWeapon.getID());
		}
		
		Boolean isHealerClass = charClass.getStaffRank() > 0;
		Boolean hasAtLeastOneHealingStaff = false;
		
		Boolean classCanAttack = classData.canClassAttack(charClass.getID());
		Boolean hasAtLeastOneWeapon = false;
		
		Set<GBAFEItemData> itemsToRetain = itemsToRetain(chapterUnit, itemData);
		
		DebugPrinter.log(DebugPrinter.Key.CLASS_RANDOMIZER, "Validating inventory for character 0x" + Integer.toHexString(character.getID()) + " (" + textData.getStringAtIndex(character.getNameIndex(), true) +") in class 0x" + Integer.toHexString(charClass.getID()) + " (" + textData.getStringAtIndex(charClass.getNameIndex(), true) + ")");
		DebugPrinter.log(DebugPrinter.Key.CLASS_RANDOMIZER, "Original Inventory: [0x" + Integer.toHexString(item1ID) + (item1 == null ? "" : " (" + textData.getStringAtIndex(item1.getNameIndex(), true) + ")") + ", 0x" + Integer.toHexString(item2ID) + (item2 == null ? "" : " (" + textData.getStringAtIndex(item2.getNameIndex(), true) + ")") + ", 0x" + Integer.toHexString(item3ID) + (item3 == null ? "" : " (" + textData.getStringAtIndex(item3.getNameIndex(), true) + ")") + ", 0x" + Integer.toHexString(item4ID) + (item4 == null ? "" : " (" + textData.getStringAtIndex(item4.getNameIndex(), true) + ")") + "]");
		
		if (itemData.isWeapon(item1) || (item1 != null && item1.getType() == WeaponType.STAFF)) {
			if (!canCharacterUseItem(character, item1, itemData) || (item1.getWeaponRank() == WeaponRank.PRF && !prfIDs.contains(item1ID))) {
				GBAFEItemData replacementItem = itemData.getBasicWeaponForCharacter(character, ranged, false, rng);
				if (!forceBasic) {
					if (inventoryOptions.weaponPolicy == WeaponReplacementPolicy.ANY_USABLE) {
						replacementItem = itemData.getRandomWeaponForCharacter(character, ranged, melee, rng);
					} else {
						replacementItem = itemData.getSidegradeWeapon(character, item1, inventoryOptions.weaponPolicy == WeaponReplacementPolicy.STRICT, rng);
					}
				}
				
				if (item1.getWeaponRank() == WeaponRank.S) {
					GBAFEItemData[] topWeapons = topRankWeaponsForClass(charClass, itemData);
					if (topWeapons.length > 0) {
						replacementItem = topWeapons[rng.nextInt(topWeapons.length)];
					}
				}
				if (replacementItem != null) {
					if (replacementItem.getType() == WeaponType.STAFF) { hasAtLeastOneHealingStaff = hasAtLeastOneHealingStaff || itemData.isHealingStaff(replacementItem.getID()); }
					else { hasAtLeastOneWeapon = hasAtLeastOneWeapon || itemData.isWeapon(replacementItem); }
					chapterUnit.setItem1(replacementItem.getID());
				} else {
					chapterUnit.setItem1(0);
				}
			} else {
				if (item1.getType() == WeaponType.STAFF) { hasAtLeastOneHealingStaff = hasAtLeastOneHealingStaff || itemData.isHealingStaff(item1.getID()); }
				else { hasAtLeastOneWeapon = hasAtLeastOneWeapon || itemData.isWeapon(item1); }
			}
		}
		
		if (itemData.isWeapon(item2) || (item2 != null && item2.getType() == WeaponType.STAFF)) {
			if (!canCharacterUseItem(character, item2, itemData) || (item2.getWeaponRank() == WeaponRank.PRF && !prfIDs.contains(item2ID))) {
				GBAFEItemData replacementItem = itemData.getBasicWeaponForCharacter(character, ranged, false, rng);
				if (!forceBasic) {
					if (inventoryOptions.weaponPolicy == WeaponReplacementPolicy.ANY_USABLE) {
						replacementItem = itemData.getRandomWeaponForCharacter(character, ranged, melee, rng);
					} else {
						replacementItem = itemData.getSidegradeWeapon(character, item2, inventoryOptions.weaponPolicy == WeaponReplacementPolicy.STRICT, rng);
					}
				}
				
				if (item2.getWeaponRank() == WeaponRank.S) {
					GBAFEItemData[] topWeapons = topRankWeaponsForClass(charClass, itemData);
					if (topWeapons.length > 0) {
						replacementItem = topWeapons[rng.nextInt(topWeapons.length)];
					}
				}
				if (replacementItem != null) {
					if (replacementItem.getType() == WeaponType.STAFF) { hasAtLeastOneHealingStaff = hasAtLeastOneHealingStaff || itemData.isHealingStaff(replacementItem.getID()); }
					else { hasAtLeastOneWeapon = hasAtLeastOneWeapon || itemData.isWeapon(replacementItem); }
					chapterUnit.setItem2(replacementItem.getID());
				} else {
					chapterUnit.setItem2(0);
				}
			} else {
				if (item2.getType() == WeaponType.STAFF) { hasAtLeastOneHealingStaff = hasAtLeastOneHealingStaff || itemData.isHealingStaff(item2.getID()); }
				else { hasAtLeastOneWeapon = hasAtLeastOneWeapon || itemData.isWeapon(item2); }
			}
		}
		
		if (itemData.isWeapon(item3) || (item3 != null && item3.getType() == WeaponType.STAFF)) {
			if (!canCharacterUseItem(character, item3, itemData) || (item3.getWeaponRank() == WeaponRank.PRF && !prfIDs.contains(item3ID))) {
				GBAFEItemData replacementItem = itemData.getBasicWeaponForCharacter(character, ranged, false, rng);
				if (!forceBasic) {
					if (inventoryOptions.weaponPolicy == WeaponReplacementPolicy.ANY_USABLE) {
						replacementItem = itemData.getRandomWeaponForCharacter(character, ranged, melee, rng);
					} else {
						replacementItem = itemData.getSidegradeWeapon(character, item3, inventoryOptions.weaponPolicy == WeaponReplacementPolicy.STRICT, rng);
					}
				}
				
				if (item3.getWeaponRank() == WeaponRank.S) {
					GBAFEItemData[] topWeapons = topRankWeaponsForClass(charClass, itemData);
					if (topWeapons.length > 0) {
						replacementItem = topWeapons[rng.nextInt(topWeapons.length)];
					}
				}
				if (replacementItem != null) {
					if (replacementItem.getType() == WeaponType.STAFF) { hasAtLeastOneHealingStaff = hasAtLeastOneHealingStaff || itemData.isHealingStaff(replacementItem.getID()); }
					else { hasAtLeastOneWeapon = hasAtLeastOneWeapon || itemData.isWeapon(replacementItem); }
					chapterUnit.setItem3(replacementItem.getID());
				} else {
					chapterUnit.setItem3(0);
				}
			} else {
				if (item3.getType() == WeaponType.STAFF) { hasAtLeastOneHealingStaff = hasAtLeastOneHealingStaff || itemData.isHealingStaff(item3.getID()); }
				else { hasAtLeastOneWeapon = hasAtLeastOneWeapon || itemData.isWeapon(item3); }
			}
		}
		
		if (itemData.isWeapon(item4) || (item4 != null && item4.getType() == WeaponType.STAFF)) {
			if (!canCharacterUseItem(character, item4, itemData) || (item4.getWeaponRank() == WeaponRank.PRF && !prfIDs.contains(item4ID))) {
				GBAFEItemData replacementItem = itemData.getBasicWeaponForCharacter(character, ranged, false, rng);
				if (!forceBasic) {
					if (inventoryOptions.weaponPolicy == WeaponReplacementPolicy.ANY_USABLE) {
						replacementItem = itemData.getRandomWeaponForCharacter(character, ranged, melee, rng);
					} else {
						replacementItem = itemData.getSidegradeWeapon(character, item4, inventoryOptions.weaponPolicy == WeaponReplacementPolicy.STRICT, rng);
					}
				}
				
				if (item4.getWeaponRank() == WeaponRank.S) {
					GBAFEItemData[] topWeapons = topRankWeaponsForClass(charClass, itemData);
					if (topWeapons.length > 0) {
						replacementItem = topWeapons[rng.nextInt(topWeapons.length)];
					}
				}
				if (replacementItem != null) {
					if (replacementItem.getType() == WeaponType.STAFF) { hasAtLeastOneHealingStaff = hasAtLeastOneHealingStaff || itemData.isHealingStaff(replacementItem.getID()); }
					else { hasAtLeastOneWeapon = hasAtLeastOneWeapon || itemData.isWeapon(replacementItem); }
					chapterUnit.setItem4(replacementItem.getID());
				} else {
					chapterUnit.setItem4(0);
				}
			} else {
				if (item4.getType() == WeaponType.STAFF) { hasAtLeastOneHealingStaff = hasAtLeastOneHealingStaff || itemData.isHealingStaff(item4.getID()); }
				else { hasAtLeastOneWeapon = hasAtLeastOneWeapon || itemData.isWeapon(item4); }
			}
		}
		
		if (isHealerClass && !hasAtLeastOneHealingStaff) {
			chapterUnit.giveItems(new int[] {itemData.getRandomHealingStaff(itemData.weaponRankFromValue(character.getStaffRank()), rng).getID()});
		}
		if (classCanAttack && !hasAtLeastOneWeapon) {
			GBAFEItemData basicWeapon = itemData.getBasicWeaponForCharacter(character, ranged, true, rng);
			if (basicWeapon != null) {
				chapterUnit.giveItems(new int[] {basicWeapon.getID()});
			}
		}
		
		itemsToGiveBack(chapterUnit, itemsToRetain, itemData);
		if (!itemsToRetain.isEmpty()) {
			int[] idsToGiveBack = itemsToRetain.stream().mapToInt(item -> (item.getID())).toArray();
			chapterUnit.giveItems(idsToGiveBack);
		}
		
		DebugPrinter.log(DebugPrinter.Key.CLASS_RANDOMIZER, "Final Inventory: [0x" + Integer.toHexString(item1ID) + (item1 == null ? "" : " (" + textData.getStringAtIndex(item1.getNameIndex(), true) + ")") + ", 0x" + Integer.toHexString(item2ID) + (item2 == null ? "" : " (" + textData.getStringAtIndex(item2.getNameIndex(), true) + ")") + ", 0x" + Integer.toHexString(item3ID) + (item3 == null ? "" : " (" + textData.getStringAtIndex(item3.getNameIndex(), true) + ")") + ", 0x" + Integer.toHexString(item4ID) + (item4 == null ? "" : " (" + textData.getStringAtIndex(item4.getNameIndex(), true) + ")") + "]");
	}
	
	private static GBAFEItemData[] topRankWeaponsForClass(GBAFEClassData characterClass, ItemDataLoader itemData) {
		ArrayList<GBAFEItemData> items = new ArrayList<GBAFEItemData>();
		if (characterClass.getSwordRank() > 0) { items.addAll(Arrays.asList(itemData.itemsOfTypeAndEqualRank(WeaponType.SWORD, WeaponRank.S, false, false, true))); }
		if (characterClass.getLanceRank() > 0) { items.addAll(Arrays.asList(itemData.itemsOfTypeAndEqualRank(WeaponType.LANCE, WeaponRank.S, false, false, true))); }
		if (characterClass.getAxeRank() > 0) { items.addAll(Arrays.asList(itemData.itemsOfTypeAndEqualRank(WeaponType.AXE, WeaponRank.S, false, false, true))); }
		if (characterClass.getBowRank() > 0) { items.addAll(Arrays.asList(itemData.itemsOfTypeAndEqualRank(WeaponType.BOW, WeaponRank.S, false, false, true))); }
		if (characterClass.getAnimaRank() > 0) { items.addAll(Arrays.asList(itemData.itemsOfTypeAndEqualRank(WeaponType.ANIMA, WeaponRank.S, false, false, true))); }
		if (characterClass.getLightRank() > 0) { items.addAll(Arrays.asList(itemData.itemsOfTypeAndEqualRank(WeaponType.LIGHT, WeaponRank.S, false, false, true))); }
		if (characterClass.getDarkRank() > 0) { items.addAll(Arrays.asList(itemData.itemsOfTypeAndEqualRank(WeaponType.DARK, WeaponRank.S, false, false, true))); }
		if (characterClass.getStaffRank() > 0) { items.addAll(Arrays.asList(itemData.itemsOfTypeAndEqualRank(WeaponType.STAFF, WeaponRank.S, false, false, true))); }
		
		return items.toArray(new GBAFEItemData[items.size()]);
	}
	
	private static Boolean canCharacterUseItem(GBAFECharacterData character, GBAFEItemData weapon, ItemDataLoader itemData) {
		int weaponRankValue = itemData.weaponRankValueForRank(weapon.getWeaponRank());
		if ((weapon.getType() == WeaponType.SWORD && character.getSwordRank() >= weaponRankValue) ||
				(weapon.getType() == WeaponType.LANCE && character.getLanceRank() >= weaponRankValue) ||
				(weapon.getType() == WeaponType.AXE && character.getAxeRank() >= weaponRankValue) ||
				(weapon.getType() == WeaponType.BOW && character.getBowRank() >= weaponRankValue) ||
				(weapon.getType() == WeaponType.ANIMA && character.getAnimaRank() >= weaponRankValue) ||
				(weapon.getType() == WeaponType.LIGHT && character.getLightRank() >= weaponRankValue) ||
				(weapon.getType() == WeaponType.DARK && character.getDarkRank() >= weaponRankValue) ||
				(weapon.getType() == WeaponType.STAFF && character.getStaffRank() >= weaponRankValue)) {
			return true;
		}
		
		return false;
	}
	
	private static void transferWeaponLevels(GBAFECharacterData character, GBAFEClassData sourceClass, GBAFEClassData targetClass, Random rng) {
		ArrayList<Integer> ranks = new ArrayList<Integer>();
		
		if (character.getSwordRank() > 0) { ranks.add(character.getSwordRank()); }
		if (character.getLanceRank() > 0) { ranks.add(character.getLanceRank()); }
		if (character.getAxeRank() > 0) { ranks.add(character.getAxeRank()); }
		if (character.getBowRank() > 0) { ranks.add(character.getBowRank()); }
		if (character.getAnimaRank() > 0) { ranks.add(character.getAnimaRank()); }
		if (character.getLightRank() > 0) { ranks.add(character.getLightRank()); }
		if (character.getDarkRank() > 0) { ranks.add(character.getDarkRank()); }
		if (character.getStaffRank() > 0) { ranks.add(character.getStaffRank()); }
		
		Collections.sort(ranks);
		
		Boolean applySwordRank = targetClass.getSwordRank() > 0;
		Boolean applyLanceRank = targetClass.getLanceRank() > 0;
		Boolean applyAxeRank = targetClass.getAxeRank() > 0;
		Boolean applyBowRank = targetClass.getBowRank() > 0;
		Boolean applyAnimaRank = targetClass.getAnimaRank() > 0;
		Boolean applyLightRank = targetClass.getLightRank() > 0;
		Boolean applyDarkRank = targetClass.getDarkRank() > 0;
		Boolean applyStaffRank = targetClass.getStaffRank() > 0;
		
		int weaponUsageCount = 0;
		if (applySwordRank) { weaponUsageCount++; }
		if (applyLanceRank) { weaponUsageCount++; }
		if (applyAxeRank) { weaponUsageCount++; }
		if (applyBowRank) { weaponUsageCount++; }
		if (applyAnimaRank) { weaponUsageCount++; }
		if (applyLightRank) { weaponUsageCount++; }
		if (applyDarkRank) { weaponUsageCount++; }
		if (applyStaffRank) { weaponUsageCount++; }
		
		while (ranks.size() > weaponUsageCount) {
			ranks.remove(0); // Remove the lowest rank if we have more ranks to work with than slots to fill
		}
		
		int[] targetRanks = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		
		if (applySwordRank) {
			int rankToApply = targetClass.getBaseRankValue();
			if (ranks.size() > 0) {
				int rankIndex = rng.nextInt(ranks.size());
				rankToApply = Math.max(ranks.get(rankIndex), rankToApply);
				if (rng.nextInt(2) == 0) {
					ranks.remove(rankIndex);
				}
			}
			targetRanks[0] = rankToApply;
		}
		if (applyLanceRank) {
			int rankToApply = targetClass.getBaseRankValue();
			if (ranks.size() > 0) {
				int rankIndex = rng.nextInt(ranks.size());
				rankToApply = Math.max(ranks.get(rankIndex), rankToApply);
				if (rng.nextInt(2) == 0) {
					ranks.remove(rankIndex);
				}
			}
			targetRanks[1] = rankToApply;
		}
		if (applyAxeRank) {
			int rankToApply = targetClass.getBaseRankValue();
			if (ranks.size() > 0) {
				int rankIndex = rng.nextInt(ranks.size());
				rankToApply = Math.max(ranks.get(rankIndex), rankToApply);
				if (rng.nextInt(2) == 0) {
					ranks.remove(rankIndex);
				}
			}
			targetRanks[2] = rankToApply;
		}
		if (applyBowRank) {
			int rankToApply = targetClass.getBaseRankValue();
			if (ranks.size() > 0) {
				int rankIndex = rng.nextInt(ranks.size());
				rankToApply = Math.max(ranks.get(rankIndex), rankToApply);
				if (rng.nextInt(2) == 0) {
					ranks.remove(rankIndex);
				}
			}
			targetRanks[3] = rankToApply;
		}
		if (applyAnimaRank) {
			int rankToApply = targetClass.getBaseRankValue();
			if (ranks.size() > 0) {
				int rankIndex = rng.nextInt(ranks.size());
				rankToApply = Math.max(ranks.get(rankIndex), rankToApply);
				if (rng.nextInt(2) == 0) {
					ranks.remove(rankIndex);
				}
			}
			targetRanks[4] = rankToApply;
		}
		if (applyLightRank) {
			int rankToApply = targetClass.getBaseRankValue();
			if (ranks.size() > 0) {
				int rankIndex = rng.nextInt(ranks.size());
				rankToApply = Math.max(ranks.get(rankIndex), rankToApply);
				if (rng.nextInt(2) == 0) {
					ranks.remove(rankIndex);
				}
			}
			targetRanks[5] = rankToApply;
		}
		if (applyDarkRank) {
			int rankToApply = targetClass.getBaseRankValue();
			if (ranks.size() > 0) {
				int rankIndex = rng.nextInt(ranks.size());
				rankToApply = Math.max(ranks.get(rankIndex), rankToApply);
				if (rng.nextInt(2) == 0) {
					ranks.remove(rankIndex);
				}
			}
			targetRanks[6] = rankToApply;
		}
		if (applyStaffRank) {
			int rankToApply = targetClass.getBaseRankValue();
			if (ranks.size() > 0) {
				int rankIndex = rng.nextInt(ranks.size());
				rankToApply = Math.max(ranks.get(rankIndex), rankToApply);
				if (rng.nextInt(2) == 0) {
					ranks.remove(rankIndex);
				}
			}
			targetRanks[7] = rankToApply;
		}
		
		character.setSwordRank(targetRanks[0]);
		character.setLanceRank(targetRanks[1]);
		character.setAxeRank(targetRanks[2]);
		character.setBowRank(targetRanks[3]);
		character.setAnimaRank(targetRanks[4]);
		character.setLightRank(targetRanks[5]);
		character.setDarkRank(targetRanks[6]);
		character.setStaffRank(targetRanks[7]);
	}
}
