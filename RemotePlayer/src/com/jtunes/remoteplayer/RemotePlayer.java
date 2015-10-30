package com.jtunes.remoteplayer;

import java.net.ConnectException;
import java.net.URISyntaxException;

import oaxws.annotation.WebService;
import oaxws.annotation.WsMethod;
import oaxws.annotation.WsParam;
import serialiser.factory.SerialiserFactory;

import com.jaudiostream.client.SlidingWindowClient;
import com.jtunes.util.audio.AudioPlayer;
import com.jtunes.util.audio.AudioPlayerEventListener;
import com.jtunes.util.client.RemoteClient;
import com.jtunes.util.client.RunnableClient;
import com.jtunes.util.domain.DeviceStatus;
import com.jtunes.util.domain.DeviceType;
import com.jtunes.util.domain.PlayerState;
import com.jtunes.util.domain.PlayerStatus;
import com.jtunes.util.webservices.JTunesWsConstants.RemotePlayerService;

@RunnableClient
@WebService(name=RemotePlayerService.remotePlayer)
public class RemotePlayer extends RemoteClient implements AudioPlayerEventListener {

	private SlidingWindowClient jaudioStream;
	private AudioPlayer player;
	private PlayerStatus status = new PlayerStatus();
	private final long statusTimeout = 700;
				
	public RemotePlayer() {
		super(SerialiserFactory.getJsonSerialiser());
	}
			
	@Override
	protected void loggedIn() {
		String audioStreamAddress = getServerAudioAddress();
		if (audioStreamAddress != null) {
			try {
				audioStreamAddress += "/slwindow?"+name;
				logger.info("Connecting to server at address ["+audioStreamAddress+"]");
				jaudioStream.connect(audioStreamAddress);
				if (player == null) {
					player = new AudioPlayer(this, jaudioStream.getInputStream());
				}
				client.registerRemoteDevice(name, DeviceType.REMOTE_PLAYER);
			} catch (ConnectException | NumberFormatException | URISyntaxException e) {
				logger.error("Could not connect to audio streaming service.", e);
				fatalError();
			}
		} else {
			logger.error("Jtunes did not respond with audio streaming service address.");
			fatalError();
		}
	}
	
	private void terminate() {
		if (player != null) {
			player.stop();
			player.terminate();
		}
		if (jaudioStream != null) {
			jaudioStream.shutdown();
		}
	}
		
	@Override
	protected void beforeShutdown() {
		terminate();
	}
	
	@WsMethod(name=RemotePlayerService.pause)
	public void pause() {
		player.pause();
	}
	
	@WsMethod(name=RemotePlayerService.stop)
	public void stop() {
		player.stop();
		player.waitForState(PlayerState.STOPPED);
	}
	
	@WsMethod(name=RemotePlayerService.play)
	public void play() {
		player.play();
	}
	
	@WsMethod(name=RemotePlayerService.next)
	public void next() {
		player.stop();
		logger.info("Play received, waiting for clear stream...");
		player.waitForState(PlayerState.STOPPED);
        jaudioStream.awaitClear();
    	logger.info("Got clear stream.");
        player.play();  
	}
	
	@WsMethod(name=RemotePlayerService.seek)
	public void seek(@WsParam(name=RemotePlayerService.seekTo) int seekTo) {
		logger.info("Seek command received, seeking to ["+seekTo+"]");
		boolean wasPlaying = player.getState() == PlayerState.PLAYING;
		player.pause();
		player.waitForState(PlayerState.PAUSED);
		//player.stop();
		jaudioStream.seek(seekTo);
		if (wasPlaying) {
			player.play();	
		}
	}
	
	@Override
	protected void beforeStart() {
		jaudioStream = new SlidingWindowClient();
		wsManager.registerWebService(this);
	}

	@Override
	protected void onFatalError() {
		terminate();
	}

	@Override
	protected String version() {
		return "0.1";
	}

	@Override
	protected String serviceName() {
		return "RemotePlayer";
	}

	@Override
	public void onStopped() {
		stopStatus();
	}

	@Override
	public void onPlaying() {
		broadcastStatus(statusTimeout);
	}

	@Override
	public void onPaused() {
		stopStatus();
	}
	
	@Override
	public void onWaiting() {
		stopStatus();
	}

	@Override
	public void onComplete() {
		stopStatus();
		client.followOn();
	}
	
	@Override
	protected DeviceStatus getStatus() {
		status.setState(player.getState());
		status.setPosition(player.position());
		return status;
	}
	
}
