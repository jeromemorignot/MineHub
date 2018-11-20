package victor.minehub;

import java.util.regex.Pattern;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Coin {

	private String name;
	private double btcPrice;
	private double netHashRate; //per second
	private double difficulty;
	private double reward;
	private String nodeHost;
	private int nodePort;
	private String rpcUsername;
	private String rpcPassword;
	private double dKoef,hrKoef;
	private String algo;
	
	public double getBTCPerDay(double hashrate) {
		return btcPrice * getCoinsPerDay(hashrate);
	}
	
	public double getCoinsPerDay(double hashrate) {
		return getReward() * hashrate * hrKoef * 60 * 60 * 24 / (getDifficulty() * dKoef);
	}
	
	public void setKoef(String koefStr) {
		String[] koefs = koefStr.split(":");
		dKoef = !koefs[0].contains("^") ? Double.valueOf(koefs[0]) 
				: Double.valueOf(Math.pow(Double.valueOf(koefs[0].split(Pattern.quote("^"))[0]), Double.valueOf(koefs[0].split(Pattern.quote("^"))[1])));
		hrKoef = !koefs[1].contains("^") ? Double.valueOf(koefs[1]) 
				: Double.valueOf(Math.pow(Double.valueOf(koefs[1].split(Pattern.quote("^"))[0]), Double.valueOf(koefs[1].split(Pattern.quote("^"))[1])));
	}
	
	public double getDKoef() {
		return dKoef;
	}
	
	public double getHrKoef() {
		return hrKoef;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);		
	}
	
	public Coin(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setBtcPrice(double lastDayBtcPrice) {
		this.btcPrice = lastDayBtcPrice;
	}
	
	public double getBtcPrice() {
		return btcPrice;
	}
	
	public void setNetHashRate(double netHashRate) {
		this.netHashRate = netHashRate;
	}
	
	public synchronized double getNetHashRate() {
		return netHashRate;
	}
	
	public void setDifficulty(double difficulty) {
		this.difficulty = difficulty;
	}
	
	public synchronized double getDifficulty() {
		return difficulty;
	}
	
	public void setReward(double reward) {
		this.reward = reward;
	}
	
	public double getReward() {
		return reward;
	}

	public String getNodeHost() {
		return nodeHost;
	}

	public void setNodeHost(String nodeHost) {
		this.nodeHost = nodeHost;
	}

	public int getNodePort() {
		return nodePort;
	}

	public void setNodePort(int nodePort) {
		this.nodePort = nodePort;
	}

	public String getRpcUsername() {
		return rpcUsername;
	}

	public void setRpcUsername(String rpcUsername) {
		this.rpcUsername = rpcUsername;
	}

	public String getRpcPassword() {
		return rpcPassword;
	}

	public void setRpcPassword(String rpcPassword) {
		this.rpcPassword = rpcPassword;
	}

	public String getAlgo() {
		return algo;
	}

	public void setAlgo(String algo) {
		this.algo = algo;
	}
	
	public void parseMinginInfoJson(String jsonText) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject topJson = (JSONObject)parser.parse(jsonText);
		topJson = (JSONObject)topJson.get("result");
		parseMinginInfoJson(topJson);
	}
	
	protected synchronized void parseMinginInfoJson(JSONObject topJson) {
		double netHashRate = Double.valueOf(topJson.get("networkhashps").toString());
		double difficulty = Double.valueOf(topJson.get("difficulty").toString());					
		this.setNetHashRate(netHashRate);
		this.setDifficulty(difficulty);
	}
	
}
