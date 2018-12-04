package com.neromatt.epiphany.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.neromatt.epiphany.Constants;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.Date;

import androidx.annotation.NonNull;

public class EpiphanyApplication extends Application {

    private static final int MAX_STACK_TRACE_SIZE = 131071; //128 KB - 1

    //Shared preferences
    private static final String SHARED_PREFERENCES_FILE = "custom_activity_on_crash";
    private static final String SHARED_PREFERENCES_FIELD_TIMESTAMP = "last_crash_timestamp";

    //Internal variables
    private static WeakReference<Activity> lastActivityCreated = new WeakReference<>(null);

    @Override
    public void onCreate() {
        super.onCreate();

        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, final Throwable throwable) {

                if (hasCrashedInTheLastSeconds()) {
                    if (oldHandler != null) {
                        oldHandler.uncaughtException(thread, throwable);
                        return;
                    }
                } else {
                    setLastCrashTimestamp(new Date().getTime());

                    Class<? extends Activity> errorActivityClass = ErrorActivity.class;

                    if (isStackTraceLikelyConflictive(throwable, errorActivityClass)) {
                        if (oldHandler != null) {
                            oldHandler.uncaughtException(thread, throwable);
                            return;
                        }
                    } else {

                        final Intent intent = new Intent(getApplicationContext(), errorActivityClass);
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        throwable.printStackTrace(pw);
                        String stackTraceString = sw.toString();

                        //Reduce data to 128KB so we don't get a TransactionTooLargeException when sending the intent.
                        //The limit is 1MB on Android but some devices seem to have it lower.
                        //See: http://developer.android.com/reference/android/os/TransactionTooLargeException.html
                        //And: http://stackoverflow.com/questions/11451393/what-to-do-on-transactiontoolargeexception#comment46697371_12809171
                        if (stackTraceString.length() > MAX_STACK_TRACE_SIZE) {
                            String disclaimer = " [stack trace too large]";
                            stackTraceString = stackTraceString.substring(0, MAX_STACK_TRACE_SIZE - disclaimer.length()) + disclaimer;
                        }
                        intent.putExtra(Constants.EXTRA_STACK_TRACE, stackTraceString);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                }
                final Activity lastActivity = lastActivityCreated.get();
                if (lastActivity != null) {
                    //We finish the activity, this solves a bug which causes infinite recursion.
                    //See: https://github.com/ACRA/acra/issues/42
                    lastActivity.finish();
                    lastActivityCreated.clear();
                }
                killCurrentProcess();
            }
        });

        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            //int currentlyStartedActivities = 0;
            //final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (activity.getClass() != ErrorActivity.class) {
                    lastActivityCreated = new WeakReference<>(activity);
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
                //currentlyStartedActivities++;
                //isInBackground = (currentlyStartedActivities == 0);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                //Do nothing
            }

            @Override
            public void onActivityPaused(Activity activity) {
                //Do nothing
            }

            @Override
            public void onActivityStopped(Activity activity) {
                //currentlyStartedActivities--;
                //isInBackground = (currentlyStartedActivities == 0);
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                //Do nothing
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                //Do nothing
            }
        });
    }

    private static boolean isStackTraceLikelyConflictive(@NonNull Throwable throwable, @NonNull Class<? extends Activity> activityClass) {
        do {
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                if ((element.getClassName().equals("android.app.ActivityThread") && element.getMethodName().equals("handleBindApplication")) || element.getClassName().equals(activityClass.getName())) {
                    return true;
                }
            }
        } while ((throwable = throwable.getCause()) != null);
        return false;
    }

    private static void killCurrentProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    @SuppressLint("ApplySharedPref") //This must be done immediately since we are killing the app
    private void setLastCrashTimestamp(long timestamp) {
        getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE).edit().putLong(SHARED_PREFERENCES_FIELD_TIMESTAMP, timestamp).commit();
    }

    private long getLastCrashTimestamp() {
        return getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE).getLong(SHARED_PREFERENCES_FIELD_TIMESTAMP, -1);
    }

    private boolean hasCrashedInTheLastSeconds() {
        long lastTimestamp = getLastCrashTimestamp();
        long currentTimestamp = new Date().getTime();

        return (lastTimestamp <= currentTimestamp && currentTimestamp - lastTimestamp < 2000);
    }
}
