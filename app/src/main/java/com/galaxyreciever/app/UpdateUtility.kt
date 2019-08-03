package com.galaxyreciever.app

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File

class UpdateUtility {
    private val UPDATE_DIRECTORY_NAME = "TvUpdate"
    val UPDATE_FILE_NAME = "tv.apk"
    val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private val MINIMUM_UPDATE_SPACE = (350 * 1000 * 1000).toLong()
    private val FILE_SIZE_MARGIN = (2 * 1000 * 1000).toLong()

    /* Checks if external storage is available for read and write */
    fun isExternalStorageWritable(): Boolean {
        return Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
    }

    /* Checks if external storage is available to at least read */
    fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
    }

    fun getUpdateDirectory(): File? {
        val file = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ), UPDATE_DIRECTORY_NAME
        )

        if (!file.exists() && !file.mkdirs()) {
            Log.e("ERROR", "Directory not created")
            return null
        }

        return file
    }

    @SuppressLint("UsableSpace")
    fun getAvailableSize(): Long {
        val file = getUpdateDirectory() ?: return 0L

        return if (file.usableSpace > MINIMUM_UPDATE_SPACE) {
            file.usableSpace
        } else 0L

    }

//    public static Observable<Long> saveApkFile(final ResponseBody body, String channelName) {
//
//        return Observable.create(new ObservableOnSubscribe<Long>() {
//            @Override
//            public void subscribe(ObservableEmitter<Long> emitter) throws Exception {
//                long fileSizeDownloaded = 0;
//                long fileSize = body.contentLength();
//
//                String fileName = channelName + "_" + String.valueOf(System.currentTimeMillis()) + ".mp4";
////                File recordedFile = new File(getRecordDirectory(), fileName);
//                File recordedFile = new File(getSuitableStorage(fileSize), fileName);
//
//                byte[] fileReader = new byte[4096];
//                try (InputStream inputStream = body.byteStream();
//                     OutputStream outputStream = new FileOutputStream(recordedFile)) {
//
//                    while (true) {
//                        int read = inputStream.read(fileReader);
//
//                        if (read == -1) {
//                            break;
//                        }
//
//                        outputStream.write(fileReader, 0, read);
//
//                        fileSizeDownloaded += read;
//
//                        emitter.onNext(fileSizeDownloaded);
//                    }
//
//                    outputStream.flush();
//                    emitter.onComplete();
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    emitter.onError(e);
//                }
//            }
//        });
//    }

//    public static List<File> getUpdateFile() {
//        ArrayList<File> recordedFiles = new ArrayList<>();
//        for (File recordDirectory : getAvailableUpdateDirectories()) {
//            recordedFiles.addAll(Arrays.asList(recordDirectory.listFiles()));
//        }
//
//        return recordedFiles;
//    }

    fun getAvailableStorage(): Array<File>? {
        val storage = File("/storage")
        return if (storage.exists()) {
            storage.listFiles { dir, name -> name != "self" && name != "emulated" }
        } else null

    }

    fun getAvailableUpdateDirectory(): File? {
        val externalUpdateDirectory = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ), UPDATE_DIRECTORY_NAME
        )
        return if (externalUpdateDirectory.exists() || externalUpdateDirectory.mkdirs()) {
            externalUpdateDirectory
        } else null

    }

//    public static boolean isStorageSizeAvailable() {
//        List<File> recordDirectories = getAvailableUpdateDirectories();
//        for (File directory : recordDirectories) {
//            Log.i("FilesInfo", String.valueOf(directory.getAbsolutePath()));
//            Log.i("FilesInfo", String.valueOf(directory.getUsableSpace()));
//            if (directory.getUsableSpace() > MINIMUM_UPDATE_SPACE) {
//                return true;
//            }
//        }
//        return false;
//    }

//    public static boolean isStorageSizeSutable(long fileSize) {
//        List<File> recordDirectories = getAvailableUpdateDirectories();
//        for (File directory : recordDirectories) {
//            if ((fileSize + FILE_SIZE_MARGIN) < directory.getUsableSpace()) {
//                return true;
//            }
//        }
//
//        return false;
//    }

    fun getSuitableStorage(fileSize: Long): File? {
        val updateDirectory = getAvailableUpdateDirectory()
        if (updateDirectory != null) {
            val usableSpace = updateDirectory.usableSpace
            if (fileSize + FILE_SIZE_MARGIN < usableSpace) {
                return updateDirectory
            }
        }

        return null
    }

    fun getUpdateFile(fileSize: Long): File? {
        val directory = getSuitableStorage(fileSize)
        if (directory != null) {
            val file = File(getSuitableStorage(fileSize), UPDATE_FILE_NAME)
            if (file.exists()) file.delete()
            return file
        }

        return null
    }

    fun getUpdateFileUri(): Uri? {
        val updateDirectory = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ), UPDATE_DIRECTORY_NAME
        )

        val files = updateDirectory.listFiles { dir, name -> name == UPDATE_FILE_NAME }
        val updateFile = files[0]
        return if (updateFile != null) {
            Uri.fromFile(files[0])
        } else null

    }

}