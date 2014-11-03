package com.jtunes.remoteplayer;

import java.net.ConnectException;

import oaxws.annotation.WebService;
import oaxws.annotation.WsMethod;
import serialiser.factory.SerialiserFactory;

import com.jaudiostream.client.JAudioStreamClient;
import com.jtunes.util.audio.SynchronousTransportAudioPlayer;
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
	private SynchronousTransportAudioPlayer player;
	private String name;
	private boolean playing;
	private boolean paused;
		
	public RemotePlayer() {
		super(SerialiserFactory.getJsonSerialiser(new JTunesTypeRegistry()));
		name = "Remote player";
		jaudioStream = new JAudioStreamClient();
		player = new SynchronousTransportAudioPlayer(jaudioStream.getInputStream());
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
	protected void shutdown() {
		super.shutdown();
		player.stop();
		player.terminate();
		jaudioStream.shutdown();
	}
	
	@WsMethod(name=RemotePlayerService.pause)
	void pause() {
		player.pause();
		playing = false;    
        paused = true;	
	}
	
	@WsMethod(name=RemotePlayerService.stop)
	void stop() {
		player.stop();
		playing = false;
    	paused = false;	
	}
	
	@WsMethod(name=RemotePlayerService.play)
	void play() {
		if (playing && !paused) {
            pause();    
        } else {
            if (!playing) {
                logger.info("Play received, waiting for clear stream. Playing ["+playing+"], paused ["+paused+"], clear stream ["+jaudioStream.isClear()+"]");
                while (!playing && !jaudioStream.isClear() && !paused) {
                    try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						
					}
                }    
                logger.info("Got clear stream.");
                player.play();  
                playing = true;  
                paused = false;  
            }        
        }
	}
	
	@Override
	protected void beforeStart() {
		
	}

	@Override
	protected void onFatalError() {
		jaudioStream.shutdown();
	}
	
}
