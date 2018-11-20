package victor.minehub;

import java.util.Arrays;

public class SibCoin extends Coin {
	public SibCoin() {
		super("SIB");
	}
	
	@Override
	public void setReward(double reward) {}
	
	private static double[] diffs = {0, 84, 139, 198, 261, 328, 400, 478,
			563, 654, 755, 865, 986, 1122, 1273, 1446, 1643, 1873,
			2144, 2471, 2878, 99000
	};
	
	private static double[] rewards = {8.62, 8.28, 7.93, 7.59, 7.24,
			6.9, 6.55, 6.21, 5.86, 5.52, 5.17, 4.83, 4.48, 4.14, 3.79,
			3.45, 3.1, 2.76, 2.41, 2.07, 1.72, 1.72
	};
	
	@Override
	public double getReward() {
		return getReward(getDifficulty());
	}
	
	protected double getReward(double diff) {
		if (diff == 0) return 0; // invalid input, but no exception is by purpose
		int index = Arrays.binarySearch(diffs, diff);
		if (index < 0) {
			index = -2 - index;
		} else {
			return rewards[index];
		}
		//some approximation here, about 3% to 20% inaccuracy! Really unclear where's the gap, on sib official site or whattomine.
		//however, sibcoin get on top on big spikes, so I'd keep it even that innacurate
		double diffOffset = diff-diffs[index];
		double diffsDelta = diffs[index+1]-diffs[index];
		double rewardsDelta = rewards[index+1]-rewards[index];
		double result = rewards[index] + diffOffset * rewardsDelta / diffsDelta;
		return result; 
	}
}
