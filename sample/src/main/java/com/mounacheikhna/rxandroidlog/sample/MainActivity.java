package com.mounacheikhna.rxandroidlog.sample;

import android.app.Activity;
import android.os.Bundle;

import com.mounacheikhna.rxandroidlog.RxLogging;

import rx.Observable;

/**
 * Created by m.cheikhna on 22/12/15.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Observable
                .range(1,20)
                .lift(RxLogging.<Integer>logger()
                        .showCount("total")
                        .when( x -> x%2==0)
                        .showCount("pairs total")
                        .onNext(false)
                        .log())
                .subscribe();
    }

}
