import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;


public class Main {

    public static byte[] createBytes() {
	byte[] bytes = new byte[512];
	for (byte b : bytes) {
	    b = (byte) 1;
	}
	return bytes;
    }

    public static void main(String[] args) throws Exception {
	ByteBuffer buf = ByteBuffer.allocate(1024);
	byte[] b = createBytes();
	buf.put(b);
	System.out.println("# buf: " + buf);
	buf.put(b);
	System.out.println("# buf: " + buf);
	try {
	    buf.put(b);
	} catch (BufferOverflowException e) {
	    buf.clear();
	}
	System.out.println("# buf: " + buf);
	buf.put(b);
	System.out.println("# buf: " + buf);

	System.out.println("####");

	buf.flip();
	System.out.println("# buf: " + buf);
	byte[] c = new byte[buf.remaining()];
	buf.get(c, 0, c.length);
	System.out.println("# buf: " + buf);
	buf.clear();
	System.out.println("# buf: " + buf);
    }
}
