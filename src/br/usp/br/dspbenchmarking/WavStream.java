package br.usp.br.dspbenchmarking;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import android.util.Log;



public class WavStream {

    // Wav file info
	private String filePath;
	private int channels;
	private int sampleRate;
	private InputStream wavStream;
	private int dataSizeInBytes = 0;
	
	// Contants
	//private static final String RIFF_HEADER = "RIFF";
	//private static final String WAVE_HEADER = "WAVE";
	//private static final String FMT_HEADER = "fmt ";
	//private static final String DATA_HEADER = "data";
	private static final int HEADER_SIZE = 44;
	//private static final String CHARSET = "ASCII";
	
	// Buffers
	ShortBuffer dataBuffer = null;

	/**
	 * Constructor
	 * @throws FileNotFoundException 
	 */
	public WavStream(String path) throws FileNotFoundException, IOException {
		filePath = new String(path);
		wavStream = new BufferedInputStream(new FileInputStream(filePath));
		wavStream.mark(44);
		readHeader();
		wavStream.reset();
		initWavPcm();
	}


	/**
	 * Reads the Wav Header.
	 * @throws IOException 
	 */
	private void readHeader() throws IOException {

		ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		wavStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity());
		buffer.rewind();
		buffer.position(buffer.position() + 16);
		
		int subChunkSize = buffer.getInt();
		Log.e("WavStream", "subChunkSize="+subChunkSize);
		
		int format = buffer.getShort();
		checkFormat(format == 1, "Unsupported encoding: " + format); // 1 means
																		// Linear
																		// PCM
		channels = buffer.getShort();
		checkFormat(channels == 1, "Unsupported channels: "
				+ channels);
		sampleRate = buffer.getInt();
		checkFormat(sampleRate <= 48000 && sampleRate >= 11025, "Unsupported rate: " + sampleRate);
		buffer.position(buffer.position() + 6);
		int bits = buffer.getShort();
		checkFormat(bits == 16, "Unsupported bits: " + bits);
		while (buffer.getInt() != 0x61746164) { // "data" marker
			//Log.d("WavInfo", "Skipping non-data chunk");
			//int size = buffer.getInt();
			//wavStream.skip(size);
			//buffer.rewind();
			//wavStream.read(buffer.array(), buffer.arrayOffset(), 8);
			//buffer.rewind();
		}
		dataSizeInBytes = buffer.getInt();
		checkFormat(dataSizeInBytes > 0, "wrong datasize: " + dataSizeInBytes);

		wavStream.reset();
		wavStream.mark(44 + dataSizeInBytes);
		//return new WavInfo(new FormatSpec(rate, channels == 2), dataSize);
	}
	
	/**
	 * Prepares the Short buffer for reading the wav file.
	 * @throws IOException
	 */
	private void initWavPcm() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(44 + dataSizeInBytes);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		wavStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity());
		buffer.rewind();
		buffer.position(buffer.position() + 44);
		dataBuffer = buffer.asShortBuffer();
		dataBuffer.mark();
	}
	
	/**
	 * @return Short buffer with the data of the WAV file.
	 */
	public ShortBuffer getBuffer() {
		return dataBuffer;
	}
	
	/**
	 * resets the internal buffer.
	 */
	public void reset() {
		dataBuffer.reset();
	}
	
	/**
	 * Queries the Short buffer for more data.
	 * @param buffer
	 * @param offset
	 * @param size
	 */
	public void getFromBuffer(short[] buffer, int offset, int size) {
		dataBuffer.get(buffer, offset, size);
	}
	
	
	private void checkFormat(Boolean b, String msg) throws IOException {
		if (!b)
			throw new IOException(msg);
	}


	/**
	 * Returns the sample rate.
	 * @return
	 */
	public int getSampleRate() {
		return sampleRate;
	}
	
	/**
	 * Returns the WAV size in Bytes.
	 * @return
	 */
	public int getDataSizeInBytes() {
		return dataSizeInBytes;
	}
	
	/**
	 * Returns the WAV size in Shorts.
	 * @return
	 */
	public int getDataSizeInShorts() {
		return dataSizeInBytes / 2;
	}

}