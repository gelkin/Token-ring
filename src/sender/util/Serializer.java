package sender.util;

import java.io.*;

public class Serializer {
    public byte[] serialize(Object obj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (
                ObjectOutputStream out = new ObjectOutputStream(bos)
        ) {
            out.writeObject(obj);
        } catch (IOException e) {
            throw new Error("Unexpected exception", e);
        }
        return bos.toByteArray();
    }

    public Object deserialize(byte[] bytes) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try (ObjectInputStream in = new ObjectInputStream(bis)){
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }
}
