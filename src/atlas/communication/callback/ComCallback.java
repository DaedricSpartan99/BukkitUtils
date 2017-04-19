package atlas.communication.callback;

import java.io.ByteArrayOutputStream;

public interface ComCallback {

	void communicate(String ip, byte[] inputData, ByteArrayOutputStream out);
}
