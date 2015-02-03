package com.jtunes.remoteplayer;

import java.net.ConnectException;

import oaxws.annotation.WebService;
import oaxws.annotation.WsMethod;
import oaxws.annotation.WsParam;
import serialiser.factory.SerialiserFactory;

import com.jaudiostream.client.JAudioStreamClient;
import com.jtunes.util.audio.AudioPlayer;
import com.jtunes.util.audio.AudioPlayerEventListener;
import com.jtunes.util.client.RemoteClient;
import com.jtunes.util.domain.DeviceStatus;
import com.jtunes.util.domain.DeviceType;
import com.jtunes.util.domain.JTunesTypeRegistry;
import com.jtunes.util.domain.PlayerState;
import com.jtunes.util.domain.PlayerStatus;
import com.jtunes.util.webservices.JTunesWsConstants.RemotePlayerService;

@WebService(name=RemotePlayerService.remotePlayer)
public class RemotePlayer extends RemoteClient implements AudioPlayerEventListener {

	public static void main(String[] args) {
		RemotePlayer rp = new RemotePlayer();
		rp.start("remote_player", "remote_player");
	}
	
	private JAudioStreamClient jaudioStream;
	private AudioPlayer player;
	private PlayerStatus status = new PlayerStatus();
	private final long statusTimeout = 700;
				
	public RemotePlayer() {
		super(SerialiserFactory.getJsonSerialiser(new JTunesTypeRegistry()));
	}
			
	@Override
	protected void loggedIn() {
		client.registerRemoteDevice(name, DeviceType.REMOTE_PLAYER);
		String audioStreamAddress = getServerAudioAddress();
		if (audioStreamAddress != null) {
			String[] addr = audioStreamAddress.split(",");
			try {
				jaudioStream.connect(addr[0], Integer.valueOf(addr[1]).intValue(), name);
			} catch (ConnectException | NumberFormatException e) {
				logger.error("Could not connect to audio streaming service.");
				fatalError();
			}
			setupStatusSprayer(statusTimeout);
		} else {
			logger.error("Jtunes did not respond with audio streaming service address.");
			fatalError();
		}
	}
		
	@Override
	protected void beforeShutdown() {
		if (player != null) {
			player.stop();
			player.terminate();
		}
		if (jaudioStream != null) {
			jaudioStream.shutdown();
		}
	}
	
	@WsMethod(name=RemotePlayerService.pause)
	public void pause() {
		player.pause();
	}
	
	@WsMethod(name=RemotePlayerService.stop)
	public void stop() {
		player.stop();
	}
	
	@WsMethod(name=RemotePlayerService.play)
	public void play() {
		player.play();
	}
	
	@WsMethod(name=RemotePlayerService.next)
	public void next() {
		player.stop();
		logger.info("Play received, waiting for clear stream...");
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
		jaudioStream = new JAudioStreamClient();
		player = new AudioPlayer(this, jaudioStream.getInputStream());
		wsManager.registerWebService(this);
	}

	@Override
	protected void onFatalError() {
		player.stop();
		player.terminate();
		jaudioStream.shutdown();
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
		sendStatus();
	}

	@Override
	public void onPlaying() {
		sendStatus();
	}

	@Override
	public void onPaused() {
		sendStatus();
	}

	@Override
	public void onComplete() {
		sendStatus();
		client.followOn();
	}
	
	private void sendStatus() {
		status.setState(player.getState());
		status.setPosition(player.position());
		client.sendDeviceStatus(status);
	}

	@Override
	protected DeviceStatus getStatus() {
		status.setState(player.getState());
		status.setPosition(player.position());
		return status;
	}
	
}
