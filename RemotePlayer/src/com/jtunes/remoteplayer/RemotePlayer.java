package com.jtunes.remoteplayer;

import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.jaudiostream.client.SlidingWindowClient;
import com.jaudiostream.client.SlidingWindowClient.State;
import com.jtunes.util.audio.Analiser.AudioLevel;
import com.jtunes.util.audio.AudioPlayer;
import com.jtunes.util.audio.AudioPlayerEventListener;
import com.jtunes.util.audio.LevelListener;
import com.jtunes.util.client.JTunesAddress;
import com.jtunes.util.client.RemoteDeviceClient;
import com.jtunes.util.client.RunnableClient;
import com.jtunes.util.domain.DeviceStatus;
import com.jtunes.util.domain.DeviceType;
import com.jtunes.util.domain.PlayerState;
import com.jtunes.util.domain.PlayerStatus;
import com.jtunes.util.webservices.JTunesWsConstants.RemotePlayerService;

import oaxws.annotation.WebService;
import oaxws.annotation.WsMethod;
import oaxws.annotation.WsParam;
import oaxws.domain.WsSession;
import serialiser.factory.SerialiserFactory;

@RunnableClient
@WebService(RemotePlayerService.remotePlayer)
public class RemotePlayer extends RemoteDeviceClient implements AudioPlayerEventListener, LevelListener {

	private SlidingWindowClient jaudioStream;
	private AudioPlayer player;
	private PlayerStatus status = new PlayerStatus();
	private final long statusTimeout = 140;
				
	public RemotePlayer() {
		super(SerialiserFactory.getJsonSerialiser(), DeviceType.REMOTE_PLAYER);
	}
			
	@Override
	protected void loggedIn(WsSession session) {
		String audioStreamAddress = getAddress(JTunesAddress.AUDIO_STREAM_ADDRESS);
		if (audioStreamAddress != null) {
			try {
				audioStreamAddress += "/slwindow?"+name;
				logger.info("Connecting to server at address ["+audioStreamAddress+"]");
				jaudioStream.connect(audioStreamAddress);
				if (player == null) {
					player = new AudioPlayer(this, this, jaudioStream.getInputStream());
				}
				registerRemoteDevice();
				if (player.is(PlayerState.PLAYING)) {
					broadcastStatus(statusTimeout);
				}
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
	
	@WsMethod(RemotePlayerService.pause)
	public void pause() {
		player.pause();
		player.waitForState(PlayerState.PAUSED);
	}
	
	@WsMethod(RemotePlayerService.stop)
	public void stop() {
		player.stop();
		player.waitForState(PlayerState.STOPPED);
	}
	
	@WsMethod(RemotePlayerService.play)
	public void play() {
		player.play();
		waitForPlaying();
	}
	
	@WsMethod(RemotePlayerService.next)
	public void next() {
		logger.info("\n\n******************  Received next command  *******************");
		if (player.is(PlayerState.PLAYING) || player.is(PlayerState.PAUSED) || player.is(PlayerState.STOPPING)) {
			player.stop();
			logger.info("Waiting for player to stop...");
			player.waitForState(PlayerState.STOPPED);
		}
        logger.info("Waiting for clear stream...");
        try {
			jaudioStream.awaitClear(-1L, null);
			logger.info("Got clear stream.");
	        player.play();  
	        waitForPlaying();
		} catch (TimeoutException e) {
			logger.error("Timeout waiting for clear stream!", e);
		}
	}
	
	@WsMethod(RemotePlayerService.seek)
	public void seek(@WsParam(RemotePlayerService.seekTo) int seekTo) {
		logger.info("Seek command received, seeking to ["+seekTo+"]");
		boolean wasPlaying = player.getState() == PlayerState.PLAYING;
		player.pause();
		player.waitForState(PlayerState.PAUSED);
		//player.stop();
		jaudioStream.seek(seekTo);
		if (wasPlaying) {
			player.play();
			waitForPlaying();
		}
	}
	
	private void waitForPlaying() {
		try {
			player.waitForState(PlayerState.PLAYING, 2, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			// do nothing except log an error
			logger.error("Timedout waiting for audio player to play.");
		}
	}
	
	@Override
	protected void beforeStart() {
		jaudioStream = new SlidingWindowClient((newState, oldState) -> handleStreamStateChange(newState, oldState));
		wsManager.registerWebService(this);
	}
	
	private void handleStreamStateChange(State newState, State oldState) {
		logger.info("Stream state transition new ["+newState+"] old ["+oldState+"]");
	}

	@Override
	protected void onFatalError() {
		terminate();
	}

	@Override
	protected String version() {
		return "0.2";
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
		status.setPositionSecs(player.positionSecs());
		status.setPositionMilliSecs(player.positionMilliSecs());
		return status;
	}

	@Override
	protected void mainHook() {

	}

	@Override
	public void onLevelChange(AudioLevel level) {
		// convert infinite values to tangible numbers
		if (level.leftDb == Float.NEGATIVE_INFINITY) {
			status.setLeftLevel(-100);
		} else if (level.leftDb == Float.POSITIVE_INFINITY) {
			status.setLeftLevel(10);
		} else {
			status.setLeftLevel(level.leftDb);
		}
		if (level.rightDb == Float.NEGATIVE_INFINITY) {
			status.setRightLevel(-100);
		} else if (level.rightDb == Float.POSITIVE_INFINITY) {
			status.setRightLevel(10);
		} else {
			status.setRightLevel(level.rightDb);
		}
	}
	
}
