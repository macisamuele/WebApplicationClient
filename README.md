# WebApplicationClient
Generic Asynchronous HTTP Client for connection between Web Application and Android Client

This Abstract Client allows an effective simplification of the work related with the communication of thea Android Client with a REST HTTP Application.

The client provides an Asynchronous pattern to avoid to wrap it inside android.os.AsyncTask, android.content.Loader, java.lang.Thread or similar structures.

### Client Definition
For the definition of a "real" client it is suggested to define an extension of the WebApplicationClient class that defines the real client parameter.

##### Example of Client Definition
The API that is described in this example is essentially an HTTP GET Request in which is defined only 1 GET parameter (name is parameter).

	package your.package;
	
	import it.macisamuele.network.WebApplicationClient;

	/**
	 * Client for a real Web-Application
	 */
	public class RealClient extends WebApplicationClient {
		private static RealClient instance = new RealClient();

		public static RealClient getInstance() {
			return instance;
		}

		@Override
		public String getBaseUrl() {
			return url_of_the_web_application;
		}

		public static void send(Request request, Callback callback) {
			send(instance, request, callback);
		}

		public static void enablePersistenCookieStore(Context context) {
			instance.setPersistentCookieStore(context);
		}
	}

### Requests
For the requests in the WebApplicationClient class there is the implementation of a Generic Request, as Abstract Class.

This class provides a simple tool to simplify the handling of the message sending and answer management.

##### Example of Definition of Request Sender (class)
	package your.package;
	
	import java.util.HashMap;
	import java.util.Map;
	
	/**
	 * Definition of a simple Get Request
	 */
	public class GetRequest extends RealClient.Request {
	
		public static final String API = "get";
	
		private String parameter;
	
		private LoginRequest(String parameter) {
			this.parameter = parameter;
		}
	
		public static void send(String parameter, RealClient.Callback callback) {
			RealClient.send(new GetRequest(parameter), callback);
		}
	
		@Override
		public String getPath() {
			return API;
		}
	
		@Override
		public RealClient.TYPE getType() {
			return RealClient.TYPE.POST;
		}
	
		@Override
		public Map<String, String> getParameters() {
	
			final Map<String, String> parameters = new HashMap<>(2);
	
			if (username != null && password != null) {
				parameters.put("parameter", parameter);
			}
	
			return parameters;
		}
	}

##### Example of Use of Request Sender (i.e. from Activity)
	package your.package;
	
	import android.app.Activity;
	import android.os.Bundle;
	import android.util.Log;
	
	import org.apache.http.Header;
	
	import fr.eurecom.tapevent.network.LoginRequest;
	import fr.eurecom.tapevent.network.WebApplicationClient;
	
	public class MainActivity extends Activity {
		
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);
	
			GetRequest.send("example of parameter", new WebApplicationClient.Callback() {
				//describe additional methods for a lot of different interactions (when starts, ends, ...)
				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
					super.onSuccess(statusCode, headers, responseBody);
					//describe the behaviour when the request ends with success (HTTP Status 200)
				}
	
				@Override
				public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
					super.onFailure(statusCode, headers, responseBody, error);
					//describe the behaviour when the request ends with error (HTTP Status 4xx, 5xx)
				}
			});
		}
	}
