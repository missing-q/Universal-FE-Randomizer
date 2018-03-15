package fedata;

import util.WhyDoesJavaNotHaveThese;

public class FE7Character implements FECharacter {

	private byte[] originalData;
	private byte[] data;
	
	private long originalOffset;
	
	private Boolean wasModified = false;
	
	public FE7Character(byte[] data, long originalOffset) {
		super();
		this.originalData = data;
		this.data = data;
		this.originalOffset = originalOffset;
	}
	
	public int getNameIndex() {
		return (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
	}
	
	public int getDescriptionIndex() {
		return (data[2] & 0xFF) | ((data[3] & 0xFF) << 8);
	}
	
	public int getHPGrowth() {
		return data[28] & 0xFF;
	}
	
	public void setHPGrowth(int hpGrowth) {
		hpGrowth = WhyDoesJavaNotHaveThese.clamp(hpGrowth, 0, 255);
		data[28] = (byte)(hpGrowth & 0xFF);
		wasModified = true;
	}
	
	public int getSTRGrowth() {
		return data[29] & 0xFF;
	}
	
	public void setSTRGrowth(int strGrowth) {
		strGrowth = WhyDoesJavaNotHaveThese.clamp(strGrowth, 0, 255);
		data[29] = (byte)(strGrowth & 0xFF);
		wasModified = true;
	}
	
	public int getSKLGrowth() {
		return data[30] & 0xFF;
	}
	
	public void setSKLGrowth(int sklGrowth) {
		sklGrowth = WhyDoesJavaNotHaveThese.clamp(sklGrowth, 0, 255);
		data[30] = (byte)(sklGrowth & 0xFF);
		wasModified = true;
	}
	
	public int getSPDGrowth() {
		return data[31] & 0xFF;
	}
	
	public void setSPDGrowth(int spdGrowth) {
		spdGrowth = WhyDoesJavaNotHaveThese.clamp(spdGrowth, 0, 255);
		data[31] = (byte)(spdGrowth & 0xFF);
		wasModified = true;
	}
	
	public int getDEFGrowth() {
		return data[32] & 0xFF;
	}
	
	public void setDEFGrowth(int defGrowth) {
		defGrowth = WhyDoesJavaNotHaveThese.clamp(defGrowth, 0, 255);
		data[32] = (byte)(defGrowth & 0xFF);
		wasModified = true;
	}
	
	public int getRESGrowth() {
		return data[33] & 0xFF;
	}
	
	public void setRESGrowth(int resGrowth) {
		resGrowth = WhyDoesJavaNotHaveThese.clamp(resGrowth, 0, 255);
		data[33] = (byte)(resGrowth & 0xFF);
		wasModified = true;
	}
	
	public int getLCKGrowth() {
		return data[34] & 0xFF;
	}
	
	public void setLCKGrowth(int lckGrowth) {
		lckGrowth = WhyDoesJavaNotHaveThese.clamp(lckGrowth, 0, 255);
		data[34] = (byte)(lckGrowth & 0xFF);
		wasModified = true;
	}
	
	public void resetData() {
		data = originalData;
		wasModified = false;
	}
	
	public byte[] getCharacterData() {
		return data;
	}
	
	public Boolean wasModified() {
		return wasModified;
	}
	
	public long getAddressOffset() {
		return originalOffset;
	}
}