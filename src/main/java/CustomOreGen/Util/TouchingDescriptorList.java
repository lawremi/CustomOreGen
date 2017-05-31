package CustomOreGen.Util;

import java.util.ArrayList;

import CustomOreGen.Server.DistributionSettingMap.Copyable;

public class TouchingDescriptorList extends ArrayList<TouchingDescriptor> implements Copyable<TouchingDescriptorList> {
	private static final long serialVersionUID = 1L;

	@Override
	public void copyFrom(TouchingDescriptorList var1) {
		this.clear();
		
		for (TouchingDescriptor descriptor : var1) {
			this.add(descriptor);
		}
	}
}
