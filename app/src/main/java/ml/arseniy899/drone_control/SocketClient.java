package ml.arseniy899.drone_control;

import android.app.Activity;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SocketClient
{
	static class ClientCallback
	{
		void onMessageRecived(String message){}
		void onMessageRecived(byte [] message){}
		void onMessageSent(byte [] message){}
		void onMessageSent(String message){}
		void onConnectionStateCh(boolean isConnected){}
	}
	private WebSocketFactory factory = new WebSocketFactory();
	private WebSocket ws;
	private ClientCallback callback;
	Activity activity;
	public SocketClient(Activity mActivity, String url, ClientCallback mCallback)
	{
		try
		{
			activity = mActivity;
			this.callback = mCallback;
			if(url.isEmpty())
				url = "r-ho.ml:8765";
			ws = factory.createSocket("ws://"+url, 300);
			ws.addListener(new WebSocketAdapter() {
				@Override
				public void onTextMessage(WebSocket websocket, String message)
				{
					// Received a text message.
					activity.runOnUiThread(()->callback.onMessageRecived(message));
				}
				
				@Override
				public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception
				{
					super.onBinaryMessage(websocket, binary);
					activity.runOnUiThread(()->callback.onMessageRecived(binary));
					
				}
				
				@Override
				public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception
				{
					super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
					callback.onConnectionStateCh(false);
					connect();
					LogKeeper.d("WS/onDisconnected","closedByServer? "+closedByServer);
				}
				
				@Override
				public void onError(WebSocket websocket, WebSocketException cause) throws Exception
				{
					super.onError(websocket, cause);
					callback.onConnectionStateCh(false);
					if(ws.isOpen())
						ws.disconnect(150);
					connect();
					LogKeeper.d("WS/onError","cause: "+cause.getError());
				}
				
				@Override
				public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception
				{
					super.onUnexpectedError(websocket, cause);
					callback.onConnectionStateCh(false);
					if(ws.isOpen())
						ws.disconnect(150);
					connect();
					LogKeeper.d("WS/onUnexpectedError","cause: "+cause.getError());
				}
				
				@Override
				public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception
				{
					super.onConnected(websocket, headers);
					activity.runOnUiThread(()->callback.onConnectionStateCh(true));
					LogKeeper.d("WS/onConnected","Hooray! ");
				}
				
				@Override
				public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception
				{
					super.onStateChanged(websocket, newState);
					if(newState == WebSocketState.OPEN)
						callback.onConnectionStateCh(true);
					else if(newState == WebSocketState.CLOSED)
						callback.onConnectionStateCh(false);
					LogKeeper.d("WS/onStateChanged","isConnected="+isConnected()+"; state="+newState);
				}
			});
			connect();
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
			LogKeeper.e("WS/create","WS excep: "+e.getLocalizedMessage()+"; "+LogKeeper.getExceptionInfo(e));
		}
	}
	
	public void connect()
	{
		new Thread(()->
		{
			try
			{
				Thread.sleep(200);
				if (ws.getState() == WebSocketState.CLOSED || ws.getState() == WebSocketState.CLOSING)
					ws = ws.recreate();
				if (ws.getState() != WebSocketState.CONNECTING)
					ws.connect();
			}
			catch (WebSocketException e)
			{
				callback.onConnectionStateCh(false);
				e.printStackTrace();
				LogKeeper.e("WS/connect","WS excep: "+e.getError()+"; "+LogKeeper.getExceptionInfo(e));
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				LogKeeper.e("WS/connect","WS excep: "+e.getLocalizedMessage()+"; "+LogKeeper.getExceptionInfo(e));
			}
			catch (IOException e)
			{
				e.printStackTrace();
				LogKeeper.e("WS/connect","WS excep: "+e.getLocalizedMessage()+"; "+LogKeeper.getExceptionInfo(e));
			}
			
		}).start();
	}
	public void disconnect()
	{
		if(ws != null && ws.isOpen())
			ws.disconnect(149);
	}
	void sendData(byte [] data)
	{
		if(ws != null && ws.isOpen())
		{
			ws.sendBinary(data);
			callback.onMessageSent(data);
		}
		else if(ws != null)
			connect();
		else
			LogKeeper.e("SocketClient/Send","No WebSocket is opened");
	}
	
	/**
	 *
	 * @param yaw рыскание (0...254)
	 * @param throttle газ (0...254)
	 * @param roll крен (0...254)
	 * @param pitch тангаж (0...254)
	 * @param flyMode режим полета (0...3)
	 */
	void sendFlyData(int yaw, int throttle, int roll, int pitch, int flyMode)
	{
		sendData(new byte[]{(byte) 255, (byte) yaw,(byte) throttle,(byte) roll,(byte) pitch,(byte) flyMode});
	}
	void sendMotorOn(int flyMode)
	{
		sendData(new byte[]{(byte) 255, (byte) 254,(byte) 254,(byte) 127, (byte) 127, (byte) flyMode});
	}
	void sendMotorOff(int flyMode)
	{
		sendData(new byte[]{(byte) 255, (byte) 1,(byte) 254,(byte) 127, (byte) 127, (byte) flyMode});
	}
	boolean isConnected()
	{
		return ws != null && ws.isOpen();
	}
}
