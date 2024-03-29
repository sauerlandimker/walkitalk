package de.pixart.messenger.ui.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import androidx.annotation.DimenRes;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.concurrent.RejectedExecutionException;

import de.pixart.messenger.R;
import de.pixart.messenger.services.AvatarService;
import de.pixart.messenger.ui.XmppActivity;

public class AvatarWorkerTask extends AsyncTask<AvatarService.Avatarable, Void, Bitmap> {
    private final WeakReference<ImageView> imageViewReference;
    private AvatarService.Avatarable avatarable = null;
    private @DimenRes
    int size;

    public AvatarWorkerTask(ImageView imageView, @DimenRes int size) {
        imageViewReference = new WeakReference<>(imageView);
        this.size = size;
    }

    @Override
    protected Bitmap doInBackground(AvatarService.Avatarable... params) {
        this.avatarable = params[0];
        final XmppActivity activity = XmppActivity.find(imageViewReference);
        if (activity == null) {
            return null;
        }
        return activity.avatarService().get(avatarable, (int) activity.getResources().getDimension(size), isCancelled());
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null && !isCancelled()) {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setBackgroundColor(0x00000000);
            }
        }
    }

    public static boolean cancelPotentialWork(AvatarService.Avatarable avatarable, ImageView imageView) {
        final AvatarWorkerTask workerTask = getBitmapWorkerTask(imageView);

        if (workerTask != null) {
            final AvatarService.Avatarable old = workerTask.avatarable;
            if (old == null || avatarable != old) {
                workerTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    public static AvatarWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getAvatarWorkerTask();
            }
        }
        return null;
    }

    public static void loadAvatar(final AvatarService.Avatarable avatarable, final ImageView imageView, final @DimenRes int size) {
        loadAvatar(avatarable, imageView, size, false);
    }

    public static void loadAvatar(final AvatarService.Avatarable avatarable, final ImageView imageView, final @DimenRes int size, final boolean overlay) {
        if (cancelPotentialWork(avatarable, imageView)) {
            final XmppActivity activity = XmppActivity.find(imageView);
            if (activity == null) {
                return;
            }
            final Bitmap bm = activity.avatarService().get(avatarable, (int) activity.getResources().getDimension(size), false);
            if (bm != null) {
                cancelPotentialWork(avatarable, imageView);
                if (overlay) {
                    activity.xmppConnectionService.fileBackend.drawOverlay(bm, R.drawable.pencil_overlay, 1.0f);
                    imageView.setImageBitmap(bm);
                } else {
                    imageView.setImageBitmap(bm);
                }
                imageView.setBackgroundColor(0x00000000);
            } else {
                imageView.setBackgroundColor(avatarable.getAvatarBackgroundColor());
                imageView.setImageDrawable(null);
                final AvatarWorkerTask task = new AvatarWorkerTask(imageView, size);
                final AsyncDrawable asyncDrawable = new AsyncDrawable(activity.getResources(), null, task);
                if (overlay) {
                    activity.xmppConnectionService.fileBackend.drawOverlayFromDrawable(asyncDrawable, R.drawable.pencil_overlay, 1.0f);
                    imageView.setImageDrawable(asyncDrawable);
                } else {
                    imageView.setImageDrawable(asyncDrawable);
                }
                try {
                    task.execute(avatarable);
                } catch (final RejectedExecutionException ignored) {
                }
            }
        }
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<AvatarWorkerTask> avatarWorkerTaskReference;

        AsyncDrawable(Resources res, Bitmap bitmap, AvatarWorkerTask workerTask) {
            super(res, bitmap);
            avatarWorkerTaskReference = new WeakReference<>(workerTask);
        }

        AvatarWorkerTask getAvatarWorkerTask() {
            return avatarWorkerTaskReference.get();
        }
    }
}
