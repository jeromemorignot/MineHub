package victor.minehub;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import sun.misc.BASE64Encoder;
import sun.net.www.protocol.https.HttpsURLConnectionImpl;

public class Main {
	
	private static final Logger logger = Logger.getLogger(Main.class);
	private static Properties props = new Properties();
	private static double btcToUsd;
	
	private static void exit() {
		System.exit(-121);
	}
	
	private static String getStringByURL(URL jsonURL) throws IOException {
		HttpURLConnection conn;
		String jsonText = null;
		InputStream httpIn = null;
		
		try {
			conn = (HttpURLConnection) jsonURL.openConnection();
			
			conn.setRequestMethod("GET");
	        conn.setRequestProperty("Content-Type", 
	                   "application/x-www-form-urlencoded");
	        conn.setRequestProperty("Content-Language", "en-US"); 
	        conn.setRequestProperty("User-Agent",
	                "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11");
	        conn.setUseCaches(false);
	        conn.setDoInput(true);
	        conn.setDoOutput(true);
			
			httpIn = new BufferedInputStream(conn.getInputStream());     
			jsonText = IOUtils.toString(httpIn);
		} finally {
			IOUtils.closeQuietly(httpIn);
		}
		
		return jsonText;
	}
	
	private static String bittrexSummariesUrl, cryptopiaSummariesUrl;
	
	static void getCoinsPrice(Map<String, Coin> coins) throws IOException, ParseException {
		Set<String> coinsFilled = new HashSet<>();
		JSONParser parser = new JSONParser();
		String jsonText = getStringByURL(new URL(bittrexSummariesUrl));
		JSONObject topJson = (JSONObject)parser.parse(jsonText);
		for(Object marketObj : (JSONArray)topJson.get("result")) {
			JSONObject market = (JSONObject)marketObj;
			String marketName = market.get("MarketName").toString();
			if (marketName.equals("USDT-BTC")) {
				btcToUsd = Double.valueOf(market.get("Last").toString());
				logger.debug(String.format("USDT-BTC: %f2.5 ", btcToUsd));
				continue;
			}
			String coinName;
			if (marketName.startsWith("BTC") && coins.keySet().contains(coinName = marketName.split("-")[1])) {
				double price = Double.valueOf(market.get("Last").toString());
				coins.get(coinName).setBtcPrice(price);
				coinsFilled.add(coinName);
			}
		}
		logger.info("Received prices from Bittex (primary source)");
		jsonText = getStringByURL(new URL(cryptopiaSummariesUrl));
		topJson = (JSONObject)parser.parse(jsonText);
		for(Object marketObj : (JSONArray)topJson.get("Data")) {
			JSONObject market = (JSONObject)marketObj;
			String marketName = market.get("Label").toString();
			String coinName;
			if (!coinsFilled.contains(coinName = marketName.split("/")[0]) && coins.keySet().contains(coinName)) {
				// no need to check for non-BTC markets as BTC was selected in URL
				double price = Double.valueOf(market.get("LastPrice").toString());
				coins.get(coinName).setBtcPrice(price);
				coinsFilled.add(coinName);
			}
		}
		logger.info("Received prices from Cryptopia (secondary source)");
		if (coins.keySet().contains("BTG")) {
			//jsonText = getStringByURL(new URL("https://api.bitfinex.com/v1/pubticker/btgbtc"));
			jsonText = getStringByURL(new URL("https://api.hitbtc.com/api/2/public/ticker/BTGBTC"));
			topJson = (JSONObject)parser.parse(jsonText);
			//double btgPrice = Double.valueOf(topJson.get("mid").toString());
			double btgPrice = Double.valueOf(topJson.get("last").toString());
			coins.get("BTG").setBtcPrice(btgPrice);
			//logger.info("Received BTG price from Bitfinex");
			logger.info("Received BTG price from Hitbtc");
		}

	}
	
