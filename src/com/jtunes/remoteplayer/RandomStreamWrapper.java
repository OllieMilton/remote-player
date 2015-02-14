package com.jtunes.remoteplayer;


public class RandomStreamWrapper {

//	private final JAudioStreamClient input;
//	private CrappyWrapper wrapper;
//	
//	public RandomStreamWrapper(JAudioStreamClient input) { 
//		this.input = input;
//		try {
//			wrapper = new CrappyWrapper();
//		} catch (FileNotFoundException e) {}
//	}
//	
//	public CrappyWrapper getWrapper() {
//		return wrapper;
//	}
//	
//	class CrappyWrapper extends RandomFileInputStream {
//		
//		CrappyWrapper() throws FileNotFoundException {
//			super((File)null);
//		}
//
//		@Override
//		public int read() throws IOException {
//			return input.getInputStream().read();
//		}
//
//		@Override
//		public synchronized void reset() {
//			try {
//				input.getInputStream().reset();
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//		}
//
//		@Override
//		public void close() throws IOException {
//			input.getInputStream().close();
//		}
//
//		@Override
//		public long getLength() throws IOException {
//			return input.getExpectedBytes();
//		}
//
//		@Override
//		public boolean markSupported() {
//			return input.getInputStream().markSupported();
//		}
//
//		@Override
//		public synchronized void mark(int arg0) {
//			input.getInputStream().mark(arg0);
//		}
//
//		@Override
//		public long skip(long bytes) throws IOException {
//			return input.getInputStream().skip(bytes);
//		}
//
//		@Override
//		public int read(byte[] buffer) throws IOException {
//			return input.getInputStream().read(buffer);
//		}
//
//		@Override
//		public int read(byte[] buffer, int pos, int bytes) throws IOException {
//			return input.getInputStream().read(buffer, pos, bytes);
//		}
//
//		@Override
//		public void seek(long pos) throws IOException {
//			input.seek((int)pos);
//		}
//
//		@Override
//		public long getPosition() throws IOException {
//			return (long) input.readPos();
//		}
//	}
}
