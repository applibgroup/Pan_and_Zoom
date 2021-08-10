package xyz.zpayh.hdimageview.datasource.interceptor;

//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.BitmapRegionDecoder;
//import android.net.Uri;
//import androidx.annotation.NonNull;
//import androidx.exifinterface.media.ExifInterface;
//import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


import ohos.aafwk.ability.DataAbilityHelper;
import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.agp.render.render3d.BuildConfig;
import ohos.media.image.PixelMap;
import ohos.media.image.ImageSource;
import ohos.app.Context;
import ohos.utils.net.Uri;
//import xyz.zpayh.hdimageview.BuildConfig;
//import xyz.zpayh.hdimageview.ImageSource;
import xyz.zpayh.hdimageview.state.Orientation;
import xyz.zpayh.hdimageview.util.DiskLruCache;
import xyz.zpayh.hdimageview.util.ImageCache;
import xyz.zpayh.hdimageview.util.Preconditions;
import xyz.zpayh.hdimageview.util.UriUtil;

import static xyz.zpayh.hdimageview.Instance.newInstance;

/**
 * 创建人： zp
 * 创建时间：2017/8/3
 */

public class Interceptors {

    private static final String TAG = "Interceptors";

    private static final int FIX_CACHE_SIZE = 10 * 1024 * 1024; // 20MB
    private static final String FIX_CACHE_DIR = "fixJPEG";
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final int DISK_CACHE_INDEX = 0;
    private static DiskLruCache mHttpDiskCache;
    private static Context interceptors_context;

    public static void initDiskLruCache(Context context){
        interceptors_context = context;
        if (mHttpDiskCache != null){
            return;
        }
        Preconditions.checkNotNull(context);
        File httpCacheDir = ImageCache.getDiskCacheDir(context, FIX_CACHE_DIR);
        if (!httpCacheDir.exists()){
            if (!httpCacheDir.mkdirs()){
                mHttpDiskCache = null;
                return;
            }
        }
        if (ImageCache.getUsableSpace(httpCacheDir) > FIX_CACHE_SIZE){
            try {
                mHttpDiskCache = DiskLruCache.open(httpCacheDir,1,1, FIX_CACHE_SIZE);
            } catch (IOException e) {
                e.printStackTrace();
                mHttpDiskCache = null;
            }
        }
    }

    private static synchronized File processFile(InputStream data, String url, IOException e) throws IOException{
        if (BuildConfig.DEBUG) {
//            Log.d(TAG, "processFile - " + data);
        }
        final String key = ImageCache.hashKeyForDisk(url);
        DiskLruCache.Snapshot snapshot;

        File file = null;

        if (mHttpDiskCache != null) {
            snapshot = mHttpDiskCache.get(key);
            if (snapshot == null) {
                if (BuildConfig.DEBUG) {
//                    Log.d(TAG, "processBitmap, not found in http cache, downloading...");
                }
                DiskLruCache.Editor editor = mHttpDiskCache.edit(key);
                if (editor != null) {
                    if (downloadUrlToStream(data,
                            editor.newOutputStream(DISK_CACHE_INDEX))) {
                        editor.commit();
                    } else {
                        editor.abort();
                    }
                }
                mHttpDiskCache.flush();
                snapshot = mHttpDiskCache.get(key);
            }
            if (snapshot != null) {
                file = new File(mHttpDiskCache.getDirectory(), key + "." + DISK_CACHE_INDEX);
            }
        }

        if (file == null || !file.exists()){
            if (BuildConfig.DEBUG) {
//                Log.d(TAG, "下载缓存失败:" + url);
            }
            throw e;
        }

        return file;
    }

    private static boolean downloadUrlToStream(InputStream inputStream, OutputStream outputStream) throws IOException{

        BufferedInputStream in = new BufferedInputStream(inputStream, IO_BUFFER_SIZE);
        BufferedOutputStream out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

        int b;
        while ((b = in.read()) != -1) {
            out.write(b);
        }
        try {
            out.close();
            in.close();
        } catch (final IOException e) {e.printStackTrace();}

        return true;
    }

    public static ImageSource fixJPEGDecoder(InputStream inputStream, Uri uri, IOException e) throws IOException {
        return fixJPEGDecoder(processFile(inputStream,uri.toString(),e),e);
    }

    public static ImageSource fixJPEGDecoder(File file, IOException e) throws IOException {

        if (file == null || !file.exists()){
            throw e;
        }


        Uri uri = Uri.getUriFromFile(file);
        PixelMap bitmap = PixelMapFromFile.getPixelMapByUri(uri.toString());
        if (bitmap == null) {
            if (BuildConfig.DEBUG) {
//                Log.d(TAG, "加载缓存失败:" + file.getAbsolutePath());
            }
            throw e;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        bitmap.(PixelMap.CompressFormat.WEBP, 85, baos);
        ImageSource decoder = newInstance(baos.toByteArray(),0,baos.size(),false);
        bitmap.release();
        if (BuildConfig.DEBUG) {
//            Log.d(TAG, "fixJPEGDecoder: 从此处修复Bitmap");
        }
        return decoder;
    }

    static int getExifOrientation(String sourceUri) {
        Uri uri = Uri.parse(sourceUri);
        if (UriUtil.isNetworkUri(uri)) {
            try {
                final String key = ImageCache.hashKeyForDisk(sourceUri);
                if (mHttpDiskCache != null) {
                    DiskLruCache.Snapshot snapshot = mHttpDiskCache.get(key);
                    if (snapshot != null) {
                        File file = new File(mHttpDiskCache.getDirectory(), key + "." + DISK_CACHE_INDEX);
                        DataAbilityHelper dataAbilityHelper = DataAbilityHelper.creator(interceptors_context, uri);
                        InputStream inputStream = dataAbilityHelper.obtainInputStream(uri);
                        ImageSource exifInterface = ImageSource.create(inputStream, null);
                        int orientationAttr = exifInterface.getImagePropertyInt("Orientation",1);
                        switch (orientationAttr) {
                            case 1:
                            case 0:
                                return Orientation.ORIENTATION_0;
                            case 6:
                                return Orientation.ORIENTATION_90;
                            case 3:
                                return Orientation.ORIENTATION_180;
                            case 8:
                                return Orientation.ORIENTATION_270;
                        }
                    }
                }
            } catch (IOException | DataAbilityRemoteException e) {
                e.printStackTrace();
            }
        }
        return Orientation.ORIENTATION_EXIF;
    }
}
