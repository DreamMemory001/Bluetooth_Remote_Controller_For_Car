package com.example.xuchao.blueteeth_test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothChatService {
    //蓝牙发送机理
	private static final String TAG = "BluetoothChatService";
	private static final boolean D = true;

	private static final String NAME = "BluetoothChat";

	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//是通用唯一识别码

	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;

	public static final int STATE_NONE = 0;
	private static final int STATE_LISTEN = 1;
												
	private static final int STATE_CONNECTING = 2;
													
	public static final int STATE_CONNECTED = 3;

	//获取蓝牙设备信息
	public BluetoothChatService(Context context, Handler handler){
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
	}
	//进行异步回调信息
	private synchronized void setState(int state){
		if (D)
			Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	public synchronized int getState(){
		return mState;
	}
    //实现生命周期中的start（）方法的同步
	public synchronized void start(){
		if (D)
			Log.d(TAG, "start");

		if (mConnectThread != null){
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null){
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		if (mAcceptThread == null){
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}
		setState(STATE_LISTEN); //STATE_LISTEN为1
	}

	//连接设备的
	public synchronized void connect(BluetoothDevice device){
		if (D)
			Log.d(TAG, "connect to: " + device);

		if (mState == STATE_CONNECTING){   //STATE_CONNECTING为2
			if (mConnectThread != null){
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}
		if (mConnectedThread != null){
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		//开辟连接线程
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}
      //单独的BluetoothSocket类去初始化一个外接连接和管理该连接。 真正的去连接某一个蓝牙设备
	public synchronized void connected(BluetoothSocket socket,BluetoothDevice device){
		if (D)
			Log.d(TAG, "connected");

		if (mConnectThread != null){
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null){
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		if (mAcceptThread != null){
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
        //把信息用Handler进行传递
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}
	public synchronized void stop(){
		if (D)
			Log.d(TAG, "stop");
		if (mConnectThread != null){
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null){
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		if (mAcceptThread != null){
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
		setState(STATE_NONE);
	}

	public void write(byte[] out){
		ConnectedThread r;
		synchronized (this){
			if (mState != STATE_CONNECTED)  // STATE_CONNECTED为3
				return;
			r = mConnectedThread;
		}
		r.write(out);
	}

	//连接失败的处理函数
	private void connectionFailed(){
		setState(STATE_LISTEN);
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	//连接丢失的操作
	private void connectionLost() {
		setState(STATE_LISTEN);
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}


    //创建蓝牙socket，获取输入输出流
    private class AcceptThread extends Thread{
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread(){
			BluetoothServerSocket tmp = null;

			try{
				tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			}
			catch (IOException e){
				Log.e(TAG, "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run(){
			if (D)
				Log.d(TAG, "BEGIN mAcceptThread" + this);
			setName("AcceptThread");
			BluetoothSocket socket = null;

			while (mState != STATE_CONNECTED){
				try{
					socket = mmServerSocket.accept();
				}
				catch (IOException e){
					Log.e(TAG, "accept() failed", e);
					break;
				}

				if (socket != null){
					synchronized (BluetoothChatService.this){
						switch (mState){
							case STATE_LISTEN:
							case STATE_CONNECTING:
								connected(socket, socket.getRemoteDevice());
								break;
							case STATE_NONE:
							case STATE_CONNECTED:
								try{
									socket.close();
								}
								catch (IOException e){
									Log.e(TAG,
											"Could not close unwanted socket", e);
								}
								break;
						}
					}
				}
			}
			if (D)
				Log.i(TAG, "END mAcceptThread");
		}

		void cancel(){
			if (D)
				Log.d(TAG, "cancel " + this);
			try{
				mmServerSocket.close();
			}
			catch (IOException e){
				Log.e(TAG, "close() of server failed", e);
			}
		}
	}
	
	private class ConnectThread extends Thread{
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device){
			mmDevice = device;
			BluetoothSocket tmp = null;

			try{
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			}
			catch (IOException e){
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run(){
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			mAdapter.cancelDiscovery();

			try{
				mmSocket.connect();
			}
			catch (IOException e){
				connectionFailed();
				try{
					mmSocket.close();
				}
				catch (IOException e2){
					Log.e(TAG,"unable to close() socket during connection failure",e2);
				}
				BluetoothChatService.this.start();
				return;
			}

			synchronized (BluetoothChatService.this){
				mConnectThread = null;
			}
			connected(mmSocket, mmDevice);
		}

		public void cancel(){
			try{
				mmSocket.close();
			}
			catch (IOException e){
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	private class ConnectedThread extends Thread{
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;


		public ConnectedThread(BluetoothSocket socket){
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			try{
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			}
			catch (IOException e){
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
          //复写线程中的run（） 开启线程
		public void run(){
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;

			while (true){
				try{
					bytes = mmInStream.read(buffer);

					mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes,-1, buffer).sendToTarget();
				}
				catch (IOException e){
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
		}
		//写入操作
		public void write(byte[] buffer){
			try{
				mmOutStream.write(buffer);

				mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1,
						buffer).sendToTarget();
			}
			catch (IOException e){
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel(){
			try{
				mmSocket.close();
			}
			catch (IOException e){
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}
