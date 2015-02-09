package com.crossbow.volley.toolbox;

import android.content.Context;
import android.os.Build;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.crossbow.volley.VolleyStack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * Created by Patrick on 11/01/14.
 */
public class Crossbow {

    private Context context;
    private VolleyStack volleyStack;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
    private ImageLoader.ImageCache imageCache;
    private static Crossbow crossbow;

    private static final String STACK_DIR = "Crossbow";
    private static final String STACK_FILE = "stack.data";

    private Crossbow(Context context) {
        this.context = context.getApplicationContext();
        if(volleyStack == null) {
            volleyStack = createStack();
        }
        buildFromStack(volleyStack);
    }

    public static Crossbow get(Context context) {
        if(crossbow == null) {
            crossbow = new Crossbow(context);
        }
        return crossbow;
    }

    public static <T extends VolleyStack> void registerStack(Context context, Class<T> volleyStack) {

        File dir = new File(context.getCacheDir().getAbsolutePath() + File.separator + STACK_DIR);
        dir.mkdirs();
        File file = new File(dir, STACK_FILE);
        String stackName = volleyStack.getName();
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(stackName.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private VolleyStack createStack() {
        File file = new File(context.getCacheDir().getAbsolutePath() + File.separator + STACK_DIR, STACK_FILE);
        String stackName;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            stackName = sb.toString();

            Class<?> stackClass = Class.forName(stackName);

            Class[] types = {Context.class};
            Constructor<?> constructor = stackClass.getConstructor(types);

            Object[] parameters = {context};
            volleyStack = (VolleyStack) constructor.newInstance(parameters);
            br.close();
            return volleyStack;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return new DefaultVolleyStack(context);

    }

    protected final Context getContext() {
        return context.getApplicationContext();
    }

    private void buildFromStack(VolleyStack volleyStack) {
        if(requestQueue == null) {
            requestQueue = volleyStack.createRequestQueue();
        }

        if(imageCache == null) {
            imageCache = volleyStack.createImageCache();
        }

        if(imageLoader == null) {
            imageLoader = volleyStack.createImageLoader(requestQueue, imageCache);
        }
    }

    public final RequestQueue getRequestQueue(){
        return requestQueue;
    }

    /**
     * Adds a request to the shared queue
     */
    public final <T> void addToQueue(Request<T> request) {
        requestQueue.add(request);
    }

    /**
     * Stop the shared queue
     */
    public final void stopQueue() {
       requestQueue.stop();
    }

    /**
     * Start the shared queue
     */
    public final void startQueue() {
        requestQueue.start();
    }

    /**
     * Gets the shared image loader instance
     */
    public final ImageLoader getImageLoader() {
        return imageLoader;
    }

    /**
     * Gets the {@link com.android.volley.toolbox.ImageLoader.ImageCache} used in the image loader.
     */
    public final ImageLoader.ImageCache getImageCache(){
        return imageCache;
    }

    /**
     * Test if the app is running on something newer than HoneyComb (API >= 11)
     */
    public static boolean isHoneyCombOrNewer(){
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB);
    }

    /**
     * Cancels all requests in the queue.
     * <p>Throws Runtime exception if the application object does not extend RWVolleyApplication</p>
     *
     */
    public void cancelAll() {
        requestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }
}