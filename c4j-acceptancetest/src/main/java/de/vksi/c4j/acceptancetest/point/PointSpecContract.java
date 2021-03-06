package de.vksi.c4j.acceptancetest.point;

import static de.vksi.c4j.Condition.ignored;
import static de.vksi.c4j.Condition.postCondition;
import de.vksi.c4j.Target;

public class PointSpecContract implements PointSpec {

	@Target
	private PointSpec target;

	@Override
	public int getX() {
		// No contracts identified yet
		return (Integer) ignored();
	}

	@Override
	public int getY() {
		// No contracts identified yet
		return (Integer) ignored();
	}

	@Override
	public void setX(int x) {
		if (postCondition()) {
			assert target.getX() == x : "x set";
		}
	}

	@Override
	public void setY(int y) {
		if (postCondition()) {
			assert target.getY() == y : "y set";
		}
	}

}