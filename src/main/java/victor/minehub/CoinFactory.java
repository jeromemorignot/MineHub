package victor.minehub;

public class CoinFactory {
	public static Coin newCoin(String name) {
		if (name.equalsIgnoreCase("LBC")) return new LbcCoin();
		if (name.equalsIgnoreCase("SIB")) return new SibCoin();
		return new Coin(name);
	}
	
	private CoinFactory(){}
}
