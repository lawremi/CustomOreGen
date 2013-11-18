package com.xcompwiz.mystcraft.api.internals;

/**
 * Do NOT implement this yourself!
 * @author xcompwiz
 */
public interface IStorageObject {

	public abstract boolean getBoolean(String string);
	public abstract void setBoolean(String string, boolean var2);
	public abstract int getInteger(String string);
	public abstract void setInteger(String string, int var2);
}
