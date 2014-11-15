package com.jtunes.remoteplayer;

import java.net.ConnectException;

import oaxws.annotation.WebService;
import oaxws.annotation.WsMethod;
import serialiser.factory.SerialiserFactory;

import com.jaudiostream.client.JAudioStreamClient;
import com.jtunes.util.audio.AudioPlayer;
import com.jtunes.util.audio.AudioPlayer.PlayerState;
import com.jtunes.util.client.RemoteClient;
import com.jtunes.util.domain.DeviceType;
import com.jtunes.util.domain.JTunesTypeRegistry;
import com.jtunes.util.webservices.JTunesWsConstants.RemotePlayerService;

@WebService(name=RemotePlayerService.remotePlayer)
public class RemotePlayer extends RemoteClient {

	public static void main(String[] args) {
		RemotePlayer rp = new RemotePlayer();
		rp.start("remote_player", "remote_player");
	}
	
	private JAudioStreamClient jaudioStream;
	private AudioPlayer player;
			
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
		} else {
			logger.error("Jtunes did not respond with audio streaming service address.");
			fatalError();
		}
	}
		
	@Override
	protected void beforeShutdown() {
		player.stop();
		player.terminate();
		jaudioStream.shutdown();
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
		if (player.getState() == PlayerState.PLAYING) {
            player.pause();    
        } else {
            if (player.getState() != PlayerState.PLAYING) {
                logger.info("Play received, waiting for clear stream. Playing clear stream ["+jaudioStream.isClear()+"]");
                // need to wait for a clear stream unless we are paused, if paused just play.
                while (!jaudioStream.isClear() && player.getState() != PlayerState.PAUSED) {
                    try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						
					}
                }    
                logger.info("Got clear stream.");
                player.play();  
            }        
        }
	}
	
	@Override
	protected void beforeStart() {
		jaudioStream = new JAudioStreamClient();
		player = new AudioPlayer(null, jaudioStream.getInputStream());
		wsManager.registerWebService(this);
	}

	@Override
	protected void onFatalError() {
		player.stop();
		player.terminate();
		jaudioStream.shutdown();
	}
	
}
