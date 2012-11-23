package common;

import java.util.Arrays;

public class Response {
	byte[] data;
	byte sw1;
	byte sw2;
	int length = 0;

	public Response(byte sw1, byte sw2, byte[] data) {
		this.length = data.length;
		this.data = Arrays.copyOf(data, data.length);
		this.sw1 = sw1;
		this.sw2 = sw2;
	}

	public Response(byte sw1, byte sw2) {
		this.sw1 = sw1;
		this.sw2 = sw2;
	}

	public boolean success() {
		return (sw1 == CONSTANTS.SW1_SUCCESS);
	}

	public byte getStatus1() {
		return sw1;
	}

	public byte getStatus2() {
		return sw2;
	}

	public short getStatus() {
		return (short) (((sw1 & 0xff) << 8) | (sw2 & 0xff));
	}

	public int getNr() {
		return data.length;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
}