	static void getCoinsMiningInfo(Map<String, Coin> coins) {
		CountDownLatch latch = new CountDownLatch(coins.size());
		
		for(Entry<String, Coin> entry : coins.entrySet()) {
			new Thread(()->{
				
				try {
				
					URL url = new URL(String.format("http://%s:%s", entry.getValue().getNodeHost(), entry.getValue().getNodePort()));
					
					URLConnection conn = url.openConnection();
					conn.setReadTimeout(2000); //2sec
					//conn.setRequestProperty("content-type", "text/plain");
					conn.setDoOutput(true);
					BASE64Encoder enc = new BASE64Encoder();
					String userpassword = entry.getValue().getRpcUsername() + ":" + entry.getValue().getRpcPassword();
					String encodedAuthorization = enc.encode( userpassword.getBytes() );
					conn.setRequestProperty("Authorization", "Basic "+ encodedAuthorization);
					OutputStream os = conn.getOutputStream();
					//String message = "{\"jsonrpc\": \"1.0\", \"id\":\"minehub\", \"method\": \"getmininginfo\", \"params\": [] }\r\n";
					String message = "{\"method\": \"getmininginfo\"}\r\n";
					os.write(message.getBytes());
					os.flush();
					InputStream is = conn.getInputStream();
					BufferedReader br = new BufferedReader(new InputStreamReader(is));
					StringBuffer sb = new StringBuffer();
					String line;
					while((line = br.readLine()) != null) {
						sb.append(line);
					}
					String jsonText = sb.toString();
					logger.debug(jsonText);
					entry.getValue().parseMinginInfoJson(jsonText);
					
				} catch (IOException | ParseException e) {
					logger.warn("failed to obtain mining info for " + entry.getKey() + " : " + e);
				} finally {
					latch.countDown();
				}
			}).start();
		}
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			// do nothing
		}
		logger.debug("collected mining info");
		
	}
	
	private static void loadProperties() {
		try {
			String propStr = FileUtils.readFileToString(new File("minehub.properties"));
			props.load(new StringReader(propStr.replace("\\","\\\\")));
		} catch (FileNotFoundException e) {
			logger.error("no minehub.properties found");
			exit();
		} catch (IOException e) {
			logger.error("error reading properties: " + e);
			exit();
		}
		
		bittrexSummariesUrl = props.getProperty("bittrex.summaries.url").trim();
		cryptopiaSummariesUrl = props.getProperty("cryptopia.summaries.url").trim();

	}

	public static void main(String[] args) {
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
		    class BusinessIntelligenceX509TrustManager implements X509TrustManager {
		        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		            return null;
		        }
		        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
		        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
		    }
			TrustManager[] trustAllCerts = new TrustManager[] { new  BusinessIntelligenceX509TrustManager() };
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (NoSuchAlgorithmException | KeyManagementException e1) {
			logger.error(e1);
			exit();
		}
		HttpsURLConnection.setDefaultHostnameVerifier((a, b) -> {return true;});

		loadProperties();
		
		String[] coinNames = props.getProperty("active.coins").split(",");
		Map<String, Coin> coins = new HashMap<>();
		for(String coinName : coinNames) {
			Coin coin = CoinFactory.newCoin(coinName = coinName.trim());
			coin.setReward(Double.valueOf(props.getProperty(String.format("coin.%s.reward", coinName)).trim()));
			String[] hostAndPort = props.getProperty(String.format("coin.%s.node.host", coinName)).trim().split(":");
			coin.setNodeHost(hostAndPort[0]);
			coin.setNodePort(Integer.valueOf(hostAndPort[1]));
			coin.setRpcUsername(props.getProperty(String.format("coin.%s.node.login", coinName)).trim());
			coin.setRpcPassword(props.getProperty(String.format("coin.%s.node.password", coinName)).trim());
			coin.setKoef(props.getProperty(String.format("coin.%s.koef", coinName)).trim());
			coin.setAlgo(props.getProperty(String.format("coin.%s.algo", coinName)).trim());
			coins.put(coinName, coin);
		}
		
		getCoinsMiningInfo(coins);
		try {
			getCoinsPrice(coins);
		} catch (IOException | ParseException e) {
			logger.error("Failed to obtain prices from exchanges: " + e);
			exit();
		}
		for(Coin coin : coins.values()) {
			logger.debug(coin);
		}
		
		// monitoring thread
		class MonitoringThread extends Thread {
			
			boolean hung = false;
			public synchronized boolean isHung() {
				return hung;
			}
			
			public synchronized void setHung(boolean hung) {
				this.hung = hung;
			}
			
			@Override
			public void run() {
				Thread t = null;	
				final MonitoringThread thisOne = this;
				
				while(true) {
					if (t == null) {
						t = new Thread(() -> {
							while (true) {
								try {
									Thread.sleep(3000); // 3s
								} catch (InterruptedException e) {
								}
								try {
									getCoinsMiningInfo(coins);
									thisOne.setHung(false);
								} catch (Throwable thro) {
									logger.error("Unexpected severe error during obtaining mining info: " + thro);
								}
							}
						});
						t.start();
					}
					try {
						Thread.sleep(10000); //10s
					} catch (InterruptedException e) {}
					if (this.isHung()) {
						t.stop();
						t = null;
					}
					this.setHung(true);
				}	
			}
		}
		
		new MonitoringThread().start();
		
		new Thread(()->{
			
			while(true) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {}
				try {
					getCoinsMiningInfo(coins);
				} catch (Throwable t) {
					logger.error("Unexpected severe error during obtaining mining info: " + t);
				}
			}
		}).start();
		new Thread(()->{
			while(true) {
				try {
					Thread.sleep(4000*60);
				} catch (InterruptedException e) {}
				try {
					getCoinsPrice(coins);
				} catch (IOException | ParseException e) {
					logger.warn("Failed to obtain prices from exchanges, using old ones : " + e);
				}				
			}
		}).start();
		
		HttpServer httpServer = null;
		try {
			httpServer = HttpServer.create(new InetSocketAddress(8018), 0);
		} catch (IOException e) {
			logger.error("Couldn't start server : " + e);
			exit();
		}
		httpServer.createContext("/minehub", new HttpHandler() {
			
			public Map<String, String> queryToMap(String query){
			    Map<String, String> result = new HashMap<String, String>();
			    for (String param : query.split("&")) {
			        String pair[] = param.split("=");
			        if (pair.length>1) {
			            result.put(pair[0], pair[1]);
			        }else{
			            result.put(pair[0], "");
			        }
			    }
			    return result;
			}
			
			@Override
			public void handle(HttpExchange t) throws IOException {
				Map<String, String> params = queryToMap(t.getRequestURI().getQuery());
				logger.debug(params);
			
				boolean superUser = "123123123".equals(params.get("key"));
				
				Map<Double, Coin> result = new HashMap<Double, Coin>(); //TreeMap is not an option here, as I insert coins before fill in their ordering criteria
				
				String message = null;
				out:
				for(Entry<String, String> entry : params.entrySet()) {
					Double hashrate = 0.0d;
					try {
						hashrate = Double.valueOf(entry.getValue());
					} catch (NumberFormatException e) {
						message = String.format("{\"error\": \"Parameter '%s' has incorrect numeric value '%s'\"}", entry.getKey(), entry.getValue());
						break out;
					}
					for(Coin coin: coins.values()) {
						if (entry.getKey().equalsIgnoreCase(coin.getAlgo())){
							double usdPerDay = btcToUsd * coin.getBTCPerDay(hashrate);
							while(result.containsKey(usdPerDay))
								usdPerDay += 0.0000001d;
							result.put(usdPerDay, coin);
						}
					}
					
				}
				
				if (message !=null) {
					t.sendResponseHeaders(404, message.length());
				} else {		
					JSONObject respObj = new JSONObject();
					//<Double> sortedProfits = new ArrayList<>(result.keySet());
					//Collections.sort(sortedProfits, Comparator.reverseOrder());
					for(Double usdPerDay : result.keySet()) {
						Coin coin = result.get(usdPerDay);
						JSONArray jarr = new JSONArray();
						jarr.add(usdPerDay);
						if (superUser) {
							jarr.add(Double.valueOf(coin.getBTCPerDay(Double.valueOf(params.get(coin.getAlgo())))));
							jarr.add(coin.getDifficulty());
//							jarr.add(coin.getNetHashRate());			
//							jarr.add(coin.getBtcPrice());
						}
						respObj.put(coin.getName(), jarr);
					}
					message = respObj.toJSONString();
					t.sendResponseHeaders(200, message.length());	
				}
				OutputStream os = t.getResponseBody();
				try {
					os.write(message.getBytes());
				} finally {
					os.close();
				}
				
				
				
			}
		});
		httpServer.setExecutor(null);
		httpServer.start();
	}

}
