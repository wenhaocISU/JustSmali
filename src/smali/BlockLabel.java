package smali;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class BlockLabel implements Serializable{


	private ArrayList<String> normalLabels;
	private int normalLabelSection;
	private ArrayList<String> tryLabels;
	
	public BlockLabel() {
		this.setNormalLabels(new ArrayList<String>());
		this.setTryLabels(new ArrayList<String>());
		this.setNormalLabelSection(0);
	}
	
	public ArrayList<String> getTryLabels() {
		return tryLabels;
	}
	
	public void setTryLabels(ArrayList<String> tryLabels) {
		this.tryLabels = tryLabels;
	}

	public void addTryLabel(String tryLabel) {
		this.tryLabels.add(tryLabel);
	}
	

	public ArrayList<String> getNormalLabels() {
		return normalLabels;
	}


	public void setNormalLabels(ArrayList<String> normalLabels) {
		this.normalLabels = normalLabels;
	}
	
	public void addNormalLabel(String normalLabel) {
		this.normalLabels.add(normalLabel);
	}

	public int getNormalLabelCount() {
		return normalLabels.size();
	}

	public int getNormalLabelSection() {
		return normalLabelSection;
	}

	public void setNormalLabelSection(int normalLabelSection) {
		this.normalLabelSection = normalLabelSection;
	}
	
	
	///////////////////////////////////////// utility
	
	public boolean hasSameNormalLabels(BlockLabel l) {
		if (normalLabels.size() == l.getNormalLabelCount())
			for (int i = 0, len = getNormalLabelCount(); i < len; i++) {
				if (!l.getNormalLabels().get(i).equals(normalLabels.get(i)))
					return false;
			}
		return true;
	}
	
	public boolean hasSameNormalLabelSection(BlockLabel l) {
		return (l.getNormalLabelSection() == getNormalLabelSection());
	}
	
	public boolean isSameNormalLabel(BlockLabel l) {
		return (hasSameNormalLabels(l) && hasSameNormalLabelSection(l));
	}
	
	public boolean hasSameTryLabels(BlockLabel l) {
		for (int i = 0, len = tryLabels.size(); i < len; i++)
			if (!l.getTryLabels().get(i).equals(tryLabels.get(i)))
				return false;
		return true;
	}
	
	public boolean isSameLabel(BlockLabel l) {
		return (hasSameNormalLabels(l) && hasSameNormalLabelSection(l)
				&& hasSameTryLabels(l));
	}
	
	public String toString() {
		String result = "";
		for (String s : normalLabels)
			result += s + " ";
		result += "(" + normalLabelSection + ")";
		if (tryLabels.size() > 0) {
			result += "  ";
			for (String s : tryLabels)
				result += s + " ";
		}
		return result;
	}

}
