package com.jtunes.remoteplayer;

import oaxws.annotation.WebService;
import serialiser.factory.SerialiserFactory;

import com.jaudiostream.client.JAudioStreamClient;
import com.jtunes.util.audio.AudioPlayer;
import com.jtunes.util.audio.AudioPlayerEventListener;
import com.jtunes.util.client.RemoteClient;
import com.jtunes.util.domain.DeviceType;
import com.jtunes.util.domain.JTunesTypeRegistry;
import com.jtunes.util.webservices.JTunesWsConstants.RemotePlayerService;

@WebService(name=RemotePlayerService.remotePlayer)
public class RemotePlayer extends RemoteClient implements AudioPlayerEventListener {

	public static void main(String[] args) {
		RemotePlayer rp = new RemotePlayer();
		rp.start("remote_player", "remote_player");
	}
	
	private JAudioStreamClient jaudioStream;
	private AudioPlayer player;
		
	public RemotePlayer() {
		super(SerialiserFactory.getJsonSerialiser(new JTunesTypeRegistry()));
		jaudioStream = new JAudioStreamClient();
		player = new AudioPlayer(this, jaudioStream.getInputStream());
	}
	
	protected void start(String user, String password) {
		super.start(user, password);
		startComplete();
	}
	
	@Override
	protected void loggedIn() {
		client.registerRemoteDevice("", DeviceType.REMOTE_PLAYER);
	}
		
	@Override
	protected void shutdown() {
		super.shutdown();
		player.stop();
		player.terminate();
		try {
			jaudioStream.getInputStream().close();
		} catch (Exception e) {}
	}

	@Override
	public void onStopped() {
			
	}

	@Override
	public void onPlaying() {
			
	}

	@Override
	public void onPaused() {
			
	}

}
