package com.jtunes.remoteplayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.FrameListener;
import org.kc7bfi.jflac.PCMProcessor;
import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.io.RandomFileInputStream;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.SeekTable;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteData;

import com.jtunes.util.audio.Analiser;
import com.jtunes.util.audio.Analiser.AudioLevel;

public class FLACPlayer implements PCMProcessor, FrameListener {

	private AudioFormat fmt;
    private DataLine.Info info;
    private SourceDataLine line;
    private Analiser analiser = new Analiser();
    
    private void play(InputStream stream) {
    	FLACDecoder decoder = new FLACDecoder(stream);
    	decoder.addPCMProcessor(this);
    	decoder.addFrameListener(this);
    	try {
    		//decoder.readMetadata();
    		//decoder.seek(10000000);
    		decoder.decode();
    	} catch (IOException e) {
    		e.printStackTrace();
    	} finally {
    		if (line != null) {
    			line.drain();
        		line.close();
    		}
    	}
    }
    
	@Override
	public void processPCM(ByteData pcm) {
		AudioLevel lev = analiser.getLevel(analiser.getWindow(pcm.getData()));
		System.out.println("left: "+lev.leftDb+"\t\t right: "+lev.rightDb+"\t\t TS: "+System.currentTimeMillis());
		line.write(pcm.getData(), 0, pcm.getLen());
	}

	@Override
	public void processStreamInfo(StreamInfo streamInfo) {
		try {
            fmt = getAudioFormat(streamInfo);
            info = new DataLine.Info(SourceDataLine.class, fmt, AudioSystem.NOT_SPECIFIED);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt, AudioSystem.NOT_SPECIFIED);
            line.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
	}
	
	public static void main(String[] args) throws Exception {
		FLACPlayer player = new FLACPlayer();
		player.play(new RandomFileInputStream(new File("D:/music/St._Germain/Tourist/03_So_Flute.flac")));
	}

	@Override
	public void processError(String arg0) {

	}

	@Override
	public void processFrame(Frame arg0) {

	}

	@Override
	public void processMetadata(Metadata metadata) {
		if (metadata instanceof SeekTable) {
		
		}
	}

	private AudioFormat getAudioFormat(StreamInfo streamInfo) {
        return new AudioFormat(streamInfo.getSampleRate(), streamInfo.getBitsPerSample(), streamInfo.getChannels(), (streamInfo.getBitsPerSample() <= 8) ? false : true, false);
    }
	
}
