package com.jtunes.remoteplayer;

import java.net.ConnectException;

import oaxws.annotation.WebService;
import oaxws.annotation.WsMethod;
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
	private String name;
	private boolean playing;
	private boolean paused;
		
	public RemotePlayer() {
		super(SerialiserFactory.getJsonSerialiser(new JTunesTypeRegistry()));
		name = "Remote player";
		jaudioStream = new JAudioStreamClient();
		player = new AudioPlayer(this, jaudioStream.getInputStream());
	}
	
	@Override
	protected void start(String user, String password) {
		super.start(user, password);
		startComplete();
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
			logger.error("Jtunes did mot respond with audio streaming service address.");
			fatalError();
		}
		client.registerStreamingClient(jaudioStream.getLocalPort());
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
	
	@WsMethod(name=RemotePlayerService.pause)
	void pause() {
		player.pause();
	}
	
	@WsMethod(name=RemotePlayerService.stop)
	void stop() {
		player.stop();
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
            }        
        }
	}

	@Override
	public void onStopped() {
		playing = false;
    	paused = false;	
	}

	@Override
	public void onPlaying() {
		playing = true;  
        paused = false;  
	}

	@Override
	public void onPaused() {
		playing = false;    
        paused = true;	
	}

}
