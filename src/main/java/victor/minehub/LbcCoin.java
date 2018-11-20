package victor.minehub;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class LbcCoin extends Coin {
	
	private int blockCount;
	
	@Override
	protected synchronized void parseMinginInfoJson(JSONObject topJson) {
		super.parseMinginInfoJson(topJson);
		this.blockCount = Integer.valueOf(topJson.get("blocks").toString());
	}
	
	public synchronized int getBlockCount() {
		return blockCount;
	}
	
	public LbcCoin() {
		super("LBC");
	}
	
	@Override
	public void setReward(double reward) {}
	
	
	@Override
	public double getReward() {
		if (super.getReward() != 0d) {
			return super.getReward();
		}
		super.setReward(calculateReward());
		return super.getReward();
	}
	
	/**
	 * Below if taken and modified from https://github.com/lbryio/lbrycrd/blob/master/src/main.cpp, mind the copyright
	 */
	
	private int calculateReward() {
		int nSubsidyLevelInterval = 1<<5;
		
		int nStartingSubsidy = 500;
	    int nLevel = (getBlockCount() - 55001) / nSubsidyLevelInterval;
	    int nReduction = ((-1 + (int)Math.sqrt((8 * nLevel) + 1)) / 2);
	    while (!(withinLevelBounds(nReduction, nLevel)))
	    {
	        if (((nReduction * nReduction + nReduction) >> 1) > nLevel)
	        {
	            nReduction--;
	        }
	        else
	        {
	            nReduction++;
	        }
	    }
	    int nSubsidyReduction = nReduction;
	    if (nSubsidyReduction >= nStartingSubsidy)
	        return 0;
	    return nStartingSubsidy - nSubsidyReduction;
	}
	
	private boolean withinLevelBounds(int nReduction, int nLevel)
	{
	    if (((nReduction * nReduction + nReduction) >> 1) > nLevel)
	        return false;
	    nReduction += 1;
	    if (((nReduction * nReduction + nReduction) >> 1) <= nLevel)
	        return false;
	    return true;
	}
}
