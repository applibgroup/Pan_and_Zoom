package xyz.zpayh.hdimageview;

import ohos.media.image.ImageSource;

import java.io.FileDescriptor;
import java.io.IOException;

public class Instance {
    public static ImageSource newInstance(byte[] data,
                                          int offset, int length, boolean isShareable) throws IOException {
        if ((offset | length) < 0 || data.length < offset + length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return nativeNewInstance(data, offset, length, isShareable);
    }

    public static ImageSource newInstance(
            FileDescriptor fd, boolean isShareable) throws IOException {
        return nativeNewInstance(fd, isShareable);
    }

    private static native ImageSource nativeNewInstance(
            byte[] data, int offset, int length, boolean isShareable);

    private static native ImageSource nativeNewInstance(
            FileDescriptor fd, boolean isShareable);


}
