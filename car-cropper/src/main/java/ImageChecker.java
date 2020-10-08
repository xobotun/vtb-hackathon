import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.datavec.image.loader.NativeImageLoader;

import java.io.File;
import java.io.IOException;

@Slf4j
public class ImageChecker {
    public static void main(String[] args) {
        val imageLoader = new NativeImageLoader(64, 64, 3);

        File root = new File("I:\\vtb_hack_cars\\raw\\");
        File[] subdirs = root.listFiles(File::isDirectory);

        if (subdirs == null || subdirs.length == 0) {
            log.warn("No directories inside root dir!");
            return;
        }

        for (File subdir : subdirs) {
            File[] images = subdir.listFiles();
            if (images == null) continue;

            log.info("Found {} images in {}", images.length, subdir);
            for (File image : images) {
                try {
                    imageLoader.asImageMatrix(image);
                } catch (IOException e) {
                    log.warn("Image {} is corrupted!", image);
                    log.debug("Details:", e);
                }
            }
        }
    }
}
